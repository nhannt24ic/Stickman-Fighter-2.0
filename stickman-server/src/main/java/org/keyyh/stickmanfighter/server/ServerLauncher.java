package org.keyyh.stickmanfighter.server;

public class ServerLauncher {
    public static void main(String[] args) {
        // Khởi động MainServer (TCP) trong thread riêng
        Thread mainServerThread = new Thread(() -> {
            try {
                MainServer.main(args);
            } catch (Exception e) {
                System.err.println("MainServer gặp lỗi: " + e.getMessage());
                e.printStackTrace();
            }
        }, "MainServer-Thread");
        mainServerThread.start();

        // Khởi động GameServer (UDP) trong thread riêng
        Thread gameServerThread = new Thread(() -> {
            try {
                GameServer.main(args);
            } catch (Exception e) {
                System.err.println("GameServer gặp lỗi: " + e.getMessage());
                e.printStackTrace();
            }
        }, "GameServer-Thread");
        gameServerThread.start();

        System.out.println(">>> Đã khởi động cả MainServer (TCP) và GameServer (UDP)");
    }
}
