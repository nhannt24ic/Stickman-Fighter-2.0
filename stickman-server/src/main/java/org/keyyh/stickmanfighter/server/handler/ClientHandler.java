package org.keyyh.stickmanfighter.server.handler;

import org.keyyh.stickmanfighter.server.service.LoginResponse;
import org.keyyh.stickmanfighter.server.service.LoginStatus;
import org.keyyh.stickmanfighter.server.service.RegistrationResult;
import org.keyyh.stickmanfighter.server.service.UserService;
import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final UserService userService;

    public ClientHandler(Socket socket, UserService userService) {
        this.clientSocket = socket;
        this.userService = userService;
    }

    @Override
    public void run() {
        try (
                InputStreamReader isr = new InputStreamReader(clientSocket.getInputStream());
                BufferedReader reader = new BufferedReader(isr);
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Nhận từ client " + clientSocket.getInetAddress() + ": " + line);
                String response = processRequest(line);
                writer.println(response);
                System.out.println("Gửi tới client: " + response);
            }
        } catch (IOException e) {
            System.err.println("Lỗi I/O với client " + clientSocket.getInetAddress() + ": " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Đã đóng kết nối với client " + clientSocket.getInetAddress());
        }
    }

    private String processRequest(String request) {
        String[] parts = request.split(",", 2); // Tách lệnh và phần còn lại
        String command = parts[0];

        switch (command) {
            case "REGISTER":
                return handleRegister(parts[1]);
            case "LOGIN":
                return handleLogin(parts[1]);
            default:
                return "ERROR:UNKNOWN_COMMAND";
        }
    }

    private String handleRegister(String data) {
        String[] params = data.split(",", 4);
        if (params.length < 4) return "ERROR:INVALID_REGISTER_PARAMS";

        RegistrationResult result = userService.register(params[0], params[1], params[2], params[3]);
        if (result == RegistrationResult.SUCCESS) {
            return "REGISTER_SUCCESS";
        } else {
            return "REGISTER_ERROR:" + result.toString();
        }
    }

    private String handleLogin(String data) {
        String[] params = data.split(",", 2);
        if (params.length < 2) return "ERROR:INVALID_LOGIN_PARAMS";

        LoginResponse response = userService.login(params[0], params[1]);
        if (response.getStatus() == LoginStatus.SUCCESS) {
            return "LOGIN_SUCCESS," + response.getUser().getDisplayName() + "," + response.getUser().getRankingScore();
        } else {
            return "LOGIN_ERROR:" + response.getStatus().toString();
        }
    }
}