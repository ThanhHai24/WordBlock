package com.wordblock.dao;
import com.wordblock.util.DBUtil;
import java.sql.*;

public class WordDAOImpl implements WordDAO {
    @Override public boolean isValidWord(String word) throws Exception {
        if(word==null || word.isBlank()) return false;
        String sql="SELECT 1 FROM dictionary WHERE LOWER(word)=LOWER(?)";
        try(Connection c=DBUtil.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){
            ps.setString(1,word.trim()); try(ResultSet rs=ps.executeQuery()){ return rs.next(); }
        }
    }
}
