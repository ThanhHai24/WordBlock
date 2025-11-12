package com.wordblock.client.ui;

import com.google.gson.*;
import com.wordblock.client.net.NetworkClient;
import com.formdev.flatlaf.FlatLightLaf; // Hoặc FlatDarkLaf
import com.formdev.flatlaf.FlatClientProperties;

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

        // ================= Kết nối Server =================
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

        // ================= Khởi tạo giao diện =================
        initUI();
    }

    private void initUI() {
        // ----------------- Theme FlatLaf -----------------
        try {
            FlatLightLaf.setup(); // Hoặc FlatDarkLaf.setup();
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf: " + ex.getMessage());
        }

        // Font mặc định
        Font uiFont = new Font("Segoe UI", Font.PLAIN, 14);

        // ----------------- Ảnh nền -----------------
        ImageIcon bgIcon = new ImageIcon("assets/img/bg_login.jpg");
        JPanel bgPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(bgIcon.getImage(), 0, 0, getWidth(), getHeight(), this);
            }
        };
        bgPanel.setLayout(new GridBagLayout());

        // ----------------- Logo -----------------
        ImageIcon logoIcon = new ImageIcon(
                new ImageIcon("assets/img/gamelogo.png")
                        .getImage()
                        .getScaledInstance(300, 80, Image.SCALE_SMOOTH)
        );
        JLabel logoLabel = new JLabel(logoIcon);
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // ----------------- Form Login -----------------
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.anchor = GridBagConstraints.WEST;

        // Label và input
        // Load icon và resize
        ImageIcon userIcon = new ImageIcon("assets/icons/user.png");
        Image imgUser = userIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
        userIcon = new ImageIcon(imgUser);

        ImageIcon lockIcon = new ImageIcon("assets/icons/lock.png");
        Image imgLock = lockIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
        lockIcon = new ImageIcon(imgLock);

        // Gán vào JLabel
        JLabel lbUser = new JLabel("Username:");
        lbUser.setIcon(userIcon);

        JLabel lbPass = new JLabel("Password:");
        lbPass.setIcon(lockIcon);

        tfUser.setFont(uiFont);
        tfPass.setFont(uiFont);

        // Bo góc và placeholder
        tfUser.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter username...");
        tfPass.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter password...");
        tfUser.putClientProperty(FlatClientProperties.STYLE, "arc:12; focusWidth:1; margin:5,10,5,10");
        tfPass.putClientProperty(FlatClientProperties.STYLE, "arc:12; focusWidth:1; margin:5,10,5,10");

        c.gridx = 0; c.gridy = 0;
        formPanel.add(lbUser, c);
        c.gridx = 1;
        formPanel.add(tfUser, c);

        c.gridx = 0; c.gridy = 1;
        formPanel.add(lbPass, c);
        c.gridx = 1;
        formPanel.add(tfPass, c);

        // Nút Login / Register
        JButton btLogin = new JButton("Login");
        JButton btReg = new JButton("Register");
        btLogin.setFont(uiFont);
        btReg.setFont(uiFont);

        // Style nút
        btLogin.putClientProperty(FlatClientProperties.STYLE, "arc:20; background:#0078D7;");
        btReg.putClientProperty(FlatClientProperties.STYLE, "arc:20; background:#eeeeee;");

        JPanel south = new JPanel();
        south.setOpaque(false);
        south.add(btReg);
        south.add(btLogin);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        formPanel.add(south, c);

        // ----------------- Wrapper -----------------
        JPanel wrapper = new JPanel(new BorderLayout(10, 10));
        wrapper.setOpaque(false);
        wrapper.add(logoLabel, BorderLayout.NORTH);
        wrapper.add(formPanel, BorderLayout.CENTER);

        bgPanel.add(wrapper);

        // ----------------- Thiết lập frame -----------------
        setContentPane(bgPanel);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setSize(450, 350);
        setLocationRelativeTo(null);

        // ----------------- Sự kiện nút -----------------
        btLogin.addActionListener(e -> net.send("login", Map.of(
                "username", tfUser.getText().trim(),
                "password", new String(tfPass.getPassword())
        )));

        btReg.addActionListener(e -> {
            new RegisterFrame(net).setVisible(true);
            dispose();
        });

        setVisible(true);
    }

    // ======================= Xử lý server =======================
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
                    default -> {
                        // Ignore others
                    }
                }
            } catch (Exception ex) {
                System.err.println("Error parsing server message: " + ex.getMessage());
            }
        });
    }
}
