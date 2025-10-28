/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.wordblock.client.ui;

/**
 *
 * @author duydk
 */

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wordblock.client.net.NetworkClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Map;

public class ChangePwFrame extends JFrame {
    private final NetworkClient net;
    private final JTextField tfOld = new JPasswordField();
    private final JTextField tfNew = new JPasswordField();
    private final JTextField tfConfirm = new JPasswordField();
    private final JButton btnSubmit = new JButton("Change Password");
    private final JLabel lbMsg = new JLabel(" ", SwingConstants.CENTER);

    public ChangePwFrame(NetworkClient net) {
        this.net = net;
        setTitle("Change Password");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setResizable(false);

        initUI();
        net.setOnMessage(this::onServer);
    }

    private void initUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(250, 250, 250));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel lbTitle = new JLabel("Change Password", SwingConstants.CENTER);
        lbTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lbTitle.setForeground(new Color(50, 50, 50));

        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(lbTitle, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;

        JLabel lbOld = new JLabel("Old Password:");
        JLabel lbNew = new JLabel("New Password:");
        JLabel lbConfirm = new JLabel("Confirm Password:");

        tfOld.setColumns(18);
        tfNew.setColumns(18);
        tfConfirm.setColumns(18);

        // === Old Password ===
        gbc.gridx = 0;
        panel.add(lbOld, gbc);
        gbc.gridx = 1;
        panel.add(tfOld, gbc);

        // === New Password ===
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(lbNew, gbc);
        gbc.gridx = 1;
        panel.add(tfNew, gbc);

        // === Confirm Password ===
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(lbConfirm, gbc);
        gbc.gridx = 1;
        panel.add(tfConfirm, gbc);

        // === Submit Button ===
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        btnSubmit.setBackground(new Color(70, 130, 180));
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setFocusPainted(false);
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSubmit.addActionListener(this::handleChangePw);
        panel.add(btnSubmit, gbc);

        // === Message Label ===
        gbc.gridy++;
        lbMsg.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbMsg.setForeground(new Color(100, 100, 100));
        panel.add(lbMsg, gbc);

        add(panel);
    }

    private void handleChangePw(ActionEvent e) {
        String oldPw = tfOld.getText().trim();
        String newPw = tfNew.getText().trim();
        String confirm = tfConfirm.getText().trim();

        if (oldPw.isEmpty() || newPw.isEmpty() || confirm.isEmpty()) {
            lbMsg.setText("Please fill in all fields.");
            lbMsg.setForeground(Color.RED);
            return;
        }
        if (!newPw.equals(confirm)) {
            lbMsg.setText("Password confirmation does not match.");
            lbMsg.setForeground(Color.RED);
            return;
        }

        net.send("change_password", Map.of(
                "old_password", oldPw,
                "new_password", newPw
        ));
        lbMsg.setText("Processing...");
        lbMsg.setForeground(Color.GRAY);
    }

    private void onServer(String line) {
        try {
            JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
            String type = obj.get("type").getAsString();
            if (!type.equals("change_password_result")) return;

            JsonObject payload = obj.getAsJsonObject("payload");
            boolean success = payload.get("success").getAsBoolean();
            String message = payload.get("message").getAsString();

            SwingUtilities.invokeLater(() -> {
                lbMsg.setText(message);
                lbMsg.setForeground(success ? new Color(0, 128, 0) : Color.RED);
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
}
