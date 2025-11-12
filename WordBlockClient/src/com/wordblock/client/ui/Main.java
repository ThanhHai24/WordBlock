package com.wordblock.client.ui;

import com.formdev.flatlaf.FlatLightLaf; // hoặc FlatDarkLaf
import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        try {
            // Cài đặt theme FlatLaf
            FlatLightLaf.setup(); // Hoặc FlatDarkLaf.setup();

            // Tuỳ chỉnh giao diện (bo góc, font, màu, v.v.)
            UIManager.put("Component.arc", 12);
            UIManager.put("Button.arc", 20);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("Button.font", new Font("Segoe UI Emoji", Font.PLAIN, 14));
            UIManager.put("Label.font", new Font("Segoe UI Emoji", Font.PLAIN, 13));
            UIManager.put("Component.focusWidth", 1);
            UIManager.put("Button.innerFocusWidth", 0);
            UIManager.put("Button.margin", new Insets(5, 12, 5, 12));
            UIManager.put("defaultFont", new Font("Segoe UI Emoji", Font.PLAIN, 13));

        } catch (Exception ex) {
            System.err.println("Không thể khởi tạo FlatLaf: " + ex.getMessage());
        }

        // Khởi chạy giao diện chính
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
