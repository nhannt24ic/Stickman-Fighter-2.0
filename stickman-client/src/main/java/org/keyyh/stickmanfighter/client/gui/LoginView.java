package org.keyyh.stickmanfighter.client.gui;

import org.keyyh.stickmanfighter.client.service.NetworkService;
import javax.swing.*;
import java.awt.*;

public class LoginView extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JLabel messageLabel;
    private final NetworkService networkService;

    public LoginView(NetworkService networkService) {
        this.networkService = networkService;

        setTitle("Stickman Fighter - Login");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initializeUI();

        // Thêm listener để ngắt kết nối khi tắt app
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                networkService.disconnect();
            }
        });
    }

    private void initializeUI() {

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Username
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Tên đăng nhập:"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField(20);
        panel.add(usernameField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Mật khẩu:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        panel.add(passwordField, gbc);

        // Các nút
        JPanel buttonPanel = new JPanel();
        loginButton = new JButton("Đăng nhập");
        registerButton = new JButton("Đăng ký");
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        gbc.gridx = 1; gbc.gridy = 2;
        panel.add(buttonPanel, gbc);

        // Nhãn thông báo
        gbc.gridwidth = 2;
        gbc.gridx = 0; gbc.gridy = 3;
        messageLabel = new JLabel(" ", SwingConstants.CENTER);
        panel.add(messageLabel, gbc);
        add(panel);

        addListeners();
    }

    private void addListeners() {
        loginButton.addActionListener(e -> handleLogin());
        passwordField.addActionListener(e -> handleLogin());

        // ** THAY ĐỔI QUAN TRỌNG Ở ĐÂY **
        // Sự kiện khi nhấn nút Đăng ký
        registerButton.addActionListener(e -> {
            // Tạo và hiển thị cửa sổ đăng ký
            RegisterView registerView = new RegisterView(LoginView.this, networkService);
            registerView.setVisible(true);
        });
    }

    // Các phương thức handleLogin, processLoginResponse, showMessage, setUIEnabled giữ nguyên...
    private void handleLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (username.trim().isEmpty() || password.trim().isEmpty()) {
            showMessage("Tên đăng nhập và mật khẩu không được để trống.", Color.RED);
            return;
        }
        setUIEnabled(false);
        showMessage("Đang đăng nhập...", Color.BLACK);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                String request = "LOGIN," + username + "," + password;
                return networkService.sendRequest(request);
            }
            @Override
            protected void done() {
                try {
                    String response = get();
                    processLoginResponse(response);
                } catch (Exception ex) {
                    showMessage("Lỗi: " + ex.getMessage(), Color.RED);
                    ex.printStackTrace();
                } finally {
                    setUIEnabled(true);
                }
            }
        }.execute();
    }

    private void processLoginResponse(String response) {
        if (response.startsWith("LOGIN_SUCCESS")) {
            String[] parts = response.split(",");
            String displayName = parts.length > 1 ? parts[1] : "người dùng";
            showMessage("Đăng nhập thành công! Chào mừng " + displayName, new Color(0, 128, 0));
            // Sau khi đăng nhập thành công, khởi tạo MainClient và đóng LoginView
            SwingUtilities.invokeLater(() -> {
                this.dispose();
                new org.keyyh.stickmanfighter.client.MainClient();
            });
        } else if (response.startsWith("LOGIN_ERROR")) {
            String errorReason = response.substring(response.indexOf(":") + 1);
            showMessage("Lỗi đăng nhập: " + errorReason, Color.RED);
        } else {
            showMessage("Lỗi không xác định: " + response, Color.RED);
        }
    }

    private void showMessage(String message, Color color) {
        messageLabel.setText(message);
        messageLabel.setForeground(color);
    }

    private void setUIEnabled(boolean enabled) {
        loginButton.setEnabled(enabled);
        registerButton.setEnabled(enabled);
        usernameField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
    }
}
