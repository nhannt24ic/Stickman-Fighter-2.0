package org.keyyh.stickmanfighter.client.gui;

import org.keyyh.stickmanfighter.client.MainClient;
import org.keyyh.stickmanfighter.client.network.NetworkClient;
import org.keyyh.stickmanfighter.common.network.requests.FindMatchRequest;

import javax.swing.*;
import java.awt.*;

// <<< THAY ĐỔI: Implement PacketListener để có thể nhận gói tin mạng
public class LobbyScreen extends JPanel implements NetworkClient.PacketListener {

    private final JButton findMatchButton;
    private final JLabel statusLabel;

    public LobbyScreen(MainClient mainClient) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        findMatchButton = new JButton("Tìm Trận (1v1)");
        findMatchButton.setFont(new Font("Arial", Font.BOLD, 24));

        statusLabel = new JLabel("Chào mừng đến với Stickman Fighter!");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 16));

        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10);
        add(findMatchButton, gbc);

        gbc.gridy = 1;
        add(statusLabel, gbc);

        findMatchButton.addActionListener(e -> {
            findMatchButton.setEnabled(false);
            findMatchButton.setText("Đang tìm trận...");
            statusLabel.setText("Đang tìm trận, vui lòng chờ...");
            NetworkClient.getInstance().send(new FindMatchRequest());
        });
    }

    // <<< THÊM MỚI: Được gọi bởi MainClient khi màn hình này hiển thị
    public void onBecameVisible() {
        // Đăng ký làm người nghe gói tin
        NetworkClient.getInstance().addListener(this);
        // Reset lại trạng thái các nút
        reset();
    }

    // <<< THÊM MỚI: Được gọi bởi MainClient khi chuyển sang màn hình khác
    public void onBecameHidden() {
        // Hủy đăng ký để không nhận các gói tin không liên quan (như GameStatePacket)
        NetworkClient.getInstance().removeListener(this);
    }

    // <<< THÊM MỚI: Xử lý các gói tin nhận được dành cho màn hình này
    @Override
    public void received(Object packet) {
        // Hiện tại LobbyScreen không cần nhận gói tin nào đặc biệt
        // Nhưng chúng ta có thể thêm logic ở đây sau, ví dụ: "Tìm thấy đối thủ..."
    }

    public void reset() {
        findMatchButton.setEnabled(true);
        findMatchButton.setText("Tìm Trận (1v1)");
        statusLabel.setText("Chào mừng! Nhấn nút để tìm trận.");
    }
}