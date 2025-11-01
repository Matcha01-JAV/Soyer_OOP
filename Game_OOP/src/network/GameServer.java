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
    private ServerSocket serverSocket;
    private ExecutorService clientThreadPool;
    private List<ClientHandler> clients;
    private volatile boolean isRunning;
    private int port;
    private String hostPlayerName;

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
        for (Map.Entry<String, PlayerState> e : playerStates.entrySet()) {
            client.sendMessage("PLAYER_STATE:" + e.getKey() + ":" + e.getValue().toString());
        }
    }


    public void broadcastWinner(String winnerName) {
        broadcast("PLAYER_WIN:" + winnerName);
    }


    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        isRunning = true;
        System.out.println("Game server started on port " + port);
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


    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }


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


    public void broadcast(String message) {
        for (ClientHandler client : clients) {
            if (client != null && client.isConnected()) {
                client.sendMessage(message);
            }
        }
    }


    public void broadcastExcept(String message, String excludePlayer) {
        for (ClientHandler client : clients) {
            if (client.getPlayerName() != null && !client.getPlayerName().equals(excludePlayer)) {
                client.sendMessage(message);
            }
        }
    }


    public synchronized void addPlayer(String playerName, ClientHandler client) {
        if (playerName != null && client != null) {

            if (hostPlayerName == null) {
                hostPlayerName = playerName;
            }

            playerNames.add(playerName);
            playerStates.put(playerName, new PlayerState());

            broadcast("PLAYER_JOINED:" + playerName);
            sendPlayerList(client);
            sendAllStates(client);


            PlayerState newState = new PlayerState(300, 359, 0, true);
            playerStates.put(playerName, newState);
            broadcast("PLAYER_STATE:" + playerName + ":" + newState.toString());

        }
    }


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


    public void sendPlayerList(ClientHandler client) {
        StringBuilder playerListMsg = new StringBuilder("PLAYER_LIST:");
        for (String playerName : playerNames) {
            playerListMsg.append(playerName).append(",");
        }

        if (playerListMsg.length() > "PLAYER_LIST:".length()) {
            playerListMsg.deleteCharAt(playerListMsg.length() - 1);
        }
        client.sendMessage(playerListMsg.toString());
    }


    public void updatePlayerState(String playerName, PlayerState state) {
        playerStates.put(playerName, state);

        broadcast("STATE|" + playerName + "|" + state.toString());


        if (state.score >= 500) {
            broadcastWinner(playerName);
        }
    }


    public void startGame() {
        broadcast("GAME_START");
    }


    public synchronized List<String> getPlayerNames() {
        return clients.stream().map(ClientHandler::getPlayerName).collect(Collectors.toList());
    }

    public boolean isRunning() {
        return isRunning;
    }
}
