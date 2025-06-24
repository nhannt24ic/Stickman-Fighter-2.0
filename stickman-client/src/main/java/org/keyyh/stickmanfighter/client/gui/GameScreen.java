package org.keyyh.stickmanfighter.client.gui;

import org.keyyh.stickmanfighter.client.game.StickmanCharacter;
import org.keyyh.stickmanfighter.common.data.GameStatePacket;
import org.keyyh.stickmanfighter.common.data.InputPacket;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.keyyh.stickmanfighter.client.network.NetworkClient;
import org.keyyh.stickmanfighter.common.enums.ActionModifier;
import org.keyyh.stickmanfighter.common.enums.PlayerAction;

public class GameScreen extends JPanel implements ActionListener {
    private final int screenWidth;
    private final int screenHeight;
    private final int DELAY = 16;
    private final Map<UUID, StickmanCharacter> characters = new ConcurrentHashMap<>();
    private final InputHandler inputHandler;
    private Timer gameLoopTimer;
    private UUID myPlayerId;
    private long clockOffset = 0;

    public GameScreen(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        this.inputHandler = new InputHandler();
        initPanel();
    }

    // Hàm mới để khởi tạo game với dữ liệu từ server
    public void startGame(GameStatePacket initialState, UUID myId, long clockOffset) {
        this.myPlayerId = myId;
        this.clockOffset = clockOffset;
        updateCharactersFromServer(initialState);
        startGameLoop();
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
        NetworkClient.getInstance().send(inputHandler.getCurrentInputPacket());
        long synchronizedServerTime = System.currentTimeMillis() - clockOffset;
        long renderTime = synchronizedServerTime - 100;
        for (StickmanCharacter character : characters.values()) {
//            if (isMyPlayer(character.getId())) {
//                character.updateLocalPlayer(inputHandler);
//            } else {
                character.interpolate(renderTime);
//            }
        }
        repaint();
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