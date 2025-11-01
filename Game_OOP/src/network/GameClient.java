package network;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;

/**
 * Client ฝั่งผู้เล่น: เชื่อมต่อ server, ส่ง/รับข้อความ, ยิง callback ให้ UI
 */
public class GameClient {
    private Socket clientSocket;
    private BufferedReader input;
    private PrintWriter output;
    private String playerName;
    // callback เมื่อได้รับข้อความจาก server (GameFrame/GamePanel จะตั้งให้)
    private MessageListener messageListener;
    // เธรดเดียวไว้ฟังข้อความจาก server ตลอดเวลา
    private ExecutorService listenerThread;
    private volatile boolean isConnected;

    public GameClient(String playerName, MessageListener listener) {
        this.playerName = playerName;
        this.messageListener = listener;
        // executor เธรดเดียวพอ เพราะมีงานฟังข้อความงานเดียว
        this.listenerThread = Executors.newSingleThreadExecutor();
    }

    // เชื่อมต่อไปยัง host:port
    public boolean connect(String host, int port) {
        try {
            clientSocket = new Socket(host, port);
            // สร้าง reader/writer แบบบรรทัด
            input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            output = new PrintWriter(clientSocket.getOutputStream(), true);
            isConnected = true;

            // ส่ง REGISTER:<name> ไปแจ้งชื่อกับ server
            output.println("REGISTER:" + playerName);
            output.flush();

            // เริ่มฟังข้อความจาก server แบบ async (ไม่บล็อก UI)
            listenerThread.execute(this::listenForMessages);
            return true;

        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            return false;
        }
    }

    /**
     * ลูปอ่านข้อความจาก server ต่อเนื่อง
     * - ถ้าเจอ null = server ปิด/หลุด → จะหลุดลูปเอง
     * - ทุกข้อความถูกส่งต่อไปที่ messageListener.onMessageReceived(message)
     */
    private void listenForMessages() {
        try {
            String message;
            while (isConnected && (message = input.readLine()) != null) {
                if (messageListener != null) {
                    messageListener.onMessageReceived(message);
                }
            }
        } catch (IOException e) {
            if (isConnected) {
                System.err.println("Error reading from server: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Unexpected error in message listener: " + e.getMessage());
        } finally {
            // ถ้าหลุด/จบลูป ให้ปิด connection ฝั่ง client
            disconnect();
        }
    }

    // ส่งข้อความไปยัง server (เช่น PLAYER_STATE|..., PLAYER_SHOOT:..., ฯลฯ)
    public void sendMessage(String message) {
        if (output != null && isConnected) {
            output.println(message);
            output.flush();
        }
    }

    // ปิดการเชื่อมต่อ + ปิดเธรดฟังข้อความ
    public void disconnect() {
        isConnected = false;
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing client connection: " + e.getMessage());
        }
        // ขอให้หยุด executor (ไม่รับงานใหม่) — ถ้าจะให้หยุดทันทีใช้ shutdownNow()
        listenerThread.shutdown();
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    // interface callback ให้ UI เอาไป implement (เช่นอัปเดต GamePanel)
    public interface MessageListener {
        void onMessageReceived(String message);
    }
}
