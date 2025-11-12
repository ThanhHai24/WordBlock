package com.wordblock.client.ui;

import com.google.gson.*;
import com.wordblock.client.net.NetworkClient;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

public class LobbyFrame extends JFrame {
    private final NetworkClient net;
    private final String username;
    private final Gson gson = new Gson();

    private final DefaultListModel<String> rankModel = new DefaultListModel<>();
    private final DefaultListModel<String> onlineModel = new DefaultListModel<>();

    private final JList<String> lstRank = new JList<>(rankModel);
    private final JList<String> lstOnline = new JList<>(onlineModel);

    private final JLabel lblWelcome = new JLabel();
    private final JButton btnRefresh = new JButton("🔄 Refresh");
    private final JButton btnChangePass = new JButton("🔑 Change Password");
    private final JButton btnHistory = new JButton("📜 Match History");
    private final JButton btnLogout = new JButton("🚪 Logout");

    public LobbyFrame(NetworkClient net, String username) {
        super("WordBlock – Lobby (" + username + ")");
        this.net = net;
        this.username = username;

        // ================= FlatLaf Theme =================
        try {
            FlatLightLaf.setup();
        } catch (Exception ex) {
            System.err.println("Failed to init FlatLaf: " + ex.getMessage());
        }

        initUI();
        initActions();

        net.setOnMessage(this::onServer);

        // Initial server requests
        net.send("list_online", Map.of());
        net.send("leaderboard_request", Map.of());
    }

    private void initUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 480);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        Font emojiFont = new Font("Segoe UI Emoji", Font.PLAIN, 14);

        // ===== Header =====
        lblWelcome.setText("👋 Hello, " + username + "!");
        lblWelcome.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        top.add(lblWelcome, BorderLayout.WEST);

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        for (JButton b : new JButton[]{btnRefresh, btnHistory, btnChangePass, btnLogout}) {
            b.setFont(emojiFont);
            b.putClientProperty(FlatClientProperties.STYLE,
                    "background:#0078D7; focusWidth:0;");
            rightButtons.add(b);
        }
        top.add(rightButtons, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // ===== Leaderboard Panel =====
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("🏆 Leaderboard"));
        lstRank.setFont(emojiFont);
        lstRank.putClientProperty(FlatClientProperties.STYLE, "background:#F8F8F8");
        leftPanel.add(new JScrollPane(lstRank), BorderLayout.CENTER);

        // ===== Online Players Panel =====
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("💬 Online Players"));
        lstOnline.setFont(emojiFont);
        lstOnline.putClientProperty(FlatClientProperties.STYLE, "background:#F8F8F8");
        rightPanel.add(new JScrollPane(lstOnline), BorderLayout.CENTER);

        // ===== Split Pane =====
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setDividerLocation(350);
        add(split, BorderLayout.CENTER);

        setVisible(true);
    }

    private void initActions() {
        // ===== Refresh =====
        btnRefresh.addActionListener(e -> {
            net.send("list_online", Map.of());
            net.send("leaderboard_request", Map.of());
        });

        // ===== Logout =====
        btnLogout.addActionListener(e -> {
            int opt = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to log out?",
                    "Confirm Logout",
                    JOptionPane.YES_NO_OPTION
            );
            if (opt == JOptionPane.YES_OPTION) {
                net.send("logout", Map.of()); // Gửi trước
                SwingUtilities.invokeLater(() -> {
                    dispose();
                    new LoginFrame().setVisible(true);
                });
            }
        });

        // ===== Change password =====
        btnChangePass.addActionListener(e -> {
            new ChangePwFrame(net, username).setVisible(true);
            dispose();
        });

        // ===== Match history =====
        btnHistory.addActionListener(e -> net.send(Map.of("type", "match_history", "payload", Map.of())));

        // ===== Double click challenge =====
        lstOnline.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selected = lstOnline.getSelectedValue();
                    if (selected == null) return;

                    String target = selected.split(" ")[0].trim();
                    if (target.equalsIgnoreCase(username)) {
                        JOptionPane.showMessageDialog(LobbyFrame.this, "⚠️ You cannot challenge yourself!");
                        return;
                    }

                    if (selected.contains("🎮 Playing")) {
                        JOptionPane.showMessageDialog(LobbyFrame.this, "⏳ That player is currently in a match!");
                        return;
                    }

                    int opt = JOptionPane.showConfirmDialog(
                            LobbyFrame.this,
                            "🎯 Do you want to challenge " + target + "?",
                            "Challenge Player",
                            JOptionPane.YES_NO_OPTION
                    );
                    if (opt == JOptionPane.YES_OPTION)
                        net.send("invite", Map.of("to", target));
                }
            }
        });
    }

    private void onServer(String line) {
        SwingUtilities.invokeLater(() -> {
            try {
                JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
                String type = obj.get("type").getAsString();
                JsonObject payload = obj.getAsJsonObject("payload");

                switch (type) {
                    case "online_list" -> {
                        onlineModel.clear();
                        var users = payload.getAsJsonArray("users");
                        for (var el : users) {
                            JsonObject u = el.getAsJsonObject();
                            String name = u.get("name").getAsString();
                            String status = u.get("status").getAsString();
                            int points = u.has("points") ? u.get("points").getAsInt() : 0;
                            String displayStatus = switch (status) {
                                case "Playing" -> " (🎮 Playing)";
                                case "Online" -> " (🟢 Online)";
                                default -> " (" + status + ")";
                            };
                            onlineModel.addElement(String.format("%s%s - %d pts", name, displayStatus, points));
                        }
                    }
                    case "leaderboard_result" -> {
                        rankModel.clear();
                        var arr = payload.getAsJsonArray("leaderboard");
                        int rank = 1;
                        for (var el : arr) {
                            JsonObject u = el.getAsJsonObject();
                            String name = u.get("username").getAsString();
                            int pts = u.get("points").getAsInt();
                            rankModel.addElement("🏅 " + rank++ + ". " + name + " – " + pts + " pts");
                        }
                    }
                    case "user_join", "user_left", "status_update" -> {
                        net.send("list_online", Map.of());
                        net.send("leaderboard_request", Map.of());
                    }
                    case "invite_received" -> {
                        String from = payload.get("from").getAsString();
                        int opt = JOptionPane.showConfirmDialog(
                                this,
                                "🎮 You have received a challenge from " + from + ".\nDo you want to accept?",
                                "Game Invitation",
                                JOptionPane.YES_NO_OPTION
                        );
                        net.send("invite_reply", Map.of(
                                "from", from,
                                "decision", opt == JOptionPane.YES_OPTION ? "accept" : "reject"
                        ));
                    }
                    case "invite_result" -> {
                        boolean ok = payload.get("success").getAsBoolean();
                        if (!ok) JOptionPane.showMessageDialog(this, "❌ Failed to send challenge!");
                    }
                    case "invite_rejected" -> JOptionPane.showMessageDialog(this, "🚫 Your invitation was rejected!");
                    case "match_start" -> {
                        String roomId = payload.get("roomId").getAsString();
                        String opp = payload.get("opponent").getAsString();
                        String letters = payload.get("letters").getAsString();
                        int duration = payload.get("durationSec").getAsInt();

                        new GameFrame(net, username, opp, roomId, letters, duration).setVisible(true);
                        dispose();
                    }
                    case "match_history_result" -> {
                        boolean success = payload.get("success").getAsBoolean();
                        if (!success) {
                            JOptionPane.showMessageDialog(this, "⚠️ Failed to load match history!");
                            return;
                        }
                        var matchesArray = payload.getAsJsonArray("matches");
                        java.lang.reflect.Type listType =
                                new com.google.gson.reflect.TypeToken<java.util.List<java.util.Map<String, Object>>>() {}.getType();
                        java.util.List<java.util.Map<String, Object>> matches =
                                new Gson().fromJson(matchesArray, listType);

                        new MatchHistoryFrame(matches).setVisible(true);
                    }
                }

            } catch (Exception ex) {
                System.err.println("Error parsing JSON from server: " + ex.getMessage());
            }
        });
    }
}
