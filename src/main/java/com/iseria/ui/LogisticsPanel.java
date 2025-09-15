package com.iseria.ui;

import com.iseria.domain.*;
import com.iseria.infra.FactionRegistry;
import com.iseria.service.LogisticsService;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LogisticsPanel extends JPanel {
    private LogisticsService logisticsService;
    private IHexRepository hexRepository;
    private String selectedHexKey;

    // UI Components
    private JComboBox<String> hexCombo;
    private DefaultListModel<String> vehicleListModel;
    private JList<String> vehicleList;
    private JLabel warehouseLocationLabel;
    private JLabel warehouseCapacityLabel;
    private JLabel warehouseUsageLabel;
    private JTextArea transportTimesArea;

    public LogisticsPanel(LogisticsService logisticsService, IHexRepository hexRepository) {
        this.logisticsService = logisticsService;
        this.hexRepository = hexRepository;
        initializeUI();
        loadInitialData();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2),
                "🚚 Gestion Logistique",
                0, 0,
                new Font("Arial", Font.BOLD, 18),
                Color.WHITE));

        // Panel principal avec fond semi-transparent
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setOpaque(true);
        mainPanel.setBackground(new Color(0, 0, 0, 150));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Panel de sélection hex
        JPanel hexSelectionPanel = createHexSelectionPanel();
        mainPanel.add(hexSelectionPanel, BorderLayout.NORTH);

        // Panel central avec informations logistiques
        JPanel centerPanel = createCenterPanel();
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Panel de contrôles
        JPanel controlPanel = createControlPanel();
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createHexSelectionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setOpaque(false);

        JLabel label = new JLabel("🎯 Hexagone sélectionné:");
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setForeground(Color.WHITE);
        panel.add(label);

        hexCombo = new JComboBox<>();
        hexCombo.setPreferredSize(new Dimension(200, 30));
        hexCombo.addActionListener(e -> {
            selectedHexKey = (String) hexCombo.getSelectedItem();
            updateLogisticsDisplay();
        });
        panel.add(hexCombo);

        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 20, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        // Section véhicules
        panel.add(createVehicleManagementPanel());

        // Section entrepôt
        panel.add(createWarehouseInfoPanel());

        // Section temps de transport
        panel.add(createTransportTimesPanel());

        return panel;
    }

    private JPanel createVehicleManagementPanel() {
        String factionId = MainMenu.getCurrentFactionId();
        Faction faction = FactionRegistry.getFactionId(factionId);  // ou via votre service
        Color factionColor = faction != null
                ? faction.getFactionColor()
                : Color.CYAN;
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(factionColor, 1),
                "🚗 Véhicules Assignés",
                0, 0,
                new Font("Arial", Font.BOLD, 14),
                Color.WHITE));
        panel.setOpaque(true);
        panel.setBackground(factionColor);

        vehicleListModel = new DefaultListModel<>();
        vehicleList = new JList<>(vehicleListModel);
        vehicleList.setBackground(new Color(40, 40, 40));
        vehicleList.setForeground(Color.WHITE);
        vehicleList.setFont(new Font("Arial", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(vehicleList);
        scrollPane.setPreferredSize(new Dimension(200, 150));
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setOpaque(false);

        JButton addVehicleButton = createStyledButton("Ajouter", Color.GREEN);
        addVehicleButton.addActionListener(this::showAddVehicleDialog);
        buttonPanel.add(addVehicleButton);

        JButton removeVehicleButton = createStyledButton("Retirer", Color.RED);
        removeVehicleButton.addActionListener(this::removeSelectedVehicle);
        buttonPanel.add(removeVehicleButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createWarehouseInfoPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.ORANGE, 1),
                "📦 Entrepôt le Plus Proche",
                0, 0,
                new Font("Arial", Font.BOLD, 14),
                Color.ORANGE));
        panel.setOpaque(true);
        panel.setBackground(new Color(60, 40, 20, 200));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Labels d'information
        addWarehouseInfoRow(panel, "📍 Localisation:", warehouseLocationLabel = createInfoLabel("-"));
        addWarehouseInfoRow(panel, "📏 Capacité:", warehouseCapacityLabel = createInfoLabel("-"));
        addWarehouseInfoRow(panel, "📊 Utilisation:", warehouseUsageLabel = createInfoLabel("-"));

        return panel;
    }

    private void addWarehouseInfoRow(JPanel parent, String labelText, JLabel valueLabel) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        label.setForeground(Color.WHITE);
        parent.add(label);
        parent.add(valueLabel);
    }

    private JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.PLAIN, 12));
        label.setForeground(Color.LIGHT_GRAY);
        return label;
    }

    private JPanel createTransportTimesPanel() {

        String factionId = MainMenu.getCurrentFactionId();
        Faction faction = FactionRegistry.getFactionId(factionId);  // ou via votre service
        Color factionColor = faction != null
                ? faction.getFactionColor()
                : Color.MAGENTA;
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(factionColor, 1),
                "⏱️ Temps de Transport",
                0, 0,
                new Font("Arial", Font.BOLD, 14),
                Color.WHITE));
        panel.setOpaque(true);
        panel.setBackground(factionColor);

        transportTimesArea = new JTextArea(8, 20);
        transportTimesArea.setEditable(false);
        transportTimesArea.setBackground(new Color(30, 30, 30));
        transportTimesArea.setForeground(Color.WHITE);
        transportTimesArea.setFont(new Font("Courier New", Font.PLAIN, 10));

        JScrollPane scrollPane = new JScrollPane(transportTimesArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setOpaque(false);

        JButton refreshButton = createStyledButton("🔄 Actualiser", Color.BLUE);
        refreshButton.addActionListener(e -> {
            System.out.println("🔄 Actualisation manuelle des données logistiques...");

            // Recharger toutes les données
            refreshData();

            // Feedback utilisateur
            JOptionPane.showMessageDialog(this,
                    "✅ Données logistiques actualisées!\n" +
                            "• Liste des hexagones rechargée\n" +
                            "• Véhicules mis à jour\n" +
                            "• Entrepôts recalculés",
                    "Actualisation réussie",
                    JOptionPane.INFORMATION_MESSAGE);
        });
        panel.add(refreshButton);

        JButton networkButton = createStyledButton("🗺️ Réseau", Color.DARK_GRAY);
        networkButton.addActionListener(e -> showTransportNetwork());
        panel.add(networkButton);

        return panel;
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 11));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setOpaque(true);
        button.setBorderPainted(false);
        return button;
    }

    private void loadInitialData() {
        // Charger seulement les hexagones de la faction du joueur
        hexCombo.removeAllItems();
        hexCombo.addItem("-- Sélectionner --");

        // ✅ NOUVEAU: Obtenir la faction du joueur connecté
        String currentFactionId = MainMenu.getCurrentFactionId();
        if (currentFactionId == null) {
            System.err.println("❌ Impossible d'obtenir la faction du joueur connecté dans LogisticsPanel!");
            return;
        }

        Map<String, SafeHexDetails> hexGrid = hexRepository.loadSafeAll();
        int playerHexCount = 0;

        for (Map.Entry<String, SafeHexDetails> entry : hexGrid.entrySet()) {
            String hexKey = entry.getKey();
            SafeHexDetails details = entry.getValue();

            if (hexKey != null && details != null) {
                // ✅ NOUVEAU: Vérifier si l'hexagone appartient à la faction du joueur
                if (currentFactionId.equals(details.getFactionClaim())) {
                    hexCombo.addItem(hexKey);
                    playerHexCount++;
                }
            }
        }

        System.out.println("🎯 LogisticsPanel: " + playerHexCount + " hexagones chargés pour la faction " + currentFactionId);
    }

    private void showAddVehicleDialog(ActionEvent e) {
        if (selectedHexKey == null || selectedHexKey.equals("-- Sélectionner --")) {
            JOptionPane.showMessageDialog(this,
                    "⚠️ Sélectionnez d'abord un hexagone",
                    "Sélection requise",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        TransportVehicle.VehicleType[] vehicleTypes = TransportVehicle.VehicleType.values();
        TransportVehicle.VehicleType selected = (TransportVehicle.VehicleType)
                JOptionPane.showInputDialog(
                        this,
                        "🚚 Choisissez le type de véhicule:",
                        "Ajouter Véhicule",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        vehicleTypes,
                        vehicleTypes.length > 1 ? vehicleTypes[1] : vehicleTypes[0] // CHARRETTE par défaut
                );

        if (selected != null && selected != TransportVehicle.VehicleType.NONE) {
            TransportVehicle vehicle = new TransportVehicle(selected, selectedHexKey);
            if (logisticsService.assignVehicle(selectedHexKey, vehicle)) {
                updateLogisticsDisplay();
                JOptionPane.showMessageDialog(this,
                        "✅ Véhicule " + selected.name() + " ajouté avec succès!",
                        "Succès",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "❌ Impossible d'assigner le véhicule",
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void removeSelectedVehicle(ActionEvent e) {
        String selectedVehicle = vehicleList.getSelectedValue();
        if (selectedVehicle == null) {
            JOptionPane.showMessageDialog(this,
                    "⚠️ Sélectionnez d'abord un véhicule à retirer",
                    "Sélection requise",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int result = JOptionPane.showConfirmDialog(this,
                "Êtes-vous sûr de vouloir retirer ce véhicule ?\n" + selectedVehicle,
                "Confirmation",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            // ✅ SIMPLIFIÉ: Utiliser seulement le service et le repository
            boolean removed = logisticsService.removeVehicleFromHex(selectedHexKey, selectedVehicle);

            if (removed) {
                updateLogisticsDisplay();
                JOptionPane.showMessageDialog(this,
                        "✅ Véhicule retiré avec succès!",
                        "Succès",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "❌ Erreur lors du retrait du véhicule",
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateLogisticsDisplay() {
        if (selectedHexKey == null || selectedHexKey.equals("-- Sélectionner --")) {
            clearDisplay();
            return;
        }

        // ✅ CORRECTION: Toujours récupérer des données fraîches
        SafeHexDetails hex = hexRepository.getHexDetails(selectedHexKey);
        if (hex != null) {
            updateVehicleList(hex);
            updateWarehouseInfo();
            updateTransportTimes();
        } else {
            clearDisplay();
            System.err.println("❌ Hex non trouvé: " + selectedHexKey);
        }
    }

    private void clearDisplay() {
        vehicleListModel.clear();
        warehouseLocationLabel.setText("-");
        warehouseCapacityLabel.setText("-");
        warehouseUsageLabel.setText("-");
        transportTimesArea.setText("");
    }

    private void updateVehicleList(SafeHexDetails hex) {
        vehicleListModel.clear();

        // ✅ TOUJOURS récupérer depuis HexDetails via repository
        SafeHexDetails freshHex = hexRepository.getHexDetails(selectedHexKey);
        if (freshHex == null) {
            vehicleListModel.addElement("Hex non trouvé");
            return;
        }

        List<TransportVehicle> vehicles = freshHex.getAssignedVehicles();
        if (vehicles.isEmpty()) {
            vehicleListModel.addElement("Aucun véhicule assigné");
        } else {
            for (TransportVehicle vehicle : vehicles) {
                String vehicleInfo = String.format("%s (Speed: %.1fx)",
                        vehicle.getType().name(),
                        vehicle.getSpeedMultiplier());
                vehicleListModel.addElement(vehicleInfo);
            }
        }

        vehicleList.revalidate();
        vehicleList.repaint();
    }

    private void updateWarehouseInfo() {
        StorageWarehouse nearestWarehouse = logisticsService.findNearestWarehouse(selectedHexKey);

        if (nearestWarehouse == null) {
            warehouseLocationLabel.setText("Aucun entrepôt trouvé");
            warehouseCapacityLabel.setText("-");
            warehouseUsageLabel.setText("-");
        } else {
            warehouseLocationLabel.setText(nearestWarehouse.getHexKey());
            warehouseCapacityLabel.setText(nearestWarehouse.getMaxCapacity() + " unités");

            int used = nearestWarehouse.getMaxCapacity() - nearestWarehouse.getAvailableSpace();
            double usagePercent = (double) used / nearestWarehouse.getMaxCapacity() * 100;
            warehouseUsageLabel.setText(String.format("%d/%d (%.1f%%)",
                    used, nearestWarehouse.getMaxCapacity(), usagePercent));
        }
    }

    private void updateTransportTimes() {
        if (logisticsService == null) {
            transportTimesArea.setText("Service logistique non disponible");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== TEMPS DE TRANSPORT ===\n");
        sb.append("Depuis: ").append(selectedHexKey).append("\n\n");

        // 🆕 Récupérer les ressources réellement produites
        List<ProducedResource> producedResources = getProducedResources(selectedHexKey);

        if (producedResources.isEmpty()) {
            sb.append("⚠️ Aucune ressource produite dans cet hexagone\n");
            sb.append("Veuillez configurer la production dans l'onglet Production\n\n");

            // Fallback vers ressources par défaut pour tests
            sb.append("📋 Exemples avec ressources standard:\n");
            sb.append("─".repeat(40)).append("\n");
            calculateDefaultTransportTimes(sb);
        } else {
            sb.append("📦 Ressources produites localement:\n");
            for (ProducedResource resource : producedResources) {
                sb.append("• ").append(resource).append("\n");
            }
            sb.append("\n");

            // Calculer temps pour les vraies ressources
            calculateRealTransportTimes(sb, producedResources);
        }

        transportTimesArea.setText(sb.toString());
    }
    private void calculateRealTransportTimes(StringBuilder sb, List<ProducedResource> producedResources) {
        Map<String, StorageWarehouse> warehouses = logisticsService.getWarehouses();

        if (warehouses.isEmpty()) {
            sb.append("⚠️ Aucun entrepôt configuré\n");
            return;
        }

        sb.append("🎯 Temps de transport vers les entrepôts:\n");
        sb.append("─".repeat(50)).append("\n");

        for (StorageWarehouse warehouse : warehouses.values()) {
            String destination = warehouse.getHexKey();
            if (!destination.equals(selectedHexKey)) {
                sb.append(String.format("📍 Vers %s:\n", destination));

                for (ProducedResource resource : producedResources) {
                    // Calculer pour une semaine de production
                    double weeklyQuantity = resource.weeklyProduction;

                    int transportTime = logisticsService.calculateTransportTime(
                            selectedHexKey, destination, resource.resourceType, weeklyQuantity);
                    int joursParTours = 7;
                    if (transportTime < Integer.MAX_VALUE) {
                        sb.append(String.format("  • %s (%.1f unités): %d tours\n" ,
                                resource.resourceType, weeklyQuantity, transportTime/joursParTours));

                        // Calculer rentabilité
                        if (transportTime > 0) {
                            double efficiency = (weeklyQuantity * 7.0) / transportTime;
                            sb.append(String.format("    → Efficacité: %.1f unités/jour\n", efficiency));
                        }
                    } else {
                        sb.append(String.format("  • %s: Route inaccessible\n", resource.resourceType));
                    }
                }
                sb.append("\n");
            }
        }
    }
    private void calculateDefaultTransportTimes(StringBuilder sb) {
        Map<String, StorageWarehouse> warehouses = logisticsService.getWarehouses();

        if (warehouses.isEmpty()) {
            sb.append("Aucun entrepôt configuré\n");
            return;
        }

        // Ressources de test avec quantités réalistes
        String[] testResources = {"no ressources produced"};
        double[] testQuantities = {0.0}; // Production hebdomadaire typique

        for (StorageWarehouse warehouse : warehouses.values()) {
            String destination = warehouse.getHexKey();
            if (!destination.equals(selectedHexKey)) {
                sb.append(String.format("→ %s:\n", destination));

                for (int i = 0; i < testResources.length; i++) {
                    int transportTime = logisticsService.calculateTransportTime(
                            selectedHexKey, destination, testResources[i], testQuantities[i]);
                    int joursParTours = 7;
                    if (transportTime < Integer.MAX_VALUE) {
                        sb.append(String.format("  %s (%.0f): 0 tours\n",
                                testResources[i], testQuantities[i]));
                    } else {
                        sb.append(String.format("  %s: Inaccessible\n", testResources[i]));
                    }
                }
                sb.append("\n");
            }
        }
    }
    private void showTransportNetwork() {
        // Afficher les informations du réseau de transport dans une nouvelle fenêtre
        JDialog networkDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Réseau de Transport", true);
        networkDialog.setSize(600, 400);
        networkDialog.setLocationRelativeTo(this);

        JTextArea networkArea = new JTextArea();
        networkArea.setEditable(false);
        networkArea.setBackground(Color.BLACK);
        networkArea.setForeground(Color.GREEN);
        networkArea.setFont(new Font("Courier New", Font.PLAIN, 12));

        // Debug info du réseau
        StringBuilder networkInfo = new StringBuilder();
        networkInfo.append("=== RÉSEAU DE TRANSPORT DEBUG ===\n\n");

        Map<String, List<TransportVehicle>> vehiclesByHex = logisticsService.getVehiclesByHex();
        networkInfo.append("Véhicules par hexagone:\n");
        for (Map.Entry<String, List<TransportVehicle>> entry : vehiclesByHex.entrySet()) {
            networkInfo.append(entry.getKey()).append(": ").append(entry.getValue().size()).append(" véhicules\n");
        }

        networkInfo.append("\nEntrepôts:\n");
        Map<String, StorageWarehouse> warehouses = logisticsService.getWarehouses();
        for (StorageWarehouse warehouse : warehouses.values()) {
            networkInfo.append(warehouse.getHexKey()).append(": Capacité ").append(warehouse.getMaxCapacity()).append("\n");
        }

        networkArea.setText(networkInfo.toString());

        JScrollPane scrollPane = new JScrollPane(networkArea);
        networkDialog.add(scrollPane);

        // Imprimer aussi dans la console pour debug
        logisticsService.printTransportNetwork();

        networkDialog.setVisible(true);
    }

    // Méthode publique pour actualiser depuis l'extérieur
    public void refreshData() {
        System.out.println("🔄 Rafraîchissement des données logistics...");

        // Recharger la liste des hexagones
        loadInitialData();

        // Si un hex était sélectionné, le garder sélectionné
        if (selectedHexKey != null && !selectedHexKey.equals("-- Sélectionner --")) {
            // Vérifier si l'hex existe toujours
            boolean hexExists = false;
            for (int i = 0; i < hexCombo.getItemCount(); i++) {
                if (selectedHexKey.equals(hexCombo.getItemAt(i))) {
                    hexExists = true;
                    break;
                }
            }

            if (hexExists) {
                hexCombo.setSelectedItem(selectedHexKey);
            } else {
                selectedHexKey = null;
                hexCombo.setSelectedItem("-- Sélectionner --");
            }
        }

        // Rafraîchir l'affichage
        updateLogisticsDisplay();

        System.out.println("✅ Rafraîchissement terminé");
    }

    // Méthode pour sélectionner un hex spécifique (pour l'intégration Mondes)
    public void selectHex(String hexKey) {
        hexCombo.setSelectedItem(hexKey);
        selectedHexKey = hexKey;
        updateLogisticsDisplay();
    }
    private List<ProducedResource> getProducedResources(String hexKey) {
        List<ProducedResource> producedResources = new ArrayList<>();

        try {
            SafeHexDetails hex = hexRepository.getHexDetails(hexKey);
            if (hex == null) return producedResources;

            // Vérifier production du bâtiment principal
            String mainResourceType = hex.getSelectedResourceType("main");
            double mainProduction = hex.getSelectedResourceProduction("main");
            if (mainResourceType != null && mainProduction > 0) {
                producedResources.add(new ProducedResource(
                        mainResourceType, mainProduction, "Main Building"));
            }

            // Vérifier production du bâtiment auxiliaire
            String auxResourceType = hex.getSelectedResourceType("aux");
            double auxProduction = hex.getSelectedResourceProduction("aux");
            if (auxResourceType != null && auxProduction > 0) {
                producedResources.add(new ProducedResource(
                        auxResourceType, auxProduction, "Auxiliary Building"));
            }

            // Vérifier production du bâtiment de fortification
            String fortResourceType = hex.getSelectedResourceType("fort");
            double fortProduction = hex.getSelectedResourceProduction("fort");
            if (fortResourceType != null && fortProduction > 0) {
                producedResources.add(new ProducedResource(
                        fortResourceType, fortProduction, "Fort Building"));
            }

            // Vérifier production d'élevage si disponible
            if (hex.getLivestockFarm() != null) {
                // Ajouter logique pour récupérer production élevage
                // selon votre implémentation
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la récupération des ressources produites: " + e.getMessage());
        }

        return producedResources;
    }

    private static class ProducedResource {
        final String resourceType;
        final double weeklyProduction;
        final String source;

        ProducedResource(String resourceType, double weeklyProduction, String source) {
            this.resourceType = resourceType;
            this.weeklyProduction = weeklyProduction;
            this.source = source;
        }

        @Override
        public String toString() {
            return String.format("%s (%.1f/sem, %s)", resourceType, weeklyProduction, source);
        }
    }
}
