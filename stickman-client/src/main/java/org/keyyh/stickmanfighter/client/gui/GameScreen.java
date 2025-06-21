package org.keyyh.stickmanfighter.client.gui;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.keyyh.stickmanfighter.client.game.models.StickmanCharacter;
import org.keyyh.stickmanfighter.common.data.GameStatePacket;
import org.keyyh.stickmanfighter.common.data.InputPacket;
import org.keyyh.stickmanfighter.common.network.KryoManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.*;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;

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
        // Gửi input của người chơi đi
        sendInputToServer();

        // <<< THAY ĐỔI: Thực hiện nội suy cho tất cả nhân vật
        long renderTime = System.currentTimeMillis() - 100; // Render trễ 100ms
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

                    if (receivedObject instanceof GameStatePacket) {
                        updateCharactersFromServer((GameStatePacket) receivedObject);
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void updateCharactersFromServer(GameStatePacket packet) {
        SwingUtilities.invokeLater(() -> {
            Set<UUID> receivedIds = new HashSet<>();
            if (packet.players == null) return;

            for (var state : packet.players) {
                receivedIds.add(state.id);
                StickmanCharacter character = characters.get(state.id);
                if (character == null) {
                    character = new StickmanCharacter(state.id, state.x, state.y, Color.BLUE, false);
                    characters.put(state.id, character);
                }
                // <<< THAY ĐỔI: Gọi hàm addState mới
                character.addState(state, packet.timestamp);
            }
            characters.keySet().removeIf(id -> !receivedIds.contains(id));
        });
    }

    private void sendInputToServer() {
        if (clientSocket == null) return;
        try {
            InputPacket packetToSend = new InputPacket(inputHandler.isMoveLeft(), inputHandler.isMoveRight(), inputHandler.isJumpPressed());
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (StickmanCharacter c : characters.values()) {
            c.draw(g2d);
        }
    }

    // Lớp InputHandler giữ nguyên
    public static class InputHandler extends KeyAdapter {
        private boolean moveLeft = false;
        private boolean moveRight = false;
        private boolean jump = false;

        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();
            if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) moveLeft = true;
            if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) moveRight = true;
            if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP || key == KeyEvent.VK_SPACE) jump = true;
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int key = e.getKeyCode();
            if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) moveLeft = false;
            if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) moveRight = false;
            if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP || key == KeyEvent.VK_SPACE) jump = false;
        }

        public boolean isMoveLeft() { return moveLeft; }
        public boolean isMoveRight() { return moveRight; }
        public boolean isJumpPressed() { return jump; }
    }
}