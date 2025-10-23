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
    ImageIcon bgIcon = new ImageIcon(System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
            + File.separator + "game" + File.separator + "BG.png");
    Image bg = bgIcon.getImage();

    ImageIcon startIcon = new ImageIcon(System.getProperty("user.dir") + File.separator + "Game_OOP"+ File.separator + "src"
            + File.separator + "game" + File.separator + "Start.png");
    ImageIcon characterIcon = new ImageIcon(System.getProperty("user.dir") + File.separator + "Game_OOP"+ File.separator + "src"
            + File.separator + "game" + File.separator + "Character.png");
    JButton start = new JButton(startIcon);
    JButton character = new JButton(characterIcon);

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


        start.setLocation(startX, screenCenterY - 50);
        character.setLocation(characterX, screenCenterY - 50);

        // อย่าลืม add
        add(start);

        start.setLocation(startX, screenCenterY - 50); // START button in center

        // Add action listeners
        start.addActionListener(e -> {
            // Check if game frame is already open
            if (gameFrame != null && gameFrame.isDisplayable()) {
                gameFrame.toFront(); // Bring existing frame to front
                return;
            }

            // Hide main frame
            JFrame mainFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            mainFrame.setVisible(false);

            // Create new game frame with same background and character button
            gameFrame = new JFrame("Soyer VS Zombies - Game");
            gameFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            gameFrame.setResizable(false);

            // Create game panel with same background and character button
            JPanel gamePanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    g.drawImage(bg, 0, 0, this);
                }
            };
            gamePanel.setPreferredSize(new Dimension(bgIcon.getIconWidth(), bgIcon.getIconHeight()));
            gamePanel.setLayout(null);

            // Add character button to game frame
            JButton gameCharacterBtn = new JButton(characterIcon);
            gameCharacterBtn.setSize(characterIcon.getIconWidth(), characterIcon.getIconHeight());
            gameCharacterBtn.setOpaque(false);
            gameCharacterBtn.setContentAreaFilled(false);
            gameCharacterBtn.setBorderPainted(false);
            gameCharacterBtn.setFocusPainted(false);

            int gameCharacterX = (bgIcon.getIconWidth() / 2 - characterIcon.getIconWidth() / 2) + 200;
            int gameCharacterY = bgIcon.getIconHeight() / 2 - 50 ;
            gameCharacterBtn.setLocation(gameCharacterX, gameCharacterY);

            // Add character action to game frame button
            gameCharacterBtn.addActionListener(evt -> {
                if (characterFrame != null && characterFrame.isDisplayable()) {
                    characterFrame.toFront();
                    return;
                }

                gameFrame.setVisible(false);

                characterFrame = new JFrame("Character Selection");
                characterFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                characterFrame.setSize(500, 400);
                characterFrame.setLocationRelativeTo(null);
                characterFrame.setResizable(false);

                JLabel label = new JLabel("Character Selection", JLabel.CENTER);
                label.setFont(new Font("Arial", Font.BOLD, 20));
                characterFrame.add(label);

                characterFrame.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                        characterFrame = null;
                        gameFrame.setVisible(true);
                    }
                });

                characterFrame.setVisible(true);
            });

            gamePanel.add(gameCharacterBtn);
            gameFrame.add(gamePanel);
            gameFrame.pack();
            gameFrame.setLocationRelativeTo(null);

            // Add window listener to show main frame when game frame is closed
            gameFrame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                    gameFrame = null;
                    mainFrame.setVisible(true); // Show main frame again
                }
            });

            gameFrame.setVisible(true);
        });

        // Add only start button to main panel
        add(start);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(bg, 0, 0, this);
    }
}