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
import java.util.concurrent.ConcurrentHashMap;

import network.*;  // Add network package import

/*
 * GamePanel = กระดานหลักของเกม
 * - วาดฉาก/ผู้เล่น/ซอมบี้/กระสุน
 * - ลูปเกม (อัปเดต 60FPS โดยประมาณ)
 * - อินพุตคีย์บอร์ด (กดเพื่อเริ่มเคลื่อน/ปล่อยเพื่อหยุด)
 * - ซิงก์สถานะกับผู้เล่นอื่นถ้าเป็นโหมด Multiplayer
 */
class GamePanel extends JPanel {

    static final int WIDTH = 1262;
    static final int HEIGHT = 768;

    // ชื่อผู้เล่นที่รับมาจากฝั่ง Main
    String playerName;
    String characterType = "male";

    // ตัวแปรสำหรับ Multiplayer
    boolean isMultiplayer = false;
    GameClient gameClient = null;
    Map<String, Player> otherPlayers = new ConcurrentHashMap<>();     // เก็บผู้เล่นคนอื่น โดยคีย์เป็นชื่อ
    Map<String, Integer> playerScores = new ConcurrentHashMap<>();    // เก็บคะแนนของผู้เล่นทุกคน (-1 = ตาย)
    Map<String, String> playerCharacters = new ConcurrentHashMap<>(); // เก็บชนิดตัวละครของผู้เล่นแต่ละคน
    boolean isHostPlayer = false;                                      // ธงไว้บอกว่าเป็น Host หรือไม่ (บางส่วนไม่ได้ใช้)
    boolean allPlayersDead = false;                                    // ธง “ทุกคนตายแล้ว”
    String winnerName = null;                                          // ชื่อผู้ชนะ (MVP)
    JButton nextButton = null;                                         // ปุ่ม (ยังไม่ได้ใช้งานในโค้ดนี้)

    // พื้นหลังแผนที่
    ImageIcon bgIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "game_map.png");
    Image bg = bgIcon.getImage();

    // เธรดสำหรับควบคุมเกม
    Thread gameThread;      // ลูปหลักสำหรับอัปเดตและ repaint
    Thread shootThread;     // ยิงกระสุนอัตโนมัติ
    Thread zombieThread;    // สุ่มเกิดซอมบี้
    Thread syncThread;      // ส่งข้อมูลซิงก์ไปเครือข่ายเป็นระยะ
    Thread checkThread;     // เช็คสถานะรวม เช่น ทุกคนตายหรือยัง

    // ตัวแปรควบคุมเธรด (เปิด/ปิด)
    volatile boolean gameRunning = false;
    volatile boolean shootingActive = false;
    volatile boolean zombieSpawningActive = false;
    volatile boolean syncActive = false;
    volatile boolean checkActive = false;

    // อ็อบเจ็กต์ในเกม
    Player player;               // ผู้เล่นหลัก (ของเรา)
    List<Zombie> zombies;        // รายการซอมบี้ทั้งหมด
    List<Bullet> bullets;        // รายการกระสุนทั้งหมด
    Random random = new Random();

    int score = 0;               // คะแนนของผู้เล่นเรา
    boolean gameOver = false;    // ธง “จบเกม” ของฝั่งเรา
    long lastPositionUpdate = 0; // เวลาอัปเดตตำแหน่งล่าสุด (ms) ใช้ throttle การส่งแพ็กเก็ต

    // เผื่อเรียกแบบไม่ส่งชื่อ
    GamePanel() {
        this("Player");
    }

    // คอนสตรัคเตอร์หลัก: รับชื่อแล้วเก็บไว้ (default ตัวละครชาย)
    GamePanel(String name) {
        this(name, "male"); // default to male character
    }

    // Constructor with character selection (โหมดเดี่ยว)
    GamePanel(String name, String characterType) {
        if (name == null) {
            this.playerName = "Player";   // ถ้า name เป็น null → ใช้ค่า "Player"
        } else if (name.isBlank()) {
            this.playerName = "Player";   // ถ้า name มีแต่ช่องว่าง → ใช้ค่า "Player"
        } else {
            this.playerName = name.trim(); // ตัดช่องว่างหัวท้ายออกแล้วใช้ชื่อจริง
        }
        if (characterType != null) {
            this.characterType = characterType;   // ถ้ามีค่ามา → ใช้ค่านั้นเลย
        } else {
            this.characterType = "male";          // ถ้าไม่มี (null) → ตั้งเป็น "male"
        }
        this.isMultiplayer = false;
        this.isHostPlayer = true;
        initializeGame();
    }


    GamePanel(String name, GameClient client) {
        this(name, client, "male");

    }

    // Constructor for multiplayer game with character selection
    GamePanel(String name, GameClient client, String characterType) {
        if (name == null || name.isBlank()) {
            this.playerName = "Player";   // ถ้า name เป็น null หรือเป็นช่องว่าง → ใช้ "Player"
        } else {
            this.playerName = name.trim(); // ถ้ามีชื่อจริง → ตัดช่องว่างหัวท้ายแล้วใช้ค่านั้น
        }
        if (characterType != null) {
            this.characterType = characterType;   // ถ้ามีค่ามา → ใช้ค่านั้นเลย
        } else {
            this.characterType = "male";          // ถ้าไม่มี (เป็น null) → ใช้ค่า "male"
        }
        this.gameClient = client;
        this.isMultiplayer = true;
        this.isHostPlayer = false; // Clients are not hosts by default
        initializeGame();
    }

    // ตั้งค่าหน้าเกมครั้งแรก: สร้างผู้เล่น/ลิสต์, ใส่ KeyListener, เริ่มเธรด, ส่งแพ็กเก็ตเปิดฉากถ้าเป็น Multiplayer
    private void initializeGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        gameOver = false;
        winnerName = null; // Reset winner name

        // สร้างผู้เล่นหลัก (เริ่มพิกัดแถวกลางถนน y=359)
        player = new Player(300, 359, characterType);
        zombies = new ArrayList<>();
        bullets = new ArrayList<>();

        // เก็บคะแนน/ชนิดตัวละครของเราในแมปรวม (ใช้สำหรับ HUD และ sync ฝั่งอื่น)
        playerScores.put(playerName, 0);
        playerCharacters.put(playerName, characterType);

        // เริ่มเธรดหลักของเกม
        startGameThreads();

        // การควบคุมด้วยคีย์บอร์ด: กดเพื่อเริ่มเคลื่อน, ปล่อยเพื่อหยุด
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (gameOver) {
                    // ตายแล้วไม่รับอินพุต
                    return;
                }
                // เริ่มการเคลื่อนไหวเมื่อกดปุ่ม (ตั้งธง)
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W)
                    player.startMoveUp();
                if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S)
                    player.startMoveDown();
                if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A)
                    player.startMoveLeft();
                if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D)
                    player.startMoveRight();

                // ส่งตำแหน่งให้คนอื่นเมื่อเริ่มเคลื่อน (จำกัดให้ห่างอย่างน้อย 50ms ต่อครั้ง เพื่อไม่ flood)
                long currentTime = System.currentTimeMillis();
                if (isMultiplayer && gameClient != null && (currentTime - lastPositionUpdate >= 50)) {
                    gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + (int)player.x + "," + (int)player.y);
                    // แจ้งชนิดตัวละครด้วย (เผื่ออีกฝั่งยังไม่รู้)
                    gameClient.sendMessage("PLAYER_CHARACTER:" + playerName + ":" + characterType);
                    lastPositionUpdate = currentTime;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (gameOver) return;

                // หยุดการเคลื่อนไหวเมื่อปล่อยปุ่ม (เคลียร์ธง)
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W)
                    player.stopMoveUp();
                if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S)
                    player.stopMoveDown();
                if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A)
                    player.stopMoveLeft();
                if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D)
                    player.stopMoveRight();

                // ส่งตำแหน่งตอนหยุด เพื่อให้ตำแหน่งสุดท้ายตรงกัน (ยังคง throttle 50ms)
                long currentTime = System.currentTimeMillis();
                if (isMultiplayer && gameClient != null && (currentTime - lastPositionUpdate >= 50)) {
                    gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + (int)player.x + "," + (int)player.y);
                    gameClient.sendMessage("PLAYER_CHARACTER:" + playerName + ":" + characterType);
                    lastPositionUpdate = currentTime;
                }
            }
        });

        // ถ้าเป็น Multiplayer: แจ้งความพร้อม + ส่งตำแหน่ง/ตัวละครเริ่มต้น
        if (isMultiplayer && gameClient != null) {
            gameClient.sendMessage("PLAYER_READY:" + playerName);
            // Send initial position and character to synchronize with other players
            gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + (int)player.x + "," + (int)player.y);
            gameClient.sendMessage("PLAYER_CHARACTER:" + playerName + ":" + characterType);
        }
    }

    /** ยิงกระสุนหนึ่งนัด และถ้า multiplayer ก็ broadcast ตำแหน่งกระสุนให้คนอื่นสร้างด้วย */
    void shoot() {
        if (gameOver) return;
        bullets.add(new Bullet((int)(player.x + player.size), (int)(player.y + player.size / 2 - 5)));

        // In multiplayer, send bullet information to other players
        if (isMultiplayer && gameClient != null) {
            gameClient.sendMessage("PLAYER_SHOOT:" + playerName + ":" +
                    (int)(player.x + player.size) + "," + (int)(player.y + player.size / 2 - 5));
        }
    }

    /** สุ่มเกิดซอมบี้ (จำกัดให้อยู่เลนถนน) และใน multiplayer ให้บอกฝั่งอื่นด้วย (id/speed/type) */
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

    /** ซิงก์สถานะเกมกับผู้เล่นคนอื่น (ตำแหน่งซอมบี้/สกอร์/ตำแหน่งเรา/สถานะเรา) */
    void syncGameState() {
        if (!isMultiplayer || gameClient == null) return;

        // ส่งตำแหน่งซอมบี้ทั้งหมดในรูปแบบ id:x,y;id:x,y;...
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

        // ส่งสถานะผู้เล่น (ไม่รวมตำแหน่ง เพื่อป้องกันการกระตุก) ใช้ 0,0 แทนตำแหน่ง
        gameClient.sendMessage(
                "PLAYER_STATE:" + playerName + ":" +
                        "0,0," + score + "," + !gameOver
        );

    }

    /** เริ่มเธรดทั้งหมด (เกม/ยิง/ซอมบี้ และถ้า multiplayer: sync/check) */
    private void startGameThreads() {
        gameRunning = true;
        shootingActive = true;
        zombieSpawningActive = true;

        // เธรดอัปเดตเกมหลัก (~60 FPS): อัปเดตสถานะ + สั่ง repaint
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

            // เธรดตรวจสอบสถานะเกม (ทุก 1 วินาที) — ใช้เช็คว่าทุกคนตายหรือยัง
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

    /** หยุดเธรดทั้งหมดอย่างสุภาพ (interrupt + join ด้วย timeout) */
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

    /** อัปเดตเกมหนึ่งเฟรม: ผู้เล่น/กระสุน/ซอมบี้/ชนกัน/ชนะ-แพ้/ล้างกระสุน */
    private void updateGame() {
        // โหมดเดี่ยว: ถ้าจบเกมแล้วก็ไม่ต้องอัปเดตต่อ
        // โหมดหลายคน: ถ้าเราตาย แต่อีกคนยังเล่น ก็ยังอัปเดตเพื่อดูต่อ
        if (gameOver && (!isMultiplayer || areAllPlayersDead())) {
            return;
        }

        // อัปเดตผู้เล่นหลัก (เฉพาะเมื่อยังไม่ตาย)
        if (!gameOver) {
            // อัปเดตตำแหน่งผู้เล่นแบบต่อเนื่อง
            player.update();
        }

        // ส่งตำแหน่งเราแบบเป็นช่วง ๆ (ทุก ~100ms) เพื่อ sync ภาพฝั่งอื่น
        long currentTime = System.currentTimeMillis();
        if (isMultiplayer && gameClient != null && (currentTime - lastPositionUpdate >= 100)) {
            gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + (int)player.x + "," + (int)player.y);
            lastPositionUpdate = currentTime;
        }

        // อัปเดตการเคลื่อนของกระสุน/ซอมบี้ (ใช้สำเนา list เพื่อกัน ConcurrentModification)
        for (Bullet b : new ArrayList<>(bullets)) b.update();
        for (Zombie z : new ArrayList<>(zombies)) z.update();

        List<Bullet> bulletsToRemove = new ArrayList<>();
        List<Zombie> zombiesToRemove = new ArrayList<>();

        // ตรวจชน: กระสุนชนซอมบี้ → ลดเลือด/ตาย → เพิ่มคะแนนฝั่งเรา/แจ้งคนอื่น
        for (Bullet b : bullets) {
            Rectangle br = b.getBounds();
            for (Zombie z : zombies) {
                if (br.intersects(z.getBounds())) {
                    z.health -= b.damage;
                    bulletsToRemove.add(b);
                    if (z.health <= 0) {
                        zombiesToRemove.add(z);
                        // เพิ่มสกอร์เฉพาะกรณีเรายังไม่ตาย
                        if (!gameOver) {
                            score += 10;
                            // ชนะเมื่อคะแนนถึง 500
                            if (score >= 500) {
                                handlePlayerWin(playerName);
                            }
                            // multiplayer: แจ้งคนอื่นว่าซอมบี้ตัวนี้ตาย + อัปเดตสกอร์ของเรา
                            if (isMultiplayer && gameClient != null) {
                                gameClient.sendMessage("ZOMBIE_KILLED:" + z.id);
                                gameClient.sendMessage("PLAYER_SCORE:" + playerName + ":" + score);
                            }
                        }
                    }
                }
            }
        }

        bullets.removeAll(bulletsToRemove);
        zombies.removeAll(zombiesToRemove);

        // ตรวจแพ้: ซอมบี้ชนเรา หรือ ซอมบี้ทะลุถึง x = 250 (เฉพาะเมื่อเรายังไม่ตาย)
        if (!gameOver) {
            for (Zombie z : zombies) {
                if (z.getBounds().intersects(player.getBounds())) {
                    endGame();
                    break;
                }
            }

            for (Zombie z : zombies) {
                if (z.x <= 250) {
                    endGame();
                    break;
                }
            }
        }

        // ลบกระสุนที่ออกนอกจอ
        bullets.removeIf(b -> b.x > WIDTH);
    }

    /** วาดทั้งหมด: พื้นหลัง, ผู้เล่น, ผู้เล่นอื่น, กระสุน, ซอมบี้, HUD คะแนน, จอ Game Over/MVP */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(bg, 0, 0, this);

        // วาดผู้เล่นเรา
        player.draw(g);

        // วาดผู้เล่นคนอื่นในโหมด multiplayer (ก็อบปี้ map ก่อนกัน ConcurrentModification)
        if (isMultiplayer) {
            Map<String, Player> safeOtherPlayers = new HashMap<>(otherPlayers);
            for (Map.Entry<String, Player> entry : safeOtherPlayers.entrySet()) {
                Player otherPlayer = entry.getValue();
                String otherPlayerName = entry.getKey();
                otherPlayer.draw(g);
                drawPlayerName((Graphics2D) g, otherPlayer, otherPlayerName);
            }
        }

        // วาดชื่อผู้เล่นเราเหนือหัว
        drawPlayerName((Graphics2D) g);

        // วาดกระสุน (safe copy)
        List<Bullet> safeBullets = new ArrayList<>(bullets);
        for (Bullet b : safeBullets) b.draw(g);

        // วาดซอมบี้ (safe copy)
        List<Zombie> safeZombies = new ArrayList<>(zombies);
        for (Zombie z : safeZombies) z.draw(g);

        // คะแนนของเรา (HUD)
        g.setColor(Color.WHITE);
        g.setFont(new Font("Tahoma", Font.BOLD, 20));
        g.drawString("Your Score: " + score, 20, 30);

        // คะแนน/สถานะของผู้เล่นคนอื่น (ถ้า -1 = ตาย)
        if (isMultiplayer) {
            int ys = 60;
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

        // หน้าจอ Game Over / MVP เมื่อจบเกม
        if (gameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("Tahoma", Font.BOLD, 50));

            if (isMultiplayer) {
                if (areAllPlayersDead()) {
                    // จบเกมเพราะทุกคนตาย
                    g.drawString("GAME OVER", WIDTH / 2 - 190, HEIGHT / 2);
                    g.setFont(new Font("Tahoma", Font.BOLD, 20));
                    g.setColor(Color.YELLOW);
                    g.drawString("All players died. Return to main menu to play again.", WIDTH / 2 - 220, HEIGHT / 2 + 40);
                } else {
                    // มีผู้ชนะ ให้แสดง MVP
                    if (winnerName != null && !winnerName.isEmpty()) {
                        g.drawString("MVP: " + winnerName, WIDTH / 2 , HEIGHT / 2);
                        g.setFont(new Font("Tahoma", Font.BOLD, 20));
                        g.setColor(Color.YELLOW);
                        g.drawString("First to reach 500 points!", WIDTH / 2, HEIGHT / 2 + 40);
                    } else {
                        // เผื่อกรณีไม่ได้รับแพ็กเก็ตผู้ชนะก็ลองหาจากคะแนน
                        String winner = findWinner();
                        if (winner != null && !winner.isEmpty()) {
                            g.drawString("MVP: " + winner, WIDTH / 2 - 190, HEIGHT / 2);
                            g.setFont(new Font("Tahoma", Font.BOLD, 20));
                            g.setColor(Color.YELLOW);
                            g.drawString("First to reach 500 points!", WIDTH / 2 - 100, HEIGHT / 2 + 40);
                        } else {
                            // เรายังอยู่ในสถานะตาย แต่คนอื่นยังเล่นอยู่
                            g.drawString("YOU DIED", WIDTH / 2 - 190, HEIGHT / 2);
                            g.setFont(new Font("Tahoma", Font.BOLD, 20));
                            g.setColor(Color.YELLOW);
                            g.drawString("Waiting for other players to finish...", WIDTH / 2 - 100, HEIGHT / 2 + 40);
                        }
                    }
                }
            } else {
                // โหมดเดี่ยว: จบแล้วให้กลับเมนูเพื่อเล่นใหม่
                g.drawString("GAME OVER", WIDTH / 2 - 100, HEIGHT / 2);
                g.setFont(new Font("Tahoma", Font.BOLD, 20));
                g.setColor(Color.YELLOW);
                g.drawString("Return to main menu to play again", WIDTH / 2 - 100, HEIGHT / 2 + 40);
            }
        }
    }

    /** วาดชื่อผู้เล่นเราให้อยู่เหนือหัว */
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

        /*// กล่องพื้นหลังโปร่ง ๆ ให้อ่านง่าย (ปิดไว้ ถ้าต้องการเปิดใช้ค่อยเอาออก)
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

    /** วาดชื่อผู้เล่นอื่นให้อยู่เหนือหัว (ใช้สีเหลืองเพื่อแยกจากเรา) */
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



    /** จบเกมฝั่งเรา: หยุดกิจกรรมฝั่งเราและแจ้งเครือข่ายว่าตาย (-1) */
    void endGame() {
        gameOver = true;

        // โหมดเดี่ยว: หยุดทุกเธรดเลย
        if (!isMultiplayer) {
            stopGameThreads();
        } else {
            // โหมดหลายคน: หยุดเฉพาะกิจกรรมของเรา (ยิง/เกิดซอมบี้) แต่ลูปเกม/ซิงก์ยังรันเพื่อดูคนอื่น
            shootingActive = false; // Stop shooting for this player
            zombieSpawningActive = false; // Stop spawning zombies for this player
            // Keep gameThread and syncThread running to receive updates from other players
        }

        // แจ้งฝั่งอื่นว่าเราตายแล้ว และ mark สกอร์เราเป็น -1
        if (isMultiplayer && gameClient != null) {
            // ส่งตำแหน่งสุดท้ายก่อน mark ตาย
            gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + (int)player.x + "," + (int)player.y);
            gameClient.sendMessage("PLAYER_DIED:" + playerName);
            // Mark this player as dead in the scores
            playerScores.put(playerName, -1);
        }
    }

    /** จัดการเมื่อมีคนชนะ: เก็บชื่อผู้ชนะ หยุดเธรด และแจ้งทุกคน (ถ้าเราเป็นคนทริกเกอร์) */
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

    /** ทำความสะอาดเมื่อปิดเกม/หน้าต่าง (หยุดเธรดทั้งหมด) */
    public void cleanup() {
        stopGameThreads();
    }

    /** เช็คทุกคนตายหรือยัง (multiplayer: ดู map score == -1 ทุกคน) */
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

    /** รับข้อความเครือข่ายทั้งหมดจาก GameClient แล้วอัปเดตสถานะภายในเกม */
    public void handleNetworkMessage(String message) {
        if (!isMultiplayer) return;

        try {
            // อัปเดตตำแหน่งผู้เล่นรายคน (ใช้ lerp ลดกระตุก)
            if (message.startsWith("PLAYER_POSITION:")) {
                String[] parts = message.substring("PLAYER_POSITION:".length()).split(":");
                if (parts.length >= 2) {
                    String playerName = parts[0];
                    String[] coords = parts[1].split(",");
                    if (coords.length >= 2) {
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);

                        // Update or create player (ยกเว้นตัวเอง)
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
                // แพ็กเก็ตสถานะรวม (STATE|name|x,y,score,alive) — ใช้ซิงก์ snapshot รวดเดียว
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
                // มีผู้เล่นอื่นยิงกระสุน → สร้างกระสุนที่ตำแหน่งนั้น
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
                // สร้างซอมบี้ตามข้อมูลจาก Host/ผู้ส่ง
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
                // Snapshot ตำแหน่งซอมบี้ชุดใหญ่ (id:x,y;id:x,y;...)
                String[] zombieData = message.substring("ZOMBIE_POSITIONS:".length()).split(";");
                // สร้าง map ของซอมบี้ที่มีอยู่แล้ว
                Map<String, Zombie> existingZombies = new HashMap<>();
                for (Zombie z : zombies) {
                    existingZombies.put(z.id, z);
                }

                // ประมวลผลข้อมูลซอมบี้ที่ได้รับ
                for (String data : zombieData) {
                    String[] parts = data.split(":");
                    if (parts.length >= 3) {           // *** รูปแบบฝั่งนี้คาดว่า id: x,y  (ระวังรูปแบบที่ส่งมา)
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
                // ลบซอมบี้ตาม id ที่ถูกฆ่า
                String zombieId = message.substring("ZOMBIE_KILLED:".length());
                // Remove zombie by ID for proper synchronization
                zombies.removeIf(z -> z.id.equals(zombieId));
            } else if (message.startsWith("PLAYER_SCORE:")) {
                // อัปเดตคะแนนของผู้เล่นที่ระบุ
                String[] parts = message.substring("PLAYER_SCORE:".length()).split(":");
                if (parts.length >= 2) {
                    String playerName = parts[0];
                    int playerScore = Integer.parseInt(parts[1]);
                    playerScores.put(playerName, playerScore);

                    // ถ้าถึง 500 แต้ม ให้ประกาศชนะ
                    if (playerScore >= 500) {
                        handlePlayerWin(playerName);
                    }
                }
            } else if (message.startsWith("PLAYER_LIST:")) {
                // รายชื่อผู้เล่นทั้งหมดตอนเข้า/รีเฟรชห้อง (ใช้สร้าง otherPlayers เริ่มต้น)
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
                // เมื่อมีผู้เล่นใหม่เข้าห้อง
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

                        // ส่งตำแหน่งของเราให้ผู้เล่นใหม่รับรู้
                        if (isMultiplayer && gameClient != null) {
                            gameClient.sendMessage("PLAYER_POSITION:" + this.playerName + ":" + (int)player.x + "," + (int)player.y);
                        }
                    }
                }
            } else if (message.startsWith("PLAYER_LEFT:")) {
                // เมื่อมีผู้เล่นออกจากห้อง
                String leftPlayerName = message.substring("PLAYER_LEFT:".length());
                if (!leftPlayerName.isEmpty()) {
                    otherPlayers.remove(leftPlayerName);
                    playerScores.remove(leftPlayerName);
                }
            } else if (message.startsWith("PLAYER_WIN:")) {
                // ประกาศผู้ชนะ
                String winnerName = message.substring("PLAYER_WIN:".length());
                this.winnerName = winnerName; // Store the winner's name for MVP display
                handlePlayerWin(winnerName);
            } else if (message.startsWith("PLAYER_CHARACTER:")) {
                // ผู้เล่นประกาศชนิดตัวละคร
                String[] parts = message.substring("PLAYER_CHARACTER:".length()).split(":");
                if (parts.length >= 2) {
                    String playerName = parts[0];
                    String characterType = parts[1];

                    // อัปเดต/สร้างผู้เล่นและชนิดตัวละคร
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
                // Host กดเริ่มเกม
                gameOver = false;
                isHostPlayer = false;

                if (isMultiplayer && gameClient != null) {
                    gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + player.x + "," + player.y);
                }
            } else if (message.startsWith("PLAYER_DIED:")) {
                // มีผู้เล่นตาย → mark สกอร์ของเขาเป็น -1
                String deadPlayer = message.substring("PLAYER_DIED:".length());
                if (!deadPlayer.equals(playerName)) { // Don't process our own death message
                    playerScores.put(deadPlayer, -1); // Mark as dead with special score
                }

                // ถ้าทุกคนตายแล้วให้ตั้งธง allPlayersDead และ repaint
                if (areAllPlayersDead()) {
                    allPlayersDead = true;
                }
                repaint();


            } else if (message.startsWith("GAME_RESTART")) {
                // Handler ว่าง (เผื่ออนาคต)
            } else if (message.startsWith("HOST_RESTART")) {
                // Handler ว่าง (เผื่ออนาคต)
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing network message: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error handling network message: " + e.getMessage());
        }
    }

    // ตั้งธงว่าเราเป็น Host (ใช้ในฝั่ง Host ที่เล่นในเครื่องเดียวกับเซิร์ฟเวอร์)
    public void setAsHost() {
        this.isHostPlayer = true;
    }

    // helper ตรวจว่าเป็น Host ไหม (ฟังก์ชันนี้ยังไม่ได้ใช้ในลอจิกอื่น)
    private boolean isHost() {
        // In solo mode, the player is always the host
        if (!isMultiplayer) {
            return true;
        }
        // In multiplayer mode, check if this is the host player
        return isHostPlayer;
    }

    /** หาใครชนะ (ถึง 500 แต้ม) — ใช้เป็น fallback ตอนวาด MVP ถ้าไม่เจอแพ็กเก็ตประกาศชนะ */
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

/** คลาสผู้เล่น: เก็บสถานะ ตำแหน่ง การเคลื่อนที่ การวาด */
class Player {
    double x, y; // เปลี่ยนเป็น double เพื่อการเคลื่อนไหวที่นุ่มนวล
    int size = 60; // Increased size to better display player image
    double speed = 1.5; // ลดความเร็วลงเพื่อให้เคลื่อนไหวนุ่มนวลขึ้น
    Color playerColor = Color.CYAN; // สีของผู้เล่น (สำหรับผู้เล่นอื่น)
    boolean isMainPlayer = false; // ตัวแปรเพื่อแยกผู้เล่นหลักกับผู้เล่นอื่น (ไม่ได้ใช้ในลอจิกอื่น)
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

    // Static initializer to load images safely (โหลดครั้งเดียวทั้งคลาส)
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

    // ผู้เล่นหลัก (ใช้รูป player1, ชาย)
    Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.isMainPlayer = true; // ผู้เล่นหลักใช้รูป player1.png
        this.characterType = "male"; // default to male
    }

    // ผู้เล่นอื่น + ระบายสีทับ
    Player(int x, int y, Color color) {
        this.x = x;
        this.y = y;
        this.playerColor = color;
        this.isMainPlayer = true; // เปลี่ยนให้ผู้เล่นอื่นใช้รูปเหมือนกัน
        this.characterType = "male"; // default to male
    }

    // ผู้เล่นหลัก + ระบุชนิดตัวละคร
    Player(int x, int y, String characterType) {
        this.x = x;
        this.y = y;
        this.isMainPlayer = true;
        this.characterType = characterType != null ? characterType : "male";
    }

    // ผู้เล่นอื่น + สี + ระบุชนิดตัวละคร
    Player(int x, int y, Color color, String characterType) {
        this.x = x;
        this.y = y;
        this.playerColor = color;
        this.isMainPlayer = true; // NOTE: ค่านี้ไม่ได้ใช้ต่อ
        this.characterType = characterType != null ? characterType : "male";
    }

    // วาดผู้เล่น (รูป 60x75) + ถ้าเป็นผู้เล่นอื่นให้ทับด้วย overlay สีบาง ๆ
    void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        Image imageToDraw = null;
        if ("female".equals(characterType) && femaleImage != null) {
            imageToDraw = femaleImage;
        } else if (playerImage != null) {
            imageToDraw = playerImage;
        }

        if (imageToDraw == null) {
            // กรณีโหลดรูปไม่สำเร็จ ใช้สี่เหลี่ยมแทน
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

    // เมธอดสำหรับเริ่มการเคลื่อนไหว (ตั้งธงเมื่อกดคีย์)
    void startMoveUp() { movingUp = true; }
    void startMoveDown() { movingDown = true; }
    void startMoveLeft() { movingLeft = true; }
    void startMoveRight() { movingRight = true; }

    // เมธอดสำหรับหยุดการเคลื่อนไหว (เคลียร์ธงเมื่อปล่อยคีย์)
    void stopMoveUp() { movingUp = false; }
    void stopMoveDown() { movingDown = false; }
    void stopMoveLeft() { movingLeft = false; }
    void stopMoveRight() { movingRight = false; }

    // อัปเดตตำแหน่งแบบต่อเนื่อง (ถูกเรียกทุกเฟรมจากลูปเกม)
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

    // เมธอดเก่าเพื่อความเข้ากันได้ (deprecated) — mapping ไป startMove*
    void moveUp() { startMoveUp(); }
    void moveLeft() { startMoveLeft(); }
    void moveRight() { startMoveRight(); }
    void moveDown() { startMoveDown(); }

    // hitbox สำหรับชนกับซอมบี้/กระสุน (ขนาด 60x75 ให้ตรงกับรูป)
    Rectangle getBounds() {
        // Use actual image dimensions for collision detection
        return new Rectangle((int)x, (int)y, size, 75);
    }
}


class Zombie {
    Random  rand = new Random();
    int x, y;
    int size = 60;
    double speed;
    String id;
    int health = 30; // เลือดเริ่มต้นของซอมบี้
    String zombieType = "type1";

    // โหลดรูปซอมบี้ทั้ง 4 แบบ
    static ImageIcon zombieIcon1;
    static ImageIcon zombieIcon2;
    static ImageIcon zombieIcon3;
    static ImageIcon zombieIcon4;
    static Image zombieImage1;
    static Image zombieImage2;
    static Image zombieImage3;
    static Image zombieImage4;

    // โหลดรูปซอมบี้ทั้ง 4 ประเภท (static block เรียกครั้งเดียว)
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

    // สร้างซอมบี้ฝั่งเรา (สุ่ม type และตั้งค่า speed/HP ตามชนิด)
    Zombie(int x, int y) {
        this.x = x;
        this.y = y;
        this.id = java.util.UUID.randomUUID().toString();
        int typeNum = rand.nextInt(4) + 1; // 1-4
        this.zombieType = "type" + typeNum;
        setZombieProperties();
    }

    // สร้างซอมบี้จาก id (ใช้เวลา sync)
    Zombie(int x, int y, String id) {
        this.x = x;
        this.y = y;
        this.id = id;
        int typeNum = rand.nextInt(4) + 1; // 1-4
        this.zombieType = "type" + typeNum;
        setZombieProperties();
    }

    // สร้างซอมบี้จาก id+speed (type จะสุ่ม)
    Zombie(int x, int y, String id, double speed) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.id = id;
        int typeNum = rand.nextInt(4) + 1; // 1-4
        this.zombieType = "type" + typeNum;

    }

    // สร้างซอมบี้จาก id+speed+type (สำหรับ sync ที่กำหนดชนิดมาตรง ๆ)
    Zombie(int x, int y, String id, double speed, String zombieType) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.id = id;
        this.zombieType = zombieType != null ? zombieType : "type1";

    }

    /** ตั้งค่าความเร็วและพลังชีวิตตามชนิดของซอมบี้ (ใช้ตอนเราเป็นคน spawn) */
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

    // วาดซอมบี้ตามชนิด + แถบ HP (สีบอกชนิด)
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

        // แถบ HP พื้นแดง
        g.setColor(Color.RED);
        g.fillRect(x, y - 10, size, 5);

        // Set health bar color based on zombie type (สีแถบ)
        Color healthColor;
        switch (zombieType) {
            case "type1": healthColor = Color.GREEN; break;      // Balanced - Green
            case "type2": healthColor = Color.YELLOW; break;     // Fast - Yellow
            case "type3": healthColor = Color.BLUE; break;       // Strong - Blue
            case "type4": healthColor = Color.ORANGE; break;     // Very Fast - Orange
            default: healthColor = Color.GREEN; break;
        }
        g.setColor(healthColor);

        // คำนวณความยาวแถบตามสัดส่วน HP ปัจจุบัน
        double maxHealth = getMaxHealth();
        int hpBar = Math.max(0, (int) ((health / maxHealth) * size));
        g.fillRect(x, y - 10, hpBar, 5);
    }

    /** ค่า HP สูงสุดตามชนิด (ใช้คำนวณสัดส่วนแถบ) */
    private double getMaxHealth() {
        switch (zombieType) {
            case "type1": return 30.0;
            case "type2": return 20.0;
            case "type3": return 40.0;
            case "type4": return 15.0;
            default: return 30.0;
        }
    }

    // อัปเดตตำแหน่งซอมบี้ (วิ่งไปทางซ้ายด้วย speed)
    void update() {
        x -= speed;
    }

    // hitbox สำหรับชนกับกระสุน/ผู้เล่น (ให้ตรงกับการวาด 60x75)
    Rectangle getBounds() {
        return new Rectangle(x, y, size, size);
    }
}

/** คลาสกระสุน: วาด/เคลื่อน/ชน */
class Bullet {
    int x, y;
    int size = 20;
    int speed = 16;
    int damage = 10; // ดาเมจกระสุนนัดละ 10

    // โหลดรูปกระสุน (static: โหลดครั้งเดียว)
    static ImageIcon bulletIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "bullet.png");
    static Image bulletImage = bulletIcon.getImage();

    Bullet(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // วาดกระสุน (ขนาด 20x20)
    void draw(Graphics g) {
        g.drawImage(bulletImage, x, y, size, size, null);
    }

    // กระสุนวิ่งไปทางขวาด้วย speed
    void update() {
        x += speed;
    }

    // hitbox สำหรับชนกับซอมบี้
    Rectangle getBounds() {
        return new Rectangle(x, y, size, size);
    }
}
