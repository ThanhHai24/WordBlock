package com.wordblock.dao;
import com.wordblock.model.User;
import com.wordblock.util.DBUtil;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAOImpl implements UserDAO {
    @Override
    public boolean register(User user) throws Exception {
        String sql="INSERT INTO users (username,password_hash) VALUES (?,?)";
        try(Connection c=DBUtil.getConnection(); PreparedStatement ps=c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            String hash=BCrypt.hashpw(user.getPasswordHash(), BCrypt.gensalt());
            ps.setString(1,user.getUsername()); ps.setString(2,hash);
            int n=ps.executeUpdate(); if(n>0){ try(ResultSet rs=ps.getGeneratedKeys()){ if(rs.next()) user.setId(rs.getInt(1)); } return true; }
            return false;
        }
    }
    @Override
    public User login(String username, String passwordPlain) throws Exception {
        String sql="SELECT * FROM users WHERE username=?";
        try(Connection c=DBUtil.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){
            ps.setString(1,username);
            try(ResultSet rs=ps.executeQuery()){
                if(rs.next()){
                    String hash=rs.getString("password_hash");
                    if(BCrypt.checkpw(passwordPlain, hash)){
                        User u=new User(); u.setId(rs.getInt("id"));
                        u.setUsername(rs.getString("username"));
                        u.setTotalPoints(rs.getInt("total_points"));
                        u.setStatus(rs.getString("status"));
                        return u;
                    }
                }
            }
        }
        return null;
    }
    @Override
    public User findByUsername(String username) throws Exception {
        String sql="SELECT * FROM users WHERE username=?";
        try(Connection c=DBUtil.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){
            ps.setString(1,username);
            try(ResultSet rs=ps.executeQuery()){
                if(rs.next()){
                    User u=new User(); u.setId(rs.getInt("id"));
                    u.setUsername(rs.getString("username"));
                    u.setPasswordHash(rs.getString("password_hash"));
                    u.setTotalPoints(rs.getInt("total_points"));
                    u.setStatus(rs.getString("status"));
                    return u;
                }
            }
        }
        return null;
    }
    @Override
    public void setStatus(int userId, String status) throws Exception {
        String sql="UPDATE users SET status=? WHERE id=?";
        try(Connection c=DBUtil.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){
            ps.setString(1,status); ps.setInt(2,userId); ps.executeUpdate();
        }
    }
    @Override
    public void addPoints(int userId, int delta) throws Exception {
        String sql="UPDATE users SET total_points=total_points+? WHERE id=?";
        try(Connection c=DBUtil.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,delta); ps.setInt(2,userId); ps.executeUpdate();
        }
    }
    @Override
    public List<User> getLeaderboard(int limit) throws Exception {
        String sql = "SELECT id, username, total_points FROM users ORDER BY total_points DESC LIMIT ?";
        List<User> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User u = new User();
                    u.setId(rs.getInt("id"));
                    u.setUsername(rs.getString("username"));
                    u.setTotalPoints(rs.getInt("total_points"));
                    list.add(u);
                }
            }
        }
        return list;
    }

}
