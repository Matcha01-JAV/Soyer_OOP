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
                    + File.separator + "game" + File.separator + "Start.png");
    ImageIcon characterIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "Character.png");
    ImageIcon okIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "OK.png");
    JButton start = new JButton(startIcon);
    JButton character = new JButton(characterIcon);
    private boolean StopBugmain = false;
    private JFrame characterFrame = null; // Track character frame
    private JFrame gameFrame = null; // Track game frame

    MainPanel() {
        setPreferredSize(new Dimension(bgIcon.getIconWidth(), bgIcon.getIconHeight()));
        setLayout(null); // Use absolute positioning

        // Set button sizes to match their images
        start.setSize(startIcon.getIconWidth() - 20, startIcon.getIconHeight() - 70);
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

        start.setLocation(startX, getHeight() + 400);

        // Add action listeners
        start.addActionListener(e -> {
            if (gameFrame != null && gameFrame.isDisplayable()) {
                gameFrame.toFront();
                return;
            }

            JFrame mainFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            mainFrame.setVisible(false);

            gameFrame = new JFrame("Soyer VS Zombies - Game");
            gameFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            gameFrame.setResizable(false);

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
            gameCharacterBtn.setSize(characterIcon.getIconWidth() - 20, characterIcon.getIconHeight() - 60);
            gameCharacterBtn.setOpaque(false);
            gameCharacterBtn.setContentAreaFilled(false);
            gameCharacterBtn.setBorderPainted(false);
            gameCharacterBtn.setFocusPainted(false);

            int gameCharacterX = (bgIcon.getIconWidth() / 2 - characterIcon.getIconWidth() / 2) + 200;
            int gameCharacterY = bgIcon.getIconHeight() / 2 + 70;
            gameCharacterBtn.setLocation(gameCharacterX, gameCharacterY);

            int textWidth = 220, textHeight = 40;
            JTextField nameField = new JTextField("Input name...");
            nameField.setFont(new Font("Arial", Font.BOLD, 20));
            nameField.setHorizontalAlignment(JTextField.CENTER);
            nameField.setSize(350, 60);
            nameField.setBackground(new Color(142, 104, 84, 255));
            nameField.setForeground(new Color(0, 0, 0));
            nameField.setOpaque(true);
            nameField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(85, 85, 85), 3, true),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
            ));
            nameField.setCaretColor(new Color(100, 149, 237));
            nameField.setSelectionColor(new Color(173, 216, 230));

            int nameX = characterX - textWidth - 140;
            int nameY = screenCenterY - textHeight / 2 + 108;

            nameField.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent evt) {
                    if (nameField.getText().equals("Input name...")) {
                        nameField.setText("");
                        nameField.setForeground(new Color(0, 0, 0));
                    }
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent evt) {
                    if (nameField.getText().isEmpty()) {
                        nameField.setText("Input name...");
                        nameField.setForeground(new Color(0, 0, 0));
                    }
                }
            });
            nameField.setForeground(new Color(0, 0, 0));

            JButton okButton = new JButton(okIcon);
            okButton.setSize(okIcon.getIconWidth(), okIcon.getIconHeight());
            okButton.setOpaque(false);
            okButton.setContentAreaFilled(false);
            okButton.setBorderPainted(false);
            okButton.setFocusPainted(false);
            okButton.setBounds(300, 400, okIcon.getIconWidth(), okIcon.getIconHeight());

            gamePanel.add(okButton);
            nameField.setLocation(nameX, nameY);
            gamePanel.add(nameField);

            okButton.addActionListener(ev -> {
                StopBugmain = true;
                if (characterFrame != null)
                {
                    characterFrame.dispose();
                    characterFrame = null;
                }
                if (gameFrame != null)
                {
                    gameFrame.setVisible(false);
                }

                String name = nameField.getText();
                if ("Input name...".equals(name))
                {
                    name = "";
                }

                JFrame playFrame = new JFrame("Soyer VS Zombies");
                playFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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

                JButton hostButton = new JButton("Host");
                JButton joinButton = new JButton("Join");
                JButton StartSolo = new JButton("StartSolo");

                int btnWidth = startIcon.getIconWidth() - 25;
                int btnHeight = startIcon.getIconHeight() - 75;

                hostButton.setSize(btnWidth, btnHeight);
                joinButton.setSize(btnWidth, btnHeight);
                StartSolo.setSize(btnWidth, btnHeight);

                int centerX = bgIcon.getIconWidth() / 2 - btnWidth / 2;
                int centerY = bgIcon.getIconHeight() / 2;

                hostButton.setLocation(centerX, centerY - 100);
                joinButton.setLocation(centerX, centerY + 20);
                StartSolo.setLocation(centerX, centerY + 140);


                hostButton.setOpaque(true);
                hostButton.setContentAreaFilled(true);
                hostButton.setBorderPainted(false);
                hostButton.setFocusPainted(false);

                joinButton.setOpaque(true);
                joinButton.setContentAreaFilled(true);
                joinButton.setBorderPainted(false);
                joinButton.setFocusPainted(false);

                StartSolo.setOpaque(true);
                StartSolo.setContentAreaFilled(true);
                StartSolo.setBorderPainted(false);
                StartSolo.setFocusPainted(false);

                bgLabel.add(hostButton);
                bgLabel.add(joinButton);
                bgLabel.add(StartSolo);

                StartSolo.addActionListener(ev3 -> {
                    String input = nameField.getText();
                    String playerName = "Player";
                    if (input != null && !input.isBlank() && !input.equals("Input name...")) {
                        playerName = input.trim();
                    }
                    final String finalName = playerName;
                    playFrame.dispose();

                    SwingUtilities.invokeLater(() -> new GameFrame(finalName));
                    if (playFrame != null)
                    {
                        playFrame.dispose();
                    }
                });
            });

            gameCharacterBtn.addActionListener(evt -> {
                StopBugmain = true;
                if (mainFrame != null)
                {
                    mainFrame.dispose();
                }
                if (characterFrame != null && characterFrame.isDisplayable()) {
                    characterFrame.toFront();
                    return;
                }

                characterFrame = new JFrame("Character Selection");
                characterFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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
            @Override public void mouseEntered(java.awt.event.MouseEvent e)
            {
                backButton.setForeground(new Color(255, 200, 200));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e)
            { backButton.setForeground(Color.WHITE);
            }
        });
        backButton.addActionListener(e -> {
            currentFrame.dispose();
            if (previousFrame != null) previousFrame.setVisible(true);
        });

        return backButton;
    }
}
