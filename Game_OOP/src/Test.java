import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;



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
 class GameFrame extends JFrame {
    // เผื่อใครเรียกแบบไม่ส่งชื่อ
    GameFrame() {
        this("Player");
    }

    // ใช้คอนสตรัคเตอร์นี้เวลามาจาก Main พร้อมชื่อผู้เล่น
    GameFrame(String playerName) {
        setTitle("Soyer vs Zombies");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        add(new GamePanel(playerName));   // ← ส่งชื่อต่อให้ GamePanel
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}

/** พื้นที่หลักของเกม (Canvas) */
class GamePanel extends JPanel implements ActionListener {

    static final int WIDTH = 1262;
    static final int HEIGHT = 768;

    // ชื่อผู้เล่นที่รับมาจากฝั่ง Main
    String playerName;

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
            }
        });
    }

    /** ยิงกระสุน */
    void shoot() {
        if (gameOver) return;
        bullets.add(new Bullet(player.x + player.size, player.y + player.size / 2 - 5));
    }

    /** สุ่มเกิดซอมบี้ */
    void spawnZombie() {
        if (gameOver) return;
        // เลนถนนในแนว Y (ปรับให้ตรงภาพ)
        int roadTopY = 340;
        int roadBottomY = 700;
        int y = roadTopY + random.nextInt(Math.max(1, roadBottomY - roadTopY - 40));
        zombies.add(new Zombie(WIDTH - 50, y));
    }

    /** วาดทุกอย่าง */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(bg, 0, 0, this);

        // วาดผู้เล่น
        player.draw(g);

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

    /** อัปเดตเกมแต่ละเฟรม */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) return;

        // อัปเดตตำแหน่ง
        for (Bullet b : new ArrayList<>(bullets)) b.update();
        for (Zombie z : new ArrayList<>(zombies)) z.update();

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
        if (x < GamePanel.HEIGHT - size)
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

    int health = 30; // เลือดเริ่มต้นของซอมบี้

    Zombie(int x, int y) {
        this.x = x;
        this.y = y;
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
