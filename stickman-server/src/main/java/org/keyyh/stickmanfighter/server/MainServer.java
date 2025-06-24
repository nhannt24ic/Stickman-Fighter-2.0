package org.keyyh.stickmanfighter.server; // << Sửa lại package của bạn

import org.keyyh.stickmanfighter.server.dao.UserDAO;
import org.keyyh.stickmanfighter.server.dao.UserDaoImpl;
import org.keyyh.stickmanfighter.server.handler.ClientHandler;
import org.keyyh.stickmanfighter.server.service.UserService;
import org.keyyh.stickmanfighter.server.service.UserServiceImpl;
import org.keyyh.stickmanfighter.server.util.HibernateUtil;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainServer {
    private static final int PORT = 7070; // Cổng server sẽ lắng nghe

    public static void main(String[] args) {
        UserDAO userDao = new UserDaoImpl();
        UserService userService = new UserServiceImpl(userDao);

        Runtime.getRuntime().addShutdownHook(new Thread(HibernateUtil::shutdown));

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println(">>> Server Socket đã khởi động tại cổng " + PORT);
            System.out.println(">>> Đang chờ client kết nối...");

            while (true) {

                Socket clientSocket = serverSocket.accept();
                System.out.println("Client đã kết nối: " + clientSocket.getInetAddress());

                Thread clientThread = new Thread(new ClientHandler(clientSocket, userService));
                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("Không thể khởi động server tại cổng " + PORT);
            e.printStackTrace();
        }
    }
}