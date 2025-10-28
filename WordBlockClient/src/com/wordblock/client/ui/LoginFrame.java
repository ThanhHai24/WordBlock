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
        net = new NetworkClient("localhost", 5000);
        boolean ok = net.connect();
        if(!ok){ JOptionPane.showMessageDialog(this,"Không kết nối được server","Error",JOptionPane.ERROR_MESSAGE); System.exit(0); }

        net.setOnMessage(this::onServer);

        var pn = new JPanel(new GridBagLayout());
        var c = new GridBagConstraints(); c.insets=new Insets(6,6,6,6);
        c.gridx=0;c.gridy=0;pn.add(new JLabel("Username:"),c);
        c.gridx=1;pn.add(tfUser,c);
        c.gridx=0;c.gridy=1;pn.add(new JLabel("Password:"),c);
        c.gridx=1;pn.add(tfPass,c);

        var btLogin = new JButton("Login");
        var btReg   = new JButton("Register");

        btLogin.addActionListener(e -> {
            net.send("login", Map.of("username", tfUser.getText().trim(), "password", new String(tfPass.getPassword())));
        });
        btReg.addActionListener(e -> {
            net.send("register", Map.of("username", tfUser.getText().trim(), "password", new String(tfPass.getPassword())));
        });

        var south = new JPanel(); south.add(btReg); south.add(btLogin);

        setLayout(new BorderLayout());
        add(pn, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack(); setLocationRelativeTo(null);
        setVisible(true);
    }

    private void onServer(String line){
        SwingUtilities.invokeLater(()->{
            JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
            String type = obj.get("type").getAsString();
            JsonObject payload = obj.getAsJsonObject("payload");

            switch(type){
                case "register_result" -> {
                    boolean ok = payload.get("success").getAsBoolean();
                    JOptionPane.showMessageDialog(this, ok? "Đăng ký thành công":"Đăng ký thất bại");
                }
                case "login_result" -> {
                    boolean ok = payload.get("success").getAsBoolean();
                    if(ok){
                        String u = payload.get("username").getAsString();
                        new LobbyFrame(net, u);
                        dispose();
                    } else JOptionPane.showMessageDialog(this, "Sai thông tin đăng nhập");
                }
                default -> { /* ignore others */ }
            }
        });
    }
}
