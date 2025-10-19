package com.wordblock.game;

import com.wordblock.dao.WordDAO;
import java.util.Locale;

public class WordValidator {
    private final WordDAO dao;

    public WordValidator(WordDAO dao) {
        this.dao = dao;
    }

    public boolean isValid(String word, String pool) {
        try {
            // 1️⃣ Kiểm tra có trong từ điển
            if (!dao.isValidWord(word)) return false;

            // 2️⃣ Kiểm tra chỉ chứa ký tự trong pool
            return containsOnlyFromPool(word.toLowerCase(Locale.ROOT), pool.toLowerCase(Locale.ROOT));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Hàm phụ kiểm tra ký tự có nằm trong pool không
    private boolean containsOnlyFromPool(String word, String pool) {
        for (char c : word.toCharArray()) {
            if (pool.indexOf(c) == -1) {
                // có ký tự không nằm trong pool
                return false;
            }
        }
        return true;
    }
}
