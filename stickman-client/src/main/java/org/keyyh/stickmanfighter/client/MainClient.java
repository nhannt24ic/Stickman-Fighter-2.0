package org.keyyh.stickmanfighter.client;

import org.keyyh.stickmanfighter.client.gui.GameScreen;
import org.keyyh.stickmanfighter.client.gui.LobbyScreen;
import org.keyyh.stickmanfighter.client.network.NetworkClient;
import org.keyyh.stickmanfighter.common.data.ConnectionResponsePacket;

import javax.swing.*;
import java.awt.*;
import java.util.UUID;

public class MainClient implements NetworkClient.PacketListener {
    public static final int WINDOW_WIDTH = 1400;
    public static final int WINDOW_HEIGHT = 600;

    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private GameScreen gameScreen;
    private LobbyScreen lobbyScreen;

    public MainClient() {
        NetworkClient.getInstance().start(); // Khởi động mạng
        NetworkClient.getInstance().addListener(this); // Đăng ký lắng nghe gói tin

        EventQueue.invokeLater(() -> {
            frame = new JFrame("Stickman Fighter");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            cardLayout = new CardLayout();
            mainPanel = new JPanel(cardLayout);

            lobbyScreen = new LobbyScreen(this);
            gameScreen = new GameScreen(WINDOW_WIDTH, WINDOW_HEIGHT);

            mainPanel.add(lobbyScreen, "LOBBY");
            mainPanel.add(gameScreen, "GAME");

            frame.add(mainPanel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            showScreen("LOBBY");
        });
    }

    public void showScreen(String screenName) {
        cardLayout.show(mainPanel, screenName);
        if ("GAME".equals(screenName)) {
            gameScreen.requestFocusInWindow();
        } else {
            lobbyScreen.requestFocusInWindow();
        }
    }

    @Override
    public void received(Object packet) {
        if (packet instanceof ConnectionResponsePacket) {
            ConnectionResponsePacket response = (ConnectionResponsePacket) packet;
            System.out.println("GameStart signal received! My ID: " + response.yourPlayerId);

            // Xóa listener của LobbyScreen để nó không xử lý gói tin game
            lobbyScreen.onBecameHidden();

            // Tính toán clock offset
            long clientTime = System.currentTimeMillis();
            long serverTime = response.initialGameState.timestamp;
            long clockOffset = clientTime - serverTime;

            // Khởi tạo và chuyển sang màn hình chơi game
            gameScreen.startGame(response.initialGameState, response.yourPlayerId, clockOffset);
            showScreen("GAME");
        }
    }

    private UUID myPlayerId;
    private long clockOffset;

    public static void main(String[] args) {
        new MainClient();
    }
}