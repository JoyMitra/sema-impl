package androidx.naigation.checks;

import androidx.naigation.checks.dao.NavWidgetDao;
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
import java.util.List;
import java.util.Map;

public class NavWidgetInitDetector extends ResourceXmlDetector {

    private Map<String,List<String>> scope_map = null;
    private Map<String,String> syn_map = null;
    private Map<String, String> graph_startDest_map = null;
    private Map<String, String> graph_layout_map = null;
    private Map<String, String> activity_layout_map = null;
    private List<NavWidgetDao> mWidgets = null;

    public static final Issue INVALID_WIDGET_INITIALIZATION = Issue.create("InvalidWidgetInit",
            "Variable used to initialize widget is not in scope",
            "Variables used to initialize a widget must be provided as an <argument> to the source screen",
            Category.LINT, 10,Severity.ERROR,
            new Implementation(NavWidgetInitDetector.class, Scope.ALL_RESOURCES_SCOPE));

    public NavWidgetInitDetector() {

    }

    @Override
    public boolean appliesTo(ResourceFolderType folderType) {
        return (folderType == ResourceFolderType.NAVIGATION || folderType == ResourceFolderType.LAYOUT);
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(SdkConstants.TAG_ACTIVITY, SdkConstants.TAG_NAVIGATION,Constants.TAG_FRAGMENT,SdkConstants.TAG_ACTION,Constants.TAG_WIDGET);
    }

    @Override
    public Collection<String> getApplicableAttributes() {
        return Collections.singleton(SdkConstants.ATTR_NAV_GRAPH);
    }

    @Override
    public void afterCheckRootProject(Context context) {
        if(mWidgets == null || scope_map == null) return;
        for(NavWidgetDao widgetDao : mWidgets) {
            String id = widgetDao.getWidgetId();
            Location loc = widgetDao.getLocation();
            String src = widgetDao.getSource();
            String value = widgetDao.getValue();
            List<String> argVars = widgetDao.getParmArgs();
            List<String> screenArgs = scope_map.get(src);
            for(String key : scope_map.keySet()) {
                if(key.endsWith(Constants.SEPARATOR + src) ||
                        isKeyEndsWithNestedSynonym(key, src) ||
                        isKeyEndsWithActivitySynonym(key, src)) {
                    List<String> args = scope_map.get(key);
                    args.addAll(screenArgs);
                    if(value != null && !args.contains(value)) {
                        context.report(INVALID_WIDGET_INITIALIZATION,loc,
                                value + " used to initialize " + id +
                                        " may not be in scope. Make sure its part of all incoming transitions");
                    }
                    else if(argVars != null) {
                        for(String argVar : argVars) {
                            if(!args.contains(argVar)) {
                                context.report(INVALID_WIDGET_INITIALIZATION,loc,
                                        argVar + " in <fun> used to initialize " + id +
                                                " may not be in scope. Make sure its part of all incoming transitions");
                            }
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
        else if(name.equals(Constants.TAG_FRAGMENT)) {
            addToScope(element,Constants.ATTR_ID);
        }
        else if(name.equals(SdkConstants.TAG_ACTION)) {
            addToScope(element,Constants.NAV_ACTION_DEST);
        }
        else if(name.equals(Constants.TAG_WIDGET)
                && element.hasAttribute(Constants.NAV_ATTR_WID)
                && element.getAttribute(Constants.NAV_ATTR_WIDGET_VALUE).startsWith(Constants.NAV_PREFIX_VAR)) {
            Element src = (Element) element.getParentNode();
            if(src.hasAttribute(Constants.ATTR_ID)) {
                if(mWidgets == null) {
                    mWidgets = new ArrayList<>();
                }
                NavWidgetDao navWidgetDao = new NavWidgetDao(context.getLocation(element),
                        element.getAttribute(Constants.NAV_ATTR_WID));
                navWidgetDao.setSource(src.getAttribute(Constants.ATTR_ID).split("/")[1]);
                navWidgetDao.setValue(element.getAttribute(Constants.NAV_ATTR_WIDGET_VALUE).split("/")[1]);
                navWidgetDao.setParmArgs(null);
                mWidgets.add(navWidgetDao);
            }
        }
        else if(name.equals(Constants.TAG_WIDGET)
                && element.hasAttribute(Constants.NAV_ATTR_WID)
                && element.getAttribute(Constants.NAV_ATTR_WIDGET_VALUE).startsWith(Constants.NAV_PREFIX_FUN)) {

            Element funNode = (Element) element.getElementsByTagName(Constants.TAG_FUN).item(0);
            String v = element.getAttribute(Constants.NAV_ATTR_WIDGET_VALUE).split("/")[1];
            if(funNode != null) {
                if(!funNode.getAttribute(Constants.NAV_ATTR_NAME).equals(v)) {
                    context.report(INVALID_WIDGET_INITIALIZATION,context.getLocation(element),
                            "could not recognize " + v);
                }
                Element src = (Element) element.getParentNode();
                if(src.hasAttribute(Constants.ATTR_ID)) {
                    if(mWidgets == null) {
                        mWidgets = new ArrayList<>();
                    }
                    NavWidgetDao navWidgetDao = new NavWidgetDao(context.getLocation(funNode),
                            element.getAttribute(Constants.NAV_ATTR_WID));
                    navWidgetDao.setSource(src.getAttribute(Constants.ATTR_ID).split("/")[1]);
                    NodeList parmNodes = funNode.getElementsByTagName(Constants.TAG_PARM);
                    if(parmNodes.getLength() > 0) {
                        List<String> parmArgs = new ArrayList<>();
                        for(int i = 0; i < parmNodes.getLength(); i++) {
                            Element parmNode = (Element) parmNodes.item(i);
                            String parmArg = parmNode.getAttribute(Constants.NAV_ATTR_PARM_ARG);
                            if(parmArg.startsWith(Constants.NAV_PREFIX_VAR)) parmArgs.add(parmArg.split("/")[1]);
                        }
                        navWidgetDao.setParmArgs(parmArgs);
                    }
                    navWidgetDao.setValue(null);
                    mWidgets.add(navWidgetDao);
                }
            }
            else {
                context.report(INVALID_WIDGET_INITIALIZATION,context.getLocation(element),
                        v + " is not found. Make sure it is defined.");
            }
        }
    }

    private void addToScope(Element element, String attrName) {
        NodeList arguments = element.getElementsByTagName(Constants.TAG_ARGUMENT);
        List<String> argNames;
        String key;
        if(element.getTagName().equals(SdkConstants.TAG_ACTION)) {
            Element parentNode = (Element) element.getParentNode();
            String src = parentNode.getAttribute(Constants.ATTR_ID).split("/")[1];
            key = src + Constants.SEPARATOR + element.getAttribute(attrName).split("/")[1];
            argNames = UsefulMethods.getTagValues(arguments,Constants.NAV_ATTR_NAME,null);

        }
        else {
            key = element.getAttribute(attrName).split("/")[1];
            argNames = UsefulMethods.getTagValues(arguments,Constants.NAV_ATTR_NAME,
                    Collections.singletonList(SdkConstants.TAG_ACTION));
        }

        if(scope_map == null) scope_map = new HashMap<>();
        if(scope_map.get(key) == null) {
            scope_map.put(key,argNames);
        }
        else {
            List<String> tmp = scope_map.get(key);
            tmp.addAll(argNames);
            scope_map.put(key,tmp);
        }
    }

    private Boolean isKeyEndsWithNestedSynonym(String key, String src) {
        return (syn_map != null && syn_map.get(src) != null
                && key.endsWith(Constants.SEPARATOR + syn_map.get(src)));
    }

    private Boolean isKeyEndsWithActivitySynonym(String key, String src) {
        return (graph_startDest_map != null &&
                graph_layout_map != null &&
                activity_layout_map != null &&
                graph_startDest_map.get(src) != null &&
                graph_layout_map.get(graph_startDest_map.get(src)) != null &&
                activity_layout_map.get(graph_layout_map.get(graph_startDest_map.get(src))) != null &&
                key.endsWith(Constants.SEPARATOR +
                        activity_layout_map
                                .get(graph_layout_map.get(graph_startDest_map.get(src)))));
    }
}
