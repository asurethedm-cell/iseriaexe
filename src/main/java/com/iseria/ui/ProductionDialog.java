package com.iseria.ui;

import com.iseria.domain.HexResourceData;
import com.iseria.domain.SafeHexDetails;
import com.iseria.service.EconomicDataService;
import com.iseria.domain.DATABASE;
import com.iseria.service.PersonnelDataService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.iseria.ui.UIHelpers.configureScrollSpeed;
import static com.iseria.ui.UIHelpers.styleScrollPane;

public class ProductionDialog extends JDialog {
    private final PersonnelDataService personnelService;
    private final List<PersonnelDataService.HiredPersonnel> availableWorkers;
    private String hexKey;
    private String buildingType;
    private String buildingName;
    private int currentWorkerCount;
    private EconomicDataService economicService;
    private JSpinner workerSpinner;
    private JComboBox<DATABASE.ResourceType> resourceTypeCombo;
    private JSpinner resourceProductionSpinner;
    private JTextArea detailsArea;
    private boolean confirmed = false;
    private int selectedWorkerCount = 0;
    private DATABASE.JobBuilding building;
    private SafeHexDetails hex;
    private int maxWorkersAllowed;
    private JTable workersTable;
    private DefaultTableModel workersTableModel;


    private Map<String, Double> currentTurnSalaries = new HashMap<>();
    private Map<DATABASE.ResourceType, Double> resourceModifiers = new HashMap<>();

    public ProductionDialog(JFrame parent, String hexKey, String buildingType, String buildingName,
                            int currentWorkers, EconomicDataService economicService,
                            PersonnelDataService personnelService, SafeHexDetails hex) {

        super(parent, "Configuration Production - " + buildingName, true);
        this.hex = hex;
        this.hexKey = hexKey;
        this.building = UIHelpers.getBuildingFromHex(hex, buildingType);
        this.buildingName = Objects.requireNonNull(building).getBuildName();
        this.buildingType = buildingType;
        this.currentWorkerCount = currentWorkers;
        this.economicService = economicService;
        this.personnelService = personnelService;

        this.maxWorkersAllowed = building.getMaxWorker();
        this.availableWorkers = getAvailableWorkersForBuilding();
        initializeDialog();
        initializeIntegratedData();


    }

    private void initializeDialog() {
        setSize(1200, 800);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Production", prodTab());

        if (personnelService != null) {
            tabbedPane.addTab("Personnel", createPersonnelAssignmentPanel());
        }
        add(tabbedPane, BorderLayout.CENTER);

    }
    private JPanel createPersonnelAssignmentPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Personnel assigné à ce bâtiment
        JPanel assignedPanel = new JPanel(new BorderLayout());
        assignedPanel.setBorder(BorderFactory.createTitledBorder("Personnel Assigné"));

        String[] columns = {"Nom", "Métier", "Salaire", "Performance", "Actions"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
        JTable personnelTable = new JTable(tableModel);

        // Remplir avec le personnel assigné
        List<PersonnelDataService.HiredPersonnel> assigned =
                personnelService.getAssignedPersonnelForHex(MainMenu.getCurrentFactionId(), hexKey)
                        .stream()
                        .filter(p -> buildingType.equals(p.assignedBuilding))
                        .collect(Collectors.toList());

        for (PersonnelDataService.HiredPersonnel personnel : assigned) {
            tableModel.addRow(new Object[]{
                    personnel.name,
                    personnel.workerType.getJobName(),
                    String.format("%.2f Po", personnel.currentSalary),
                    "85%", // TODO: Calcul performance réelle
                    "Gérer"
            });
        }

        JScrollPane scrollPane = new JScrollPane(personnelTable);
        assignedPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout());

        JButton reassignButton = new JButton("↔️ Reassigner");
        reassignButton.addActionListener(e -> showReassignmentDialog());
        controlPanel.add(reassignButton);

        JButton unassignButton = new JButton("⬅️ Désassigner");
        unassignButton.addActionListener(e -> personnelService.unassignPersonnel(String.valueOf(personnelTable)));
        controlPanel.add(unassignButton);

        assignedPanel.add(controlPanel, BorderLayout.SOUTH);
        panel.add(assignedPanel, BorderLayout.CENTER);

        return panel;
    }

    private void showReassignmentDialog() {
        ReassignmentDialog dialog = new ReassignmentDialog((JFrame) SwingUtilities.getWindowAncestor(this), personnelService, hexKey, buildingType);
        dialog.setVisible(true);
    }
    private JPanel createResourceInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Données Ressource"));
        panel.setBackground(new Color(250, 250, 250));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;

        if (building != null) {
            panel.add(new JLabel("Bâtiment: " + building.getBuildName()), gbc);
            gbc.gridy++;
            panel.add(new JLabel("Type: " + buildingType.toUpperCase()), gbc);
            gbc.gridy++;
            panel.add(new JLabel("Ressources disponibles:"), gbc);

            List<DATABASE.ResourceType> resources = getResourceTypesForBuilding();
            if (resources.isEmpty()) {
                gbc.gridy++;
                JLabel noResourceLabel = new JLabel("Aucune ressource configurée");
                noResourceLabel.setForeground(Color.ORANGE);
                noResourceLabel.setFont(new Font("Arial", Font.ITALIC, 10));
                panel.add(noResourceLabel, gbc);
            } else {
                for (DATABASE.ResourceType resource : resources) {
                    JLabel resourceInfo = new JLabel(String.format("%s %s (Valeur base: %.1f)",
                            resource.getIcon(), resource.getName(), resource.getBaseValue()));
                    resourceInfo.setFont(new Font("Arial", Font.PLAIN, 10));
                    gbc.gridy++;
                    panel.add(resourceInfo, gbc);
                }
            }
            if (building instanceof DATABASE.MainBuilding || building instanceof DATABASE.AuxBuilding) {
                double efficiency = UIHelpers.getBuildingEfficiency(building) * 100;
                gbc.gridy++;
                JLabel efficiencyLabel = new JLabel(String.format("Efficacité bâtiment: %.0f%%", efficiency));
                efficiencyLabel.setFont(new Font("Arial", Font.BOLD, 10));
                efficiencyLabel.setForeground(new Color(0, 120, 0));
                panel.add(efficiencyLabel, gbc);
            }
        } else {
            panel.add(new JLabel("Aucun bâtiment (slot libre)"), gbc);
            gbc.gridy++;
            JLabel noProductionLabel = new JLabel("Pas de production possible");
            noProductionLabel.setForeground(Color.RED);
            noProductionLabel.setFont(new Font("Arial", Font.ITALIC, 10));
            panel.add(noProductionLabel, gbc);
        }
        return panel;
    }
    private void confirm(ActionEvent e) {
        selectedWorkerCount = getSelectedWorkers().size();

        if (selectedWorkerCount == 0) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Aucun travailleur sélectionné. Continuer sans assignation ?",
                    "Confirmation", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        // **ASSIGNER LES TRAVAILLEURS SÉLECTIONNÉS**
        List<PersonnelDataService.HiredPersonnel> selectedWorkers = getSelectedWorkers();
        boolean allAssigned = true;

        for (PersonnelDataService.HiredPersonnel worker : selectedWorkers) {
            boolean assigned = personnelService.assignPersonnelToBuilding(
                    worker.personnelId, hexKey, buildingType, building);
            if (!assigned) {
                allAssigned = false;
                System.err.println("Échec de l'assignation pour: " + worker.name);
            }
        }

        if (!allAssigned) {
            JOptionPane.showMessageDialog(this,
                    "Certains travailleurs n'ont pas pu être assignés.",
                    "Avertissement", JOptionPane.WARNING_MESSAGE);
        }


        DATABASE.ResourceType selectedResourceType = (DATABASE.ResourceType) resourceTypeCombo.getSelectedItem();
        double selectedResourceProduction = (Double) resourceProductionSpinner.getValue();
        confirmed = true;
        if (hex != null) {
            hex.setWorkerCountByType(buildingType, selectedWorkerCount);
            String resourceTypeName = selectedResourceType != null ? selectedResourceType.getName() : null;
            hex.setSelectedResourceType(buildingType, resourceTypeName);
            hex.setSelectedResourceProduction(buildingType, selectedResourceProduction);
            System.out.println("Production sauvegardée:");
            System.out.println("  Hex: " + hexKey);
            System.out.println("  Building: " + buildingType);
            System.out.println("  Workers: " + selectedWorkerCount);
            System.out.println("  Resource: " + resourceTypeName);
            System.out.println("  Production: " + selectedResourceProduction);
        }
        if (economicService != null) {
            economicService.updateWorkerCount(hexKey, buildingType, selectedWorkerCount);
            if (selectedResourceProduction > 0 && selectedResourceType != null) {
                Map<String, Double> production = new HashMap<>();
                production.put(selectedResourceType.getName(), selectedResourceProduction);
                economicService.updateResourceProduction(hexKey, production);
            }
        }
        dispose();
    }
    private void cancel(ActionEvent e) {
        confirmed = false;
        dispose();
    }
    private void loadExistingData() {
        if (hex != null) {
            String savedResourceType = hex.getSelectedResourceType(buildingType);
            Double savedProduction = hex.getSelectedResourceProduction(buildingType);

            if (savedResourceType != null) {
                for (int i = 0; i < resourceTypeCombo.getItemCount(); i++) {
                    DATABASE.ResourceType item = resourceTypeCombo.getItemAt(i);
                    if (item != null && item.getName().equals(savedResourceType)) {
                        resourceTypeCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
            if (savedProduction != null && savedProduction > 0) {
                resourceProductionSpinner.setValue(savedProduction);
            }
            System.out.println("Données chargées:");
            System.out.println("  Resource: " + savedResourceType);
            System.out.println("  Production: " + savedProduction);
        }
    }
    public boolean isConfirmed() { return confirmed; }
    public int getSelectedWorkerCount() { return selectedWorkerCount; }
    private void initializeIntegratedData() {
        try {
            for (DATABASE.Workers worker : DATABASE.Workers.values()) {
                currentTurnSalaries.put(worker.getJobName(), worker.getCurrentSalary());
            }

            List<DATABASE.ResourceType> availableResources = getResourceTypesForBuilding();
            for (DATABASE.ResourceType resource : availableResources) {
                double modifier = economicService != null ?
                        economicService.getResourceProductionModifier(hexKey, resource) : 1.0;
                resourceModifiers.put(resource, modifier);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation des données intégrées: " + e.getMessage());
            initializeFallbackData();
        }
    }
    private void initializeFallbackData() {
        for (DATABASE.Workers worker : DATABASE.Workers.values()) {
            currentTurnSalaries.put(worker.getJobName(), worker.getBaseSalary());
        }
    }
    private double getSalaryForWorker(String buildingType) {
        try {
            String optimalJob = determineOptimalWorkerType(buildingType);
            if (currentTurnSalaries.containsKey(optimalJob)) {
                return currentTurnSalaries.get(optimalJob);
            }

            // **ÉTAPE 3** - Utiliser DATABASE pour les calculs réels
            return DATABASE.getSalaryForJob(optimalJob);

        } catch (Exception e) {
            System.err.println("Erreur lors du calcul de salaire: " + e.getMessage());
            // Fallback sécurisé
            return DATABASE.getSalaryForJob("Ouvrier");
        }
    }
    private String determineOptimalWorkerType(String buildingType) {
        if (building == null) return "Ouvrier";
        Set<String> compatibleJobs = DATABASE.getJobsForBuilding(building);
        if (!compatibleJobs.isEmpty()) {
            return compatibleJobs.iterator().next();
        }
        return switch (buildingType.toLowerCase()) {
            case "main", "main building" -> {
                if (building instanceof DATABASE.MainBuilding mb) {
                    String name = mb.getBuildName().toLowerCase();
                    if (name.contains("mine")) yield "Compagnie de Mineur";
                    if (name.contains("ferme")) yield "Fermier libre";
                    if (name.contains("camp")) yield "Bcheron";
                }
                yield "Fermier libre";
            }
            case "aux", "auxiliary building" -> {
                if (building instanceof DATABASE.AuxBuilding ab) {
                    String name = ab.getBuildName().toLowerCase();
                    if (name.contains("moulin")) yield "Meunier";
                    if (name.contains("atelier")) yield "Artisan";
                    if (name.contains("cuisine")) yield "Cuisinier";
                }
                yield "Artisan";
            }
            case "fort", "fort building" -> "Garde";
            default -> "Ouvrier";
        };
    }
    private double calculateMaxProduction(DATABASE.ResourceType resource, int workers) {
        if (building == null || resource == null || workers <= 0) return 0.0;

        try {
            double efficiency = UIHelpers.getBuildingEfficiency(building);

            // **CORRECTION** - Utiliser la méthode existante au lieu de la méthode manquante
            double baseProduction = resource.getBaseValue();
            double totalProduction = baseProduction * workers * efficiency;

            // Appliquer les modificateurs si disponibles
            double modifier = resourceModifiers.getOrDefault(resource, 1.0);
            double factionBonus = economicService != null ?
                    economicService.getFactionProductionBonus(hexKey, resource) : 1.0;

            return totalProduction * modifier * factionBonus;

        } catch (Exception e) {
            System.err.println("Erreur lors du calcul de production: " + e.getMessage());
            // Fallback sécurisé
            return resource.getBaseValue() * workers * 0.8;
        }
    }
    private List<DATABASE.ResourceType> getResourceTypesForBuilding() {
        if (building == null) return Collections.emptyList();

        try {
            // **CORRECTION** - Utiliser la méthode disponible ou créer une logique simple
            return Arrays.stream(DATABASE.ResourceType.values())
                    .filter(resource -> canBuildingProduceResource(building, resource))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des ressources: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    private boolean canBuildingProduceResource(DATABASE.JobBuilding building, DATABASE.ResourceType resource) {
        // Logique simple basée sur les types de bâtiments
        if (building instanceof DATABASE.MainBuilding) {
            return "Nourriture".equals(resource.getCategory()) || "Matériaux".equals(resource.getCategory());
        } else if (building instanceof DATABASE.AuxBuilding) {
            return "Artisanat".equals(resource.getCategory()) || "Nourriture".equals(resource.getCategory());
        }
        return false;
    }
    private void updateProductionDetails() {
        StringBuilder details = new StringBuilder();
        int newWorkers;
        if (getSelectedWorkers() != null) {
            newWorkers = getSelectedWorkers().size();
        }
        else {newWorkers = 0;}
        DATABASE.ResourceType resourceType = (DATABASE.ResourceType) resourceTypeCombo.getSelectedItem();
        double production = (Double) resourceProductionSpinner.getValue();

        details.append("=== CONFIGURATION ACTUELLE ===\n");
        details.append(String.format("Hexagone: %s\n", hexKey));
        details.append(String.format("Bâtiment: %s (%s)\n", buildingName, buildingType));

        details.append(String.format("Personnel: %d -> %d (%+d)\n", currentWorkerCount, newWorkers, newWorkers - currentWorkerCount));

        details.append("\n=== PRODUCTION HEBDOMADAIRE ===\n");
        if (resourceType != null && production > 0) {
            details.append(String.format("%s %s: %.1f unités\n", resourceType.getIcon(), resourceType.getName(), production));

            // **UTILISER VALEURS INTÉGRÉES** au lieu de getBaseValue direct
            double realValue = economicService != null ?
                    economicService.getResourceMarketValue(resourceType) : resourceType.getBaseValue();
            double valeurEconomique = production * realValue;
            details.append(String.format("Valeur économique: %.1f Po\n", valeurEconomique));

            // **CALCUL DE SALAIRE INTÉGRÉ**
            String optimalJob = determineOptimalWorkerType(buildingType);
            double salaireParWorker = getSalaryForWorker(buildingType);
            double totalSalaires = newWorkers * salaireParWorker;
            details.append(String.format("Coût salaires (%s): %.1f Po\n", optimalJob, totalSalaires));

            double profitNet = valeurEconomique - totalSalaires;
            details.append(String.format("Profit net: %.1f Po\n", profitNet));

            if (newWorkers > 0) {
                double efficacite = production / newWorkers;
                details.append(String.format("Efficacité: %.2f unités/worker\n", efficacite));

                // **UTILISER CALCUL INTÉGRÉ** pour production max
                double productionMax = calculateMaxProduction(resourceType, newWorkers);
                if (productionMax > production) {
                    details.append(String.format("Production max possible: %.1f unités\n", productionMax));
                }
            }
        } else {
            details.append("Aucune production configurée\n");
        }

        details.append("\n=== IMPACT ÉCONOMIQUE ===\n");
        if (newWorkers != currentWorkerCount) {
            details.append(String.format("Population totale: %+d\n", newWorkers - currentWorkerCount));

            // **UTILISER DATABASE** pour consommation nourriture
            String optimalJob = determineOptimalWorkerType(buildingType);
            double foodConsumption = DATABASE.getFoodConsumptionForJob(optimalJob) * (newWorkers - currentWorkerCount);
            details.append(String.format("Consommation nourriture: %.1f/sem\n", foodConsumption));
        }

        if (resourceType != null && production > 0) {
            details.append(String.format("Production %s: %.1f/sem\n", resourceType.getName(), production));
            if ("Nourriture".equals(resourceType.getName())) {
                details.append("-> Réduit la faim de la population\n");
            }
        }

        detailsArea.setText(details.toString());
    }
    private JPanel createResourceStatePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("État des Ressources"));
        panel.setBackground(new Color(60, 60, 60, 200));

        HexResourceData resourceData = economicService.getHexResourceData(hexKey);
        if (resourceData == null) {
            panel.add(new JLabel("Aucune donnée d'état disponible", SwingConstants.CENTER));
            return panel;
        }

        JTextArea statusArea = new JTextArea(8, 40);
        statusArea.setText(resourceData.getDetailedStatusReport());
        statusArea.setEditable(false);
        statusArea.setBackground(new Color(245, 245, 245));
        statusArea.setFont(new Font("Monospaced", Font.PLAIN, 11));

        JScrollPane scrollPane = new JScrollPane(statusArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Panel de maintenance
        JPanel maintenancePanel = new JPanel(new FlowLayout());
        maintenancePanel.setBackground(new Color(60, 60, 60, 200));

        JButton maintenanceButton = new JButton("🔧 Effectuer Maintenance");
        maintenanceButton.addActionListener(e -> performMaintenanceAction(resourceData));
        maintenancePanel.add(maintenanceButton);

        panel.add(maintenancePanel, BorderLayout.SOUTH);

        return panel;
    }
    private JPanel prodTab(){
        setSize(800, 700);
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel titleLabel = new JLabel("Configuration: " + buildingName);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);

        gbc.gridwidth = 1; gbc.gridy++;
        JLabel workerLabel = new JLabel("Personnel:");
        workerLabel.setFont(new Font("Arial", Font.BOLD, 12));
        gbc.gridx = 0;
        mainPanel.add(workerLabel, gbc);

        JPanel workerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gbc.gridy++;
        gbc.gridx = 0; gbc.gridwidth = 5;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0; gbc.weighty = 1;
        JPanel workerSelectionPanel = createAvailableWorkersPanel();
        mainPanel.add(workerSelectionPanel, gbc);


        gbc.gridx = 1;
        mainPanel.add(workerPanel, gbc);
        gbc.gridx = 2;

        gbc.gridy++; gbc.gridx = 0;
        JLabel resourceLabel = new JLabel("Production Ressource:");
        resourceLabel.setFont(new Font("Arial", Font.BOLD, 12));
        mainPanel.add(resourceLabel, gbc);

        JPanel resourcePanel = new JPanel(new GridBagLayout());
        GridBagConstraints rgbc = new GridBagConstraints();
        rgbc.insets = new Insets(2, 5, 2, 5);

        rgbc.gridx = 0; rgbc.gridy = 0;
        resourcePanel.add(new JLabel("Type:"), rgbc);

        resourceTypeCombo = new JComboBox<>();
        resourceTypeCombo.addItem(null);

        List<DATABASE.ResourceType> availableResources = getResourceTypesForBuilding();
        for (DATABASE.ResourceType resource : availableResources) {
            resourceTypeCombo.addItem(resource);
        }

        resourceTypeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof DATABASE.ResourceType resource) {
                    setText(resource.getIcon() + " " + resource.getName());
                } else if (value == null) {
                    setText("Aucune");
                }
                return this;
            }
        });

        resourceTypeCombo.addActionListener(e -> updateProductionDetails());
        rgbc.gridx = 1;
        resourcePanel.add(resourceTypeCombo, rgbc);

        rgbc.gridx = 0; rgbc.gridy = 1;
        resourcePanel.add(new JLabel("Quantité/sem:"), rgbc);

        resourceProductionSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1000.0, 0.1));
        resourceProductionSpinner.setPreferredSize(new Dimension(100, 25));
        resourceProductionSpinner.addChangeListener(e -> updateProductionDetails());
        rgbc.gridx = 1;
        resourcePanel.add(resourceProductionSpinner, rgbc);

        rgbc.gridx = 0; rgbc.gridy = 2;
        double efficiency = UIHelpers.getBuildingEfficiency(building) * 100;
        resourcePanel.add(new JLabel(String.format("Efficacité bâtiment: %.0f%%", efficiency)));

        JLabel efficiencyLabel = new JLabel("100%");
        efficiencyLabel.setToolTipText("Efficacité de production basée sur les workers et le bâtiment");
        rgbc.gridx = 1;
        resourcePanel.add(efficiencyLabel, rgbc);

        gbc.gridx = 1;
        mainPanel.add(resourcePanel, gbc);

        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 1;
        JLabel infoLabel = new JLabel("Informations Ressource:");
        infoLabel.setFont(new Font("Arial", Font.BOLD, 12));
        mainPanel.add(infoLabel, gbc);

        gbc.gridy++;
        JPanel infoResourcePanel = createResourceInfoPanel();
        JScrollPane infoResourcescrollPane = new JScrollPane(infoResourcePanel);
        infoResourcescrollPane.setPreferredSize(new Dimension(300, 200)); // adjust as needed
        infoResourcescrollPane.setBorder(BorderFactory.createEmptyBorder());
        styleScrollPane(infoResourcescrollPane);
        configureScrollSpeed(infoResourcescrollPane, 20,80);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        mainPanel.add(infoResourcescrollPane, gbc);

        gbc.gridy = 3 ; gbc.gridx = 1; gbc.gridwidth = 2;
        JLabel detailsLabel = new JLabel("Résumé Production:");
        detailsLabel.setFont(new Font("Arial", Font.BOLD, 12));
        mainPanel.add(detailsLabel, gbc);

        gbc.gridy++; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0;
        detailsArea = new JTextArea(8, 50);
        detailsArea.setEditable(false);
        detailsArea.setBackground(new Color(245, 245, 245));
        detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 11));

        JScrollPane scrollPane = new JScrollPane(detailsArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Impact Économique"));
        mainPanel.add(scrollPane, gbc);

        gbc.gridy++;
        JPanel buttonPanel = new JPanel(new FlowLayout());

        JButton confirmButton = new JButton("Confirmer");
        confirmButton.setBackground(new Color(76, 175, 80));
        confirmButton.setForeground(Color.WHITE);
        confirmButton.addActionListener(this::confirm);

        JButton cancelButton = new JButton("Annuler");
        cancelButton.setBackground(new Color(244, 67, 54));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.addActionListener(this::cancel);

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);
        mainPanel.add(buttonPanel, gbc);

        add(mainPanel);
        loadExistingData();
        updateProductionDetails();
        return mainPanel;
    }
    private JPanel createAvailableWorkersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Travailleurs Disponibles"));

        if (availableWorkers.isEmpty()) {
            JLabel noWorkersLabel = new JLabel("Aucun travailleur disponible pour ce type de bâtiment");
            noWorkersLabel.setForeground(Color.BLUE);
            noWorkersLabel.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(noWorkersLabel, BorderLayout.CENTER);
            return panel;
        }

        // **TABLE DES TRAVAILLEURS DISPONIBLES**
        String[] columns = {"Sélectionner", "Nom", "Métier", "Salaire", "Consommation"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                if (column != 0) return false; // Seule la checkbox est éditable

                // **VÉRIFIER LES LIMITES AVANT D'AUTORISER L'ÉDITION**
                boolean currentlySelected = (Boolean) getValueAt(row, 0);

                if (!currentlySelected) { // Si pas encore sélectionné
                    int selectedCount = getSelectedWorkers().size();
                    int maxAllowed = maxWorkersAllowed - currentWorkerCount;

                    if (selectedCount >= maxAllowed) {
                        // Afficher un message et empêcher la sélection
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(ProductionDialog.this,
                                    String.format("Capacité maximum atteinte!\n" +
                                                    "Limite: %d travailleurs\n" +
                                                    "Actuellement assignés: %d\n" +
                                                    "Disponible: %d",
                                            maxWorkersAllowed, currentWorkerCount, maxAllowed),
                                    "Limite de Capacité", JOptionPane.WARNING_MESSAGE);
                        });
                        return false;
                    }
                }

                return true;
            }
        };

        JTable workersTable = new JTable(tableModel);
        workersTable.getModel().addTableModelListener(e -> updateWorkerSelection());

        // Remplir la table
        for (PersonnelDataService.HiredPersonnel worker : availableWorkers) {
            tableModel.addRow(new Object[]{
                    false, // Checkbox non cochée par défaut
                    worker.name,
                    worker.workerType.getJobName(),
                    String.format("%.2f Po", worker.currentSalary),
                    String.format("%.1f", worker.foodConsumption)
            });
        }
        JScrollPane scrollPane = new JScrollPane(workersTable);
        styleScrollPane(scrollPane);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Stocker la référence pour usage ultérieur
        this.workersTable = workersTable;
        this.workersTableModel = tableModel;

        return panel;
    }
    private void updateWorkerSelection() {
        if (workersTable == null) return;

        selectedWorkerCount = getSelectedWorkers().size();
        int selectedCount = 0;

        // Compter les travailleurs sélectionnés
        for (int i = 0; i < workersTableModel.getRowCount(); i++) {
            if ((Boolean) workersTableModel.getValueAt(i, 0)) {
                selectedCount++;
            }
        }

        // **DÉSACTIVER LES CHECKBOX SI LIMITE ATTEINTE**
        for (int i = 0; i < workersTableModel.getRowCount(); i++) {
            boolean isSelected = (Boolean) workersTableModel.getValueAt(i, 0);
            if (!isSelected && selectedCount >= maxWorkersAllowed - currentWorkerCount) {
                // Désactiver visuellement cette ligne
                workersTable.setRowSelectionAllowed(false);
            }
        }

        updateProductionDetails();
    }
    private List<PersonnelDataService.HiredPersonnel> getAvailableWorkersForBuilding() {

        if (personnelService == null) return Collections.emptyList();
        String faction = MainMenu.getCurrentFactionId();
        List<PersonnelDataService.HiredPersonnel> unassigned = personnelService.getUnassignedPersonnel(faction);
        System.out.println("DEBUG: faction=" + faction + ", unassigned total=" + unassigned.size());
        unassigned.forEach(p -> System.out.println("  • " + p.name + " – " + p.workerType.getJobName()));

        return unassigned.stream()
                .filter(p -> canWorkerWorkInBuilding(p.workerType, building))
                .collect(Collectors.toList());


    }
    private List<PersonnelDataService.HiredPersonnel> getSelectedWorkers() {
        List<PersonnelDataService.HiredPersonnel> selected = new ArrayList<>();
        if(workersTableModel != null) {
            for (int i = 0; i < workersTableModel.getRowCount(); i++) {
                if ((Boolean) workersTableModel.getValueAt(i, 0)) {
                    selected.add(availableWorkers.get(i));
                }
            }
            return selected;
        }
        else return null;
    }
    private boolean canWorkerWorkInBuilding(DATABASE.Workers workerType, DATABASE.JobBuilding building) {
        return workerType.getWorkBuildings().contains(building);
    }
    private void performMaintenanceAction(HexResourceData resourceData) {
        List<String> recommendations = resourceData.getMaintenanceRecommendations();

        if (recommendations.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Aucune maintenance requise actuellement.",
                    "Maintenance", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Dialog de sélection de maintenance
        String[] options = recommendations.toArray(new String[0]);
        String selected = (String) JOptionPane.showInputDialog(this,
                "Sélectionnez l'élément à maintenir:",
                "Maintenance",
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (selected != null) {
            // Calculer le coût et effectuer la maintenance
            double cost = calculateMaintenanceCost(selected);

            int confirm = JOptionPane.showConfirmDialog(this,
                    String.format("Maintenance coûtera %.0f Po. Continuer?", cost),
                    "Confirmer Maintenance",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                // TODO: Effectuer la maintenance via EconomicDataService
                boolean success = economicService.performHexMaintenance(hexKey, null, cost);

                if (success) {
                    JOptionPane.showMessageDialog(this,
                            "Maintenance effectuée avec succès!",
                            "Maintenance", JOptionPane.INFORMATION_MESSAGE);
                    updateProductionDetails(); // Rafraîchir l'affichage
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Maintenance échouée (fonds insuffisants?)",
                            "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    private double calculateMaintenanceCost(String maintenanceItem) {
        // Calculer le coût basé sur le type de maintenance
        if (maintenanceItem.contains("bâtiment")) {
            return 500.0; // Coût fixe pour maintenance de bâtiment
        } else {
            return 200.0; // Coût pour maintenance de ressource
        }
    }

    static class ReassignmentDialog extends JDialog {

        private final PersonnelDataService personnelService;
        private final String currentHexKey;
        private final String currentBuildingType;

        public ReassignmentDialog(JFrame parent, PersonnelDataService personnelService,
                                  String hexKey, String buildingType) {
            super(parent, "Reassignation du Personnel", true);
            this.personnelService = personnelService;
            this.currentHexKey = hexKey;
            this.currentBuildingType = buildingType;

            initializeDialog();
        }

        private void initializeDialog() {
            setSize(600, 400);
            setLocationRelativeTo(getParent());

            JPanel mainPanel = new JPanel(new BorderLayout());

            // Interface de reassignation
            JLabel titleLabel = new JLabel("Sélectionnez la nouvelle destination", SwingConstants.CENTER);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
            mainPanel.add(titleLabel, BorderLayout.NORTH);

            // TODO: Implémenter interface complète
            JTextArea infoArea = new JTextArea("Interface de reassignation à implémenter:\n" +
                    "- Liste des hexagones disponibles\n" +
                    "- Sélection du type de bâtiment\n" +
                    "- Validation des capacités");
            mainPanel.add(new JScrollPane(infoArea), BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout());
            JButton confirmButton = new JButton("Confirmer");
            JButton cancelButton = new JButton("Annuler");

            confirmButton.addActionListener(e -> {
                // TODO: Implémenter logique de reassignation
                dispose();
            });

            cancelButton.addActionListener(e -> dispose());

            buttonPanel.add(confirmButton);
            buttonPanel.add(cancelButton);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);

            add(mainPanel);
        }
    }
}
