package com.iseria.ui;

import com.iseria.domain.DATABASE;
import com.iseria.domain.Faction;
import com.iseria.service.MoralDataService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.iseria.ui.UIHelpers.*;

public class MoralPanel {

    private static JPanel createMoralControlPanel(MoralDataService moralService, Faction faction, MoralPanelResult result) {
        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setOpaque(false);
        controlPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                "🎯 Sélection d'Actions (UNIQUE = Une seule autorisée)",
                0, 0,
                new Font("Arial", Font.BOLD, 12),
                Color.WHITE
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        try {
            result.availableActions = moralService.getAvailableActions(faction);
            String[] actionSlots = {"Action 1", "Action 2", "Action 3", "Action 4", "Action 5", };

            for (int i = 0; i < actionSlots.length; i++) {
                String slotName = actionSlots[i];

                gbc.gridx = 0; gbc.gridy = i;
                JLabel slotLabel = new JLabel(slotName + ":");
                slotLabel.setFont(new Font("Arial", Font.BOLD, 11));
                slotLabel.setForeground(Color.WHITE);
                controlPanel.add(slotLabel, gbc);

                JComboBox<DATABASE.MoralAction> actionCombo = createIntelligentMoralActionCombo(result);
                actionCombo.setPreferredSize(new Dimension(250, 25));

                setupComboBoxListeners(actionCombo, result);

                gbc.gridx = 1;
                controlPanel.add(actionCombo, gbc);

                result.dropdownMap.put(slotName, actionCombo);
            }

            gbc.gridx = 0; gbc.gridy = actionSlots.length;
            JLabel moralLabel = new JLabel("Moral Total:");
            moralLabel.setFont(new Font("Arial", Font.BOLD, 25));
            moralLabel.setForeground(Color.WHITE);
            controlPanel.add(moralLabel, gbc);

            gbc.gridx = 1;
            JLabel moralValueLabel = new JLabel("5.0");
            moralValueLabel.setFont(new Font("Arial", Font.BOLD, 25));
            moralValueLabel.setForeground(Color.GREEN);
            controlPanel.add(moralValueLabel, gbc);

            attachIntelligentMoralUpdater(moralValueLabel, result);

        } catch (Exception e) {
            System.err.println("Erreur lors de la création des contrôles moraux: " + e.getMessage());

            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            JLabel errorLabel = new JLabel("Erreur lors du chargement des actions morales");
            errorLabel.setForeground(Color.RED);
            controlPanel.add(errorLabel, gbc);
        }

        return controlPanel;
    }

    private static JButton createClickableMoralActionButton(DATABASE.MoralAction action) {
        JButton actionButton = new JButton("<html><center>" + action.getName() + "</center></html>");

        actionButton.setFont(new Font("Arial", Font.BOLD, 9));
        actionButton.setPreferredSize(new Dimension(110, 45));
        actionButton.setMinimumSize(new Dimension(110, 45));
        actionButton.setMaximumSize(new Dimension(110, 45));
        actionButton.setMargin(new Insets(3, 3, 3, 3));

        Color buttonColor;
        Color textColor = Color.WHITE;

        switch (action.getType()) {
            case UNIQUE -> {
                buttonColor = new Color(70, 130, 180); // Bleu
                actionButton.setToolTipText("Action Unique - Effet: " + action.getMoralEffect());
            }
            case AUTO -> {
                buttonColor = new Color(220, 20, 60); // Rouge
                actionButton.setToolTipText("Action Automatique - Effet: " + action.getMoralEffect());
            }
            default -> {
                buttonColor = new Color(128, 128, 128); // Gris
                actionButton.setToolTipText("Action Standard - Effet: " + action.getMoralEffect());
            }
        }

        // Effet positif/négatif sur la couleur
        if (action.getMoralEffect() > 0) {
            int r = Math.max(0, buttonColor.getRed() - 30);
            int g = Math.min(255, buttonColor.getGreen() + 50);
            int b = Math.max(0, buttonColor.getBlue() - 30);
            buttonColor = new Color(r, g, b);
        } else if (action.getMoralEffect() < 0) {
            int r = Math.min(255, buttonColor.getRed() + 50);
            int g = Math.max(0, buttonColor.getGreen() - 30);
            int b = Math.max(0, buttonColor.getBlue() - 30);
            buttonColor = new Color(r, g, b);
        }

        actionButton.setBackground(buttonColor);
        actionButton.setForeground(textColor);
        actionButton.setOpaque(true);
        actionButton.setBorderPainted(true);
        actionButton.setFocusPainted(false);

        Color originalColor = buttonColor;
        actionButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                actionButton.setBackground(brightenColor(originalColor, 30));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                actionButton.setBackground(originalColor);
            }
        });

        actionButton.addActionListener(e -> showMoralActionPopup(actionButton, action));

        return actionButton;
    }

    private static JPanel createClickableActionsGrid(MoralDataService moralService, Faction faction) {
        JPanel mainGridPanel = new JPanel(new BorderLayout());
        mainGridPanel.setOpaque(false);
        mainGridPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.CYAN, 1),
                "🎯 Actions Morales par Catégorie (Cliquez pour détails)",
                0, 0,
                new Font("Arial", Font.BOLD, 12),
                Color.CYAN
        ));

        try {
            java.util.List<DATABASE.MoralAction> availableActions = moralService.getAvailableActions(faction);

            if (availableActions == null || availableActions.isEmpty()) {
                JLabel noActionsLabel = new JLabel("Aucune action disponible", SwingConstants.CENTER);
                noActionsLabel.setForeground(Color.GRAY);
                mainGridPanel.add(noActionsLabel, BorderLayout.CENTER);
                return mainGridPanel;
            }

            Map<DATABASE.MoralAction.ActionType, java.util.List<DATABASE.MoralAction>> actionsByType =
                    availableActions.stream()
                            .collect(Collectors.groupingBy(DATABASE.MoralAction::getType));

            JPanel categorizedPanel = new JPanel();
            categorizedPanel.setLayout(new BoxLayout(categorizedPanel, BoxLayout.Y_AXIS));
            categorizedPanel.setOpaque(false);

            DATABASE.MoralAction.ActionType[] typeOrder = {
                    DATABASE.MoralAction.ActionType.UNIQUE,
                    DATABASE.MoralAction.ActionType.AUTO,
                    DATABASE.MoralAction.ActionType.REPETABLE
            };

            for (DATABASE.MoralAction.ActionType type : typeOrder) {
                java.util.List<DATABASE.MoralAction> actionsOfType = actionsByType.get(type);
                if (actionsOfType != null && !actionsOfType.isEmpty()) {
                    JPanel typeSection = createActionTypeSection(type, actionsOfType);
                    categorizedPanel.add(typeSection);
                    categorizedPanel.add(Box.createVerticalStrut(10));
                }
            }

            mainGridPanel.add(categorizedPanel, BorderLayout.CENTER);

        } catch (Exception e) {
            System.err.println("Erreur lors de la création de la grille d'actions: " + e.getMessage());
            e.printStackTrace();

            JLabel errorLabel = new JLabel("Erreur lors du chargement des actions", SwingConstants.CENTER);
            errorLabel.setForeground(Color.RED);
            mainGridPanel.add(errorLabel, BorderLayout.CENTER);
        }

        return mainGridPanel;
    }

    private static Color brightenColor(Color color, int amount) {
        int r = Math.min(255, color.getRed() + amount);
        int g = Math.min(255, color.getGreen() + amount);
        int b = Math.min(255, color.getBlue() + amount);
        return new Color(r, g, b);
    }

    private static JPanel createActionTypeSection(DATABASE.MoralAction.ActionType type, List<DATABASE.MoralAction> actions) {
        JPanel sectionPanel = new JPanel(new BorderLayout());
        sectionPanel.setOpaque(false);

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setOpaque(false);

        String typeInfo = getActionTypeInfo(type);
        JLabel typeLabel = new JLabel(typeInfo);
        typeLabel.setFont(new Font("Arial", Font.BOLD, 11));
        typeLabel.setForeground(getColorForActionType(type));
        headerPanel.add(typeLabel);

        int cols = Math.min(4, Math.max(2, actions.size()));
        int rows = (int) Math.ceil((double) actions.size() / cols);

        JPanel buttonGrid = new JPanel(new GridLayout(rows, cols, 5, 5));
        buttonGrid.setOpaque(false);
        buttonGrid.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

        actions.sort(Comparator.comparing(DATABASE.MoralAction::getName));

        for (DATABASE.MoralAction action : actions) {
            JButton actionButton = createClickableMoralActionButton(action);
            buttonGrid.add(actionButton);
        }

        int totalSlots = rows * cols;
        for (int i = actions.size(); i < totalSlots; i++) {
            JPanel emptyPanel = new JPanel();
            emptyPanel.setOpaque(false);
            buttonGrid.add(emptyPanel);
        }

        sectionPanel.add(headerPanel, BorderLayout.NORTH);
        sectionPanel.add(buttonGrid, BorderLayout.CENTER);

        return sectionPanel;
    }

    private static String getActionTypeInfo(DATABASE.MoralAction.ActionType type) {
        return switch (type) {
            case UNIQUE -> "🏆 Actions Uniques (Une seule sélectionnable)";
            case AUTO -> "⚡ Actions Automatiques";
            default -> "📋 Actions Répétables";
        };
    }

    private static Color getColorForActionType(DATABASE.MoralAction.ActionType type) {
        return switch (type) {
            case UNIQUE -> new Color(255, 215, 0); // Or
            case AUTO -> new Color(255, 69, 0);    // Rouge-orange
            default -> new Color(173, 216, 230);   // Bleu clair
        };
    }

    private static void showMoralActionPopup(Component parentComponent, DATABASE.MoralAction action) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='width: 300px; font-family: Arial; padding: 10px;'>");
        html.append("<h2 style='color: #4CAF50; margin-top: 0;'>").append(action.getName()).append("</h2>");

        String effectColor = action.getMoralEffect() >= 0 ? "#4CAF50" : "#F44336";
        String effectSign = action.getMoralEffect() >= 0 ? "+" : "";
        html.append("<p><strong>Effet Moral:</strong> <span style='color: ")
                .append(effectColor).append("; font-weight: bold;'>")
                .append(effectSign).append(action.getMoralEffect()).append("</span></p>");

        html.append("<p><strong>Type:</strong> ").append(action.getType().name()).append("</p>");

        try {
            int instabilityEffect = action.getInstabilityEffect();
            String instabilityColor = instabilityEffect >= 0 ? "#F44336" : "#4CAF50";
            String instabilitySign = instabilityEffect >= 0 ? "+" : "";
            html.append("<p><strong>Effet d'Instabilité:</strong> <span style='color: ")
                    .append(instabilityColor).append("; font-weight: bold;'>")
                    .append(instabilitySign).append(instabilityEffect).append("</span></p>");
        } catch (Exception e) {}
        try {
            String description = action.getDescription();
            String loreDescription =action.getLoreDescription();
            if (description != null && !description.trim().isEmpty()) {
                html.append("<hr><p><strong>Description:</strong></p>");
                html.append("<p style='font-style: italic; color:black;'>")
                        .append(description)
                        .append("</p>")
                        .append(loreDescription)
                        .append("</p>");
            }
        } catch (Exception e) {
            html.append("<hr><p><strong>Description:</strong></p>");
            html.append("<p style='font-style: italic; color: #666;'>");
            html.append("Cette action modifie le moral de votre faction de ");
            html.append(effectSign).append(action.getMoralEffect()).append(" points.");
            html.append("</p>");
        }

        html.append("<hr><p style='font-size: 11px; color: #888;'>");
        if (action.getMoralEffect() > 0) {
            html.append("💡 <em>Action bénéfique pour le moral de votre faction.</em>");
        } else if (action.getMoralEffect() < 0) {
            html.append("⚠️ <em>Action risquée - affecte négativement le moral.</em>");
        } else {
            html.append("ℹ️ <em>Action neutre - aucun effet direct sur le moral.</em>");
        }
        html.append("</p>");
        html.append("</body></html>");

        JDialog popup = new JDialog();
        popup.setTitle("Détails: " + action.getName());
        popup.setModal(true);
        popup.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JEditorPane editorPane = new JEditorPane("text/html", html.toString());
        editorPane.setEditable(false);
        editorPane.setOpaque(false);
        editorPane.setBackground(new Color(50, 50, 50));
        editorPane.setPreferredSize(new Dimension(500, 500));
        editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);

        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(500, 500));

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setOpaque(false);

        JButton closeButton = new JButton("Fermer");
        closeButton.addActionListener(e -> popup.dispose());
        buttonPanel.add(closeButton);

        popup.setLayout(new BorderLayout());
        popup.add(scrollPane, BorderLayout.CENTER);
        popup.add(buttonPanel, BorderLayout.SOUTH);
        popup.getContentPane().setBackground(new Color(50, 50, 50));

        popup.pack();
        popup.setLocationRelativeTo(parentComponent);
        popup.setVisible(true);
    }

    private static JScrollPane createScrollableActionsGrid(MoralDataService moralService, Faction faction) {
        JPanel gridPanel = createClickableActionsGrid(moralService, faction);

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);

        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        scrollPane.setPreferredSize(new Dimension(0, 150));
        scrollPane.setMinimumSize(new Dimension(0, 100));
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        scrollPane.setBackground(new Color(50, 50, 50, 200));

        styleScrollPane(scrollPane);
        configureScrollSpeed(scrollPane, 15, 60);

        return scrollPane;
    }

    private static JComboBox<DATABASE.MoralAction> createIntelligentMoralActionCombo(MoralPanelResult result) {
        JComboBox<DATABASE.MoralAction> combo = new JComboBox<>();
        combo.setRenderer(createEnhancedMoralActionRenderer());

        updateDropdownModel(combo, result);

        return combo;
    }

    public static DefaultListCellRenderer createEnhancedMoralActionRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value instanceof DATABASE.MoralAction action) {
                    String effectText = action.getMoralEffect() >= 0 ? "+" + action.getMoralEffect() : String.valueOf(action.getMoralEffect());
                    label.setText(" " + action.getName() + " (" + effectText + ")");
                    Color textColor = getColorForActionType(action.getType());
                    if (!isSelected) {
                        label.setForeground(textColor);
                    }
                    if (action.getType() == DATABASE.MoralAction.ActionType.UNIQUE) {
                        label.setFont(label.getFont().deriveFont(Font.BOLD));
                    }
                } else if (value == null) {
                    label.setText("🚫 Aucune action");
                    label.setForeground(Color.GRAY);
                }
                return label;
            }
        };
    }

    private static void updateDropdownModel(JComboBox<DATABASE.MoralAction> combo, MoralPanelResult result) {
        DATABASE.MoralAction currentSelection = (DATABASE.MoralAction) combo.getSelectedItem();

        ItemListener[] listeners = combo.getItemListeners();
        for (ItemListener listener : listeners) {
            combo.removeItemListener(listener);
        }
        try {
            combo.removeAllItems();
            combo.addItem(null);
            for (DATABASE.MoralAction action : result.availableActions) {
                boolean shouldAdd = false;
                switch (action.getType()) {
                    case UNIQUE:
                        shouldAdd = result.selectedUniqueActions.isEmpty() ||
                                result.selectedUniqueActions.contains(action) ||
                                action.equals(currentSelection);
                        break;
                    case AUTO:
                        break;
                    default:
                        shouldAdd = true;
                        break;
                }
                if (shouldAdd) {
                    combo.addItem(action);
                }
            }
            if (currentSelection != null && isActionAvailableInCombo(combo, currentSelection)) {
                combo.setSelectedItem(currentSelection);
            } else if (currentSelection != null && currentSelection.getType() == DATABASE.MoralAction.ActionType.UNIQUE) {
                combo.setSelectedItem(null);
            }

        } finally {
            for (ItemListener listener : listeners) {
                combo.addItemListener(listener);
            }
        }
    }
    private static boolean isActionAvailableInCombo(JComboBox<DATABASE.MoralAction> combo, DATABASE.MoralAction action) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (action.equals(combo.getItemAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static void setupComboBoxListeners(JComboBox<DATABASE.MoralAction> combo, MoralPanelResult result) {
        final boolean[] isUpdating = {false};
        combo.addItemListener(e -> {
            if (isUpdating[0]) {
                return;
            }
            try {
                isUpdating[0] = true;

                if (e.getStateChange() == ItemEvent.SELECTED) {
                    DATABASE.MoralAction selectedAction = (DATABASE.MoralAction) e.getItem();

                    if (selectedAction != null && selectedAction.getType() == DATABASE.MoralAction.ActionType.UNIQUE) {
                        if (result.selectedUniqueActions.contains(selectedAction)) {
                            SwingUtilities.invokeLater(() -> combo.setSelectedItem(null));
                            return;
                        }
                        result.addSelectedUniqueAction(selectedAction);
                    }
                } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                    DATABASE.MoralAction deselectedAction = (DATABASE.MoralAction) e.getItem();
                    if (deselectedAction != null && deselectedAction.getType() == DATABASE.MoralAction.ActionType.UNIQUE) {
                        result.removeSelectedUniqueAction(deselectedAction);
                    }
                }
            } finally {
                isUpdating[0] = false;
            }
        });
    }

    public static MoralPanelResult createModernMoralPanel(MoralDataService moralService, Faction currentUserFaction) {
        MoralPanelResult result = new MoralPanelResult();

        JPanel moralPanel = new JPanel(new BorderLayout());
        moralPanel.setOpaque(true);
        moralPanel.setBackground(new Color(50, 50, 50, 200));
        moralPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2),
                "📊 État du Moral - " + currentUserFaction.getDisplayName(),
                0, 0,
                new Font("Arial", Font.BOLD, 16),
                Color.WHITE
        ));

        JPanel headerPanel = createMoralHeaderPanel(currentUserFaction);
        headerPanel.setBackground(new Color(50, 50, 50, 200));
        JScrollPane actionsScrollPane = createScrollableActionsGrid(moralService, currentUserFaction);
        UIHelpers.styleScrollPane(actionsScrollPane);

        actionsScrollPane.setBackground(new Color(50, 50, 50, 200));
        JPanel controlPanel = createMoralControlPanel(moralService, currentUserFaction, result);
        controlPanel.setBackground(new Color(50, 50, 50, 200));
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(actionsScrollPane, BorderLayout.NORTH);
        centerPanel.setBackground(new Color(50, 50, 50, 200));

        moralPanel.add(headerPanel, BorderLayout.NORTH);
        moralPanel.add(centerPanel, BorderLayout.CENTER);
        moralPanel.add(controlPanel, BorderLayout.SOUTH);
        moralPanel.setBackground(new Color(50, 50, 50, 200));
        result.panel = moralPanel;

        return result;
    }

    public static class MoralPanelResult {
        public JPanel panel;
        public Map<String, JComboBox<DATABASE.MoralAction>> dropdownMap = new HashMap<>();

        private Set<DATABASE.MoralAction> selectedUniqueActions = new HashSet<>();
        private List<DATABASE.MoralAction> availableActions = new ArrayList<>();

        public void addSelectedUniqueAction(DATABASE.MoralAction action) {
            if (action.getType() == DATABASE.MoralAction.ActionType.UNIQUE) {
                selectedUniqueActions.add(action);
                updateDropdownAvailability();
            }
        }
        public void removeSelectedUniqueAction(DATABASE.MoralAction action) {
            if (action.getType() == DATABASE.MoralAction.ActionType.UNIQUE) {
                selectedUniqueActions.remove(action);
                updateDropdownAvailability();
            }
        }
        private void updateDropdownAvailability() {
            for (JComboBox<DATABASE.MoralAction> dropdown : dropdownMap.values()) {
                updateDropdownModel(dropdown, this);
            }
        }
    }

    private static JPanel createMoralHeaderPanel(Faction faction) {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setOpaque(false);

        JLabel factionLabel = new JLabel(faction.getDisplayName());
        factionLabel.setFont(new Font("Arial", Font.BOLD, 14));
        factionLabel.setForeground(Color.WHITE);
        headerPanel.add(factionLabel);

        return headerPanel;
    }
}
