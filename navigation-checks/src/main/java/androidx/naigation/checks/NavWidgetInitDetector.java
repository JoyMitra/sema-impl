package androidx.naigation.checks;

import androidx.naigation.checks.dao.NavWidgetDao;

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

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class NavWidgetInitDetector extends ResourceXmlDetector {

    private HashMap<String,List<String>> scope_map = null;
    private List<NavWidgetDao> mWidgets = null;

    public static final Issue INVALID_WIDGET_INITIALIZATION = Issue.create("InvalidWidgetInit",
            "Variable used to initialize widget is not in scope",
            "Variables used to initialize a widget must be provided as an <argument> to the source screen",
            Category.CORRECTNESS, 10,Severity.ERROR,
            new Implementation(NavWidgetInitDetector.class, Scope.RESOURCE_FILE_SCOPE));

    public NavWidgetInitDetector() {

    }

    @Override
    public boolean appliesTo(ResourceFolderType folderType) {
        return folderType == ResourceFolderType.NAVIGATION;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(Constants.TAG_FRAGMENT,SdkConstants.TAG_ACTION,Constants.TAG_WIDGET);
    }

    @Override
    public void afterCheckFile(Context context) {
        if(mWidgets == null) return;
        for(NavWidgetDao widgetDao : mWidgets) {
            String id = widgetDao.getWidgetId();
            Location loc = widgetDao.getLocation();
            String src = widgetDao.getSource();
            String value = widgetDao.getValue();
            List<String> argVars = widgetDao.getParmArgs();
            List<String> args = scope_map.get(src);
            if(value != null && !args.contains(value)) {
                context.report(INVALID_WIDGET_INITIALIZATION,loc,
                        value + " used to initialize " + id + "is not in scope");
            }
            else if(argVars != null) {
                for(String argVar : argVars) {
                    if(!args.contains(argVar)) {
                        context.report(INVALID_WIDGET_INITIALIZATION,loc,
                                argVar + " in <fun> used to initialize " + id + " is not in scope");
                    }
                }
            }
        }
    }

    @Override
    public void visitElement(XmlContext context, Element element) {
        String name = element.getTagName();
        if(name.equals(Constants.TAG_FRAGMENT)) {
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
    }

    private void addToScope(Element element, String attrName) {
        NodeList arguments = element.getElementsByTagName(Constants.TAG_ARGUMENT);
        List<String> argNames;
        String key = element.getAttribute(attrName).split("/")[1];
        if(scope_map == null) scope_map = new HashMap<>();
        if(scope_map.get(key) == null) {
            argNames = new ArrayList<>();
            for(int i = 0; i < arguments.getLength();i++) {
                Element e = (Element) arguments.item(i);
                argNames.add(e.getAttribute(Constants.NAV_ATTR_NAME));
            }
            scope_map.put(key,argNames);
        }
        else {
            argNames = scope_map.get(key);
            for(int i = 0; i < arguments.getLength();i++) {
                Element e = (Element) arguments.item(i);
                argNames.add(e.getAttribute(Constants.NAV_ATTR_NAME));
            }
            scope_map.put(key,argNames);
        }
    }
}
