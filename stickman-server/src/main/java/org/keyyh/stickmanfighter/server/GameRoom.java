package org.keyyh.stickmanfighter.server;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import org.keyyh.stickmanfighter.common.data.CharacterState;
import org.keyyh.stickmanfighter.common.data.GameStatePacket;
import org.keyyh.stickmanfighter.common.data.InputPacket;
import org.keyyh.stickmanfighter.common.enums.PlayerAction;
import org.keyyh.stickmanfighter.common.network.responses.RoomInfo;
import org.keyyh.stickmanfighter.server.game.StickmanCharacterServer;

import java.awt.*;
import java.awt.geom.Area;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class GameRoom implements Runnable {
    public final UUID roomId;
    public final String roomName;

    // Quản lý 2 người chơi trong phòng này
    private final Map<SocketAddress, StickmanCharacterServer> players = new ConcurrentHashMap<>();
    private final Map<SocketAddress, UUID> addressToIdMap = new ConcurrentHashMap<>();
    private final Map<UUID, SocketAddress> idToAddressMap = new ConcurrentHashMap<>();
    private final Map<SocketAddress, InputPacket> lastInputs = new ConcurrentHashMap<>();

    private final DatagramSocket socket;
    private final Kryo kryo;
    private final Consumer<UUID> onRoomFinished; // Callback để báo cho GameServer biết khi phòng kết thúc

    private volatile boolean isRunning = false;

    private final int TICKS_PER_SECOND = 60;
    private final int MS_PER_TICK = 1000 / TICKS_PER_SECOND;

    public GameRoom(String roomName, DatagramSocket socket, Kryo kryo, Consumer<UUID> onRoomFinished) {
        this.roomId = UUID.randomUUID();
        this.roomName = roomName;
        this.socket = socket;
        this.kryo = kryo;
        this.onRoomFinished = onRoomFinished;
    }

    public void addPlayer(SocketAddress clientAddress, UUID playerId) {
        if (isFull()) return;

        double startX = players.isEmpty() ? 200 : 1200;
        StickmanCharacterServer newPlayer = new StickmanCharacterServer(playerId, startX, 450);

        players.put(clientAddress, newPlayer);
        addressToIdMap.put(clientAddress, playerId);
        idToAddressMap.put(playerId, clientAddress);

        System.out.printf("Player %s (ID: %s) added to room %s. Player count: %d%n", clientAddress, playerId, roomName, players.size());
    }

    public void processInput(SocketAddress clientAddress, InputPacket input) {
        // Chỉ cần lưu lại input cuối cùng, game loop sẽ xử lý
        lastInputs.put(clientAddress, input);

        StickmanCharacterServer character = players.get(clientAddress);
        if (character != null) {
            character.lastUpdateTime = System.currentTimeMillis();
        }
    }

    @Override
    public void run() {
        this.isRunning = true;
        long nextGameTick = System.currentTimeMillis();

        while (isRunning) {
            try {
                int loops = 0;
                while (System.currentTimeMillis() > nextGameTick && loops < 5) {
                    updateGameLogic(System.currentTimeMillis());
                    checkCollisions();
                    nextGameTick += MS_PER_TICK;
                    loops++;
                }
                broadcastGameState();
                Thread.sleep(16); // Nghỉ giữa các vòng lặp để giảm tải CPU
            } catch (Exception e) {
                e.printStackTrace();
                isRunning = false; // Dừng vòng lặp nếu có lỗi nghiêm trọng
            }
        }
        System.out.println("GameRoom " + roomName + " has stopped.");
        onRoomFinished.accept(this.roomId); // Báo cho GameServer để xóa phòng này
    }

    private void updateGameLogic(long currentTime) {
        for (StickmanCharacterServer character : players.values()) {
            SocketAddress address = idToAddressMap.get(character.id);
            InputPacket lastInput = lastInputs.get(address);

            if (lastInput != null) {
                character.update(lastInput, currentTime);
            } else {
                // Nếu chưa từng có input nào, dùng input mặc định
                character.update(new InputPacket(PlayerAction.IDLE, Collections.emptySet()), currentTime);
            }
        }
    }

    private void checkCollisions() {
        // Logic này giống hệt như trong GameServer cũ
        if (players.size() < 2) return;
        List<StickmanCharacterServer> playerList = new ArrayList<>(players.values());
        StickmanCharacterServer playerA = playerList.get(0);
        StickmanCharacterServer playerB = playerList.get(1);
        checkAttack(playerA, playerB);
        checkAttack(playerB, playerA);
    }

    private void checkAttack(StickmanCharacterServer attacker, StickmanCharacterServer defender) {
        // Logic này giống hệt như trong GameServer cũ
    }

    private void broadcastGameState() throws Exception {
        if (players.isEmpty()) return;

        GameStatePacket gameStatePacket = createCurrentGameState();
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

    public GameStatePacket createCurrentGameState() {
        List<CharacterState> currentStates = new ArrayList<>();
        for (StickmanCharacterServer serverCharacter : players.values()) {
            CharacterState state = new CharacterState(
                    serverCharacter.id, serverCharacter.x, serverCharacter.y,
                    serverCharacter.getCurrentPose(), serverCharacter.isFacingRight,
                    serverCharacter.fsmState, serverCharacter.getActiveHitbox(),
                    serverCharacter.getHealth(), serverCharacter.getStamina()
            );
            currentStates.add(state);
        }
        return new GameStatePacket(currentStates, System.currentTimeMillis());
    }

    public boolean isFull() { return players.size() >= 2; }
    public Set<SocketAddress> getPlayerAddresses() { return players.keySet(); }
    public Map<UUID, SocketAddress> getPlayerIdAddressMap() { return idToAddressMap; }
    public UUID getPlayerIdByAddress(SocketAddress address) { return addressToIdMap.get(address); }
    public RoomInfo getRoomInfo() {
        return new RoomInfo(this.roomId, this.roomName, this.players.size());
    }
    public boolean isRunning() { return isRunning; }
}