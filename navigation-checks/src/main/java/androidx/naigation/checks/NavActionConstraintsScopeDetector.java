package androidx.naigation.checks;

import androidx.naigation.checks.dao.NavConstraintsDao;

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
import com.sun.istack.NotNull;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NavActionConstraintsScopeDetector extends ResourceXmlDetector {

    private Map<String, List<String>> scope = null;
    private List<NavConstraintsDao> constraintsToCheck = null;
    public static final Issue ACTION_CONSTRAINT_NOT_IN_SCOPE = Issue.create("ActionConstraintNotInScope",
            "Variable used in action constraint is not in scope",
            "Variable used in constraint of an action " +
                    "must be a widget in the source screen or must be" +
                    "an incoming variable a part of a transition into the source screen",
            Category.CORRECTNESS, 10, Severity.ERROR,
            new Implementation(NavActionConstraintsScopeDetector.class, Scope.RESOURCE_FILE_SCOPE));

    public NavActionConstraintsScopeDetector() {

    }

    @Override
    public boolean appliesTo(ResourceFolderType folderType) {
        return folderType == ResourceFolderType.NAVIGATION;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Collections.singleton(SdkConstants.TAG_ACTION);
    }

    @Override
    public void afterCheckFile(Context context) {
        if(constraintsToCheck != null) {
            for(NavConstraintsDao navConstraintsDao : constraintsToCheck) {
                String srcId = navConstraintsDao.getSrcId();
                List<String> parms = navConstraintsDao.getConstraintArgs();
                if(!scope.get(srcId).containsAll(parms)) {
                    context.report(ACTION_CONSTRAINT_NOT_IN_SCOPE,navConstraintsDao.getLocation(),"<fun> in action uses a variable that is not in scope");
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
            List<String> parentArgNames = getTagValues(parentArguments,Constants.NAV_ATTR_NAME);
            if(parentArgNames != null) {
                updateScope(parentId,parentArgNames);
            }

            NodeList parentWidgets = parent.getElementsByTagName(Constants.TAG_WIDGET);
            List<String> widgetIds = getTagValues(parentWidgets,Constants.NAV_ATTR_WID);
            if(widgetIds != null) {
                updateScope(parentId,widgetIds);
            }

            NodeList parmArgs = element.getElementsByTagName(Constants.TAG_PARM);
            List<String> parmArgVals = getTagValues(parmArgs,Constants.NAV_ATTR_PARM_ARG);
            if(parmArgVals != null) {
                NavConstraintsDao navConstraintsDao = new NavConstraintsDao(parentId,parmArgVals,context.getLocation(element));
                if(constraintsToCheck == null) constraintsToCheck = new ArrayList<>();
                constraintsToCheck.add(navConstraintsDao);
            }

            NodeList actionArgumentTags = element.getElementsByTagName(Constants.TAG_ARGUMENT);
            List<String> actionArgumentNames = getTagValues(actionArgumentTags,Constants.NAV_ATTR_NAME);
            if(actionArgumentNames != null && element.getAttribute(Constants.NAV_ACTION_DEST).contains("/")) {
                updateScope(element.getAttribute(Constants.NAV_ACTION_DEST).split("/")[1],actionArgumentNames);
            }
        }
    }

    private List<String> getTagValues(NodeList nodes, String attrName) {
        List<String> tmpList = new ArrayList<>();
        for(int i = 0; i < nodes.getLength(); i++) {
            Element elem = (Element) nodes.item(i);
            if(attrName.equals(Constants.NAV_ATTR_PARM_ARG)) {
                if(elem.getAttribute(Constants.NAV_ATTR_PARM_ARG).startsWith(Constants.NAV_PREFIX_VAR)
                        || elem.getAttribute(Constants.NAV_ATTR_PARM_ARG).startsWith(SdkConstants.ID_PREFIX)) {
                    tmpList.add(elem.getAttribute(Constants.NAV_ATTR_PARM_ARG).split("/")[1]);
                }
            }
            else if(attrName.equals(Constants.NAV_ATTR_NAME)) {
                String varName = elem.getAttribute(Constants.NAV_ATTR_NAME);
                tmpList.add(varName);
            }
            else if(attrName.equals(Constants.NAV_ATTR_WID)) {
                String value = elem.getAttribute(Constants.NAV_ATTR_WID);
                if(value.startsWith(SdkConstants.ID_PREFIX)) {
                    String wid = value.split("/")[1];
                    tmpList.add(wid);
                }
            }
        }
        return tmpList;
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
