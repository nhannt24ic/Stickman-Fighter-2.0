package org.keyyh.stickmanfighter.server.service;

import org.keyyh.stickmanfighter.common.model.User;

public class LoginResponse {
    private final LoginStatus status;
    private final User user;

    public LoginResponse(LoginStatus status, User user) {
        this.status = status;
        this.user = user;
    }

    public LoginStatus getStatus() {
        return status;
    }

    public User getUser() {
        return user;
    }
}