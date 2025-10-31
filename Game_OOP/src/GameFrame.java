import javax.swing.*;
import network.GameClient;

public class GameFrame extends JFrame {
    private GamePanel gamePanel;

    // เผื่อเรียกแบบไม่ส่งชื่อ
    public GameFrame() {
        this("Player");
    }

    // โหมด Solo/Host
    public GameFrame(String playerName) {
        super("Soyer vs Zombies");
        commonInit();
        gamePanel = new GamePanel(playerName);
        setContentPane(gamePanel);
        finalizeInit();
    }

    // โหมด Multiplayer (มี GameClient)
    public GameFrame(String playerName, GameClient client) {
        super("Soyer vs Zombies - Multiplayer");
        commonInit();
        gamePanel = new GamePanel(playerName, client);
        setContentPane(gamePanel);
        finalizeInit();
    }

    /** ตั้งค่าพื้นฐานที่ใช้ร่วมกัน */
    private void commonInit() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); // ปิดทั้งแอปเมื่อปิดหน้าต่าง
        setResizable(false);
    }

    /** เก็บขั้นตอนท้าย ๆ ให้สั้น */
    private void finalizeInit() {
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /** เผื่ออยากเข้าถึง panel ภายนอก */
    public GamePanel getGamePanel() {
        return gamePanel;
    }
}