package kairos.leeway.app.guiUtil;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import java.awt.*;
import java.awt.event.InputEvent;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MouseClickerGUI extends Application implements NativeKeyListener {



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
            GlobalScreen.addNativeKeyListener(this);
        } catch (NativeHookException e) {
            e.printStackTrace();
        }

        // 表格
        table = new TableView<>(points);
        table.setEditable(true);

        TableColumn<ClickPoint, Integer> xCol = new TableColumn<>("X");
        xCol.setCellValueFactory(data -> data.getValue().xProperty().asObject());
        xCol.setCellFactory(col -> createEditingCell(new javafx.util.converter.IntegerStringConverter()));
        xCol.setOnEditCommit(event -> event.getRowValue().setX(event.getNewValue()));

        TableColumn<ClickPoint, Integer> yCol = new TableColumn<>("Y");
        yCol.setCellValueFactory(data -> data.getValue().yProperty().asObject());
        yCol.setCellFactory(col -> createEditingCell(new javafx.util.converter.IntegerStringConverter()));
        yCol.setOnEditCommit(event -> event.getRowValue().setY(event.getNewValue()));

        TableColumn<ClickPoint, String> buttonCol = new TableColumn<>("按键");
        buttonCol.setCellValueFactory(data -> data.getValue().buttonProperty());
        buttonCol.setCellFactory(ComboBoxTableCell.forTableColumn("LEFT", "RIGHT"));
        buttonCol.setOnEditCommit(event -> event.getRowValue().setButton(event.getNewValue()));

        TableColumn<ClickPoint, Integer> moveDelayCol = new TableColumn<>("移动延时(ms)");
        moveDelayCol.setCellValueFactory(data -> data.getValue().moveDelayProperty().asObject());
        moveDelayCol.setCellFactory(col -> createEditingCell(new javafx.util.converter.IntegerStringConverter()));
        moveDelayCol.setOnEditCommit(event -> event.getRowValue().setMoveDelay(event.getNewValue()));

        TableColumn<ClickPoint, Integer> clickDelayCol = new TableColumn<>("点击延时(ms)");
        clickDelayCol.setCellValueFactory(data -> data.getValue().clickDelayProperty().asObject());
        clickDelayCol.setCellFactory(col -> createEditingCell(new javafx.util.converter.IntegerStringConverter()));
        clickDelayCol.setOnEditCommit(event -> event.getRowValue().setClickDelay(event.getNewValue()));

        TableColumn<ClickPoint, Boolean> doClickCol = new TableColumn<>("是否点击");
        doClickCol.setCellValueFactory(data -> data.getValue().doClickProperty());
        doClickCol.setCellFactory(CheckBoxTableCell.forTableColumn(doClickCol));
        doClickCol.setEditable(true);

        table.getColumns().addAll(xCol, yCol, buttonCol, moveDelayCol, clickDelayCol, doClickCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Label recordLabel = new Label("F2 记录鼠标位置");

        Button runBtn = new Button("执行/停止方案(F8)");
        Button saveBtn = new Button("保存方案");
        Button loadBtn = new Button("加载方案");
        Button clearBtn = new Button("清空方案");
        Button deleteBtn = new Button("删除选中");
        deleteBtn.setOnAction(e -> {
            ClickPoint selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                points.remove(selected);
                appendLog("删除点: X=" + selected.getX() + " Y=" + selected.getY());
            } else {
                appendLog("未选择任何点进行删除");
            }
        });
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

        ToolBar toolbar = new ToolBar(
                recordLabel, runBtn, saveBtn, loadBtn,deleteBtn, clearBtn, new Separator(),
                new Label("循环模式:"), loopModeBox, new Separator(),
                new Label("值:"), loopCountInput, endTimeInput,
                new Label("循环间隔(ms):"), loopDelayField
        );

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(150);

        runBtn.setOnAction(e -> runScheme());
        saveBtn.setOnAction(e -> saveScheme(stage));
        loadBtn.setOnAction(e -> loadScheme(stage));
        clearBtn.setOnAction(e -> points.clear());

        VBox root = new VBox(1, table, toolbar, logArea);
        stage.setScene(new Scene(root, 1350, 500));
        stage.setTitle("鼠标点击器");
        stage.show();
    }
    // 工具方法：失焦时也提交
    private <T> TextFieldTableCell<ClickPoint, T> createEditingCell(javafx.util.StringConverter<T> converter) {
        TextFieldTableCell<ClickPoint, T> cell = new TextFieldTableCell(converter) {
            @Override
            public void startEdit() {
                super.startEdit();
                if (getGraphic() instanceof TextField) {
                    TextField textField = (TextField) getGraphic();
                    textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                        if (!isNowFocused) {
                            // 失去焦点时强制提交
                            commitEdit(converter.fromString(textField.getText()));
                        }
                    });
                }
            }
        };
        return cell;
    }
    @Override
    public void stop() throws Exception {
        GlobalScreen.unregisterNativeHook();
        super.stop();
    }

    // ====== 全局快捷键 ======
    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if (e.getKeyCode() == NativeKeyEvent.VC_F2) {
            Point p = MouseInfo.getPointerInfo().getLocation();
            Platform.runLater(() -> points.add(new ClickPoint(p.x, p.y, "LEFT", 0, 100, true)));
            appendLog("添加点: X=" + p.x + " Y=" + p.y);
        } else if (e.getKeyCode() == NativeKeyEvent.VC_F3) {
            Platform.runLater(points::clear);
            appendLog("清空所有点");
        } else if (e.getKeyCode() == NativeKeyEvent.VC_F8) {
            if (!running) runScheme();
            else {
                running = false;
                appendLog("停止执行方案");
            }
        }
    }
    @Override public void nativeKeyReleased(NativeKeyEvent e) {}
    @Override public void nativeKeyTyped(NativeKeyEvent e) {}

    // ====== 保存/加载方案 ======
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

    // ====== 执行方案 ======
    private void runScheme() {
        if (points.isEmpty()) return;
        running = true;

        int loopCount = 0;
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
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    System.out.println(endTimeText);
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

                        if (p.getMoveDelay() > 0) Thread.sleep(p.getMoveDelay());
                        robot.mouseMove(p.getX(), p.getY());

                        if (p.isDoClick()) {
                            if (p.getClickDelay() > 0) Thread.sleep(p.getClickDelay());
                            int mask = p.getButton().equalsIgnoreCase("LEFT") ?
                                    InputEvent.BUTTON1_DOWN_MASK : InputEvent.BUTTON3_DOWN_MASK;
                            robot.mousePress(mask);
                            robot.mouseRelease(mask);
                        }

                        appendLog(String.format("移动到点 X=%d Y=%d 按键=%s 点击=%s 移动延时=%d 点击延时=%d",
                                p.getX(), p.getY(), p.getButton(), p.isDoClick(),
                                p.getMoveDelay(), p.getClickDelay()));
                    }

                    currentLoop++;
                    if (finalLoopCount > 0 && currentLoop >= finalLoopCount) break;
                    if (finalEndMillis > 0 && System.currentTimeMillis() >= finalEndMillis) break;
                    if (finalLoopDelay > 0) Thread.sleep(finalLoopDelay);
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

    // ====== 日志输出 ======
    private void appendLog(String text) {
        Platform.runLater(() -> {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            logArea.appendText("[" + timestamp + "] " + text + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    // ====== 屏蔽 JNativeHook 日志 ======
    private void disableJNativeHookLogger() {
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);
    }
}
