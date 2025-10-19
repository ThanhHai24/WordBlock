package com.wordblock.client.net;

import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.function.Consumer;

public class NetworkClient {
    private final String host; private final int port;
    private Socket socket; private PrintWriter out; private BufferedReader in;
    private Thread listenThread; private final Gson gson = new Gson();
    private Consumer<String> onMessage;

    public NetworkClient(String host, int port){ this.host=host; this.port=port; }
    public void setOnMessage(Consumer<String> cb){ this.onMessage = cb; }

    public boolean connect(){
        try {
            socket=new Socket(host,port);
            out=new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8"),true);
            in =new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));
            listenThread = new Thread(()->{
                try {
                    String line;
                    while((line=in.readLine())!=null){ if(onMessage!=null) onMessage.accept(line); }
                } catch (Exception ignore) {}
            });
            listenThread.start();
            return true;
        } catch (Exception e){ e.printStackTrace(); return false; }
    }

    public void send(Object obj){ out.println(gson.toJson(obj)); }
    public void send(String type, Map<String,Object> payload){ send(Map.of("type", type, "payload", payload)); }

    public void close(){ try{ socket.close(); } catch(Exception ignore){} }
}
