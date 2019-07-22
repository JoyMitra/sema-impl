package androidx.naigation.checks.dao;

import com.android.tools.lint.detector.api.Location;

import java.util.List;

public class NavConstraintsDao {
    private List<String> constraintArgs;
    private String argVal;
    private String srcId;
    private Location location;

    public NavConstraintsDao(String srcId, List<String> constraintArgs, Location location) {
        this.srcId = srcId;
        this.constraintArgs = constraintArgs;
        this.location = location;
    }

    public NavConstraintsDao(String srcId, String argVal, Location location) {
        this.srcId = srcId;
        this.argVal = argVal;
        this.location = location;
    }

    public List<String> getConstraintArgs() {
        return constraintArgs;
    }

    public String getSrcId() {
        return srcId;
    }

    public Location getLocation() { return location; }

    public String getArgVal() {
        return argVal;
    }
}
