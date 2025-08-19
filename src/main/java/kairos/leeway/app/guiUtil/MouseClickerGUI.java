package kairos.leeway.app.guiUtil;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;

import java.awt.*;
import java.awt.event.InputEvent;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MouseClickerGUI extends Application {

    private final ObservableList<ClickPoint> points = FXCollections.observableArrayList();
    private volatile boolean running = false;

    private TableView<ClickPoint> table;
    private TextField loopCountInput;
    private TextField loopDelayField;
    private TextField endTimeInput;
    private ComboBox<String> loopModeBox;
    private TextArea logArea;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        disableJNativeHookLogger();
        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(new org.jnativehook.keyboard.NativeKeyListener() {
                @Override
                public void nativeKeyPressed(NativeKeyEvent e) {
                    if (e.getKeyCode() == NativeKeyEvent.VC_F2) {
                        Point p = MouseInfo.getPointerInfo().getLocation();
                        Platform.runLater(() -> points.add(new ClickPoint(p.x, p.y, "LEFT", 100, 50)));
                        appendLog("添加点: X=" + p.x + " Y=" + p.y);
                    } else if (e.getKeyCode() == NativeKeyEvent.VC_F3) {
                        Platform.runLater(points::clear);
                        appendLog("清空所有点");
                    } else if (e.getKeyCode() == NativeKeyEvent.VC_F8) {
                        if (!running) runScheme(); else {
                            running = false;
                            appendLog("停止执行方案");
                        }
                    }
                }

                @Override public void nativeKeyReleased(NativeKeyEvent e) {}
                @Override public void nativeKeyTyped(NativeKeyEvent e) {}
            });
        } catch (NativeHookException e) {
            e.printStackTrace();
        }

        // 表格
        table = new TableView<>(points);
        table.setEditable(true);

        TableColumn<ClickPoint, Integer> xCol = new TableColumn<>("X");
        xCol.setCellValueFactory(data -> data.getValue().xProperty().asObject());
        xCol.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.IntegerStringConverter()));
        xCol.setOnEditCommit(event -> event.getRowValue().setX(event.getNewValue()));

        TableColumn<ClickPoint, Integer> yCol = new TableColumn<>("Y");
        yCol.setCellValueFactory(data -> data.getValue().yProperty().asObject());
        yCol.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.IntegerStringConverter()));
        yCol.setOnEditCommit(event -> event.getRowValue().setY(event.getNewValue()));

        TableColumn<ClickPoint, String> buttonCol = new TableColumn<>("按键");
        buttonCol.setCellValueFactory(data -> data.getValue().buttonProperty());
        buttonCol.setCellFactory(ComboBoxTableCell.forTableColumn("LEFT", "RIGHT"));

        TableColumn<ClickPoint, Integer> clickDelayCol = new TableColumn<>("点击延时(ms)");
        clickDelayCol.setCellValueFactory(data -> data.getValue().clickDelayProperty().asObject());
        clickDelayCol.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.IntegerStringConverter()));
        clickDelayCol.setOnEditCommit(event -> event.getRowValue().setClickDelay(event.getNewValue()));

        TableColumn<ClickPoint, Integer> moveDelayCol = new TableColumn<>("移动延时(ms)");
        moveDelayCol.setCellValueFactory(data -> data.getValue().moveDelayProperty().asObject());
        moveDelayCol.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.IntegerStringConverter()));
        moveDelayCol.setOnEditCommit(event -> event.getRowValue().setMoveDelay(event.getNewValue()));

        table.getColumns().addAll(xCol, yCol, buttonCol, clickDelayCol, moveDelayCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Label recordLabel = new Label("F2 记录鼠标位置");

        Button runBtn = new Button("执行/停止方案(F8)");
        Button saveBtn = new Button("保存方案");
        Button loadBtn = new Button("加载方案");
        Button clearBtn = new Button("清空方案");

        loopModeBox = new ComboBox<>();
        loopModeBox.getItems().addAll("循环次数", "结束时间");
        loopModeBox.setValue("循环次数");

        loopCountInput = new TextField("1");
        endTimeInput = new TextField(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        endTimeInput.setVisible(false);
        endTimeInput.setManaged(false);
        loopDelayField = new TextField("1000");

        loopModeBox.setOnAction(e -> {
            String mode = loopModeBox.getValue();
            loopCountInput.setVisible(mode.equals("循环次数"));
            loopCountInput.setManaged(mode.equals("循环次数"));
            endTimeInput.setVisible(mode.equals("结束时间"));
            endTimeInput.setManaged(mode.equals("结束时间"));
        });

        ToolBar toolbar = new ToolBar(recordLabel, runBtn, saveBtn, loadBtn, clearBtn, new Separator(),
                new Label("循环模式:"), loopModeBox, new Separator(),
                new Label("值:"), loopCountInput, endTimeInput,
                new Label("间隔(ms):"), loopDelayField);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(150);

        runBtn.setOnAction(e -> runScheme());
        saveBtn.setOnAction(e -> saveScheme(stage));
        loadBtn.setOnAction(e -> loadScheme(stage));
        clearBtn.setOnAction(e -> points.clear());

        VBox root = new VBox(10, table, toolbar, logArea);
        stage.setScene(new Scene(root, 1250, 500));
        stage.setTitle("鼠标点击器");
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        GlobalScreen.unregisterNativeHook();
        super.stop();
    }

    private void saveScheme(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存方案");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON 文件", "*.json"));
        fileChooser.setInitialFileName("scheme.json");
        File file = fileChooser.showSaveDialog(stage);
        if (file == null) return;

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
            writer.write(JSON.toJSONString(points));
            appendLog("方案已保存到: " + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            appendLog("保存失败: " + e.getMessage());
        }
    }

    private void loadScheme(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("加载方案");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON 文件", "*.json"));
        File file = fileChooser.showOpenDialog(stage);
        if (file == null) return;

        try (Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8")) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[1024];
            int len;
            while ((len = reader.read(buf)) != -1) sb.append(buf, 0, len);
            List<ClickPoint> loaded = JSON.parseObject(sb.toString(), new TypeReference<List<ClickPoint>>() {});
            points.setAll(loaded);
            appendLog("方案已加载: " + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            appendLog("加载失败: " + e.getMessage());
        }
    }

    private void runScheme() {
        if (points.isEmpty()) return;
        running = true;

        int loopCount = 1;
        long endMillis = 0;
        int loopDelay = 1000;

        try {
            String loopDelayText = loopDelayField.getText().trim();
            if (!loopDelayText.isEmpty()) loopDelay = Integer.parseInt(loopDelayText);

            if ("循环次数".equals(loopModeBox.getValue())) {
                String loopCountText = loopCountInput.getText().trim();
                if (!loopCountText.isEmpty()) loopCount = Integer.parseInt(loopCountText);
            } else if ("结束时间".equals(loopModeBox.getValue())) {
                String endTimeText = endTimeInput.getText().trim();
                if (!endTimeText.isEmpty()) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                    Date endDate = sdf.parse(endTimeText);
                    Calendar now = Calendar.getInstance();
                    Calendar endCal = Calendar.getInstance();
                    endCal.setTime(endDate);
                    endCal.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
                    endMillis = endCal.getTimeInMillis();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            appendLog("参数解析错误：" + e.getMessage());
            running = false;
            return;
        }

        int finalLoopCount = loopCount;
        long finalEndMillis = endMillis;
        int finalLoopDelay = loopDelay;

        new Thread(() -> {
            try {
                Robot robot = new Robot();
                int currentLoop = 0;

                while (running) {
                    for (ClickPoint p : points) {
                        if (!running) break;

                        if (p.getMoveDelay() > 0) Thread.sleep(p.getMoveDelay()); // 等待移动延时
                        robot.mouseMove(p.getX(), p.getY());

                        if (p.getClickDelay() > 0) Thread.sleep(p.getClickDelay()); // 等待点击延时
                        int mask = p.getButton().equalsIgnoreCase("LEFT") ? InputEvent.BUTTON1_DOWN_MASK : InputEvent.BUTTON3_DOWN_MASK;
                        robot.mousePress(mask);
                        robot.mouseRelease(mask);

                        appendLog(String.format("点击点 X=%d Y=%d 按键=%s 移动延时=%dms 点击延时=%dms",
                                p.getX(), p.getY(), p.getButton(), p.getMoveDelay(), p.getClickDelay()));

                        Thread.sleep(p.getClickDelay());
                    }

                    currentLoop++;
                    if (finalLoopDelay > 0) Thread.sleep(finalLoopDelay);

                    if (finalLoopCount > 0 && currentLoop >= finalLoopCount) break;
                    if (finalEndMillis > 0 && System.currentTimeMillis() >= finalEndMillis) break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                appendLog("执行异常：" + e.getMessage());
            } finally {
                running = false;
                appendLog("执行结束");
            }
        }).start();
    }

    private void appendLog(String text) {
        Platform.runLater(() -> {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            logArea.appendText("[" + timestamp + "] " + text + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void disableJNativeHookLogger() {
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);
    }

    // ======= ClickPoint 类 =======
    public static class ClickPoint {
        private final IntegerProperty x = new SimpleIntegerProperty();
        private final IntegerProperty y = new SimpleIntegerProperty();
        private final javafx.beans.property.SimpleStringProperty button = new javafx.beans.property.SimpleStringProperty();
        private final IntegerProperty clickDelay = new SimpleIntegerProperty();
        private final IntegerProperty moveDelay = new SimpleIntegerProperty();

        public ClickPoint(int x, int y, String button, int clickDelay, int moveDelay) {
            this.x.set(x);
            this.y.set(y);
            this.button.set(button);
            this.clickDelay.set(clickDelay);
            this.moveDelay.set(moveDelay);
        }

        public int getX() { return x.get(); }
        public void setX(int value) { x.set(value); }
        public IntegerProperty xProperty() { return x; }

        public int getY() { return y.get(); }
        public void setY(int value) { y.set(value); }
        public IntegerProperty yProperty() { return y; }

        public String getButton() { return button.get(); }
        public void setButton(String value) { button.set(value); }
        public javafx.beans.property.SimpleStringProperty buttonProperty() { return button; }

        public int getClickDelay() { return clickDelay.get(); }
        public void setClickDelay(int value) { clickDelay.set(value); }
        public IntegerProperty clickDelayProperty() { return clickDelay; }

        public int getMoveDelay() { return moveDelay.get(); }
        public void setMoveDelay(int value) { moveDelay.set(value); }
        public IntegerProperty moveDelayProperty() { return moveDelay; }
    }
}
