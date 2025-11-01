package network;

public class PlayerState {
    public int x;
    public int y;
    public int score;
    public boolean isAlive;

    public PlayerState() {
        // state เริ่มต้น
        this.x = 300;
        this.y = 359;
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
        // payload string: x,y,score,isAlive(1/0)
        String aliveText;
        if (isAlive) {
            aliveText = "1";
        } else {
            aliveText = "0";
        }
        return x + "," + y + "," + score + "," + aliveText;
    }

        public static PlayerState fromString(String stateStr) {
        try {
            // เผื่อกรณีโดนส่งมารูปแบบ name:payload → ตัดส่วน name ออก
            int firstColon = stateStr.indexOf(':');
            if (firstColon != -1 && stateStr.substring(firstColon + 1).contains(",")) {
                stateStr = stateStr.substring(firstColon + 1);
            }
            String[] parts = stateStr.split(",");
            if (parts.length >= 4) {
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int score = Integer.parseInt(parts[2].trim());
                String v = parts[3].trim();
                boolean isAlive;
                if (v.equals("1")) {              // รองรับ "1" = เป็นจริง
                    isAlive = true;
                } else if (v.equalsIgnoreCase("true")) { // รองรับ "true"/"TRUE"
                    isAlive = true;
                } else {                          // อย่างอื่นทั้งหมด = เท็จ (รวม "0", "false", ค่าว่าง ฯลฯ)
                    isAlive = false;
                }
                return new PlayerState(x, y, score, isAlive);
            }
        } catch (Exception e) {
            System.err.println("Error parsing player state: '" + stateStr + "' -> " + e);
        }

        return new PlayerState();
    }
}
