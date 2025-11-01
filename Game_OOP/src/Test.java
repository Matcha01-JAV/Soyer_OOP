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

import network.*;

class GamePanel extends JPanel {

    static final int WIDTH = 1262;
    static final int HEIGHT = 768;

    String playerName;
    String characterType = "male";

    boolean isMultiplayer = false;
    GameClient gameClient = null;
    Map<String, Player> otherPlayers = new ConcurrentHashMap<>();
    Map<String, Integer> playerScores = new ConcurrentHashMap<>();
    Map<String, String> playerCharacters = new ConcurrentHashMap<>();
    boolean isHostPlayer = false;
    boolean allPlayersDead = false;
    String winnerName = null;
    JButton nextButton = null;

    ImageIcon bgIcon = new ImageIcon(
            System.getProperty("user.dir") + File.separator + "Game_OOP" + File.separator + "src"
                    + File.separator + "game" + File.separator + "game_map.png");
    Image bg = bgIcon.getImage();

    Thread gameThread;
    Thread shootThread;
    Thread zombieThread;
    Thread syncThread;
    Thread checkThread;

    volatile boolean gameRunning = false;
    volatile boolean shootingActive = false;
    volatile boolean zombieSpawningActive = false;
    volatile boolean syncActive = false;
    volatile boolean checkActive = false;

    Player player;
    List<Zombie> zombies;
    List<Bullet> bullets;
    Random random = new Random();

    int score = 0;
    boolean gameOver = false;
    long lastPositionUpdate = 0;

    GamePanel() {
        this("Player");
    }

    GamePanel(String name) {
        this(name, "male");
    }

    GamePanel(String name, String characterType) {
        if (name == null) {
            this.playerName = "Player";
        } else if (name.isBlank()) {
            this.playerName = "Player";
        } else {
            this.playerName = name.trim();
        }
        if (characterType != null) {
            this.characterType = characterType;
        } else {
            this.characterType = "male";
        }
        this.isMultiplayer = false;
        this.isHostPlayer = true;
        initializeGame();
    }

    GamePanel(String name, GameClient client) {
        this(name, client, "male");
    }

    GamePanel(String name, GameClient client, String characterType) {
        if (name == null || name.isBlank()) {
            this.playerName = "Player";
        } else {
            this.playerName = name.trim();
        }
        if (characterType != null) {
            this.characterType = characterType;
        } else {
            this.characterType = "male";
        }
        this.gameClient = client;
        this.isMultiplayer = true;
        this.isHostPlayer = false;
        initializeGame();
    }

    private void initializeGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        gameOver = false;
        winnerName = null;

        player = new Player(300, 359, characterType);
        zombies = new ArrayList<>();
        bullets = new ArrayList<>();

        playerScores.put(playerName, 0);
        playerCharacters.put(playerName, characterType);

        startGameThreads();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (gameOver) {
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W)
                    player.startMoveUp();
                if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S)
                    player.startMoveDown();
                if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A)
                    player.startMoveLeft();
                if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D)
                    player.startMoveRight();

                long currentTime = System.currentTimeMillis();
                if (isMultiplayer && gameClient != null && (currentTime - lastPositionUpdate >= 50)) {
                    gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + (int)player.x + "," + (int)player.y);
                    gameClient.sendMessage("PLAYER_CHARACTER:" + playerName + ":" + characterType);
                    lastPositionUpdate = currentTime;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (gameOver) return;

                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W)
                    player.stopMoveUp();
                if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S)
                    player.stopMoveDown();
                if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A)
                    player.stopMoveLeft();
                if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D)
                    player.stopMoveRight();

                long currentTime = System.currentTimeMillis();
                if (isMultiplayer && gameClient != null && (currentTime - lastPositionUpdate >= 50)) {
                    gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + (int)player.x + "," + (int)player.y);
                    gameClient.sendMessage("PLAYER_CHARACTER:" + playerName + ":" + characterType);
                    lastPositionUpdate = currentTime;
                }
            }
        });

        if (isMultiplayer && gameClient != null) {
            gameClient.sendMessage("PLAYER_READY:" + playerName);
            gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + (int)player.x + "," + (int)player.y);
            gameClient.sendMessage("PLAYER_CHARACTER:" + playerName + ":" + characterType);
        }
    }

    void shoot() {
        if (gameOver) return;
        synchronized (bullets) {
            bullets.add(new Bullet((int)(player.x + player.size), (int)(player.y + player.size / 2 - 5)));
        }

        if (isMultiplayer && gameClient != null) {
            gameClient.sendMessage("PLAYER_SHOOT:" + playerName + ":" +
                    (int)(player.x + player.size) + "," + (int)(player.y + player.size / 2 - 5));
        }
    }

    void spawnZombie() {
        if (gameOver) return;
        int roadTopY = 340;
        int roadBottomY = 700;
        int y = roadTopY + random.nextInt(Math.max(1, roadBottomY - roadTopY - 40));
        Zombie zombie = new Zombie(WIDTH - 50, y);
        synchronized (zombies) {
            zombies.add(zombie);
        }
        System.out.println("Spawned zombie: " + zombie.zombieType + " (speed: " + String.format("%.2f", zombie.speed) + ", health: " + zombie.health + ")");

        if (isMultiplayer && gameClient != null) {
            gameClient.sendMessage("ZOMBIE_SPAWN:" + zombie.id + ":" + (WIDTH - 50) + "," + y + "," + zombie.speed + "," + zombie.zombieType);
        }
    }

    void syncGameState() {
        if (!isMultiplayer || gameClient == null) return;

        StringBuilder zombiePositions = new StringBuilder("ZOMBIE_POSITIONS:");
        List<Zombie> safeZombies = new ArrayList<>(zombies);
        for (Zombie z : safeZombies) {
            zombiePositions.append(z.id).append(":").append(z.x).append(",").append(z.y).append(";");
        }
        if (zombiePositions.length() > "ZOMBIE_POSITIONS:".length()) {
            zombiePositions.setLength(zombiePositions.length() - 1);
            gameClient.sendMessage(zombiePositions.toString());
        }

        gameClient.sendMessage("PLAYER_SCORE:" + playerName + ":" + score);
        gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + (int)player.x + "," + (int)player.y);
        gameClient.sendMessage(
                "PLAYER_STATE:" + playerName + ":" +
                        "0,0," + score + "," + !gameOver
        );

    }

    private void startGameThreads() {
        gameRunning = true;
        shootingActive = true;
        zombieSpawningActive = true;

        gameThread = new Thread(() -> {
            while (gameRunning) {
                try {
                    updateGame();
                    SwingUtilities.invokeLater(this::repaint);
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        gameThread.setName("GameThread");
        gameThread.start();

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

    private void stopGameThreads() {
        gameRunning = false;
        shootingActive = false;
        zombieSpawningActive = false;
        syncActive = false;
        checkActive = false;

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

    private void updateGame() {
        if (gameOver && (!isMultiplayer || areAllPlayersDead())) {
            return;
        }

        if (!gameOver) {
            player.update();
        }

        long currentTime = System.currentTimeMillis();
        if (isMultiplayer && gameClient != null && (currentTime - lastPositionUpdate >= 100)) {
            gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + (int)player.x + "," + (int)player.y);
            lastPositionUpdate = currentTime;
        }

        for (Bullet b : new ArrayList<>(bullets)) b.update();
        for (Zombie z : new ArrayList<>(zombies)) z.update();

        List<Bullet> bulletsToRemove = new ArrayList<>();
        List<Zombie> zombiesToRemove = new ArrayList<>();

        List<Bullet> safeBullets = new ArrayList<>(bullets);
        List<Zombie> safeZombies = new ArrayList<>(zombies);

        for (Bullet b : safeBullets) {
            Rectangle br = b.getBounds();
            for (Zombie z : safeZombies) {
                if (br.intersects(z.getBounds())) {
                    z.health -= b.damage;
                    bulletsToRemove.add(b);
                    if (z.health <= 0) {
                        zombiesToRemove.add(z);
                        if (!gameOver) {
                            score += 10;
                            if (score >= 500) {
                                handlePlayerWin(playerName);
                            }
                            if (isMultiplayer && gameClient != null) {
                                gameClient.sendMessage("ZOMBIE_KILLED:" + z.id);
                                gameClient.sendMessage("PLAYER_SCORE:" + playerName + ":" + score);
                            }
                        }
                    }
                }
            }
        }

        synchronized (bullets) {
            bullets.removeAll(bulletsToRemove);
        }
        synchronized (zombies) {
            zombies.removeAll(zombiesToRemove);
        }

        if (!gameOver) {
            List<Zombie> safeZombiesCheck = new ArrayList<>(zombies);
            for (Zombie z : safeZombiesCheck) {
                if (z.getBounds().intersects(player.getBounds())) {
                    endGame();
                    break;
                }
            }

            for (Zombie z : safeZombiesCheck) {
                if (z.x <= 250) {
                    endGame();
                    break;
                }
            }
        }

        synchronized (bullets) {
            bullets.removeIf(b -> b.x > WIDTH);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(bg, 0, 0, this);

        player.draw(g);

        if (isMultiplayer) {
            Map<String, Player> safeOtherPlayers = new HashMap<>(otherPlayers);
            for (Map.Entry<String, Player> entry : safeOtherPlayers.entrySet()) {
                Player otherPlayer = entry.getValue();
                String otherPlayerName = entry.getKey();
                otherPlayer.draw(g);
                drawPlayerName((Graphics2D) g, otherPlayer, otherPlayerName);
            }
        }

        drawPlayerName((Graphics2D) g);

        List<Bullet> safeBullets = new ArrayList<>(bullets);
        for (Bullet b : safeBullets) b.draw(g);

        List<Zombie> safeZombies = new ArrayList<>(zombies);
        for (Zombie z : safeZombies) z.draw(g);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Tahoma", Font.BOLD, 20));
        g.drawString("Your Score: " + score, 20, 30);

        if (isMultiplayer) {
            int ys = 60;
            Map<String, Integer> safePlayerScores = new HashMap<>(playerScores);
            for (Map.Entry<String, Integer> entry : safePlayerScores.entrySet()) {
                if (!entry.getKey().equals(playerName)) {
                    if (entry.getValue() == -1) {
                        g.drawString(entry.getKey() + ": DIE ", 20, ys);
                    } else {
                        g.drawString(entry.getKey() + ": " + entry.getValue(), 20, ys);
                    }
                    ys += 30;
                }
            }
        }

        if (gameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("Tahoma", Font.BOLD, 50));

            if (isMultiplayer) {
                if (areAllPlayersDead()) {
                    g.drawString("GAME OVER", WIDTH / 2 - 190, HEIGHT / 2);
                    g.setFont(new Font("Tahoma", Font.BOLD, 20));
                    g.setColor(Color.YELLOW);
                    g.drawString("All players died. Return to main menu to play again.", WIDTH / 2 - 220, HEIGHT / 2 + 40);
                } else {
                    if (winnerName != null && !winnerName.isEmpty()) {
                        g.drawString("MVP: " + winnerName, WIDTH / 2, HEIGHT / 2);
                        g.setFont(new Font("Tahoma", Font.BOLD, 20));
                        g.setColor(Color.YELLOW);
                        g.drawString("First to reach 500 points!", WIDTH / 2, HEIGHT / 2 + 40);
                    } else {
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
                g.drawString("GAME OVER", WIDTH / 2 - 100, HEIGHT / 2);
                g.setFont(new Font("Tahoma", Font.BOLD, 20));
                g.setColor(Color.YELLOW);
                g.drawString("Return to main menu to play again", WIDTH / 2 - 100, HEIGHT / 2 + 40);
            }
        }
    }

    private void drawPlayerName(Graphics2D g2) {
        if (playerName == null || playerName.isBlank()) return;

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font font = new Font("Tahoma", Font.BOLD, 18);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();

        int textW = fm.stringWidth(playerName);
        int textH = fm.getAscent();

        int imageWidth = 60;
        int centerX = (int)(player.x + imageWidth / 2);
        int nameX = centerX - textW / 2;
        int nameY = (int)(player.y - 8);
        if (nameY - textH < 0) nameY = textH + 4;

        g2.setColor(Color.WHITE);
        g2.drawString(playerName, nameX, nameY);
    }

    private void drawPlayerName(Graphics2D g2, Player player, String name) {
        if (player == null || name == null || name.isEmpty()) return;

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font font = new Font("Tahoma", Font.BOLD, 18);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();

        int textW = fm.stringWidth(name);
        int textH = fm.getAscent();

        int imageWidth = 60;
        int centerX = (int)(player.x + imageWidth / 2);
        int nameX = centerX - textW / 2;
        int nameY = (int)(player.y - 8);
        if (nameY - textH < 0) nameY = textH + 4;

        g2.setColor(Color.YELLOW);
        g2.drawString(name, nameX, nameY);
    }

    void endGame() {
        gameOver = true;

        if (!isMultiplayer) {
            stopGameThreads();
        } else {
            shootingActive = false;
            zombieSpawningActive = false;
        }

        if (isMultiplayer && gameClient != null) {
            gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + (int)player.x + "," + (int)player.y);
            gameClient.sendMessage("PLAYER_DIED:" + playerName);
            playerScores.put(playerName, -1);
        }
    }

    void handlePlayerWin(String winnerName) {
        gameOver = true;
        this.winnerName = winnerName;
        stopGameThreads();
        if (isMultiplayer && gameClient != null) {
            gameClient.sendMessage("PLAYER_WIN:" + winnerName);
        }
        SwingUtilities.invokeLater(this::repaint);
    }

    public void cleanup() {
        stopGameThreads();
    }

    private boolean areAllPlayersDead() {
        if (!isMultiplayer) {
            return gameOver;
        }

        for (Map.Entry<String, Integer> entry : playerScores.entrySet()) {
            String playerName = entry.getKey();
            Integer score = entry.getValue();
            if (score != null && score != -1) {
                return false;
            }
        }

        return true;
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

                        Player otherPlayer = otherPlayers.get(playerName);
                        if (otherPlayer == null) {
                            if (!playerName.equals(this.playerName)) {
                                Color[] playerColors = {Color.MAGENTA, Color.ORANGE, Color.YELLOW, Color.PINK, Color.LIGHT_GRAY};
                                Color playerColor = playerColors[otherPlayers.size() % playerColors.length];
                                String playerCharacterType = playerCharacters.getOrDefault(playerName, "male");
                                otherPlayer = new Player(x, y, playerColor, playerCharacterType);
                                otherPlayers.put(playerName, otherPlayer);
                                playerScores.put(playerName, 0);
                                System.out.println("Created player: " + playerName + " at (" + x + ", " + y + ") with character type: " + playerCharacterType);
                                if (isMultiplayer && gameClient != null) {
                                    gameClient.sendMessage("PLAYER_POSITION:" + this.playerName + ":" + (int)player.x + "," + (int)player.y);
                                }
                            }
                        } else {
                            double distance = Math.sqrt(Math.pow(x - otherPlayer.x, 2) + Math.pow(y - otherPlayer.y, 2));
                            if (distance > 150) {
                                otherPlayer.x = x;
                                otherPlayer.y = y;
                            } else if (distance > 5) {
                                double lerpFactor = 0.3;
                                otherPlayer.x = otherPlayer.x + (x - otherPlayer.x) * lerpFactor;
                                otherPlayer.y = otherPlayer.y + (y - otherPlayer.y) * lerpFactor;
                            } else {
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
            } else if (message.startsWith("PLAYER_SHOOT:")) {
                String[] parts = message.substring("PLAYER_SHOOT:".length()).split(":");
                if (parts.length >= 2) {
                    String playerName = parts[0];
                    String[] coords = parts[1].split(",");
                    if (coords.length >= 2) {
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        synchronized (bullets) {
                            bullets.add(new Bullet(x, y));
                        }
                    }
                }
            } else if (message.startsWith("ZOMBIE_SPAWN:")) {
                String[] parts = message.substring("ZOMBIE_SPAWN:".length()).split(":");
                if (parts.length >= 2) {
                    String zombieId = parts[0];
                    String[] coords = parts[1].split(",");
                    if (coords.length >= 4) {
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        double speed = Double.parseDouble(coords[2]);
                        String zombieType = coords[3];
                        Zombie zombie = new Zombie(x, y, zombieId, speed, zombieType);
                        synchronized (zombies) {
                            zombies.add(zombie);
                        }
                    } else if (coords.length >= 3) {
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        double speed = Double.parseDouble(coords[2]);
                        Zombie zombie = new Zombie(x, y, zombieId, speed);
                        synchronized (zombies) {
                            zombies.add(zombie);
                        }
                    } else if (coords.length >= 2) {
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        Zombie zombie = new Zombie(x, y, zombieId);
                        synchronized (zombies) {
                            zombies.add(zombie);
                        }
                    }
                }
            } else if (message.startsWith("ZOMBIE_POSITIONS:")) {
                String[] zombieData = message.substring("ZOMBIE_POSITIONS:".length()).split(";");
                Map<String, Zombie> existingZombies = new HashMap<>();
                List<Zombie> safeZombies = new ArrayList<>(zombies);
                for (Zombie z : safeZombies) {
                    existingZombies.put(z.id, z);
                }

                for (String data : zombieData) {
                    String[] parts = data.split(":");
                    if (parts.length >= 3) {
                        String id = parts[0];
                        String[] coords = parts[1].split(",");
                        if (coords.length >= 2) {
                            int x = Integer.parseInt(coords[0]);
                            int y = Integer.parseInt(coords[1]);
                            Zombie zombie = existingZombies.get(id);
                            if (zombie == null) {
                                zombie = new Zombie(x, y, id);
                                synchronized (zombies) {
                                    zombies.add(zombie);
                                }
                            }
                            zombie.x = x;
                            zombie.y = y;
                        }
                    }
                }
            } else if (message.startsWith("ZOMBIE_KILLED:")) {
                String zombieId = message.substring("ZOMBIE_KILLED:".length());
                synchronized (zombies) {
                    zombies.removeIf(z -> z.id.equals(zombieId));
                }
            } else if (message.startsWith("PLAYER_SCORE:")) {
                String[] parts = message.substring("PLAYER_SCORE:".length()).split(":");
                if (parts.length >= 2) {
                    String playerName = parts[0];
                    int playerScore = Integer.parseInt(parts[1]);
                    playerScores.put(playerName, playerScore);
                    if (playerScore >= 500) {
                        handlePlayerWin(playerName);
                    }
                }
            } else if (message.startsWith("PLAYER_LIST:")) {
                String[] players = message.substring("PLAYER_LIST:".length()).split(",");
                for (String player : players) {
                    if (!player.isEmpty() && !player.equals(this.playerName)) {
                        if (!otherPlayers.containsKey(player) && !playerScores.containsKey(player)) {
                            Color[] playerColors = {Color.MAGENTA, Color.ORANGE, Color.YELLOW, Color.PINK, Color.LIGHT_GRAY};
                            Color playerColor = playerColors[otherPlayers.size() % playerColors.length];
                            String playerCharacterType = playerCharacters.getOrDefault(player, "male");
                            Player otherPlayer = new Player(300, 359, playerColor, playerCharacterType);
                            otherPlayers.put(player, otherPlayer);
                            playerScores.put(player, 0);
                        }
                    }
                }
            } else if (message.startsWith("PLAYER_JOINED:")) {
                String newPlayerName = message.substring("PLAYER_JOINED:".length());
                if (!newPlayerName.isEmpty() && !newPlayerName.equals(this.playerName)) {
                    if (!otherPlayers.containsKey(newPlayerName) && !playerScores.containsKey(newPlayerName)) {
                        Color[] playerColors = {Color.MAGENTA, Color.ORANGE, Color.YELLOW, Color.PINK, Color.LIGHT_GRAY};
                        Color playerColor = playerColors[otherPlayers.size() % playerColors.length];
                        String playerCharacterType = playerCharacters.getOrDefault(newPlayerName, "male");
                        Player otherPlayer = new Player(300, 359, playerColor, playerCharacterType);
                        otherPlayers.put(newPlayerName, otherPlayer);
                        playerScores.put(newPlayerName, 0);
                        if (isMultiplayer && gameClient != null) {
                            gameClient.sendMessage("PLAYER_POSITION:" + this.playerName + ":" + (int)player.x + "," + (int)player.y);
                        }
                    }
                }
            } else if (message.startsWith("PLAYER_LEFT:")) {
                String leftPlayerName = message.substring("PLAYER_LEFT:".length());
                if (!leftPlayerName.isEmpty()) {
                    otherPlayers.remove(leftPlayerName);
                    playerScores.remove(leftPlayerName);
                }
            } else if (message.startsWith("PLAYER_WIN:")) {
                String winnerName = message.substring("PLAYER_WIN:".length());
                this.winnerName = winnerName;
                handlePlayerWin(winnerName);
            } else if (message.startsWith("PLAYER_CHARACTER:")) {
                String[] parts = message.substring("PLAYER_CHARACTER:".length()).split(":");
                if (parts.length >= 2) {
                    String playerName = parts[0];
                    String characterType = parts[1];
                    Player otherPlayer = otherPlayers.get(playerName);
                    if (otherPlayer == null) {
                        otherPlayers.put(playerName, otherPlayer);
                        playerScores.put(playerName, 0);
                    } else {
                        otherPlayer.characterType = characterType;
                    }
                }
            } else if (message.startsWith("GAME_START")) {
                gameOver = false;
                isHostPlayer = false;
                if (isMultiplayer && gameClient != null) {
                    gameClient.sendMessage("PLAYER_POSITION:" + playerName + ":" + player.x + "," + player.y);
                }
            } else if (message.startsWith("PLAYER_DIED:")) {
                String deadPlayer = message.substring("PLAYER_DIED:".length());
                if (!deadPlayer.equals(playerName)) {
                    playerScores.put(deadPlayer, -1);
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
        if (!isMultiplayer) {
            return true;
        }
        return isHostPlayer;
    }

    private String findWinner() {
        if (score >= 500) {
            return playerName;
        }
        for (Map.Entry<String, Integer> entry : playerScores.entrySet()) {
            if (entry.getValue() != null && entry.getValue() >= 500) {
                return entry.getKey();
            }
        }
        return null;
    }

}

class Player {
    double x, y;
    int size = 60;
    double speed = 1.5;
    Color playerColor = Color.CYAN;
    boolean isMainPlayer = false;
    String characterType = "male";

    static ImageIcon playerIcon;
    static ImageIcon femaleIcon;
    static Image playerImage;
    static Image femaleImage;

    boolean movingUp = false;
    boolean movingDown = false;
    boolean movingLeft = false;
    boolean movingRight = false;

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
            playerImage = null;
            femaleImage = null;
        }
    }

    Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.isMainPlayer = true;
        this.characterType = "male";
    }

    Player(int x, int y, Color color) {
        this.x = x;
        this.y = y;
        this.playerColor = color;
        this.isMainPlayer = true;
        this.characterType = "male";
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
            g.drawImage(imageToDraw, (int)x, (int)y, size, 75, null);
        } else {
            g.drawImage(imageToDraw, (int)x, (int)y, size, 75, null);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            g2.setColor(playerColor);
            g2.fillRect((int)x, (int)y, size, 75);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }

    void startMoveUp() { movingUp = true; }
    void startMoveDown() { movingDown = true; }
    void startMoveLeft() { movingLeft = true; }
    void startMoveRight() { movingRight = true; }

    void stopMoveUp() { movingUp = false; }
    void stopMoveDown() { movingDown = false; }
    void stopMoveLeft() { movingLeft = false; }
    void stopMoveRight() { movingRight = false; }

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

    void moveUp() { startMoveUp(); }
    void moveLeft() { startMoveLeft(); }
    void moveRight() { startMoveRight(); }
    void moveDown() { startMoveDown(); }

    Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, size, 75);
    }
}

class Zombie {
    Random  rand = new Random();
    int x, y;
    int size = 60;
    double speed;
    String id;
    int health = 30;
    String zombieType = "type1";

    static ImageIcon zombieIcon1;
    static ImageIcon zombieIcon2;
    static ImageIcon zombieIcon3;
    static ImageIcon zombieIcon4;
    static Image zombieImage1;
    static Image zombieImage2;
    static Image zombieImage3;
    static Image zombieImage4;

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
        int typeNum = rand.nextInt(4) + 1;
        this.zombieType = "type" + typeNum;
        setZombieProperties();
    }

    Zombie(int x, int y, String id) {
        this.x = x;
        this.y = y;
        this.id = id;
        int typeNum = rand.nextInt(4) + 1;
        this.zombieType = "type" + typeNum;
        setZombieProperties();
    }

    Zombie(int x, int y, String id, double speed) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.id = id;
        int typeNum = rand.nextInt(4) + 1;
        this.zombieType = "type" + typeNum;
    }

    Zombie(int x, int y, String id, double speed, String zombieType) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.id = id;
        if (zombieType != null) {
            this.zombieType = zombieType;
        } else {
            this.zombieType = "type1";
        }
    }

    private void setZombieProperties() {
        switch (zombieType) {
            case "type1":
                this.speed = rand.nextDouble() * 1 + 0.5;
                this.health = 30;
                break;
            case "type2":
                this.speed = rand.nextDouble() * 1.5 + 0.8;
                this.health = 20;
                break;
            case "type3":
                this.speed = rand.nextDouble() * 0.8 + 0.3;
                this.health = 40;
                break;
            case "type4":
                this.speed = rand.nextDouble() * 2 + 1.0;
                this.health = 15;
                break;
            default:
                this.speed = rand.nextDouble() * 1 + 0.5;
                this.health = 30;
                break;
        }
    }

    void draw(Graphics g) {
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
                imageToDraw = zombieImage1;
                break;
        }

        if (imageToDraw != null) {
            g.drawImage(imageToDraw, x, y, size, 75, null);
        } else {
            g.setColor(Color.GREEN);
            g.fillRect(x, y, size, 75);
        }

        g.setColor(Color.RED);
        g.fillRect(x, y - 10, size, 5);

        Color healthColor;
        switch (zombieType) {
            case "type1": healthColor = Color.GREEN; break;
            case "type2": healthColor = Color.YELLOW; break;
            case "type3": healthColor = Color.BLUE; break;
            case "type4": healthColor = Color.ORANGE; break;
            default: healthColor = Color.GREEN; break;
        }
        g.setColor(healthColor);

        double maxHealth = getMaxHealth();
        int hpBar = Math.max(0, (int) ((health / maxHealth) * size));
        g.fillRect(x, y - 10, hpBar, 5);
    }

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

class Bullet {
    int x, y;
    int size = 20;
    int speed = 16;
    int damage = 10;

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
