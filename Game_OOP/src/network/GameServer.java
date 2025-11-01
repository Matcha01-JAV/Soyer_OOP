package network;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * เซิร์ฟเวอร์สำหรับรองรับผู้เล่นหลายคน: รับ connection, กระจายข้อความ, เก็บ state
 */
public class GameServer {
    private ServerSocket serverSocket;                 // socket ฝั่ง server
    private ExecutorService clientThreadPool;          // thread pool รับ/ดูแล client
    private List<ClientHandler> clients;               // รายชื่อ client ที่ต่ออยู่
    private volatile boolean isRunning;                // ธงสถานะ server เปิด/ปิด
    private int port;
    private String hostPlayerName; // เก็บชื่อ host คนแรกที่เข้ามา (ใช้เป็น “เจ้าห้อง”)

    // Game state กลาง
    private List<String> playerNames;                  // รายชื่อผู้เล่น (ซ้ำกับ clients ได้ → ระวัง sync)
    private Map<String, PlayerState> playerStates;     // state ของผู้เล่นแต่ละคน

    public GameServer(int port) {
        this.port = port;
        this.clients = new CopyOnWriteArrayList<>();
        this.playerNames = new ArrayList<>();
        this.playerStates = new ConcurrentHashMap<>();
        // สร้าง pool แบบ cached (สร้างเธรดใหม่เมื่อจำเป็น, ว่างแล้วเก็บ reuse)
        this.clientThreadPool = Executors.newCachedThreadPool();
    }

    // ส่ง state ของผู้เล่นทุกคนให้ client รายหนึ่ง (ใช้ตอนเพิ่งเชื่อมต่อเสร็จ)
    public void sendAllStates(ClientHandler client) {
        for (java.util.Map.Entry<String, PlayerState> e : playerStates.entrySet()) {
            // โปรโตคอล: ส่งแบบ PLAYER_STATE:<name>:<payload>
            client.sendMessage("PLAYER_STATE:" + e.getKey() + ":" + e.getValue().toString());
        }
    }

    // ประกาศผู้ชนะให้ทุกคน
    public void broadcastWinner(String winnerName) {
        broadcast("PLAYER_WIN:" + winnerName);
    }

    // เปิดเซิร์ฟเวอร์ (บล็อกใน accept loop)
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        isRunning = true;
        System.out.println("Game server started on port " + port);

        // วนรับ connection ของลูกค้าใหม่
        while (isRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clients.add(clientHandler);                                // เก็บ handler
                clientThreadPool.execute(clientHandler);                   // ให้ pool จัดเธรดไป run()
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        }
    }

    // ปิดเซิร์ฟเวอร์และ client ทั้งหมด
    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // จะปลดบล็อก accept แล้วหลุด loop
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

        // ปิด client ทุกคน
        for (ClientHandler client : clients) {
            client.disconnect();
        }

        // ปิด thread pool อย่างสุภาพ
        clientThreadPool.shutdown();
        try {
            if (!clientThreadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                clientThreadPool.shutdownNow(); // บังคับหยุดถ้าไม่ยอมจบ
            }
        } catch (InterruptedException e) {
            clientThreadPool.shutdownNow();
        }
    }

    // ส่งข้อความให้ทุก client
    public void broadcast(String message) {
        for (ClientHandler client : clients) {
            if (client != null && client.isConnected()) {
                client.sendMessage(message);
            }
        }
    }

    // ส่งให้ทุกคนยกเว้นผู้เล่นคนหนึ่ง (ใช้กระจาย event ของคนส่ง)
    public void broadcastExcept(String message, String excludePlayer) {
        for (ClientHandler client : clients) {
            if (client.getPlayerName() != null && !client.getPlayerName().equals(excludePlayer)) {
                client.sendMessage(message);
            }
        }
    }

    // มีผู้เล่นใหม่เข้ามา → ลงทะเบียนเข้า list + ใส่ state เริ่มต้น + sync ให้ทุกคน
    public synchronized void addPlayer(String playerName, ClientHandler client) {
        if (playerName != null && client != null) {
            // คนแรกที่เข้ามาถือเป็น host
            if (hostPlayerName == null) {
                hostPlayerName = playerName;
            }

            playerNames.add(playerName);                        // เก็บชื่อใน list (ซ้ำกับคลัง clients)
            playerStates.put(playerName, new PlayerState());    // ใส่ค่า default

            broadcast("PLAYER_JOINED:" + playerName);  // บอกทุกคนว่ามีคนเข้ามา
            sendPlayerList(client);                    // ส่งรายชื่อทั้งหมดให้คนที่เพิ่งเข้ามา
            sendAllStates(client);                     // ส่งสถานะของทุกคนให้คนที่เพิ่งเข้ามา


            PlayerState newState = new PlayerState(300, 359, 0, true);
            playerStates.put(playerName, newState);
            broadcast("PLAYER_STATE:" + playerName + ":" + newState.toString()); // แจ้ง state ของคนใหม่ให้ทุกคน
        }
    }

    // ผู้เล่นออก → เอาออกจาก list + ลบ state + แจ้งทุกคน
    public synchronized void removePlayer(String playerName) {
        if (playerName != null) {
            playerNames.remove(playerName);
            playerStates.remove(playerName);
            clients.removeIf(client -> client != null
                    && client.getPlayerName() != null
                    && client.getPlayerName().equals(playerName));
            broadcast("PLAYER_LEFT:" + playerName);
        }
    }

    // ส่งรายชื่อผู้เล่นทั้งหมดให้ client รายเดียว (ใช้ตอน join ใหม่)
    public void sendPlayerList(ClientHandler client) {
        StringBuilder playerListMsg = new StringBuilder("PLAYER_LIST:");
        for (String playerName : playerNames) {
            playerListMsg.append(playerName).append(",");
        }
        // ลบ comma ท้ายสุด
        if (playerListMsg.length() > "PLAYER_LIST:".length()) {
            playerListMsg.deleteCharAt(playerListMsg.length() - 1);
        }
        client.sendMessage(playerListMsg.toString());
    }

    // อัปเดต state ของผู้เล่น แล้วกระจายให้ทุกคน
    public void updatePlayerState(String playerName, PlayerState state) {
        playerStates.put(playerName, state);
        // ⚠ โปรโตคอลตรงนี้ใช้ "STATE|" (pipe) → อาจไม่สอดคล้องกับที่ client อื่น ๆ ฟังอยู่
        broadcast("STATE|" + playerName + "|" + state.toString());

        // ตัวอย่างกติกาชนะlobby
        if (state.score >= 500) {
            broadcastWinner(playerName);
        }
    }

    // เริ่มเกม (ประกาศให้ทุกคน)
    public void startGame() {
        broadcast("GAME_START");
    }

    // ดึงรายชื่อผู้เล่นจาก clients ที่เชื่อมต่อ (อาจมี null ระหว่าง register)
    public synchronized List<String> getPlayerNames() {
        return clients.stream().map(ClientHandler::getPlayerName).collect(Collectors.toList());
    }

    public boolean isRunning() {
        return isRunning;
    }
}
