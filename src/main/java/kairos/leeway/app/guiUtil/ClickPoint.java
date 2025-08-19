package kairos.leeway.app.guiUtil;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ClickPoint {
    private final IntegerProperty x = new SimpleIntegerProperty();
    private final IntegerProperty y = new SimpleIntegerProperty();
    private final StringProperty button = new SimpleStringProperty();
    private final IntegerProperty delay = new SimpleIntegerProperty();

    public ClickPoint(int x, int y, String button, int delay) {
        this.x.set(x);
        this.y.set(y);
        this.button.set(button);
        this.delay.set(delay);
    }

    public int getX() { return x.get(); }
    public void setX(int value) { x.set(value); }
    public IntegerProperty xProperty() { return x; }

    public int getY() { return y.get(); }
    public void setY(int value) { y.set(value); }
    public IntegerProperty yProperty() { return y; }

    public String getButton() { return button.get(); }
    public void setButton(String value) { button.set(value); }
    public StringProperty buttonProperty() { return button; }

    public int getDelay() { return delay.get(); }
    public void setDelay(int value) { delay.set(value); }
    public IntegerProperty delayProperty() { return delay; }

    @Override
    public String toString() {
        return "ClickPoint{" +
                "x=" + getX() +
                ", y=" + getY() +
                ", button='" + getButton() + '\'' +
                ", delay=" + getDelay() +
                '}';
    }
}
