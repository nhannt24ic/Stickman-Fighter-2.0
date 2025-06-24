package org.keyyh.stickmanfighter.client;

import org.keyyh.stickmanfighter.client.gui.GameScreen;

import javax.swing.JFrame;
import java.awt.EventQueue;

public class MainClient {

    public static final int WINDOW_WIDTH = 1400;
    public static final int WINDOW_HEIGHT = 600;

    public static void main(String[] args) {

        EventQueue.invokeLater(() -> {
            JFrame frame = new JFrame("Stickman Fighter");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            GameScreen gameScreen = new GameScreen(WINDOW_WIDTH, WINDOW_HEIGHT);
            frame.add(gameScreen);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            gameScreen.requestFocusInWindow();

            gameScreen.startGameLoop();
        });
    }
}