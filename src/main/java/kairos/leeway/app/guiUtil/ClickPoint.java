package kairos.leeway.app.guiUtil;

import javafx.beans.property.*;

public class ClickPoint {
    private final IntegerProperty x = new SimpleIntegerProperty();
    private final IntegerProperty y = new SimpleIntegerProperty();
    private final StringProperty button = new SimpleStringProperty("LEFT");
    private final IntegerProperty moveDelay = new SimpleIntegerProperty();
    private final IntegerProperty keepDelay = new SimpleIntegerProperty();
    private final IntegerProperty clickDelay = new SimpleIntegerProperty();
    private final BooleanProperty doClick = new SimpleBooleanProperty(true);
    private final BooleanProperty doMove = new SimpleBooleanProperty(true); // 新增：是否挪动
    private final StringProperty remark = new SimpleStringProperty("");
    public String getRemark() { return remark.get(); }
    public void setRemark(String value) { remark.set(value); }
    public StringProperty remarkProperty() { return remark; }
    public ClickPoint(int x, int y, String button, int moveDelay, int keepDelay, int clickDelay, boolean doClick, boolean doMove, String remark) {
        this.x.set(x);
        this.y.set(y);
        this.button.set(button);
        this.moveDelay.set(moveDelay);
        this.keepDelay.set(keepDelay);
        this.clickDelay.set(clickDelay);
        this.doClick.set(doClick);
        this.doMove.set(doMove);
        this.remark.set(remark);
    }

    // getter/setter
    public int getX() { return x.get(); }
    public void setX(int v) { x.set(v); }
    public IntegerProperty xProperty() { return x; }

    public int getY() { return y.get(); }
    public void setY(int v) { y.set(v); }
    public IntegerProperty yProperty() { return y; }

    public String getButton() { return button.get(); }
    public void setButton(String v) { button.set(v); }
    public StringProperty buttonProperty() { return button; }

    public int getMoveDelay() { return moveDelay.get(); }
    public void setMoveDelay(int v) { moveDelay.set(v); }
    public IntegerProperty moveDelayProperty() { return moveDelay; }

    public int getKeepDelay() { return keepDelay.get(); }
    public void setKeepDelay(int v) { keepDelay.set(v); }
    public IntegerProperty keepDelayProperty() { return keepDelay; }

    public int getClickDelay() { return clickDelay.get(); }
    public void setClickDelay(int v) { clickDelay.set(v); }
    public IntegerProperty clickDelayProperty() { return clickDelay; }

    public boolean isDoClick() { return doClick.get(); }
    public void setDoClick(boolean v) { doClick.set(v); }
    public BooleanProperty doClickProperty() { return doClick; }

    public boolean isDoMove() { return doMove.get(); }
    public void setDoMove(boolean v) { doMove.set(v); }
    public BooleanProperty doMoveProperty() { return doMove; }
}
