package org.keyyh.stickmanfighter.client;

import org.keyyh.stickmanfighter.client.gui.GameScreen;
import org.keyyh.stickmanfighter.client.gui.LobbyScreen;
import org.keyyh.stickmanfighter.client.gui.LoginView;
import org.keyyh.stickmanfighter.client.network.NetworkClient;
import org.keyyh.stickmanfighter.client.service.NetworkService;
import org.keyyh.stickmanfighter.common.data.ConnectionResponsePacket;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class MainClient implements NetworkClient.PacketListener {
    public static final int WINDOW_WIDTH = 1400;
    public static final int WINDOW_HEIGHT = 600;

    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private GameScreen gameScreen;
    private LobbyScreen lobbyScreen;

    public NetworkService networkService;

    public MainClient() {
        NetworkClient.getInstance().start();
        NetworkClient.getInstance().addListener(this);

        networkService = new NetworkService();

        EventQueue.invokeLater(() -> {
            frame = new JFrame("Stickman Fighter");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
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

            showScreen("LOBBY"); // Bắt đầu ở màn hình Lobby
        });
    }

    public void showScreen(String screenName) {
        cardLayout.show(mainPanel, screenName);

        // Kích hoạt listener cho màn hình mới và vô hiệu hóa listener của màn hình cũ
        if ("GAME".equals(screenName)) {
            lobbyScreen.onBecameHidden(); // Lobby ngừng lắng nghe
            gameScreen.requestFocusInWindow();
        } else { // LOBBY
            gameScreen.stopGame(); // Game ngừng lắng nghe và dọn dẹp
            lobbyScreen.onBecameVisible(); // Lobby bắt đầu lắng nghe
            lobbyScreen.requestFocusInWindow();
        }
    }

    // <<< THAY ĐỔI: Hàm received của MainClient giờ chỉ xử lý gói tin báo bắt đầu game
    @Override
    public void received(Object packet) {
        if (packet instanceof ConnectionResponsePacket) {
            SwingUtilities.invokeLater(() -> {
                ConnectionResponsePacket response = (ConnectionResponsePacket) packet;
                System.out.println("Match found signal received! My ID is: " + response.yourPlayerId);

                // Tính toán clock offset
                long clientTime = System.currentTimeMillis();
                long serverTime = response.initialGameState.timestamp;
                long clockOffset = clientTime - serverTime;

                // Khởi tạo và chuyển sang màn hình chơi game
                gameScreen.startGame(response.initialGameState, response.yourPlayerId, clockOffset);
                showScreen("GAME");
            });
        }
    }

    public static void main(String[] args) {
        // Luôn khởi tạo giao diện Swing trên Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            NetworkService networkService = new NetworkService();
            try {
                // Cố gắng kết nối đến server trước khi hiển thị giao diện
                networkService.connect("localhost", 7070);

                // Nếu kết nối thành công, hiển thị cửa sổ đăng nhập
                LoginView loginView = new LoginView(networkService);
                loginView.setVisible(true);

                // Thêm một shutdown hook để đảm bảo ngắt kết nối khi app tắt
                Runtime.getRuntime().addShutdownHook(new Thread(networkService::disconnect));

            } catch (IOException e) {
                // Nếu kết nối thất bại, hiển thị thông báo lỗi và thoát
                JOptionPane.showMessageDialog(null,
                        "Không thể kết nối đến máy chủ game.\nHãy chắc chắn rằng server đang chạy.",
                        "Lỗi Kết Nối",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1); // Thoát ứng dụng
            }
        });

//        new MainClient();

    }
}