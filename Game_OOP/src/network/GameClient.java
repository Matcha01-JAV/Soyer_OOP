package network;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;

/**
 * Game client class to connect to the game server
 */
public class GameClient {
    private Socket clientSocket;
    private BufferedReader input;
    private PrintWriter output;
    private String playerName;
    private MessageListener messageListener;
    private ExecutorService listenerThread;
    private volatile boolean isConnected;
    
    public GameClient(String playerName, MessageListener listener) {
        this.playerName = playerName;
        this.messageListener = listener;
        this.listenerThread = Executors.newSingleThreadExecutor();
    }

    public void sendPlayerState(PlayerState st) {
        if (!isConnected || output == null) return;
        output.println("PLAYER_STATE|" + st.toString());
        output.flush();
    }
    /**
     * Connect to the game server
     */
    public boolean connect(String host, int port) {
        try {
            clientSocket = new Socket(host, port);
            input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            output = new PrintWriter(clientSocket.getOutputStream(), true);
            isConnected = true;
            
            // Register player name
            output.println("REGISTER:" + playerName);
            output.flush();
            
            // Start listening for messages
            listenerThread.execute(this::listenForMessages);
            
            return true;
        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Listen for messages from the server
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
            disconnect();
        }
    }

    
    /**
     * Send message to the server
     */
    public void sendMessage(String message) {
        if (output != null && isConnected) {
            output.println(message);
            output.flush();
        }
    }
    
    /**
     * Disconnect from the server
     */
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
        
        listenerThread.shutdown();
    }
    
    /**
     * Check if client is connected
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * Set the message listener
     */
    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }
    
    /**
     * Interface for handling incoming messages
     */
    public interface MessageListener {
        void onMessageReceived(String message);
    }

}