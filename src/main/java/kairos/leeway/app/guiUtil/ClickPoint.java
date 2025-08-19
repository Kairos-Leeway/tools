// ClickPoint.java
package kairos.leeway.app.guiUtil;

import javafx.beans.property.*;

public class ClickPoint {
    private final IntegerProperty x = new SimpleIntegerProperty();
    private final IntegerProperty y = new SimpleIntegerProperty();
    private final StringProperty button = new SimpleStringProperty("LEFT");
    private final IntegerProperty moveDelay = new SimpleIntegerProperty(500);
    private final IntegerProperty keepDelay = new SimpleIntegerProperty(100);
    private final IntegerProperty clickDelay = new SimpleIntegerProperty(200);
    private final BooleanProperty doClick = new SimpleBooleanProperty(true);

    public ClickPoint() {}
    public ClickPoint(int x, int y, String button, int moveDelay, int keepDelay, int clickDelay, boolean doClick) {
        this.x.set(x); this.y.set(y); this.button.set(button);
        this.moveDelay.set(moveDelay); this.keepDelay.set(keepDelay);
        this.clickDelay.set(clickDelay); this.doClick.set(doClick);
    }

    public IntegerProperty xProperty() { return x; }
    public IntegerProperty yProperty() { return y; }
    public StringProperty buttonProperty() { return button; }
    public IntegerProperty moveDelayProperty() { return moveDelay; }
    public IntegerProperty keepDelayProperty() { return keepDelay; }
    public IntegerProperty clickDelayProperty() { return clickDelay; }
    public BooleanProperty doClickProperty() { return doClick; }

    public int getX() { return x.get(); }
    public int getY() { return y.get(); }
    public String getButton() { return button.get(); }
    public int getMoveDelay() { return moveDelay.get(); }
    public int getKeepDelay() { return keepDelay.get(); }
    public int getClickDelay() { return clickDelay.get(); }
    public boolean isDoClick() { return doClick.get(); }

    public void setX(int val) { x.set(val); }
    public void setY(int val) { y.set(val); }
    public void setButton(String val) { button.set(val); }
    public void setMoveDelay(int val) { moveDelay.set(val); }
    public void setKeepDelay(int val) { keepDelay.set(val); }
    public void setClickDelay(int val) { clickDelay.set(val); }
    public void setDoClick(boolean val) { doClick.set(val); }
}
