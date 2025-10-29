package com.wordblock.client.ui;

import com.google.gson.*;
import com.wordblock.client.net.NetworkClient;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class RegisterFrame extends JFrame {
    private final NetworkClient net;
    private final JTextField tfUser = new JTextField(15);
    private final JPasswordField tfPass = new JPasswordField(15);
    private final JPasswordField tfConfirm = new JPasswordField(15);

    public RegisterFrame(NetworkClient net) {
        super("WordBlock – Register");
        this.net = net;
        net.setOnMessage(this::onServer);
        initUI();
    }

    private void initUI() {
        Font emojiFont = new Font("Segoe UI Emoji", Font.PLAIN, 14);

        JPanel pn = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;

        JLabel lbUser = new JLabel("👤 Username:");
        JLabel lbPass = new JLabel("🔒 Password:");
        JLabel lbConfirm = new JLabel("🔁 Confirm:");
        lbUser.setFont(emojiFont);
        lbPass.setFont(emojiFont);
        lbConfirm.setFont(emojiFont);
        tfUser.setFont(emojiFont);
        tfPass.setFont(emojiFont);
        tfConfirm.setFont(emojiFont);

        c.gridx = 0; c.gridy = 0; pn.add(lbUser, c);
        c.gridx = 1; pn.add(tfUser, c);
        c.gridx = 0; c.gridy = 1; pn.add(lbPass, c);
        c.gridx = 1; pn.add(tfPass, c);
        c.gridx = 0; c.gridy = 2; pn.add(lbConfirm, c);
        c.gridx = 1; pn.add(tfConfirm, c);

        JButton btReg = new JButton("📝 Register");
        JButton btBack = new JButton("⬅️ Back");
        btReg.setFont(emojiFont);
        btBack.setFont(emojiFont);

        btReg.addActionListener(e -> {
            String pass = new String(tfPass.getPassword());
            String confirm = new String(tfConfirm.getPassword());
            if (!pass.equals(confirm)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match!");
                return;
            }
            net.send("register", Map.of(
                    "username", tfUser.getText().trim(),
                    "password", pass
            ));
        });

        btBack.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });

        JPanel south = new JPanel();
        south.add(btBack);
        south.add(btReg);

        setLayout(new BorderLayout());
        add(pn, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void onServer(String line) {
        try {
            JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
            String type = obj.get("type").getAsString();
            if (!type.equals("register_result")) return;

            JsonObject payload = obj.getAsJsonObject("payload");
            boolean ok = payload.get("success").getAsBoolean();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                        this,
                        ok ? "✅ Registration successful!" : "❌ Registration failed.",
                        "Register",
                        ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE
                );
                if (ok) {
                    new LoginFrame().setVisible(true);
                    dispose();
                }
            });
        } catch (Exception ex) {
            System.err.println("Error parsing server message: " + ex.getMessage());
        }
    }
}
