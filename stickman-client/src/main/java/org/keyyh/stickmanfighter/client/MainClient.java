package org.keyyh.stickmanfighter.client;

import org.keyyh.stickmanfighter.client.gui.GameScreen;

import javax.swing.JFrame;
import java.awt.EventQueue;

public class MainClient {

    public static final int WINDOW_WIDTH = 1400;
    public static final int WINDOW_HEIGHT = 600;

    public static void main(String[] args) {
        // Chạy GUI trên Event Dispatch Thread (EDT) của Swing
        EventQueue.invokeLater(() -> {
            JFrame frame = new JFrame("Stickman Fighter");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            // Tạo GameScreen (nơi vẽ và xử lý logic game chính)
            GameScreen gameScreen = new GameScreen(WINDOW_WIDTH, WINDOW_HEIGHT);
            frame.add(gameScreen); // Thêm panel game vào frame

            frame.pack(); // Điều chỉnh kích thước frame cho vừa với preferredSize của GameScreen
            frame.setLocationRelativeTo(null); // Hiển thị cửa sổ ở giữa màn hình
            frame.setVisible(true);

            // GameScreen sẽ tự quản lý việc focus để nhận input
            gameScreen.requestFocusInWindow();
            // Bắt đầu game loop trong GameScreen
            gameScreen.startGameLoop();
        });
    }
}