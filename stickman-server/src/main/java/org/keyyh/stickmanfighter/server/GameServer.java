package org.keyyh.stickmanfighter.server;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.keyyh.stickmanfighter.common.data.ConnectionResponsePacket;
import org.keyyh.stickmanfighter.common.data.GameStatePacket;
import org.keyyh.stickmanfighter.common.data.InputPacket;
import org.keyyh.stickmanfighter.common.network.KryoManager;
import org.keyyh.stickmanfighter.common.network.requests.FindMatchRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameServer {
    private final int port;
    private DatagramSocket socket;
    private final Kryo kryo;

    private final Map<UUID, GameRoom> runningGames = new ConcurrentHashMap<>();
    private final Map<SocketAddress, GameRoom> playerToRoomMap = new ConcurrentHashMap<>();
    private final Queue<SocketAddress> matchmakingQueue = new ConcurrentLinkedQueue<>();

    public GameServer(int port) {
        this.port = port;
        this.kryo = new Kryo();
        KryoManager.register(this.kryo);
    }

    public void start() throws Exception {
        System.out.println("Matchmaking Server starting on port " + port + "...");
        socket = new DatagramSocket(port);
        startPacketListener();
        startMatchmakerThread();
        System.out.println("Server started. Waiting for players to find a match...");
    }

    private void startPacketListener() {
        Thread listenerThread = new Thread(() -> {
            while (true) {
                try {
                    byte[] receiveBuffer = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    socket.receive(receivePacket);
                    handlePacket(receivePacket);
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
        listenerThread.setDaemon(false);
        listenerThread.start();
    }

    private void handlePacket(DatagramPacket receivePacket) throws Exception {
        SocketAddress clientAddress = receivePacket.getSocketAddress();
        Object request = kryo.readClassAndObject(new Input(new ByteArrayInputStream(receivePacket.getData(), 0, receivePacket.getLength())));

        if (request instanceof FindMatchRequest) {
            if (!matchmakingQueue.contains(clientAddress) && !playerToRoomMap.containsKey(clientAddress)) {
                System.out.println("Player " + clientAddress + " added to matchmaking queue.");
                matchmakingQueue.add(clientAddress);
            }
        } else if (request instanceof InputPacket) {
            GameRoom room = playerToRoomMap.get(clientAddress);
            if (room != null && room.isRunning()) {
                room.processInput(clientAddress, (InputPacket) request);
            }
        }
    }

    private void startMatchmakerThread() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (matchmakingQueue.size() >= 2) {
                    System.out.println("Found a match! Creating a new game room...");
                    SocketAddress player1Address = matchmakingQueue.poll();
                    SocketAddress player2Address = matchmakingQueue.poll();

                    if (player1Address == null || player2Address == null) return;

                    GameRoom newRoom = new GameRoom("Match-" + UUID.randomUUID().toString().substring(0, 4), socket, kryo, this::removeRoom);
                    UUID player1Id = UUID.randomUUID();
                    UUID player2Id = UUID.randomUUID();

                    newRoom.addPlayer(player1Address, player1Id);
                    newRoom.addPlayer(player2Address, player2Id);

                    runningGames.put(newRoom.roomId, newRoom);
                    playerToRoomMap.put(player1Address, newRoom);
                    playerToRoomMap.put(player2Address, newRoom);

                    // Gửi gói tin bắt đầu trận đấu cho cả hai
                    GameStatePacket initialGameState = newRoom.createCurrentGameState();
                    sendToOne(player1Address, new ConnectionResponsePacket(player1Id, initialGameState));
                    sendToOne(player2Address, new ConnectionResponsePacket(player2Id, initialGameState));

                    new Thread(newRoom).start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 3, 3, TimeUnit.SECONDS); // Kiểm tra hàng đợi sau mỗi 3 giây
    }

    public void removeRoom(UUID roomId) {
        GameRoom removedRoom = runningGames.remove(roomId);
        if (removedRoom != null) {
            for(SocketAddress addr : removedRoom.getPlayerAddresses()) {
                playerToRoomMap.remove(addr);
            }
            System.out.println("Room " + roomId + " removed.");
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
            new GameServer(9876).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}