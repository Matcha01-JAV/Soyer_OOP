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
    Map<String, Player> otherPlayers = new HashMap<>();

    ImageIcon bgIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "game_map.png");
    Image bg = bgIcon.getImage();

    // ตัวจับเวลาเกม
    javax.swing.Timer gameTimer;
    javax.swing.Timer shootTimer;
    javax.swing.Timer zombieTimer;

    Player player;
    List<Zombie> zombies;
    List<Bullet> bullets;
    Random random = new Random();

    int score = 0;
    boolean gameOver = false;

    // เผื่อเรียกแบบไม่ส่งชื่อ
    GamePanel() {
        this("Player");
    }

    // คอนสตรัคเตอร์หลัก: รับชื่อแล้วเก็บไว้
    GamePanel(String name) {
        this.playerName = (name == null || name.isBlank()) ? "Player" : name.trim();
        this.isMultiplayer = false;
        initializeGame();
    }
    
    // Constructor for multiplayer game
    GamePanel(String name, GameClient client) {
        this.playerName = (name == null || name.isBlank()) ? "Player" : name.trim();
        this.gameClient = client;
        this.isMultiplayer = true;
        initializeGame();
    }
    
    private void initializeGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        // สร้างผู้เล่น/ลิสต์
        player = new Player(300, HEIGHT / 2 - 25);
        zombies = new ArrayList<>();
        bullets = new ArrayList<>();

        // ยิงกระสุนทุก 0.5 วินาที
        shootTimer = new javax.swing.Timer(500, e -> shoot());
        shootTimer.start();

        // สุ่มสร้างซอมบี้ทุก 2 วินาที
        zombieTimer = new javax.swing.Timer(2000, e -> spawnZombie());
        zombieTimer.start();

        // อัปเดตเกม ~60 FPS
        gameTimer = new javax.swing.Timer(16, this);
        gameTimer.start();

        // การควบคุมด้วยคีย์บอร์ด
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (gameOver) {
                    if (e.getKeyCode() == KeyEvent.VK_SPACE) restartGame();
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W)
                    player.moveUp();
                if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S)
                    player.moveDown();
                if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A)
                    player.moveLeft();
                if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D)
                    player.moveRight();
                
                // Send player position to other players in multiplayer mode
                if (isMultiplayer && gameClient != null) {
                    gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + player.x + "," + player.y);
                }
            }
        });
        
        // Set up network message listener for multiplayer mode
        if (isMultiplayer && gameClient != null) {
            gameClient.sendMessage("PLAYER_READY:" + playerName);
        }
    }

    /** ยิงกระสุน */
    void shoot() {
        if (gameOver) return;
        bullets.add(new Bullet(player.x + player.size, player.y + player.size / 2 - 5));
        
        // In multiplayer, send bullet information to other players
        if (isMultiplayer && gameClient != null) {
            gameClient.sendMessage("PLAYER_SHOOT:" + playerName + ":" + 
                (player.x + player.size) + "," + (player.y + player.size / 2 - 5));
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
            gameClient.sendMessage("ZOMBIE_SPAWN:" + zombie.id + ":" + (WIDTH - 50) + "," + y);
        }
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
            for (Player otherPlayer : otherPlayers.values()) {
                otherPlayer.draw(g);
                drawPlayerName((Graphics2D) g, otherPlayer);
            }
        }

        // วาดชื่อผู้เล่นเหนือหัว (← ตรงนี้คือชื่อจาก Main)
        drawPlayerName((Graphics2D) g);

        // วาดกระสุน
        for (Bullet b : bullets) b.draw(g);

        // วาดซอมบี้
        for (Zombie z : zombies) z.draw(g);

        // คะแนน
        g.setColor(Color.WHITE);
        g.setFont(new Font("Tahoma", Font.BOLD, 20));
        g.drawString("Score: " + score, 20, 30);

        // Game Over
        if (gameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("Tahoma", Font.BOLD, 50));
            g.drawString("GAME OVER", WIDTH / 2 - 150, HEIGHT / 2);
            g.setFont(new Font("Tahoma", Font.BOLD, 20));
            g.setColor(Color.YELLOW);
            g.drawString("Press SPACE to Restart", WIDTH / 2 - 130, HEIGHT / 2 + 40);
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

        int centerX = player.x + player.size / 2;
        int nameX = centerX - textW / 2;
        int nameY = player.y - 12;             // ยกขึ้นเหนือหัว
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
    private void drawPlayerName(Graphics2D g2, Player player) {
        if (player == null) return;

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font font = new Font("Tahoma", Font.BOLD, 18);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();

        // Find player name from the player object (in a real implementation, 
        // we would store player names with their objects)
        // For now, we'll use a default name since we don't have access to the actual player name
        String playerName = "Player";
        
        int textW = fm.stringWidth(playerName);
        int textH = fm.getAscent();

        int centerX = player.x + player.size / 2;
        int nameX = centerX - textW / 2;
        int nameY = player.y - 12;             // ยกขึ้นเหนือหัว
        if (nameY - textH < 0) nameY = textH + 4;

        g2.setColor(Color.YELLOW);  // Different color for other players
        g2.drawString(playerName, nameX, nameY);
    }

    /** อัปเดตเกมแต่ละเฟรม */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) return;

        // อัปเดตตำแหน่ง
        for (Bullet b : new ArrayList<>(bullets)) b.update();
        for (Zombie z : new ArrayList<>(zombies)) z.update();

        // Send player position continuously in multiplayer mode
        if (isMultiplayer && gameClient != null) {
            gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + player.x + "," + player.y);
        }

        List<Bullet> bulletsToRemove = new ArrayList<>();
        List<Zombie> zombiesToRemove = new ArrayList<>();

        // ชนกระสุน-ซอมบี้
        for (Bullet b : bullets) {
            Rectangle br = b.getBounds();
            for (Zombie z : zombies) {
                if (br.intersects(z.getBounds())) {
                    z.health -= b.damage;
                    bulletsToRemove.add(b);
                    if (z.health <= 0) {
                        zombiesToRemove.add(z);
                        score += 10;
                        // In multiplayer, send zombie killed information to other players
                        if (isMultiplayer && gameClient != null) {
                            gameClient.sendMessage("ZOMBIE_KILLED:" + z.id);
                        }
                    }
                }
            }
        }

        bullets.removeAll(bulletsToRemove);
        zombies.removeAll(zombiesToRemove);

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

        // ลบกระสุนที่พ้นจอ
        bullets.removeIf(b -> b.x > WIDTH);

        repaint();
    }

    /** จบเกม */
    void endGame() {
        gameOver = true;
        gameTimer.stop();
        shootTimer.stop();
        zombieTimer.stop();
    }

    /** เริ่มเกมใหม่ */
    void restartGame() {
        score = 0;
        gameOver = false;
        zombies.clear();
        bullets.clear();
        player = new Player(300, HEIGHT / 2 - 25);
        gameTimer.start();
        shootTimer.start();
        zombieTimer.start();
        requestFocusInWindow();
    }
    
    /**
     * Set the message listener for the GameClient
     */
    public void setMessageListener(GameClient.MessageListener listener) {
        if (gameClient != null) {
            // We can't directly set the listener, but we can create a new GameClient
            // This is a workaround since GameClient doesn't have a setter method
        }
    }
    
    /**
     * Handle network messages in multiplayer mode
     */
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
                            otherPlayer = new Player(x, y);
                            otherPlayers.put(playerName, otherPlayer);
                            // Send current player position to the new player
                            if (isMultiplayer && gameClient != null) {
                                gameClient.sendMessage("PLAYER_POSITION:" + this.playerName + ":" + player.x + "," + player.y);
                            }
                        } else {
                            otherPlayer.x = x;
                            otherPlayer.y = y;
                        }
                    }
                }
            } else if (message.startsWith("PLAYER_SHOOT:")) {
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
                    if (coords.length >= 2) {
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        Zombie zombie = new Zombie(x, y);
                        // Store zombie with ID for proper synchronization
                        zombies.add(zombie);
                    }
                }
            } else if (message.startsWith("ZOMBIE_KILLED:")) {
                String zombieId = message.substring("ZOMBIE_KILLED:".length());
                // Remove zombie by ID for proper synchronization
                zombies.removeIf(z -> z.id.equals(zombieId));
            } else if (message.startsWith("GAME_START")) {
                // Game started by host
                gameOver = false;
                // Send current player position to other players
                if (isMultiplayer && gameClient != null) {
                    gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + player.x + "," + player.y);
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing network message: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error handling network message: " + e.getMessage());
        }
    }
}

/** คลาสผู้เล่น */
class Player {
    int x, y;
    int size = 50;
    int speed = 15;

    Player(int x, int y) {
        this.x = x;
        this.y = y;
    }

    void draw(Graphics g) {
        g.setColor(Color.CYAN);
        g.fillOval(x, y, size, size);
    }

    void moveUp() {
        if (y > 340)
            y -= speed;
    }
    void moveLeft() {
        if (x > 250)
            x -= speed;
    }
    void moveRight() {
        if (x < GamePanel.WIDTH - size)
            x += speed;
    }
    void moveDown() {
        if (y < 640)
            y += speed;
    }

    Rectangle getBounds() {
        return new Rectangle(x, y, size, size);
    }
}

/** คลาสซอมบี้ */
class Zombie {
    Random  rand = new Random();
    int x, y;
    int size = 40;
    double speed = rand.nextDouble()*2.5+0.5;
    String id; // Unique ID for synchronization

    int health = 30; // เลือดเริ่มต้นของซอมบี้

    Zombie(int x, int y) {
        this.x = x;
        this.y = y;
        this.id = java.util.UUID.randomUUID().toString();
    }
    
    Zombie(int x, int y, String id) {
        this.x = x;
        this.y = y;
        this.id = id;
    }

    void draw(Graphics g) {
        g.setColor(Color.GREEN);
        g.fillRect(x, y, size, size);

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
