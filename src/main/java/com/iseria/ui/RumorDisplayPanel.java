package com.iseria.ui;

import com.iseria.domain.Rumor;
import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RumorDisplayPanel extends JPanel {

    public void displayRumors(List<Rumor> rumors) {
        removeAll();

        for (Rumor rumor : rumors) {
            JPanel rumorPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            // ✅ AFFICHAGE FORMAT TNCD CLASSIQUE
            // Type (avec couleur selon catégorie)
            JLabel typeLabel = new JLabel("[" + rumor.getType() + "]");
            typeLabel.setForeground(getColorForType(rumor.getType()));

            // Name (titre en gras)
            JLabel nameLabel = new JLabel(rumor.getName());
            nameLabel.setFont(new Font("Arial", Font.BOLD, 14));

            // Content (texte principal)
            JTextArea contentArea = new JTextArea(rumor.getContent());
            contentArea.setWrapStyleWord(true);
            contentArea.setLineWrap(true);
            contentArea.setEditable(false);

            // Date (coin bas droit)
            JLabel dateLabel = new JLabel(formatDate(rumor.getDate()));
            dateLabel.setFont(new Font("Arial", Font.ITALIC, 10));
            dateLabel.setForeground(Color.GRAY);

            // Layout classique TNCD
            gbc.gridx = 0; gbc.gridy = 0;
            rumorPanel.add(typeLabel, gbc);

            gbc.gridx = 1; gbc.weightx = 1.0;
            rumorPanel.add(nameLabel, gbc);

            gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
            rumorPanel.add(new JScrollPane(contentArea), gbc);

            gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
            rumorPanel.add(dateLabel, gbc);

            add(rumorPanel);
        }

        revalidate();
        repaint();
    }

    // Méthodes helper manquantes
    private Color getColorForType(String type) {
        return switch (type.toLowerCase()) {
            case "politique" -> Color.RED;
            case "économie" -> Color.GREEN;
            case "militaire" -> Color.BLUE;
            case "social" -> Color.ORANGE;
            default -> Color.BLACK;
        };
    }

    private String formatDate(java.time.LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}
