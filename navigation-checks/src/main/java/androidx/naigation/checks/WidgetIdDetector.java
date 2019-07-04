package androidx.naigation.checks;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.ddmlib.Log;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.jar.Attributes;

public class WidgetIdDetector extends ResourceXmlDetector {

    private static final String ATTR_NAV_ACTION_WIDGET = "widgetOn";
    private static final String ATTR_NAV_WID = "wid";

    public static final Issue INVALID_WIDGET_ID = Issue.create("InvalidWidget",
            "onWidget should be an ID",
            "The value of the attribute must be an ID corresponding to the source screen",
            Category.CORRECTNESS, 10, Severity.ERROR,
            new Implementation(WidgetIdDetector.class, Scope.RESOURCE_FILE_SCOPE));

    public WidgetIdDetector() {

    }

    @Override
    public Collection<String> getApplicableAttributes() {
        return Arrays.asList(ATTR_NAV_ACTION_WIDGET,ATTR_NAV_WID);
    }

    @Override
    public boolean appliesTo(ResourceFolderType folder) {
        return true;
    }

    @Override
    public void visitAttribute(@NonNull XmlContext xmlContext, @NonNull Attr attr) {
        String value = attr.getValue();
        if(!value.startsWith(SdkConstants.ID_PREFIX)) {
            xmlContext.report(INVALID_WIDGET_ID,xmlContext.getLocation(attr)
                    ,"value in " + attr.getName() + " must start with " + SdkConstants.ID_PREFIX);
        }
    }
}
