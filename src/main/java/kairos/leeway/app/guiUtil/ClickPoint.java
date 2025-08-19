package kairos.leeway.app.guiUtil;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

// ====== ClickPoint 类 ======
public class ClickPoint {
    private final IntegerProperty x = new SimpleIntegerProperty();
    private final IntegerProperty y = new SimpleIntegerProperty();
    private final StringProperty button = new SimpleStringProperty("LEFT");
    private final IntegerProperty moveDelay = new SimpleIntegerProperty(0);
    private final IntegerProperty clickDelay = new SimpleIntegerProperty(100);
    private final BooleanProperty doClick = new SimpleBooleanProperty(true);

    public ClickPoint(int x, int y, String button, int moveDelay, int clickDelay, boolean doClick) {
        this.x.set(x);
        this.y.set(y);
        this.button.set(button);
        this.moveDelay.set(moveDelay);
        this.clickDelay.set(clickDelay);
        this.doClick.set(doClick);
    }

    public IntegerProperty xProperty() { return x; }
    public IntegerProperty yProperty() { return y; }
    public StringProperty buttonProperty() { return button; }
    public IntegerProperty moveDelayProperty() { return moveDelay; }
    public IntegerProperty clickDelayProperty() { return clickDelay; }
    public BooleanProperty doClickProperty() { return doClick; }

    public int getX() { return x.get(); }
    public int getY() { return y.get(); }
    public String getButton() { return button.get(); }
    public int getMoveDelay() { return moveDelay.get(); }
    public int getClickDelay() { return clickDelay.get(); }
    public boolean isDoClick() { return doClick.get(); }

    public void setX(int val) { x.set(val); }
    public void setY(int val) { y.set(val); }
    public void setButton(String val) { button.set(val); }
    public void setMoveDelay(int val) { moveDelay.set(val); }
    public void setClickDelay(int val) { clickDelay.set(val); }
    public void setDoClick(boolean val) { doClick.set(val); }
}
