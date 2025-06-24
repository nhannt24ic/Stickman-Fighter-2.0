package org.keyyh.stickmanfighter.server.service;

import org.keyyh.stickmanfighter.common.model.User;
import org.keyyh.stickmanfighter.server.dao.UserDAO;
import org.mindrot.jbcrypt.BCrypt;

public class UserServiceImpl implements UserService {

    private final UserDAO userDao;

    // "Tiêm" (Inject) UserDao vào qua constructor.
    // Điều này giúp tầng Service không phụ thuộc cứng vào một implementation cụ thể của DAO.
    public UserServiceImpl(UserDAO userDao) {
        this.userDao = userDao;
    }

    @Override
    public RegistrationResult register(String username, String password, String displayName, String email) {
        // 1. Kiểm tra đầu vào
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return RegistrationResult.INVALID_INPUT;
        }

        // 2. Gọi DAO để kiểm tra username đã tồn tại chưa
        if (userDao.findByUsername(username) != null) {
            return RegistrationResult.USERNAME_ALREADY_EXISTS;
        }

        // 3. Gọi DAO để kiểm tra email đã tồn tại chưa
        if (userDao.findByEmail(email) != null) {
            return RegistrationResult.EMAIL_ALREADY_EXISTS;
        }

        // 4. Logic nghiệp vụ: Băm mật khẩu
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        // 5. Tạo đối tượng User mới
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(hashedPassword);
        newUser.setDisplayName(displayName);
        newUser.setEmail(email);
        newUser.setRankingScore(1000); // Điểm khởi tạo mặc định

        // 6. Ra lệnh cho DAO lưu user
        userDao.save(newUser);

        return RegistrationResult.SUCCESS;
    }

    @Override
    public LoginResponse login(String username, String password) {
        // 1. Gọi DAO để tìm user
        User user = userDao.findByUsername(username);
        if (user == null) {
            return new LoginResponse(LoginStatus.USER_NOT_FOUND, null);
        }

        // 2. Logic nghiệp vụ: Kiểm tra mật khẩu đã băm
        if (BCrypt.checkpw(password, user.getPassword())) {
            // Mật khẩu khớp
            return new LoginResponse(LoginStatus.SUCCESS, user);
        } else {
            // Mật khẩu không khớp
            return new LoginResponse(LoginStatus.INVALID_PASSWORD, null);
        }
    }
}