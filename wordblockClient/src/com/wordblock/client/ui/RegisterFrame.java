package com.wordblock.client.ui;

import com.google.gson.*;
import com.wordblock.client.net.NetworkClient;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatClientProperties;

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

        // Chuyển hướng khi nhấn "X" về LoginFrame
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                new LoginFrame().setVisible(true);
                dispose();
            }
        });

        initUI();
    }

    private void initUI() {
        try {
            FlatLightLaf.setup(); // Hoặc FlatDarkLaf
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf: " + ex.getMessage());
        }

        Font uiFont = new Font("Segoe UI", Font.PLAIN, 14);

        // Panel form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;

        // Load icon PNG (resize)
        JLabel lbUser = new JLabel("Username:");
        lbUser.setIcon(loadIcon("assets/icons/user.png", 20, 20));
        JLabel lbPass = new JLabel("Password:");
        lbPass.setIcon(loadIcon("assets/icons/lock.png", 20, 20));
        JLabel lbConfirm = new JLabel("Confirm:");
        lbConfirm.setIcon(loadIcon("assets/icons/lock.png", 20, 20));

        lbUser.setFont(uiFont);
        lbPass.setFont(uiFont);
        lbConfirm.setFont(uiFont);

        tfUser.setFont(uiFont);
        tfPass.setFont(uiFont);
        tfConfirm.setFont(uiFont);

        // Placeholder + bo góc
        tfUser.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter username...");
        tfPass.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter password...");
        tfConfirm.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Confirm password...");

        tfUser.putClientProperty(FlatClientProperties.STYLE, "focusWidth:1; margin:5,10,5,10");
        tfPass.putClientProperty(FlatClientProperties.STYLE, "focusWidth:1; margin:5,10,5,10");
        tfConfirm.putClientProperty(FlatClientProperties.STYLE, "focusWidth:1; margin:5,10,5,10");

        // Add components
        c.gridx = 0; c.gridy = 0; formPanel.add(lbUser, c);
        c.gridx = 1; formPanel.add(tfUser, c);
        c.gridx = 0; c.gridy = 1; formPanel.add(lbPass, c);
        c.gridx = 1; formPanel.add(tfPass, c);
        c.gridx = 0; c.gridy = 2; formPanel.add(lbConfirm, c);
        c.gridx = 1; formPanel.add(tfConfirm, c);

        // Buttons
        JButton btReg = new JButton("Register");
        JButton btBack = new JButton("Back");
        btReg.setFont(uiFont);
        btBack.setFont(uiFont);

        btReg.putClientProperty(FlatClientProperties.STYLE, "background:#0078D7; foreground:#FFFFFF");
        btBack.putClientProperty(FlatClientProperties.STYLE, "background:#eeeeee;");

        btReg.addActionListener(e -> {
            String pass = new String(tfPass.getPassword());
            String confirm = new String(tfConfirm.getPassword());
            if (!pass.equals(confirm)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match!", "Register", JOptionPane.WARNING_MESSAGE);
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
        south.setOpaque(false);
        south.add(btBack);
        south.add(btReg);

        // Wrapper
        JPanel wrapper = new JPanel(new BorderLayout(10, 10));
        wrapper.setOpaque(false);
        wrapper.add(formPanel, BorderLayout.CENTER);
        wrapper.add(south, BorderLayout.SOUTH);

        wrapper.putClientProperty(FlatClientProperties.STYLE, "background:#F8F8F8; borderWidth:0;");

        add(wrapper);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private ImageIcon loadIcon(String path, int w, int h) {
        ImageIcon icon = new ImageIcon(path);
        Image img = icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
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
                        ok ? "Registration successful!" : "Registration failed.",
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
