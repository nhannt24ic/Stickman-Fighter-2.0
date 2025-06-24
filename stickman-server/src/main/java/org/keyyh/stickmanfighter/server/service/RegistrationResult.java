package org.keyyh.stickmanfighter.server.service;

public enum RegistrationResult {
    SUCCESS,
    USERNAME_ALREADY_EXISTS,
    EMAIL_ALREADY_EXISTS,
    INVALID_INPUT // Ví dụ: password quá ngắn
}