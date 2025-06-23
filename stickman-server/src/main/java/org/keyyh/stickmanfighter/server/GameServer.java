package org.keyyh.stickmanfighter.server;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.keyyh.stickmanfighter.common.data.*;
import org.keyyh.stickmanfighter.common.enums.PlayerAction;
import org.keyyh.stickmanfighter.common.network.KryoManager;
import org.keyyh.stickmanfighter.server.game.StickmanCharacterServer;

import java.awt.Rectangle;
import java.awt.Shape; // <<< THÊM MỚI
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameServer {
    private final int port;
    private DatagramSocket socket;
    private final Kryo kryo;
    private final Map<SocketAddress, StickmanCharacterServer> players = new ConcurrentHashMap<>();
    private final Map<SocketAddress, InputPacket> clientInputs = new ConcurrentHashMap<>();
    private static final long PLAYER_TIMEOUT_MS = 10000;

    private final int TICKS_PER_SECOND = 60;
    private final int MS_PER_TICK = 1000 / TICKS_PER_SECOND;

    public GameServer(int port) {
        this.port = port;
        this.kryo = new Kryo();
        KryoManager.register(this.kryo);
    }

    public void start() throws Exception {
        System.out.println("Game Server starting on port " + port + "...");
        socket = new DatagramSocket(port);
        startPacketListener();
        startReaperThread();
        System.out.println("Game Server started successfully. Game loop running...");
        gameLoop();
    }

    private void gameLoop() throws Exception {
        long nextGameTick = System.currentTimeMillis();
        while (true) {
            int loops = 0;
            while (System.currentTimeMillis() > nextGameTick && loops < 5) {
                updateGameLogic();
                checkCollisions();
                nextGameTick += MS_PER_TICK;
                loops++;
            }
            broadcastGameState();
            Thread.sleep(1);
        }
    }

    private void startPacketListener() {
        Thread listenerThread = new Thread(() -> {
            while (true) {
                try {
                    byte[] receiveBuffer = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    socket.receive(receivePacket);
                    handlePacket(receivePacket);
                } catch (Exception e) {
                    System.err.println("Error receiving packet: " + e.getMessage());
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void handlePacket(DatagramPacket receivePacket) {
        try {
            SocketAddress clientAddress = receivePacket.getSocketAddress();

            if (!players.containsKey(clientAddress)) {
                System.out.println("New player connected: " + clientAddress);
                UUID newId = UUID.randomUUID();
                StickmanCharacterServer newPlayer = new StickmanCharacterServer(newId, 100 + players.size() * 200, 450);
                players.put(clientAddress, newPlayer);
                sendConnectionResponse(clientAddress, newId);
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(receivePacket.getData(), 0, receivePacket.getLength());
            Input input = new Input(bais);
            Object receivedObject = kryo.readClassAndObject(input);
            input.close();

            if (receivedObject instanceof InputPacket) {
                clientInputs.put(clientAddress, (InputPacket) receivedObject);
                StickmanCharacterServer character = players.get(clientAddress);
                if (character != null) {
                    character.lastUpdateTime = System.currentTimeMillis();
                }
            }
        } catch(Exception e) {
            System.err.println("Error handling packet: " + e.getMessage());
        }
    }

    private void updateGameLogic() {
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<SocketAddress, StickmanCharacterServer> entry : players.entrySet()) {
            StickmanCharacterServer character = entry.getValue();
            InputPacket lastInput = clientInputs.get(entry.getKey());
            if (lastInput != null) {
                character.update(lastInput, currentTime);
            } else {
                character.update(new InputPacket(PlayerAction.IDLE, Collections.emptySet()), currentTime);
            }
        }
    }

    private void checkCollisions() {
        List<StickmanCharacterServer> playerList = new ArrayList<>(players.values());
        for (int i = 0; i < playerList.size(); i++) {
            for (int j = i + 1; j < playerList.size(); j++) {
                StickmanCharacterServer playerA = playerList.get(i);
                StickmanCharacterServer playerB = playerList.get(j);
                checkAttack(playerA, playerB);
                checkAttack(playerB, playerA);
            }
        }
    }

    // <<< THAY ĐỔI: Sửa lại hoàn toàn để làm việc với Shape
    private void checkAttack(StickmanCharacterServer attacker, StickmanCharacterServer defender) {
        Rectangle hitbox = attacker.getActiveHitbox();
        if (hitbox == null) {
            return;
        }

        // Lấy danh sách các Shape (vùng nhận sát thương) của người phòng thủ
        List<Shape> hurtboxes = defender.getHurtboxes();
        for (Shape hurtbox : hurtboxes) {
            // Kiểm tra nếu hurtbox (Area, Ellipse) GIAO với hitbox (Rectangle)
            // Phương thức intersects của Shape có thể nhận một Rectangle
            if (hurtbox.intersects(hitbox)) {
                System.out.printf("HIT! Player %s attacked Player %s%n", attacker.id, defender.id);
                // TODO: Áp dụng sát thương và trạng thái
                // defender.takeHit(...);
                break; // Thoát khỏi vòng lặp sau khi đã xác nhận trúng đòn
            }
        }
    }

    private void sendConnectionResponse(SocketAddress clientAddress, UUID newId) throws Exception {
        List<CharacterState> currentStates = new ArrayList<>();
        for (StickmanCharacterServer character : players.values()) {
            CharacterState state = new CharacterState(
                    character.id, character.x, character.y,
                    character.getCurrentPose(), character.isFacingRight, character.fsmState,
                    character.getActiveHitbox());
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
    }

    private void broadcastGameState() throws Exception {
        if (players.isEmpty()) return;
        List<CharacterState> currentStates = new ArrayList<>();
        for (StickmanCharacterServer serverCharacter : players.values()) {
            CharacterState state = new CharacterState(
                    serverCharacter.id, serverCharacter.x, serverCharacter.y,
                    serverCharacter.getCurrentPose(), serverCharacter.isFacingRight,
                    serverCharacter.fsmState, serverCharacter.getActiveHitbox());
            currentStates.add(state);
        }
        GameStatePacket gameStatePacket = new GameStatePacket(currentStates, System.currentTimeMillis());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryo.writeClassAndObject(output, gameStatePacket);
        output.close();
        byte[] sendBuffer = baos.toByteArray();
        for (SocketAddress clientAddr : players.keySet()) {
            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, clientAddr);
            socket.send(sendPacket);
        }
    }

    private void startReaperThread() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            players.entrySet().removeIf(entry -> {
                boolean timedOut = currentTime - entry.getValue().lastUpdateTime > PLAYER_TIMEOUT_MS;
                if (timedOut) System.out.println("Player " + entry.getKey() + " timed out. Removing.");
                return timedOut;
            });
        }, 5, 5, TimeUnit.SECONDS);
    }

    public static void main(String[] args) {
        try {
            GameServer server = new GameServer(9876);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}