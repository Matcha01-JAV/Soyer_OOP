import javax.swing.*;
import java.awt.*;
import java.io.File;
import network.*;  // ใช้ GameClient/GameServer

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
    // โหลดรูป
    private final ImageIcon bgIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "newbg.png");
    private final Image bg = bgIcon.getImage();

    private final ImageIcon bgIcon2 = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "ch.png");

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

    // ปุ่มหน้าแรก
    private final JButton start = new JButton(startIcon);
    private final JButton character = new JButton(characterIcon);

    // สถานะ/หน้าต่าง
    private JFrame joinLobby = null;
    private boolean StopBugmain = false;
    private JFrame characterFrame = null;
    private JFrame joinFrame = null;
    private JFrame hostFrame = null;
    private JFrame gameFrame = null;

    MainPanel() {
        setPreferredSize(new Dimension(bgIcon.getIconWidth(), bgIcon.getIconHeight()));
        setLayout(null);

        // ขนาด/สไตล์ปุ่ม
        start.setSize(startIcon.getIconWidth(), startIcon.getIconHeight());
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
            // ชื่อผู้เล่น
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
            int nameX = characterX - textWidth - 140;
            int nameY = screenCenterY - textHeight / 2 + 108;

            gamePanel.add(nameField);
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

                if (mainFrame != null) {
                    mainFrame.dispose();
                }
                if (characterFrame != null && characterFrame.isDisplayable()) {
                    characterFrame.toFront();
                    return;
                }

                characterFrame = new JFrame("Character Selection");
                characterFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                characterFrame.setSize(500, 430);
                characterFrame.setLocationRelativeTo(null);
                characterFrame.setResizable(false);

                // พื้นหลัง + Layout
                JLabel bgLabel = new JLabel(bgIcon2);
                bgLabel.setLayout(new BorderLayout());
                characterFrame.setContentPane(bgLabel);

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
            okButton.addActionListener(ev -> {
                StopBugmain = true;
                if (characterFrame != null) {
                    characterFrame.dispose();
                    characterFrame = null;
                }
                if (gameFrame != null) {
                    gameFrame.dispose(); // ใช้ dispose ให้ windowClosed ทำงาน
                }

                String name = nameField.getText();
                if ("Input name...".equals(name)) {
                    name = "";
                }
                int randomNum = (int) (Math.random() * 9000) + 1000;
                final String playerName;
                if (name == null || name.isBlank()) {
                    playerName = "Player" + randomNum;
                } else {
                    playerName = name.trim() + randomNum;
                }
                // Create a final variable for the clean name to use in lambda expressions
                final String cleanPlayerName;
                if (name == null || name.isBlank()) {
                    cleanPlayerName = "Player";
                } else {
                    cleanPlayerName = name.trim();
                }

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
                bgLabel.add(soloButton);

                playFrame.setContentPane(bgLabel);
                playFrame.pack();
                playFrame.setLocationRelativeTo(null);
                playFrame.setVisible(true);

                String finalName = name;
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
                    portField.addFocusListener(new java.awt.event.FocusAdapter() {
                        @Override
                        public void focusGained(java.awt.event.FocusEvent evt) {
                            if (portField.getText().equals("INPUT PORT 1025 - 65535"))
                            {
                                portField.setText("");
                                portField.setForeground(new Color(210, 188, 148));
                            }
                        }

                        @Override
                        public void focusLost(java.awt.event.FocusEvent evt) {
                            if (portField.getText().isEmpty())
                            {
                                portField.setText("INPUT PORT 1025 - 65535");
                                portField.setForeground(new Color(210, 188, 148));
                            }
                        }
                    });
                    portField.setSize(400, 60);
                    portField.setLocation((bgIcon.getIconWidth() - portField.getWidth()) / 2 - 80, (bgIcon.getIconHeight() / 2) - 40);
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
                                JOptionPane.showMessageDialog(hostFrame, "Port must be between 1024 and 65535", "Invalid Port", JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(hostFrame, "Please enter a valid port number", "Invalid Port", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        hostFrame.dispose();

                        GameServer server = new GameServer(port);
                        new Thread(() -> {
                            try {
                                server.start();
                            } catch (Exception ey) {
                                System.err.println("Server error: " + ey.getMessage());
                                SwingUtilities.invokeLater(() ->
                                        JOptionPane.showMessageDialog(null, "Failed to start server: " + ey.getMessage(),
                                                "Server Error", JOptionPane.ERROR_MESSAGE)
                                );
                            }
                        }).start();

                        // Lobby ฝั่ง Host
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
                        } catch (Exception ignore) {
                        }

                        JPanel playersPanel = new JPanel(null);
                        playersPanel.setBackground(new Color(54, 54, 48, 220));
                        playersPanel.setBounds(70, 130, 600, 330);
                        bgLobby.add(playersPanel);

                        JLabel playersTitle = new JLabel("Players");
                        playersTitle.setFont(new Font("Arial", Font.BOLD, 42));
                        playersTitle.setForeground(new Color(193, 193, 193));
                        playersTitle.setBounds(18, 8, 400, 50);
                        playersPanel.add(playersTitle);

                        DefaultListModel<String> model = new DefaultListModel<>();
                        JList<String> playerList = new JList<>(model);

                        playerList.setFont(new Font("Arial", Font.PLAIN, 24));
                        playerList.setForeground(new Color(220, 220, 220));
                        playerList.setOpaque(false);

                        JScrollPane scroll = new JScrollPane(playerList);
                        scroll.setOpaque(false);
                        scroll.getViewport().setOpaque(false);
                        scroll.setBorder(BorderFactory.createEmptyBorder());
                        scroll.setBounds(18, 60, 560, 250);
                        playersPanel.add(scroll);

                        // Use clean player name for display
                        String displayName = finalName; // This is the original name without random number
                        if (displayName == null || displayName.isBlank()) {
                            displayName = "Player";
                        }
                        model.addElement(displayName + " (Host)");

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

                        startGameBtn.addActionListener(ed -> {
                            GameServer serverVar = (GameServer) lobby.getRootPane().getClientProperty("server");
                            if (serverVar != null) serverVar.startGame();
                            lobby.dispose();
                            
                            // Create a GameClient for the host to participate in the multiplayer game
                            GameClient hostClient = new GameClient(playerName, message -> {
                                // The host will receive its own messages through the server loopback
                                // In a real implementation, we would handle this properly
                            });
                            
                            // Connect the host client to its own server
                            SwingUtilities.invokeLater(() -> {
                                if (hostClient.connect("localhost", port)) {
                                    GameFrame frame = new GameFrame(cleanPlayerName, hostClient);
                                    // Set up message forwarding to the game panel
                                    hostClient.setMessageListener(msg -> {
                                        GamePanel panel = frame.getGamePanel();
                                        if (panel != null) {
                                            panel.handleNetworkMessage(msg);
                                        }
                                    });
                                } else {
                                    // Fallback to solo mode if connection fails
                                    new GameFrame(cleanPlayerName);
                                }
                            });
                        });

                        bgLobby.add(startGameBtn);
                        lobby.pack();
                        lobby.setSize(bgIcon.getIconWidth(), bgIcon.getIconHeight());
                        lobby.setLocationRelativeTo(null);
                        lobby.setVisible(true);
                        javax.swing.Timer poll = new javax.swing.Timer(500, er -> {
                            try {
                                java.util.List<String> names = server.getPlayerNames();
                                model.clear();
                                model.addElement(cleanPlayerName + " (Host)");
                                for (String n : names) {
                                    if (n != null && !n.isBlank() && !n.equals(playerName)) {
                                        model.addElement(n);
                                    }
                                }
                            } catch (Exception ignore) {
                            }
                        });
                        poll.start();


                        lobby.addWindowListener(new java.awt.event.WindowAdapter() {
                            @Override
                            public void windowClosed(java.awt.event.WindowEvent e) {
                                poll.stop();
                            }
                        });
                    });
                });

                // === Join ===
                String finalName1 = name;
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
                    ipField.addFocusListener(new java.awt.event.FocusAdapter() {
                        @Override
                        public void focusGained(java.awt.event.FocusEvent evt) {
                            if (ipField.getText().equals("INPUT HOST IP")) {
                                ipField.setText("");
                                ipField.setForeground(new Color(210, 188, 148));
                            }
                        }

                        @Override
                        public void focusLost(java.awt.event.FocusEvent evt) {
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

                    // ยืนยัน Join
                    joinConfirm.addActionListener(ej -> {
                        String ipPort = ipField.getText();
                        if (ipPort == null || ipPort.isEmpty() || ipPort.equals("INPUT HOST IP")) {
                            JOptionPane.showMessageDialog(joinFrame, "Please enter a valid IP address and port",
                                    "Invalid Address", JOptionPane.ERROR_MESSAGE);
                            return;
                        }

                        String[] parts = ipPort.split(":");
                        String host = parts[0];
                        int port = 8080; // default
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
                        final DefaultListModel<String>[] modelRef = new DefaultListModel[]{null};
                        final String selfName = cleanPlayerName; // Use clean name for display

                        clientHolder[0] = new GameClient(pn, message -> {
                            SwingUtilities.invokeLater(() -> {
                                if (message.startsWith("PLAYER_LIST:")) {
                                    String[] players = message.substring("PLAYER_LIST:".length()).split(",");
                                    modelRef[0].clear();
                                    for (String player : players) {
                                        if (!player.isEmpty()) {
                                            modelRef[0].addElement(player);
                                        }
                                    }
                                } else if (message.startsWith("PLAYER_JOINED:")) {
                                    String player = message.substring("PLAYER_JOINED:".length());
                                    if (!modelRef[0].contains(player)) {
                                        modelRef[0].addElement(player);
                                    }
                                } else if (message.startsWith("PLAYER_LEFT:")) {
                                    String player = message.substring("PLAYER_LEFT:".length());
                                    modelRef[0].removeElement(player);
                                } else if (message.startsWith("GAME_START")) {
                                    joinFrame.dispose();
                                    SwingUtilities.invokeLater(() -> {
                                        GameFrame frame = new GameFrame(selfName, clientHolder[0]);
                                        // Set up message forwarding to the game panel
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

                            // สร้าง Lobby ของฝั่ง Join
                            final GameClient client = clientHolder[0];

                            // <<==== วางตรงนี้ ====>>>
                            DefaultListModel<String> model = new DefaultListModel<>();
                            modelRef[0] = model;
                            model.addElement(cleanPlayerName + " (You)"); // Use clean name for display

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
                            playersTitle.setBounds(130,130,400,50);
                            playersPanel.add(playersTitle);

                            JButton backBtn = createBackButton(lobby, playFrame);
                            backBtn.addActionListener(e2 -> {
                                client.disconnect();
                                lobby.dispose();
                            });
                            bgLobby.add(backBtn);

                            lobby.addWindowListener(new java.awt.event.WindowAdapter() {
                                @Override public void windowClosed(java.awt.event.WindowEvent e3) {
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

                soloButton.addActionListener(ev3 -> {
                    playFrame.dispose();
                    SwingUtilities.invokeLater(() -> new GameFrame(cleanPlayerName));
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
            if (previousFrame != null) {
                previousFrame.setVisible(true);
            }
        });

        return backButton;
    }
}

