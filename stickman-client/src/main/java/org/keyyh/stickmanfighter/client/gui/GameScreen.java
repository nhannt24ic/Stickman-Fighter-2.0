package org.keyyh.stickmanfighter.client.gui;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.keyyh.stickmanfighter.client.game.models.StickmanCharacter;
import org.keyyh.stickmanfighter.common.data.ConnectionResponsePacket;
import org.keyyh.stickmanfighter.common.data.GameStatePacket;
import org.keyyh.stickmanfighter.common.data.InputPacket;
import org.keyyh.stickmanfighter.common.enums.ActionModifier;
import org.keyyh.stickmanfighter.common.enums.PlayerAction;
import org.keyyh.stickmanfighter.common.network.KryoManager;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameScreen extends JPanel implements ActionListener {
    private final int screenWidth;
    private final int screenHeight;
    private final int DELAY = 16;
    private final Kryo kryo;
    private DatagramSocket clientSocket;
    private InetAddress serverAddress;
    private final int serverPort = 9876;
    private final Map<UUID, StickmanCharacter> characters = new ConcurrentHashMap<>();
    private InputHandler inputHandler;
    private Timer gameLoopTimer;
    private UUID myPlayerId;
    private long clockOffset = 0;

    public GameScreen(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        this.inputHandler = new InputHandler();
        this.kryo = new Kryo();
        KryoManager.register(this.kryo);

        try {
            clientSocket = new DatagramSocket();
            serverAddress = InetAddress.getByName("localhost");
        } catch (Exception e) {
            clientSocket = null;
            serverAddress = null;
            e.printStackTrace();
        }

        initPanel();
        startServerListener();
    }

    private void initPanel() {
        setPreferredSize(new Dimension(screenWidth, screenHeight));
        setBackground(Color.LIGHT_GRAY);
        setFocusable(true);
        addKeyListener(inputHandler);
    }

    public void startGameLoop() {
        gameLoopTimer = new Timer(DELAY, this);
        gameLoopTimer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        sendInputToServer();

        long synchronizedServerTime = System.currentTimeMillis() - clockOffset;
        long renderTime = synchronizedServerTime - 100;
        for (StickmanCharacter character : characters.values()) {
            character.interpolate(renderTime);
        }
        repaint();
    }

    private void startServerListener() {
        Thread listenerThread = new Thread(() -> {
            while (true) {
                try {
                    byte[] receiveBuffer = new byte[4096];
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    clientSocket.receive(receivePacket);
                    ByteArrayInputStream bais = new ByteArrayInputStream(receiveBuffer, 0, receivePacket.getLength());
                    Input input = new Input(bais);
                    Object receivedObject = kryo.readClassAndObject(input);
                    input.close();
                    if (receivedObject instanceof ConnectionResponsePacket) {
                        ConnectionResponsePacket response = (ConnectionResponsePacket) receivedObject;
                        this.myPlayerId = response.yourPlayerId;
                        long clientTime = System.currentTimeMillis();
                        this.clockOffset = clientTime - response.initialGameState.timestamp;
                        updateCharactersFromServer(response.initialGameState);
                    } else if (receivedObject instanceof GameStatePacket) {
                        updateCharactersFromServer((GameStatePacket) receivedObject);
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void updateCharactersFromServer(GameStatePacket packet) {
        if (packet == null || packet.players == null) return;
        SwingUtilities.invokeLater(() -> {
            Set<UUID> receivedIds = new HashSet<>();
            for (var state : packet.players) {
                receivedIds.add(state.id);
                StickmanCharacter character = characters.get(state.id);
                if (character == null) {
                    character = new StickmanCharacter(state.id, state.x, state.y, Color.BLACK, false);
                    characters.put(state.id, character);
                }
                if (isMyPlayer(state.id)) {
                    character.reconcile(state);
                } else {
                    character.addState(state, packet.timestamp);
                }
            }
            characters.keySet().removeIf(id -> !receivedIds.contains(id));
        });
    }

    private void sendInputToServer() {
        if (clientSocket == null) return;
        try {
            InputPacket packetToSend = inputHandler.getCurrentInputPacket();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Output output = new Output(baos);
            kryo.writeClassAndObject(output, packetToSend);
            output.close();
            byte[] sendBuffer = baos.toByteArray();
            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, serverAddress, serverPort);
            clientSocket.send(sendPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isMyPlayer(UUID characterId) {
        return myPlayerId != null && myPlayerId.equals(characterId);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (StickmanCharacter c : characters.values()) {
            c.draw(g2d);

            if (isMyPlayer(c.getId())) {
                Point2D.Double headCenter = c.getHeadCenter();
                double headRadius = c.getHeadRadius();
                boolean facingRight = c.isFacingRight();

                if (headCenter != null) {
                    final int ARROW_CENTER_Y = (int) (headCenter.y - headRadius) - 15;
                    final int ARROW_CENTER_X = (int) headCenter.x;
                    final int BASE_HALF = 7;
                    final int TOP_HALF = 12;
                    final int BOTTOM_HALF = 10;
                    int[] xPoints = new int[3];
                    int[] yPoints = new int[3];
                    if (facingRight) {
                        xPoints[0] = ARROW_CENTER_X + TOP_HALF; yPoints[0] = ARROW_CENTER_Y;
                        xPoints[1] = ARROW_CENTER_X - BOTTOM_HALF ; yPoints[1] = ARROW_CENTER_Y - BASE_HALF;
                        xPoints[2] = ARROW_CENTER_X - BOTTOM_HALF ; yPoints[2] = ARROW_CENTER_Y + BASE_HALF;
                    } else {
                        xPoints[0] = ARROW_CENTER_X - TOP_HALF; yPoints[0] = ARROW_CENTER_Y;
                        xPoints[1] = ARROW_CENTER_X + BOTTOM_HALF ; yPoints[1] = ARROW_CENTER_Y - BASE_HALF;
                        xPoints[2] = ARROW_CENTER_X + BOTTOM_HALF ; yPoints[2] = ARROW_CENTER_Y + BASE_HALF;
                    }
                    g2d.setColor(Color.GREEN);
                    g2d.fillPolygon(xPoints, yPoints, 3);
                }
            }
        }
    }

    public static class InputHandler extends KeyAdapter {
        private final Set<Integer> pressedKeys = Collections.synchronizedSet(new HashSet<>());

        @Override
        public void keyPressed(KeyEvent e) {
            pressedKeys.add(e.getKeyCode());
        }

        @Override
        public void keyReleased(KeyEvent e) {
            pressedKeys.remove(e.getKeyCode());
        }

        public InputPacket getCurrentInputPacket() {
            Set<ActionModifier> modifiers = new HashSet<>();
            synchronized (pressedKeys) {
                for (Integer keyCode : pressedKeys) {
                    switch (keyCode) {
                        case KeyEvent.VK_W: modifiers.add(ActionModifier.W); break;
                        case KeyEvent.VK_S: modifiers.add(ActionModifier.S); break;
                        case KeyEvent.VK_A: modifiers.add(ActionModifier.A); break;
                        case KeyEvent.VK_D: modifiers.add(ActionModifier.D); break;
                        case KeyEvent.VK_CONTROL: modifiers.add(ActionModifier.CTRL); break;
                        case KeyEvent.VK_SHIFT: modifiers.add(ActionModifier.SHIFT); break;
                        case KeyEvent.VK_ENTER: modifiers.add(ActionModifier.ENTER); break;
                        case KeyEvent.VK_L: modifiers.add(ActionModifier.L); break;
                    }
                }
            }

            PlayerAction primaryAction = PlayerAction.IDLE;
            if (pressedKeys.contains(KeyEvent.VK_L)) primaryAction = PlayerAction.DASH;
            else if (pressedKeys.contains(KeyEvent.VK_SHIFT)) primaryAction = PlayerAction.BLOCK;
            else if (pressedKeys.contains(KeyEvent.VK_P)) primaryAction = PlayerAction.PUNCH;
            else if (pressedKeys.contains(KeyEvent.VK_K)) primaryAction = PlayerAction.KICK;
            else if (pressedKeys.contains(KeyEvent.VK_CONTROL)) primaryAction = PlayerAction.CROUCH;
            else if (pressedKeys.contains(KeyEvent.VK_SPACE)) primaryAction = PlayerAction.JUMP;
            else if (pressedKeys.contains(KeyEvent.VK_A) || pressedKeys.contains(KeyEvent.VK_D)) primaryAction = PlayerAction.MOVE;

            return new InputPacket(primaryAction, modifiers);
        }
    }
}