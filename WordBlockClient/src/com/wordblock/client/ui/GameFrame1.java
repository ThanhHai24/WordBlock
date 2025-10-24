package com.wordblock.client.ui;

import com.google.gson.*;
import com.wordblock.client.net.NetworkClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.Timer;

public class GameFrame1 extends JFrame {
    private final NetworkClient net;
    private final String me, opponent, roomId;
    private final JLabel lblPlayer1, lblPlayer2, lblTimer, lblScore1, lblScore2;
    private final JTextField txtInput = new JTextField(32);
    private final JButton btnSubmit = new JButton("Gửi");
    private final JButton btnExit = new JButton("Thoát");
    private final JPanel lettersPanel = new JPanel();
    private final JLabel lblStatus = new JLabel(" ");


    // --- Bên phải: danh sách từ hợp lệ ---
    private final DefaultListModel<String> wordListModel = new DefaultListModel<>();
    private final JList<String> lstWords = new JList<>(wordListModel);
    private final JScrollPane scrollRight = new JScrollPane(lstWords);

    private String submitedWord = "";
    private Timer timer;
    private int timeLeft;
    private String letters;
    
    private int scoreMe = 0;
    private int scoreOp = 0;

    public GameFrame1(NetworkClient net, String me, String opponent, String roomId, String letters, int durationSec) {
        this.net = net;
        this.me = me;
        this.opponent = opponent;
        this.roomId = roomId;
        this.letters = letters;
        this.timeLeft = durationSec;

        setTitle("Trận đấu giữa " + me + " và " + opponent);
        setSize(800, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                onExit();
            }
        });
        setLayout(new BorderLayout(10, 10));

        // === Header ===
        JPanel topPanel = new JPanel(new GridLayout(2, 3, 10, 5));
        lblPlayer1 = new JLabel("👤 " + me, SwingConstants.CENTER);
        lblPlayer2 = new JLabel(opponent + " 👤", SwingConstants.CENTER);
        lblTimer = new JLabel("⏱ " + durationSec + "s", SwingConstants.CENTER);
        lblScore1 = new JLabel("Điểm: 0", SwingConstants.CENTER);
        lblScore2 = new JLabel("Điểm: 0", SwingConstants.CENTER);

        lblPlayer1.setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));
        lblPlayer2.setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));
        lblTimer.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));

        topPanel.add(lblPlayer1);
        topPanel.add(lblTimer);
        topPanel.add(lblPlayer2);
        topPanel.add(lblScore1);
        topPanel.add(new JLabel(""));
        topPanel.add(lblScore2);
        add(topPanel, BorderLayout.NORTH);

        // === Letters (ở giữa) ===
        lettersPanel.setLayout(new GridLayout(2, 4, 5, 5));
        for (char c : letters.toCharArray()) {
            JLabel lbl = new JLabel(String.valueOf(c).toUpperCase(), SwingConstants.CENTER);
            lbl.setFont(new Font("Consolas", Font.BOLD, 28));
            lbl.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            lettersPanel.add(lbl);
        }
        add(lettersPanel, BorderLayout.CENTER);

        // === Danh sách từ đúng (bên phải) ===
        lstWords.setBorder(BorderFactory.createTitledBorder("Từ hợp lệ"));
        JScrollPane scrollRight = new JScrollPane(lstWords);
        scrollRight.setPreferredSize(new Dimension(200, 0));
        add(scrollRight, BorderLayout.EAST);

        // === Input (dưới cùng) ===
        JPanel bottomPanel = new JPanel();
        txtInput.setFont(txtInput.getFont().deriveFont(20f));
        txtInput.setPreferredSize(new Dimension(300, 40));
        bottomPanel.add(new JLabel("Nhập từ:"));
        bottomPanel.add(txtInput);
        bottomPanel.add(btnSubmit);
        bottomPanel.add(btnExit);
        add(bottomPanel, BorderLayout.SOUTH);
        
        bottomPanel.setLayout(new BorderLayout());
        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("Nhập từ:"));
        inputPanel.add(txtInput);
        inputPanel.add(btnSubmit);
        inputPanel.add(btnExit);

        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));

        bottomPanel.add(inputPanel, BorderLayout.CENTER);
        bottomPanel.add(lblStatus, BorderLayout.SOUTH);

        
        

        // === Event ===
        btnSubmit.addActionListener(this::onSubmit);
        btnExit.addActionListener(e -> onExit());
        txtInput.addActionListener(e -> btnSubmit.doClick());

        SwingUtilities.invokeLater(() -> txtInput.requestFocusInWindow());

        // === Socket listener ===
        net.setOnMessage(this::onServer);

        setVisible(true);
    }

    private void onSubmit(ActionEvent e) {
        String w = txtInput.getText().trim();
        if (w.isEmpty()) return;
        submitedWord = w;
        net.send("word_submit", Map.of("roomId", roomId, "word", w));
        txtInput.setText("");
    }

    private void onServer(String line) {
        SwingUtilities.invokeLater(() -> {
            JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
            String type = obj.get("type").getAsString();
            JsonObject p = obj.getAsJsonObject("payload");

            switch (type) {
                case "timer_tick" -> {
                    if (!roomId.equals(p.get("roomId").getAsString())) return;
                    int left = p.get("secLeft").getAsInt();
                    lblTimer.setText("⏱ " + left + "s");
                    JsonObject scores = p.getAsJsonObject("scores");
                    updateScores(scores);
                }
                case "score_update" -> {
                    if (!roomId.equals(p.get("roomId").getAsString())) return;
                    updateScores(p.getAsJsonObject("scores"));
                }
                case "word_result" -> {
                    boolean ok = p.get("accepted").getAsBoolean();
                    if (!submitedWord.isEmpty()) {
                        if (ok) {
                            wordListModel.addElement(submitedWord);
                            int lastIndex = wordListModel.getSize() - 1;
                            if (lastIndex >= 0) lstWords.ensureIndexIsVisible(lastIndex);
                            lblStatus.setForeground(new Color(0, 128, 0));
                            lblStatus.setText("✅ Từ hợp lệ!");
                        } else {
                            lblStatus.setForeground(Color.RED);
                            lblStatus.setText("❌ Từ không hợp lệ!");
                        }

                        // Tự động ẩn thông báo sau 1.5 giây
                        new javax.swing.Timer(1500, ev -> lblStatus.setText(" ")).start();
                    }
                }
                case "game_end" -> {
                    if (!roomId.equals(p.get("roomId").getAsString())) return;
                    updateScores(p.getAsJsonObject("scores"));
                    String winner = pickWinner();
                    JOptionPane.showMessageDialog(this, "🎮 Trò chơi kết thúc!\nNgười thắng: " + winner);
                    new LobbyFrame(net, me);
                    dispose();
                }
            }
        });
    }

    private void updateScores(JsonObject scores) {
        scoreMe = scores.has(me) ? scores.get(me).getAsInt() : 0;
        scoreOp = scores.has(opponent) ? scores.get(opponent).getAsInt() : 0;
        lblScore1.setText("Điểm: " + scoreMe);
        lblScore2.setText("Điểm: " + scoreOp);
    }

    private String pickWinner() {
        if (scoreMe > scoreOp) return me;
        if (scoreOp > scoreMe) return opponent;
        return "Hòa";
    }

    private void onExit() {
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có muốn thoát trận không?", "Thoát", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (timer != null) timer.cancel();
            net.send(Map.of("type", "leave_game"));
            new LobbyFrame(net, me);
            dispose();
        }
    }
}
