package com.wordblock.client.ui;

import com.google.gson.*;
import com.wordblock.client.net.NetworkClient;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class LobbyFrame extends JFrame {
    private final NetworkClient net;
    private final String username;
    private final DefaultListModel<String> onlineModel = new DefaultListModel<>();
    private final JList<String> lstOnline = new JList<>(onlineModel);
    private final Gson gson = new Gson();

    public LobbyFrame(NetworkClient net, String username){
        super("WordBlock – Lobby ("+username+")");
        this.net=net; this.username=username;

        var left = new JPanel(new BorderLayout());
        left.add(new JLabel("Đang online:"), BorderLayout.NORTH);
        left.add(new JScrollPane(lstOnline), BorderLayout.CENTER);

        var btnRefresh = new JButton("Refresh");
        var btnInvite = new JButton("Invite");
        btnRefresh.addActionListener(e -> net.send("list_online", Map.of()));
        btnInvite.addActionListener(e -> {
            String target = lstOnline.getSelectedValue();
            if(target==null || target.equals(username)){ JOptionPane.showMessageDialog(this,"Chọn user khác để mời"); return; }
            net.send("invite", Map.of("to", target));
        });

        var south = new JPanel(); south.add(btnRefresh); south.add(btnInvite);

        setLayout(new BorderLayout()); add(left, BorderLayout.CENTER); add(south, BorderLayout.SOUTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(360, 420); setLocationRelativeTo(null); setVisible(true);

        net.setOnMessage(this::onServer);
        net.send("list_online", Map.of());
    }

    private void onServer(String line){
        SwingUtilities.invokeLater(()->{
            JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
            String type = obj.get("type").getAsString();
            JsonObject payload = obj.getAsJsonObject("payload");

            switch (type) {
                case "online_list" -> {
                    onlineModel.clear();
                    var arr = payload.getAsJsonArray("users");
                    for (var el : arr) onlineModel.addElement(el.getAsString());
                }
                case "invite_received" -> {
                    String from = payload.get("from").getAsString();
                    int opt = JOptionPane.showConfirmDialog(this, "Nhận lời mời từ "+from+"?", "Invite", JOptionPane.YES_NO_OPTION);
                    net.send("invite_reply", Map.of("decision", opt==JOptionPane.YES_OPTION ? "accept" : "reject"));
                }
                case "invite_result" -> {
                    boolean ok = payload.get("success").getAsBoolean();
                    if(!ok) JOptionPane.showMessageDialog(this, "Gửi lời mời thất bại");
                }
                case "match_start" -> {
                    String roomId = payload.get("roomId").getAsString();
                    String opp    = payload.get("opponent").getAsString();
                    String letters= payload.get("letters").getAsString();
                    int duration  = payload.get("durationSec").getAsInt();
                    new GameFrame(net, username, opp, roomId, letters, duration);
                    dispose();
                }
                case "invite_rejected" -> JOptionPane.showMessageDialog(this, "Lời mời bị từ chối");
            }
        });
    }
}
