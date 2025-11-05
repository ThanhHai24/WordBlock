package com.wordblock.client.ui;

import com.google.gson.*;
import com.wordblock.client.net.NetworkClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;
import java.util.Timer;

public class GameFrame extends JFrame {
    private final NetworkClient net;
    private final String me, opponent, roomId;
    private JLabel lblPlayer1, lblPlayer2, lblTimer, lblScore1, lblScore2;
    private final JTextField txtInput = new JTextField(32);
    private final JButton btnSubmit = new JButton("Submit");
    private final JButton btnExit = new JButton("Exit");
    private final JPanel lettersPanel = new JPanel();
    private final JLabel lblStatus = new JLabel(" ");

    private final DefaultListModel<String> wordListModel = new DefaultListModel<>();
    private final JList<String> lstWords = new JList<>(wordListModel);
    private final JScrollPane scrollRight = new JScrollPane(lstWords);

    private String lastSubmittedWord = null;
    private Timer timer;
    private int timeLeft;
    private String letters;
    private int scoreMe = 0;
    private int scoreOp = 0;
    
    // --- Rematch state ---
    private boolean rematchDialogShown = false;
    private JDialog rematchDialog = null;

    public GameFrame(NetworkClient net, String me, String opponent, String roomId, String letters, int durationSec) {
        this.net = net;
        this.me = me;
        this.opponent = opponent;
        this.roomId = roomId;
        this.letters = letters;
        this.timeLeft = durationSec;

        setTitle("Match: " + me + " vs " + opponent);
        setSize(800, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                onExit();
            }
        });

        initUI(durationSec);
        initEvents();

        net.setOnMessage(this::onServer);
        setVisible(true);
    }

    /** ====================== UI SETUP ====================== */
    private void initUI(int durationSec) {
        setLayout(new BorderLayout(10, 10));

        // === HEADER ===
        JPanel topPanel = new JPanel(new GridLayout(2, 3, 10, 5));
        lblPlayer1 = new JLabel("👤 " + me, SwingConstants.CENTER);
        lblPlayer2 = new JLabel(opponent + " 👤", SwingConstants.CENTER);
        lblTimer = new JLabel("⏱ " + durationSec + "s", SwingConstants.CENTER);
        lblScore1 = new JLabel("Score: 0", SwingConstants.CENTER);
        lblScore2 = new JLabel("Score: 0", SwingConstants.CENTER);

        Font headerFont = new Font("Segoe UI Emoji", Font.BOLD, 14);
        lblPlayer1.setFont(headerFont);
        lblPlayer2.setFont(headerFont);
        lblTimer.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));

        topPanel.add(lblPlayer1);
        topPanel.add(lblTimer);
        topPanel.add(lblPlayer2);
        topPanel.add(lblScore1);
        topPanel.add(new JLabel(""));
        topPanel.add(lblScore2);
        add(topPanel, BorderLayout.NORTH);

        // === LETTERS PANEL ===
        lettersPanel.setLayout(new GridLayout(2, 4, 5, 5));
        for (char c : letters.toCharArray()) {
            JLabel lbl = new JLabel(String.valueOf(c).toUpperCase(), SwingConstants.CENTER);
            lbl.setFont(new Font("Consolas", Font.BOLD, 28));
            lbl.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            lettersPanel.add(lbl);
        }
        add(lettersPanel, BorderLayout.CENTER);

        // === WORD LIST (RIGHT SIDE) ===
        lstWords.setBorder(BorderFactory.createTitledBorder("Valid Words"));
        lstWords.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        scrollRight.setPreferredSize(new Dimension(200, 0));
        add(scrollRight, BorderLayout.EAST);

        // === INPUT AREA (BOTTOM) ===
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("Enter word:"));
        inputPanel.add(txtInput);
        inputPanel.add(btnSubmit);
        inputPanel.add(btnExit);

        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));

        bottomPanel.add(inputPanel, BorderLayout.CENTER);
        bottomPanel.add(lblStatus, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /** ====================== EVENTS ====================== */
    private void initEvents() {
        btnSubmit.addActionListener(this::onSubmit);
        btnExit.addActionListener(e -> onExit());
        txtInput.addActionListener(e -> btnSubmit.doClick());
        SwingUtilities.invokeLater(() -> txtInput.requestFocusInWindow());
    }

    /** ====================== LOGIC ====================== */
    private void onSubmit(ActionEvent e) {
        String word = txtInput.getText().trim();
        if (word.isEmpty()) return;
        lastSubmittedWord = word;
        net.send("word_submit", Map.of("roomId", roomId, "word", word));
        txtInput.setText("");
    }

    private void onServer(String line) {
        SwingUtilities.invokeLater(() -> {
            try {
                JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
                String type = obj.get("type").getAsString();
                JsonObject p = obj.getAsJsonObject("payload");

                switch (type) {
                    case "timer_tick" -> {
                        if (!roomId.equals(p.get("roomId").getAsString())) return;
                        int left = p.get("secLeft").getAsInt();
                        lblTimer.setText("⏱ " + left + "s");
                        updateScores(p.getAsJsonObject("scores"));
                    }

                    case "score_update" -> {
                        if (!roomId.equals(p.get("roomId").getAsString())) return;
                        updateScores(p.getAsJsonObject("scores"));
                    }

                    case "word_result" -> {
                        if (p.has("word") && !p.get("word").getAsString().equalsIgnoreCase(lastSubmittedWord))
                            return;

                        boolean ok = p.get("accepted").getAsBoolean();
                        if (ok) {
                            wordListModel.addElement(lastSubmittedWord);
                            lstWords.ensureIndexIsVisible(wordListModel.size() - 1);
                            showStatus("✅ Valid word: " + lastSubmittedWord, new Color(0, 128, 0));
                        } else {
                            showStatus("❌ Invalid word: " + lastSubmittedWord, Color.RED);
                        }
                    }

                    case "game_end" -> {
                        if (!roomId.equals(p.get("roomId").getAsString())) return;
                        updateScores(p.getAsJsonObject("scores"));
                        boolean endedByLeave = p.has("endedByLeave") && p.get("endedByLeave").getAsBoolean();
                        if (endedByLeave) {
                            updateScores(p.getAsJsonObject("scores"));
                            String winner = pickWinner();
                            JOptionPane.showMessageDialog(this,
                                    "🎮 Game Over!\nOpponent Left!.\nWinner: " + winner,
                                    "Game Ended", JOptionPane.INFORMATION_MESSAGE);
                            new LobbyFrame(net, me);
                            dispose();
                        } else {
                            showRematchDialog();
                        }
                    }

                    case "rematch_offer" -> {
                        if (!rematchDialogShown) showRematchDialog();
                    }

                    case "rematch_update", "rematch_cancelled", "rematch_start" -> {
                        handleRematchMessage(type, p);
                    }
                }
            } catch (Exception ex) {
                System.err.println("Error parsing message: " + ex.getMessage());
            }
        });
    }

    private void showStatus(String message, Color color) {
        lblStatus.setForeground(color);
        lblStatus.setText(message);

        // Mỗi thông báo có timer riêng, không bị ghi đè
        Timer timer = new Timer();
        timer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> lblStatus.setText(" "));
            }
        }, 3000);
    }

    private void updateScores(JsonObject scores) {
        scoreMe = scores.has(me) ? scores.get(me).getAsInt() : 0;
        scoreOp = scores.has(opponent) ? scores.get(opponent).getAsInt() : 0;
        lblScore1.setText("Score: " + scoreMe);
        lblScore2.setText("Score: " + scoreOp);
    }

    private String pickWinner() {
        if (scoreMe > scoreOp) return me;
        if (scoreOp > scoreMe) return opponent;
        return "Draw";
    }

    private void onExit() {
        int confirm = JOptionPane.showConfirmDialog(this, "Do you want to leave the match?", "Exit", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (timer != null) timer.cancel();
            net.send(Map.of("type", "leave_game"));
            new LobbyFrame(net, me);
            dispose();
        }
    }
    /** ====================== REMATCH HANDLING ====================== */
    private void showRematchDialog() {
        rematchDialogShown = true;
        updateScoresFromLabels();

        String winner = pickWinner();
        rematchDialog = new JDialog(this, "Game End!", true);
        rematchDialog.setLayout(new BorderLayout(10, 10));
        rematchDialog.setSize(320, 220);
        rematchDialog.setLocationRelativeTo(this);

        JLabel lbl = new JLabel("<html><center>🎮 Game End!!<br>Winner: <b>"
                + winner + "</b><br><br>Do you want to play again?</center></html>", SwingConstants.CENTER);
        rematchDialog.add(lbl, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton btnYes = new JButton("Play Again");
        JButton btnNo = new JButton("Leave");

        Runnable leaveAction = () -> {
            net.send("rematch_response", Map.of("roomId", roomId, "accept", false));
            lbl.setText("<html><center>❌ You left the game.<br>Returning to lobby...</center></html>");
            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    SwingUtilities.invokeLater(() -> {
                        rematchDialog.dispose();
                        new LobbyFrame(net, me);
                        dispose();
                    });
                }
            }, 1500);
        };
        
        btnYes.addActionListener(e -> {
            net.send("rematch_response", Map.of("roomId", roomId, "accept", true));
            btnYes.setEnabled(false);
            lbl.setText("<html><center>⏳ Waiting for opponent...</center></html>");
        });

        btnNo.addActionListener(e -> leaveAction.run());

        rematchDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                leaveAction.run();
            }
        });
        
        btnPanel.add(btnYes);
        btnPanel.add(btnNo);
        rematchDialog.add(btnPanel, BorderLayout.SOUTH);

        rematchDialog.setVisible(true);
    }

    private void handleRematchMessage(String type, JsonObject p) {
        switch (type) {
            case "rematch_update" -> {
                String user = p.get("user").getAsString();
                boolean accept = p.get("accept").getAsBoolean();

                if (rematchDialog != null && rematchDialog.isVisible()) {
                    Component[] comps = rematchDialog.getContentPane().getComponents();
                    JLabel lbl = null;
                    for (Component c : comps)
                        if (c instanceof JLabel) lbl = (JLabel) c;

                    if (lbl != null) {
                        if (accept) {
                            lbl.setText("<html><center>🤝 " + user + " accepted.<br>Waitting for your choice...</center></html>");
                        } else {
                            lbl.setText("<html><center>❌ " + user + " rejected.<br>Game End.<br>Winner: <b>"
                                    + pickWinner() + "</b></center></html>");
                        }
                    }

                    // Ẩn nút "Chơi lại" nếu đối thủ từ chối
                    if (!accept) {
                        for (Component c : ((JPanel) rematchDialog.getContentPane().getComponent(1)).getComponents()) {
                            if (c instanceof JButton btn && btn.getText().equals("Play Again")) {
                                btn.setEnabled(false);
                                btn.setVisible(false);
                            }
                        }
                    }
                    rematchDialog.revalidate();
                    rematchDialog.repaint();
                }
            }

            case "rematch_cancelled" -> {
                if (rematchDialog != null && rematchDialog.isVisible()) {
                    // Giữ lại popup nhưng cập nhật nội dung thay vì đóng
                    Component[] comps = rematchDialog.getContentPane().getComponents();
                    JLabel lbl = null;
                    for (Component c : comps)
                        if (c instanceof JLabel) lbl = (JLabel) c;
                    if (lbl != null) {
                        lbl.setText("<html><center>❌ Opponent left or rejected.<br>Game End!.<br>Winner: <b>"
                                + pickWinner() + "</b></center></html>");
                    }

                    // Ẩn các nút để chỉ hiển thị kết quả
                    JPanel panel = (JPanel) rematchDialog.getContentPane().getComponent(1);
                    for (Component c : panel.getComponents()) {
                        if (c instanceof JButton) c.setVisible(false);
                    }

                    // Tự quay về sảnh sau 2 giây
                    Timer t = new Timer();
                    t.schedule(new java.util.TimerTask() {
                        @Override
                        public void run() {
                            SwingUtilities.invokeLater(() -> {
                                rematchDialog.dispose();
                                new LobbyFrame(net, me);
                                dispose();
                            });
                        }
                    }, 2000);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Opponent left or rejected. Return to lobby.",
                            "Rematch Cancelled", JOptionPane.INFORMATION_MESSAGE);
                    new LobbyFrame(net, me);
                    dispose();
                }
            }

            case "rematch_start" -> {
                if (rematchDialog != null) rematchDialog.dispose();
                String newRoom = p.get("roomId").getAsString();
                String newLetters = p.get("letters").getAsString();
                int duration = p.get("durationSec").getAsInt();
                new GameFrame(net, me, opponent, newRoom, newLetters, duration);
                dispose();
            }
        }
    }


    /** Lấy lại điểm hiện tại từ nhãn để hiển thị winner */
    private void updateScoresFromLabels() {
        try {
            scoreMe = Integer.parseInt(lblScore1.getText().replaceAll("\\D", ""));
            scoreOp = Integer.parseInt(lblScore2.getText().replaceAll("\\D", ""));
        } catch (Exception ignore) {}
    }

}
