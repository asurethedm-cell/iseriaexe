package com.iseria.ui;

import com.iseria.domain.DATABASE;
import com.iseria.domain.Faction;
import com.iseria.domain.Rumor;
import com.iseria.service.RumorService;
import com.iseria.infra.FactionRegistry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

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
        setTitle("🛡️ Administration des Rumeurs - Panel Admin");
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

        JLabel titleLabel = new JLabel("🛡️ Panel Administrateur - Gestion des Rumeurs");
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
        // Panel principal pour cette faction
        JPanel factionMainPanel = new JPanel(new BorderLayout());
        factionMainPanel.setBackground(new Color(50, 50, 50));

        // Header de faction avec contrôles
        JPanel factionHeader = createFactionHeader(factionName);
        factionMainPanel.add(factionHeader, BorderLayout.NORTH);

        // Panel d'affichage des rumeurs
        RumorDisplayPanel rumorPanel = new RumorDisplayPanel();
        rumorPanel.setBackground(new Color(60, 60, 60));

        JScrollPane scrollPane = new JScrollPane(rumorPanel);
        scrollPane.setBackground(new Color(60, 60, 60));
        scrollPane.getViewport().setBackground(new Color(60, 60, 60));

        factionMainPanel.add(scrollPane, BorderLayout.CENTER);

        // Stocker les références
        factionPanels.put(factionName, rumorPanel);
        factionScrollPanes.put(factionName, scrollPane);

        // Charger les rumeurs de cette faction
        refreshFactionRumors(factionName);

        // Ajouter l'onglet
        tabbedPane.addTab("📜 " + factionName, factionMainPanel);
    }

    private JPanel createFactionHeader(String factionName) {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBackground(new Color(40, 40, 40));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Titre de faction
        JLabel factionLabel = new JLabel("Faction: " + factionName);
        factionLabel.setFont(new Font("Arial", Font.BOLD, 16));
        factionLabel.setForeground(Color.WHITE);
        headerPanel.add(factionLabel);

        headerPanel.add(Box.createHorizontalStrut(20));

        // Bouton créer nouvelle rumeur
        JButton createRumorBtn = createStyledButton("➕ Nouvelle Rumeur", new Color(34, 139, 34));
        createRumorBtn.addActionListener(e -> showCreateRumorDialog(factionName));
        headerPanel.add(createRumorBtn);

        headerPanel.add(Box.createHorizontalStrut(10));

        // Bouton rafraîchir
        JButton refreshBtn = createStyledButton("🔄 Actualiser", new Color(70, 130, 180));
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

        // Charger toutes les rumeurs
        List<Rumor> allRumors = rumorService.getAllRumors()
                .stream()
                .sorted(Comparator.comparing(Rumor::getDate).reversed())
                .collect(Collectors.toList());

        rumorPanel.displayRumors(allRumors);

        tabbedPane.addTab("📚 Toutes", allRumorsPanel);
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

        JButton approveAllBtn = createStyledButton("✅ Tout Approuver", new Color(34, 139, 34));
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

        tabbedPane.addTab("⏳ En Attente (" + pendingRumors.size() + ")", pendingPanel);
    }

    private JPanel createGlobalControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        controlPanel.setBackground(new Color(30, 30, 30));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Bouton actualiser tout
        JButton refreshAllBtn = createStyledButton("🔄 Actualiser Tout", new Color(70, 130, 180));
        refreshAllBtn.addActionListener(e -> refreshAllTabs());
        controlPanel.add(refreshAllBtn);

        controlPanel.add(Box.createHorizontalStrut(20));

        // Bouton export
        JButton exportBtn = createStyledButton("💾 Exporter", new Color(128, 0, 128));
        exportBtn.addActionListener(e -> exportAllRumors());
        controlPanel.add(exportBtn);

        controlPanel.add(Box.createHorizontalStrut(20));

        // Bouton fermer
        JButton closeBtn = createStyledButton("❌ Fermer", new Color(220, 20, 60));
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

        // Type
        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(new JLabel("Type:"), gbc);
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Reçue", "Propagé"});
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(typeCombo, gbc);

        // Nom
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Nom:"), gbc);
        JTextField nameField = new JTextField(30);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(nameField, gbc);

        // **NOUVEAU - Date avec JSpinner**
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Date:"), gbc);

        // Créer le JSpinner avec DateEditor pour date + heure
        SpinnerDateModel dateModel = new SpinnerDateModel();
        JSpinner dateSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy HH:mm");
        dateSpinner.setEditor(dateEditor);
        dateSpinner.setValue(new java.util.Date()); // Date actuelle par défaut

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(dateSpinner, gbc);

        // Contenu
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Contenu:"), gbc);
        JTextArea contentArea = new JTextArea(8, 30);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        JScrollPane contentScroll = new JScrollPane(contentArea);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0;
        mainPanel.add(contentScroll, gbc);

        // Boutons
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

            // Convertir java.util.Date vers LocalDateTime
            java.time.LocalDateTime dateTime = java.time.LocalDateTime.ofInstant(
                    selectedDate.toInstant(),
                    java.time.ZoneId.systemDefault()
            );

            // Créer la rumeur avec la date sélectionnée
            Rumor newRumor = rumorService.createFromTNCD(type, name, content, dateTime);
            newRumor.setAuthorFactionId(factionName);

            JOptionPane.showMessageDialog(createDialog, "Rumeur créée avec succès!", "Succès", JOptionPane.INFORMATION_MESSAGE);
            rumorService.saveRumor(newRumor);
            createDialog.dispose();

            // Actualiser l'affichage
            refreshFactionRumors(factionName);
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
}
