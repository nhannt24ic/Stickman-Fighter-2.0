package org.keyyh.stickmanfighter.server;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.keyyh.stickmanfighter.common.data.*;
import org.keyyh.stickmanfighter.common.network.KryoManager;
import org.keyyh.stickmanfighter.common.network.requests.*;
import org.keyyh.stickmanfighter.common.network.responses.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class GameServer {
    private final int port;
    private DatagramSocket socket;
    private final Kryo kryo;

    // Quản lý các phòng chơi đang hoạt động
    private final Map<UUID, GameRoom> rooms = new ConcurrentHashMap<>();
    // Quản lý xem người chơi (SocketAddress) đang ở trong phòng nào
    private final Map<SocketAddress, GameRoom> playerToRoomMap = new ConcurrentHashMap<>();
    // <<< THÊM MỚI: Lưu lại tất cả các client đã từng kết nối để gửi thông báo chung
    private final Set<SocketAddress> allConnectedClients = ConcurrentHashMap.newKeySet();

    public GameServer(int port) {
        this.port = port;
        this.kryo = new Kryo();
        KryoManager.register(this.kryo);
    }

    public void start() throws Exception {
        System.out.println("Lobby Server starting on port " + port + "...");
        socket = new DatagramSocket(port);
        System.out.println("Lobby Server started. Waiting for clients...");
        listenForPackets();
    }

    private void listenForPackets() {
        Thread listenerThread = new Thread(() -> {
            while (true) {
                try {
                    byte[] receiveBuffer = new byte[2048];
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    socket.receive(receivePacket);
                    handlePacket(receivePacket);
                } catch (Exception e) {
                    System.err.println("Error processing packet: " + e.getMessage());
                }
            }
        });
        listenerThread.setDaemon(false);
        listenerThread.start();
    }

    private void handlePacket(DatagramPacket receivePacket) throws Exception {
        SocketAddress clientAddress = receivePacket.getSocketAddress();
        allConnectedClients.add(clientAddress); // Thêm client vào danh sách chung

        ByteArrayInputStream bais = new ByteArrayInputStream(receivePacket.getData(), 0, receivePacket.getLength());
        Input input = new Input(bais);
        Object request = kryo.readClassAndObject(input);
        input.close();

        // Nếu là gói tin Input trong game, chuyển tiếp nó đến đúng phòng
        if (request instanceof InputPacket) {
            GameRoom room = playerToRoomMap.get(clientAddress);
            if (room != null && room.isRunning()) {
                room.processInput(clientAddress, (InputPacket) request);
            }
            return;
        }

        // Nếu là các yêu cầu liên quan đến Lobby
        if (request instanceof ListRoomsRequest) {
            handleListRoomsRequest(clientAddress);
        } else if (request instanceof CreateRoomRequest) {
            handleCreateRoomRequest(clientAddress);
        } else if (request instanceof JoinRoomRequest) {
            handleJoinRoomRequest((JoinRoomRequest) request, clientAddress);
        }
    }

    private void handleListRoomsRequest(SocketAddress clientAddress) throws Exception {
        List<RoomInfo> roomInfos = new ArrayList<>();
        for (GameRoom room : rooms.values()) {
            if (!room.isFull()) {
                roomInfos.add(room.getRoomInfo());
            }
        }
        sendToOne(clientAddress, new RoomListResponse(roomInfos));
    }

    private void handleCreateRoomRequest(SocketAddress clientAddress) throws Exception {
        if (playerToRoomMap.containsKey(clientAddress)) return;

        String roomName = "Room #" + (rooms.size() + 1);
        GameRoom newRoom = new GameRoom(roomName, socket, kryo, this::removeRoom);
        UUID playerId = addPlayerToRoom(newRoom, clientAddress);
        rooms.put(newRoom.roomId, newRoom);

        System.out.println("Player " + clientAddress + " created and joined " + roomName);
        broadcastRoomListUpdate();
    }

    private void handleJoinRoomRequest(JoinRoomRequest request, SocketAddress clientAddress) throws Exception {
        if (playerToRoomMap.containsKey(clientAddress)) return;

        GameRoom room = rooms.get(request.roomId);
        if (room != null && !room.isFull()) {
            UUID playerId = addPlayerToRoom(room, clientAddress);
            System.out.println("Player " + clientAddress + " joined room " + request.roomId);

            broadcastRoomListUpdate();

            if (room.isFull()) {
                System.out.println("Room " + room.roomId + " is full. Starting game...");

                GameStatePacket initialGameState = room.createCurrentGameState();

                // <<< THAY ĐỔI: Gửi trực tiếp ConnectionResponsePacket cho từng người chơi
                for(Map.Entry<UUID, SocketAddress> entry : room.getPlayerIdAddressMap().entrySet()) {
                    UUID pId = entry.getKey();
                    SocketAddress pAddr = entry.getValue();
                    // Gói tin này báo cho client biết ID của nó và trạng thái game để bắt đầu
                    ConnectionResponsePacket responsePacket = new ConnectionResponsePacket(pId, initialGameState);
                    sendToOne(pAddr, responsePacket);
                }

                new Thread(room).start();
            }
        } else {
            System.out.println("Player " + clientAddress + " failed to join room " + request.roomId);
            // TODO: Gửi gói tin báo lỗi về cho client
        }
    }

    private UUID addPlayerToRoom(GameRoom room, SocketAddress clientAddress) {
        UUID playerId = UUID.randomUUID();
        room.addPlayer(clientAddress, playerId);
        playerToRoomMap.put(clientAddress, room);
        return playerId;
    }

    // <<< THÊM MỚI: Hàm bị thiếu đã được implement
    public void broadcastRoomListUpdate() throws Exception {
        System.out.println("Broadcasting room list update to all clients in lobby.");
        List<RoomInfo> roomInfos = new ArrayList<>();
        for (GameRoom room : rooms.values()) {
            // Chỉ gửi thông tin về các phòng chưa đầy
            if (!room.isFull()) {
                roomInfos.add(room.getRoomInfo());
            }
        }
        RoomListResponse response = new RoomListResponse(roomInfos);

        // Gửi cho tất cả các client đã kết nối nhưng chưa ở trong phòng nào
        for (SocketAddress clientAddr : allConnectedClients) {
            if (!playerToRoomMap.containsKey(clientAddr)) {
                sendToOne(clientAddr, response);
            }
        }
    }

    public void removeRoom(UUID roomId) {
        GameRoom removedRoom = rooms.remove(roomId);
        if (removedRoom != null) {
            for(SocketAddress addr : removedRoom.getPlayerAddresses()) {
                playerToRoomMap.remove(addr);
            }
            System.out.println("Room " + roomId + " removed.");
            try {
                broadcastRoomListUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendToOne(SocketAddress clientAddress, Serializable packetObject) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryo.writeClassAndObject(output, packetObject);
        output.close();
        byte[] buffer = baos.toByteArray();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, clientAddress);
        socket.send(packet);
    }

    public static void main(String[] args) {
        try {
            // port được lấy từ constructor
            GameServer server = new GameServer(9876);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}