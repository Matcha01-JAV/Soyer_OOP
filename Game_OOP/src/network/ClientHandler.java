package network;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private GameServer server;
    private BufferedReader input;
    private PrintWriter output;
    // ชื่อผู้เล่นที่ลงทะเบียนแล้ว (null จนกว่าจะ REGISTER)
    private String playerName;
    private volatile boolean isConnected;

    public ClientHandler(Socket socket, GameServer server) {
        this.clientSocket = socket;
        this.server = server;
        this.isConnected = true; // เริ่มต้นเป็นต่ออยู่
    }

    @Override
    public void run() {
        try {
            // ครอบ input/output ของซ็อกเก็ตด้วย reader/writer แบบ text
            input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            // autoFlush=true เพื่อให้ println ส่งออกทันที
            output = new PrintWriter(clientSocket.getOutputStream(), true);


            // ==== ขั้นตอน handshake: รอคำสั่งแรก REGISTER:<name> ====
            String message = input.readLine(); // บล็อกจนมีข้อความหรือเชื่อมต่อหลุด
            if (message != null && message.startsWith("REGISTER:")) {
                // ตัด prefix แล้วเก็บชื่อ
                playerName = message.substring("REGISTER:".length()).trim();
                System.out.println("Player registered: " + playerName);
                // บอก server ว่ามีผู้เล่นใหม่ พร้อมผูก ClientHandler นี้กับชื่อนั้น
                server.addPlayer(playerName, this);

                // ==== วนรับข้อความจาก client ต่อไปจนกว่าจะหลุด ====
                while (isConnected && server.isRunning()) {
                    message = input.readLine(); // null = อีกฝั่งปิดการเชื่อมต่อ
                    if (message == null) {
                        break;
                    }
                    handleMessage(message); // แยกประเภทข้อความและจัดการ
                }
            }
        } catch (IOException e) {
            // ถ้า isConnected ยัง true แปลว่าหลุดแบบไม่คาดคิด (ไม่ใช่เราปิดเอง)
            if (isConnected) {
                System.err.println("Error handling client " + playerName + ": " + e.getMessage());
            }
        } finally {
            // ทำความสะอาด/ลบออกจาก server เสมอ
            disconnect();
        }
    }

    // แยกประเภท protocol ที่ client ส่งมาแต่ละบรรทัด แล้วลงมือทำ
    private void handleMessage(String message) {
        if (playerName == null) { // กันเผื่อไม่ REGISTER แต่ส่งมา
            return;
        }

        if (message.startsWith("PLAYER_STATE|")) {

            String stateData = message.substring("PLAYER_STATE|".length());
            PlayerState st = PlayerState.fromString(stateData);
            server.updatePlayerState(playerName, st);

        } else if (message.startsWith("START_GAME:")) {
            server.startGame();

        } else if (message.startsWith("PLAYER_POSITION:")) {
            // รูปแบบ: PLAYER_POSITION:<name>:x,y
            String positionData = message.substring("PLAYER_POSITION:".length());
            String[] parts = positionData.split(":");
            if (parts.length >= 2) {
                String[] coords = parts[1].split(",");
                if (coords.length >= 2) {
                    try {
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        // อัปเดตตำแหน่งของผู้เล่นตามชื่อใน parts[0]
                        network.PlayerState state = new network.PlayerState(x, y, 0, true);
                        server.updatePlayerState(parts[0], state);
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing player position: " + e.getMessage());
                    }
                }
            }
            return; // ออกจากเมธอด (ไม่ให้ตกไปเช็ค else-if ด้านล่างต่อ)

        } else if (message.startsWith("PLAYER_SHOOT:")) {

            String shootData = message.substring("PLAYER_SHOOT:".length());
            server.broadcastExcept("PLAYER_SHOOT:" + shootData, playerName);

        } else if (message.startsWith("ZOMBIE_SPAWN:")) {
            // โฮสต์สั่ง spawn ซอมบี้ → กระจายให้ทุกคนยกเว้นคนส่ง
            server.broadcastExcept(message, playerName);

        } else if (message.startsWith("ZOMBIE_KILLED:")) {
            // มีซอมบี้ถูกกำจัด → กระจายให้คนอื่น
            String zombieData = message.substring("ZOMBIE_KILLED:".length());
            server.broadcastExcept("ZOMBIE_KILLED:" + zombieData, playerName);

        } else if (message.startsWith("PLAYER_READY:")) {
            // ผู้เล่นกด ready ใน lobby
            server.broadcastExcept(message, playerName);

        } else if (message.startsWith("PLAYER_DIED:")) {
            // ผู้เล่นตาย → แจ้งทุกคน (ยกเว้นคนส่ง)
            server.broadcastExcept("PLAYER_DIED:" + playerName, playerName);

        } else if (message.startsWith("PLAYER_WIN:")) {
            // ประกาศผู้ชนะ → ส่งให้ทุกคน (รวมทั้งคนส่ง)
            String winner = message.substring("PLAYER_WIN:".length());
            server.broadcast("PLAYER_WIN:" + winner);
        }
    }

    // ส่งข้อความกลับไปยัง client รายนี้
    public void sendMessage(String message) {
        if (output != null && isConnected) {
            output.println(message);
            output.flush();
        }
    }

    // ปิดการเชื่อมต่อ + ลบผู้เล่นออกจาก server
    public void disconnect() {
        isConnected = false;
        try {
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing client connection: " + e.getMessage());
        }

        if (playerName != null) {
            server.removePlayer(playerName); // แจ้ง server ว่า player นี้ออกแล้ว
        }
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isConnected() {
        return isConnected;
    }
}
