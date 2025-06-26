package org.keyyh.stickmanfighter.client.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class NetworkService {
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private boolean isConnected = false;

    public void connect(String host, int port) throws IOException {
        try {
            socket = new Socket(host, port);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            isConnected = true;
            System.out.println("Đã kết nối thành công đến server.");
        } catch (UnknownHostException e) {
            System.err.println("Không tìm thấy host: " + host);
            throw e;
        } catch (IOException e) {
            System.err.println("Lỗi I/O khi kết nối đến host: " + host);
            throw e;
        }
    }

    public String sendRequest(String request) {
        if (!isConnected) {
            return "ERROR:NOT_CONNECTED";
        }
        try {
            System.out.println("Gửi tới server: " + request);
            writer.println(request);
            String response = reader.readLine();
            System.out.println("Nhận từ server: " + response);
            return response;
        } catch (IOException e) {
            isConnected = false;
            e.printStackTrace();
            return "ERROR:CONNECTION_LOST";
        }
    }

    public void disconnect() {
        isConnected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Đã ngắt kết nối với server.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
