package org.keyyh.stickmanfighter.server.service;

import org.keyyh.stickmanfighter.common.model.User;
import org.keyyh.stickmanfighter.server.dao.UserDAO;
import org.mindrot.jbcrypt.BCrypt;

public class UserServiceImpl implements UserService {

    private final UserDAO userDao;

    public UserServiceImpl(UserDAO userDao) {
        this.userDao = userDao;
    }

    @Override
    public RegistrationResult register(String username, String password, String displayName, String email) {

        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return RegistrationResult.INVALID_INPUT;
        }

        if (userDao.findByUsername(username) != null) {
            return RegistrationResult.USERNAME_ALREADY_EXISTS;
        }

        if (userDao.findByEmail(email) != null) {
            return RegistrationResult.EMAIL_ALREADY_EXISTS;
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(hashedPassword);
        newUser.setDisplayName(displayName);
        newUser.setEmail(email);
        newUser.setRankingScore(1000);

        userDao.save(newUser);

        return RegistrationResult.SUCCESS;
    }

    @Override
    public LoginResponse login(String username, String password) {

        User user = userDao.findByUsername(username);
        if (user == null) {
            return new LoginResponse(LoginStatus.USER_NOT_FOUND, null);
        }

        if (BCrypt.checkpw(password, user.getPassword())) {
            return new LoginResponse(LoginStatus.SUCCESS, user);
        } else {
            return new LoginResponse(LoginStatus.INVALID_PASSWORD, null);
        }
    }
}