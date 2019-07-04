package androidx.naigation.checks.dao;

import com.android.tools.lint.detector.api.Location;

import java.util.List;

public class NavWidgetDao {
    private Location location;
    private String layout;
    private String widgetId;
    private String source;
    private String value;
    private List<String> parmArgs;

    public NavWidgetDao(Location location, String widgetId) {
        this.widgetId = widgetId;
        this.location = location;
    }

    public NavWidgetDao(Location location, String layout, String widgetId) {
        this.location = location;
        this.layout = layout;
        this.widgetId = widgetId;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setParmArgs(List<String> parmArgs) {
        this.parmArgs = parmArgs;
    }

    public Location getLocation() {
        return location;
    }

    public String getLayout() {
        return layout;
    }

    public String getWidgetId() {
        return widgetId;
    }

    public String getSource() {
        return source;
    }

    public String getValue() {
        return value;
    }

    public List<String> getParmArgs() {
        return parmArgs;
    }
}
