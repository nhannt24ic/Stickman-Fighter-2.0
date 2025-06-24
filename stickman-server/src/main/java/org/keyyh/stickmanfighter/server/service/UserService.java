package org.keyyh.stickmanfighter.server.service;

public interface UserService {
    RegistrationResult register(String username, String password, String displayName, String email);
    LoginResponse login(String username, String password);
}
