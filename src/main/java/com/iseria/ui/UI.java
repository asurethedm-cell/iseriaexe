package com.iseria.ui;

import com.iseria.domain.*;
import com.iseria.domain.DATABASE;
import com.iseria.service.*;


import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


import static com.iseria.ui.MainMenu.*;
import static com.iseria.ui.UIHelpers.*;


public class UI {

    private static EconomicDataService economicService;

    private static MoralSaveService moralSaveService = new MoralSaveService();

    public static JPanel createGeneralInfoPanel(JTextArea myNoteArea) {
        JPanel gIP = new JPanel(new GridBagLayout());
        gIP.setOpaque(false);
        gIP.setBackground(Color.orange);


        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.PAGE_START;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 0, 5, 5);

        JLabel prochainTour = new JLabel("Prochain Tour :");
        prochainTour.setFont(new Font("Bahnschrift", Font.BOLD, 15));
        prochainTour.setForeground(Color.GREEN);
        gIP.add(prochainTour, gbc);

        gbc.gridx = 1;
        JLabel prochainTourDATA = new JLabel("SoonTM");
        prochainTourDATA.setFont(new Font("Bahnschrift", Font.BOLD, 15));
        prochainTourDATA.setForeground(Color.GREEN);
        gIP.add(prochainTourDATA, gbc);

        gbc.insets = new Insets(5, 0, 5, 5);
        gbc.gridx = 1;
        gbc.gridy = 2;
        JLabel mesNotes = new JLabel("Mes Notes : ");
        mesNotes.setFont(new Font("Bahnschrift", Font.BOLD, 15));
        mesNotes.setHorizontalAlignment(SwingConstants.CENTER);
        mesNotes.setForeground(Color.BLACK);
        gIP.add(mesNotes, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridheight = gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(5, 5, 5, 0);

        myNoteArea.setLineWrap(true);
        myNoteArea.setWrapStyleWord(true);
        myNoteArea.setOpaque(true);
        myNoteArea.setBackground(new Color(211, 211, 211, 128));

        JScrollPane scroll = new JScrollPane(myNoteArea);
        scroll.setBorder(BorderFactory.createLineBorder(Color.BLACK, 5));
        scroll.setOpaque(false);


        gIP.add(scroll, gbc);

        return gIP;
    }

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

            // Créer des dropdowns pour différents slots d'actions
            String[] actionSlots = {"Action 1", "Action 2", "Action 3", "Action 4", "Action 5", };

            for (int i = 0; i < actionSlots.length; i++) {
                String slotName = actionSlots[i];

                // Label de slot
                gbc.gridx = 0; gbc.gridy = i;
                JLabel slotLabel = new JLabel(slotName + ":");
                slotLabel.setFont(new Font("Arial", Font.BOLD, 11));
                slotLabel.setForeground(Color.WHITE);
                controlPanel.add(slotLabel, gbc);

                // ComboBox avec modèle intelligent
                JComboBox<DATABASE.MoralAction> actionCombo = createIntelligentMoralActionCombo(result);
                actionCombo.setPreferredSize(new Dimension(250, 25));

                // Ajouter les listeners pour gérer les règles UNIQUE
                setupComboBoxListeners(actionCombo, result);

                gbc.gridx = 1;
                controlPanel.add(actionCombo, gbc);

                result.dropdownMap.put(slotName, actionCombo);
            }

            // Affichage du moral total avec mise à jour en temps réel
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

            // Connecter le système de mise à jour
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
    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(100, 100, 100);
                this.trackColor = new Color(50, 50, 50);
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                JButton button = super.createDecreaseButton(orientation);
                button.setBackground(new Color(70, 70, 70));
                button.setBorder(null);
                return button;
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                JButton button = super.createIncreaseButton(orientation);
                button.setBackground(new Color(70, 70, 70));
                button.setBorder(null);
                return button;
            }
        });
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setBlockIncrement(64);
    }
    public static void configureScrollSpeed(JScrollPane scrollPane, int unitIncrement, int blockIncrement) {
        scrollPane.getVerticalScrollBar().setUnitIncrement(unitIncrement);
        scrollPane.getVerticalScrollBar().setBlockIncrement(blockIncrement);

        // Amélioration du scroll avec la molette de souris
        scrollPane.addMouseWheelListener(e -> {
            JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
            int currentValue = scrollBar.getValue();
            int scrollAmount = e.getScrollAmount() * unitIncrement;

            if (e.getWheelRotation() < 0) {
                // Scroll vers le haut
                scrollBar.setValue(currentValue - scrollAmount);
            } else {
                // Scroll vers le bas
                scrollBar.setValue(currentValue + scrollAmount);
            }
        });
    }
    private static JButton createClickableMoralActionButton(DATABASE.MoralAction action) {
        // Créer le bouton avec le nom de l'action
        JButton actionButton = new JButton("<html><center>" + action.getName() + "</center></html>");

        // Style du bouton - **MODIFIÉ** pour être plus compact
        actionButton.setFont(new Font("Arial", Font.BOLD, 9));
        actionButton.setPreferredSize(new Dimension(110, 45));
        actionButton.setMinimumSize(new Dimension(110, 45));
        actionButton.setMaximumSize(new Dimension(110, 45));
        actionButton.setMargin(new Insets(3, 3, 3, 3));

        // Reste du code couleur inchangé...
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

        // Effets hover
        Color originalColor = buttonColor;
        actionButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                actionButton.setBackground(brightenColor(originalColor, 30));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                actionButton.setBackground(originalColor);
            }
        });

        // Action de clic pour afficher la popup
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
            List<DATABASE.MoralAction> availableActions = moralService.getAvailableActions(faction);

            if (availableActions == null || availableActions.isEmpty()) {
                JLabel noActionsLabel = new JLabel("Aucune action disponible", SwingConstants.CENTER);
                noActionsLabel.setForeground(Color.GRAY);
                mainGridPanel.add(noActionsLabel, BorderLayout.CENTER);
                return mainGridPanel;
            }

            // **NOUVEAU** - Trier les actions par type
            Map<DATABASE.MoralAction.ActionType, List<DATABASE.MoralAction>> actionsByType =
                    availableActions.stream()
                            .collect(Collectors.groupingBy(DATABASE.MoralAction::getType));

            JPanel categorizedPanel = new JPanel();
            categorizedPanel.setLayout(new BoxLayout(categorizedPanel, BoxLayout.Y_AXIS));
            categorizedPanel.setOpaque(false);

            // Ordre de priorité des types
            DATABASE.MoralAction.ActionType[] typeOrder = {
                    DATABASE.MoralAction.ActionType.UNIQUE,
                    DATABASE.MoralAction.ActionType.AUTO,
                    DATABASE.MoralAction.ActionType.REPETABLE
            };

            for (DATABASE.MoralAction.ActionType type : typeOrder) {
                List<DATABASE.MoralAction> actionsOfType = actionsByType.get(type);
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

        // Header avec le nom du type et description
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setOpaque(false);

        String typeInfo = getActionTypeInfo(type);
        JLabel typeLabel = new JLabel(typeInfo);
        typeLabel.setFont(new Font("Arial", Font.BOLD, 11));
        typeLabel.setForeground(getColorForActionType(type));
        headerPanel.add(typeLabel);

        // Grille des boutons pour ce type
        int cols = Math.min(4, Math.max(2, actions.size()));
        int rows = (int) Math.ceil((double) actions.size() / cols);

        JPanel buttonGrid = new JPanel(new GridLayout(rows, cols, 5, 5));
        buttonGrid.setOpaque(false);
        buttonGrid.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

        // Trier les actions par nom pour une présentation cohérente
        actions.sort(Comparator.comparing(DATABASE.MoralAction::getName));

        for (DATABASE.MoralAction action : actions) {
            JButton actionButton = createClickableMoralActionButton(action);
            buttonGrid.add(actionButton);
        }

        // Remplir les cases vides si nécessaire
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
        // Créer le contenu HTML formaté pour la popup
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='width: 300px; font-family: Arial; padding: 10px;'>");
        html.append("<h2 style='color: #4CAF50; margin-top: 0;'>").append(action.getName()).append("</h2>");

        // Effet moral
        String effectColor = action.getMoralEffect() >= 0 ? "#4CAF50" : "#F44336";
        String effectSign = action.getMoralEffect() >= 0 ? "+" : "";
        html.append("<p><strong>Effet Moral:</strong> <span style='color: ")
                .append(effectColor).append("; font-weight: bold;'>")
                .append(effectSign).append(action.getMoralEffect()).append("</span></p>");

        // Type d'action
        html.append("<p><strong>Type:</strong> ").append(action.getType().name()).append("</p>");

        // Effet d'instabilité si disponible
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
            if (description != null && !description.trim().isEmpty()) {
                html.append("<hr><p><strong>Description:</strong></p>");
                html.append("<p style='font-style: italic; color: #666;'>").append(description).append("</p>");
            }
        } catch (Exception e) {
            // Si getDescription() n'existe pas, créer une description basique
            html.append("<hr><p><strong>Description:</strong></p>");
            html.append("<p style='font-style: italic; color: #666;'>");
            html.append("Cette action modifie le moral de votre faction de ");
            html.append(effectSign).append(action.getMoralEffect()).append(" points.");
            html.append("</p>");
        }

        // Conseils d'utilisation
        html.append("<hr><p style='font-size: 11px; color: #888;'>");
        if (action.getMoralEffect() > 0) {
            html.append("💡 <em>Action bénéfique pour le moral de votre faction.</em>");
        } else if (action.getMoralEffect() < 0) {
            html.append("⚠️ <em>Action risquée - peut affecter négativement le moral.</em>");
        } else {
            html.append("ℹ️ <em>Action neutre - aucun effet direct sur le moral.</em>");
        }
        html.append("</p>");

        html.append("</body></html>");

        // Créer et afficher le dialog
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
        // Créer la grille normale
        JPanel gridPanel = createClickableActionsGrid(moralService, faction);

        // Envelopper dans un JScrollPane
        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);

        // Configuration du scroll
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Hauteur préférée pour montrer environ 2-3 lignes de boutons
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

        // Initialiser avec le modèle filtré
        updateDropdownModel(combo, result);

        return combo;
    }
    public static DefaultListCellRenderer createEnhancedMoralActionRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value instanceof DATABASE.MoralAction) {
                    DATABASE.MoralAction action = (DATABASE.MoralAction) value;

                    // Texte avec effet et type
                    String effectText = action.getMoralEffect() >= 0 ? "+" + action.getMoralEffect() : String.valueOf(action.getMoralEffect());

                    label.setText(" " + action.getName() + " (" + effectText + ")");

                    // Couleur selon le type
                    Color textColor = getColorForActionType(action.getType());
                    if (!isSelected) {
                        label.setForeground(textColor);
                    }

                    // Style spécial pour les actions UNIQUE
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
        // **NOUVEAU** - Sauvegarder l'état actuel pour éviter les événements en cascade
        DATABASE.MoralAction currentSelection = (DATABASE.MoralAction) combo.getSelectedItem();

        // Temporairement retirer tous les listeners
        ItemListener[] listeners = combo.getItemListeners();
        for (ItemListener listener : listeners) {
            combo.removeItemListener(listener);
        }

        try {
            combo.removeAllItems();
            combo.addItem(null); // Option "Aucune action"

            for (DATABASE.MoralAction action : result.availableActions) {
                boolean shouldAdd = false;

                switch (action.getType()) {
                    case UNIQUE:
                        shouldAdd = result.selectedUniqueActions.isEmpty() ||
                                result.selectedUniqueActions.contains(action) ||
                                action.equals(currentSelection);
                        break;

                    case AUTO:
                        shouldAdd = false;
                        break;

                    default: // REPETABLE et autres
                        shouldAdd = true;
                        break;
                }

                if (shouldAdd) {
                    combo.addItem(action);
                }
            }

            // Restaurer la sélection si elle est toujours valide
            if (currentSelection != null && isActionAvailableInCombo(combo, currentSelection)) {
                combo.setSelectedItem(currentSelection);
            } else if (currentSelection != null && currentSelection.getType() == DATABASE.MoralAction.ActionType.UNIQUE) {
                // L'action UNIQUE n'est plus disponible - la désélectionner
                combo.setSelectedItem(null);
            }

        } finally {
            // **IMPORTANT** - Restaurer tous les listeners
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
        // **NOUVEAU** - Flag pour éviter les boucles infinies
        final boolean[] isUpdating = {false};

        combo.addItemListener(e -> {
            // **CORRECTION** - Ignorer les événements pendant les mises à jour programmatiques
            if (isUpdating[0]) {
                return;
            }

            try {
                isUpdating[0] = true;

                if (e.getStateChange() == ItemEvent.SELECTED) {
                    DATABASE.MoralAction selectedAction = (DATABASE.MoralAction) e.getItem();

                    if (selectedAction != null && selectedAction.getType() == DATABASE.MoralAction.ActionType.UNIQUE) {
                        // Vérifier si cette action UNIQUE est déjà sélectionnée ailleurs
                        if (result.selectedUniqueActions.contains(selectedAction)) {
                            // Déjà sélectionnée - désélectionner dans ce combo
                            SwingUtilities.invokeLater(() -> {
                                combo.setSelectedItem(null);
                            });
                            return;
                        }

                        // Ajouter à la liste des actions UNIQUE sélectionnées
                        result.addSelectedUniqueAction(selectedAction);
                    }
                } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                    DATABASE.MoralAction deselectedAction = (DATABASE.MoralAction) e.getItem();

                    if (deselectedAction != null && deselectedAction.getType() == DATABASE.MoralAction.ActionType.UNIQUE) {
                        // Retirer de la liste des actions UNIQUE sélectionnées
                        result.removeSelectedUniqueAction(deselectedAction);
                    }
                }
            } finally {
                isUpdating[0] = false;
            }
        });
    }





    private static String formatMoralContentForDisplay(String text) {
        if (text == null || text.isEmpty()) {
            return "<div class='section'><p>← Les effets du moral s'afficheront ici</p></div>";
        }

        StringBuilder html = new StringBuilder();
        String[] lines = text.split("\n");

        boolean inSection = false;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.contains(":") && !line.startsWith("*")) {
                if (inSection) {
                    html.append("</div>");
                }
                html.append("<div class='section'>");
                html.append("<h3>").append(line).append("</h3>");
                inSection = true;
            } else {
                String cssClass = "";
                if (line.contains("+")) {
                    cssClass = " class='positive'";
                } else if (line.contains("-")) {
                    cssClass = " class='negative'";
                }

                html.append("<p").append(cssClass).append(">")
                        .append(line.replace("*", "•"))
                        .append("</p>");
            }
        }

        if (inSection) {
            html.append("</div>");
        }

        return html.toString();
    }
    public static MoralPanelResult createModernMoralPanel(MoralDataService moralService, Faction currentUserFaction, String currentUser) {
        MoralPanelResult result = new MoralPanelResult();

        // Panneau principal avec style moderne
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

        // Header avec informations de faction
        JPanel headerPanel = createMoralHeaderPanel(currentUserFaction);
        headerPanel.setBackground(new Color(50, 50, 50, 200));
        // **MODIFIÉ** - Grille d'actions avec ScrollPane
        JScrollPane actionsScrollPane = createScrollableActionsGrid(moralService, currentUserFaction);
        UI.styleScrollPane(actionsScrollPane);

        actionsScrollPane.setBackground(new Color(50, 50, 50, 200));
        // Panneau de contrôles des sélecteurs (en bas)
        JPanel controlPanel = createMoralControlPanel(moralService, currentUserFaction, result);
        controlPanel.setBackground(new Color(50, 50, 50, 200));
        // Assembly du panneau avec la nouvelle grille scrollable
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(actionsScrollPane, BorderLayout.NORTH);
        centerPanel.setBackground(new Color(50, 50, 50, 200));

        moralPanel.add(headerPanel, BorderLayout.NORTH);
        moralPanel.add(centerPanel, BorderLayout.CENTER);
        moralPanel.add(controlPanel, BorderLayout.SOUTH);
        moralPanel.setBackground(new Color(50, 50, 50, 200));
        // Configuration du résultat
        result.panel = moralPanel;

        return result;
    }
    public static class MoralPanelResult {
        public JPanel panel;
        public JTextPane contentPane;
        public Map<String, JComboBox<DATABASE.MoralAction>> dropdownMap = new HashMap<>();
        public JButton refreshButton;

        // **NOUVEAU** - Suivi des sélections pour les règles
        private Set<DATABASE.MoralAction> selectedUniqueActions = new HashSet<>();
        private List<DATABASE.MoralAction> availableActions = new ArrayList<>();

        public void updateMoralDisplay(String moralText) {
            if (contentPane != null) {
                String formattedContent = formatMoralContentForDisplay(moralText);
                contentPane.setText(formattedContent);
            }
        }

        // **NOUVEAU** - Méthodes de gestion des sélections
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

        public boolean canSelectUniqueAction(DATABASE.MoralAction action) {
            return action.getType() != DATABASE.MoralAction.ActionType.UNIQUE ||
                    selectedUniqueActions.isEmpty() ||
                    selectedUniqueActions.contains(action);
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

    public static class WorkerSelectionDialog extends JDialog {
        private JSpinner workerSpinner;
        private int selectedCount = 0;
        private boolean confirmed = false;
        private final String buildingType;
        private final String buildingName;

        public WorkerSelectionDialog(JFrame parent, String buildingType, String buildingName, int currentWorkers) {
            super(parent, "Affecter du personnel", true);
            this.buildingType = buildingType;
            this.buildingName = buildingName;
            initializeDialog(currentWorkers);
        }

        private void initializeDialog(int currentWorkers) {
            setSize(400, 200);
            setLocationRelativeTo(getParent());
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            JPanel mainPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            // Title
            JLabel titleLabel = new JLabel("Personnel pour " + buildingName);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            gbc.insets = new Insets(10, 10, 20, 10);
            mainPanel.add(titleLabel, gbc);

            // Current workers display
            JLabel currentLabel = new JLabel("Personnel actuel: " + currentWorkers);
            gbc.gridy = 1; gbc.gridwidth = 1;
            gbc.insets = new Insets(5, 10, 5, 10);
            mainPanel.add(currentLabel, gbc);

            // Worker spinner
            JLabel spinnerLabel = new JLabel("Nouveau personnel:");
            gbc.gridx = 0; gbc.gridy = 2;
            mainPanel.add(spinnerLabel, gbc);

            workerSpinner = new JSpinner(new SpinnerNumberModel(currentWorkers, 0, 50, 1));
            workerSpinner.setPreferredSize(new Dimension(100, 30));
            gbc.gridx = 1;
            mainPanel.add(workerSpinner, gbc);

            // Buttons
            JPanel buttonPanel = new JPanel(new FlowLayout());
            JButton confirmButton = new JButton("Confirmer");
            JButton cancelButton = new JButton("Annuler");

            confirmButton.addActionListener(e -> {
                selectedCount = (Integer) workerSpinner.getValue();
                confirmed = true;
                dispose();
            });

            cancelButton.addActionListener(e -> {
                confirmed = false;
                dispose();
            });

            buttonPanel.add(confirmButton);
            buttonPanel.add(cancelButton);

            gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
            gbc.insets = new Insets(20, 10, 10, 10);
            mainPanel.add(buttonPanel, gbc);

            add(mainPanel);
        }

        public int getSelectedCount() { return selectedCount; }
        public boolean isConfirmed() { return confirmed; }
    }

    public static class HexSnapshotCache {
        private static final Map<String, BufferedImage> hexImageCache = new ConcurrentHashMap<>();
        public static BufferedImage mapBackground; // Image de fond de la carte

        static {
            // Initialiser l'image de fond au démarrage
            try {
                mapBackground = ImageIO.read(UI.class.getResource("/Iseria.png"));
            } catch (IOException e) {
                System.err.println("Erreur chargement carte: " + e.getMessage());
            }
        }

        /**
         * Retourne l'image hexagonale pour la clé donnée avec cache
         */
        public static BufferedImage getHexSnapshot(String hexKey, IHexRepository repo) {
            return hexImageCache.computeIfAbsent(hexKey, key -> generateHexSnapshot(key, repo));
        }

        private static BufferedImage generateHexSnapshot(String hexKey, IHexRepository repo) {
            if (mapBackground == null) return null;

            // 1. Récupérer les coordonnées pixel de l'hexagone
            Rectangle bounds = getHexPixelBounds(hexKey, repo);

            // 2. Extraire la zone rectangulaire
            BufferedImage square = extractSquareRegion(bounds);

            // 3. Appliquer le masque hexagonal
            return createHexagonalSnapshot(square);
        }

        private static Rectangle getHexPixelBounds(String hexKey, IHexRepository repo) {
            int[] pos = repo.getHexPosition(hexKey);
            int size = 100; // Taille souhaitée pour le snapshot

            // Centrer le rectangle sur la position de l'hex
            return new Rectangle(
                    pos[0] - size/2,
                    pos[1] - size/2,
                    size,
                    size
            );
        }

        private static BufferedImage extractSquareRegion(Rectangle bounds) {
            int x = Math.max(0, Math.min(bounds.x, mapBackground.getWidth() - bounds.width));
            int y = Math.max(0, Math.min(bounds.y, mapBackground.getHeight() - bounds.height));
            int w = Math.min(bounds.width, mapBackground.getWidth() - x);
            int h = Math.min(bounds.height, mapBackground.getHeight() - y);

            return mapBackground.getSubimage(x, y, w, h);
        }

        private static BufferedImage createHexagonalSnapshot(BufferedImage square) {
            int size = square.getWidth();
            BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = result.createGraphics();
                Path2D hexPath = createHexagonPath(size);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setClip(hexPath);
            g2d.drawImage(square, 0, 0, null);

            g2d.dispose();
            return result;
        }

        private static Path2D createHexagonPath(int size) {
            Path2D hexagon = new Path2D.Double();
            double radius = size / 2.0;
            double centerX = size / 2.0;
            double centerY = size / 2.0;

            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(60 * i);
                double x = centerX + radius * Math.cos(angle);
                double y = centerY + radius * Math.sin(angle);

                if (i == 0) {
                    hexagon.moveTo(x, y);
                } else {
                    hexagon.lineTo(x, y);
                }
            }
            hexagon.closePath();
            return hexagon;
        }

        public static void clearCache() {
            hexImageCache.clear();
        }
    }

    // Enhanced Economic Panel with instability from saved state
    public static class EnhancedEconomicPanel extends JPanel
            implements EconomicDataService.EconomicDataObserver {

        private EconomicDataService economicService;
        private JLabel tresorerieLabel, populationLabel, instabiliteLabel, agressiviteLabel, faimLabel;
        private JPanel resourcePanel, salaryPanel;
        private IHexRepository hexRepository;
        private LogisticsPanel logisticsPanel;


        public EnhancedEconomicPanel(EconomicDataService economicService, IHexRepository hexRepository) {
            this.economicService = economicService;
            this.hexRepository = hexRepository;
            economicService.addObserver(this);
            initializePanel();
            loadInstabilityFromSavedState();
        }
        private void loadInstabilityFromSavedState() {
            try {
                MoralSaveService.MoralSaveData savedData = moralSaveService.loadMoralSelections(
                        Login.currentUser, currentUserFaction);
                if (savedData.calculatedInstability > 0) {
                    economicService.updateInstabilityFromMoral(savedData.calculatedInstability);
                }
            } catch (Exception e) {
                System.err.println("Could not load saved instability: " + e.getMessage());
            }
        }
        private void initializePanel() {
            setLayout(new GridBagLayout());
            setOpaque(false);
            setBackground(new Color(50, 50, 50, 200));
            GridBagConstraints gbc = new GridBagConstraints();

            // Gestion Sociale section with "faim" parameter
            gbc.gridx = 0; gbc.gridy = 0;
            gbc.fill = GridBagConstraints.BOTH;
            add(createGestionSocialePanel(), gbc);

            // Trésorerie
            gbc.gridy++;
            add(createTresoreriePanel(), gbc);

            // Salaries
            gbc.gridy++;
            salaryPanel = new JPanel(new GridBagLayout());
            TitledBorder salaryPanelBorder = BorderFactory.createTitledBorder("Salaires");
            salaryPanelBorder.setTitleColor(Color.WHITE);
            salaryPanel.setBorder(salaryPanelBorder);
            salaryPanel.setOpaque(true);
            salaryPanel.setBackground(new Color(50, 50, 50, 200));
            add(salaryPanel, gbc);

            // Resources
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.BOTH;
            resourcePanel = new JPanel(new GridBagLayout());
            TitledBorder resourcePanelBorder = BorderFactory.createTitledBorder("Ressource Stockées");
            resourcePanelBorder.setTitleColor(Color.WHITE);
            resourcePanel.setBorder(resourcePanelBorder);
            resourcePanel.setOpaque(true);
            resourcePanel.setBackground(new Color(50, 50, 50, 200));
            add(resourcePanel, gbc);


            updateDisplay(economicService.getEconomicData());
        }
        private JPanel createGestionSocialePanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            TitledBorder border = BorderFactory.createTitledBorder("Gestion Sociale");
            border.setTitleColor(Color.WHITE);
            panel.setBorder(border);
            panel.setOpaque(true);
            panel.setBackground(new Color(50, 50, 50, 200));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);

            // Population
            gbc.gridx = 0; gbc.gridy = 0;
            JLabel pops = new JLabel("Population:");
            pops.setFont(new Font("Arial", Font.BOLD, 11));
            pops.setForeground(Color.WHITE);
            panel.add(pops, gbc);
            gbc.gridx = 1;
            populationLabel = new JLabel("0");
            populationLabel.setFont(new Font("Arial", Font.BOLD, 11));
            populationLabel.setForeground(Color.WHITE);
            panel.add(populationLabel, gbc);

            // Instabilité
            gbc.gridx = 0; gbc.gridy = 1;
            JLabel insta = new JLabel("Instabilité:");
            insta.setFont(new Font("Arial", Font.BOLD, 11));
            insta.setForeground(Color.WHITE);
            panel.add(insta, gbc);
            gbc.gridx = 1;
            instabiliteLabel = new JLabel("0%");
            instabiliteLabel.setFont(new Font("Arial", Font.BOLD, 11));
            instabiliteLabel.setForeground(Color.WHITE);
            panel.add(instabiliteLabel, gbc);

            // Agressivité
            gbc.gridx = 0; gbc.gridy = 2;
            JLabel aggro = new JLabel("Agressivité:");
            aggro.setFont(new Font("Arial", Font.BOLD, 11));
            aggro.setForeground(Color.WHITE);
            panel.add(aggro, gbc);
            gbc.gridx = 1;
            agressiviteLabel = new JLabel("0");
            agressiviteLabel.setFont(new Font("Arial", Font.BOLD, 11));
            agressiviteLabel.setForeground(Color.WHITE);
            panel.add(agressiviteLabel, gbc);

            // Faim - NEW PARAMETER
            gbc.gridx = 0; gbc.gridy = 3;
            JLabel hunger = new JLabel("Faim:");
            hunger.setFont(new Font("Arial", Font.BOLD, 11));
            hunger.setForeground(Color.WHITE);
            panel.add(hunger, gbc);
            gbc.gridx = 1;
            faimLabel = new JLabel();
            faimLabel.setFont(new Font("Arial", Font.BOLD, 11));
            faimLabel.setForeground(Color.WHITE);
            faimLabel.setToolTipText("Niveau de faim de la population");
            panel.add(faimLabel, gbc);

            return panel;
        }
        private JPanel createTresoreriePanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            TitledBorder border = BorderFactory.createTitledBorder("Trésorerie ");
            border.setTitleColor(Color.WHITE);
            panel.setBorder(border);
            panel.setOpaque(true);
            panel.setBackground(new Color(50, 50, 50, 200));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0; gbc.gridy = 0;
            JLabel treasury = new JLabel("Trésorerie : ");
            treasury.setFont(new Font("Arial", Font.BOLD, 11));
            treasury.setForeground(Color.WHITE);
            panel.add(treasury, gbc);
            gbc.gridx = 1;
            tresorerieLabel = new JLabel();
            tresorerieLabel.setFont(new Font("Arial", Font.BOLD, 11));
            tresorerieLabel.setForeground(Color.WHITE);
            panel.add(tresorerieLabel, gbc);

            return panel;
        }

        @Override
        public void onEconomicDataChanged(EconomicDataService.EconomicData data) {
            SwingUtilities.invokeLater(() -> updateDisplay(data));
        }
        @Override
        public void onResourceProductionChanged(String hexKey, Map<String, Double> production) {
            SwingUtilities.invokeLater(() -> updateDisplay(economicService.getEconomicData()));
        }
        @Override
        public void onWorkerCountChanged(String hexKey, String buildingType, int count) {
            SwingUtilities.invokeLater(() -> updateDisplay(economicService.getEconomicData()));
        }
        private void updateDisplay(EconomicDataService.EconomicData data) {
            tresorerieLabel.setText(String.format("%.2f Po", data.tresorerie));
            populationLabel.setText(String.valueOf(data.populationTotale));
            if (data.instabilite>50){instabiliteLabel.setForeground(Color.red);}
            else{instabiliteLabel.setForeground(Color.white);}
            instabiliteLabel.setText(String.format("%.1f%%", data.instabilite));
            agressiviteLabel.setText(String.format("%.0f", data.agressivite));

            // Calculate and display "faim" based on food production vs consumption
            double faim = Math.max(0, data.consommationNourriture - data.productionNourriture);
            faimLabel.setText(String.format("%.1f", faim));
            if (faim > 0) {
                faimLabel.setForeground(Color.RED);
            } else {
                faimLabel.setForeground(Color.GREEN);
            }

            updateResourcesDisplay();
            updateSalaryDisplay(data);
        }

        public void updateResourcesDisplay() {
            resourcePanel.removeAll();
            String factionName = economicService.getFactionName();

            Map<String, SafeHexDetails> hexGrid = hexRepository.loadSafeAll();
            Map<String, Double> totalProduction = new HashMap<>();
            Map<String, Set<String>> resourceSources = new HashMap<>(); // Pour tracking des sources
            for (Map.Entry<String, SafeHexDetails> entry : hexGrid.entrySet()) {
                String hexKey = entry.getKey();
                SafeHexDetails hex = entry.getValue();
                if (!factionName.equals(hex.getFactionClaim())) continue;

                for (String buildingType : Arrays.asList("main", "aux", "fort")) {
                    String resourceName = hex.getSelectedResourceType(buildingType);
                    Double production = hex.getSelectedResourceProduction(buildingType);

                    if (resourceName != null && production != null && production > 0) {
                        totalProduction.merge(resourceName, production, Double::sum);


                        resourceSources.computeIfAbsent(resourceName, k -> new HashSet<>())
                                .add(hexKey + ":" + buildingType);
                    }
                }
            }

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(2, 5, 2, 5);
            gbc.anchor = GridBagConstraints.WEST;

            int row = 0;


            for (Map.Entry<String, Double> resource : totalProduction.entrySet()) {
                String resourceName = resource.getKey();
                Double production = resource.getValue();
                Color resourceColor = determineResourceColor(resourceName, production);
                gbc.gridx = 0; gbc.gridy = row;
                String icon = DATABASE.ResourceType.getIconForResource(resourceName);
                JLabel iconLabel = new JLabel(icon);
                iconLabel.setPreferredSize(new Dimension(30, 20));
                resourcePanel.add(iconLabel, gbc);

                gbc.gridx = 1;
                JLabel nameLabel = new JLabel(resourceName);
                nameLabel.setForeground(resourceColor);
                nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
                resourcePanel.add(nameLabel, gbc);

                gbc.gridx = 2;
                JLabel productionLabel = new JLabel(String.format("%.1f/sem", production));
                productionLabel.setForeground(resourceColor);
                productionLabel.setFont(new Font("Arial", Font.PLAIN, 11));
                resourcePanel.add(productionLabel, gbc);

                // Info tooltip avec sources
                if (resourceSources.containsKey(resourceName)) {
                    String tooltip = "Produit par: " + String.join(", ", resourceSources.get(resourceName));
                    nameLabel.setToolTipText(tooltip);
                    productionLabel.setToolTipText(tooltip);
                }

                row++;
            }
            if (totalProduction.isEmpty()) {
                gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3;
                JLabel noResourcesLabel = new JLabel("Aucune ressource en production");
                noResourcesLabel.setForeground(Color.GRAY);
                noResourcesLabel.setFont(new Font("Arial", Font.ITALIC, 12));
                resourcePanel.add(noResourcesLabel, gbc);
            }

            resourcePanel.revalidate();
            resourcePanel.repaint();

            System.out.println("Resource display updated: " + totalProduction.size() + " active resources");
        }

        private Color determineResourceColor(String resourceName, double production) {
            if (isRecentlyAdded(resourceName) || hasProductionIncreased(resourceName, production)) {
                return new Color(34, 139, 34);
            }

            DATABASE.ResourceType resourceType = DATABASE.ResourceType.lookupByName(resourceName);
            if (resourceType != null) {
                return switch (resourceType.getCategory()) {
                    case "Nourriture" -> new Color(200, 101, 25); // Brun pour nourriture
                    case "Minerais" -> new Color(140, 140, 140);    // Gris foncé pour minerais
                    case "Luxueuse" -> new Color(186, 1, 186);   // Violet pour luxe
                    case "Artisanat" -> new Color(0, 100, 200);  // Bleu pour artisanat
                    default -> Color.WHITE; // Noir par défaut
                };
            }

            return Color.BLACK; // Fallback
        }

        private boolean isRecentlyAdded(String resourceName) {
            return false;
        }
        private boolean hasProductionIncreased(String resourceName, double currentProduction) {
            return false;
        }
        private void updateSalaryDisplay(EconomicDataService.EconomicData data) {
            salaryPanel.removeAll();
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(2, 5, 2, 5);
            gbc.anchor = GridBagConstraints.WEST;

            int row = 0;
            for (Map.Entry<String, Double> entry : data.salaires.entrySet()) {
                if (entry.getValue() > 0) {
                    gbc.gridx = 0; gbc.gridy = row;
                    JLabel entryLabel = new JLabel(entry.getKey() + ":");
                    entryLabel.setFont(new Font("Arial", Font.BOLD, 11));
                    entryLabel.setForeground(Color.WHITE);
                    salaryPanel.add(entryLabel, gbc);

                    gbc.gridx = 1;
                    JLabel valueLabel = new JLabel(String.format("%.1f Po", entry.getValue()));
                    valueLabel.setFont(new Font("Arial", Font.BOLD, 11));
                    valueLabel.setForeground(Color.WHITE);
                    salaryPanel.add(valueLabel, gbc);
                    row++;
                }
            }

            if (row == 0) {
                gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
                salaryPanel.add(new JLabel("Aucun salaire configuré"), gbc);
            }

            salaryPanel.revalidate();
            salaryPanel.repaint();
        }
    }

    // Enhanced Production Panel with merged functionality
    public static class EnhancedProductionPanel extends JScrollPane
            implements EconomicDataService.EconomicDataObserver {
        private JPanel contentPanel;
        private Map<String, SafeHexDetails> hexGrid;
        private String factionName;
        private IHexRepository repository;
        private EconomicDataService economicService;
        private WorkDetailsPopup workDetailsPopup;
        private Map<String, JPanel> hexPanels = new HashMap<>();
        private boolean showHexPreview = true;
        public EnhancedProductionPanel(Map<String, SafeHexDetails> hexGrid, String factionName,
                                       IHexRepository repo, EconomicDataService economicService) {
            super();
            this.hexGrid = hexGrid;
            this.factionName = factionName;
            this.repository = repo;
            this.economicService = economicService;

            economicService.addObserver(this);
            workDetailsPopup = new WorkDetailsPopup();

            initializePanel();
        }
        private void initializePanel() {

            contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setOpaque(true);
            contentPanel.setBackground(new Color(211, 211, 211, 128));

            refreshContent();

            setViewportView(contentPanel);
            setOpaque(false);
            getViewport().setOpaque(false);
            configureScrollSpeed();

            if (getParent() instanceof JLayeredPane) {
                ((JLayeredPane) getParent()).add(workDetailsPopup, JLayeredPane.POPUP_LAYER);
            }
        }
        public void configureScrollSpeed() {

            int scrollSpeedMultiplier =10;
            addMouseWheelListener(e -> {
                JScrollBar scrollBar = getVerticalScrollBar();
                int currentValue = scrollBar.getValue();
                int scrollAmount = e.getScrollAmount() * scrollSpeedMultiplier;

                if (e.getWheelRotation() < 0) {
                    // Scroll up
                    scrollBar.setValue(currentValue - scrollAmount);
                } else {
                    // Scroll down
                    scrollBar.setValue(currentValue + scrollAmount);
                }
            });
        }
        public void refreshContent() {
            contentPanel.removeAll();
            hexPanels.clear();

            // Add hex preview toggle
            JPanel controlPanel = createControlPanel();
            contentPanel.add(controlPanel);

            for (Map.Entry<String, SafeHexDetails> entry : hexGrid.entrySet()) {
                SafeHexDetails hex = entry.getValue();

                if (factionName.equals(hex.getFactionClaim())) {

                    JPanel hexPanel = createEnhancedHexPanel(entry.getKey(), hex, this.repository);
                    hexPanels.put(entry.getKey(), hexPanel);
                    contentPanel.add(hexPanel);
                    contentPanel.add(Box.createVerticalStrut(5));
                }
            }

            contentPanel.revalidate();
            contentPanel.repaint();
        }
        private JPanel createControlPanel() {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            panel.setOpaque(true);
            panel.setBackground(new Color(211, 211, 211, 128));

            JCheckBox hexPreviewToggle = new JCheckBox("Afficher aperçu hexagones", showHexPreview);
            hexPreviewToggle.setOpaque(false);
            hexPreviewToggle.addActionListener(e -> {
                showHexPreview = hexPreviewToggle.isSelected();
                refreshContent();
            });
            panel.add(hexPreviewToggle);

            return panel;
        }
        private JPanel createEnhancedHexPanel(String hexKey, SafeHexDetails hex, IHexRepository repo) {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createTitledBorder(hexKey));
            panel.setOpaque(false);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;

            // Hex preview (if enabled)
            if (showHexPreview) {
                gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3;
                JPanel hexPreview = createHexPreview(hexKey, hex, repo);
                panel.add(hexPreview, gbc);
                gbc.gridy++;
                gbc.gridwidth = 1;
            }

            // Building sections
            String[] buildingTypes = {"main", "aux", "fort"};
            String[] buildingLabels = {"Principal", "Auxiliaire", "Fortification"};

            for (int i = 0; i < buildingTypes.length; i++) {
                gbc.gridx = i;
                if (!showHexPreview) gbc.gridy = 0;
                JPanel buildingPanel = createBuildingProductionPanel(hexKey, hex, buildingTypes[i], buildingLabels[i]);
                panel.add(buildingPanel, gbc);
            }

            return panel;
        }
        private JPanel createHexPreview(String hexKey, SafeHexDetails hex, IHexRepository repo) {

            JLabel hexImageLabel = hexImageLabels.computeIfAbsent(hexKey, key -> {
                BufferedImage hexSnapshot = UI.HexSnapshotCache.getHexSnapshot(key, repo);
                JLabel hexLabel = new JLabel();
                if (hexSnapshot != null) {
                    hexLabel.setIcon(new ImageIcon(hexSnapshot));
                }
                hexLabel.setPreferredSize(new Dimension(100, 100));
                return hexLabel;
            });

            JPanel preview = new JPanel(new FlowLayout());
            preview.setBorder(BorderFactory.createTitledBorder("Aperçu Hexagone"));
            preview.setOpaque(false);

            String buildingNames = UIHelpers.getBuildingNamesSafe(
                    hex.getMainBuildingIndex(),
                    hex.getAuxBuildingIndex(),
                    hex.getFortBuildingIndex()
            );

            JLabel info = new JLabel(String.format("Faction: %s | %s",
                    hex.getFactionClaim(), buildingNames));;
            preview.add(info);
            preview.add(hexImageLabel);

            return preview;
        }
        private JPanel createBuildingProductionPanel(String hexKey, SafeHexDetails hex,
                                                     String buildingType, String label) {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createTitledBorder(label));
            panel.setBackground(new Color(211, 211, 211, 128));
            panel.setOpaque(true);


            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.anchor = GridBagConstraints.WEST;

            // Workers count with pull ability
            int workerCount = hex.getWorkerCountByType(buildingType);
            gbc.gridx = 0; gbc.gridy = 0;

            JPanel workerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
            workerPanel.setOpaque(false);

            JLabel workerLabel = new JLabel("👥 " + workerCount);
            workerPanel.add(workerLabel);

            // Pull workers button
            JButton pullButton = new JButton("↑");
            pullButton.setToolTipText("Retirer des workers");
            pullButton.addActionListener(e -> pullWorkers(hexKey, hex, buildingType));
            workerPanel.add(pullButton);

            // Add workers button
            JButton addButton = new JButton("↓");
            addButton.setToolTipText("Ajouter des workers");
            addButton.addActionListener(e -> addWorkers(hexKey, hex, buildingType));
            workerPanel.add(addButton);

            panel.add(workerPanel, gbc);

            // Lock mechanism
            gbc.gridy = 1;
            JCheckBox lockCheckbox = new JCheckBox("🔒", hex.isSlotLocked(buildingType));
            lockCheckbox.setOpaque(false);
            lockCheckbox.setToolTipText("Verrouiller ce slot");
            lockCheckbox.addActionListener(e -> {
                if (lockCheckbox.isSelected()) {
                    hex.lockSlot(buildingType);
                } else {
                    hex.getLockedSlots().put(buildingType, false);
                }
                repository.updateHexDetails(hexKey, hex);
            });
            panel.add(lockCheckbox, gbc);

            // Resource production info using DATABASE resources
            gbc.gridy = 2;
            String resourceInfo = getResourceProductionInfo(hexKey, buildingType);
            JLabel resourceLabel = new JLabel(resourceInfo);

            panel.add(resourceLabel, gbc);

            // Configuration button
            gbc.gridy = 3;
            JButton configButton = new JButton("Config");
            configButton.addActionListener(e -> openProductionDialog(hexKey, hex, buildingType, label));
            panel.add(configButton, gbc);

            gbc.gridy = 3; gbc.gridx++;
            JButton livestockButton = new JButton("Élevage");
            livestockButton.setPreferredSize(new Dimension(120, 30));
            livestockButton.setBackground(new Color(139, 69, 19)); // Couleur terre
            livestockButton.setForeground(Color.WHITE);
            livestockButton.setVisible(false);
            livestockButton.addActionListener(e -> openLivestockDialog(hexKey, hex));
            if(UIHelpers.isFarmBuilding(hex, buildingType)) {livestockButton.setVisible(true);}
            panel.add(livestockButton, gbc);

            return panel;
        }
        private void pullWorkers(String hexKey, SafeHexDetails hex, String buildingType) {
            int currentCount = hex.getWorkerCountByType(buildingType);
            if (currentCount > 0) {
                hex.setWorkerCountByType(buildingType, currentCount - 1);
                repository.updateHexDetails(hexKey, hex);
                economicService.updateWorkerCount(hexKey, buildingType, currentCount - 1);
                refreshContent();
            }
        }
        private void addWorkers(String hexKey, SafeHexDetails hex, String buildingType) {
            int currentCount = hex.getWorkerCountByType(buildingType);
            hex.setWorkerCountByType(buildingType, currentCount + 1);
            repository.updateHexDetails(hexKey, hex);
            economicService.updateWorkerCount(hexKey, buildingType, currentCount + 1);
            refreshContent();
        }
        private String getResourceProductionInfo(String hexKey, String buildingType) {
            // Use DATABASE resources instead of hardcoded values
            DATABASE.ResourceType[] availableResources = getAvailableResourcesForBuilding(buildingType);

            if (availableResources.length > 0) {
                DATABASE.ResourceType resource = availableResources[0]; // Default to first available
                return String.format(" %s: %.1f/sem", resource.getName(), 0.0);
            }

            return " Aucune: 0/sem";
        }
        private DATABASE.ResourceType[] getAvailableResourcesForBuilding(String buildingType) {
            return switch (buildingType.toLowerCase()) {
                case "main", "main building" -> DATABASE.BUILDING_RESOURCES.get("MAIN");
                case "aux", "auxiliary building" -> DATABASE.BUILDING_RESOURCES.get("AUX");
                default -> new DATABASE.ResourceType[0];
            };
        }
        private String getDetailedResourceInfo(String hexKey, SafeHexDetails hex, String buildingType) {
            StringBuilder details = new StringBuilder();
            details.append("<html><body>");
            details.append("<h3>").append(hexKey).append(" - ").append(buildingType.toUpperCase()).append("</h3>");

            DATABASE.ResourceType[] resources = getAvailableResourcesForBuilding(buildingType);
            details.append("<p><b>Ressources disponibles:</b></p><ul>");

            for (DATABASE.ResourceType resource : resources) {
                details.append("<li>").append(resource.getName()).append(" (").append(resource.getCategory()).append(") - Base: ").append(resource.getBaseValue()).append("</li>");
            }

            details.append("</ul>");
            details.append("<p><b>Workers:</b> ").append(hex.getWorkerCountByType(buildingType)).append("</p>");
            details.append("<p><b>Verrouillé:</b> ").append(hex.isSlotLocked(buildingType) ? "Oui" : "Non").append("</p>");
            details.append("</body></html>");

            return details.toString();
        }
        private void openProductionDialog(String hexKey, SafeHexDetails hex, String buildingType, String buildingLabel) {
            DATABASE.JobBuilding building = UIHelpers.getBuildingFromHex(hex, buildingType);

            if (building == null || building.getBuildName().contains("Free Slot")) {
                JOptionPane.showMessageDialog(this,
                        "Aucun bâtiment configuré pour ce slot.",
                        "Configuration manquante",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            List<DATABASE.ResourceType> availableResources = DATABASE.getResourcesForBuilding(building);

            if (availableResources.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Aucune ressource disponible pour ce bâtiment:\n" + building.getBuildName(),
                        "Pas de production possible",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Créer et afficher le dialog avec l'hex
            ProductionDialog dialog = new ProductionDialog(
                    (JFrame) SwingUtilities.getWindowAncestor(this),
                    hexKey, buildingType, building.getBuildName(),
                    hex.getWorkerCountByType(buildingType),
                    economicService,
                    hex  // ← NOUVEAU paramètre
            );

            dialog.setVisible(true);
            if (dialog.isConfirmed()) {
                int newCount = dialog.getSelectedWorkerCount();
                hex.setWorkerCountByType(buildingType, newCount);
                repository.updateHexDetails(hexKey, hex);
                refreshContent();
            }
        }
        private void openLivestockDialog(String hexKey, SafeHexDetails hex) {
            JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            LivestockDialog dialog = new LivestockDialog(parentFrame, hex);
            dialog.setVisible(true);

            if (dialog.isConfirmed()) {
                // Actualiser l'affichage et les données économiques
                if (economicService != null) {
                    economicService.calculateInitialData();
                }
                repaint();
            }
        }
        @Override
        public void onEconomicDataChanged(EconomicDataService.EconomicData data) {
            SwingUtilities.invokeLater(this::refreshContent);
        }
        @Override
        public void onResourceProductionChanged(String hexKey, Map<String, Double> production) {
            SwingUtilities.invokeLater(this::refreshContent);
        }
        @Override
        public void onWorkerCountChanged(String hexKey, String buildingType, int count) {
            SwingUtilities.invokeLater(this::refreshContent);
        }

    }


}