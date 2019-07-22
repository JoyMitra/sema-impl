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
import com.android.tools.lint.detector.api.Location;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NavDuplicateVariableDetector extends ResourceXmlDetector {

    private Map<String,String> syn_map = null;
    private Map<String, List<String>> scope = null;
    private Map<String, String> graph_startDest_map = null;
    private Map<String, String> graph_layout_map = null;
    private Map<String, String> activity_layout_map = null;
    private List<NavConstraintsDao> screenIds = null;

    public static final Issue NAV_DUPLICATE_VAR = Issue.create("NavDuplicateVariable",
            "Duplicate variables are not allowed",
            "A variable propagating into a screen as an argument cannot be used as an argument to another screen",
            Category.LINT, 10, Severity.ERROR,
            new Implementation(NavDuplicateVariableDetector.class, Scope.RESOURCE_FILE_SCOPE));

    public NavDuplicateVariableDetector() {

    }

    @Override
    public boolean appliesTo(ResourceFolderType folderType) {
        return (folderType == ResourceFolderType.NAVIGATION || folderType == ResourceFolderType.LAYOUT);
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(SdkConstants.TAG_ACTIVITY, SdkConstants.TAG_NAVIGATION, SdkConstants.TAG_ACTION,Constants.TAG_FRAGMENT);
    }

    @Override
    public Collection<String> getApplicableAttributes() {
        return Collections.singleton(SdkConstants.ATTR_NAV_GRAPH);
    }

    @Override
    public void afterCheckFile(Context context) {
        if(screenIds != null && scope != null) {
            for(NavConstraintsDao navConstraintsDao1 : screenIds) {
                String screenId1 = navConstraintsDao1.getSrcId();
                List<String> inVars1 = scope.get(screenId1);
                if(hasNestedSynonym(screenId1)) {
                    if(inVars1 == null) inVars1 = scope.get(syn_map.get(screenId1));
                    else inVars1.addAll(scope.get(syn_map.get(screenId1)));
                }
                if(hasActivitySynonym(screenId1)) {
                    if(inVars1 == null)
                        inVars1 = scope.get(activity_layout_map
                                .get(graph_layout_map
                                        .get(graph_startDest_map
                                                .get(screenId1))));
                    else
                        inVars1.addAll(scope.get(activity_layout_map
                                .get(graph_layout_map
                                        .get(graph_startDest_map
                                                .get(screenId1)))));
                }
                for (NavConstraintsDao navConstraintsDao2 : screenIds) {
                    String screenId2 = navConstraintsDao2.getSrcId();
                    if(!screenId1.equals(screenId2)) {
                        List<String> inVars2 = scope.get(screenId2);
                        if(hasNestedSynonym(screenId2)) {
                            if (inVars2 == null) inVars2 = scope.get(syn_map.get(screenId2));
                            else inVars2.addAll(scope.get(syn_map.get(screenId2)));
                        }
                        if(hasActivitySynonym(screenId2)) {
                            if(inVars2 == null)
                                inVars2 = scope.get(activity_layout_map
                                        .get(graph_layout_map
                                                .get(graph_startDest_map
                                                        .get(screenId2))));
                            else
                                inVars2.addAll(scope.get(activity_layout_map
                                        .get(graph_layout_map
                                                .get(graph_startDest_map
                                                        .get(screenId2)))));
                        }
                        if(containsAny(inVars1,inVars2)) {
                            context.report(NAV_DUPLICATE_VAR,navConstraintsDao1.getLocation(),
                                    "Duplicate variables exist in : " + screenId1 + "," + screenId2);
                        }
                    }
                }
            }
        }
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
        else if (name.equals(SdkConstants.TAG_ACTION)) {
            Element parent = (Element) element.getParentNode();
            String parentId = parent.getAttribute(Constants.ATTR_ID).split("/")[1];

            NodeList parentArguments = parent.getElementsByTagName(Constants.TAG_ARGUMENT);
            List<String> parentArgNames = UsefulMethods.getTagValues(parentArguments, Constants.NAV_ATTR_NAME,
                    Collections.singletonList(SdkConstants.TAG_ACTION));
            if (parentArgNames != null) {
                updateScope(parentId, parentArgNames);
            }

            NodeList actionArgumentTags = element.getElementsByTagName(Constants.TAG_ARGUMENT);
            List<String> actionArgumentNames = UsefulMethods.getTagValues(actionArgumentTags, Constants.NAV_ATTR_NAME, null);
            if (actionArgumentNames != null && element.getAttribute(Constants.NAV_ACTION_DEST).contains("/")) {
                updateScope(element.getAttribute(Constants.NAV_ACTION_DEST).split("/")[1], actionArgumentNames);
            }
        }
        else if(name.equals(Constants.TAG_FRAGMENT)){
            String id = element.getAttribute(Constants.ATTR_ID).split("/")[1];
            NavConstraintsDao navConstraintsDao = new NavConstraintsDao(id,"",context.getLocation(element));
            if(screenIds == null) screenIds = new ArrayList<>();
            screenIds.add(navConstraintsDao);
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

    private boolean containsAny(List<String> list1, List<String> list2) {
        if(list1 != null && list2 != null) {
            for(String l1 : list1) {
                if(list2.contains(l1)) return true;
            }
        }
        return false;
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
