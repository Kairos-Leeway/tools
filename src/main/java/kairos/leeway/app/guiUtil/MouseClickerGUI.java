package kairos.leeway.app.guiUtil;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
    private final ObservableList<MouseScheme> schemes = FXCollections.observableArrayList();
    private volatile boolean running = false;
    private static boolean currentSchemeModified = false;
    private static boolean ignoreSchemeSelectionChange = false;
    private TableView<ClickPoint> table;
    private TextField loopCountInput, loopDelayField, endTimeInput;
    private ComboBox<String> loopModeBox;
    private TextArea logArea;
    private ListView<MouseScheme> schemeListView;

    private File schemesFile = new File("schemes.json");

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

        // 加载方案
        loadAllSchemes();

        schemeListView = new ListView<>(schemes);
        schemeListView.setPrefWidth(180);
        schemeListView.setPrefHeight(400);
        schemeListView.setStyle(
                "-fx-border-color: #ccc; -fx-border-radius: 5; -fx-background-radius: 5;" +
                        "-fx-focus-color: #4CAF50; -fx-faint-focus-color: transparent;"
        );

        schemeListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (ignoreSchemeSelectionChange) return;

            if (oldVal != null && currentSchemeModified) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("保存方案");
                alert.setHeaderText("方案 '" + oldVal.getName() + "' 已修改，是否保存？");

                ButtonType btnSave = new ButtonType("保存");
                ButtonType btnDiscard = new ButtonType("不保存");
                ButtonType btnCancel = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(btnSave, btnDiscard, btnCancel);

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent()) {
                    if (result.get() == btnSave) {
                        oldVal.setPoints(FXCollections.observableArrayList(points));
                        saveAllSchemes();
                        appendLog("方案 '" + oldVal.getName() + "' 保存成功！");
                    } else if (result.get() == btnCancel) {
                        // 使用 runLater 延迟切换
                        Platform.runLater(() -> {
                            ignoreSchemeSelectionChange = true;
                            schemeListView.getSelectionModel().select(oldVal);
                            ignoreSchemeSelectionChange = false;
                        });
                        return;
                    }
                    // btnDiscard 不保存，直接切换即可
                }
            }

            if (newVal != null) {
                points.setAll(FXCollections.observableArrayList(newVal.getPoints()));
                currentSchemeModified = false;
            }
        });
        schemeListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                MouseScheme selected = schemeListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    TextInputDialog dialog = new TextInputDialog(selected.getName());
                    dialog.setTitle("重命名方案");
                    dialog.setHeaderText(null);
                    dialog.setContentText("名称:");
                    dialog.showAndWait().ifPresent(name -> {
                        selected.setName(name);
                        schemeListView.refresh();
                        saveAllSchemes();
                    });
                }
            }
        });
        Button newSchemeBtn = new Button("新建方案");
        Button saveSchemeBtn = new Button("保存方案");
        Button deleteSchemeBtn = new Button("删除方案");

        Button[] sideButtons = {newSchemeBtn, saveSchemeBtn, deleteSchemeBtn};
        for (Button btn : sideButtons) {
            btn.setMinWidth(140);
            btn.setStyle("-fx-font-size: 14px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5;");
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-font-size: 14px; -fx-background-color: #45a049; -fx-text-fill: white; -fx-background-radius: 5;"));
            btn.setOnMouseExited(e -> btn.setStyle("-fx-font-size: 14px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5;"));
        }
        // Button renameSchemeBtn = new Button("重命名方案");
        Label tips = new Label("F2 记录当前鼠标位置;\nF8 启动/停止方案;\nF3 清空当前方案中的所有点记录");
        tips.setStyle("-fx-font-size: 13px; -fx-text-fill: #555;");
        tips.setWrapText(true);
        tips.setPadding(new Insets(5, 0, 0, 0));
        saveSchemeBtn.setOnAction(event -> {
            MouseScheme current = schemeListView.getSelectionModel().getSelectedItem();
            if (current != null) {
                current.setPoints(FXCollections.observableArrayList(points));
            }
            saveAllSchemes();
            // 弹窗提示
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("保存方案");
            alert.setHeaderText(null);
            alert.setContentText("方案已保存成功！");
            alert.showAndWait();
            currentSchemeModified = false;
        });
        newSchemeBtn.setOnAction(e -> {
            String name = "方案" + (schemes.size() + 1);
            MouseScheme ms = new MouseScheme(name, FXCollections.observableArrayList());
            schemes.add(ms);
            schemeListView.getSelectionModel().select(ms);
            saveAllSchemes();
        });

        deleteSchemeBtn.setOnAction(e -> {
            MouseScheme selected = schemeListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                int idx = schemeListView.getSelectionModel().getSelectedIndex();
                schemes.remove(selected);
                if (!schemes.isEmpty()) {
                    schemeListView.getSelectionModel().select(idx > 0 ? idx - 1 : 0);
                }
                saveAllSchemes();
            }
        });
        //
        // renameSchemeBtn.setOnAction(e -> {
        //     MouseScheme selected = schemeListView.getSelectionModel().getSelectedItem();
        //     if (selected != null) {
        //         TextInputDialog dialog = new TextInputDialog(selected.getName());
        //         dialog.setTitle("重命名方案");
        //         dialog.setHeaderText(null);
        //         dialog.setContentText("名称:");
        //         dialog.showAndWait().ifPresent(selected::setName);
        //         schemeListView.refresh();
        //         saveAllSchemes();
        //     }
        // });

        VBox schemeBox = new VBox(5, schemeListView, newSchemeBtn,saveSchemeBtn, deleteSchemeBtn,tips);
        schemeBox.setPadding(new Insets(10)); // 四周空白
        schemeBox.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc; -fx-border-radius: 8; -fx-background-radius: 8;");
        // 表格
        table = new TableView<>(points);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

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
        buttonCol.setStyle(
                "-fx-font-size: 13px; -fx-alignment: CENTER; -fx-padding: 0 5 0 5;"
        );
        buttonCol.setOnEditCommit(event -> event.getRowValue().setButton(event.getNewValue()));

        TableColumn<ClickPoint, Integer> moveDelayCol = new TableColumn<>("移动延时(ms)");
        moveDelayCol.setCellValueFactory(data -> data.getValue().moveDelayProperty().asObject());
        moveDelayCol.setCellFactory(col -> createEditingCell(new javafx.util.converter.IntegerStringConverter()));
        moveDelayCol.setOnEditCommit(event -> event.getRowValue().setMoveDelay(event.getNewValue()));

        TableColumn<ClickPoint, Integer> keepDelayCol = new TableColumn<>("按住延时(ms)");
        keepDelayCol.setCellValueFactory(data -> data.getValue().keepDelayProperty().asObject());
        keepDelayCol.setCellFactory(col -> createEditingCell(new javafx.util.converter.IntegerStringConverter()));
        keepDelayCol.setOnEditCommit(event -> event.getRowValue().setKeepDelay(event.getNewValue()));

        TableColumn<ClickPoint, Integer> clickDelayCol = new TableColumn<>("点击延时(ms)");
        clickDelayCol.setCellValueFactory(data -> data.getValue().clickDelayProperty().asObject());
        clickDelayCol.setCellFactory(col -> createEditingCell(new javafx.util.converter.IntegerStringConverter()));
        clickDelayCol.setOnEditCommit(event -> event.getRowValue().setClickDelay(event.getNewValue()));

        TableColumn<ClickPoint, Boolean> doClickCol = new TableColumn<>("是否点击");
        doClickCol.setCellValueFactory(data -> data.getValue().doClickProperty());
        doClickCol.setCellFactory(col -> {
            CheckBoxTableCell<ClickPoint, Boolean> cell = new CheckBoxTableCell<>();
            cell.setStyle("-fx-alignment: CENTER;");
            return cell;
        });
        doClickCol.setEditable(true);

        table.getColumns().addAll(xCol, yCol, buttonCol, moveDelayCol, keepDelayCol, clickDelayCol, doClickCol);
        table.setStyle(
                "-fx-font-size: 13px;" +             // 字体大小
                        "-fx-background-color: #f9f9f9;" +   // 背景颜色
                        "-fx-border-color: #ccc;" +          // 边框颜色
                        "-fx-border-radius: 5;" +            // 圆角边框
                        "-fx-table-cell-border-color: #ddd;" // 单元格分割线
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(28);
        for (TableColumn<ClickPoint, ?> col : table.getColumns()) {
            col.setStyle("-fx-alignment: CENTER; -fx-font-size: 13px;"); // 内容居中
        }

        table.setRowFactory(tv -> {
            TableRow<ClickPoint> row = new TableRow<>();
            row.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) row.setStyle("-fx-background-color: #ff9357;");
                else row.setStyle("-fx-background-color: transparent;");
            });
            return row;
        });

        // 循环模式
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

        Button runBtn = new Button("执行/停止方案(F8)");
        Button deletePointBtn = new Button("删除选中点");
        Button clearPointsBtn = new Button("清空所有点(F3)");
        Button[] bottomButtons = {runBtn, deletePointBtn, clearPointsBtn};
        for (Button btn : bottomButtons) {
            btn.setMinWidth(140);
            btn.setStyle("-fx-font-size: 14px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5;");
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-font-size: 14px; -fx-background-color: #45a049; -fx-text-fill: white; -fx-background-radius: 5;"));
            btn.setOnMouseExited(e -> btn.setStyle("-fx-font-size: 14px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5;"));
        }

        runBtn.setOnAction(e -> runScheme());
        deletePointBtn.setOnAction(e -> {
            ClickPoint selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) points.remove(selected);
        });
        clearPointsBtn.setOnAction(e -> points.clear());

        Label loopDelayLabel = new Label("循环间隔(ms):");
        loopDelayLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");

        setStyle();

        // 然后布局HBox时增加间距
        HBox controls = new HBox(8, runBtn, deletePointBtn, clearPointsBtn,
                loopModeBox, new Label(":"), loopCountInput, endTimeInput,
                loopDelayLabel, loopDelayField);
        controls.setPadding(new Insets(5));
        controls.setStyle("-fx-background-color: #e8e8e8; -fx-border-radius: 5; -fx-background-radius: 5;");
        controls.setPadding(new Insets(5));
        controls.setAlignment(Pos.CENTER_LEFT); // 垂直居中

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(150);
        logArea.setStyle("-fx-font-family: Consolas; -fx-font-size: 12px; -fx-control-inner-background: #1e1e1e; -fx-text-fill: #f0f0f0;");
        BorderPane root = new BorderPane();
        root.setLeft(schemeBox);
        VBox centerBox = new VBox(10, table, controls, logArea);
        centerBox.setPadding(new Insets(10));
        root.setCenter(centerBox);
        root.setStyle("-fx-background-color: #fafafa;");
        stage.setScene(new Scene(root, 1300, 600));
        stage.setTitle("鼠标点击器");
        stage.show();

        if (!schemes.isEmpty()) {
            schemeListView.getSelectionModel().select(0);
            points.setAll(schemeListView.getSelectionModel().getSelectedItem().getPoints());
        }

        points.addListener((javafx.collections.ListChangeListener<ClickPoint>) c -> {
            // MouseScheme current = schemeListView.getSelectionModel().getSelectedItem();
            // if (current != null) current.setPoints(FXCollections.observableArrayList(points));
            // saveAllSchemes();
            currentSchemeModified = true; // 标记当前方案已修改
        });
    }

    private void setStyle() {
        // 假设按钮高度为28
        double controlHeight = 28;

        // 循环模式下拉框
        loopModeBox.setPrefHeight(controlHeight);
        loopModeBox.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-background-radius: 5;" +
                        "-fx-border-radius: 5;" +
                        "-fx-padding: 0 5 0 5;"
        );
        // 循环次数 / 结束时间输入框
        loopCountInput.setPrefHeight(controlHeight);
        loopCountInput.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-background-radius: 5;" +
                        "-fx-border-radius: 5;" +
                        "-fx-padding: 0 5 0 5;"
        );
        endTimeInput.setPrefHeight(controlHeight);
        endTimeInput.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-background-radius: 5;" +
                        "-fx-border-radius: 5;" +
                        "-fx-padding: 0 5 0 5;"
        );

        // 循环间隔输入框
        loopDelayField.setPrefHeight(controlHeight);
        loopDelayField.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-background-radius: 5;" +
                        "-fx-border-radius: 5;" +
                        "-fx-padding: 0 5 0 5;"
        );
    }

    // MouseClickerGUI.java 加载方案部分
    private void loadAllSchemes() {
        if (!schemesFile.exists()) {
            schemes.clear();
            return;
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(schemesFile), "UTF-8")) {
            char[] buf = new char[(int) schemesFile.length()];
            reader.read(buf);
            String content = new String(buf);
            if (content.trim().isEmpty()) {
                schemes.clear();
                return;
            }

            List<MouseScheme> list = JSON.parseObject(content, new TypeReference<List<MouseScheme>>() {});
            schemes.clear();
            // 转成 ObservableList 并绑定
            for (MouseScheme ms : list) {
                ms.setPoints(new ArrayList<>(ms.getPoints())); // 确保不为 null
                schemes.add(ms);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveAllSchemes() {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(schemesFile), "UTF-8")) {
            writer.write(JSON.toJSONString(schemes));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runScheme() {
        MouseScheme current = schemeListView.getSelectionModel().getSelectedItem();
        if (current == null || current.getPoints().isEmpty()) return;

        // 复制当前方案点列表，保证执行时不受外部修改影响
        List<ClickPoint> pointsToRun = new ArrayList<>();
        for (ClickPoint p : current.getPoints()) {
            pointsToRun.add(new ClickPoint(
                    p.getX(), p.getY(), p.getButton(),
                    p.getMoveDelay(), p.getKeepDelay(),
                    p.getClickDelay(), p.isDoClick()
            ));
        }

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
                    Date endDate = sdf.parse(endTimeText);
                    endMillis = endDate.getTime();
                }
            }
        } catch (Exception e) {
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
                    for (ClickPoint p : pointsToRun) {
                        if (!running) break;

                        if (p.getMoveDelay() > 0) Thread.sleep(p.getMoveDelay());
                        robot.mouseMove(p.getX(), p.getY());

                        if (p.isDoClick()) {
                            if (p.getClickDelay() > 0) Thread.sleep(p.getClickDelay());
                            int mask = p.getButton().equalsIgnoreCase("LEFT") ?
                                    InputEvent.BUTTON1_DOWN_MASK : InputEvent.BUTTON3_DOWN_MASK;
                            robot.mousePress(mask);
                            robot.delay(p.getKeepDelay());
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
                appendLog("执行异常：" + e.getMessage());
            } finally {
                running = false;
                appendLog("执行结束");
            }
        }).start();
    }

    private <T> TextFieldTableCell<ClickPoint, T> createEditingCell(javafx.util.StringConverter<T> converter) {
        TextFieldTableCell<ClickPoint, T> cell = new TextFieldTableCell<>(converter);
        cell.setStyle(
                "-fx-background-radius: 5;" +
                        "-fx-border-radius: 5;" +
                        "-fx-padding: 0 5 0 5;" +
                        "-fx-font-size: 13px;"
        );
        cell.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                cell.commitEdit(cell.getConverter().fromString(cell.getText()));
            }
        });
        return cell;
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

    @Override
    public void stop() throws Exception {
        MouseScheme current = schemeListView.getSelectionModel().getSelectedItem();
        if (current != null) {
            current.setPoints(FXCollections.observableArrayList(points));
        }
        saveAllSchemes();
        GlobalScreen.unregisterNativeHook();
        super.stop();
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if (e.getKeyCode() == NativeKeyEvent.VC_F2) {
            Point p = MouseInfo.getPointerInfo().getLocation();
            Platform.runLater(() -> points.add(new ClickPoint(p.x, p.y, "LEFT", 500, 100, 100, true)));
        } else if (e.getKeyCode() == NativeKeyEvent.VC_F3) {
            Platform.runLater(points::clear);
        } else if (e.getKeyCode() == NativeKeyEvent.VC_F8) {
            if (!running) runScheme();
            else running = false;
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {}
    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {}
}
