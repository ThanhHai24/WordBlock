package com.wordblock.server;

import com.google.gson.*;
import com.wordblock.dao.*;
import com.wordblock.game.GameSession;
import com.wordblock.model.User;
import static com.wordblock.server.Server.userDAO;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ClientHandler extends Thread {
    private final Socket socket;
    private PrintWriter out; private BufferedReader in;
    private String username = null;

    private static class Msg { String type; JsonObject payload; }

    public ClientHandler(Socket s){ this.socket=s; }

    @Override public void run(){
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream(),  "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            String line;
            while((line=in.readLine())!=null){
                Msg m = Server.gson.fromJson(line, Msg.class);
                if(m==null || m.type==null) continue;
                switch (m.type) {
                    case "register"     -> onRegister(m.payload);
                    case "login"        -> onLogin(m.payload);
                    case "list_online"  -> onListOnline();
                    case "invite"       -> onInvite(m.payload);
                    case "invite_reply" -> onInviteReply(m.payload);
                    case "word_submit"  -> onWordSubmit(m.payload);
                    case "change_password" -> handleChangePassword(m.payload);
                    case "logout"       -> onLogout();
                    case "leave_game"   -> onLeaveGame();
                    case "leaderboard_request" -> onLeaderboardRequest();
                    case "match_history" -> onMatchHistory();
                    default             -> sendRaw(Server.gson.toJson(Map.of("type","error","payload",Map.of("message","Unknown type"))));
                }
            }
        } catch (Exception ignore) {
        } finally { cleanup(); }
    }

    void sendRaw(String json){ out.println(json); }

    private void onRegister(JsonObject p) {
        try {
            String username = p.get("username").getAsString();
            String password = p.get("password").getAsString();

            boolean ok = Server.userDAO.register(new User(username, password));

            sendRaw(Server.gson.toJson(Map.of(
                "type", "register_result",
                "payload", Map.of("success", ok)
            )));

            // ✅ Ghi log ra console
            System.out.println("[REGISTER] " + username + " → " + (ok ? "Register Success" : "Register Failed"));

        } catch (Exception e) {
            e.printStackTrace();
            sendRaw(Server.gson.toJson(Map.of(
                "type", "register_result",
                "payload", Map.of("success", false)
            )));
        }
    }


    private void onLogin(JsonObject p) {
        try {
            String u = p.get("username").getAsString();
            String pw = p.get("password").getAsString();

            // 🔹 Nếu đã có client dùng tài khoản này
            if (Server.online.containsKey(u)) {
                sendRaw(Server.gson.toJson(Map.of(
                    "type", "login_result",
                    "payload", Map.of(
                        "success", false,
                        "message", "This account already logged in."
                    )
                )));
                return;
            }

            // 🔹 Kiểm tra thông tin đăng nhập
            User user = Server.userDAO.login(u, pw);

            if (user != null) {
                // ✅ Đăng nhập thành công
                username = u;
                Server.online.put(u, this);
                Server.userDAO.setStatus(user.getId(), "ONLINE");

                sendRaw(Server.gson.toJson(Map.of(
                    "type", "login_result",
                    "payload", Map.of(
                        "success", true,
                        "username", u,
                        "message", "Đăng nhập thành công."
                    )
                )));

                // Gửi danh sách người chơi online cập nhật
                Server.broadcastOnline();

                System.out.println("[LOGIN] " + u + " Login Success From: " + socket.getRemoteSocketAddress());
            } else {
                // ❌ Sai thông tin
                sendRaw(Server.gson.toJson(Map.of(
                    "type", "login_result",
                    "payload", Map.of(
                        "success", false,
                        "message", "Invalid username or password."
                    )
                )));
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendRaw(Server.gson.toJson(Map.of(
                "type", "login_result",
                "payload", Map.of(
                    "success", false,
                    "message", "Login ERROR."
                )
            )));
        }
    }


    private void onInvite(JsonObject p){
        String to=p.get("to").getAsString();
        if(username==null || !Server.online.containsKey(to)){ sendRaw(Server.gson.toJson(Map.of("type","invite_result","payload", Map.of("success",false,"message","Target offline")))); return; }
        Server.pendingInvites.put(to, username);
        Server.online.get(to).sendRaw(Server.gson.toJson(Map.of("type","invite_received","payload", Map.of("from",username))));
        sendRaw(Server.gson.toJson(Map.of("type","invite_result","payload", Map.of("success",true))));
    }

    private void onInviteReply(JsonObject p){
        String decision=p.get("decision").getAsString(); String from = Server.pendingInvites.getOrDefault(username,null);
        if(from==null){ sendRaw(Server.gson.toJson(Map.of("type","invite_reply_result","payload", Map.of("success",false)))); return; }
        ClientHandler inviter = Server.online.get(from);
        if(inviter==null){ sendRaw(Server.gson.toJson(Map.of("type","invite_reply_result","payload", Map.of("success",false)))); return; }

        if("accept".equalsIgnoreCase(decision)){
            String roomId = "R"+ThreadLocalRandom.current().nextInt(100000,999999);
            String letters = genLetters(8);
            GameSession session = new GameSession(roomId, from, username, Server.validator, 120_000, letters);
            session.setTickListener(new GameSession.TickListener() {
                @Override public void onTick(String rid, int secLeft, Map<String,Integer> sc) {
                    Server.broadcastToRoom(rid, Map.of("type","timer_tick","payload", Map.of("roomId", rid, "secLeft", secLeft, "scores", sc)));
                }
                @Override public void onEnd(String rid, Map<String,Integer> finalScores) {
                    Server.broadcastToRoom(rid, Map.of("type","game_end","payload", Map.of("roomId", rid, "scores", finalScores)));
                    Server.rooms.remove(rid);
                    // clear user->room
                        try {
                            String winner = null, loser = null;
                            int max = Integer.MIN_VALUE;
                            Server.broadcastOnline();
                            for (var e : finalScores.entrySet()) {
                                if (e.getValue() > max) {
                                    max = e.getValue();
                                    winner = e.getKey();
                                }
                            }
                            for (var e : finalScores.entrySet()) {
                                if (!e.getKey().equals(winner)) loser = e.getKey();
                            }

                            if (winner != null) {
                                UserDAO dao = Server.userDAO;
                                User w = dao.findByUsername(winner);
                                if (w != null) dao.addPoints(w.getId(), 5);
                            }
                            if (loser != null) {
                                UserDAO dao = Server.userDAO;
                                User l = dao.findByUsername(loser);
                                if (l != null) dao.addPoints(l.getId(), -3);
                            }
                            if (winner != null && loser != null) {
                                UserDAO dao = Server.userDAO;
                                User p1 = dao.findByUsername(winner);
                                User p2 = dao.findByUsername(loser);

                                if (p1 != null && p2 != null) {
                                    int s1 = finalScores.getOrDefault(winner, 0);
                                    int s2 = finalScores.getOrDefault(loser, 0);

                                    // gọi DAO để lưu vào bảng matches
                                    Server.matchDAO.saveMatch(p1.getId(), p2.getId(), s1, s2);
                                    System.out.printf("[DB] Lưu trận: %s (%d) vs %s (%d)%n", winner, s1, loser, s2);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } 
                    
                    Server.userRoom.entrySet().removeIf(e -> e.getValue().equals(rid));
                }
            });
            Server.rooms.put(roomId, session);
            Server.userRoom.put(from, roomId);
            Server.userRoom.put(username, roomId);

            inviter.sendRaw(Server.gson.toJson(Map.of("type","match_start","payload", Map.of("roomId",roomId,"opponent",username,"letters",letters,"durationSec",120))));
            sendRaw(Server.gson.toJson(Map.of("type","match_start","payload", Map.of("roomId",roomId,"opponent",from,"letters",letters,"durationSec",120))));
            session.start();
        } else {
            inviter.sendRaw(Server.gson.toJson(Map.of("type","invite_rejected","payload", Map.of("by",username))));
        }
        Server.pendingInvites.remove(username);
        sendRaw(Server.gson.toJson(Map.of("type","invite_reply_result","payload", Map.of("success",true))));
    }

    private void onWordSubmit(JsonObject p){
        String roomId=p.get("roomId").getAsString(); String word=p.get("word").getAsString();
        GameSession session = Server.rooms.get(roomId);
        if(session==null){ sendRaw(Server.gson.toJson(Map.of("type","word_result","payload", Map.of("accepted",false,"reason","Room not found")))); return; }
        boolean ok = session.submitWord(username, word);
        sendRaw(Server.gson.toJson(Map.of("type","word_result","payload", Map.of("accepted",ok))));
        if(ok){
            // broadcast điểm mới
            var sc = session.getScores();
            Server.broadcastToRoom(roomId, Map.of("type","score_update","payload", Map.of("roomId",roomId,"scores", sc)));
        }
    }

    private void onLogout(){ cleanup(); sendRaw(Server.gson.toJson(Map.of("type","logout_result","payload", Map.of("success",true)))); }

    private void cleanup(){
        try {
            if(username!=null){
                Server.online.remove(username);
                try{ UserDAO udao=Server.userDAO; User u=udao.findByUsername(username); if(u!=null) udao.setStatus(u.getId(),"OFFLINE"); } catch(Exception ignore){}
                String rid = Server.userRoom.remove(username);
                if (rid != null) {
                    var s = Server.rooms.get(rid);
                    if (s != null && s.isRunning()) {
                        // 🟢 Gán điểm thua cho người chơi thoát
                        s.setPlayerScore(username, -999);

                        // 🟢 Dừng trận (gửi game_end cho đối thủ)
                        s.stop();
                    }
                }
                Server.broadcastOnline();
            }
        } catch(Exception ignore){}
        try{ socket.close(); } catch(Exception ignore){}
    }

    private static String genLetters(int count) {
        // Toàn bộ bảng chữ cái
        String vowels = "aeiou";
        String consonants = "bcdfghjklmnpqrstvwxyz";

        // Danh sách để chọn ngẫu nhiên
        List<Character> pool = new ArrayList<>();
        for (char c : (vowels + consonants).toCharArray()) pool.add(c);

        // Kiểm tra count hợp lệ
        if (count < 2) count = 2;               // ít nhất 2 ký tự
        if (count > 26) count = 26;             // không vượt quá bảng chữ cái

        ThreadLocalRandom rand = ThreadLocalRandom.current();
        Server.broadcastOnline();

        while (true) {
            // Tạo danh sách tạm sao chép
            List<Character> temp = new ArrayList<>(pool);
            Collections.shuffle(temp, rand);

            // Chọn 'count' chữ cái đầu tiên
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < count; i++) b.append(temp.get(i));

            String result = b.toString();

            // ✅ Kiểm tra có ít nhất 1 nguyên âm & 1 phụ âm
            boolean hasVowel = result.chars().anyMatch(ch -> vowels.indexOf(ch) >= 0);
            boolean hasConsonant = result.chars().anyMatch(ch -> consonants.indexOf(ch) >= 0);

            if (hasVowel && hasConsonant)
                return result; // hợp lệ → trả về luôn

            // Nếu không đạt điều kiện → lặp lại (rất nhanh)
        }
    }
    
    private void onLeaveGame() {
        try {
            String rid = Server.userRoom.remove(username);
            if (rid != null) {
                var session = Server.rooms.get(rid);
                if (session != null && session.isRunning()) {
                    session.setPlayerScore(username, -999);
                    session.stop(); // Dừng trận, gửi game_end cho đối thủ
                }
            }

            // Gửi xác nhận lại cho client đã thoát
            sendRaw(Server.gson.toJson(Map.of(
                "type", "leave_result",
                "payload", Map.of("success", true)
            )));

            // Cập nhật danh sách người chơi online
            Server.broadcastOnline();

            System.out.println("[INFO] Player " + username + " left the game manually (score set to -999).");
        } catch (Exception e) {
            e.printStackTrace();
            sendRaw(Server.gson.toJson(Map.of(
                "type", "leave_result",
                "payload", Map.of("success", false)
            )));
        }
    }
    private void onLeaderboardRequest() {
        try {
            var list = Server.userDAO.getLeaderboard(10); // Top 10
            var data = list.stream().map(u -> Map.of(
                "username", u.getUsername(),
                "points", u.getTotalPoints()
            )).toList();

            sendRaw(Server.gson.toJson(Map.of(
                "type", "leaderboard_result",
                "payload", Map.of("leaderboard", data)
            )));
        } catch (Exception e) {
            e.printStackTrace();
            sendRaw(Server.gson.toJson(Map.of(
                "type", "leaderboard_result",
                "payload", Map.of("leaderboard", List.of())
            )));
        }
    }
    private void onListOnline() {
        try {
            // Duyệt tất cả người đang online và lấy trạng thái
            var users = Server.online.entrySet().stream()
                .map(e -> Map.of(
                    "name", e.getKey(),
                    "status", (Server.userRoom.containsKey(e.getKey()) ? "Playing" : "Online")
                ))
                .toList();

            // Gửi về client
            sendRaw(Server.gson.toJson(Map.of(
                "type", "online_list",
                "payload", Map.of("users", users)
            )));
        } catch (Exception e) {
            e.printStackTrace();
            sendRaw(Server.gson.toJson(Map.of(
                "type", "online_list",
                "payload", Map.of("users", List.of())
            )));
        }
    }
    private void handleChangePassword(JsonObject payload) {
        try {
            String oldPass = payload.get("old_password").getAsString();
            String newPass = payload.get("new_password").getAsString();

            boolean success;
            String message;

            if (username == null) {
                success = false;
                message = "Bạn chưa đăng nhập.";
            } else {
                success = userDAO.changePassword(username, oldPass, newPass);
                message = success ? "Đổi mật khẩu thành công." : "Mật khẩu cũ không đúng.";
            }

            JsonObject resPayload = new JsonObject();
            resPayload.addProperty("success", success);
            resPayload.addProperty("message", message);

            JsonObject response = new JsonObject();
            response.addProperty("type", "change_password_result");
            response.add("payload", resPayload);

            // ✅ Gửi theo kiểu bạn đang dùng
            sendRaw(Server.gson.toJson(response));

        } catch (Exception e) {
            e.printStackTrace();

            JsonObject resPayload = new JsonObject();
            resPayload.addProperty("success", false);
            resPayload.addProperty("message", "Lỗi xử lý yêu cầu đổi mật khẩu.");

            JsonObject response = new JsonObject();
            response.addProperty("type", "change_password_result");
            response.add("payload", resPayload);

            sendRaw(Server.gson.toJson(response));
        }
    }
    private void onMatchHistory() {
        try {
            if (username == null) {
                sendRaw(Server.gson.toJson(Map.of(
                    "type", "match_history_result",
                    "payload", Map.of("success", false, "message", "Bạn chưa đăng nhập")
                )));
                return;
            }

            User u = Server.userDAO.findByUsername(username);
            if (u == null) {
                sendRaw(Server.gson.toJson(Map.of(
                    "type", "match_history_result",
                    "payload", Map.of("success", false, "message", "Không tìm thấy người dùng")
                )));
                return;
            }

            var matches = Server.matchDAO.getMatchesForUser(u.getId());
            sendRaw(Server.gson.toJson(Map.of(
                "type", "match_history_result",
                "payload", Map.of("success", true, "matches", matches)
            )));

        } catch (Exception e) {
            e.printStackTrace();
            sendRaw(Server.gson.toJson(Map.of(
                "type", "match_history_result",
                "payload", Map.of("success", false, "message", "Lỗi truy vấn lịch sử đấu")
            )));
        }
    }

    
}
