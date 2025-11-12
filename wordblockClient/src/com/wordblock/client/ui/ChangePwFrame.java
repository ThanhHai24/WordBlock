package com.wordblock.client.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wordblock.client.net.NetworkClient;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Map;

public class ChangePwFrame extends JFrame {
    private final NetworkClient net;
    private final String username;
    private final JPasswordField tfOld = new JPasswordField();
    private final JPasswordField tfNew = new JPasswordField();
    private final JPasswordField tfConfirm = new JPasswordField();
    private final JButton btnSubmit = new JButton("Change Password");
    private final JLabel lbMsg = new JLabel(" ", SwingConstants.CENTER);

    public ChangePwFrame(NetworkClient net, String username) {
        FlatLightLaf.setup(); // ✅ FlatLaf theme
        this.net = net;
        this.username = username;

        setTitle("Change Password");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(420, 350);
        setLocationRelativeTo(null);
        setResizable(false);

        initUI();
        net.setOnMessage(this::onServer);
    }

    private void initUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UIManager.getColor("Panel.background"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // === Title ===
        JLabel lbTitle = new JLabel("🔐 Change Password", SwingConstants.CENTER);
        lbTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold 18px 'Segoe UI';");
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(lbTitle, gbc);

        // === Fields ===
        gbc.gridwidth = 1;
        gbc.gridy++;

        JLabel lbOld = new JLabel("Old Password:");
        JLabel lbNew = new JLabel("New Password:");
        JLabel lbConfirm = new JLabel("Confirm Password:");

        stylePasswordField(tfOld, "Enter old password");
        stylePasswordField(tfNew, "Enter new password");
        stylePasswordField(tfConfirm, "Re-enter new password");

        // Old password
        gbc.gridx = 0;
        panel.add(lbOld, gbc);
        gbc.gridx = 1;
        panel.add(tfOld, gbc);

        // New password
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(lbNew, gbc);
        gbc.gridx = 1;
        panel.add(tfNew, gbc);

        // Confirm password
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(lbConfirm, gbc);
        gbc.gridx = 1;
        panel.add(tfConfirm, gbc);

        // === Submit Button ===
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSubmit.putClientProperty(FlatClientProperties.STYLE,
                "background: #1976D2; arc: 20; borderWidth: 0; hoverBackground: #1565C0;");
        btnSubmit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSubmit.addActionListener(this::handleChangePw);
        panel.add(btnSubmit, gbc);

        // === Message Label ===
        gbc.gridy++;
        lbMsg.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbMsg.setForeground(new Color(120, 120, 120));
        panel.add(lbMsg, gbc);

        // === Return to Lobby on close ===
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                dispose();
                new LobbyFrame(net, username).setVisible(true);
            }
        });

        add(panel);
    }

    private void stylePasswordField(JPasswordField field, String placeholder) {
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        field.putClientProperty(FlatClientProperties.STYLE,
                "arc:15; borderWidth:1; borderColor:#D0D0D0; focusWidth:2; focusColor:#1976D2;");
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setColumns(18);
    }

    private void handleChangePw(ActionEvent e) {
        String oldPw = new String(tfOld.getPassword()).trim();
        String newPw = new String(tfNew.getPassword()).trim();
        String confirm = new String(tfConfirm.getPassword()).trim();

        if (oldPw.isEmpty() || newPw.isEmpty() || confirm.isEmpty()) {
            showMessage("Please fill in all fields.", Color.RED);
            return;
        }
        if (!newPw.equals(confirm)) {
            showMessage("Password confirmation does not match.", Color.RED);
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Processing...");
        showMessage("Processing...", Color.GRAY);

        net.send("change_password", Map.of(
                "old_password", oldPw,
                "new_password", newPw
        ));
    }

    private void onServer(String line) {
        try {
            JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
            if (!obj.get("type").getAsString().equals("change_password_result")) return;

            JsonObject payload = obj.getAsJsonObject("payload");
            boolean success = payload.get("success").getAsBoolean();
            String message = payload.get("message").getAsString();

            SwingUtilities.invokeLater(() -> {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Change Password");

                showMessage(message, success ? new Color(0, 128, 0) : Color.RED);
                if (success) {
                    JOptionPane.showMessageDialog(this,
                            "Password changed successfully!",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                }
            });
        } catch (Exception ex) {
            System.err.println("Error parsing message in ChangePwFrame: " + ex.getMessage());
        }
    }

    private void showMessage(String text, Color color) {
        lbMsg.setText(text);
        lbMsg.setForeground(color);
    }
}
