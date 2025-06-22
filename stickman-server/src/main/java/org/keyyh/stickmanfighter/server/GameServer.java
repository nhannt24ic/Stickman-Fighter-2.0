package org.keyyh.stickmanfighter.server;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.keyyh.stickmanfighter.common.data.*;
import org.keyyh.stickmanfighter.common.network.KryoManager;
import org.keyyh.stickmanfighter.server.game.StickmanCharacterServer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameServer {
    private final int port = 9876;
    private DatagramSocket socket;
    private final Kryo kryo;
    private final Map<SocketAddress, StickmanCharacterServer> players = new ConcurrentHashMap<>();
    private static final long PLAYER_TIMEOUT_MS = 10000;

    public GameServer() {
        this.kryo = new Kryo();
        KryoManager.register(this.kryo);
    }

    public void start() throws Exception {
        System.out.println("Game Server starting on port " + port + "...");
        socket = new DatagramSocket(port);
        System.out.println("Game Server started successfully. Waiting for Kryo packets...");

        startReaperThread();

        while (true) {
            try {
                byte[] receiveBuffer = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                socket.receive(receivePacket);

                SocketAddress clientAddress = receivePacket.getSocketAddress();

                if (!players.containsKey(clientAddress)) {
                    System.out.println("New player connected: " + clientAddress);
                    UUID newId = UUID.randomUUID();
                    StickmanCharacterServer newPlayer = new StickmanCharacterServer(newId, 100 + players.size() * 200, 450);
                    players.put(clientAddress, newPlayer);

                    List<CharacterState> currentStates = new ArrayList<>();
                    for (StickmanCharacterServer character : players.values()) {
                        CharacterState state = new CharacterState(character.id, character.x, character.y, character.getCurrentPose(), character.isFacingRight);
                        currentStates.add(state);
                    }
                    GameStatePacket initialGameState = new GameStatePacket(currentStates, System.currentTimeMillis());

                    ConnectionResponsePacket responsePacket = new ConnectionResponsePacket(newId, initialGameState);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    Output output = new Output(baos);
                    kryo.writeClassAndObject(output, responsePacket);
                    output.close();
                    byte[] sendBuffer = baos.toByteArray();
                    DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, clientAddress);
                    socket.send(sendPacket);
                    System.out.println("Sent ConnectionResponsePacket to " + clientAddress);

                    continue;
                }

                ByteArrayInputStream bais = new ByteArrayInputStream(receivePacket.getData(), 0, receivePacket.getLength());
                Input input = new Input(bais);
                Object receivedObject = kryo.readClassAndObject(input);
                input.close();

                if (receivedObject instanceof InputPacket) {
                    InputPacket packet = (InputPacket) receivedObject;
                    StickmanCharacterServer character = players.get(clientAddress);

                    if (character != null) {
                        character.update(packet);
                        character.lastUpdateTime = System.currentTimeMillis();
                    }
                }

                // --- LOGIC BROADCAST (Giữ nguyên) ---

                List<CharacterState> currentStates = new ArrayList<>();
                for (StickmanCharacterServer serverCharacter : players.values()) {
                    CharacterState state = new CharacterState();
                    state.id = serverCharacter.id;
                    state.x = serverCharacter.x;
                    state.y = serverCharacter.y;
                    state.isFacingRight = serverCharacter.isFacingRight;
                    state.currentPose = serverCharacter.getCurrentPose();
                    currentStates.add(state);
                }

                GameStatePacket gameStatePacket = new GameStatePacket(currentStates, System.currentTimeMillis());

//                if (!gameStatePacket.players.isEmpty()) {
//                    Pose firstPlayerPose = gameStatePacket.players.get(0).currentPose;
//                    if (firstPlayerPose != null) {
//                        // In ra góc của thân mình để xem có phải là một giá trị hợp lệ không
//                        System.out.printf("[SERVER-BROADCAST] Sending Pose with torso angle: %.2f%n", Math.toDegrees(firstPlayerPose.torso));
//                    } else {
//                        System.out.println("[SERVER-BROADCAST] WARNING: Sending a NULL pose!");
//                    }
//                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Output output = new Output(baos);
                kryo.writeClassAndObject(output, gameStatePacket);
                output.close();
                byte[] sendBuffer = baos.toByteArray();

//                System.out.println("[SERVER-BROADCAST] Packet size: " + sendBuffer.length + " bytes");

                for (SocketAddress clientAddr : players.keySet()) {
                    DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, clientAddr);
                    socket.send(sendPacket);
                }

            } catch (Exception e) {
                System.err.println("Error processing packet: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // Dùng import java.util.concurrent.*
    private void startReaperThread() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        // Lên lịch để chạy tác vụ sau mỗi 5 giây
        scheduler.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            System.out.println("[Reaper] Checking for timed-out players...");
            players.entrySet().removeIf(entry -> {
                boolean timedOut = currentTime - entry.getValue().lastUpdateTime > PLAYER_TIMEOUT_MS;
                if (timedOut) {
                    System.out.println("Player " + entry.getKey() + " timed out. Removing.");
                }
                return timedOut;
            });
        }, 5, 5, TimeUnit.SECONDS);
    }

    public static void main(String[] args) {
        try {
            GameServer server = new GameServer();
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}