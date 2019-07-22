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

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NavActionArgumentScopeDetector extends ResourceXmlDetector {

    private Map<String, String> syn_map = null;
    private Map<String, List<String>> scope = null;
    private Map<String, String> graph_startDest_map = null;
    private Map<String, String> graph_layout_map = null;
    private Map<String, String> activity_layout_map = null;
    private List<NavConstraintsDao> argumentsToCheck = null;

    public static final Issue ACTION_ARGUMENT_VALUE_NOT_IN_SCOPE = Issue.create("ArgValueScope",
            "Argument value not in scope",
            "If the default value of an argument in an action is a variable then " +
                    "the variable should be in the scope of the screen from which the action is originating",
            Category.LINT, 10, Severity.ERROR,
            new Implementation(NavActionArgumentScopeDetector.class, Scope.ALL_RESOURCES_SCOPE));

    public NavActionArgumentScopeDetector() {

    }

    @Override
    public boolean appliesTo(ResourceFolderType folderType) {
        return (folderType == ResourceFolderType.NAVIGATION || folderType == ResourceFolderType.LAYOUT);
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(SdkConstants.TAG_ACTIVITY, SdkConstants.TAG_NAVIGATION, Constants.TAG_ARGUMENT,SdkConstants.TAG_ACTION);
    }

    @Override
    public Collection<String> getApplicableAttributes() {
        return Collections.singleton(SdkConstants.ATTR_NAV_GRAPH);
    }

    @Override
    public void visitAttribute(XmlContext context, Attr attr) {
        String fileNm = context.file.getName();
        String graph = attr.getValue();
        if(graph.contains("/")) {
            String key = graph.split("/")[1];
            if(graph_layout_map == null) graph_layout_map = new HashMap<>();
            graph_layout_map.put(key, fileNm.split(".xml")[0]);
        }
    }

    @Override
    public void afterCheckFile(Context context) {
        if(argumentsToCheck != null && scope != null) {
            for(NavConstraintsDao navConstraintsDao : argumentsToCheck) {
                String srcId = navConstraintsDao.getSrcId();
                List<String> parms = navConstraintsDao.getConstraintArgs();
                List<String> args = scope.get(srcId);
                if(hasNestedSynonym(srcId)) {
                    if(args == null) args = scope.get(syn_map.get(srcId));
                    else args.addAll(scope.get(syn_map.get(srcId)));
                }
                if(hasActivitySynonym(srcId)) {
                    if(args == null)
                        args = scope.get(activity_layout_map
                                .get(graph_layout_map
                                    .get(graph_startDest_map
                                            .get(srcId))));
                    else
                        args.addAll(scope.get(activity_layout_map
                                .get(graph_layout_map
                                        .get(graph_startDest_map
                                                .get(srcId)))));
                }
                if(parms != null && !args.containsAll(parms)) {
                    context.report(ACTION_ARGUMENT_VALUE_NOT_IN_SCOPE,navConstraintsDao.getLocation(),"defaultValue <argument> may not be in scope." +
                            "Make sure it is a widget in parent screen or part of an incoming transitions into the parent screen.");
                }
            }
        }
    }

    @Override
    public void visitElement(XmlContext context, Element element) {
        String name = element.getTagName();
        if(name.equals(SdkConstants.TAG_ACTIVITY)) {
            String id = element.getAttribute(Constants.ATTR_ID);
            String layout = element.getAttribute(Constants.ATTR_NAV_PARENT_LAYOUT);
            if(id.contains("/") && layout.contains("/")) {
                if(activity_layout_map == null) activity_layout_map = new HashMap<>();
                activity_layout_map.put(layout.split("/")[1], id.split("/")[1]);
            }
        }
        else if(name.equals(SdkConstants.TAG_NAVIGATION)) {
            String navId = element.getAttribute(Constants.ATTR_ID);
            String startDest = element.getAttribute(Constants.NAV_ATTR_START_DEST);
            String fileNm = context.file.getName();
            if(syn_map == null) syn_map = new HashMap<>();
            if(startDest.contains("/") && navId.contains("/")){
                syn_map.put(startDest.split("/")[1],navId.split("/")[1]);
                if(graph_startDest_map == null) graph_startDest_map = new HashMap<>();
                else graph_startDest_map.put(startDest.split("/")[1],fileNm.split(".xml")[0]);
            }
            else
                throw new IndexOutOfBoundsException("startDestination or navigation ID is missing.");
        }
        else if(name.equals(SdkConstants.TAG_ACTION)) {
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
            List<String> argumentValues = UsefulMethods.getTagValues(actionArgumentTags,Constants.NAV_ATTR_VAL_ARG,null);
            if(!argumentValues.isEmpty()) {
                NavConstraintsDao navConstraintsDao = new NavConstraintsDao(parentId,argumentValues,context.getLocation(element));
                if(argumentsToCheck == null) argumentsToCheck = new ArrayList<>();
                argumentsToCheck.add(navConstraintsDao);
            }

            List<String> actionArgumentNames = UsefulMethods.getTagValues(actionArgumentTags,Constants.NAV_ATTR_NAME,null);
            if(actionArgumentNames != null && element.getAttribute(Constants.NAV_ACTION_DEST).contains("/")) {
                updateScope(element.getAttribute(Constants.NAV_ACTION_DEST).split("/")[1],actionArgumentNames);
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

    private Boolean hasNestedSynonym(String srcId) {
        return(syn_map != null && syn_map.get(srcId) != null
                && scope.get(syn_map.get(srcId)) != null);
    }

    private Boolean hasActivitySynonym(String srcId) {
        return(graph_startDest_map != null &&
                graph_layout_map != null &&
                activity_layout_map != null &&
                graph_startDest_map.get(srcId) != null &&
                graph_layout_map.get(graph_startDest_map.get(srcId)) != null &&
                activity_layout_map.get(graph_layout_map.get(graph_startDest_map.get(srcId))) != null);
    }
}
