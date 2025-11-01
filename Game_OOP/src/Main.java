// ===== Imports: UI (Swing/AWT), Events, ไฟล์, และเครือข่ายเกม =====
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import network.*; // ใช้ GameClient/GameServer ที่คุณเขียนเอง

// ===== จุดเริ่มโปรแกรม =====
public class Main {
    public static void main(String[] args) {
        // สร้างหน้าต่างหลัก แล้วใส่ MainPanel เข้าไป
        MainFrame frame = new MainFrame();
        MainPanel panel = new MainPanel();
        frame.add(panel);

        // จัดขนาดตามเนื้อหา + จัดกลางจอ + แสดง
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

// ===== เฟรมหลักของเกม (เมนูเริ่มต้น) =====
class MainFrame extends JFrame {
    MainFrame() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); // ปิดโปรแกรมเมื่อปิดหน้าต่างนี้
        setResizable(false);                                     // ไม่ให้ resize
        setTitle("Soyer VS Zombies");
    }
}

// ===== แผงหลัก: วาดพื้นหลัง + ปุ่มเริ่ม/เลือกตัวละคร + ลอจิกเปิดหน้าต่างถัดไป =====
class MainPanel extends JPanel {

    // --- โหลดรูปจากโฟลเดอร์โปรเจกต์ (พื้นหลัง/ปุ่มต่าง ๆ) ---
    private final ImageIcon bgIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "newbg.png");
    private final Image bg = bgIcon.getImage();

    private final ImageIcon bgIcon2 = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "charbg.png");
    private final ImageIcon startIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "startnew.png");
    private final ImageIcon characterIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "ch2.png");
    private final ImageIcon okIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "ok2.png");
    private final ImageIcon hostIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "host.png");
    private final ImageIcon joinIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "join.png");
    private final ImageIcon soloIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "solo.png");
    private final ImageIcon chgril = new ImageIcon( // (ไม่ได้ใช้ในไฟล์นี้ แต่เผื่ออนาคต)
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "Chgirl.png");

    // ตัวแปรเก็บตัวละครที่เลือก (แชร์ข้ามหน้าต่างง่าย ๆ)
    public static String selectedCharacter = "male";

    // ปุ่มในหน้าแรก
    private final JButton start = new JButton(startIcon);
    private final JButton character = new JButton(characterIcon);

    // อ้างอิงหน้าต่างที่เปิดไว้ (กันเปิดซ้ำ/จัดการกลับหน้าเดิม)
    private JFrame joinLobby = null;
    private boolean StopBugmain = false;   // flag กันการ “เด้งกลับหน้าแรก” ตอนกำลังเปลี่ยนฉาก
    private JFrame characterFrame = null;
    private JFrame joinFrame = null;
    private JFrame hostFrame = null;
    private JFrame gameFrame = null;

    // ===== สร้างหน้าแรก (พื้นหลัง + ปุ่ม Start/Character) =====
    MainPanel() {
        // ขนาดเท่ารูปพื้นหลัง และใช้ absolute layout (setBounds เอง)
        setPreferredSize(new Dimension(bgIcon.getIconWidth(), bgIcon.getIconHeight()));
        setLayout(null);

        // ขนาดปุ่ม
        start.setSize(startIcon.getIconWidth(), startIcon.getIconHeight() - 97);
        character.setSize(characterIcon.getIconWidth(), characterIcon.getIconHeight());

        // ปรับสไตล์ปุ่มให้เป็นปุ่มรูป (โปร่ง/ไม่มีกรอบ/มี cursor)
        Button(start);
        Button(character);

        // คำนวณ center ของหน้าจอ
        int screenCenterX = bgIcon.getIconWidth() / 2;
        int screenCenterY = bgIcon.getIconHeight() / 2;

        // คำนวณตำแหน่งปุ่ม
        int startX = screenCenterX - startIcon.getIconWidth() / 2;
        int S = (screenCenterX - characterIcon.getIconWidth() / 2) + 200; // (ตัวแปรนี้ไม่ได้ใช้จริง)

        int characterX = (screenCenterX - characterIcon.getIconWidth() / 2) + 200;
        character.setLocation(characterX, screenCenterY - 50);

        // เริ่มแรกวางปุ่ม Start ไว้ล่าง (ใช้เอฟเฟกต์เลื่อนขึ้นได้ภายหลัง)
        start.setLocation(startX - 15, getHeight() + 400);
        add(start);

        // ===== เมื่อกดปุ่ม Start → ไปหน้า Game Setup (กรอกชื่อ/เลือกตัวละคร/โหมดเล่น) =====
        start.addActionListener(e -> {
            // ถ้ามี gameFrame อยู่แล้ว → ดึงขึ้นหน้า ไม่สร้างใหม่
            if (gameFrame != null && gameFrame.isDisplayable()) {
                gameFrame.toFront();
                return;
            }

            // ซ่อน main frame เพื่อไปหน้าเกม (กันซ้อนหลายบาน)
            JFrame mainFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            if (mainFrame != null) {
                mainFrame.setVisible(false);
            }

            // หน้าต่าง Game Setup
            gameFrame = new JFrame("Soyer VS Zombies - Game");
            gameFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            gameFrame.setResizable(false);

            // พาเนลวาดพื้นหลังในหน้า Game Setup
            JPanel gamePanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    g.drawImage(bg, 0, 0, this); // วาดพื้นหลังเกม
                }
            };
            gamePanel.setPreferredSize(new Dimension(bgIcon.getIconWidth(), bgIcon.getIconHeight()));
            gamePanel.setLayout(null);

            // ปุ่ม Back: ปิด gameFrame แล้วกลับไป mainFrame
            JButton backBtni = createBackButton(gameFrame, mainFrame);
            gamePanel.add(backBtni);

            // ปุ่มเปิดหน้าจอเลือกตัวละคร (ไป Character Selection)
            JButton gameCharacterBtn = new JButton(characterIcon);
            gameCharacterBtn.setSize(characterIcon.getIconWidth(), characterIcon.getIconHeight() - 70);
            Button(gameCharacterBtn);
            int gameCharacterX = (bgIcon.getIconWidth() / 2 - characterIcon.getIconWidth() / 2) + 200;
            int gameCharacterY = bgIcon.getIconHeight() / 2 + 70;
            gameCharacterBtn.setLocation(gameCharacterX, gameCharacterY + 10);

            // ช่องกรอกชื่อ
            int textWidth = 220, textHeight = 40;
            JTextField nameField = new JTextField("Input name...");
            nameField.setFont(new Font("Arial", Font.BOLD, 20));
            nameField.setHorizontalAlignment(JTextField.CENTER);
            nameField.setSize(350, 60);
            // สไตล์ช่องกรอกชื่อ
            nameField.setBackground(new Color(54, 54, 48, 255));
            nameField.setForeground(new Color(210, 188, 148));
            nameField.setOpaque(true);
            nameField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(35, 34, 29), 4, true),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)));
            nameField.setCaretColor(new Color(100, 149, 237));
            nameField.setSelectionColor(new Color(173, 216, 230));
            nameField.setForeground(new Color(193, 193, 193));
            // Placeholder logic: ลบ/คืนข้อความ "Input name..." เมื่อ focus เข้า/ออก
            nameField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent evt) {
                    if (nameField.getText().equals("Input name...")) {
                        nameField.setText("");
                        nameField.setForeground(new Color(210, 188, 148));
                    }
                }
                @Override
                public void focusLost(FocusEvent evt) {
                    if (nameField.getText().isEmpty()) {
                        nameField.setText("Input name...");
                        nameField.setForeground(new Color(210, 188, 148));
                    }
                }
            });
            int nameX = characterX - textWidth - 140;
            int nameY = screenCenterY - textHeight / 2 + 108;

            // ปุ่ม OK (ไปเลือกโหมด Host/Join/Solo)
            JButton okButton = new JButton(okIcon);
            Button(okButton);
            okButton.setSize(okIcon.getIconWidth(), okIcon.getIconHeight() - 90);
            okButton.setLocation(280, 450);

            // ใส่คอมโพเนนต์ลง gamePanel
            gamePanel.add(gameCharacterBtn);
            gamePanel.add(nameField);
            nameField.setLocation(nameX, nameY);
            gamePanel.add(okButton);

            // แสดง gameFrame
            gameFrame.add(gamePanel);
            gameFrame.pack();
            gameFrame.setLocationRelativeTo(null);
            gameFrame.setVisible(true);

            // ===== ปุ่มเลือกตัวละคร: เปิด Character Selection =====
            gameCharacterBtn.addActionListener(et -> {
                StopBugmain = true;            // กันเด้งกลับหน้าแรกตอนปิด
                if (mainFrame != null) mainFrame.dispose();

                // หน้าต่างเลือกตัวละคร
                characterFrame = new JFrame("Character Selection");
                characterFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // (แนะนำ: ใช้ DISPOSE_ON_CLOSE จะปลอดภัยกว่า)
                characterFrame.setSize(1248, 850);
                characterFrame.setLocationRelativeTo(null);
                characterFrame.setResizable(false);

                // พื้นหลังหน้าเลือกตัวละคร
                JLabel bgLabel = new JLabel(bgIcon2);
                bgLabel.setLayout(null);
                characterFrame.setContentPane(bgLabel);

                // แสดงตัวละคร Soyer (ชาย)
                ImageIcon soyerMaleIcon = new ImageIcon(
                        System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                                + File.separator + "game" + File.separator + "soyer1.png");
                JLabel soyerMaleLabel = new JLabel(soyerMaleIcon);
                soyerMaleLabel.setBounds(300, 300, soyerMaleIcon.getIconWidth(), soyerMaleIcon.getIconHeight());
                bgLabel.add(soyerMaleLabel);

                // ป้ายชื่อ
                JLabel soyerMaleName = new JLabel("Soyer (Male)", JLabel.CENTER);
                soyerMaleName.setFont(new Font("Arial", Font.BOLD, 20));
                soyerMaleName.setForeground(Color.WHITE);
                soyerMaleName.setOpaque(true);
                soyerMaleName.setBackground(new Color(0, 0, 0, 150));
                soyerMaleName.setBounds(300 - 10, 300 + soyerMaleIcon.getIconHeight() + 10,
                        soyerMaleIcon.getIconWidth() + 20, 30);
                bgLabel.add(soyerMaleName);

                // ปุ่มเลือกตัวละครชาย
                JButton selectSoyerBtn = new JButton("Select Soyer");
                selectSoyerBtn.setFont(new Font("Arial", Font.BOLD, 18));
                selectSoyerBtn.setBackground(new Color(76, 175, 80));
                selectSoyerBtn.setForeground(Color.WHITE);
                selectSoyerBtn.setFocusPainted(false);
                selectSoyerBtn.setBounds(300 + (soyerMaleIcon.getIconWidth() - 150) / 2,
                        300 + soyerMaleIcon.getIconHeight() + 50, 150, 40);
                selectSoyerBtn.addActionListener(ev -> {
                    // เซ็ตตัวละครที่เลือก แล้วปิดหน้าจอเลือกตัวละคร
                    MainPanel.selectedCharacter = "male";
                    if (characterFrame != null) {
                        characterFrame.dispose();
                        characterFrame = null;
                    }
                    if (gameFrame != null) {
                        gameFrame.setVisible(true);
                    }
                    // โหลดรูปผู้เล่น (ยังไม่ใช้ในจุดนี้ แต่อาจใช้ใน GameFrame)
                    ImageIcon male = new ImageIcon(
                            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                                    + File.separator + "game" + File.separator + "player1.png");
                });
                // เอฟเฟกต์ hover ปุ่มเลือกตัวละครชาย
                selectSoyerBtn.addMouseListener(new MouseListener() {
                    public void mouseClicked(MouseEvent e) { }
                    public void mousePressed(MouseEvent e) { }
                    public void mouseReleased(MouseEvent e) { }
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        selectSoyerBtn.setBackground(new Color(102, 187, 106));
                        selectSoyerBtn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
                    }
                    @Override
                    public void mouseExited(MouseEvent e) {
                        selectSoyerBtn.setBackground(new Color(76, 175, 80));
                        selectSoyerBtn.setBorder(BorderFactory.createEmptyBorder());
                    }
                });
                bgLabel.add(selectSoyerBtn);

                // แสดงตัวละคร Soyer (หญิง)
                ImageIcon soyerFemaleIcon = new ImageIcon(
                        System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                                + File.separator + "game" + File.separator + "soyer2.png");
                JLabel soyerFemaleLabel = new JLabel(soyerFemaleIcon);
                soyerFemaleLabel.setBounds(800, 300, soyerFemaleIcon.getIconWidth(), soyerFemaleIcon.getIconHeight());
                bgLabel.add(soyerFemaleLabel);

                JLabel soyerFemaleName = new JLabel("Soyer(Female)", JLabel.CENTER);
                soyerFemaleName.setFont(new Font("Arial", Font.BOLD, 20));
                soyerFemaleName.setForeground(Color.WHITE);
                soyerFemaleName.setOpaque(true);
                soyerFemaleName.setBackground(new Color(0, 0, 0, 150));
                soyerFemaleName.setBounds(800 - 10, 300 + soyerFemaleIcon.getIconHeight() + 10,
                        soyerFemaleIcon.getIconWidth() + 20, 30);
                bgLabel.add(soyerFemaleName);

                // ปุ่มเลือกตัวละครหญิง
                JButton selectSoyerGBtn = new JButton("Select Soyer");
                selectSoyerGBtn.setFont(new Font("Arial", Font.BOLD, 18));
                selectSoyerGBtn.setBackground(new Color(76, 175, 80));
                selectSoyerGBtn.setForeground(Color.WHITE);
                selectSoyerGBtn.setFocusPainted(false);
                selectSoyerGBtn.setBounds(800 + (soyerFemaleIcon.getIconWidth() - 150) / 2,
                        300 + soyerFemaleIcon.getIconHeight() + 50, 150, 40);
                selectSoyerGBtn.addActionListener(ev -> {
                    MainPanel.selectedCharacter = "female";
                    if (characterFrame != null) {
                        characterFrame.dispose();
                        characterFrame = null;
                    }
                    if (gameFrame != null) {
                        gameFrame.setVisible(true);
                    }
                    ImageIcon girl = new ImageIcon(
                            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                                    + File.separator + "game" + File.separator + "player2.png");
                });
                // เอฟเฟกต์ hover ปุ่มเลือกตัวละครหญิง
                selectSoyerGBtn.addMouseListener(new MouseListener() {
                    @Override public void mouseClicked(MouseEvent e) {}
                    @Override public void mousePressed(MouseEvent e) {}
                    @Override public void mouseReleased(MouseEvent e) {}
                    @Override public void mouseEntered(MouseEvent e) {
                        selectSoyerGBtn.setBackground(new Color(102, 187, 106));
                        selectSoyerGBtn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
                    }
                    @Override public void mouseExited(MouseEvent e) {
                        selectSoyerGBtn.setBackground(new Color(76, 175, 80));
                        selectSoyerGBtn.setBorder(BorderFactory.createEmptyBorder());
                    }
                });
                bgLabel.add(selectSoyerGBtn);

                // ปุ่ม Back ที่หน้าเลือกตัวละคร (กลับไป Game Setup)
                JButton backBtnC = createBackButton(characterFrame, gameFrame);
                bgLabel.add(backBtnC);

                // เมื่อปิดหน้าต่างเลือกตัวละคร: เคลียร์อ้างอิง + ถ้ายังไม่ได้เปลี่ยนฉาก ให้เด้งกลับหน้าแรก
                characterFrame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        characterFrame = null;
                        if (!StopBugmain && mainFrame != null) {
                            mainFrame.setVisible(true);
                        }
                    }
                });

                characterFrame.setVisible(true);
            });

            // ===== ปุ่ม OK (ยืนยันชื่อ → ไปหน้าเลือกโหมด Host/Join/Solo) =====
            okButton.addActionListener(ev -> {
                StopBugmain = true;             // กันเด้งกลับหน้าแรก
                if (characterFrame != null) {   // ถ้าเลือกตัวละครค้างไว้ ให้ปิดก่อน
                    characterFrame.dispose();
                    characterFrame = null;
                }
                if (gameFrame != null) {
                    gameFrame.dispose();        // ปิด Game Setup เพื่อไปหน้าโหมด
                }

                // เคลียร์ placeholder เป็นชื่อจริง
                String name = nameField.getText();
                if ("Input name...".equals(name)) name = "";

                // สุ่มเลขท้าย ถ้าไม่ได้ใส่ชื่อ
                int randomNum = (int) (Math.random() * 9000) + 1000;
                final String playerName = (name == null || name.isBlank())
                        ? "Player" + "" + randomNum
                        : name.trim();

                // ===== หน้าเลือกโหมด (Host / Join / Solo) =====
                JFrame playFrame = new JFrame("Soyer VS Zombies");
                playFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                playFrame.setResizable(false);

                JLabel bgLabel = new JLabel(bgIcon);
                bgLabel.setLayout(null);
                bgLabel.setPreferredSize(new Dimension(bgIcon.getIconWidth(), bgIcon.getIconHeight()));

                // ปุ่ม Back (กลับสู่หน้า Game Setup)
                JButton backBtn1 = createBackButton(playFrame, gameFrame);
                bgLabel.add(backBtn1);

                // ปุ่มโหมด
                JButton hostButton = new JButton(hostIcon);
                JButton joinButton = new JButton(joinIcon);
                JButton soloButton = new JButton(soloIcon);

                // ขนาดปุ่ม
                hostButton.setSize(hostIcon.getIconWidth(), hostIcon.getIconHeight() - 90);
                joinButton.setSize(joinIcon.getIconWidth(), joinIcon.getIconHeight() - 90);
                soloButton.setSize(soloIcon.getIconWidth(), soloIcon.getIconHeight() - 90);

                // ตำแหน่งปุ่ม
                int centerX = bgIcon.getIconWidth() / 2;
                int centerY = bgIcon.getIconHeight() / 2;
                hostButton.setLocation(centerX - hostButton.getWidth() / 2, centerY - 100);
                joinButton.setLocation(centerX - joinButton.getWidth() / 2, centerY);
                soloButton.setLocation(centerX - soloButton.getWidth() / 2, centerY + 100);

                // สไตล์ปุ่ม
                Button(hostButton);
                Button(joinButton);
                Button(soloButton);

                // ใส่ลงพื้นหลัง
                bgLabel.add(hostButton);
                bgLabel.add(joinButton);
                // (soloButton ยังไม่ได้ add ในโค้ดเดิม — ถ้าจะใช้ ให้ bgLabel.add(soloButton) ด้วย)

                // แสดงหน้าต่างโหมด
                playFrame.setContentPane(bgLabel);
                playFrame.pack();
                playFrame.setLocationRelativeTo(null);
                playFrame.setVisible(true);

                // ====== โหมด Host: เปิดพอร์ต + สร้าง Lobby + เริ่มเกม ======
                hostButton.addActionListener(ev1 -> {
                    StopBugmain = true;
                    playFrame.dispose();

                    hostFrame = new JFrame("Soyer VS Zombies");
                    hostFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                    hostFrame.setResizable(false);

                    JLabel bgLabelHost = new JLabel(bgIcon);
                    bgLabelHost.setLayout(null);
                    hostFrame.setContentPane(bgLabelHost);

                    // ช่องกรอกพอร์ต (มี placeholder)
                    JTextField portField = new JTextField("INPUT PORT 1025 - 65535");
                    portField.setFont(new Font("Arial", Font.BOLD, 20));
                    portField.setHorizontalAlignment(JTextField.CENTER);
                    portField.setSize(350, 60);
                    portField.setBackground(new Color(54, 54, 48, 255));
                    portField.setForeground(new Color(210, 188, 148));
                    portField.setOpaque(true);
                    portField.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(35, 34, 29), 4, true),
                            BorderFactory.createEmptyBorder(10, 15, 10, 15)));
                    portField.setCaretColor(new Color(100, 149, 237));
                    portField.setSelectionColor(new Color(173, 216, 230));
                    portField.setForeground(new Color(193, 193, 193));
                    // placeholder focus logic
                    portField.addFocusListener(new FocusAdapter() {
                        @Override
                        public void focusGained(FocusEvent evt) {
                            if (portField.getText().equals("INPUT PORT 1025 - 65535")) {
                                portField.setText("");
                                portField.setForeground(new Color(210, 188, 148));
                            }
                        }
                        @Override
                        public void focusLost(FocusEvent evt) {
                            if (portField.getText().isEmpty()) {
                                portField.setText("INPUT PORT 1025 - 65535");
                                portField.setForeground(new Color(210, 188, 148));
                            }
                        }
                    });
                    portField.setSize(400, 60);
                    portField.setLocation((bgIcon.getIconWidth() - portField.getWidth()) / 2 - 80,
                            (bgIcon.getIconHeight() / 2) - 40);
                    bgLabelHost.add(portField);

                    // ปุ่มเปิดพอร์ต
                    JButton openPortBtn = new JButton(hostIcon);
                    Button(openPortBtn);
                    openPortBtn.setSize(200, 60);
                    openPortBtn.setLocation(portField.getX() + portField.getWidth() + 20, portField.getY());
                    bgLabelHost.add(openPortBtn);

                    // ปุ่ม Back (กลับหน้าโหมด)
                    JButton backBtnHost = createBackButton(hostFrame, playFrame);
                    bgLabelHost.add(backBtnHost);

                    hostFrame.pack();
                    hostFrame.setSize(bgIcon.getIconWidth(), bgIcon.getIconHeight());
                    hostFrame.setLocationRelativeTo(null);
                    hostFrame.setVisible(true);

                    // กดเปิดพอร์ต → สตาร์ทเซิร์ฟเวอร์ → เปิด Lobby ของ Host
                    openPortBtn.addActionListener(ae -> {
                        String portStr = portField.getText();
                        if (portStr == null || portStr.isEmpty() || portStr.equals("INPUT PORT")) {
                            JOptionPane.showMessageDialog(hostFrame, "Please enter a valid port number",
                                    "Invalid Port", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        int port;
                        try {
                            port = Integer.parseInt(portStr);
                            if (port < 1024 || port > 65535) {
                                JOptionPane.showMessageDialog(hostFrame, "Port must be between 1024 and 65535",
                                        "Invalid Port", JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(hostFrame, "Please enter a valid port number", "Invalid Port",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }


                        hostFrame.dispose();
                        GameServer server = new GameServer(port);
                        new Thread(() -> {
                            try {
                                server.start(); // เปิด ServerSocket แล้ววนรอ client
                            } catch (Exception ey) {
                                System.err.println("Server error: " + ey.getMessage());
                                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                                        "Failed to start server: " + ey.getMessage(),
                                        "Server Error", JOptionPane.ERROR_MESSAGE));
                            }
                        }).start();

                        // ===== Lobby ฝั่ง Host: โชว์ IP/รายชื่อผู้เล่น/ปุ่ม Start Game =====
                        JFrame lobby = new JFrame("Soyer VS Zombies - Lobby");
                        lobby.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                        lobby.setResizable(false);
                        JLabel bgLobby = new JLabel(bgIcon);
                        bgLobby.setLayout(null);
                        lobby.setContentPane(bgLobby);

                        // แสดง IP เครื่อง Host
                        JPanel ipPanel = new JPanel(null);
                        ipPanel.setBackground(new Color(54, 54, 48, 220));
                        ipPanel.setBounds(70, 50, 430, 70);
                        bgLobby.add(ipPanel);

                        JLabel ipLabel = new JLabel("Host IP: ....");
                        ipLabel.setFont(new Font("Arial", Font.BOLD, 20));
                        ipLabel.setForeground(new Color(193, 193, 193));
                        ipLabel.setBounds(18, 10, 390, 50);
                        ipPanel.add(ipLabel);

                        try {
                            String ip = java.net.InetAddress.getLocalHost().getHostAddress();
                            ipLabel.setText("Host IP: " + ip + ":" + port);
                        } catch (Exception ignore) {}

                        // พื้นที่รายชื่อผู้เล่น
                        JPanel playersPanel = new JPanel(null);
                        playersPanel.setBackground(new Color(54, 54, 48, 220));
                        playersPanel.setBounds(70, 130, 600, 330);
                        bgLobby.add(playersPanel);

                        JLabel playersTitle = new JLabel("Players");
                        playersTitle.setFont(new Font("Arial", Font.BOLD, 42));
                        playersTitle.setForeground(new Color(255, 255, 255));
                        playersTitle.setBounds(18, 8, 400, 50);
                        playersPanel.add(playersTitle);

                        DefaultListModel<String> model = new DefaultListModel<>();
                        JList<String> playerList = new JList<>(model);
                        playerList.setFont(new Font("Arial", Font.PLAIN, 24));
                        playerList.setForeground(new Color(0, 0, 0));
                        playerList.setOpaque(false);
                        JScrollPane scroll = new JScrollPane(playerList);
                        scroll.setOpaque(false);
                        scroll.getViewport().setOpaque(false);
                        scroll.setBorder(BorderFactory.createEmptyBorder());
                        scroll.setBounds(18, 60, 560, 250);
                        playersPanel.add(scroll);

                        // แสดงชื่อ Host เป็นรายการแรก
                        model.addElement(playerName + " (Host)");

                        // เก็บ server ไว้ที่ rootPane (ให้ปุ่มอื่นดึงใช้)
                        lobby.getRootPane().putClientProperty("server", server);

                        // ปุ่ม Back (หยุด server แล้วปิด Lobby)
                        JButton backBtn5 = createBackButton(lobby, playFrame);
                        backBtn5.addActionListener(eh -> {
                            server.stop();
                            lobby.dispose();
                        });
                        bgLobby.add(backBtn5);

                        // ปุ่มเริ่มเกม
                        JButton startGameBtn = new JButton(startIcon);
                        Button(startGameBtn);
                        int btnW = 200, btnH = 60;
                        startGameBtn.setSize(btnW, btnH);
                        int btnY = playersPanel.getY() + playersPanel.getHeight();
                        startGameBtn.setLocation((bgIcon.getIconWidth() - btnW) / 2, btnY);
                        bgLobby.add(startGameBtn);

                        // กด Start Game → สั่ง server ส่ง GAME_START ให้ทุกคน + สร้าง GameClient ให้ Host เล่นด้วย
                        startGameBtn.addActionListener(ed -> {

                            GameServer serverVar = (GameServer) lobby.getRootPane().getClientProperty("server");
                            if (serverVar != null)
                            {
                                serverVar.startGame();
                            }
                            lobby.dispose();

                            // Host เชื่อมต่อเข้าตัวเอง (localhost) เพื่อเข้าเกมมัลติเพลเยอร์
                            GameClient hostClient = new GameClient(playerName, message -> {});

                            SwingUtilities.invokeLater(() -> {
                                if (hostClient.connect("localhost", port)) {
                                    GameFrame frame = new GameFrame(playerName, hostClient,
                                            MainPanel.selectedCharacter);

                                    // ส่งข้อความเครือข่ายไปที่ GamePanel
                                    hostClient.setMessageListener(msg -> {
                                        GamePanel panel = frame.getGamePanel();
                                        if (panel != null) {
                                            panel.handleNetworkMessage(msg);
                                        }
                                    });

                                    // ตั้งค่า host flag ในเกม (ถ้าต้องใช้)
                                    GamePanel panel = frame.getGamePanel();
                                    if (panel != null) {
                                        panel.setAsHost();
                                    }
                                } else {
                                    // ถ้าเชื่อมต่อไม่ได้ → เล่นโหมดเดี่ยว
                                    new GameFrame(playerName, MainPanel.selectedCharacter);
                                }
                            });
                        });

                        // แสดง Lobby
                        lobby.pack();
                        lobby.setSize(bgIcon.getIconWidth(), bgIcon.getIconHeight());
                        lobby.setLocationRelativeTo(null);
                        lobby.setVisible(true);

                        // เธรด background: โพลรายชื่อผู้เล่นจาก server ทุก 0.5 วินาที แล้วอัปเดต UI
                        Thread pollThread = new Thread(() -> {
                            try {
                                while (!Thread.currentThread().isInterrupted()) {
                                    java.util.List<String> names = server.getPlayerNames();
                                    SwingUtilities.invokeLater(() -> {
                                        model.clear();
                                        model.addElement(playerName + " (Host)");
                                        for (String n : names) {
                                            if (n != null && !n.isBlank() && !n.equals(playerName)) {
                                                model.addElement(n);
                                            }
                                        }
                                    });
                                    Thread.sleep(500);
                                }
                            } catch (InterruptedException eh) {
                                System.out.println("Polling thread stopped.");
                            } catch (Exception ej) {
                                ej.printStackTrace();
                            }
                        });
                        pollThread.start();

                        // เมื่อปิด Lobby ให้หยุดเธรดโพลด้วย
                        lobby.addWindowListener(new WindowAdapter() {
                            @Override
                            public void windowClosed(WindowEvent e) {
                                pollThread.interrupt();
                            }
                        });
                    });
                });

                // ====== โหมด Join: กรอก IP:Port แล้วเชื่อมต่อเข้า Lobby ของ Host ======
                joinButton.addActionListener(ev2 -> {
                    StopBugmain = true;
                    playFrame.dispose();

                    joinFrame = new JFrame("Soyer VS Zombies");
                    joinFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                    joinFrame.setResizable(false);

                    JLabel bgLabelJoin = new JLabel(bgIcon);
                    bgLabelJoin.setLayout(null);
                    joinFrame.setContentPane(bgLabelJoin);

                    // ช่องกรอก IP:Port (placeholder)
                    JTextField ipField = new JTextField("INPUT HOST IP");
                    ipField.setFont(new Font("Arial", Font.BOLD, 20));
                    ipField.setHorizontalAlignment(JTextField.CENTER);
                    ipField.setSize(350, 60);
                    ipField.setBackground(new Color(54, 54, 48, 255));
                    ipField.setForeground(new Color(210, 188, 148));
                    ipField.setOpaque(true);
                    ipField.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(35, 34, 29), 4, true),
                            BorderFactory.createEmptyBorder(10, 15, 10, 15)));
                    ipField.setCaretColor(new Color(100, 149, 237));
                    ipField.setSelectionColor(new Color(173, 216, 230));
                    ipField.setForeground(new Color(193, 193, 193));
                    ipField.addFocusListener(new FocusAdapter() {
                        @Override
                        public void focusGained(FocusEvent evt) {
                            if (ipField.getText().equals("INPUT HOST IP")) {
                                ipField.setText("");
                                ipField.setForeground(new Color(210, 188, 148));
                            }
                        }
                        @Override
                        public void focusLost(FocusEvent evt) {
                            if (ipField.getText().isEmpty()) {
                                ipField.setText("INPUT HOST IP");
                                ipField.setForeground(new Color(210, 188, 148));
                            }
                        }
                    });

                    ipField.setSize(400, 60);
                    ipField.setLocation((bgIcon.getIconWidth() - ipField.getWidth()) / 2 - 80,
                            (bgIcon.getIconHeight() / 2) - 40);
                    bgLabelJoin.add(ipField);

                    // ปุ่ม Join
                    JButton joinConfirm = new JButton(joinIcon);
                    Button(joinConfirm);
                    joinConfirm.setSize(200, 60);
                    joinConfirm.setLocation(ipField.getX() + ipField.getWidth() + 20, ipField.getY());
                    bgLabelJoin.add(joinConfirm);

                    // ปุ่ม Back (กลับหน้าโหมด)
                    JButton backBtnj = createBackButton(joinFrame, playFrame);
                    bgLabelJoin.add(backBtnj);

                    // กด Join → เชื่อมต่อ GameClient → เข้า Lobby ฝั่ง Join
                    joinConfirm.addActionListener(ej -> {
                        String ipPort = ipField.getText();
                        if (ipPort == null || ipPort.isEmpty() || ipPort.equals("INPUT HOST IP")) {
                            JOptionPane.showMessageDialog(joinFrame, "Please enter a valid IP address and port",
                                    "Invalid Address", JOptionPane.ERROR_MESSAGE);
                            return;
                        }

                        // แยก host:port (ถ้าไม่ใส่ port → ใช้ 8080)
                        String[] parts = ipPort.split(":");
                        String host = parts[0];
                        int port = 8080;
                        if (parts.length > 1) {
                            try {
                                port = Integer.parseInt(parts[1]);
                            } catch (NumberFormatException ex) {
                                JOptionPane.showMessageDialog(joinFrame, "Invalid port number",
                                        "Invalid Port", JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                        }

                        // สร้าง client และตั้ง message handler
                        final String pn = playerName;
                        final GameClient[] clientHolder = new GameClient[1];
                        final DefaultListModel<String>[] modelRef = new DefaultListModel[] { null };
                        final String selfName = playerName;

                        clientHolder[0] = new GameClient(pn, message -> {



                            SwingUtilities.invokeLater(() -> {
                                if (message.startsWith("PLAYER_LIST:")) {

                                } else if (message.startsWith("PLAYER_JOINED:")) {

                                } else if (message.startsWith("PLAYER_LEFT:")) {

                                } else if (message.startsWith("GAME_START")) {
                                    joinFrame.dispose();
                                    // แล้วเปิด GameFrame ใหม่
                                    SwingUtilities.invokeLater(() -> {
                                        GameFrame frame = new GameFrame(selfName, clientHolder[0],
                                                MainPanel.selectedCharacter);

                                        // ส่งข้อความเครือข่ายเข้า GamePanel
                                        clientHolder[0].setMessageListener(msg -> {
                                            GamePanel panel = frame.getGamePanel();
                                            if (panel != null) {
                                                panel.handleNetworkMessage(msg);
                                            }
                                        });
                                    });
                                }
                            });
                        });

                        // เชื่อมต่อไปยัง host
                        if (clientHolder[0].connect(host, port)) {
                            joinFrame.dispose();
                            // ===== Lobby ฝั่ง Join: รอ Host กด Start =====
                            final GameClient client = clientHolder[0];

                            DefaultListModel<String> model = new DefaultListModel<>();
                            modelRef[0] = model;
                            model.addElement(playerName + " (You)");

                            JFrame lobby = new JFrame("Soyer VS Zombies - Lobby");
                            lobby.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                            lobby.setResizable(false);

                            JLabel bgLobby = new JLabel(bgIcon);
                            bgLobby.setLayout(null);
                            lobby.setContentPane(bgLobby);

                            JPanel playersPanel = new JPanel(null);
                            playersPanel.setBackground(new Color(54, 54, 48, 220));
                            playersPanel.setBounds(70, 130, 600, 330);
                            bgLobby.add(playersPanel);

                            JLabel playersTitle = new JLabel("Waiting for Host...");
                            playersTitle.setFont(new Font("Arial", Font.BOLD, 42));
                            playersTitle.setForeground(new Color(193, 193, 193));
                            playersTitle.setBounds(130, 130, 400, 50);
                            playersPanel.add(playersTitle);

                            // ปุ่ม Back (ออกจาก Lobby และตัดการเชื่อมต่อ)
                            JButton backBtn = createBackButton(lobby, playFrame);
                            backBtn.addActionListener(e2 -> {
                                client.disconnect();
                                lobby.dispose();
                            });
                            bgLobby.add(backBtn);

                            // เมื่อปิด Lobby ด้วยวิธีอื่น ๆ → ตัดการเชื่อมต่อด้วย
                            lobby.addWindowListener(new WindowAdapter() {
                                @Override
                                public void windowClosed(WindowEvent e3) {
                                    client.disconnect();
                                }
                            });

                            lobby.pack();
                            lobby.setSize(bgIcon.getIconWidth(), bgIcon.getIconHeight());
                            lobby.setLocationRelativeTo(null);
                            lobby.setVisible(true);

                        } else {
                            JOptionPane.showMessageDialog(joinFrame, "Failed to connect to server",
                                    "Connection Error", JOptionPane.ERROR_MESSAGE);
                        }
                    });


                    joinFrame.pack();
                    joinFrame.setLocationRelativeTo(null);
                    joinFrame.setVisible(true);
                });


            });
        });
    }

    // ===== helper: ปรับสไตล์ปุ่มให้เป็นปุ่มรูป โปร่ง/ไม่มีกรอบ/มือชี้ =====
    private void Button(AbstractButton b) {
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // ===== วาดพื้นหลังของ MainPanel =====
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(bg, 0, 0, this);
    }

    // ===== ปุ่ม Back มาตรฐาน: hover เปลี่ยนสี + ปิดหน้าต่างปัจจุบัน + โชว์หน้าก่อนหน้า =====
    private JButton createBackButton(JFrame currentFrame, JFrame previousFrame) {
        JButton backButton = new JButton("← Back");
        backButton.setFont(new Font("Arial", Font.BOLD, 18));
        backButton.setForeground(Color.WHITE);
        backButton.setBounds(15, 10, 120, 40);
        backButton.setFocusPainted(false);
        backButton.setContentAreaFilled(false);
        backButton.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // เอฟเฟกต์ hover
        backButton.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { backButton.setForeground(new Color(255, 200, 200)); }
            @Override public void mouseExited (MouseEvent e) { backButton.setForeground(Color.WHITE); }
        });

        // คลิกแล้วปิดหน้าต่างปัจจุบัน + กลับไปหน้าก่อนหน้า (ถ้ามี)
        backButton.addActionListener(e -> {
            currentFrame.dispose();
            if (previousFrame != null) {
                previousFrame.setVisible(true);
            }
        });

        return backButton;
    }
}
