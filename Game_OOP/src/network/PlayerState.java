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
        this.x = 300;  // Default starting x position
        this.y = 359;  // Default starting y position (middle of road)
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
            // ตัด prefix ที่อาจติดมารูปแบบ name:payload ออก (กันเคส "dwa:0,100,5,1")
            int firstColon = stateStr.indexOf(':');
            if (firstColon != -1 && stateStr.substring(firstColon + 1).contains(",")) {
                stateStr = stateStr.substring(firstColon + 1);
            }

            String[] parts = stateStr.split(",");
            if (parts.length >= 4) {
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int score = Integer.parseInt(parts[2].trim());
                boolean isAlive = "1".equals(parts[3].trim()) || Boolean.parseBoolean(parts[3].trim());
                return new PlayerState(x, y, score, isAlive);
            }
        } catch (Exception e) {
            System.err.println("Error parsing player state: '" + stateStr + "' -> " + e);
        }
        return new PlayerState(); // fallback ปลอดภัย
    }
}