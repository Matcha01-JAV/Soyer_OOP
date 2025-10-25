import javax.swing.*;
import java.awt.*;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        MainFrame frame = new MainFrame();
        MainPanel panel = new MainPanel();
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null); // Center the frame on screen

        frame.setVisible(true);
    }
}

class MainFrame extends JFrame {
    MainFrame() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setTitle("Soyer VS Zombies");

    }
}

class MainPanel extends JPanel {
    ImageIcon bgIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "newbg.png");
    Image bg = bgIcon.getImage();
    ImageIcon bgIcon2 = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "ch.png");
    Image bg2 = bgIcon2.getImage();

    ImageIcon startIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "startnew.png");
    ImageIcon characterIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "ch2.png");
    ImageIcon okIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "ok2.png");
    ImageIcon hostIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "host.png");
    ImageIcon joinIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "join.png");
    ImageIcon soloIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "solo.png");
    JButton start = new JButton(startIcon);
    JButton character = new JButton(characterIcon);
    private boolean StopBugmain = false;
    private JFrame characterFrame = null; // Track character frame
    private JFrame gameFrame = null; // Track game frame

    MainPanel() {
        setPreferredSize(new Dimension(bgIcon.getIconWidth(), bgIcon.getIconHeight()));
        setLayout(null); // Use absolute positioning

        // Set button sizes to match their images
        start.setSize(startIcon.getIconWidth(), startIcon.getIconHeight());
        character.setSize(characterIcon.getIconWidth(), characterIcon.getIconHeight());

        // Make buttons transparent (image only)
        start.setOpaque(false);
        start.setContentAreaFilled(false);
        start.setBorderPainted(false);
        start.setFocusPainted(false);

        character.setOpaque(false);
        character.setContentAreaFilled(false);
        character.setBorderPainted(false);
        character.setFocusPainted(false);

        int screenCenterX = bgIcon.getIconWidth() / 2;
        int screenCenterY = bgIcon.getIconHeight() / 2;

        int startX = screenCenterX - startIcon.getIconWidth() / 2;
        int characterX = (screenCenterX - characterIcon.getIconWidth() / 2) + 200;

        // วางตำแหน่ง

        character.setLocation(characterX, screenCenterY - 50);
        add(start);
        // (ถ้าต้องการให้ปุ่ม character แสดง ให้ปลดคอมเมนต์บรรทัดถัดไป)
        // add(character);

        start.setLocation(startX - 15, getHeight() + 400);

        // Add action listeners
        start.addActionListener(e -> {
            if (gameFrame != null && gameFrame.isDisplayable()) {
                gameFrame.toFront();
                return;
            }

            JFrame mainFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            mainFrame.setVisible(false);

            gameFrame = new JFrame("Soyer VS Zombies - Game");
            // gameFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            gameFrame.setResizable(false);
            gameFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            JPanel gamePanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    g.drawImage(bg, 0, 0, this);
                }
            };
            gamePanel.setPreferredSize(new Dimension(bgIcon.getIconWidth(), bgIcon.getIconHeight()));
            gamePanel.setLayout(null);

            JButton backBtn = createBackButton(gameFrame, mainFrame);
            gamePanel.add(backBtn);

            JButton gameCharacterBtn = new JButton(characterIcon);
            gameCharacterBtn.setSize(characterIcon.getIconWidth(), characterIcon.getIconHeight() - 70);
            gameCharacterBtn.setOpaque(false);
            gameCharacterBtn.setContentAreaFilled(false);
            gameCharacterBtn.setBorderPainted(false);
            gameCharacterBtn.setFocusPainted(false);

            int gameCharacterX = (bgIcon.getIconWidth() / 2 - characterIcon.getIconWidth() / 2) + 200;
            int gameCharacterY = bgIcon.getIconHeight() / 2 + 70;
            gameCharacterBtn.setLocation(gameCharacterX, gameCharacterY + 10);

            int textWidth = 220, textHeight = 40;
            JTextField nameField = new JTextField("Input name...");
            nameField.setFont(new Font("Arial", Font.BOLD, 20));
            nameField.setHorizontalAlignment(JTextField.CENTER);
            nameField.setSize(350, 60);
            nameField.setBackground(new Color(54, 54, 48, 255));
            nameField.setForeground(new Color(210, 188, 148));
            nameField.setOpaque(true);
            nameField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(35, 34, 29), 4, true),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)));
            nameField.setCaretColor(new Color(100, 149, 237));
            nameField.setSelectionColor(new Color(173, 216, 230));

            int nameX = characterX - textWidth - 140;
            int nameY = screenCenterY - textHeight / 2 + 108;

            nameField.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent evt) {
                    if (nameField.getText().equals("Input name...")) {
                        nameField.setText("");
                        nameField.setForeground(new Color(210, 188, 148));
                    }
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent evt) {
                    if (nameField.getText().isEmpty()) {
                        nameField.setText("Input name...");
                        nameField.setForeground(new Color(210, 188, 148));
                    }
                }
            });
            nameField.setForeground(new Color(193, 193, 193));

            JButton okButton = new JButton(okIcon);
            // Set exact size to match the image
            okButton.setSize(okIcon.getIconWidth(), okIcon.getIconHeight() - 90);
            okButton.setOpaque(false);
            okButton.setContentAreaFilled(false);
            okButton.setBorderPainted(false);
            okButton.setFocusPainted(false);

            // Use setLocation instead of setBounds to ensure exact image size
            okButton.setLocation(280, 450);

            gamePanel.add(okButton);
            nameField.setLocation(nameX, nameY);
            gamePanel.add(nameField);

            okButton.addActionListener(ev -> {
                StopBugmain = true;
                if (characterFrame != null) {
                    characterFrame.dispose();
                    characterFrame = null;
                }
                if (gameFrame != null) {
                    gameFrame.setVisible(false);
                }

                String name = nameField.getText();
                if ("Input name...".equals(name)) {
                    name = "";
                }

                JFrame playFrame = new JFrame("Soyer VS Zombies");
                playFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                playFrame.setResizable(false);
                JLabel bgLabel = new JLabel(bgIcon);
                bgLabel.setLayout(null);

                bgLabel.setPreferredSize(new Dimension(bgIcon.getIconWidth(), bgIcon.getIconHeight()));

                JButton backBtn1 = createBackButton(playFrame, gameFrame);
                bgLabel.add(backBtn1);

                playFrame.setContentPane(bgLabel);
                playFrame.pack();
                playFrame.setLocationRelativeTo(null);
                playFrame.setVisible(true);

                JButton hostButton = new JButton(hostIcon);
                JButton joinButton = new JButton(joinIcon);
                JButton Solo = new JButton(soloIcon);

                // Set button sizes to match their images
                hostButton.setSize(hostIcon.getIconWidth(), hostIcon.getIconHeight()-90);
                joinButton.setSize(joinIcon.getIconWidth(), joinIcon.getIconHeight()-90);
                Solo.setSize(soloIcon.getIconWidth(), soloIcon.getIconHeight()-90);

                // Calculate center positions for each button
                int centerX = bgIcon.getIconWidth() / 2;
                int centerY = bgIcon.getIconHeight() / 2;

                int hostX = centerX - hostIcon.getIconWidth() / 2;
                int joinX = centerX - joinIcon.getIconWidth() / 2;
                int soloX = centerX - soloIcon.getIconWidth() / 2;

                hostButton.setLocation(hostX, centerY - 100);
                joinButton.setLocation(joinX, centerY );
                Solo.setLocation(soloX, centerY + 100);

                // Make buttons transparent (image only)
                hostButton.setOpaque(false);
                hostButton.setContentAreaFilled(false);
                hostButton.setBorderPainted(false);
                hostButton.setFocusPainted(false);

                joinButton.setOpaque(false);
                joinButton.setContentAreaFilled(false);
                joinButton.setBorderPainted(false);
                joinButton.setFocusPainted(false);

                Solo.setOpaque(false);
                Solo.setContentAreaFilled(false);
                Solo.setBorderPainted(false);
                Solo.setFocusPainted(false);

                bgLabel.add(hostButton);
                bgLabel.add(joinButton);
                bgLabel.add(Solo);

                Solo.addActionListener(ev3 -> {
                    String input = nameField.getText();
                    String playerName = "Player";
                    if (input != null && !input.isBlank() && !input.equals("Input name...")) {
                        playerName = input.trim();
                    }
                    final String finalName = playerName;
                    playFrame.dispose();

                    SwingUtilities.invokeLater(() -> new GameFrame(finalName));
                    if (playFrame != null) {
                        playFrame.dispose();
                    }
                });
            });

            gameCharacterBtn.addActionListener(evt -> {
                StopBugmain = true;
                if (mainFrame != null) {
                    mainFrame.dispose();
                }
                if (characterFrame != null && characterFrame.isDisplayable()) {
                    characterFrame.toFront();
                    return;
                }

                characterFrame = new JFrame("Character Selection");
                characterFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                characterFrame.setSize(500, 430);
                characterFrame.setLocationRelativeTo(null);
                characterFrame.setResizable(false);

                JLabel label = new JLabel("Character Selection", JLabel.CENTER);
                label.setFont(new Font("Arial", Font.BOLD, 20));
                characterFrame.add(label);

                JLabel bgLabel = new JLabel(bgIcon2);
                bgLabel.setLayout(new BorderLayout());
                characterFrame.setContentPane(bgLabel);

                JLabel labelch = new JLabel("Character Selection", JLabel.CENTER);
                labelch.setFont(new Font("Arial", Font.BOLD, 24));
                labelch.setForeground(Color.WHITE);
                labelch.add(label, BorderLayout.CENTER);

                characterFrame.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                        characterFrame = null;
                        // แสดงหน้า Start เฉพาะกรณี "ไม่ได้กำลังกดไปหน้าถัดไป"
                        if (!StopBugmain && mainFrame != null) {
                            mainFrame.setVisible(true);
                        }
                    }
                });

                characterFrame.setVisible(true);
            });

            gamePanel.add(gameCharacterBtn);
            gameFrame.add(gamePanel);
            gameFrame.pack();
            gameFrame.setLocationRelativeTo(null);

            gameFrame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                    gameFrame = null;
                    if (!StopBugmain && mainFrame != null) {
                        mainFrame.setVisible(true);
                    }
                }
            });

            gameFrame.setVisible(true);
        });

    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(bg, 0, 0, this);
    }

    private JButton createBackButton(JFrame currentFrame, JFrame previousFrame) {
        JButton backButton = new JButton("← Back");
        backButton.setFont(new Font("Arial", Font.BOLD, 18));
        backButton.setForeground(Color.WHITE);
        backButton.setBounds(15, 10, 100, 40);
        backButton.setFocusPainted(false);
        backButton.setContentAreaFilled(false);
        backButton.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        backButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                backButton.setForeground(new Color(255, 200, 200));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                backButton.setForeground(Color.WHITE);
            }
        });
        backButton.addActionListener(e -> {
            currentFrame.dispose();
            if (previousFrame != null)
                previousFrame.setVisible(true);
        });

        return backButton;
    }
}
