package com.wordblock.dao;
import com.wordblock.model.User;

public interface UserDAO {
    boolean register(User user) throws Exception;
    User login(String username, String passwordPlain) throws Exception;
    User findByUsername(String username) throws Exception;
    void setStatus(int userId, String status) throws Exception;
    void addPoints(int userId, int delta) throws Exception;
}
