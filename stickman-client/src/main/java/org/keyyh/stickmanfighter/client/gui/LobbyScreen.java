package org.keyyh.stickmanfighter.client.gui;

import org.keyyh.stickmanfighter.client.MainClient;
import org.keyyh.stickmanfighter.client.network.NetworkClient;
import org.keyyh.stickmanfighter.common.network.requests.CreateRoomRequest;
import org.keyyh.stickmanfighter.common.network.requests.JoinRoomRequest;
import org.keyyh.stickmanfighter.common.network.requests.ListRoomsRequest;
import org.keyyh.stickmanfighter.common.network.responses.RoomInfo;
import org.keyyh.stickmanfighter.common.network.responses.RoomListResponse;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class LobbyScreen extends JPanel implements NetworkClient.PacketListener {

    private final DefaultListModel<RoomInfo> roomListModel;
    private final JList<RoomInfo> roomList;
    private final JButton createButton, joinButton, refreshButton;
    private final JLabel statusLabel;

    public LobbyScreen(MainClient mainClient) {
        setLayout(new BorderLayout());

        roomListModel = new DefaultListModel<>();
        roomList = new JList<>(roomListModel);
        roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomList.setCellRenderer(new RoomListRenderer());
        add(new JScrollPane(roomList), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout());

        statusLabel = new JLabel("Chào mừng! Lấy danh sách phòng...", SwingConstants.CENTER);

        createButton = new JButton("Tạo phòng");
        joinButton = new JButton("Vào phòng");
        refreshButton = new JButton("Làm mới");

        buttonPanel.add(createButton);
        buttonPanel.add(joinButton);
        buttonPanel.add(refreshButton);

        bottomPanel.add(buttonPanel, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        // <<< SỬA LỖI: Chỉ đăng ký ActionListener một lần duy nhất
        refreshButton.addActionListener(e -> {
            statusLabel.setText("Đang lấy danh sách phòng...");
            NetworkClient.getInstance().send(new ListRoomsRequest());
        });
        createButton.addActionListener(e -> {
            statusLabel.setText("Đang tạo phòng...");
            disableButtons();
            NetworkClient.getInstance().send(new CreateRoomRequest());
        });
        joinButton.addActionListener(e -> {
            RoomInfo selectedRoom = roomList.getSelectedValue();
            if (selectedRoom != null) {
                statusLabel.setText("Đang vào phòng " + selectedRoom.roomName + "...");
                disableButtons();
                NetworkClient.getInstance().send(new JoinRoomRequest(selectedRoom.roomId));
            }
        });
    }

    public void onBecameVisible() {
        // Hàm này sẽ được gọi từ MainClient khi màn hình này được hiển thị
        NetworkClient.getInstance().addListener(this);
        enableButtons();
        statusLabel.setText("Chào mừng! Nhấn 'Làm mới' để xem các phòng.");
        NetworkClient.getInstance().send(new ListRoomsRequest());
    }

    public void onBecameHidden() {
        // Hàm này được gọi khi chuyển sang màn hình khác
        NetworkClient.getInstance().removeListener(this);
    }

    @Override
    public void received(Object packet) {
        if (packet instanceof RoomListResponse) {
            SwingUtilities.invokeLater(() -> {
                List<RoomInfo> rooms = ((RoomListResponse) packet).rooms;
                roomListModel.clear();
                roomListModel.addAll(rooms);
                statusLabel.setText("Đã nhận được " + rooms.size() + " phòng. Hãy chọn một phòng!");
                enableButtons();
            });
        }
        // Thêm các xử lý gói tin báo lỗi tham gia phòng ở đây sau
    }

    private void disableButtons() {
        createButton.setEnabled(false);
        joinButton.setEnabled(false);
        refreshButton.setEnabled(false);
    }

    public void enableButtons() {
        createButton.setEnabled(true);
        joinButton.setEnabled(true);
        refreshButton.setEnabled(true);
    }

    // Lớp nội để vẽ danh sách phòng cho đẹp
    static class RoomListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof RoomInfo) {
                RoomInfo info = (RoomInfo) value;
                setText(String.format("%s (%d/%d)", info.roomName, info.playerCount, info.MAX_PLAYERS));
            }
            return this;
        }
    }
}