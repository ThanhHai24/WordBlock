package com.wordblock.dao;
import com.wordblock.model.User;
import java.util.List;

public interface UserDAO {
    boolean register(User user) throws Exception;
    List<User> getLeaderboard(int limit) throws Exception;
    User login(String username, String passwordPlain) throws Exception;
    User findByUsername(String username) throws Exception;
    void setStatus(int userId, String status) throws Exception;
    void addPoints(int userId, int delta) throws Exception;
}
