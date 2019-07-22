package androidx.naigation.checks;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.ApiKt;
import com.android.tools.lint.detector.api.Issue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NavIssueRegistry extends IssueRegistry {
    private static final List<Issue> sIssues;

    static {
        List<Issue> issues = new ArrayList<>();
        issues.add(WidgetIdDetector.INVALID_WIDGET_ID);
        issues.add(NavWidgetReferenceDetector.INVALID_NAV_WIDGET_REF);
        issues.add(NavWidgetInitDetector.INVALID_WIDGET_INITIALIZATION);
        issues.add(UndefinedFunRefDetector.UNDEFINED_FUN_REF);
        issues.add(NavActionConstraintsScopeDetector.ACTION_CONSTRAINT_NOT_IN_SCOPE);
        issues.add(NavActionArgumentScopeDetector.ACTION_ARGUMENT_VALUE_NOT_IN_SCOPE);
        issues.add(NavScreenArgumentScopeDetector.SCREEN_ARGUMENT_VALUE_NOT_IN_SCOPE);
        issues.add(NavDuplicateVariableDetector.NAV_DUPLICATE_VAR);
        issues.add(NavDataInjectionDetector.DATA_INJECTION_SCREEN);
        sIssues = Collections.unmodifiableList(issues);
    }

    @Override
    public List<Issue> getIssues() {
        return sIssues;
    }

    @Override
    public int getApi() {
        return ApiKt.CURRENT_API;
    }
}
