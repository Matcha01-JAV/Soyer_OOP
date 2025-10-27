package network;

/**
 * Represents the state of a player in the game
 */
public class PlayerState {
    public int x;
    public int y;
    public int score;
    public boolean isAlive;
    
    public PlayerState() {
        this.x = 0;
        this.y = 0;
        this.score = 0;
        this.isAlive = true;
    }
    
    public PlayerState(int x, int y, int score, boolean isAlive) {
        this.x = x;
        this.y = y;
        this.score = score;
        this.isAlive = isAlive;
    }
    
    @Override
    public String toString() {
        return x + "," + y + "," + score + "," + (isAlive ? "1" : "0");
    }
    
    /**
     * Parse player state from string
     */
    public static PlayerState fromString(String stateStr) {
        try {
            String[] parts = stateStr.split(",");
            if (parts.length >= 4) {
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                int score = Integer.parseInt(parts[2]);
                boolean isAlive = "1".equals(parts[3]);
                return new PlayerState(x, y, score, isAlive);
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing player state: " + e.getMessage());
        }
        return new PlayerState();
    }
}