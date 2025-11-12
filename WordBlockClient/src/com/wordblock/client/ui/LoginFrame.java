package com.wordblock.client.ui;

import com.google.gson.*;
import com.wordblock.client.net.NetworkClient;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class LoginFrame extends JFrame {
    private final NetworkClient net;
    private final JTextField tfUser = new JTextField(15);
    private final JPasswordField tfPass = new JPasswordField(15);
    private final Gson gson = new Gson();

    public LoginFrame() {
        super("WordBlock – Login");

        // Kết nối tới server
//        net = new NetworkClient("172.11.151.215", 5000);
        net = new NetworkClient("localhost", 5000);
        boolean ok = net.connect();
        if (!ok) {
            JOptionPane.showMessageDialog(
                    this,
                    "Cannot connect to server.",
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(0);
        }

        net.setOnMessage(this::onServer);
        initUI();
    }

    /** ======================= GIAO DIỆN ======================= */
    private void initUI() {
        Font emojiFont = new Font("Segoe UI Emoji", Font.PLAIN, 14);

        // Ảnh nền
        ImageIcon bgIcon = new ImageIcon("assets/img/bg_login.jpg");

        JPanel bgPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(bgIcon.getImage(), 0, 0, getWidth(), getHeight(), this);
            }
        };
        bgPanel.setLayout(new GridBagLayout());

        // ===== LOGO GAME =====
        ImageIcon logoIcon = new ImageIcon(
                new ImageIcon("assets/img/gamelogo.png")
                        .getImage()
                        .getScaledInstance(300, 80, Image.SCALE_SMOOTH)
        );
        JLabel logoLabel = new JLabel(logoIcon);
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // ===== FORM LOGIN =====
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.anchor = GridBagConstraints.WEST;

        JLabel lbUser = new JLabel("👤 Username:");
        lbUser.setFont(emojiFont);
        lbUser.setForeground(Color.BLACK);

        JLabel lbPass = new JLabel("🔒 Password:");
        lbPass.setFont(emojiFont);
        lbPass.setForeground(Color.BLACK);

        tfUser.setFont(emojiFont);
        tfPass.setFont(emojiFont);

        c.gridx = 0; c.gridy = 0; contentPanel.add(lbUser, c);
        c.gridx = 1; contentPanel.add(tfUser, c);
        c.gridx = 0; c.gridy = 1; contentPanel.add(lbPass, c);
        c.gridx = 1; contentPanel.add(tfPass, c);

        JButton btLogin = new JButton("🔑 Login");
        JButton btReg = new JButton("📝 Register");
        btLogin.setFont(emojiFont);
        btReg.setFont(emojiFont);

        JPanel south = new JPanel();
        south.setOpaque(false);
        south.add(btReg);
        south.add(btLogin);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        contentPanel.add(south, c);

        // ===== BỐ CỤC TỔNG =====
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(logoLabel, BorderLayout.NORTH);
        wrapper.add(contentPanel, BorderLayout.CENTER);

        bgPanel.add(wrapper);

        setContentPane(bgPanel);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setSize(400, 300);
        setLocationRelativeTo(null);

        // ===== SỰ KIỆN NÚT =====
        btLogin.addActionListener(e -> {
            net.send("login", Map.of(
                    "username", tfUser.getText().trim(),
                    "password", new String(tfPass.getPassword())
            ));
        });

        btReg.addActionListener(e -> {
            new RegisterFrame(net).setVisible(true);
            dispose();
        });

        setVisible(true);
    }


    /** ======================= XỬ LÝ SERVER ======================= */
    private void onServer(String line) {
        SwingUtilities.invokeLater(() -> {
            try {
                JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
                String type = obj.get("type").getAsString();
                JsonObject payload = obj.getAsJsonObject("payload");

                switch (type) {
                    case "login_result" -> {
                        boolean ok = payload.get("success").getAsBoolean();
                        if (ok) {
                            String u = payload.get("username").getAsString();
                            new LobbyFrame(net, u);
                            dispose();
                        } else {
                            JOptionPane.showMessageDialog(
                                    this,
                                    payload.get("message").getAsString(),
                                    "Login Failed",
                                    JOptionPane.WARNING_MESSAGE
                            );
                        }
                    }
                    default -> { /* Ignore others */ }
                }
            } catch (Exception ex) {
                System.err.println("Error parsing server message: " + ex.getMessage());
            }
        });
    }
}
