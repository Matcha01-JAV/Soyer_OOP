import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;

import network.*;  // Add network package import

class GamePanel extends JPanel {

    static final int WIDTH = 1262;
    static final int HEIGHT = 768;

    // ชื่อผู้เล่นที่รับมาจากฝั่ง Main
    String playerName;
    String characterType = "male"; // Character type: "male" or "female"
    
    // Multiplayer support
    boolean isMultiplayer = false;
    GameClient gameClient = null;
    Map<String, Player> otherPlayers = new java.util.concurrent.ConcurrentHashMap<>();
    Map<String, Integer> playerScores = new java.util.concurrent.ConcurrentHashMap<>(); // Track scores for each player
    Map<String, String> playerCharacters = new java.util.concurrent.ConcurrentHashMap<>(); // Track character types for each player
    boolean isHostPlayer = false; // Track if this player is the host
    boolean allPlayersDead = false; // Track if all players are dead
    String winnerName = null; // Track the winner's name for MVP display
    JButton nextButton = null; // Button for host to restart when all players are dead

    ImageIcon bgIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "game_map.png");
    Image bg = bgIcon.getImage();

    // เธรดสำหรับควบคุมเกม
    Thread gameThread;
    Thread shootThread;
    Thread zombieThread;
    Thread syncThread; // Thread for periodic synchronization
    Thread checkThread; // Thread to check game state
    
    // ตัวแปรควบคุมเธรด
    volatile boolean gameRunning = false;
    volatile boolean shootingActive = false;
    volatile boolean zombieSpawningActive = false;
    volatile boolean syncActive = false;
    volatile boolean checkActive = false;

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
        this(name, "male"); // default to male character
    }
    
    // Constructor with character selection
    GamePanel(String name, String characterType) {
        this.playerName = (name == null || name.isBlank()) ? "Player" : name.trim();
        this.characterType = characterType != null ? characterType : "male";
        this.isMultiplayer = false;
        this.isHostPlayer = true; // Solo player is always the host
        initializeGame();
    }
    
    // Constructor for multiplayer game
    GamePanel(String name, GameClient client) {
        this(name, client, "male"); // default to male character
    }
    
    // Constructor for multiplayer game with character selection
    GamePanel(String name, GameClient client, String characterType) {
        this.playerName = (name == null || name.isBlank()) ? "Player" : name.trim();
        this.characterType = characterType != null ? characterType : "male";
        this.gameClient = client;
        this.isMultiplayer = true;
        this.isHostPlayer = false; // Clients are not hosts by default
        initializeGame();
    }
    
    private void initializeGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        
        // Reset game state
        gameOver = false;
        winnerName = null; // Reset winner name

        // สร้างผู้เล่น/ลิสต์
        player = new Player(300, 359, characterType); // Start in the middle of the road (359 is approximately center of road)
        zombies = new ArrayList<>();
        bullets = new ArrayList<>();
        
        // Initialize player scores and characters
        playerScores.put(playerName, 0);
        playerCharacters.put(playerName, characterType);

        // เริ่มเธรดต่าง ๆ
        startGameThreads();

        // การควบคุมด้วยคีย์บอร์ด
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (gameOver) {
                    // Remove the restart functionality completely
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
                    // Also send character type
                    gameClient.sendMessage("PLAYER_CHARACTER:" + playerName + ":" + characterType);
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
                    // Also send character type
                    gameClient.sendMessage("PLAYER_CHARACTER:" + playerName + ":" + characterType);
                    lastPositionUpdate = currentTime;
                }
            }
        });
        
        // Set up network message listener for multiplayer mode
        if (isMultiplayer && gameClient != null) {
            gameClient.sendMessage("PLAYER_READY:" + playerName);
            // Send initial position and character to synchronize with other players
            gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + (int)player.x + "," + (int)player.y);
            gameClient.sendMessage("PLAYER_CHARACTER:" + playerName + ":" + characterType);
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
        System.out.println("Spawned zombie: " + zombie.zombieType + " (speed: " + String.format("%.2f", zombie.speed) + ", health: " + zombie.health + ")");
        
        // In multiplayer, send zombie information to other players
        if (isMultiplayer && gameClient != null) {
            gameClient.sendMessage("ZOMBIE_SPAWN:" + zombie.id + ":" + (WIDTH - 50) + "," + y + "," + zombie.speed + "," + zombie.zombieType);
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

    /** เริ่มเธรดทั้งหมด */
    private void startGameThreads() {
        gameRunning = true;
        shootingActive = true;
        zombieSpawningActive = true;
        
        // เธรดอัปเดตเกมหลัก (~60 FPS)
        gameThread = new Thread(() -> {
            while (gameRunning) {
                try {
                    updateGame();
                    SwingUtilities.invokeLater(this::repaint);
                    Thread.sleep(16); // ~60 FPS
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        gameThread.setName("GameThread");
        gameThread.start();

        // เธรดยิงกระสุน (ทุก 0.5 วินาที)
        shootThread = new Thread(() -> {
            while (shootingActive) {
                try {
                    if (!gameOver) {
                        shoot();
                    }
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        shootThread.setName("ShootThread");
        shootThread.start();

        // เธรดสร้างซอมบี้ (ทุก 2 วินาที)
        zombieThread = new Thread(() -> {
            while (zombieSpawningActive) {
                try {
                    if (!gameOver) {
                        spawnZombie();
                    }
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        zombieThread.setName("ZombieThread");
        zombieThread.start();

        // เธรด sync สำหรับ multiplayer (ทุก 100ms)
        if (isMultiplayer && gameClient != null) {
            syncActive = true;
            syncThread = new Thread(() -> {
                while (syncActive) {
                    try {
                        syncGameState();
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            syncThread.setName("SyncThread");
            syncThread.start();

            // เธรดตรวจสอบสถานะเกม (ทุก 1 วินาที)
            checkActive = true;
            checkThread = new Thread(() -> {
                while (checkActive) {
                    try {
                        if (gameOver && areAllPlayersDead() && !allPlayersDead) {
                            allPlayersDead = true;
                            stopGameThreads();
                            SwingUtilities.invokeLater(this::repaint);
                        }
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            checkThread.setName("CheckThread");
            checkThread.start();
        }
    }

    /** หยุดเธรดทั้งหมด */
    private void stopGameThreads() {
        gameRunning = false;
        shootingActive = false;
        zombieSpawningActive = false;
        syncActive = false;
        checkActive = false;

        // รอให้เธรดทั้งหมดหยุดทำงาน
        try {
            if (gameThread != null && gameThread.isAlive()) {
                gameThread.interrupt();
                gameThread.join(1000);
            }
            if (shootThread != null && shootThread.isAlive()) {
                shootThread.interrupt();
                shootThread.join(1000);
            }
            if (zombieThread != null && zombieThread.isAlive()) {
                zombieThread.interrupt();
                zombieThread.join(1000);
            }
            if (syncThread != null && syncThread.isAlive()) {
                syncThread.interrupt();
                syncThread.join(1000);
            }
            if (checkThread != null && checkThread.isAlive()) {
                checkThread.interrupt();
                checkThread.join(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** อัปเดตเกม (แทนที่ actionPerformed) */
    private void updateGame() {
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
                            // Check if this player has won
                            if (score >= 500) {
                                handlePlayerWin(playerName);
                            }
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

            if (isMultiplayer) {
                if (areAllPlayersDead()) {
                    // All players are dead - show game over
                    g.drawString("GAME OVER", WIDTH / 2 - 190, HEIGHT / 2);
                    g.setFont(new Font("Tahoma", Font.BOLD, 20));
                    g.setColor(Color.YELLOW);
                    g.drawString("All players died. Return to main menu to play again.", WIDTH / 2 - 220, HEIGHT / 2 + 40);
                } else {
                    // Show MVP when there's a winner
                    if (winnerName != null && !winnerName.isEmpty()) {
                        g.drawString("MVP: " + winnerName, WIDTH / 2 , HEIGHT / 2);
                        g.setFont(new Font("Tahoma", Font.BOLD, 20));
                        g.setColor(Color.YELLOW);
                        g.drawString("First to reach 500 points!", WIDTH / 2, HEIGHT / 2 + 40);
                    } else {
                        // Check for a winner using the old method as fallback
                        String winner = findWinner();
                        if (winner != null && !winner.isEmpty()) {
                            g.drawString("MVP: " + winner, WIDTH / 2 - 190, HEIGHT / 2);
                            g.setFont(new Font("Tahoma", Font.BOLD, 20));
                            g.setColor(Color.YELLOW);
                            g.drawString("First to reach 500 points!", WIDTH / 2 - 100, HEIGHT / 2 + 40);
                        } else {
                            g.drawString("YOU DIED", WIDTH / 2 - 190, HEIGHT / 2);
                            g.setFont(new Font("Tahoma", Font.BOLD, 20));
                            g.setColor(Color.YELLOW);
                            g.drawString("Waiting for other players to finish...", WIDTH / 2 - 100, HEIGHT / 2 + 40);
                        }
                    }
                }
            } else {
                // Solo mode - show game over without restart option
                g.drawString("GAME OVER", WIDTH / 2 - 100, HEIGHT / 2);
                g.setFont(new Font("Tahoma", Font.BOLD, 20));
                g.setColor(Color.YELLOW);
                g.drawString("Return to main menu to play again", WIDTH / 2 - 100, HEIGHT / 2 + 40);
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
        if (player == null || name == null || name.isEmpty()) return;

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



    /** จบเกม */
    void endGame() {
        gameOver = true;
        
        // In solo mode, stop all threads immediately
        if (!isMultiplayer) {
            stopGameThreads();
        } else {
            // In multiplayer mode, only stop player-specific activities
            shootingActive = false; // Stop shooting for this player
            zombieSpawningActive = false; // Stop spawning zombies for this player
            // Keep gameThread and syncThread running to receive updates from other players
        }
        
        // In multiplayer mode, notify other players about game over
        if (isMultiplayer && gameClient != null) {
            // Send final position before marking as dead
            gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + (int)player.x + "," + (int)player.y);
            gameClient.sendMessage("PLAYER_DIED:" + playerName);
            // Mark this player as dead in the scores
            playerScores.put(playerName, -1);
        }
    }
    
    /** Handle player win condition */
    void handlePlayerWin(String winnerName) {
        // Stop the game for all players
        gameOver = true;
        this.winnerName = winnerName; // Store the winner's name for MVP display
        
        // Stop all threads immediately for both solo and multiplayer modes
        stopGameThreads();
        
        // In multiplayer mode, notify other players about the win
        if (isMultiplayer && gameClient != null) {
            gameClient.sendMessage("PLAYER_WIN:" + winnerName);
        }
        
        SwingUtilities.invokeLater(this::repaint);
    }

    /** ทำความสะอาดเมื่อปิดเกม */
    public void cleanup() {
        stopGameThreads();
    }
    

    private boolean areAllPlayersDead() {
        // In solo mode, if this player is dead, game is over
        if (!isMultiplayer) {
            return gameOver;
        }
        
        // In multiplayer mode, check if all players are marked as dead (-1)
        for (Map.Entry<String, Integer> entry : playerScores.entrySet()) {
            String playerName = entry.getKey();
            Integer score = entry.getValue();
            
            // Check if player is still alive (not marked as dead with -1)
            if (score != null && score != -1) {
                return false; // At least one player is still alive
            }
        }
        
        return true; // All players are dead
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
                            // Don't create player if it's ourselves
                            if (!playerName.equals(this.playerName)) {
                                // Create other players with different colors
                                Color[] playerColors = {Color.MAGENTA, Color.ORANGE, Color.YELLOW, Color.PINK, Color.LIGHT_GRAY};
                                Color playerColor = playerColors[otherPlayers.size() % playerColors.length];
                                // Get the character type for this player if available
                                String playerCharacterType = playerCharacters.getOrDefault(playerName, "male");
                                otherPlayer = new Player(x, y, playerColor, playerCharacterType);
                                otherPlayers.put(playerName, otherPlayer);
                                playerScores.put(playerName, 0); // Initialize score for new player
                                System.out.println("Created player: " + playerName + " at (" + x + ", " + y + ") with character type: " + playerCharacterType);
                                // Send current player position to the new player
                                if (isMultiplayer && gameClient != null) {
                                    gameClient.sendMessage("PLAYER_POSITION:" + this.playerName + ":" + (int)player.x + "," + (int)player.y);
                                }
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
            }  else if (message.startsWith("STATE|")) {
                String[] parts = message.split("\\|", 3);
                if (parts.length == 3) {
                    String name = parts[1];
                    PlayerState st = PlayerState.fromString(parts[2]);
                    if (!name.equals(playerName)) {
                        // อัปเดตตำแหน่งของผู้เล่นอื่น
                        Player other = otherPlayers.get(name);
                        if (other == null) {
                            other = new Player(st.x, st.y, Color.CYAN);
                            otherPlayers.put(name, other);
                        } else {
                            other.x = st.x;
                            other.y = st.y;
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
                    if (coords.length >= 4) { // x, y, speed, zombieType
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        double speed = Double.parseDouble(coords[2]);
                        String zombieType = coords[3];
                        Zombie zombie = new Zombie(x, y, zombieId, speed, zombieType);
                        // Store zombie with ID for proper synchronization
                        zombies.add(zombie);
                    } else if (coords.length >= 3) { // x, y, speed (fallback)
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        double speed = Double.parseDouble(coords[2]);
                        Zombie zombie = new Zombie(x, y, zombieId, speed);
                        // Store zombie with ID for proper synchronization
                        zombies.add(zombie);
                    } else if (coords.length >= 2) { // x, y (old format fallback)
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
                    
                    // Check if any player has reached 500 points to win the game
                    if (playerScore >= 500) {
                        handlePlayerWin(playerName);
                    }
                }
            } else if (message.startsWith("PLAYER_LIST:")) {
                // Handle player list message to create other players
                String[] players = message.substring("PLAYER_LIST:".length()).split(",");
                for (String player : players) {
                    if (!player.isEmpty() && !player.equals(this.playerName)) {
                        // Create player if not already exists
                        if (!otherPlayers.containsKey(player) && !playerScores.containsKey(player)) {
                            // Create other players with different colors
                            Color[] playerColors = {Color.MAGENTA, Color.ORANGE, Color.YELLOW, Color.PINK, Color.LIGHT_GRAY};
                            Color playerColor = playerColors[otherPlayers.size() % playerColors.length];
                            // Get the character type for this player if available
                            String playerCharacterType = playerCharacters.getOrDefault(player, "male");
                            Player otherPlayer = new Player(300, 359, playerColor, playerCharacterType); // Default position
                            otherPlayers.put(player, otherPlayer);
                            playerScores.put(player, 0); // Initialize score
                        }
                    }
                }
            } else if (message.startsWith("PLAYER_JOINED:")) {
                // Handle when a new player joins the game
                String newPlayerName = message.substring("PLAYER_JOINED:".length());
                if (!newPlayerName.isEmpty() && !newPlayerName.equals(this.playerName)) {
                    // Create player if not already exists
                    if (!otherPlayers.containsKey(newPlayerName) && !playerScores.containsKey(newPlayerName)) {
                        // Create other players with different colors
                        Color[] playerColors = {Color.MAGENTA, Color.ORANGE, Color.YELLOW, Color.PINK, Color.LIGHT_GRAY};
                        Color playerColor = playerColors[otherPlayers.size() % playerColors.length];
                        // Get the character type for this player if available
                        String playerCharacterType = playerCharacters.getOrDefault(newPlayerName, "male");
                        Player otherPlayer = new Player(300, 359, playerColor, playerCharacterType); // Default position
                        otherPlayers.put(newPlayerName, otherPlayer);
                        playerScores.put(newPlayerName, 0); // Initialize score
                        
                        // Send current player position to the new player
                        if (isMultiplayer && gameClient != null) {
                            gameClient.sendMessage("PLAYER_POSITION:" + this.playerName + ":" + (int)player.x + "," + (int)player.y);
                        }
                    }
                }
            } else if (message.startsWith("PLAYER_LEFT:")) {
                // Handle when a player leaves the game
                String leftPlayerName = message.substring("PLAYER_LEFT:".length());
                if (!leftPlayerName.isEmpty()) {
                    otherPlayers.remove(leftPlayerName);
                    playerScores.remove(leftPlayerName);
                }
            } else if (message.startsWith("PLAYER_WIN:")) {
                // Handle when a player wins the game
                String winnerName = message.substring("PLAYER_WIN:".length());
                this.winnerName = winnerName; // Store the winner's name for MVP display
                handlePlayerWin(winnerName);
            } else if (message.startsWith("PLAYER_CHARACTER:")) {
                // Handle when a player selects a character
                String[] parts = message.substring("PLAYER_CHARACTER:".length()).split(":");
                if (parts.length >= 2) {
                    String playerName = parts[0];
                    String characterType = parts[1];
                    
                    // Update or create player with character type
                    Player otherPlayer = otherPlayers.get(playerName);
                    if (otherPlayer == null) {
                        // Create other players with different colors
                        Color[] playerColors = {Color.MAGENTA, Color.ORANGE, Color.YELLOW, Color.PINK, Color.LIGHT_GRAY};
                        Color playerColor = playerColors[otherPlayers.size() % playerColors.length];
                        otherPlayer = new Player(300, 359, playerColor, characterType);
                        otherPlayers.put(playerName, otherPlayer);
                        playerScores.put(playerName, 0); // Initialize score for new player
                    } else {
                        // Update character type
                        otherPlayer.characterType = characterType;
                    }
                }
            } else if (message.startsWith("GAME_START")) {
                // Game started by host
                gameOver = false;
                isHostPlayer = false;

                if (isMultiplayer && gameClient != null) {
                    gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + player.x + "," + player.y);
                }
            } else if (message.startsWith("PLAYER_DIED:")) {

                String deadPlayer = message.substring("PLAYER_DIED:".length());
                if (!deadPlayer.equals(playerName)) { // Don't process our own death message
                    playerScores.put(deadPlayer, -1); // Mark as dead with special score
                }
                

                if (areAllPlayersDead()) {
                    allPlayersDead = true;
                }
                repaint();
                

            } else if (message.startsWith("GAME_RESTART")) {

            } else if (message.startsWith("HOST_RESTART")) {

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
    
    /** Find the player who has won (reached 500 points) */
    private String findWinner() {

        if (score >= 500) {
            return playerName;
        }
        
        // Check other players' scores
        for (Map.Entry<String, Integer> entry : playerScores.entrySet()) {
            if (entry.getValue() != null && entry.getValue() >= 500) {
                return entry.getKey();
            }
        }
        
        return null;
    }

}

/** คลาสผู้เล่น */
class Player {
    double x, y; // เปลี่ยนเป็น double เพื่อการเคลื่อนไหวที่นุ่มนวล
    int size = 60; // Increased size to better display player image
    double speed = 1.5; // ลดความเร็วลงเพื่อให้เคลื่อนไหวนุ่มนวลขึ้น
    Color playerColor = Color.CYAN; // สีของผู้เล่น (สำหรับผู้เล่นอื่น)
    boolean isMainPlayer = false; // ตัวแปรเพื่อแยกผู้เล่นหลักกับผู้เล่นอื่น
    String characterType = "male"; // Character type: "male" or "female"


    static ImageIcon playerIcon;
    static ImageIcon femaleIcon;
    static Image playerImage;
    static Image femaleImage;
    
    // ตัวแปรสำหรับการเคลื่อนไหวแบบต่อเนื่อง
    boolean movingUp = false;
    boolean movingDown = false;
    boolean movingLeft = false;
    boolean movingRight = false;
    
    // Static initializer to load images safely
    static {
        try {
            playerIcon = new ImageIcon(
                System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                        + File.separator + "game" + File.separator + "player1.png");
            playerImage = playerIcon.getImage();
            
            femaleIcon = new ImageIcon(
                System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                        + File.separator + "game" + File.separator + "player2.png");
            femaleImage = femaleIcon.getImage();
        } catch (Exception e) {
            System.err.println("Failed to load player images: " + e.getMessage());
            // Create a default image if loading fails
            playerImage = null;
            femaleImage = null;
        }
    }

    Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.isMainPlayer = true; // ผู้เล่นหลักใช้รูป player1.png
        this.characterType = "male"; // default to male
    }
    
    Player(int x, int y, Color color) {
        this.x = x;
        this.y = y;
        this.playerColor = color;
        this.isMainPlayer = true; // เปลี่ยนให้ผู้เล่นอื่นใช้รูปเหมือนกัน
        this.characterType = "male"; // default to male
    }
    
    Player(int x, int y, String characterType) {
        this.x = x;
        this.y = y;
        this.isMainPlayer = true;
        this.characterType = characterType != null ? characterType : "male";
    }
    
    Player(int x, int y, Color color, String characterType) {
        this.x = x;
        this.y = y;
        this.playerColor = color;
        this.isMainPlayer = true;
        this.characterType = characterType != null ? characterType : "male";
    }

    void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        

        Image imageToDraw = null;
        if ("female".equals(characterType) && femaleImage != null) {
            imageToDraw = femaleImage;
        } else if (playerImage != null) {
            imageToDraw = playerImage;
        }
        
        if (imageToDraw == null) {
            g.setColor(playerColor);
            g.fillRect((int)x, (int)y, size, 75);
            return;
        }
        
        if (playerColor == Color.CYAN) {
            // Main player - draw normal image
            g.drawImage(imageToDraw, (int)x, (int)y, size, 75, null);
        } else {
            // Other players - draw image with color tint
            g.drawImage(imageToDraw, (int)x, (int)y, size, 75, null);
            
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


class Zombie {
    Random  rand = new Random();
    int x, y;
    int size = 60; // Increased size to better display zombie image
    double speed;
    String id; // Unique ID for synchronization

    int health = 30; // เลือดเริ่มต้นของซอมบี้
    String zombieType = "type1"; // Zombie type: "type1", "type2", "type3", or "type4"
    
    // โหลดรูปซอมบี้ทั้ง 4 แบบ
    static ImageIcon zombieIcon1;
    static ImageIcon zombieIcon2;
    static ImageIcon zombieIcon3;
    static ImageIcon zombieIcon4;
    static Image zombieImage1;
    static Image zombieImage2;
    static Image zombieImage3;
    static Image zombieImage4;
    
    // Static initializer to load zombie images safely
    static {
        try {
            zombieIcon1 = new ImageIcon(
                System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                        + File.separator + "game" + File.separator + "Zombie1rv.png");
            zombieImage1 = zombieIcon1.getImage();
            
            zombieIcon2 = new ImageIcon(
                System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                        + File.separator + "game" + File.separator + "Zombie2rv.png");
            zombieImage2 = zombieIcon2.getImage();
            
            zombieIcon3 = new ImageIcon(
                System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                        + File.separator + "game" + File.separator + "Zombie3rv.png");
            zombieImage3 = zombieIcon3.getImage();
            
            zombieIcon4 = new ImageIcon(
                System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                        + File.separator + "game" + File.separator + "Zombie4rv.png");
            zombieImage4 = zombieIcon4.getImage();
            
            System.out.println("Loaded zombie images - Type1: " + (zombieImage1 != null) + 
                             ", Type2: " + (zombieImage2 != null) + 
                             ", Type3: " + (zombieImage3 != null) + 
                             ", Type4: " + (zombieImage4 != null));
        } catch (Exception e) {
            System.err.println("Failed to load zombie images: " + e.getMessage());
            zombieImage1 = null;
            zombieImage2 = null;
            zombieImage3 = null;
            zombieImage4 = null;
        }
    }

    Zombie(int x, int y) {
        this.x = x;
        this.y = y;
        this.id = java.util.UUID.randomUUID().toString();
        // Randomly assign zombie type from 4 types
        int typeNum = rand.nextInt(4) + 1; // 1-4
        this.zombieType = "type" + typeNum;
        // Set properties based on zombie type
        setZombieProperties();
    }
    
    Zombie(int x, int y, String id) {
        this.x = x;
        this.y = y;
        this.id = id;
        // Randomly assign zombie type from 4 types
        int typeNum = rand.nextInt(4) + 1; // 1-4
        this.zombieType = "type" + typeNum;
        // Set properties based on zombie type
        setZombieProperties();
    }
    
    Zombie(int x, int y, String id, double speed) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.id = id;
        // Randomly assign zombie type from 4 types
        int typeNum = rand.nextInt(4) + 1; // 1-4
        this.zombieType = "type" + typeNum;
        // Don't override speed for this constructor since it's explicitly set
    }
    
    Zombie(int x, int y, String id, double speed, String zombieType) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.id = id;
        this.zombieType = zombieType != null ? zombieType : "type1";
        // Don't override speed and health for this constructor since they're explicitly set
    }
    
    /** Set zombie properties based on type */
    private void setZombieProperties() {
        switch (zombieType) {
            case "type1":
                // Type 1: Balanced zombie
                this.speed = rand.nextDouble() * 1 + 0.5; // 0.5-1.5 speed
                this.health = 30; // Standard health
                break;
            case "type2":
                // Type 2: Fast but weak zombie
                this.speed = rand.nextDouble() * 1.5 + 0.8; // 0.8-2.3 speed
                this.health = 20; // Low health
                break;
            case "type3":
                // Type 3: Slow but strong zombie
                this.speed = rand.nextDouble() * 0.8 + 0.3; // 0.3-1.1 speed
                this.health = 40; // High health
                break;
            case "type4":
                // Type 4: Very fast but very weak zombie
                this.speed = rand.nextDouble() * 2 + 1.0; // 1.0-3.0 speed
                this.health = 15; // Very low health
                break;
            default:
                // Default to type1 properties
                this.speed = rand.nextDouble() * 1 + 0.5;
                this.health = 30;
                break;
        }
    }

    void draw(Graphics g) {
        // เลือกรูปซอมบี้ตามประเภท
        Image imageToDraw = null;
        switch (zombieType) {
            case "type1":
                imageToDraw = zombieImage1;
                break;
            case "type2":
                imageToDraw = zombieImage2;
                break;
            case "type3":
                imageToDraw = zombieImage3;
                break;
            case "type4":
                imageToDraw = zombieImage4;
                break;
            default:
                imageToDraw = zombieImage1; // Default to type1
                break;
        }
        
        // วาดรูปซอมบี้
        if (imageToDraw != null) {
            g.drawImage(imageToDraw, x, y, size, 75, null);
        } else {
            // Fallback to colored rectangle if image loading fails
            g.setColor(Color.GREEN);
            g.fillRect(x, y, size, 75);
        }
        
        // แถบ HP with type-specific colors
        g.setColor(Color.RED);
        g.fillRect(x, y - 10, size, 5);
        
        // Set health bar color based on zombie type
        Color healthColor;
        switch (zombieType) {
            case "type1": healthColor = Color.GREEN; break;      // Balanced - Green
            case "type2": healthColor = Color.YELLOW; break;     // Fast - Yellow
            case "type3": healthColor = Color.BLUE; break;       // Strong - Blue
            case "type4": healthColor = Color.ORANGE; break;     // Very Fast - Orange
            default: healthColor = Color.GREEN; break;
        }
        g.setColor(healthColor);
        
        // Calculate health bar based on zombie type's max health
        double maxHealth = getMaxHealth();
        int hpBar = Math.max(0, (int) ((health / maxHealth) * size));
        g.fillRect(x, y - 10, hpBar, 5);
    }
    
    /** Get max health based on zombie type */
    private double getMaxHealth() {
        switch (zombieType) {
            case "type1": return 30.0;
            case "type2": return 20.0;
            case "type3": return 40.0;
            case "type4": return 15.0;
            default: return 30.0;
        }
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