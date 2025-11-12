package com.wordblock.client.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MatchHistoryFrame extends JFrame {
    private JTable table;
    private DefaultTableModel model;

    public MatchHistoryFrame(List<Map<String, Object>> matches) {
        super("📜 Match History");

        // ===== FlatLaf setup =====
        try {
            FlatLightLaf.setup();
        } catch (Exception e) {
            System.err.println("Failed to init FlatLaf: " + e.getMessage());
        }

        setSize(720, 420);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initUI(matches);
    }

    /** Initialize UI components */
    private void initUI(List<Map<String, Object>> matches) {
        Font emojiFont = new Font("Segoe UI Emoji", Font.PLAIN, 13);

        // ===== Title =====
        JLabel lblTitle = new JLabel("📜 Match History", JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI Emoji", Font.BOLD, 18));
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "foreground:#0056B3");

        // ===== Table setup =====
        String[] columns = {"Player 1", "Player 2", "Score 1", "Score 2", "Result", "Time"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(30);
        table.setFont(emojiFont);
        table.putClientProperty(FlatClientProperties.STYLE,
                "showHorizontalLines:true; showVerticalLines:true; intercellSpacing:1,1");
        
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        table.getColumnModel().getColumn(5).setPreferredWidth(140);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        header.setBackground(new Color(245, 247, 250));
        header.setForeground(new Color(50, 50, 50));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // ===== Close button =====
        JButton btnClose = new JButton("✖ Close");
        btnClose.setFont(emojiFont);
        btnClose.putClientProperty(FlatClientProperties.STYLE,
                "background:#E81123; foreground:#FFFFFF; focusWidth:0;");
        btnClose.addActionListener(e -> dispose());

        JPanel bottom = new JPanel();
        bottom.add(btnClose);

        // ===== Layout =====
        setLayout(new BorderLayout(10, 10));
        add(lblTitle, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        updateTable(matches);
    }

    /** Fill table with match data */
    private void updateTable(List<Map<String, Object>> matches) {
        model.setRowCount(0);
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        if (matches == null || matches.isEmpty()) {
            model.addRow(new Object[]{"(No matches yet)", "", "", "", "", ""});
            return;
        }

        for (Map<String, Object> m : matches) {
            String player1 = (String) m.getOrDefault("player1", "?");
            String player2 = (String) m.getOrDefault("player2", "?");

            int score1 = toInt(m.get("score1"));
            int score2 = toInt(m.get("score2"));

            String result;
            if (score1 > score2) result = player1 + " 🏆";
            else if (score1 < score2) result = player2 + " 🏆";
            else result = "🤝 Draw";

            // ===== Time formatting =====
            String timeDisplay = formatTime(m.get("started_at"), fmt);

            model.addRow(new Object[]{player1, player2, score1, score2, result, timeDisplay});
        }
    }

    /** Convert time object to formatted string */
    private String formatTime(Object timeObj, SimpleDateFormat fmt) {
        if (timeObj == null) return "-";
        try {
            if (timeObj instanceof Number num) {
                long millis = num.longValue();
                // nếu timestamp là giây (Unix time), nhân 1000
                if (millis < 100000000000L) millis *= 1000;
                return fmt.format(new Date(millis));
            }
            if (timeObj instanceof String s) {
                // nếu là string ISO-like
                return s.length() >= 19 ? s.substring(0, 19).replace('T', ' ') : s;
            }
        } catch (Exception ignored) {}
        return "-";
    }

    /** Safely convert object to int */
    private int toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return 0;
    }
}
