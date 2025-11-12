package com.wordblock.client.ui;

import com.google.gson.*;
import com.wordblock.client.net.NetworkClient;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Map;
import java.util.Timer;
import javax.sound.sampled.*;
import javazoom.jl.player.Player;

public class GameFrame extends JFrame {
    private final NetworkClient net;
    private final String me, opponent, roomId;
    private JLabel lblPlayer1, lblPlayer2, lblTimer, lblScore1, lblScore2;
    private final JTextField txtInput = new JTextField(20);
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
    
    private Player bgPlayer;
    private Thread bgMusicThread;
    private boolean bgMusicPlaying = false;

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

        try { FlatLightLaf.setup(); } catch (Exception ignored) {}

        setTitle("Match: " + me + " vs " + opponent);
        setSize(850, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { onExit(); }
        });

        initUI();
        initEvents();
        
        playBackgroundMusicMP3("assets/sounds/bg.mp3");

        net.setOnMessage(this::onServer);
        setVisible(true);
    }

    /** ====================== UI SETUP ====================== */
    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        Font emojiFont = new Font("Segoe UI Emoji", Font.BOLD, 14);

        // ===== HEADER PANEL =====
        JPanel header = new JPanel(new GridLayout(2, 3, 10, 5));
        header.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        lblPlayer1 = new JLabel("👤 " + me, SwingConstants.CENTER);
        lblPlayer2 = new JLabel(opponent + " 👤", SwingConstants.CENTER);
        lblTimer = new JLabel("⏱ " + timeLeft + "s", SwingConstants.CENTER);
        lblScore1 = new JLabel("Score: 0", SwingConstants.CENTER);
        lblScore2 = new JLabel("Score: 0", SwingConstants.CENTER);

        lblPlayer1.setFont(emojiFont);
        lblPlayer2.setFont(emojiFont);
        lblTimer.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));
        lblScore1.setFont(emojiFont);
        lblScore2.setFont(emojiFont);

        header.add(lblPlayer1);
        header.add(lblTimer);
        header.add(lblPlayer2);
        header.add(lblScore1);
        header.add(new JLabel(""));
        header.add(lblScore2);
        add(header, BorderLayout.NORTH);

        // ===== LETTERS PANEL =====
        lettersPanel.setLayout(new GridLayout(2, 4, 10, 10));
        lettersPanel.setBorder(BorderFactory.createTitledBorder("Letters"));
        for (char c : letters.toCharArray()) {
            JLabel lbl = new JLabel(String.valueOf(c).toUpperCase(), SwingConstants.CENTER);
            lbl.setFont(new Font("Consolas", Font.BOLD, 28));
            lbl.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2, true));
            lbl.setOpaque(true);
            lbl.setBackground(new Color(230, 230, 250));
            lettersPanel.add(lbl);
        }
        add(lettersPanel, BorderLayout.CENTER);

        // ===== WORD LIST PANEL =====
        lstWords.setBorder(BorderFactory.createTitledBorder("Valid Words"));
        lstWords.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        scrollRight.setPreferredSize(new Dimension(200, 0));
        add(scrollRight, BorderLayout.EAST);

        // ===== INPUT PANEL =====
        JPanel inputWrapper = new JPanel(new BorderLayout(5, 5));
        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("Enter word:"));
        inputPanel.add(txtInput);
        inputPanel.add(btnSubmit);
        btnSubmit.putClientProperty(FlatClientProperties.STYLE, "background:#0078D7;");
        btnExit.putClientProperty(FlatClientProperties.STYLE, "background:#eee;");

        inputPanel.add(btnExit);

        lblStatus.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);

        inputWrapper.add(inputPanel, BorderLayout.CENTER);
        inputWrapper.add(lblStatus, BorderLayout.SOUTH);
        inputWrapper.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(inputWrapper, BorderLayout.SOUTH);
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

    private void showStatus(String message, Color color) {
        lblStatus.setForeground(color);
        lblStatus.setText(message);
        
        if (message.startsWith("✅")) {
            playSound("assets/sounds/correct.mp3");  // đường dẫn tới file âm thanh đúng
        } else if (message.startsWith("❌")) {
            playSound("assets/sounds/wrong.mp3");    // đường dẫn tới file âm thanh sai
        }
        
        Timer t = new Timer();
        t.schedule(new java.util.TimerTask() {
            public void run() { SwingUtilities.invokeLater(() -> lblStatus.setText(" ")); }
        }, 3000);
    }

    private void updateScores(JsonObject scores) {
        scoreMe = scores.has(me) ? scores.get(me).getAsInt() : 0;
        scoreOp = scores.has(opponent) ? scores.get(opponent).getAsInt() : 0;
        lblScore1.setText("Score: " + scoreMe);
        lblScore2.setText("Score: " + scoreOp);
    }

    private String pickWinner() {
        stopBackgroundMusicMP3();
        if (scoreMe > scoreOp) {
            playSound("assets/sounds/win.mp3");
            return me;
        }
        if (scoreOp > scoreMe) {
            playSound("assets/sounds/lose.mp3");
            return opponent;
        }
        playSound("assets/sounds/draw.mp3");
        return "Draw";
    }
    
    private void playBackgroundMusicMP3(String path) {
        bgMusicPlaying = true;
        bgMusicThread = new Thread(() -> {
            while (bgMusicPlaying) {
                try (FileInputStream fis = new FileInputStream(path)) {
                    bgPlayer = new Player(fis);
                    bgPlayer.play(); // Phát nhạc
                } catch (Exception e) {
                    if (bgMusicPlaying)
                        System.err.println("Background MP3 error: " + e.getMessage());
                    break;
                }
            }
        });
        bgMusicThread.start();
    }

    private void stopBackgroundMusicMP3() {
        bgMusicPlaying = false;
        try {
            if (bgPlayer != null) {
                bgPlayer.close(); // Dừng ngay lập tức
            }
        } catch (Exception ignored) {}

        if (bgMusicThread != null && bgMusicThread.isAlive()) {
            bgMusicThread.interrupt();
        }
    }

    
    private void onExit() {
        int confirm = JOptionPane.showConfirmDialog(this, "Do you want to leave the match?", "Exit", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            stopBackgroundMusicMP3();
            if (timer != null) timer.cancel();
            net.send(Map.of("type", "leave_game"));
            new LobbyFrame(net, me);
            dispose();
        }
    }

    /** ====================== SERVER MESSAGE HANDLING ====================== */
    private void onServer(String line) {
        // Keep original logic, just SwingUtilities
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
                        stopBackgroundMusicMP3();
                        boolean endedByLeave = p.has("endedByLeave") && p.get("endedByLeave").getAsBoolean();
                        if (endedByLeave) {
                            String winner = pickWinner();
                            
                            JOptionPane.showMessageDialog(this,
                                    "🎮 Game Over!\nOpponent Left!\nWinner: " + winner,
                                    "Game Ended", JOptionPane.INFORMATION_MESSAGE);
                            new LobbyFrame(net, me);
                            dispose();
                        } else {
                            showRematchDialog();
                        }
                    }
                    case "rematch_offer" -> { if (!rematchDialogShown) showRematchDialog(); }
                    case "rematch_update", "rematch_cancelled", "rematch_start" -> handleRematchMessage(type, p);
                }
            } catch (Exception ex) {
                System.err.println("Error parsing message: " + ex.getMessage());
            }
        });
    }

    // ===== REMATCH HANDLING =====
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
    
    /** Phát âm thanh khi submit từ */
    private void playSound(String path) {
        new Thread(() -> {
            try (FileInputStream fis = new FileInputStream(path)) {
                Player player = new Player(fis);
                player.play();
            } catch (Exception e) {
                System.err.println("MP3 play error: " + e.getMessage());
            }
        }).start();
    }
}