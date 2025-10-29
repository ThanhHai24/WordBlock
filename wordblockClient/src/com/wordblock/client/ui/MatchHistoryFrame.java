package com.wordblock.client.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class MatchHistoryFrame extends JFrame {
    private JTable table;
    private DefaultTableModel model;

    public MatchHistoryFrame(List<Map<String, Object>> matches) {
        setTitle("Match History");
        setSize(700, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initUI(matches);
    }

    /** Initialize UI components */
    private void initUI(List<Map<String, Object>> matches) {
        String[] columns = {"Player 1", "Player 2", "Score 1", "Score 2", "Result", "Started", "Ended"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(28);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(scrollPane, BorderLayout.CENTER);
        updateTable(matches);
    }

    /** Fill table with match data */
    private void updateTable(List<Map<String, Object>> matches) {
        model.setRowCount(0);

        if (matches == null || matches.isEmpty()) {
            model.addRow(new Object[]{"(No matches)", "", "", "", "", "", ""});
            return;
        }

        for (Map<String, Object> m : matches) {
            String player1 = (String) m.get("player1");
            String player2 = (String) m.get("player2");

            int score1 = toInt(m.get("score1"));
            int score2 = toInt(m.get("score2"));

            String result;
            if (score1 > score2) result = player1 + " 🏆";
            else if (score1 < score2) result = player2 + " 🏆";
            else result = "🤝 Draw";

            Object started = m.getOrDefault("started_at", "-");
            Object ended = m.getOrDefault("ended_at", "-");

            model.addRow(new Object[]{player1, player2, score1, score2, result, started, ended});
        }
    }

    /** Safely convert object to int */
    private int toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }
}
