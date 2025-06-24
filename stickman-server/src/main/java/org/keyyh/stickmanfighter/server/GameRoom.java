package org.keyyh.stickmanfighter.server;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import org.keyyh.stickmanfighter.common.data.CharacterState;
import org.keyyh.stickmanfighter.common.data.GameStatePacket;
import org.keyyh.stickmanfighter.common.data.InputPacket;
import org.keyyh.stickmanfighter.common.enums.PlayerAction;
import org.keyyh.stickmanfighter.common.game.GameConstants;
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

    private final Map<SocketAddress, StickmanCharacterServer> players = new ConcurrentHashMap<>();
    private final Map<SocketAddress, UUID> addressToIdMap = new ConcurrentHashMap<>();
    private final Map<UUID, SocketAddress> idToAddressMap = new ConcurrentHashMap<>();
    private final Map<SocketAddress, InputPacket> lastInputs = new ConcurrentHashMap<>();

    private final DatagramSocket socket;
    private final Kryo kryo;
    private final Consumer<UUID> onRoomFinished;

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

        double startX = players.isEmpty() ? 300 : 1100;
        StickmanCharacterServer newPlayer = new StickmanCharacterServer(playerId, startX, GameConstants.GROUND_LEVEL);

        players.put(clientAddress, newPlayer);
        addressToIdMap.put(clientAddress, playerId);
        idToAddressMap.put(playerId, clientAddress);

        System.out.printf("Player %s (ID: %s) added to room %s. Player count: %d%n", clientAddress, playerId, roomName, players.size());
    }

    public void processInput(SocketAddress clientAddress, InputPacket input) {
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
                    checkCollisions(); // <<< GIỜ ĐÃ CÓ LOGIC
                    nextGameTick += MS_PER_TICK;
                    loops++;
                }
                broadcastGameState();
                Thread.sleep(16);
            } catch (Exception e) {
                e.printStackTrace();
                isRunning = false;
            }
        }
        System.out.println("GameRoom " + roomName + " has stopped.");
        onRoomFinished.accept(this.roomId);
    }

    private void updateGameLogic(long currentTime) {
        for (StickmanCharacterServer character : players.values()) {
            SocketAddress address = idToAddressMap.get(character.id);
            InputPacket lastInput = lastInputs.get(address);

            if (lastInput != null) {
                character.update(lastInput, currentTime);
            } else {
                character.update(new InputPacket(PlayerAction.IDLE, Collections.emptySet()), currentTime);
            }
        }
    }

    private void checkCollisions() {
        if (players.size() < 2) return;
        List<StickmanCharacterServer> playerList = new ArrayList<>(players.values());
        StickmanCharacterServer playerA = playerList.get(0);
        StickmanCharacterServer playerB = playerList.get(1);
        checkAttack(playerA, playerB);
        checkAttack(playerB, playerA);
    }

    private void checkAttack(StickmanCharacterServer attacker, StickmanCharacterServer defender) {
        Shape hitboxShape = attacker.getActiveHitbox();
        if (hitboxShape == null) return;
        if (attacker.hasHitTarget(defender.id)) return;

        double damage = 0;
        switch (attacker.fsmState) {
            case PUNCH_NORMAL: case PUNCH_HOOK: case PUNCH_HEAVY:
                damage = GameConstants.PUNCH_DAMAGE;
                break;
            case KICK_NORMAL: case KICK_AERIAL: case KICK_HIGH: case KICK_LOW:
                damage = GameConstants.KICK_DAMAGE;
                break;
        }

        if (damage > 0) {
            BasicStroke stroke = new BasicStroke(6.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            Shape thickHitbox = stroke.createStrokedShape(hitboxShape);
            Area hitboxArea = new Area(thickHitbox);

            for (Shape hurtbox : defender.getHurtboxes()) {
                Area hurtboxArea = new Area(hurtbox);
                hurtboxArea.intersect(hitboxArea);
                if (!hurtboxArea.isEmpty()) {
                    System.out.printf("HIT! Player %s dealt %.1f damage to Player %s%n", attacker.id, damage, defender.id);
                    defender.takeDamage(damage);
                    attacker.addHitTarget(defender.id);
                    break;
                }
            }
        }
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
                    serverCharacter.getHealth(), serverCharacter.getStamina());
            currentStates.add(state);
        }
        return new GameStatePacket(currentStates, System.currentTimeMillis());
    }

    public boolean isFull() { return players.size() >= 2; }
    public Set<SocketAddress> getPlayerAddresses() { return players.keySet(); }
    public Map<UUID, SocketAddress> getPlayerIdAddressMap() { return idToAddressMap; }
    public UUID getPlayerIdByAddress(SocketAddress address) { return addressToIdMap.get(address); }
    public boolean isRunning() { return isRunning; }
}