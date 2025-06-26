package org.keyyh.stickmanfighter.client.gui;

import org.keyyh.stickmanfighter.client.service.NetworkService;
import javax.swing.*;
import java.awt.*;

public class RegisterView extends JDialog {

    private final JTextField usernameField, displayNameField, emailField;
    private final JPasswordField passwordField, confirmPasswordField;
    private final JButton registerButton;
    private final JLabel messageLabel;

    private final NetworkService networkService;

    public RegisterView(Frame owner, NetworkService networkService) {
        super(owner, "Register New Account", true);
        this.networkService = networkService;

        setSize(450, 350);
        setLocationRelativeTo(owner); 
        setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        addField(formPanel, gbc, 0, "Tên đăng nhập:", usernameField = new JTextField(20));
        addField(formPanel, gbc, 1, "Mật khẩu:", passwordField = new JPasswordField(20));
        addField(formPanel, gbc, 2, "Xác nhận mật khẩu:", confirmPasswordField = new JPasswordField(20));
        addField(formPanel, gbc, 3, "Tên hiển thị:", displayNameField = new JTextField(20));
        addField(formPanel, gbc, 4, "Email:", emailField = new JTextField(20));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        messageLabel = new JLabel(" ", SwingConstants.CENTER);
        registerButton = new JButton("Đăng ký");
        bottomPanel.add(messageLabel, BorderLayout.CENTER);
        bottomPanel.add(registerButton, BorderLayout.SOUTH);

        add(formPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        registerButton.addActionListener(e -> handleRegister());
    }

    private void addField(JPanel panel, GridBagConstraints gbc, int y, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = y;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    private void handleRegister() {

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        String displayName = displayNameField.getText().trim();
        String email = emailField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || displayName.isEmpty() || email.isEmpty()) {
            showMessage("Tất cả các trường không được để trống.", Color.RED);
            return;
        }
        if (!password.equals(confirmPassword)) {
            showMessage("Mật khẩu xác nhận không khớp.", Color.RED);
            return;
        }

        setUIEnabled(false);
        showMessage("Đang xử lý...", Color.BLACK);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                String request = "REGISTER," + username + "," + password + "," + displayName + "," + email;
                return networkService.sendRequest(request);
            }

            @Override
            protected void done() {
                try {
                    String response = get();
                    if (response.equals("REGISTER_SUCCESS")) {
                        JOptionPane.showMessageDialog(RegisterView.this,
                                "Đăng ký thành công! Bây giờ bạn có thể đăng nhập.",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                        dispose(); // Đóng cửa sổ đăng ký
                    } else if (response.startsWith("REGISTER_ERROR")) {
                        String errorReason = response.substring(response.indexOf(":") + 1);
                        showMessage("Lỗi đăng ký: " + errorReason, Color.RED);
                    } else {
                        showMessage("Lỗi không xác định: " + response, Color.RED);
                    }
                } catch (Exception ex) {
                    showMessage("Lỗi: Không thể xử lý phản hồi từ server.", Color.RED);
                    ex.printStackTrace();
                } finally {
                    setUIEnabled(true);
                }
            }
        }.execute();
    }

    private void showMessage(String message, Color color) {
        messageLabel.setText(message);
        messageLabel.setForeground(color);
    }

    private void setUIEnabled(boolean enabled) {
        registerButton.setEnabled(enabled);
        usernameField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        confirmPasswordField.setEnabled(enabled);
        displayNameField.setEnabled(enabled);
        emailField.setEnabled(enabled);
    }
}
