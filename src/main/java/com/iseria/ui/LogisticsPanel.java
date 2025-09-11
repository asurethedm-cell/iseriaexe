package com.iseria.ui;

import com.iseria.domain.*;
import com.iseria.service.LogisticsService;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
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
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.CYAN, 1),
                "🚗 Véhicules Assignés",
                0, 0,
                new Font("Arial", Font.BOLD, 14),
                Color.CYAN));
        panel.setOpaque(true);
        panel.setBackground(new Color(20, 40, 60, 200));

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
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.MAGENTA, 1),
                "⏱️ Temps de Transport",
                0, 0,
                new Font("Arial", Font.BOLD, 14),
                Color.MAGENTA));
        panel.setOpaque(true);
        panel.setBackground(new Color(40, 20, 60, 200));

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
        refreshButton.addActionListener(e -> updateLogisticsDisplay());
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

        Map<String, HexDetails> hexGrid = hexRepository.loadAll();
        int playerHexCount = 0;

        for (Map.Entry<String, HexDetails> entry : hexGrid.entrySet()) {
            String hexKey = entry.getKey();
            HexDetails details = entry.getValue();

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
        HexDetails hex = hexRepository.getHexDetails(selectedHexKey);
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

    private void updateVehicleList(HexDetails hex) {
        vehicleListModel.clear();

        // ✅ TOUJOURS récupérer depuis HexDetails via repository
        HexDetails freshHex = hexRepository.getHexDetails(selectedHexKey);
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

        // Exemple de calculs vers quelques destinations importantes
        Map<String, StorageWarehouse> warehouses = logisticsService.getWarehouses();

        if (warehouses.isEmpty()) {
            sb.append("Aucun entrepôt configuré\n");
        } else {
            sb.append("Vers les entrepôts:\n");
            sb.append("─".repeat(25)).append("\n");

            for (StorageWarehouse warehouse : warehouses.values()) {
                String destination = warehouse.getHexKey();
                if (!destination.equals(selectedHexKey)) {
                    // Exemple avec ressource standard
                    int transportTime = logisticsService.calculateTransportTime(
                            selectedHexKey, destination, "nourriture", 10.0);

                    if (transportTime < Integer.MAX_VALUE) {
                        sb.append(String.format("→ %s: %d jours\n", destination, transportTime));
                    } else {
                        sb.append(String.format("→ %s: Pas de route\n", destination));
                    }
                }
            }
        }

        transportTimesArea.setText(sb.toString());
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
}
