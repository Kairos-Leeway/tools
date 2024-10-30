package kairos.leeway.app;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AutoClicker {
    private JFrame frame;
    private JComboBox<String> hotkeyComboBox, mouseButtonBox;
    private JTextField intervalField;
    private JButton startButton, stopButton;
    private JLabel statusLabel; // 用于显示状态信息
    private JTextArea historyTextArea; // 用于显示历史记录

    private int hotkeyCode = NativeKeyEvent.VC_F2; // 默认快捷键
    private int interval = 100; // 默认时间间隔
    private boolean isClicking = false;
    private int mouseButton = InputEvent.BUTTON1_DOWN_MASK; // 默认左键
    private long startTime; // 点击开始时间
    private int clickCount = 0; // 点击次数
    private List<String> historyRecords; // 存储历史记录

    public AutoClicker() {
        frame = new JFrame("Auto Clicker");
        frame.setSize(800, 450); // 增宽窗口
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(8, 1));

        // 关闭 JNativeHook 日志输出
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);

        // 初始化历史记录列表
        historyRecords = new ArrayList<>();

        // 快捷键选择下拉框
        frame.add(new JLabel("Select Hotkey:"));
        hotkeyComboBox = new JComboBox<>(new String[]{"F2", "F3", "F4", "F5", "F6"});
        hotkeyComboBox.addActionListener(e -> hotkeyCode = getKeyCode((String) hotkeyComboBox.getSelectedItem()));
        frame.add(hotkeyComboBox);

        // 间隔时间输入框
        frame.add(new JLabel("Click Interval (ms):"));
        intervalField = new JTextField("100");
        frame.add(intervalField);

        // 鼠标按钮选择
        frame.add(new JLabel("Mouse Button:"));
        mouseButtonBox = new JComboBox<>(new String[]{"Left", "Right"});
        mouseButtonBox.addActionListener(e -> mouseButton = mouseButtonBox.getSelectedItem().equals("Left") ? InputEvent.BUTTON1_DOWN_MASK : InputEvent.BUTTON3_DOWN_MASK);
        frame.add(mouseButtonBox);

        // 启动按钮
        startButton = new JButton("Start");
        startButton.addActionListener(e -> startClicking());
        frame.add(startButton);

        // 停止按钮
        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopClicking());
        frame.add(stopButton);

        // 状态标签
        statusLabel = new JLabel("状态: 停止");
        frame.add(statusLabel);

        // 历史记录文本区
        historyTextArea = new JTextArea();
        historyTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(historyTextArea);
        scrollPane.setPreferredSize(new Dimension(500, 800)); // 增加高度
        scrollPane.setMinimumSize(new Dimension(500, 800));
        frame.add(scrollPane);

        frame.setVisible(true);

        // 注册全局快捷键监听
        setupGlobalKeyListener();
    }

    private int getKeyCode(String keyText) {
        switch (keyText) {
            case "F2": return NativeKeyEvent.VC_F2;
            case "F3": return NativeKeyEvent.VC_F3;
            case "F4": return NativeKeyEvent.VC_F4;
            case "F5": return NativeKeyEvent.VC_F5;
            case "F6": return NativeKeyEvent.VC_F6;
            default: return NativeKeyEvent.VC_UNDEFINED;
        }
    }

    private void setupGlobalKeyListener() {
        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
                @Override
                public void nativeKeyPressed(NativeKeyEvent e) {
                    if (e.getKeyCode() == hotkeyCode) {
                        if (isClicking) {
                            stopClicking();
                        } else {
                            startClicking();
                        }
                    }
                }

                @Override
                public void nativeKeyReleased(NativeKeyEvent e) {}

                @Override
                public void nativeKeyTyped(NativeKeyEvent e) {}
            });
        } catch (NativeHookException ex) {
            JOptionPane.showMessageDialog(frame, "Failed to register global key listener.");
        }
    }

    private void startClicking() {
        try {
            interval = Integer.parseInt(intervalField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Invalid interval. Please enter a valid number.");
            return;
        }

        isClicking = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        hotkeyComboBox.setEnabled(false);
        intervalField.setEditable(false);
        mouseButtonBox.setEnabled(false);

        // 初始化开始时间和点击次数
        startTime = System.currentTimeMillis();
        clickCount = 0;

        // 模拟连点线程
        new Thread(() -> {
            try {
                Robot robot = new Robot();
                while (isClicking) {
                    robot.mousePress(mouseButton);
                    robot.delay(200);
                    robot.mouseRelease(mouseButton);
                    clickCount++; // 增加点击次数

                    // 更新状态标签
                    updateStatus();

                    Thread.sleep(interval);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void stopClicking() {
        isClicking = false;
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        hotkeyComboBox.setEnabled(true);
        intervalField.setEditable(true);
        mouseButtonBox.setEnabled(true);
        statusLabel.setText("状态: 停止"); // 更新状态为停止

        // 记录历史
        recordHistory();
    }

    private void updateStatus() {
        long currentTime = System.currentTimeMillis();
        long duration = currentTime - startTime;
        long hours = (duration / (1000 * 60 * 60)) % 24;
        long minutes = (duration / (1000 * 60)) % 60;
        long seconds = (duration / 1000) % 60;

        // 更新状态标签
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(String.format("状态: 持续 %d时 %d分 %d秒, 期间点击 %d 次", hours, minutes, seconds, clickCount));
        });
    }

    private void recordHistory() {
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        long hours = (duration / (1000 * 60 * 60)) % 24;
        long minutes = (duration / (1000 * 60)) % 60;
        long seconds = (duration / 1000) % 60;

        // 格式化开始和结束时间，包含年月日
        String startTimeStr = new java.text.SimpleDateFormat("MM月 dd日 HH:mm:ss").format(new java.util.Date(startTime));
        String endTimeStr = new java.text.SimpleDateFormat("MM月 dd日 HH:mm:ss").format(new java.util.Date(endTime));

        String record = String.format("%s ~ %s: 持续 %d时 %d分 %d秒, 点击 %d 次\n",
                startTimeStr, endTimeStr, hours, minutes, seconds, clickCount);
        historyRecords.add(record); // 将最新记录添加到列表的开头
        updateHistoryDisplay();
    }

    private void updateHistoryDisplay() {
        SwingUtilities.invokeLater(() -> {
            StringBuilder historyBuilder = new StringBuilder();
            for (String record : historyRecords) {
                historyBuilder.append(record);
            }
            historyTextArea.setText(historyBuilder.toString());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AutoClicker::new);
    }
}
