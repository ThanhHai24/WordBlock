package com.wordblock.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.wordblock.util.DBUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MatchDAOImpl implements MatchDAO {

    @Override
    public boolean saveMatch(int player1Id, int player2Id, int score1, int score2) {
        String sql = "INSERT INTO matches (player1, player2, score1, score2, started_at, ended_at) VALUES (?, ?, ?, ?, NOW(), NOW())";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, player1Id);
            ps.setInt(2, player2Id);
            ps.setInt(3, score1);
            ps.setInt(4, score2);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    @Override
    public List<Map<String, Object>> getMatchesForUser(int userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT m.id, u1.username AS player1_name, u2.username AS player2_name,
                   m.score1, m.score2, m.started_at, m.ended_at
            FROM matches m
            JOIN users u1 ON m.player1 = u1.id
            JOIN users u2 ON m.player2 = u2.id
            WHERE m.player1 = ? OR m.player2 = ?
            ORDER BY m.started_at DESC
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> match = new LinkedHashMap<>();
                match.put("id", rs.getInt("id"));
                match.put("player1", rs.getString("player1_name"));
                match.put("player2", rs.getString("player2_name"));
                match.put("score1", rs.getInt("score1"));
                match.put("score2", rs.getInt("score2"));
                match.put("started_at", rs.getTimestamp("started_at").toString());
                match.put("ended_at", rs.getTimestamp("ended_at") != null ? rs.getTimestamp("ended_at").toString() : null);
                list.add(match);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

}
