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

    static void broadcastOnline(){
        String json = gson.toJson(Map.of("type","online_list","payload", Map.of("users", online.keySet())));
        online.values().forEach(h -> h.sendRaw(json));
    }
}
