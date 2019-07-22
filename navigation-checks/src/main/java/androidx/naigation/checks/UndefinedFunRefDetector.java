package androidx.naigation.checks;

import androidx.naigation.checks.util.Constants;

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

public class UndefinedFunRefDetector extends ResourceXmlDetector {

    public static final Issue UNDEFINED_FUN_REF = Issue.create("UndefinedFunRef",
            "Reference to undefined <fun>",
            "All references to fun should be defined in a child <fun> tag",
            Category.LINT, 10, Severity.ERROR,
            new Implementation(UndefinedFunRefDetector.class, Scope.RESOURCE_FILE_SCOPE));

    public UndefinedFunRefDetector() {}

    @Override
    public boolean appliesTo(ResourceFolderType folderType) {
        return folderType == ResourceFolderType.NAVIGATION;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(Constants.TAG_PARM,Constants.TAG_ARGUMENT);
    }

    @Override
    public void visitElement(XmlContext context, Element element) {
        String name = element.getTagName();
        String value = "";
        if(name.equals(Constants.TAG_PARM))
            value = element.getAttribute(Constants.NAV_ATTR_PARM_ARG);
        else if(name.equals(Constants.TAG_ARGUMENT))
            value = element.getAttribute(Constants.NAV_ATTR_VAL_ARG);

        if(value.startsWith(Constants.NAV_PREFIX_FUN)) {
            String funId = value.split("/")[1];
            NodeList fundefs = element.getElementsByTagName(Constants.TAG_FUN);
            if(fundefs.getLength() == 0)
                context.report(UNDEFINED_FUN_REF,context.getLocation(element),"Missing <fun> definition");
            else {
                Element eFun = (Element) fundefs.item(0);
                if(!eFun.getAttribute(Constants.NAV_ATTR_NAME).equals(funId))
                    context.report(UNDEFINED_FUN_REF,context.getLocation(element),funId + "does not exist");
            }
        }
    }
}
