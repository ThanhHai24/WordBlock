package com.wordblock.util;
import java.sql.*;

public class DBUtil {
    private static final String URL  = "jdbc:mysql://localhost:3306/wordblock?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String USER = "root"; // đổi nếu có mật khẩu
    private static final String PASS = "";

    static { try { Class.forName("com.mysql.cj.jdbc.Driver"); } catch (Exception e) { e.printStackTrace(); } }
    public static Connection getConnection() throws SQLException { return DriverManager.getConnection(URL, USER, PASS); }
    public static void close(AutoCloseable... xs) { for (var x: xs) if (x!=null) try { x.close(); } catch(Exception ignore){} }
}
