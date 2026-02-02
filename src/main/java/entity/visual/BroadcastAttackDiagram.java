package entity.visual;

import com.mxgraph.layout.mxParallelEdgeLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;

/**
 * 5G Broadcast Attack 攻击环境图 + 攻击说明 + 实时输出面板
 */
public class BroadcastAttackDiagram extends JFrame {

    private static BroadcastAttackDiagram instance;  // 单例
    private static JTextPane logPane;
    private static StyledDocument logDoc;

    public BroadcastAttackDiagram() {
        super("5G Broadcast Attack 攻击可视化界面");

        // 全局字体设置，确保中文不乱码
        UIManager.put("Label.font", new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        UIManager.put("TextArea.font", new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        UIManager.put("TextPane.font", new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        UIManager.put("Button.font", new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        UIManager.put("TitledBorder.font", new Font("Microsoft YaHei UI", Font.BOLD, 13));

        setLayout(new BorderLayout());

        // === 顶部示意图：显示本地图片 ===
        String imagePath = "images/broadcast.png";

        JLabel imgLabel = createImagePanel(imagePath, this);
        JScrollPane imgScrollPane = new JScrollPane(imgLabel);

        // 边框标题：攻击示意图
        imgScrollPane.setBorder(BorderFactory.createTitledBorder("攻击示意图"));

        add(imgScrollPane, BorderLayout.CENTER);


        // ===== 下方面板：攻击说明 + 实时日志 =====
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        // ---- 攻击说明 ----
        JTextArea info = new JTextArea(
                "【攻击说明：Broadcast Attack（广播重放攻击）】\n\n" +
                        "1. 攻击者监听合法基站广播的 RAND / AUTN 信令信息。\n" +
                        "2. 攻击者将捕获的认证参数重放给多个 UE 终端（UE1、UE2、UE3）。\n" +
                        "3. 因认证序列号（SQN）不同步，UE 产生 Sync Failure 或 Authentication Reject。\n" +
                        "4. 受害终端出现频繁掉线、拒绝接入等现象。\n" +
                        "5. 攻击影响：可导致拒绝服务 (DoS)，甚至配合伪基站诱导攻击。\n\n"
        );
        info.setEditable(false);
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        info.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        info.setBackground(new Color(250, 250, 250));
        info.setBorder(BorderFactory.createTitledBorder("攻击说明"));

        // ---- 实时日志面板 ----
        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        logDoc = logPane.getStyledDocument();

        JScrollPane scrollPane = new JScrollPane(logPane);
        scrollPane.setBorder(BorderFactory.createTitledBorder("实时攻击日志输出"));

        // 添加到下方面板
        bottomPanel.add(new JScrollPane(info));
        bottomPanel.add(scrollPane);

        add(bottomPanel, BorderLayout.SOUTH);

        // 日志样式
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

    /** 添加日志行 */
    public static void appendLog(String msg, String style) {
        if (instance == null || logDoc == null) return;
        SwingUtilities.invokeLater(() -> {
            try {
                logDoc.insertString(logDoc.getLength(), msg + "\n", logPane.getStyle(style));
                logPane.setCaretPosition(logDoc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    /** 启动 GUI */
    public static void showDiagram() {
        SwingUtilities.invokeLater(() -> {
            instance = new BroadcastAttackDiagram();
            instance.setVisible(true);
        });
    }

    /** 将 System.out 重定向到 GUI 日志区域 */
    public static void redirectSystemOut(PrintStream logStream) {
        try {
            PrintStream guiStream = new PrintStream(new OutputStream() {
                private final StringBuilder buf = new StringBuilder();
                @Override
                public void write(int b) {
                    if (b == '\n') {
                        appendLog(buf.toString(), "info");
                        if (logStream != null) logStream.println(buf.toString()); // 同步写文件
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
                return new JLabel("❌ 图片不存在：" + absPath);
            }

            Image img = ImageIO.read(file);
            int imgW = img.getWidth(null);
            int imgH = img.getHeight(null);

            // ===== 设置窗口大小 = 图片大小 + 边框 =====
            int margin = 200;
            frame.setSize(imgW + margin, imgH + margin);

            // ===== 创建 JLabel 放图片（比例不变）=====
            JLabel label = new JLabel(new ImageIcon(img));
            label.setHorizontalAlignment(JLabel.CENTER);

            return label;

        } catch (Exception e) {
            e.printStackTrace();
            return new JLabel("❌ 图片加载失败：" + absPath);
        }
    }

}
