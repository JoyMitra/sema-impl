package androidx.naigation.checks.util;

import com.android.SdkConstants;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class UsefulMethods {

    public static List<String> getTagValues(NodeList nodes, String attrName, List<String> excludeTagNames) {
        List<String> tmpList = new ArrayList<>();
        for(int i = 0; i < nodes.getLength(); i++) {
            Element elem = (Element) nodes.item(i);
            Element parent = (Element) elem.getParentNode();
            if(excludeTagNames == null || !excludeTagNames.contains(parent.getTagName())) {
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
                else if(attrName.equals(Constants.NAV_ATTR_VAL_ARG)) {
                    String value = elem.getAttribute(Constants.NAV_ATTR_VAL_ARG);
                    if(value.startsWith(Constants.NAV_PREFIX_VAR) || value.startsWith(SdkConstants.ID_PREFIX)) {
                        tmpList.add(value.split("/")[1]);
                    }
                }
            }
        }
        return tmpList;
    }

    public static void print(String TAG, Object msg) {
        System.out.println(TAG + " : " + msg);
    }
}
