package com.wordblock.dao;

import java.util.List;
import java.util.Map;

public interface MatchDAO {
    boolean saveMatch(int player1Id, int player2Id, int score1, int score2);
    List<Map<String, Object>> getMatchesForUser(int userId);
}
