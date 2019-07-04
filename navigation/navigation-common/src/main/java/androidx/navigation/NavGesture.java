package androidx.navigation;

public final class NavGesture {
    private final int mWidgetId;
    private final int mGesture;

    /**
     * Creates a new NavGesture that will trigger the corresponding action.
     *
     * @param mWidgetId the ID of the widget that will trigger the corresponding action.
     *
     * @param mGesture the gesture on the widget ID.
     */
    public NavGesture(int mWidgetId, int mGesture) {
        this.mWidgetId = mWidgetId;
        this.mGesture = mGesture;
    }

    public int getWidgetId() {
        return mWidgetId;
    }

    public int getGesture() {
        return mGesture;
    }
}
