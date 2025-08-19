package kairos.leeway.app.guiUtil;

import java.util.ArrayList;
import java.util.List;

// MouseScheme.java
public class MouseScheme {
    private String name;
    private List<ClickPoint> points; // 改为 List<ClickPoint>

    public MouseScheme() {
        points = new ArrayList<>();
    }

    public MouseScheme(String name, List<ClickPoint> points) {
        this.name = name;
        this.points = points;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<ClickPoint> getPoints() { return points; }
    public void setPoints(List<ClickPoint> points) { this.points = points; }

    @Override
    public String toString() { return name; }
}
