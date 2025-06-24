package org.keyyh.stickmanfighter.server.dao;

import org.keyyh.stickmanfighter.common.model.User;
import java.util.List;

public interface UserDAO {
    void save(User user);
    void update(User user);
    void delete(int id);
    User findById(int id);
    User findByUsername(String username);
    List<User> findAll();
    User findByEmail(String email);
}

