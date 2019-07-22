package androidx.naigation.checks.dao;

import com.android.tools.lint.detector.api.Location;

import java.util.List;

public class FunDao {
    private Location location;
    String name;
    private List<String> funArgs;

    public FunDao(Location location, String name,List<String> funArgs) {
        this.location = location;
        this.name = name;
        this.funArgs = funArgs;
    }

    public List<String> getFunArgs() {
        return funArgs;
    }

    public Location getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }
}
