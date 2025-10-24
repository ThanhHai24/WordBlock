package com.wordblock.server;

import com.google.gson.*;
import com.wordblock.dao.*;
import com.wordblock.game.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    public static final int PORT = 5000;

    static final Map<String, ClientHandler> online = new ConcurrentHashMap<>(); // username -> handler
    static final Map<String, String> pendingInvites = new ConcurrentHashMap<>(); // target -> inviter
    static final Map<String, GameSession> rooms = new ConcurrentHashMap<>();     // roomId -> session
    static final Map<String, String> userRoom = new ConcurrentHashMap<>();       // username -> roomId

    static final UserDAO userDAO = new UserDAOImpl();
    static final WordDAO wordDAO = new WordDAOImpl();
    static final WordValidator validator = new WordValidator(wordDAO);
    static final Gson gson = new Gson();

    public static void main(String[] args) {
        System.out.println("[SERVER] Listening on " + PORT);
        try(ServerSocket ss = new ServerSocket(PORT)){
            while(true){
                Socket s = ss.accept();
                new ClientHandler(s).start();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    static void broadcastToRoom(String roomId, Object obj){
        String json = gson.toJson(obj);
        rooms.keySet().stream().filter(id -> id.equals(roomId)).findFirst().ifPresent(id -> {
            userRoom.forEach((u,r)-> { if(r.equals(id)){ var h=online.get(u); if(h!=null) h.sendRaw(json); }});
        });
    }

    public static void broadcastOnline() {
    try {
        // Duyệt qua tất cả người đang online
        var users = online.entrySet().stream()
            .map(e -> Map.of(
                "name", e.getKey(),
                "status", (userRoom.containsKey(e.getKey()) ? "Playing" : "Online")
            ))
            .toList();

        // Gói JSON gửi đi
        String json = gson.toJson(Map.of(
            "type", "online_list",
            "payload", Map.of("users", users)
        ));

        // Gửi cho tất cả client đang online
        online.values().forEach(h -> h.sendRaw(json));
    } catch (Exception e) {
        e.printStackTrace();
    }
}

}
