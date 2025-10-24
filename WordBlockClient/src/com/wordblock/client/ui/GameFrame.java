package com.wordblock.client.ui;

import com.google.gson.*;
import com.wordblock.client.net.NetworkClient;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

public class GameFrame extends JFrame {
    private final NetworkClient net;
    private final String me, opponent, roomId;
    private final JLabel lbTimer = new JLabel("120");
    private final JLabel lbLetters = new JLabel();
    private final JTextField tfWord = new JTextField(12);
    private final JButton btSend = new JButton("Send");
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Player","Score"},0);

    public GameFrame(NetworkClient net, String me, String opponent, String roomId, String letters, int durationSec){
        super("WordBlock – Room "+roomId);
        this.net=net; this.me=me; this.opponent=opponent; this.roomId=roomId;

        lbLetters.setText("Letters: "+letters);
        lbTimer.setFont(lbTimer.getFont().deriveFont(24f));
        var tbl = new JTable(model); model.addRow(new Object[]{me,0}); model.addRow(new Object[]{opponent,0});

        var north = new JPanel(new BorderLayout());
        north.add(lbLetters, BorderLayout.CENTER);
        north.add(lbTimer, BorderLayout.EAST);

        var center = new JScrollPane(tbl);

        var south = new JPanel();
        south.add(new JLabel("Word:")); south.add(tfWord); south.add(btSend);
        btSend.addActionListener(e -> {
            String w = tfWord.getText().trim();
            if(!w.isEmpty()){ net.send("word_submit", Map.of("roomId", roomId, "word", w)); tfWord.setText(""); }
        });
        
        tfWord.addActionListener(e -> btSend.doClick());

        setLayout(new BorderLayout());
        add(north, BorderLayout.NORTH); add(center, BorderLayout.CENTER); add(south, BorderLayout.SOUTH);
        setSize(420, 360); setLocationRelativeTo(null); setDefaultCloseOperation(EXIT_ON_CLOSE); setVisible(true);

        net.setOnMessage(this::onServer);
    }

    private void onServer(String line){
        SwingUtilities.invokeLater(()->{
            JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
            String type = obj.get("type").getAsString();
            JsonObject p = obj.getAsJsonObject("payload");
            switch (type){
                case "timer_tick" -> {
                    if(!roomId.equals(p.get("roomId").getAsString())) return;
                    int left = p.get("secLeft").getAsInt();
                    lbTimer.setText(String.valueOf(left));
                    JsonObject scores = p.getAsJsonObject("scores");
                    updateScores(scores);
                }
                case "score_update" -> {
                    if(!roomId.equals(p.get("roomId").getAsString())) return;
                    updateScores(p.getAsJsonObject("scores"));
                }
                case "word_result" -> {
                    boolean ok = p.get("accepted").getAsBoolean();
                    JOptionPane.showMessageDialog(this, ok? "Accepted ✅" : "Rejected ❌");
                }
                case "game_end" -> {
                    if(!roomId.equals(p.get("roomId").getAsString())) return;
                    updateScores(p.getAsJsonObject("scores"));
                    String winner = pickWinner();
                    JOptionPane.showMessageDialog(this, "Game end! Winner: "+winner);
                    // quay lại lobby
                    new LobbyFrame(net, me);
                    dispose();
                }
            }
        });
    }

    private void updateScores(JsonObject scores){
        int meScore = scores.has(me)? scores.get(me).getAsInt() : 0;
        int opScore = scores.has(opponent)? scores.get(opponent).getAsInt() : 0;
        model.setRowCount(0);
        model.addRow(new Object[]{me, meScore});
        model.addRow(new Object[]{opponent, opScore});
    }

    private String pickWinner(){
        int a = (int)model.getValueAt(0,1);
        int b = (int)model.getValueAt(1,1);
        if(a>b) return me; if(b>a) return opponent; return "Draw";
    }
}
