package com.iseria.ui;

import com.iseria.domain.Rumor;
import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;



public class RumorDisplayPanel extends JPanel {
    private JPanel rumorContainer;

    public RumorDisplayPanel() {
        setLayout(new BorderLayout());
        rumorContainer = new JPanel();
        rumorContainer.setLayout(new BoxLayout(rumorContainer, BoxLayout.Y_AXIS));
        rumorContainer.setBackground(new Color(50, 50, 50, 250));

        JScrollPane scrollPane = new JScrollPane(rumorContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);
    }

    public void displayRumors(List<Rumor> rumors) {
        rumorContainer.removeAll();

        for (Rumor rumor : rumors) {
            JPanel rumorPanel = createRumorPanel(rumor);

            rumorPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, rumorPanel.getPreferredSize().height));

            rumorContainer.add(rumorPanel);
            rumorContainer.add(Box.createVerticalStrut(10)); // Espacement
        }
        rumorContainer.add(Box.createVerticalGlue());
        rumorContainer.revalidate();
        rumorContainer.repaint();
    }

    private JPanel createRumorPanel(Rumor rumor) {
        JPanel rumorPanel = new JPanel(new BorderLayout());
        rumorPanel.setBackground(new Color(50, 50, 50, 150));
        rumorPanel.setOpaque(false);
        rumorPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setOpaque(false);
        JLabel typeLabel = new JLabel("[ " + rumor.getType() + " ]");
        typeLabel.setOpaque(false);
        typeLabel.setForeground(getColorForType(rumor.getType()));
        typeLabel.setFont(new Font("Arial", Font.BOLD, 12));

        JLabel nameLabel = new JLabel(rumor.getName());
        nameLabel.setOpaque(false);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));

        headerPanel.add(typeLabel);
        headerPanel.add(nameLabel);

        JTextArea contentArea = new JTextArea(rumor.getContent());
        contentArea.setWrapStyleWord(true);
        contentArea.setLineWrap(true);
        contentArea.setEditable(false);
        contentArea.setOpaque(false);

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        footerPanel.setBackground(new Color(50, 50, 50, 150));
        footerPanel.setOpaque(false);
        JLabel dateLabel = new JLabel(formatDate(rumor.getDate()));
        dateLabel.setOpaque(false);
        dateLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        dateLabel.setForeground(Color.GRAY);

        JLabel factionLabel = new JLabel(" " + rumor.getAuthorFactionId());
        factionLabel.setOpaque(false);
        factionLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        factionLabel.setForeground(Color.BLUE);

        footerPanel.add(dateLabel);
        footerPanel.add(Box.createHorizontalStrut(20));
        footerPanel.add(factionLabel);

        rumorPanel.add(headerPanel, BorderLayout.NORTH);
        rumorPanel.add(contentArea, BorderLayout.CENTER);
        rumorPanel.add(footerPanel, BorderLayout.SOUTH);

        return rumorPanel;
    }

    private void showEditRumorDialog(Rumor rumor) {
        if (editCallback != null) {
            editCallback.editRumor(rumor);
        }
    }
    private Color getColorForType(String type) {
        return switch (type.toLowerCase()) {
            case "reçue" -> Color.RED;
            case "envoyé" -> Color.GREEN;
            case "militaire" -> Color.BLUE;
            case "social" -> Color.ORANGE;
            default -> Color.BLACK;
        };
    }

    private String formatDate(java.time.LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public interface EditRumorCallback {
        void editRumor(Rumor rumor);
    }

    private EditRumorCallback editCallback;

    public void setEditCallback(EditRumorCallback callback) {
        this.editCallback = callback;
    }
}
