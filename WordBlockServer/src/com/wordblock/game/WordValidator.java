package com.wordblock.game;
import com.wordblock.dao.WordDAO;
public class WordValidator {
    private final WordDAO dao;
    public WordValidator(WordDAO d){ this.dao=d; }
    public boolean isValid(String w){ try { return dao.isValidWord(w); } catch(Exception e){ e.printStackTrace(); return false; } }
}
