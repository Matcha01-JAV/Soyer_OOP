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
                    + File.separator + "game" + File.separator + "BG.png");
    Image bg = bgIcon.getImage();

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
            int gameCharacterY = bgIcon.getIconHeight() / 2 - 50;
            gameCharacterBtn.setLocation(gameCharacterX, gameCharacterY);

            int textWidth = 220, textHeight = 40;
            JTextField nameField = new JTextField("Input name...");

            // Enhanced text field styling
            nameField.setFont(new Font("Arial", Font.BOLD, 20));
            nameField.setHorizontalAlignment(JTextField.CENTER);
            nameField.setSize(350, 60);

            // Beautiful styling
            nameField.setBackground(new Color(255, 255, 255, 230)); // Semi-transparent white
            nameField.setForeground(new Color(50, 50, 50)); // Dark gray text
            nameField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createRaisedBevelBorder(), // Outer raised border
                    BorderFactory.createEmptyBorder(8, 15, 8, 15) // Inner padding
            ));

            // Add rounded corners effect with custom border
            nameField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(100, 149, 237), 3, true), // Cornflower blue border
                    BorderFactory.createEmptyBorder(10, 15, 10, 15) // Inner padding
            ));

            // Add focus styling
            nameField.setCaretColor(new Color(100, 149, 237)); // Blue caret
            nameField.setSelectionColor(new Color(173, 216, 230)); // Light blue selection

            int nameX = characterX - textWidth - 140; // Adjusted for new width
            int nameY = screenCenterY - textHeight / 2 + 8;

            // Add placeholder text behavior
            nameField.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent evt) {
                    if (nameField.getText().equals("Input name...")) {
                        nameField.setText("");
                        nameField.setForeground(new Color(50, 50, 50));
                    }
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent evt) {
                    if (nameField.getText().isEmpty()) {
                        nameField.setText("Input name...");
                        nameField.setForeground(new Color(150, 150, 150));
                    }
                }
            });

            // Set initial placeholder color
            nameField.setForeground(new Color(150, 150, 150));

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