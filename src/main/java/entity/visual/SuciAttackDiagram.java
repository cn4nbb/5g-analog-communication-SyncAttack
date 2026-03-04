package entity.visual;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * 5G SUCI 弱随机攻击可视化界面
 */
public class SuciAttackDiagram extends JFrame {

    private static SuciAttackDiagram instance;
    private static JTextPane logPane;
    private static StyledDocument logDoc;

    public SuciAttackDiagram() {
        super("5G SUCI 弱随机攻击可视化界面");

        // ===== 全局字体 =====
        UIManager.put("Label.font", new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        UIManager.put("TextArea.font", new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        UIManager.put("TextPane.font", new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        UIManager.put("TitledBorder.font", new Font("Microsoft YaHei UI", Font.BOLD, 13));

        setLayout(new BorderLayout());

        // ================= 顶部示意图 =================
        String imagePath = "C:\\workspace\\back\\5g-analog-communication-SyncAttack\\src\\main\\java\\entity\\visual\\images\\suci.png";

        JLabel imgLabel = createImagePanel(imagePath, this);
        JScrollPane imgScroll = new JScrollPane(imgLabel);
        imgScroll.setBorder(BorderFactory.createTitledBorder("攻击示意图"));

        add(imgScroll, BorderLayout.CENTER);

        // ================= 下方说明 + 日志 =================
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        // ===== 攻击说明 =====
        JTextArea info = new JTextArea(
                "【攻击名称：SUCI 弱随机攻击（Weak Randomness in ECIES）】\n\n" +
                        "1. UE 使用 ECIES 机制生成 SUCI（SUPI 加密标识）。\n" +
                        "2. 正常情况下，UE 应生成高熵随机数 r，计算 R = r·G。\n" +
                        "3. 若随机数生成器熵不足或复用种子，可能导致 R 重复。\n" +
                        "4. 攻击者通过监听 SUCI，比较 R 值即可识别同源设备。\n" +
                        "5. 匿名性丧失后，可进行定向广播重放或跟踪攻击。\n\n" +
                        "【攻击影响】\n" +
                        "- 用户身份可关联\n" +
                        "- 匿名保护失效\n" +
                        "- 为后续 DoS 或伪基站攻击提供目标筛选能力\n"
        );

        info.setEditable(false);
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        info.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        info.setBackground(new Color(250, 250, 250));
        info.setBorder(BorderFactory.createTitledBorder("攻击说明"));

        // ===== 日志输出区域 =====
        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        logDoc = logPane.getStyledDocument();

        JScrollPane logScroll = new JScrollPane(logPane);
        logScroll.setBorder(BorderFactory.createTitledBorder("实时攻击日志输出"));

        bottomPanel.add(new JScrollPane(info));
        bottomPanel.add(logScroll);

        add(bottomPanel, BorderLayout.SOUTH);

        // ===== 日志样式 =====
        addStyle("info", Color.BLACK);
        addStyle("attack", Color.RED);
        addStyle("system", new Color(0, 70, 200));

        setSize(1150, 850);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void addStyle(String name, Color color) {
        Style style = logPane.addStyle(name, null);
        StyleConstants.setForeground(style, color);
        StyleConstants.setFontSize(style, 14);
    }

    public static void appendLog(String msg, String style) {
        if (instance == null || logDoc == null) return;

        SwingUtilities.invokeLater(() -> {
            try {
                logDoc.insertString(
                        logDoc.getLength(),
                        msg + "\n",
                        logPane.getStyle(style)
                );
                logPane.setCaretPosition(logDoc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    public static void showDiagram() {
        SwingUtilities.invokeLater(() -> {
            instance = new SuciAttackDiagram();
            instance.setVisible(true);
        });
    }

    public static void redirectSystemOut(PrintStream logStream) {
        try {
            PrintStream guiStream = new PrintStream(new OutputStream() {

                private final StringBuilder buf = new StringBuilder();

                @Override
                public void write(int b) {
                    if (b == '\n') {
                        appendLog(buf.toString(), "info");

                        if (logStream != null) {
                            logStream.println(buf.toString());
                        }

                        buf.setLength(0);
                    } else {
                        buf.append((char) b);
                    }
                }
            }, true, "UTF-8");

            System.setOut(guiStream);
            System.setErr(guiStream);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JLabel createImagePanel(String absPath, JFrame frame) {
        try {
            File file = new File(absPath);

            if (!file.exists()) {
                return new JLabel("图片不存在：" + absPath);
            }

            Image img = ImageIO.read(file);

            JLabel label = new JLabel(new ImageIcon(img));
            label.setHorizontalAlignment(JLabel.CENTER);

            return label;

        } catch (Exception e) {
            e.printStackTrace();
            return new JLabel("图片加载失败：" + absPath);
        }
    }
}