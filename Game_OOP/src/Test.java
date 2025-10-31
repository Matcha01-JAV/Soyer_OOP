import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;

import network.*;  // Add network package import

/**
 * เกม Soyer vs Zombies
 * - รับชื่อผู้เล่นจากฝั่ง Main (แสดงเหนือหัว)
 * - ยิงอัตโนมัติ / ซอมบี้เกิด / ชนแล้วตาย / รีสตาร์ทได้
 */
public class Test {
   /* public static void main(String[] args) {
        // รันเดี่ยว ๆ ให้มีชื่อเริ่มต้น "Player"
        SwingUtilities.invokeLater(() -> new GameFrame("Player"));
    }*/
}

/** เฟรมหลักของเกม */

/** พื้นที่หลักของเกม (Canvas) */
class GamePanel extends JPanel implements ActionListener {

    static final int WIDTH = 1262;
    static final int HEIGHT = 768;

    // ชื่อผู้เล่นที่รับมาจากฝั่ง Main
    String playerName;
    
    // Multiplayer support
    boolean isMultiplayer = false;
    GameClient gameClient = null;
    Map<String, Player> otherPlayers = new java.util.concurrent.ConcurrentHashMap<>();
    Map<String, Integer> playerScores = new java.util.concurrent.ConcurrentHashMap<>(); // Track scores for each player
    boolean isHostPlayer = false; // Track if this player is the host
    boolean allPlayersDead = false; // Track if all players are dead
    JButton nextButton = null; // Button for host to restart when all players are dead

    ImageIcon bgIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "game_map.png");
    Image bg = bgIcon.getImage();

    // ตัวจับเวลาเกม
    javax.swing.Timer gameTimer;
    javax.swing.Timer shootTimer;
    javax.swing.Timer zombieTimer;
    javax.swing.Timer syncTimer; // Timer for periodic synchronization

    Player player;
    List<Zombie> zombies;
    List<Bullet> bullets;
    Random random = new Random();

    int score = 0; // This player's score
    boolean gameOver = false;
    long lastPositionUpdate = 0; // Track last position update time

    // เผื่อเรียกแบบไม่ส่งชื่อ
    GamePanel() {
        this("Player");
    }

    // คอนสตรัคเตอร์หลัก: รับชื่อแล้วเก็บไว้
    GamePanel(String name) {
        this.playerName = (name == null || name.isBlank()) ? "Player" : name.trim();
        this.isMultiplayer = false;
        this.isHostPlayer = true; // Solo player is always the host
        initializeGame();
    }
    
    // Constructor for multiplayer game
    GamePanel(String name, GameClient client) {
        this.playerName = (name == null || name.isBlank()) ? "Player" : name.trim();
        this.gameClient = client;
        this.isMultiplayer = true;
        this.isHostPlayer = false; // Clients are not hosts by default
        initializeGame();
    }
    
    private void initializeGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        // สร้างผู้เล่น/ลิสต์
        player = new Player(300, 359); // Start in the middle of the road (359 is approximately center of road)
        zombies = new ArrayList<>();
        bullets = new ArrayList<>();
        
        // Initialize player scores
        playerScores.put(playerName, 0);

        // ยิงกระสุนทุก 0.5 วินาที
        shootTimer = new javax.swing.Timer(500, e -> shoot());
        shootTimer.start();

        // สุ่มสร้างซอมบี้ทุก 2 วินาที
        zombieTimer = new javax.swing.Timer(2000, e -> spawnZombie());
        zombieTimer.start();

        // อัปเดตเกม ~60 FPS
        gameTimer = new javax.swing.Timer(16, this);
        gameTimer.start();

        // ตัวจับเวลาสำหรับการ sync ข้อมูลทุก 100ms ในโหมด multiplayer (ลดความถี่เพื่อป้องกันการกระตุก)
        if (isMultiplayer && gameClient != null) {
            syncTimer = new javax.swing.Timer(100, e -> syncGameState());
            syncTimer.start();
            
            // Timer to check if all players are dead (every 1 second)
            javax.swing.Timer checkTimer = new javax.swing.Timer(1000, e -> {
                if (gameOver && areAllPlayersDead() && !allPlayersDead) {
                    allPlayersDead = true;
                    // When all players are dead, stop all timers
                    gameTimer.stop();
                    if (syncTimer != null) syncTimer.stop();
                    repaint();
                }
            });
            checkTimer.start();
        }

        // การควบคุมด้วยคีย์บอร์ด
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (gameOver) {
                    // Only allow restart in solo mode
                    if (e.getKeyCode() == KeyEvent.VK_SPACE && !isMultiplayer) {
                        restartGame();
                    }
                    return;
                }
                // เริ่มการเคลื่อนไหวเมื่อกดปุ่ม
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W)
                    player.startMoveUp();
                if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S)
                    player.startMoveDown();
                if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A)
                    player.startMoveLeft();
                if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D)
                    player.startMoveRight();
                
                // Send position update when movement starts (with timing control)
                long currentTime = System.currentTimeMillis();
                if (isMultiplayer && gameClient != null && (currentTime - lastPositionUpdate >= 50)) {
                    gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + (int)player.x + "," + (int)player.y);
                    lastPositionUpdate = currentTime;
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                if (gameOver) return;
                
                // หยุดการเคลื่อนไหวเมื่อปล่อยปุ่ม
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W)
                    player.stopMoveUp();
                if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S)
                    player.stopMoveDown();
                if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A)
                    player.stopMoveLeft();
                if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D)
                    player.stopMoveRight();
                
                // Send position update when movement stops (with timing control)
                long currentTime = System.currentTimeMillis();
                if (isMultiplayer && gameClient != null && (currentTime - lastPositionUpdate >= 50)) {
                    gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + (int)player.x + "," + (int)player.y);
                    lastPositionUpdate = currentTime;
                }
            }
        });
        
        // Set up network message listener for multiplayer mode
        if (isMultiplayer && gameClient != null) {
            gameClient.sendMessage("PLAYER_READY:" + playerName);
            // Send initial position to synchronize with other players
            gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + (int)player.x + "," + (int)player.y);
        }
    }

    /** ยิงกระสุน */
    void shoot() {
        if (gameOver) return;
        bullets.add(new Bullet((int)(player.x + player.size), (int)(player.y + player.size / 2 - 5)));
        
        // In multiplayer, send bullet information to other players
        if (isMultiplayer && gameClient != null) {
            gameClient.sendMessage("PLAYER_SHOOT:" + playerName + ":" + 
                (int)(player.x + player.size) + "," + (int)(player.y + player.size / 2 - 5));
        }
    }

    /** สุ่มเกิดซอมบี้ */
    void spawnZombie() {
        if (gameOver) return;
        // เลนถนนในแนว Y (ปรับให้ตรงภาพ)
        int roadTopY = 340;
        int roadBottomY = 700;
        int y = roadTopY + random.nextInt(Math.max(1, roadBottomY - roadTopY - 40));
        Zombie zombie = new Zombie(WIDTH - 50, y);
        zombies.add(zombie);
        
        // In multiplayer, send zombie information to other players
        if (isMultiplayer && gameClient != null) {
            gameClient.sendMessage("ZOMBIE_SPAWN:" + zombie.id + ":" + (WIDTH - 50) + "," + y + "," + zombie.speed);
        }
    }

    /** ซิงค์สถานะเกมกับผู้เล่นคนอื่น */
    void syncGameState() {
        if (!isMultiplayer || gameClient == null) return;
        
        // ส่งตำแหน่งซอมบี้ทั้งหมด
        StringBuilder zombiePositions = new StringBuilder("ZOMBIE_POSITIONS:");
        for (Zombie z : zombies) {
            zombiePositions.append(z.id).append(":").append(z.x).append(",").append(z.y).append(";");
        }
        if (zombiePositions.length() > "ZOMBIE_POSITIONS:".length()) {
            // ลบ ; ตัวสุดท้ายออก
            zombiePositions.setLength(zombiePositions.length() - 1);
            gameClient.sendMessage(zombiePositions.toString());
        }
        
        // ส่งคะแนนของผู้เล่นนี้
        gameClient.sendMessage("PLAYER_SCORE:" + playerName + ":" + score);
        
        // ส่งตำแหน่งผู้เล่นเสมอ เพื่อให้ตำแหน่งตรงกันทุกฝั่ง
        gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + (int)player.x + "," + (int)player.y);
        
        // ส่งสถานะผู้เล่น (ไม่รวมตำแหน่ง เพื่อป้องกันการกระตุก)
        gameClient.sendMessage(
                "PLAYER_STATE:" + playerName + ":" +
                        "0,0," + score + "," + !gameOver  // ใช้ 0,0 เพื่อไม่ให้มีการอัปเดตตำแหน่ง
        );

    }

    /** วาดทุกอย่าง */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(bg, 0, 0, this);

        // วาดผู้เล่น
        player.draw(g);

        // วาดผู้เล่นคนอื่นในโหมด multiplayer
        if (isMultiplayer) {
            // Create a safe copy to avoid ConcurrentModificationException
            Map<String, Player> safeOtherPlayers = new HashMap<>(otherPlayers);
            for (Map.Entry<String, Player> entry : safeOtherPlayers.entrySet()) {
                Player otherPlayer = entry.getValue();
                String otherPlayerName = entry.getKey();
                otherPlayer.draw(g);
                drawPlayerName((Graphics2D) g, otherPlayer, otherPlayerName);
            }
        }

        // วาดชื่อผู้เล่นเหนือหัว (← ตรงนี้คือชื่อจาก Main)
        drawPlayerName((Graphics2D) g);

        // วาดกระสุน (safe copy to avoid ConcurrentModificationException)
        List<Bullet> safeBullets = new ArrayList<>(bullets);
        for (Bullet b : safeBullets) b.draw(g);

        // วาดซอมบี้ (safe copy to avoid ConcurrentModificationException)
        List<Zombie> safeZombies = new ArrayList<>(zombies);
        for (Zombie z : safeZombies) z.draw(g);

        // คะแนน
        g.setColor(Color.WHITE);
        g.setFont(new Font("Tahoma", Font.BOLD, 20));
        g.drawString("Your Score: " + score, 20, 30);
        
        // แสดงคะแนนของผู้เล่นคนอื่นในโหมด multiplayer
        if (isMultiplayer) {
            int ys = 60;
            // Create a safe copy to avoid ConcurrentModificationException
            Map<String, Integer> safePlayerScores = new HashMap<>(playerScores);
            for (Map.Entry<String, Integer> entry : safePlayerScores.entrySet()) {
                if (!entry.getKey().equals(playerName)) {
                    if (entry.getValue() == -1) {
                        g.drawString(entry.getKey() + ": DIE ", 20, ys);
                    } else {
                        g.drawString(entry.getKey() + ": " + entry.getValue(), 20,ys );
                    }
                    ys += 30;
                }
            }
        }

        // Game Over
        if (gameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("Tahoma", Font.BOLD, 50));
            
            // Check if this is multiplayer mode
            if (isMultiplayer) {
                // Check if all players are dead
                if (areAllPlayersDead()) {
                    // All players are dead - show game over
                    g.drawString("GAME OVER", WIDTH / 2 - 150, HEIGHT / 2);
                    g.setFont(new Font("Tahoma", Font.BOLD, 20));
                    g.setColor(Color.YELLOW);
                    g.drawString("All players died. Return to main menu to play again.", WIDTH / 2 - 220, HEIGHT / 2 + 40);
                } else {
                    // This player died but others are still alive
                    g.drawString("YOU DIED", WIDTH / 2 - 120, HEIGHT / 2);
                    g.setFont(new Font("Tahoma", Font.BOLD, 20));
                    g.setColor(Color.YELLOW);
                    g.drawString("Waiting for other players to finish...", WIDTH / 2 - 180, HEIGHT / 2 + 40);
                }
            } else {
                // Solo mode - show restart option
                g.drawString("GAME OVER", WIDTH / 2 - 150, HEIGHT / 2);
                g.setFont(new Font("Tahoma", Font.BOLD, 20));
                g.setColor(Color.YELLOW);
                g.drawString("Press SPACE to restart", WIDTH / 2 - 100, HEIGHT / 2 + 40);
            }
        }
    }

    /** วาดชื่อผู้เล่นให้อยู่เหนือหัว */
    private void drawPlayerName(Graphics2D g2) {
        if (playerName == null || playerName.isBlank()) return;

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font font = new Font("Tahoma", Font.BOLD, 18);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();

        int textW = fm.stringWidth(playerName);
        int textH = fm.getAscent();

        // Position name above main player (using actual image dimensions: 60x75)
        int imageWidth = 60;  // Actual image width used in draw method
        int centerX = (int)(player.x + imageWidth / 2);
        int nameX = centerX - textW / 2;
        int nameY = (int)(player.y - 8);     // Position above the image with more space
        if (nameY - textH < 0) nameY = textH + 4;

        /*// กล่องพื้นหลังโปร่ง ๆ ให้อ่านง่าย
        int padX = 6, padY = 4;
        int bgX = nameX - padX;
        int bgY = nameY - textH - padY;
        int bgW = textW + padX * 2;
        int bgH = textH + padY * 2;

        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRoundRect(bgX, bgY, bgW, bgH, 10, 10);
        g2.setColor(new Color(255, 255, 255, 160));
        g2.drawRoundRect(bgX, bgY, bgW, bgH, 10, 10);*/

        g2.setColor(Color.WHITE);
        g2.drawString(playerName, nameX, nameY);
    }
    
    /** วาดชื่อผู้เล่นอื่นให้อยู่เหนือหัว */
    private void drawPlayerName(Graphics2D g2, Player player, String name) {
        if (player == null || name == null) return;

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font font = new Font("Tahoma", Font.BOLD, 18);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        
        int textW = fm.stringWidth(name);
        int textH = fm.getAscent();

        // All players use images with dimensions 60x75, so use consistent positioning
        int imageWidth = 60;  // Actual image width used in draw method
        int centerX = (int)(player.x + imageWidth / 2);
        int nameX = centerX - textW / 2;
        int nameY = (int)(player.y - 8);     // Position above player image with more space
        if (nameY - textH < 0) nameY = textH + 4;

        g2.setColor(Color.YELLOW);  // Different color for other players
        g2.drawString(name, nameX, nameY);
    }

    /** อัปเดตเกมแต่ละเฟรม */
    @Override
    public void actionPerformed(ActionEvent e) {
        // In multiplayer mode, continue updating even if this player is dead
        // until all players are dead
        if (gameOver && (!isMultiplayer || areAllPlayersDead())) {
            return;
        }

        // Only update player-specific things if this player is alive
        if (!gameOver) {
            // อัปเดตตำแหน่งผู้เล่นแบบต่อเนื่อง
            player.update();
        }

        // Send player position at consistent intervals (even when dead for synchronization)
        long currentTime = System.currentTimeMillis();
        if (isMultiplayer && gameClient != null && (currentTime - lastPositionUpdate >= 100)) {
            gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + (int)player.x + "," + (int)player.y);
            lastPositionUpdate = currentTime;
        }

        // อัปเดตตำแหน่ง (always update bullets and zombies so dead players can see the game)
        for (Bullet b : new ArrayList<>(bullets)) b.update();
        for (Zombie z : new ArrayList<>(zombies)) z.update();

        List<Bullet> bulletsToRemove = new ArrayList<>();
        List<Zombie> zombiesToRemove = new ArrayList<>();

        // ชนกระสุน-ซอมบี้ (always process bullet-zombie collisions)
        for (Bullet b : bullets) {
            Rectangle br = b.getBounds();
            for (Zombie z : zombies) {
                if (br.intersects(z.getBounds())) {
                    z.health -= b.damage;
                    bulletsToRemove.add(b);
                    if (z.health <= 0) {
                        zombiesToRemove.add(z);
                        // Only add score if this player is alive
                        if (!gameOver) {
                            score += 10;
                            // In multiplayer, send zombie killed information to other players
                            if (isMultiplayer && gameClient != null) {
                                gameClient.sendMessage("ZOMBIE_KILLED:" + z.id);
                                // Send updated score
                                gameClient.sendMessage("PLAYER_SCORE:" + playerName + ":" + score);
                            }
                        }
                    }
                }
            }
        }

        bullets.removeAll(bulletsToRemove);
        zombies.removeAll(zombiesToRemove);

        // Only check collisions if this player is alive
        if (!gameOver) {
            // ซอมบี้ชนผู้เล่น → จบเกม
            for (Zombie z : zombies) {
                if (z.getBounds().intersects(player.getBounds())) {
                    endGame();
                    break;
                }
            }

            // ซอมบี้เดินถึง x = 250 → จบเกม
            for (Zombie z : zombies) {
                if (z.x <= 250) {
                    endGame();
                    break;
                }
            }
        }

        // ลบกระสุนที่พ้นจอ
        bullets.removeIf(b -> b.x > WIDTH);

        repaint();
    }

    /** จบเกม */
    void endGame() {
        gameOver = true;
        
        // In solo mode, stop all timers immediately
        if (!isMultiplayer) {
            gameTimer.stop();
            shootTimer.stop();
            zombieTimer.stop();
            if (syncTimer != null) syncTimer.stop();
        } else {
            // In multiplayer mode, only stop player-specific timers
            // Keep gameTimer running so dead players can see the game continue
            shootTimer.stop(); // Stop shooting for this player
            zombieTimer.stop(); // Stop spawning zombies for this player
            // Keep syncTimer running to receive updates from other players
        }
        
        // In multiplayer mode, notify other players about game over
        if (isMultiplayer && gameClient != null) {
            // Send final position before marking as dead
            gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + (int)player.x + "," + (int)player.y);
            gameClient.sendMessage("PLAYER_DIED:" + playerName);
            // Mark this player as dead in the scores
            playerScores.put(playerName, -1);
        }
        
        // Remove Next button creation for multiplayer mode
        // Next button is only available in solo mode
    }
    
    private void createNextButton() {
        if (nextButton == null) {
            nextButton = new JButton("Next");
            nextButton.setFont(new Font("Tahoma", Font.BOLD, 20));
            nextButton.setBounds(WIDTH / 2 - 75, HEIGHT / 2 + 150, 150, 60);
            nextButton.setBackground(Color.GRAY);
            nextButton.setForeground(Color.WHITE);
            nextButton.setFocusPainted(false);
            nextButton.setEnabled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            nextButton.addActionListener(e -> {
                if (areAllPlayersDead()) {
                    if (isMultiplayer && gameClient != null) {
                        gameClient.sendMessage("HOST_RESTART");
                    }
                    revalidate();
                    repaint();
                    
                    // Show MVP screen
                    JFrame last = new JFrame("MVP");
                    last.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                    last.setResizable(false);
                    JLabel lbg = new JLabel(bgIcon);
                    lbg.setLayout(null);
                    lbg.setPreferredSize(new Dimension(bgIcon.getIconWidth(), bgIcon.getIconHeight()));
                    JPanel card = new JPanel(null);
                    card.setBackground(new Color(54,54,48));
                    card.setBounds(200, 120, 400, 300);
                    card.setBorder(BorderFactory.createLineBorder(new Color(35,34,29), 4, true));

                    JLabel title = new JLabel("MVP", SwingConstants.CENTER);
                    title.setFont(new Font("Arial", Font.BOLD, 48));
                    title.setForeground(Color.WHITE);
                    title.setBounds(0, 20, 400, 60);
                    card.add(title);

                    JLabel name = new JLabel("name: Player");
                    name.setFont(new Font("Arial", Font.PLAIN, 22));
                    name.setForeground(Color.WHITE);
                    name.setBounds(40, 100, 300, 40);
                    card.add(name);

                    JLabel score = new JLabel("Score: 1200");
                    score.setFont(new Font("Arial", Font.PLAIN, 22));
                    score.setForeground(Color.WHITE);
                    score.setBounds(40, 140, 300, 40);
                    card.add(score);

                    JLabel bottom = new JLabel("ตัวละคร", SwingConstants.CENTER);
                    bottom.setFont(new Font("Arial", Font.BOLD, 28));
                    bottom.setForeground(Color.WHITE);
                    bottom.setBounds(0, 220, 400, 50);
                    card.add(bottom);

                    last.pack();
                    last.setSize(bgIcon.getIconWidth(), bgIcon.getIconHeight());
                    last.setLocationRelativeTo(null);
                    last.setVisible(true);
                }
            });
            add(nextButton);
            setComponentZOrder(nextButton, 0);
        }
        repaint();
    }
    

    private boolean areAllPlayersDead() {
        // In solo mode, if this player is dead, game is over
        if (!isMultiplayer) {
            return gameOver;
        }
        
        // In multiplayer mode, check if all players are marked as dead (-1)
        for (Map.Entry<String, Integer> entry : playerScores.entrySet()) {
            // If any player is not marked as dead (-1), return false
            if (entry.getValue() != -1) {
                return false;
            }
        }
        return true;
    }

    void restartGame() {
        score = 0;
        gameOver = false;
        allPlayersDead = false;
        zombies.clear();
        bullets.clear();
        player = new Player(300, 359); // Start in the middle of the road
        
        // Reset player movement state for smooth movement
        player.movingUp = false;
        player.movingDown = false;
        player.movingLeft = false;
        player.movingRight = false;
        
        // Clear other players' scores but keep them in the game
        for (String playerName : playerScores.keySet()) {
            playerScores.put(playerName, 0);
        }
        
        // Remove the next button if it exists
        if (nextButton != null) {
            remove(nextButton);
            nextButton = null;
        }
        
        gameTimer.start();
        shootTimer.start();
        zombieTimer.start();
        if (isMultiplayer && gameClient != null) {
            if (syncTimer == null) {
                syncTimer = new javax.swing.Timer(100, e -> syncGameState());
            }
            if (!syncTimer.isRunning()) {
                syncTimer.start();
            }
        }
        requestFocusInWindow();
        repaint();
    }

    public void handleNetworkMessage(String message) {
        if (!isMultiplayer) return;
        
        try {
            if (message.startsWith("PLAYER_POSITION:")) {
                String[] parts = message.substring("PLAYER_POSITION:".length()).split(":");
                if (parts.length >= 2) {
                    String playerName = parts[0];
                    String[] coords = parts[1].split(",");
                    if (coords.length >= 2) {
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        
                        // Update or create player
                        Player otherPlayer = otherPlayers.get(playerName);
                        if (otherPlayer == null) {
                            // Create other players with different colors
                            Color[] playerColors = {Color.MAGENTA, Color.ORANGE, Color.YELLOW, Color.PINK, Color.LIGHT_GRAY};
                            Color playerColor = playerColors[otherPlayers.size() % playerColors.length];
                            otherPlayer = new Player(x, y, playerColor);
                            otherPlayers.put(playerName, otherPlayer);
                            playerScores.put(playerName, 0); // Initialize score for new player
                            // Send current player position to the new player
                            if (isMultiplayer && gameClient != null) {
                                gameClient.sendMessage("PLAYER_POSITION:" + this.playerName + ":" + player.x + "," + player.y);
                            }
                        } else {
                            // Improved position interpolation to prevent stuttering
                            double distance = Math.sqrt(Math.pow(x - otherPlayer.x, 2) + Math.pow(y - otherPlayer.y, 2));
                            
                            // If the distance is very large, teleport immediately
                            if (distance > 150) {
                                otherPlayer.x = x;
                                otherPlayer.y = y;
                            } else if (distance > 5) {
                                // Use stronger interpolation for medium distances
                                double lerpFactor = 0.3;
                                otherPlayer.x = otherPlayer.x + (x - otherPlayer.x) * lerpFactor;
                                otherPlayer.y = otherPlayer.y + (y - otherPlayer.y) * lerpFactor;
                            } else {
                                // For very small movements, update directly to avoid micro-stuttering
                                otherPlayer.x = x;
                                otherPlayer.y = y;
                            }
                        }
                    }
                }
            }  else if (message.startsWith("PLAYER_STATE:")) {
                // รูปแบบ: PLAYER_STATE:ชื่อผู้เล่น:x,y,score,isAlive
                String messageContent = message.substring("PLAYER_STATE:".length());
                int firstColon = messageContent.indexOf(':');
                if (firstColon > 0) {
                    String otherName = messageContent.substring(0, firstColon);
                    String stateData = messageContent.substring(firstColon + 1);
                    // ข้ามตัวเอง
                    if (!otherName.equals(playerName)) {
                        String[] fields = stateData.split(",");
                        if (fields.length >= 4) {
                            int x = Integer.parseInt(fields[0]);
                            int y = Integer.parseInt(fields[1]);
                            int sc = Integer.parseInt(fields[2]);
                            // รองรับทั้ง "1/0" และ "true/false"
                            boolean alive = "1".equals(fields[3]) || Boolean.parseBoolean(fields[3]);

                            Player op = otherPlayers.get(otherName);
                            if (op == null) {
                                // Create other players with different colors
                                Color[] playerColors = {Color.MAGENTA, Color.ORANGE, Color.YELLOW, Color.PINK, Color.LIGHT_GRAY};
                                Color playerColor = playerColors[otherPlayers.size() % playerColors.length];
                                op = new Player(x, y, playerColor);
                                otherPlayers.put(otherName, op);
                            } else {
                                // Don't update position here - use PLAYER_POSITION messages only
                                // This prevents conflicting position updates that cause stuttering
                            }
                            // เก็บคะแนน/สถานะอื่น ๆ
                            playerScores.put(otherName, sc);
                            // ถ้าจะใช้ alive ในอนาคต สามารถเก็บลง map แยกได้
                        }
                    }
                }
            }

            else if (message.startsWith("PLAYER_SHOOT:")) {
                String[] parts = message.substring("PLAYER_SHOOT:".length()).split(":");
                if (parts.length >= 2) {
                    String playerName = parts[0];
                    String[] coords = parts[1].split(",");
                    if (coords.length >= 2) {
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        bullets.add(new Bullet(x, y));
                    }
                }
            } else if (message.startsWith("ZOMBIE_SPAWN:")) {
                String[] parts = message.substring("ZOMBIE_SPAWN:".length()).split(":");
                if (parts.length >= 2) {
                    String zombieId = parts[0];
                    String[] coords = parts[1].split(",");
                    if (coords.length >= 3) { // x, y, speed
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        double speed = Double.parseDouble(coords[2]);
                        Zombie zombie = new Zombie(x, y, zombieId, speed);
                        // Store zombie with ID for proper synchronization
                        zombies.add(zombie);
                    } else if (coords.length >= 2) { // fallback for old format
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        Zombie zombie = new Zombie(x, y, zombieId);
                        // Store zombie with ID for proper synchronization
                        zombies.add(zombie);
                    }
                }
            } else if (message.startsWith("ZOMBIE_POSITIONS:")) {
                String[] zombieData = message.substring("ZOMBIE_POSITIONS:".length()).split(";");
                // สร้าง map ของซอมบี้ที่มีอยู่แล้ว
                Map<String, Zombie> existingZombies = new HashMap<>();
                for (Zombie z : zombies) {
                    existingZombies.put(z.id, z);
                }
                
                // ประมวลผลข้อมูลซอมบี้ที่ได้รับ
                for (String data : zombieData) {
                    String[] parts = data.split(":");
                    if (parts.length >= 3) {
                        String id = parts[0];
                        String[] coords = parts[1].split(",");
                        if (coords.length >= 2) {
                            int x = Integer.parseInt(coords[0]);
                            int y = Integer.parseInt(coords[1]);
                            // หาซอมบี้ที่มีอยู่แล้วหรือสร้างใหม่
                            Zombie zombie = existingZombies.get(id);
                            if (zombie == null) {
                                // สร้างซอมบี้ใหม่ถ้ายังไม่มี
                                zombie = new Zombie(x, y, id);
                                zombies.add(zombie);
                            }
                            // อัปเดตตำแหน่ง
                            zombie.x = x;
                            zombie.y = y;
                        }
                    }
                }
            } else if (message.startsWith("ZOMBIE_KILLED:")) {
                String zombieId = message.substring("ZOMBIE_KILLED:".length());
                // Remove zombie by ID for proper synchronization
                zombies.removeIf(z -> z.id.equals(zombieId));
            } else if (message.startsWith("PLAYER_SCORE:")) {
                String[] parts = message.substring("PLAYER_SCORE:".length()).split(":");
                if (parts.length >= 2) {
                    String playerName = parts[0];
                    int playerScore = Integer.parseInt(parts[1]);
                    playerScores.put(playerName, playerScore);
                }
            } else if (message.startsWith("GAME_START")) {
                // Game started by host
                gameOver = false;
                isHostPlayer = false; // When game starts, this client is not the host
                // Send current player position to other players
                if (isMultiplayer && gameClient != null) {
                    gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + player.x + "," + player.y);
                }
            } else if (message.startsWith("PLAYER_DIED:")) {
                // When another player dies, mark them as dead
                String deadPlayer = message.substring("PLAYER_DIED:".length());
                if (!deadPlayer.equals(playerName)) { // Don't process our own death message
                    playerScores.put(deadPlayer, -1); // Mark as dead with special score
                }
                
                // Check if all players are now dead and update display
                if (areAllPlayersDead()) {
                    allPlayersDead = true;
                }
                repaint();
                
                // In multiplayer mode, no restart functionality
                // Players need to return to main menu to play again
            } else if (message.startsWith("GAME_RESTART")) {
                // Restart functionality disabled in multiplayer mode
                // Players should return to main menu to play again
            } else if (message.startsWith("HOST_RESTART")) {
                // Restart functionality disabled in multiplayer mode
                // Players should return to main menu to play again
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing network message: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error handling network message: " + e.getMessage());
        }
    }



    public void setAsHost() {
        this.isHostPlayer = true;
    }

    private boolean isHost() {
        // In solo mode, the player is always the host
        if (!isMultiplayer) {
            return true;
        }
        // In multiplayer mode, check if this is the host player
        return isHostPlayer;
    }
    

    
}

/** คลาสผู้เล่น */
class Player {
    double x, y; // เปลี่ยนเป็น double เพื่อการเคลื่อนไหวที่นุ่มนวล
    int size = 60; // Increased size to better display player image
    double speed = 1.5; // ลดความเร็วลงเพื่อให้เคลื่อนไหวนุ่มนวลขึ้น
    Color playerColor = Color.CYAN; // สีของผู้เล่น (สำหรับผู้เล่นอื่น)
    boolean isMainPlayer = false; // ตัวแปรเพื่อแยกผู้เล่นหลักกับผู้เล่นอื่น
    
    // โหลดรูปผู้เล่น
    static ImageIcon playerIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "player1.png");
    static Image playerImage = playerIcon.getImage();
    
    // ตัวแปรสำหรับการเคลื่อนไหวแบบต่อเนื่อง
    boolean movingUp = false;
    boolean movingDown = false;
    boolean movingLeft = false;
    boolean movingRight = false;

    Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.isMainPlayer = true; // ผู้เล่นหลักใช้รูป player1.png
    }
    
    Player(int x, int y, Color color) {
        this.x = x;
        this.y = y;
        this.playerColor = color;
        this.isMainPlayer = true; // เปลี่ยนให้ผู้เล่นอื่นใช้รูปเหมือนกัน
    }

    void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        
        if (playerColor == Color.CYAN) {
            // Main player - draw normal image
            g.drawImage(playerImage, (int)x, (int)y, size, 75, null);
        } else {
            // Other players - draw image with color tint
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            g.drawImage(playerImage, (int)x, (int)y, size, 75, null);
            
            // Add colored overlay to distinguish players
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            g2.setColor(playerColor);
            g2.fillRect((int)x, (int)y, size, 75);
            
            // Reset composite
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }

    // เมธอดสำหรับเริ่มการเคลื่อนไหว
    void startMoveUp() { movingUp = true; }
    void startMoveDown() { movingDown = true; }
    void startMoveLeft() { movingLeft = true; }
    void startMoveRight() { movingRight = true; }
    
    // เมธอดสำหรับหยุดการเคลื่อนไหว
    void stopMoveUp() { movingUp = false; }
    void stopMoveDown() { movingDown = false; }
    void stopMoveLeft() { movingLeft = false; }
    void stopMoveRight() { movingRight = false; }

    // อัปเดตตำแหน่งแบบต่อเนื่อง
    void update() {
        if (movingUp && y > 340) {
            y -= speed;
        }
        if (movingDown && y < 640) {
            y += speed;
        }
        if (movingLeft && x > 250) {
            x -= speed;
        }
        if (movingRight && x < GamePanel.WIDTH - size) {
            x += speed;
        }
    }

    // เมธอดเก่าเพื่อความเข้ากันได้ (deprecated)
    void moveUp() { startMoveUp(); }
    void moveLeft() { startMoveLeft(); }
    void moveRight() { startMoveRight(); }
    void moveDown() { startMoveDown(); }

    Rectangle getBounds() {
        // Use actual image dimensions for collision detection
        return new Rectangle((int)x, (int)y, size, 75);
    }
}

/** คลาสซอมบี้ */
class Zombie {
    Random  rand = new Random();
    int x, y;
    int size = 50; // Increased size to better display zombie image
    double speed;
    String id; // Unique ID for synchronization

    int health = 30; // เลือดเริ่มต้นของซอมบี้
    
    // โหลดรูปซอมบี้
    static ImageIcon zombieIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "Zombie.png");
    static Image zombieImage = zombieIcon.getImage();

    Zombie(int x, int y) {
        this.x = x;
        this.y = y;
        this.speed = rand.nextDouble()*1+0.5;
        this.id = java.util.UUID.randomUUID().toString();
    }
    
    Zombie(int x, int y, String id) {
        this.x = x;
        this.y = y;
        this.speed = rand.nextDouble()*1+0.5;
        this.id = id;
    }
    
    Zombie(int x, int y, String id, double speed) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.id = id;
    }

    void draw(Graphics g) {
        // วาดรูปซอมบี้
        g.drawImage(zombieImage, x, y, size, 75, null);

        // แถบ HP
        g.setColor(Color.RED);
        g.fillRect(x, y - 10, size, 5);
        g.setColor(Color.GREEN);
        int hpBar = Math.max(0, (int) ((health / 30.0) * size));
        g.fillRect(x, y - 10, hpBar, 5);
    }

    void update() {
        x -= speed;
    }

    Rectangle getBounds() {
        return new Rectangle(x, y, size, size);
    }
}

/** คลาสกระสุน */
class Bullet {
    int x, y;
    int size = 20;
    int speed = 16;
    int damage = 10; // ดาเมจกระสุนนัดละ 10

    // โหลดรูปกระสุน
    static ImageIcon bulletIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "bullet.png");
    static Image bulletImage = bulletIcon.getImage();

    Bullet(int x, int y) {
        this.x = x;
        this.y = y;
    }

    void draw(Graphics g) {
        g.drawImage(bulletImage, x, y, size, size, null);
    }

    void update() {
        x += speed;
    }

    Rectangle getBounds() {
        return new Rectangle(x, y, size, size);
    }
}