package com.wordblock.client.ui;

import com.google.gson.*;
import com.wordblock.client.net.NetworkClient;

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
    private final JButton btnRefresh = new JButton("🔄 Làm mới");
    private final JButton btnChangePass = new JButton("Đổi mật khẩu");
    private final JButton btnLogout = new JButton("Đăng xuất");

    public LobbyFrame(NetworkClient net, String username) {
        super("WordBlock – Lobby (" + username + ")");
        this.net = net;
        this.username = username;

        // === Cấu hình khung ===
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 480);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // === Header ===
        lblWelcome.setText("Xin chào, " + username + " 👋");
        lblWelcome.setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        top.add(lblWelcome, BorderLayout.WEST);

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightButtons.add(btnChangePass);
        rightButtons.add(btnRefresh);
        rightButtons.add(btnLogout);
        top.add(rightButtons, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // === Bảng xếp hạng (bên trái) ===
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("🏆 Bảng xếp hạng"));
        lstRank.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        leftPanel.add(new JScrollPane(lstRank), BorderLayout.CENTER);

        // === Danh sách online (bên phải) ===
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("💬 Người chơi online"));
        lstOnline.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        rightPanel.add(new JScrollPane(lstOnline), BorderLayout.CENTER);

        // === Chia đôi 2 bảng ===
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setDividerLocation(350);
        add(split, BorderLayout.CENTER);

        // === Sự kiện nút ===
        btnRefresh.addActionListener(e -> {
            net.send("list_online", Map.of());
            net.send("leaderboard_request", Map.of());
        });

        btnLogout.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Bạn đã đăng xuất!");
            System.exit(0);
        });

        // Double click để mời người chơi
        lstOnline.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selected = lstOnline.getSelectedValue();
                    if (selected == null) return;

                    // Tách tên từ chuỗi "alice (Online)"
                    String target = selected.split(" ")[0].trim();
                    if (target.equalsIgnoreCase(username)) {
                        JOptionPane.showMessageDialog(LobbyFrame.this, "Không thể tự mời chính mình!");
                        return;
                    }

                    int opt = JOptionPane.showConfirmDialog(
                            LobbyFrame.this,
                            "Bạn có muốn khiêu chiến " + target + " không?",
                            "Khiêu chiến",
                            JOptionPane.YES_NO_OPTION
                    );
                    if (opt == JOptionPane.YES_OPTION)
                        net.send("invite", Map.of("to", target));
                }
            }
        });

        // === Đăng ký callback server ===
        net.setOnMessage(this::onServer);

        // === Gửi yêu cầu ban đầu ===
        net.send("list_online", Map.of());
        net.send("leaderboard_request", Map.of());

        setVisible(true);
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
                            // Hiển thị đẹp hơn: Online màu xanh, Playing màu cam
                            String display = switch (status) {
                                case "Playing" -> name + " (🎮 Đang chơi)";
                                case "Online" -> name + " (🟢 Online)";
                                default -> name + " (" + status + ")";
                            };
                            onlineModel.addElement(display);
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
                            rankModel.addElement(rank++ + ". " + name + " – " + pts + " điểm");
                        }
                    }
                    
                    case "user_join", "user_left" -> {
                        net.send("list_online", Map.of());
                    }
                    
                    case "invite_received" -> {
                        String from = payload.get("from").getAsString();
                        int opt = JOptionPane.showConfirmDialog(
                                this,
                                "Nhận lời mời từ " + from + "?",
                                "Lời mời thi đấu",
                                JOptionPane.YES_NO_OPTION
                        );
                        net.send("invite_reply", Map.of(
                                "from", from,
                                "decision", opt == JOptionPane.YES_OPTION ? "accept" : "reject"
                        ));
                    }

                    case "invite_result" -> {
                        boolean ok = payload.get("success").getAsBoolean();
                        if (!ok)
                            JOptionPane.showMessageDialog(this, "Gửi lời mời thất bại!");
                    }

                    case "invite_rejected" ->
                            JOptionPane.showMessageDialog(this, "Lời mời bị từ chối!");

                    case "match_start" -> {
                        String roomId = payload.get("roomId").getAsString();
                        String opp = payload.get("opponent").getAsString();
                        String letters = payload.get("letters").getAsString();
                        int duration = payload.get("durationSec").getAsInt();

                        new GameFrame1(net, username, opp, roomId, letters, duration).setVisible(true);
                        dispose();
                    }
                }

            } catch (Exception ex) {
                System.err.println("Lỗi parse JSON từ server: " + ex.getMessage());
            }
        });
    }
}
