package com.iseria.ui;

import com.iseria.domain.DATABASE;
import com.iseria.domain.Faction;
import com.iseria.domain.Rumor;
import com.iseria.service.RumorService;
import com.iseria.infra.FactionRegistry;
import com.iseria.service.RumorServiceImpl;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.iseria.ui.UIHelpers.configureScrollSpeed;
import static com.iseria.ui.UIHelpers.styleScrollPane;

public class AdminRumorManagementPanel extends JFrame {
    private final RumorService rumorService;
    private final Map<String, RumorDisplayPanel> factionPanels = new HashMap<>();
    private final Map<String, JScrollPane> factionScrollPanes = new HashMap<>();
    private JTabbedPane tabbedPane;

    public AdminRumorManagementPanel(RumorService rumorService) {
        this.rumorService = rumorService;
        initializeAdminPanel();
    }

    private void initializeAdminPanel() {
        setTitle("Administration des Rumeurs - Panel Admin");
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(40, 40, 40));

        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(60, 60, 60));
        tabbedPane.setForeground(Color.WHITE);

        createFactionTabs();

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        JPanel controlPanel = createGlobalControlPanel();
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBackground(new Color(30, 30, 30));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Panel Administrateur - Gestion des Rumeurs");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel);

        JLabel statsLabel = new JLabel(getGlobalStats());
        statsLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        statsLabel.setForeground(Color.LIGHT_GRAY);
        headerPanel.add(Box.createHorizontalStrut(50));
        headerPanel.add(statsLabel);

        return headerPanel;
    }

    private void createFactionTabs() {
        Collection<Faction> allFactions = FactionRegistry.all();
        Set<String> playerFactionNames = allFactions.stream()
                .filter(Faction::getIsPlayer)
                .map(Faction::getDisplayName)
                .collect(Collectors.toSet());

        for (String factionName : playerFactionNames) {
            createFactionTab(factionName);
        }
        createAllRumorsTab();
        createPendingRumorsTab();
    }

    private void createFactionTab(String factionName) {
        JPanel factionMainPanel = new JPanel(new BorderLayout());
        factionMainPanel.setBackground(new Color(50, 50, 50));

        JPanel factionHeader = createFactionHeader(factionName);
        factionMainPanel.add(factionHeader, BorderLayout.NORTH);

        RumorDisplayPanel rumorPanel = new RumorDisplayPanel();
        rumorPanel.setBackground(new Color(60, 60, 60));
        rumorPanel.setEditCallback(this::showEditRumorDialog);

        JScrollPane scrollPane = new JScrollPane(rumorPanel);
        scrollPane.setBackground(new Color(60, 60, 60));
        scrollPane.getViewport().setBackground(new Color(60, 60, 60));
        styleScrollPane(scrollPane);
        configureScrollSpeed(scrollPane,20,80);
        factionMainPanel.add(scrollPane, BorderLayout.CENTER);

        factionPanels.put(factionName, rumorPanel);
        factionScrollPanes.put(factionName, scrollPane);

        refreshFactionRumors(factionName);

        tabbedPane.addTab(" " + factionName, factionMainPanel);
    }

    private JPanel createFactionHeader(String factionName) {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBackground(new Color(40, 40, 40));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel factionLabel = new JLabel("Faction: " + factionName);
        factionLabel.setFont(new Font("Arial", Font.BOLD, 16));
        factionLabel.setForeground(Color.WHITE);
        headerPanel.add(factionLabel);
        headerPanel.add(Box.createHorizontalStrut(20));

        JButton createRumorBtn = createStyledButton("Nouvelle Rumeur", new Color(34, 139, 34));
        createRumorBtn.addActionListener(e -> showCreateRumorDialog(factionName));
        headerPanel.add(createRumorBtn);
        headerPanel.add(Box.createHorizontalStrut(10));

        JButton refreshBtn = createStyledButton("Actualiser", new Color(70, 130, 180));
        refreshBtn.addActionListener(e -> refreshFactionRumors(factionName));
        headerPanel.add(refreshBtn);

        return headerPanel;
    }

    private void createAllRumorsTab() {
        JPanel allRumorsPanel = new JPanel(new BorderLayout());
        allRumorsPanel.setBackground(new Color(50, 50, 50));

        RumorDisplayPanel rumorPanel = new RumorDisplayPanel();
        rumorPanel.setBackground(new Color(60, 60, 60));

        JScrollPane scrollPane = new JScrollPane(rumorPanel);
        allRumorsPanel.add(scrollPane, BorderLayout.CENTER);

        List<Rumor> allRumors = rumorService.getAllRumors()
                .stream()
                .sorted(Comparator.comparing(Rumor::getDate).reversed())
                .collect(Collectors.toList());

        rumorPanel.displayRumors(allRumors);

        tabbedPane.addTab("Toutes", allRumorsPanel);
    }

    private void createPendingRumorsTab() {
        JPanel pendingPanel = new JPanel(new BorderLayout());
        pendingPanel.setBackground(new Color(50, 50, 50));

        // Header avec contrôles d'approbation
        JPanel pendingHeader = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pendingHeader.setBackground(new Color(40, 40, 40));

        JLabel pendingLabel = new JLabel("Rumeurs en attente d'approbation");
        pendingLabel.setFont(new Font("Arial", Font.BOLD, 16));
        pendingLabel.setForeground(Color.YELLOW);
        pendingHeader.add(pendingLabel);

        JButton approveAllBtn = createStyledButton("Tout Approuver", new Color(34, 139, 34));
        approveAllBtn.addActionListener(e -> approveAllPendingRumors());
        pendingHeader.add(Box.createHorizontalStrut(20));
        pendingHeader.add(approveAllBtn);

        pendingPanel.add(pendingHeader, BorderLayout.NORTH);

        RumorDisplayPanel rumorPanel = new RumorDisplayPanel();
        rumorPanel.setBackground(new Color(60, 60, 60));

        JScrollPane scrollPane = new JScrollPane(rumorPanel);
        pendingPanel.add(scrollPane, BorderLayout.CENTER);

        // Charger les rumeurs en attente
        List<Rumor> pendingRumors = rumorService.getPendingRumors();
        rumorPanel.displayRumors(pendingRumors);

        tabbedPane.addTab("En Attente (" + pendingRumors.size() + ")", pendingPanel);
    }

    private JPanel createGlobalControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        controlPanel.setBackground(new Color(30, 30, 30));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton refreshAllBtn = createStyledButton("Actualiser Tout", new Color(70, 130, 180));
        refreshAllBtn.addActionListener(e -> refreshAllTabs());
        controlPanel.add(refreshAllBtn);

        controlPanel.add(Box.createHorizontalStrut(20));

        JButton exportBtn = createStyledButton("Exporter", new Color(128, 0, 128));
        exportBtn.addActionListener(e -> exportAllRumors());
        controlPanel.add(exportBtn);

        controlPanel.add(Box.createHorizontalStrut(20));

        JButton closeBtn = createStyledButton("Fermer", new Color(220, 20, 60));
        closeBtn.addActionListener(e -> dispose());
        controlPanel.add(closeBtn);

        return controlPanel;
    }

    private JButton createStyledButton(String text, Color backgroundColor) {
        JButton button = new JButton(text);
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(150, 30));
        return button;
    }

    private void showCreateRumorDialog(String factionName) {
        JDialog createDialog = new JDialog(this, "Créer une nouvelle rumeur - " + factionName, true);
        createDialog.setSize(600, 480); // Augmenté pour accommoder le champ supplémentaire
        createDialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(new JLabel("Type:"), gbc);
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Reçue", "Propagé"});
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(typeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Nom:"), gbc);
        JTextField nameField = new JTextField(30);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Date:"), gbc);

        SpinnerDateModel dateModel = new SpinnerDateModel();
        JSpinner dateSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy HH:mm");
        dateSpinner.setEditor(dateEditor);
        dateSpinner.setValue(new java.util.Date());

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(dateSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Contenu:"), gbc);
        JTextArea contentArea = new JTextArea(8, 30);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        JScrollPane contentScroll = new JScrollPane(contentArea);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0;
        mainPanel.add(contentScroll, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton createBtn = new JButton("Créer");
        JButton cancelBtn = new JButton("Annuler");

        createBtn.addActionListener(e -> {
            String type = (String) typeCombo.getSelectedItem();
            String name = nameField.getText().trim();
            String content = contentArea.getText().trim();
            java.util.Date selectedDate = (java.util.Date) dateSpinner.getValue();

            if (name.isEmpty() || content.isEmpty()) {
                JOptionPane.showMessageDialog(createDialog, "Tous les champs sont requis!", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                java.time.LocalDateTime dateTime = java.time.LocalDateTime.ofInstant(
                        selectedDate.toInstant(),
                        java.time.ZoneId.systemDefault()
                );

                Rumor newRumor = new Rumor(type, name, content, dateTime);
                newRumor.setId(((RumorServiceImpl) rumorService).getNextId());
                newRumor.setStatus(DATABASE.RumorStatus.APPROVED);
                newRumor.setAuthorFactionId(factionName);
                rumorService.saveRumor(newRumor);

                JOptionPane.showMessageDialog(createDialog, "Rumeur créée avec succès!", "Succès", JOptionPane.INFORMATION_MESSAGE);
                createDialog.dispose();
                refreshFactionRumors(factionName);

            } catch (Exception ex) {
                System.err.println("Erreur création rumeur: " + ex.getMessage());
                ex.printStackTrace();
                JOptionPane.showMessageDialog(createDialog,
                        "Erreur lors de la création: " + ex.getMessage(),
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelBtn.addActionListener(e -> createDialog.dispose());

        buttonPanel.add(createBtn);
        buttonPanel.add(cancelBtn);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weighty = 0;
        mainPanel.add(buttonPanel, gbc);

        createDialog.add(mainPanel);
        createDialog.setVisible(true);
    }

    private void refreshFactionRumors(String factionName) {
        RumorDisplayPanel panel = factionPanels.get(factionName);
        if (panel != null) {
            List<Rumor> factionRumors = rumorService.getAllRumors()
                    .stream()
                    .filter(rumor -> factionName.equals(rumor.getAuthorFactionId()))
                    .sorted(Comparator.comparing(Rumor::getDate).reversed())
                    .collect(Collectors.toList());

            panel.displayRumors(factionRumors);
        }
    }

    private void refreshAllTabs() {
        for (String faction : factionPanels.keySet()) {
            refreshFactionRumors(faction);
        }
        // TODO: Rafraîchir aussi les onglets "Toutes" et "En Attente"
    }

    private void approveAllPendingRumors() {
        List<Rumor> pendingRumors = rumorService.getPendingRumors();

        int result = JOptionPane.showConfirmDialog(this,
                "Approuver toutes les " + pendingRumors.size() + " rumeurs en attente?",
                "Confirmation",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            Set<String> defaultTargets = Set.of("Decimus", "Anima"); // ou toutes les factions

            for (Rumor rumor : pendingRumors) {
                rumorService.approveRumor(rumor.getId(), "Admin", defaultTargets);
            }

            JOptionPane.showMessageDialog(this,
                    pendingRumors.size() + " rumeurs approuvées!",
                    "Succès",
                    JOptionPane.INFORMATION_MESSAGE);

            refreshAllTabs();
        }
    }

    private void exportAllRumors() {
        // TODO: Implémenter l'export (Excel, CSV, etc.)
        JOptionPane.showMessageDialog(this, "Export des rumeurs - À implémenter", "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private String getGlobalStats() {
        List<Rumor> allRumors = rumorService.getAllRumors();
        long approved = allRumors.stream().filter(r -> r.getStatus() == DATABASE.RumorStatus.APPROVED).count();
        long pending = allRumors.stream().filter(r -> r.getStatus() == DATABASE.RumorStatus.PENDING).count();
        long rejected = allRumors.stream().filter(r -> r.getStatus() == DATABASE.RumorStatus.REJECTED).count();

        return String.format("Total: %d | Approuvées: %d | En attente: %d | Rejetées: %d",
                allRumors.size(), approved, pending, rejected);
    }
    private void showEditRumorDialog(Rumor rumor) {
        JDialog editDialog = new JDialog(this, "Éditer Rumeur - ID: " + rumor.getId(), true);
        editDialog.setSize(650, 500);
        editDialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(new JLabel("Type:"), gbc);
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Reçue", "Propagé", "Militaire", "Social"});
        typeCombo.setSelectedItem(rumor.getType());
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(typeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Nom:"), gbc);
        JTextField nameField = new JTextField(rumor.getName(), 30);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Date:"), gbc);

        SpinnerDateModel dateModel = new SpinnerDateModel();
        JSpinner dateSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy HH:mm");
        dateSpinner.setEditor(dateEditor);

        java.util.Date currentDate = java.util.Date.from(
                rumor.getDate().atZone(java.time.ZoneId.systemDefault()).toInstant()
        );
        dateSpinner.setValue(currentDate);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(dateSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Faction:"), gbc);

        Collection<Faction> allFactions = FactionRegistry.all();
        String[] factionNames = allFactions.stream()
                .filter(Faction::getIsPlayer)
                .map(Faction::getDisplayName)
                .toArray(String[]::new);

        JComboBox<String> factionCombo = new JComboBox<>(factionNames);
        factionCombo.setSelectedItem(rumor.getAuthorFactionId());
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(factionCombo, gbc);

        // Contenu (modifiable)
        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Contenu:"), gbc);
        JTextArea contentArea = new JTextArea(rumor.getContent(), 8, 30);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        JScrollPane contentScroll = new JScrollPane(contentArea);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0; gbc.weighty = 1.0;
        mainPanel.add(contentScroll, gbc);

        // Informations de statut
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;
        JLabel statusInfo = new JLabel(
                String.format("Status: %s | ID: %d | Créé: %s",
                        rumor.getStatus(), rumor.getId(), (rumor.getDate()))
        );
        statusInfo.setFont(new Font("Arial", Font.ITALIC, 10));
        statusInfo.setForeground(Color.GRAY);
        mainPanel.add(statusInfo, gbc);

        // Boutons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveBtn = new JButton("Sauvegarder");
        JButton cancelBtn = new JButton("Annuler");

        saveBtn.addActionListener(e -> {
            try {
                rumor.setType((String) typeCombo.getSelectedItem());
                rumor.setName(nameField.getText().trim());
                rumor.setContent(contentArea.getText().trim());

                java.util.Date selectedDate = (java.util.Date) dateSpinner.getValue();
                LocalDateTime newDateTime = LocalDateTime.ofInstant(
                        selectedDate.toInstant(),
                        java.time.ZoneId.systemDefault()
                );
                rumor.setDate(newDateTime);

                rumor.setAuthorFactionId((String) factionCombo.getSelectedItem());

                // Validation
                if (rumor.getName().isEmpty() || rumor.getContent().isEmpty()) {
                    JOptionPane.showMessageDialog(editDialog,
                            "Le nom et le contenu sont requis!", "Erreur", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                rumorService.saveRumor(rumor);
                JOptionPane.showMessageDialog(editDialog,
                        "Rumeur mise à jour avec succès!", "Succès", JOptionPane.INFORMATION_MESSAGE);

                editDialog.dispose();
                refreshAllTabs();

            } catch (Exception ex) {
                System.err.println("Erreur modification rumeur: " + ex.getMessage());
                ex.printStackTrace();
                JOptionPane.showMessageDialog(editDialog,
                        "Erreur lors de la modification: " + ex.getMessage(),
                        "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelBtn.addActionListener(e -> editDialog.dispose());

        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weighty = 0;
        mainPanel.add(buttonPanel, gbc);

        editDialog.add(mainPanel);
        editDialog.setVisible(true);
    }
}
