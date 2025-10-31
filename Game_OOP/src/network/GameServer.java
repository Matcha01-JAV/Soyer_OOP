package network;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Game server class to handle multiplayer connections
 */
public class GameServer {
    private ServerSocket serverSocket;
    private ExecutorService clientThreadPool;
    private List<ClientHandler> clients;
    private volatile boolean isRunning;
    private int port;
    private String hostPlayerName; // Track the host player name

    // Game state
    private List<String> playerNames;
    private Map<String, PlayerState> playerStates;

    public GameServer(int port) {
        this.port = port;
        this.clients = new CopyOnWriteArrayList<>();
        this.playerNames = new ArrayList<>();
        this.playerStates = new ConcurrentHashMap<>();
        this.clientThreadPool = Executors.newCachedThreadPool();
    }
    public void sendAllStates(ClientHandler client) {
        for (java.util.Map.Entry<String, PlayerState> e : playerStates.entrySet()) {
            client.sendMessage("PLAYER_STATE:" + e.getKey() + ":" + e.getValue().toString());
        }
    }
    /**
     * Start the game server
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        isRunning = true;
        System.out.println("Game server started on port " + port);

        // Accept client connections
        while (isRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clients.add(clientHandler);
                clientThreadPool.execute(clientHandler);
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Stop the game server
     */
    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

        // Close all client connections
        for (ClientHandler client : clients) {
            client.disconnect();
        }

        clientThreadPool.shutdown();
        try {
            if (!clientThreadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                clientThreadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            clientThreadPool.shutdownNow();
        }
    }

    /**
     * Broadcast message to all connected clients
     */
    public void broadcast(String message) {
        for (ClientHandler client : clients) {
            if (client != null && client.isConnected()) {
                client.sendMessage(message);
            }
        }
    }

    /**
     * Broadcast message to all connected clients except the specified one
     */
    public void broadcastExcept(String message, String excludePlayer) {
        for (ClientHandler client : clients) {
            if (client.getPlayerName() != null && !client.getPlayerName().equals(excludePlayer)) {
                client.sendMessage(message);
            }
        }
    }

    /**
     * Send message to a specific client
     */
    public void sendToClient(String playerName, String message) {
        for (ClientHandler client : clients) {
            if (client != null && client.isConnected() && client.getPlayerName() != null && client.getPlayerName().equals(playerName)) {
                client.sendMessage(message);
                break;
            }
        }
    }

    /**
     * Add a player to the game
     */
    public synchronized void addPlayer(String playerName, ClientHandler client) {
        if (playerName.equalsIgnoreCase("CleanName")) return;
        if (playerName != null && client != null) {
            // Set the first player as the host
            if (hostPlayerName == null) {
                hostPlayerName = playerName;
            }
            
            playerNames.add(playerName);
            playerStates.put(playerName, new PlayerState());
            broadcast("PLAYER_JOINED:" + playerName);
            sendPlayerList(client);
            sendAllStates(client);
            
            // Also send the new player's state to all other players
            PlayerState newState = new PlayerState(300, 359, 0, true); // Default starting position
            playerStates.put(playerName, newState);
            broadcast("PLAYER_STATE:" + playerName + ":" + newState.toString());
        }
    }

    /**
     * Remove a player from the game
     */
    public synchronized void removePlayer(String playerName) {
        if (playerName != null) {
            playerNames.remove(playerName);
            playerStates.remove(playerName);
            clients.removeIf(client -> client != null && client.getPlayerName() != null && client.getPlayerName().equals(playerName));
            // Notify all clients about the player leaving
            broadcast("PLAYER_LEFT:" + playerName);
        }
    }

    /**
     * Send current player list to a client
     */
    public void sendPlayerList(ClientHandler client) {
        StringBuilder playerListMsg = new StringBuilder("PLAYER_LIST:");
        for (String playerName : playerNames) {
            playerListMsg.append(playerName).append(",");
        }
        if (playerListMsg.length() > "PLAYER_LIST:".length()) {
            playerListMsg.deleteCharAt(playerListMsg.length() - 1); // Remove last comma
        }
        client.sendMessage(playerListMsg.toString());
    }


    /**
     * Update player state
     */
    public void updatePlayerState(String playerName, PlayerState state) {
        playerStates.put(playerName, state);
        broadcast("STATE|" + playerName + "|" + state.toString());
    }

    /**
     * Start the game for all players
     */
    public void startGame() {
        broadcast("GAME_START");
    }

    /**
     * Get list of connected players
     */
    public synchronized List<String> getPlayerNames() {
        // สมมติคุณมีรายการ client handlers ในตัวแปรชื่อ clients
        // และแต่ละตัวมีเมธอด getPlayerName()
        return clients.stream().map(ClientHandler::getPlayerName).collect(Collectors.toList());
    }

    /**
     * Check if server is running
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Check if a player is the host
     */
    public boolean isHost(String playerName) {
        return hostPlayerName != null && hostPlayerName.equals(playerName);
    }
}
