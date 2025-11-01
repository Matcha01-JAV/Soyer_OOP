import javax.swing.*;
import network.GameClient;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GameFrame extends JFrame {
    private GamePanel gamePanel;
    private String characterType = "male"; // ค่าดีฟอลต์ของชนิดตัวละคร

    // ctor แบบเดี่ยว: ถ้าไม่ส่งชื่อมา จะใช้ "Player"
    public GameFrame() {
        this("Player");
    }

    // ctor โหมดเล่นเดี่ยว (ไม่มี client)
    public GameFrame(String playerName) {
        super("Soyer vs Zombies");      // ตั้ง title หน้าต่าง
        commonInit();                    // ตั้งค่า window ทั่วไป + listener ปิดหน้าต่าง
        gamePanel = new GamePanel(playerName); // สร้าง GamePanel โหมด solo
        setContentPane(gamePanel);       // ใส่ panel ลงใน frame
        finalizeInit();                  // pack + จัดกึ่งกลาง + แสดงผล
    }

    // ctor โหมดเล่นเดี่ยว + ระบุชนิดตัวละคร
    public GameFrame(String playerName, String characterType) {
        super("Soyer vs Zombies");
        this.characterType = characterType;          // จดชนิดตัวละคร
        commonInit();
        gamePanel = new GamePanel(playerName, characterType); // ส่งชนิดตัวละครให้ GamePanel
        setContentPane(gamePanel);
        finalizeInit();
    }

    // ctor โหมดเล่นหลายคน (มี GameClient)
    public GameFrame(String playerName, GameClient client) {
        super("Soyer vs Zombies - Multiplayer");
        commonInit();
        gamePanel = new GamePanel(playerName, client);  // GamePanel จะรับ/ส่ง network ผ่าน client
        setContentPane(gamePanel);
        finalizeInit();
    }

    // ctor โหมดเล่นหลายคน + ระบุชนิดตัวละคร
    public GameFrame(String playerName, GameClient client, String characterType) {
        super("Soyer vs Zombies - Multiplayer");
        this.characterType = characterType;
        commonInit();
        gamePanel = new GamePanel(playerName, client, characterType);
        setContentPane(gamePanel);
        finalizeInit();
    }

    // ตั้งค่าพื้นฐานของ frame
    private void commonInit() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); // ปิดโปรแกรมเมื่อปิดหน้าต่างนี้
        setResizable(false);                                     // ไม่ให้ย่อ/ขยายขนาด

        // เวลาผู้ใช้กดปิดหน้าต่าง: เรียก cleanup() ของ GamePanel ก่อน
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                if (gamePanel != null) {
                    gamePanel.cleanup(); // ปิดเสียง/หยุดเธรด/แจ้งเซิร์ฟเวอร์/ปิด socket ฯลฯ
                }
            }
        });
    }

    // จบการตั้งค่า UI: คำนวณขนาดจาก preferredSize, จัดกึ่งกลาง, แล้วแสดงผล
    private void finalizeInit() {
        pack();                 // ให้ขนาด frame พอดีกับ content (GamePanel ต้องมี preferredSize)
        setLocationRelativeTo(null); // จัดให้อยู่กลางหน้าจอ
        setVisible(true);       // แสดงหน้าต่าง
    }

    // ให้ภายนอกเข้าถึง GamePanel ได้ (เช่น ตั้ง host/ส่งต่อ network message)
    public GamePanel getGamePanel() {
        return gamePanel;
    }
}
