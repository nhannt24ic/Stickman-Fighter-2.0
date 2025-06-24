package org.keyyh.stickmanfighter.client.gui;

import org.keyyh.stickmanfighter.client.game.StickmanCharacter;
import org.keyyh.stickmanfighter.common.data.GameStatePacket;
import org.keyyh.stickmanfighter.common.data.InputPacket;
import org.keyyh.stickmanfighter.common.game.Skeleton;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.keyyh.stickmanfighter.client.network.NetworkClient;
import org.keyyh.stickmanfighter.common.enums.ActionModifier;
import org.keyyh.stickmanfighter.common.enums.PlayerAction;

public class GameScreen extends JPanel implements ActionListener, NetworkClient.PacketListener {
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

    public void startGame(GameStatePacket initialState, UUID myId, long clockOffset) {
        System.out.println("GameScreen: Starting game...");
        this.myPlayerId = myId;
        this.clockOffset = clockOffset;
        this.characters.clear();
        updateCharactersFromServer(initialState);

        NetworkClient.getInstance().addListener(this);
        startGameLoop();
    }

    public void stopGame() {
        if (gameLoopTimer != null) {
            gameLoopTimer.stop();
        }
        NetworkClient.getInstance().removeListener(this);
        this.characters.clear();
        System.out.println("GameScreen: Stopped and cleaned up.");
    }

    private void initPanel() {
        setPreferredSize(new Dimension(screenWidth, screenHeight));
        setBackground(Color.DARK_GRAY); // Đổi màu nền cho dễ phân biệt
        setFocusable(true);
        addKeyListener(inputHandler);
    }

    public void startGameLoop() {
        gameLoopTimer = new Timer(DELAY, this);
        gameLoopTimer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Gửi input đi qua NetworkClient
        NetworkClient.getInstance().send(inputHandler.getCurrentInputPacket());

        long synchronizedServerTime = System.currentTimeMillis() - clockOffset;
        long renderTime = synchronizedServerTime - 100;
        for (StickmanCharacter character : characters.values()) {
            character.interpolate(renderTime);
        }
        repaint();
    }

    @Override
    public void received(Object packet) {
        if (packet instanceof GameStatePacket) {
            updateCharactersFromServer((GameStatePacket) packet);
        }
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
                character.addState(state, packet.timestamp);
            }
            characters.keySet().removeIf(id -> !receivedIds.contains(id));
        });
    }

    private boolean isMyPlayer(UUID characterId) {
        return myPlayerId != null && myPlayerId.equals(characterId);
    }

    private void drawPlayerStatusBar(Graphics2D g2d, double health, double stamina) {
        int barWidth = 180;
        int barHeight = 18;
        int x = 30;
        int y = 24;
        int segments = 10;
        int segmentGap = 2;
        int segmentWidth = (barWidth - (segments - 1) * segmentGap) / segments;

        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRoundRect(x, y, barWidth, barHeight, 12, 12);
        int healthSegments = (int) Math.ceil(Math.max(0, Math.min(1, health / 100.0)) * segments);
        for (int i = 0; i < healthSegments; i++) {
            int sx = x + i * (segmentWidth + segmentGap);
            g2d.setColor(new Color(220, 20, 60));
            g2d.fillRoundRect(sx, y, segmentWidth, barHeight, 10, 10);
        }
        g2d.setColor(Color.BLACK);
        g2d.drawRoundRect(x, y, barWidth, barHeight, 12, 12);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(Color.WHITE);
        g2d.drawString("HP", x + 8, y + 14);
        g2d.drawString(String.valueOf((int) health), x + barWidth + 16, y + 14);

        int y2 = y + barHeight + 8;
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRoundRect(x, y2, barWidth, barHeight, 12, 12);
        int staminaSegments = (int) Math.ceil(Math.max(0, Math.min(1, stamina / 100.0)) * segments);
        for (int i = 0; i < staminaSegments; i++) {
            int sx = x + i * (segmentWidth + segmentGap);
            g2d.setColor(new Color(30, 144, 255));
            g2d.fillRoundRect(sx, y2, segmentWidth, barHeight, 10, 10);
        }
        g2d.setColor(Color.BLACK);
        g2d.drawRoundRect(x, y2, barWidth, barHeight, 12, 12);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(Color.WHITE);
        g2d.drawString("ST", x + 8, y2 + 14);
        g2d.drawString(String.valueOf((int) stamina), x + barWidth + 16, y2 + 14);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Gradient background
        GradientPaint gp = new GradientPaint(0, 0, new Color(135, 206, 250), 0, getHeight(), new Color(255, 255, 255));
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Draw play area border
        int borderMargin = 30;
        int borderRadius = 30;
        g2d.setColor(new Color(60, 60, 60, 180));
        g2d.setStroke(new BasicStroke(4f));
        g2d.draw(new RoundRectangle2D.Double(borderMargin, borderMargin, getWidth() - 2 * borderMargin, getHeight() - 2 * borderMargin, borderRadius, borderRadius));

        StickmanCharacter localChar = null;
        for (StickmanCharacter c : characters.values()) {
            boolean isLocal = isMyPlayer(c.getId());
            // Highlight local player
            if (isLocal) {
                g2d.setStroke(new BasicStroke(8f));
                g2d.setColor(new Color(0, 255, 127, 120));
                Point2D.Double head = c.getHeadCenter();
                if (head != null) {
                    double r = c.getHeadRadius() + 20;
                    g2d.drawOval((int) (head.x - r), (int) (head.y - r), (int) (2 * r), (int) (2 * r));
                }
                localChar = c;
            }
            // Draw character
            c.draw(g2d);
        }
        // Draw local player status bar
        if (localChar != null) {
            drawPlayerStatusBar(g2d, localChar.getHealth(), localChar.getStamina());
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