package androidx.naigation.checks;

import androidx.naigation.checks.dao.NavWidgetDao;

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
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponent;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NavWidgetReferenceDetector extends ResourceXmlDetector {

    private List<NavWidgetDao> widgetRefs = new ArrayList<>();
    private Map<String,String> widgetMap = new HashMap<>();

    public static final Issue INVALID_NAV_WIDGET_REF = Issue.create("InvalidNavWidgetRef"
            ,"Checks if the action is performed on a valid widget"
            ,"Validates if the value in the widgetOn attribute of action in a navigation graph corresponds to a widget in the source layout from which the action originates"
            , Category.CORRECTNESS,10, Severity.ERROR
            ,new Implementation(NavWidgetReferenceDetector.class, Scope.ALL_RESOURCES_SCOPE));


    public NavWidgetReferenceDetector() {

    }

    @Override
    public boolean appliesTo(ResourceFolderType folderType) {
        return (folderType == ResourceFolderType.LAYOUT || folderType == ResourceFolderType.NAVIGATION);
    }

    @Override
    public Collection<String> getApplicableElements(){
        return Arrays.asList(SdkConstants.TAG_ACTION,Constants.TAG_WIDGET,Constants.TAG_BUTTON,
                Constants.TAG_TEXTVIEW,Constants.TAG_EDITTEXT,Constants.TAG_IMAGEVIW,
                Constants.TAG_LISTVIEW,Constants.TAG_WEBVIEW);
    }

    @Override
    public void afterCheckEachProject(Context context) {
        for(NavWidgetDao navWidgetDao : widgetRefs) {
            String wid = navWidgetDao.getWidgetId();
            String layoutRef = navWidgetDao.getLayout();
            String expectedRef = widgetMap.get(wid);
            if(!layoutRef.equals(expectedRef)) {
                context.report(INVALID_NAV_WIDGET_REF,navWidgetDao.getLocation(),"could not find " + wid + " in any layout file");
            }
        }
    }
    @Override
    public void visitElement(XmlContext context, Element element) {
        String name = element.getTagName();
        if(name.equals(SdkConstants.TAG_ACTION)) {
            processAttrOfInterest(context,element,Constants.ATTR_NAV_ACTION_WIDGET);
        }
        else if(name.equals(Constants.TAG_WIDGET)) {
            processAttrOfInterest(context,element,Constants.NAV_ATTR_WID);
        }
        else {
            String fileNm = context.file.getName();
            String val = element.getAttribute(Constants.ATTR_ID);
            if(val != null && val.contains("/"))
                widgetMap.put(val.split("/")[1],fileNm);
        }
    }

    private void processAttrOfInterest(XmlContext context, Element element, String attrName) {
        try {
            String a = element.getAttribute(attrName);
            if(a != null && a.contains(SdkConstants.ID_PREFIX)){
                String wid = a.split("/")[1];
                Element e = (Element) element.getParentNode();
                String layoutId = e.getAttribute(Constants.ATTR_NAV_PARENT_LAYOUT);
                if(layoutId != null && layoutId.contains(SdkConstants.LAYOUT_RESOURCE_PREFIX)) {
                    String layoutFileNm = layoutId.split("/")[1] + ".xml";
                    widgetRefs.add(new NavWidgetDao(context.getLocation(element),layoutFileNm,wid));
                }
                else {
                    context.report(INVALID_NAV_WIDGET_REF, context.getLocation(e), "Missing layout attribute on " + e.getTagName());
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println(e.getLocalizedMessage());
            System.out.println("attrId = " + element.getAttribute(attrName));
        }
    }

}

