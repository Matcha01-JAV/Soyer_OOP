package network;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private GameServer server;
    private BufferedReader input;
    private PrintWriter output;
    private String playerName;
    private volatile boolean isConnected;
    
    public ClientHandler(Socket socket, GameServer server) {
        this.clientSocket = socket;
        this.server = server;
        this.isConnected = true;
    }
    
    @Override
    public void run() {
        try {
            input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            output = new PrintWriter(clientSocket.getOutputStream(), true);
            
            // Wait for player name registration
            String message = input.readLine();
            if (message != null && message.startsWith("REGISTER:")) {
                playerName = message.substring("REGISTER:".length()).trim();
                System.out.println("Player registered: " + playerName);
                server.addPlayer(playerName, this);
                
                // Handle client messages
                while (isConnected && server.isRunning()) {
                    message = input.readLine();
                    if (message == null) {
                        break;
                    }
                    handleMessage(message);
                }
            }
        } catch (IOException e) {
            if (isConnected) {
                System.err.println("Error handling client " + playerName + ": " + e.getMessage());
            }
        } finally {
            disconnect();
        }
    }
    
    /**
     * Handle incoming messages from client
     */
    private void handleMessage(String message) {
        if (playerName == null) return;
        
        if (message.startsWith("PLAYER_STATE:"))
        {
            String stateData = message.substring("PLAYER_STATE:".length());
            server.broadcastExcept(message, playerName);
        } else if (message.startsWith("START_GAME:"))
        {
            server.startGame();
        } else if (message.startsWith("PLAYER_POSITION:"))
        {
            String positionData = message.substring("PLAYER_POSITION:".length());
            server.broadcastExcept("PLAYER_POSITION:" + positionData, playerName);
        } else if (message.startsWith("PLAYER_SHOOT:"))
        {
            String shootData = message.substring("PLAYER_SHOOT:".length());
            server.broadcastExcept("PLAYER_SHOOT:" + shootData, playerName);
        } else if (message.startsWith("ZOMBIE_SPAWN:"))
        {
            server.broadcastExcept(message, playerName);
        } else if (message.startsWith("ZOMBIE_KILLED:"))
        {
            String zombieData = message.substring("ZOMBIE_KILLED:".length());
            server.broadcastExcept("ZOMBIE_KILLED:" + zombieData, playerName);
        } else if (message.startsWith("PLAYER_READY:"))
        {
            server.broadcastExcept(message, playerName);
        }
    }

    public void sendMessage(String message) {
        if (output != null && isConnected) {
            output.println(message);
            output.flush();
        }
    }

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
            server.removePlayer(playerName);
        }
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isConnected() {
        return isConnected;
    }
}