package androidx.naigation.checks;

import androidx.naigation.checks.dao.NavConstraintsDao;
import androidx.naigation.checks.util.Constants;
import androidx.naigation.checks.util.UsefulMethods;

import com.android.SdkConstants;
import com.android.resources.ResourceFolderType;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NavScreenArgumentScopeDetector extends ResourceXmlDetector {

    private Map<String, List<String>> scope = null;
    private Map<String, List<String>> actionMap = null;
    private List<NavConstraintsDao> argsToCheck = null;

    public static final Issue SCREEN_ARGUMENT_VALUE_NOT_IN_SCOPE = Issue.create("ScreenArgValueScope",
            "Screen level argument value not in scope",
            "The default value of an argument at the screen level should either be a widget in the screen that " +
                    "triggers the screen with the argument or a variable that is in the scope of the screen that triggers" +
                    "the screen with the argument",
            Category.LINT, 10, Severity.ERROR,
            new Implementation(NavScreenArgumentScopeDetector.class, Scope.RESOURCE_FILE_SCOPE));

    public NavScreenArgumentScopeDetector() {

    }

    @Override
    public boolean appliesTo(ResourceFolderType folderType) {
        return folderType == ResourceFolderType.NAVIGATION;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(Constants.TAG_ARGUMENT, SdkConstants.TAG_ACTION);
    }

    @Override
    public void afterCheckFile(Context context) {
        if(argsToCheck != null) {
            for(NavConstraintsDao navConstraintsDao : argsToCheck) {
                String srcId = navConstraintsDao.getSrcId();
                String argVal = navConstraintsDao.getArgVal();
                List<String> parmVals = navConstraintsDao.getConstraintArgs();
                List<String> argSrcs = actionMap.get(srcId);
                if(argSrcs != null) {
                    for(String argSrc : argSrcs) {
                        if(scope.get(argSrc) != null && argVal != null && !scope.get(argSrc).contains(argVal)) {
                            context.report(SCREEN_ARGUMENT_VALUE_NOT_IN_SCOPE,navConstraintsDao.getLocation(),argVal + " could not be resolved");
                        }
                        if(scope.get(argSrc) != null && parmVals != null && !scope.get(argSrc).containsAll(parmVals)) {
                            context.report(SCREEN_ARGUMENT_VALUE_NOT_IN_SCOPE,navConstraintsDao.getLocation(), "parm arguments could not be resolved");
                        }
                    }
                }
            }
        }
    }

    @Override
    public void visitElement(XmlContext context, Element element) {
        String name = element.getTagName();
        if(name.equals(SdkConstants.TAG_ACTION)) {
            Element parent = (Element) element.getParentNode();
            String parentId = parent.getAttribute(Constants.ATTR_ID).split("/")[1];

            NodeList parentArguments = parent.getElementsByTagName(Constants.TAG_ARGUMENT);
            List<String> parentArgNames = UsefulMethods.getTagValues(parentArguments,Constants.NAV_ATTR_NAME,
                    Collections.singletonList(SdkConstants.TAG_ACTION));
            if(parentArgNames != null) {
                updateScope(parentId,parentArgNames);
            }

            NodeList parentWidgets = parent.getElementsByTagName(Constants.TAG_WIDGET);
            List<String> widgetIds = UsefulMethods.getTagValues(parentWidgets,Constants.NAV_ATTR_WID,null);
            if(widgetIds != null) {
                updateScope(parentId,widgetIds);
            }

            NodeList actionArgumentTags = element.getElementsByTagName(Constants.TAG_ARGUMENT);
            List<String> actionArgumentNames = UsefulMethods.getTagValues(actionArgumentTags,Constants.NAV_ATTR_NAME,null);
            if(actionArgumentNames != null && element.getAttribute(Constants.NAV_ACTION_DEST).contains("/")) {
                updateScope(element.getAttribute(Constants.NAV_ACTION_DEST).split("/")[1],actionArgumentNames);
            }

            if(element.getAttribute(Constants.NAV_ACTION_DEST).contains("/")) {
                String destId = element.getAttribute(Constants.NAV_ACTION_DEST).split("/")[1];
                if(actionMap == null) actionMap = new HashMap<>();
                if(actionMap.get(destId) != null){
                    List<String> tmp = actionMap.get(destId);
                    tmp.add(parentId);
                    actionMap.put(destId,tmp);
                }
                else {
                    List<String> tmp = new ArrayList<>();
                    tmp.add(parentId);
                    actionMap.put(destId,tmp);
                }
            }
        }
        else if(name.equals(Constants.TAG_ARGUMENT)) {
            Element parent = (Element) element.getParentNode();
            String parentId = parent.getAttribute(Constants.ATTR_ID).split("/")[1];
            if(!parent.getTagName().equals(SdkConstants.TAG_ACTION)) {
                String value = element.getAttribute(Constants.NAV_ATTR_VAL_ARG);
                if(value.startsWith(Constants.NAV_PREFIX_VAR) || value.startsWith(SdkConstants.ID_PREFIX)) {
                    NavConstraintsDao navConstraintsDao = new NavConstraintsDao(parentId,value.split("/")[1],
                            context.getLocation(element));
                    if(argsToCheck == null) argsToCheck = new ArrayList<>();
                    argsToCheck.add(navConstraintsDao);

                }
                else if(value.startsWith(Constants.NAV_PREFIX_FUN)) {
                    NodeList funNodes = element.getElementsByTagName(Constants.TAG_PARM);
                    List<String> values = UsefulMethods.getTagValues(funNodes,Constants.NAV_ATTR_PARM_ARG,null);
                    NavConstraintsDao navConstraintsDao = new NavConstraintsDao(parentId,values,
                            context.getLocation(element));
                    if(argsToCheck == null) argsToCheck = new ArrayList<>();
                    argsToCheck.add(navConstraintsDao);
                }
            }
        }
    }

    private void updateScope(String key, List<String> value) {
        if(scope == null) scope = new HashMap<>();
        if(scope.get(key) == null) scope.put(key,value);
        else {
            List<String> l = scope.get(key);
            l.addAll(value);
            scope.put(key,l);
        }
    }
}
