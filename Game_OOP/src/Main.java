import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import network.*;

public class Main {
    public static void main(String[] args) {
        MainFrame frame = new MainFrame();
        MainPanel panel = new MainPanel();
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

class MainFrame extends JFrame {
    MainFrame() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        setTitle("Soyer VS Zombies");
    }
}

class MainPanel extends JPanel {

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
    private final ImageIcon chgril = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "Chgirl.png");

    public static String selectedCharacter = "male";

    private final JButton start = new JButton(startIcon);
    private final JButton character = new JButton(characterIcon);

    private JFrame joinLobby = null;
    private boolean StopBugmain = false;
    private JFrame characterFrame = null;
    private JFrame joinFrame = null;
    private JFrame hostFrame = null;
    private JFrame gameFrame = null;

    MainPanel() {
        setPreferredSize(new Dimension(bgIcon.getIconWidth(), bgIcon.getIconHeight()));
        setLayout(null);

        start.setSize(startIcon.getIconWidth(), startIcon.getIconHeight() - 97);
        character.setSize(characterIcon.getIconWidth(), characterIcon.getIconHeight());

        Button(start);
        Button(character);

        int screenCenterX = bgIcon.getIconWidth() / 2;
        int screenCenterY = bgIcon.getIconHeight() / 2;

        int startX = screenCenterX - startIcon.getIconWidth() / 2;
        int S = (screenCenterX - characterIcon.getIconWidth() / 2) + 200;

        int characterX = (screenCenterX - characterIcon.getIconWidth() / 2) + 200;
        character.setLocation(characterX, screenCenterY - 50);

        start.setLocation(startX - 15, getHeight() + 400);
        add(start);

        start.addActionListener(e -> {
            if (gameFrame != null && gameFrame.isDisplayable()) {
                gameFrame.toFront();
                return;
            }

            JFrame mainFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            if (mainFrame != null) {
                mainFrame.setVisible(false);
            }

            gameFrame = new JFrame("Soyer VS Zombies - Game");
            gameFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
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

            JButton backBtni = createBackButton(gameFrame, mainFrame);
            gamePanel.add(backBtni);

            JButton gameCharacterBtn = new JButton(characterIcon);
            gameCharacterBtn.setSize(characterIcon.getIconWidth(), characterIcon.getIconHeight() - 70);
            Button(gameCharacterBtn);
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
            nameField.setForeground(new Color(193, 193, 193));
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

            JButton okButton = new JButton(okIcon);
            Button(okButton);
            okButton.setSize(okIcon.getIconWidth(), okIcon.getIconHeight() - 90);
            okButton.setLocation(280, 450);

            gamePanel.add(gameCharacterBtn);
            gamePanel.add(nameField);
            nameField.setLocation(nameX, nameY);
            gamePanel.add(okButton);

            gameFrame.add(gamePanel);
            gameFrame.pack();
            gameFrame.setLocationRelativeTo(null);
            gameFrame.setVisible(true);

            gameCharacterBtn.addActionListener(et -> {
                StopBugmain = true;
                if (mainFrame != null) mainFrame.dispose();

                characterFrame = new JFrame("Character Selection");
                characterFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                characterFrame.setSize(1248, 850);
                characterFrame.setLocationRelativeTo(null);
                characterFrame.setResizable(false);

                JLabel bgLabel = new JLabel(bgIcon2);
                bgLabel.setLayout(null);
                characterFrame.setContentPane(bgLabel);

                ImageIcon soyerMaleIcon = new ImageIcon(
                        System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                                + File.separator + "game" + File.separator + "soyer1.png");
                JLabel soyerMaleLabel = new JLabel(soyerMaleIcon);
                soyerMaleLabel.setBounds(300, 300, soyerMaleIcon.getIconWidth(), soyerMaleIcon.getIconHeight());
                bgLabel.add(soyerMaleLabel);

                JLabel soyerMaleName = new JLabel("Soyer (Male)", JLabel.CENTER);
                soyerMaleName.setFont(new Font("Arial", Font.BOLD, 20));
                soyerMaleName.setForeground(Color.WHITE);
                soyerMaleName.setOpaque(true);
                soyerMaleName.setBackground(new Color(0, 0, 0, 150));
                soyerMaleName.setBounds(300 - 10, 300 + soyerMaleIcon.getIconHeight() + 10,
                        soyerMaleIcon.getIconWidth() + 20, 30);
                bgLabel.add(soyerMaleName);

                JButton selectSoyerBtn = new JButton("Select Soyer");
                selectSoyerBtn.setFont(new Font("Arial", Font.BOLD, 18));
                selectSoyerBtn.setBackground(new Color(76, 175, 80));
                selectSoyerBtn.setForeground(Color.WHITE);
                selectSoyerBtn.setFocusPainted(false);
                selectSoyerBtn.setBounds(300 + (soyerMaleIcon.getIconWidth() - 150) / 2,
                        300 + soyerMaleIcon.getIconHeight() + 50, 150, 40);
                selectSoyerBtn.addActionListener(ev -> {
                    MainPanel.selectedCharacter = "male";
                    if (characterFrame != null) {
                        characterFrame.dispose();
                        characterFrame = null;
                    }
                    if (gameFrame != null) {
                        gameFrame.setVisible(true);
                    }
                    ImageIcon male = new ImageIcon(
                            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                                    + File.separator + "game" + File.separator + "player1.png");
                });
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

                JButton backBtnC = createBackButton(characterFrame, gameFrame);
                bgLabel.add(backBtnC);

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

            okButton.addActionListener(ev -> {
                StopBugmain = true;
                if (characterFrame != null) {
                    characterFrame.dispose();
                    characterFrame = null;
                }
                if (gameFrame != null) {
                    gameFrame.dispose();
                }

                String name = nameField.getText();
                if ("Input name...".equals(name)) name = "";

                int randomNum = (int) (Math.random() * 9000) + 1000;
                final String playerName = (name == null || name.isBlank())
                        ? "Player" + "" + randomNum
                        : name.trim();

                JFrame playFrame = new JFrame("Soyer VS Zombies");
                playFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                playFrame.setResizable(false);

                JLabel bgLabel = new JLabel(bgIcon);
                bgLabel.setLayout(null);
                bgLabel.setPreferredSize(new Dimension(bgIcon.getIconWidth(), bgIcon.getIconHeight()));

                JButton backBtn1 = createBackButton(playFrame, gameFrame);
                bgLabel.add(backBtn1);

                JButton hostButton = new JButton(hostIcon);
                JButton joinButton = new JButton(joinIcon);
                JButton soloButton = new JButton(soloIcon);

                hostButton.setSize(hostIcon.getIconWidth(), hostIcon.getIconHeight() - 90);
                joinButton.setSize(joinIcon.getIconWidth(), joinIcon.getIconHeight() - 90);
                soloButton.setSize(soloIcon.getIconWidth(), soloIcon.getIconHeight() - 90);

                int centerX = bgIcon.getIconWidth() / 2;
                int centerY = bgIcon.getIconHeight() / 2;
                hostButton.setLocation(centerX - hostButton.getWidth() / 2, centerY - 100);
                joinButton.setLocation(centerX - joinButton.getWidth() / 2, centerY);
                soloButton.setLocation(centerX - soloButton.getWidth() / 2, centerY + 100);

                Button(hostButton);
                Button(joinButton);
                Button(soloButton);

                bgLabel.add(hostButton);
                bgLabel.add(joinButton);

                playFrame.setContentPane(bgLabel);
                playFrame.pack();
                playFrame.setLocationRelativeTo(null);
                playFrame.setVisible(true);

                hostButton.addActionListener(ev1 -> {
                    StopBugmain = true;
                    playFrame.dispose();

                    hostFrame = new JFrame("Soyer VS Zombies");
                    hostFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                    hostFrame.setResizable(false);

                    JLabel bgLabelHost = new JLabel(bgIcon);
                    bgLabelHost.setLayout(null);
                    hostFrame.setContentPane(bgLabelHost);

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

                    JButton openPortBtn = new JButton(hostIcon);
                    Button(openPortBtn);
                    openPortBtn.setSize(200, 60);
                    openPortBtn.setLocation(portField.getX() + portField.getWidth() + 20, portField.getY());
                    bgLabelHost.add(openPortBtn);

                    JButton backBtnHost = createBackButton(hostFrame, playFrame);
                    bgLabelHost.add(backBtnHost);

                    hostFrame.pack();
                    hostFrame.setSize(bgIcon.getIconWidth(), bgIcon.getIconHeight());
                    hostFrame.setLocationRelativeTo(null);
                    hostFrame.setVisible(true);

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
                                server.start();
                            } catch (Exception ey) {
                                System.err.println("Server error: " + ey.getMessage());
                                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                                        "Failed to start server: " + ey.getMessage(),
                                        "Server Error", JOptionPane.ERROR_MESSAGE));
                            }
                        }).start();

                        JFrame lobby = new JFrame("Soyer VS Zombies - Lobby");
                        lobby.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                        lobby.setResizable(false);
                        JLabel bgLobby = new JLabel(bgIcon);
                        bgLobby.setLayout(null);
                        lobby.setContentPane(bgLobby);

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

                        model.addElement(playerName + " (Host)");

                        lobby.getRootPane().putClientProperty("server", server);

                        JButton backBtn5 = createBackButton(lobby, playFrame);
                        backBtn5.addActionListener(eh -> {
                            server.stop();
                            lobby.dispose();
                        });
                        bgLobby.add(backBtn5);

                        JButton startGameBtn = new JButton(startIcon);
                        Button(startGameBtn);
                        int btnW = 200, btnH = 60;
                        startGameBtn.setSize(btnW, btnH);
                        int btnY = playersPanel.getY() + playersPanel.getHeight();
                        startGameBtn.setLocation((bgIcon.getIconWidth() - btnW) / 2, btnY);
                        bgLobby.add(startGameBtn);

                        startGameBtn.addActionListener(ed -> {

                            GameServer serverVar = (GameServer) lobby.getRootPane().getClientProperty("server");
                            if (serverVar != null)
                            {
                                serverVar.startGame();
                            }
                            lobby.dispose();

                            GameClient hostClient = new GameClient(playerName, message -> {});

                            SwingUtilities.invokeLater(() -> {
                                if (hostClient.connect("localhost", port)) {
                                    GameFrame frame = new GameFrame(playerName, hostClient,
                                            MainPanel.selectedCharacter);

                                    hostClient.setMessageListener(msg -> {
                                        GamePanel panel = frame.getGamePanel();
                                        if (panel != null) {
                                            panel.handleNetworkMessage(msg);
                                        }
                                    });

                                    GamePanel panel = frame.getGamePanel();
                                    if (panel != null) {
                                        panel.setAsHost();
                                    }
                                } else {
                                    new GameFrame(playerName, MainPanel.selectedCharacter);
                                }
                            });
                        });

                        lobby.pack();
                        lobby.setSize(bgIcon.getIconWidth(), bgIcon.getIconHeight());
                        lobby.setLocationRelativeTo(null);
                        lobby.setVisible(true);

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

                        lobby.addWindowListener(new WindowAdapter() {
                            @Override
                            public void windowClosed(WindowEvent e) {
                                pollThread.interrupt();
                            }
                        });
                    });
                });

                joinButton.addActionListener(ev2 -> {
                    StopBugmain = true;
                    playFrame.dispose();

                    joinFrame = new JFrame("Soyer VS Zombies");
                    joinFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                    joinFrame.setResizable(false);

                    JLabel bgLabelJoin = new JLabel(bgIcon);
                    bgLabelJoin.setLayout(null);
                    joinFrame.setContentPane(bgLabelJoin);

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

                    JButton joinConfirm = new JButton(joinIcon);
                    Button(joinConfirm);
                    joinConfirm.setSize(200, 60);
                    joinConfirm.setLocation(ipField.getX() + ipField.getWidth() + 20, ipField.getY());
                    bgLabelJoin.add(joinConfirm);

                    JButton backBtnj = createBackButton(joinFrame, playFrame);
                    bgLabelJoin.add(backBtnj);

                    joinConfirm.addActionListener(ej -> {
                        String ipPort = ipField.getText();
                        if (ipPort == null || ipPort.isEmpty() || ipPort.equals("INPUT HOST IP")) {
                            JOptionPane.showMessageDialog(joinFrame, "Please enter a valid IP address and port",
                                    "Invalid Address", JOptionPane.ERROR_MESSAGE);
                            return;
                        }

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
                                    SwingUtilities.invokeLater(() -> {
                                        GameFrame frame = new GameFrame(selfName, clientHolder[0],
                                                MainPanel.selectedCharacter);
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

                        if (clientHolder[0].connect(host, port)) {
                            joinFrame.dispose();

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

                            JButton backBtn = createBackButton(lobby, playFrame);
                            backBtn.addActionListener(e2 -> {
                                client.disconnect();
                                lobby.dispose();
                            });
                            bgLobby.add(backBtn);

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

    private void Button(AbstractButton b) {
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
        backButton.setBounds(15, 10, 120, 40);
        backButton.setFocusPainted(false);
        backButton.setContentAreaFilled(false);
        backButton.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        backButton.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { backButton.setForeground(new Color(255, 200, 200)); }
            @Override public void mouseExited (MouseEvent e) { backButton.setForeground(Color.WHITE); }
        });

        backButton.addActionListener(e -> {
            currentFrame.dispose();
            if (previousFrame != null) {
                previousFrame.setVisible(true);
            }
        });

        return backButton;
    }
}
