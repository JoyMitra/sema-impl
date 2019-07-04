package androidx.naigation.checks;

import com.android.SdkConstants;
import com.android.resources.ResourceFolderType;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class NavParmDetector extends ResourceXmlDetector {

    public static final Issue PARM_INVALID_FUN_REF = Issue.create("ParmInvalidFunRef",
            "Reference to Fun in a parm should be defined within parm",
            "Parm depends on Fun but the Fun is not defined anywhere.",
            Category.CORRECTNESS, 10, Severity.ERROR,
            new Implementation(NavParmDetector.class, Scope.RESOURCE_FILE_SCOPE));

    public NavParmDetector() {}

    @Override
    public boolean appliesTo(ResourceFolderType folderType) {
        return folderType == ResourceFolderType.NAVIGATION;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Collections.singleton(Constants.TAG_PARM);
    }

    @Override
    public void visitElement(XmlContext context, Element element) {
        String value = element.getAttribute(Constants.NAV_ATTR_PARM_ARG);
        if(value.startsWith(Constants.NAV_PREFIX_FUN)) {
            String funId = value.split("/")[1];
            NodeList fundefs = element.getElementsByTagName(Constants.TAG_FUN);
            if(fundefs.getLength() == 0)
                context.report(PARM_INVALID_FUN_REF,context.getLocation(element),"Missing <fun>");
            else {
                Element eFun = (Element) fundefs.item(0);
                if(!eFun.getAttribute(Constants.NAV_ATTR_NAME).equals(funId))
                    context.report(PARM_INVALID_FUN_REF,context.getLocation(element),funId + "does not exist");
            }
        }
    }
}
