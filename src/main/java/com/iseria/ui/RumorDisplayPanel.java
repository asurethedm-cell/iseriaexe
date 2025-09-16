package com.iseria.ui;

import com.iseria.domain.Rumor;
import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
/*
public class RumorDisplayPanel extends JPanel {

    public void displayRumors(List<Rumor> rumors) {
        removeAll();

        for (Rumor rumor : rumors) {
            JPanel rumorPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            // ✅ AFFICHAGE FORMAT TNCD CLASSIQUE
            // Type (avec couleur selon catégorie)
            JLabel typeLabel = new JLabel("[ " + rumor.getType() + " ]");
            typeLabel.setFont(new Font("Arial", Font.BOLD, 14));
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
            gbc.anchor = GridBagConstraints.WEST;
            rumorPanel.add(typeLabel, gbc);

            gbc.gridx++;
            gbc.anchor = GridBagConstraints.CENTER;
            rumorPanel.add(nameLabel, gbc);

            gbc.gridx++; gbc.gridwidth =4; gbc.gridheight = 2;
            rumorPanel.add(new JScrollPane(contentArea), gbc);

            gbc.gridx=7;
            rumorPanel.add(dateLabel, gbc);

            add(rumorPanel);
        }

        revalidate();
        repaint();
    }
*/
    // Méthodes helper manquantes


public class RumorDisplayPanel extends JPanel {
    private JPanel rumorContainer;

    public RumorDisplayPanel() {
        setLayout(new BorderLayout());

        // Container pour les rumeurs avec scroll
        rumorContainer = new JPanel();
        rumorContainer.setLayout(new BoxLayout(rumorContainer, BoxLayout.Y_AXIS));

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

        // Ajouter un espace flexible à la fin
        rumorContainer.add(Box.createVerticalGlue());

        rumorContainer.revalidate();
        rumorContainer.repaint();
    }

    private JPanel createRumorPanel(Rumor rumor) {
        JPanel rumorPanel = new JPanel(new BorderLayout());
        rumorPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Header : Type + Nom + Boutons Admin
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JLabel typeLabel = new JLabel("[ " + rumor.getType() + " ]");
        typeLabel.setForeground(getColorForType(rumor.getType()));
        typeLabel.setFont(new Font("Arial", Font.BOLD, 12));

        JLabel nameLabel = new JLabel(rumor.getName());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));

        headerPanel.add(typeLabel);
        headerPanel.add(nameLabel);

        // ✅ NOUVEAU : Bouton Éditer (seulement pour Admin)
        if ("Admin".equals(Login.currentUser)) {
            JButton editButton = new JButton("✏️ Éditer");
            editButton.setFont(new Font("Arial", Font.PLAIN, 10));
            editButton.setPreferredSize(new Dimension(80, 25));
            editButton.addActionListener(e -> showEditRumorDialog(rumor));
            headerPanel.add(Box.createHorizontalStrut(10));
            headerPanel.add(editButton);
        }

        // Content
        JTextArea contentArea = new JTextArea(rumor.getContent());
        contentArea.setWrapStyleWord(true);
        contentArea.setLineWrap(true);
        contentArea.setEditable(false);
        contentArea.setOpaque(false);

        // Footer : Date + Faction
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel dateLabel = new JLabel("📅 " + formatDate(rumor.getDate()));
        dateLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        dateLabel.setForeground(Color.GRAY);

        JLabel factionLabel = new JLabel("🏛️ " + rumor.getAuthorFactionId());
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

    // ✅ NOUVELLE MÉTHODE : Interface d'édition
    private void showEditRumorDialog(Rumor rumor) {
        // Cette méthode sera définie dans AdminRumorManagementPanel
        // ou accessible via une interface callback
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
