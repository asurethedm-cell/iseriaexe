package com.iseria.ui;

import com.iseria.domain.SafeHexDetails;
import com.iseria.service.EconomicDataService;
import com.iseria.domain.DATABASE;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

public class ProductionDialog extends JDialog {

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

    public ProductionDialog(JFrame parent, String hexKey, String buildingType,
                            String buildingName, int currentWorkers,
                            EconomicDataService economicService,
                            SafeHexDetails hex) {

        super(parent, "Configuration Production - " + buildingName , true);
        this.hex = hex;
        this.hexKey = hexKey;
        this.building = UIHelpers.getBuildingFromHex(hex, buildingType);
        this.buildingName = Objects.requireNonNull(building).getBuildName();
        this.buildingType = buildingType;
        this.currentWorkerCount = currentWorkers;
        this.economicService = economicService;
        initializeDialog();
    }

    private void initializeDialog() {
        setSize(1000, 900);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

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
        workerPanel.add(new JLabel("Actuel: " + currentWorkerCount));
        workerPanel.add(new JLabel("→ Nouveau:"));

        workerSpinner = new JSpinner(new SpinnerNumberModel(currentWorkerCount, 0, 100, 1));
        workerPanel.add(workerSpinner);

        gbc.gridx = 1;
        mainPanel.add(workerPanel, gbc);

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
        mainPanel.add(infoResourcePanel, gbc);

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
        updateProductionDetails();
        loadExistingData();
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
    private List<DATABASE.ResourceType> getResourceTypesForBuilding() {
        if (building == null) {
            return Collections.emptyList();
        }
        return DATABASE.getResourcesForBuilding(building);
    }
    private void updateProductionDetails() {
        StringBuilder details = new StringBuilder();
        int newWorkers = (Integer) workerSpinner.getValue();
        DATABASE.ResourceType resourceType = (DATABASE.ResourceType) resourceTypeCombo.getSelectedItem();
        double production = (Double) resourceProductionSpinner.getValue();

        details.append("=== CONFIGURATION ACTUELLE ===\n");
        details.append(String.format("Hexagone: %s\n", hexKey));
        details.append(String.format("Bâtiment: %s (%s)\n", buildingName, buildingType));
        details.append(String.format("Personnel: %d → %d (%+d)\n",
                currentWorkerCount, newWorkers, newWorkers - currentWorkerCount));

        details.append("\n=== PRODUCTION HEBDOMADAIRE ===\n");

        if (resourceType != null && production > 0) {
            details.append(String.format("• %s %s: %.1f unités\n",
                    resourceType.getIcon(), resourceType.getName(), production));
            double valeurEconomique = production * resourceType.getBaseValue();
            details.append(String.format("• Valeur économique: %.1f Po\n", valeurEconomique));
            double salaireParWorker = getSalaryForWorker(buildingType);
            double totalSalaires = newWorkers * salaireParWorker;
            details.append(String.format("• Coût salaires: %.1f Po\n", totalSalaires));
            double profitNet = valeurEconomique - totalSalaires;
            details.append(String.format("• Profit net: %.1f Po\n", profitNet));
            if (newWorkers > 0) {
                double efficacite = production / newWorkers;
                details.append(String.format("• Efficacité: %.2f unités/worker\n", efficacite));
            }
            double productionMax = calculateMaxProduction(resourceType, newWorkers);
            if (productionMax > production) {
                details.append(String.format("• Production max possible: %.1f unités\n", productionMax));
            }
        } else {
            details.append("• Aucune production configurée\n");
        }
        details.append("\n=== IMPACT ÉCONOMIQUE ===\n");
        if (newWorkers != currentWorkerCount) {
            details.append(String.format("• Population totale: %+d\n",
                    newWorkers - currentWorkerCount));
            double foodImpact = (newWorkers - currentWorkerCount) * 0.7; // Consommation moyenne
            details.append(String.format("• Consommation nourriture: %+.1f/sem\n", foodImpact));
        }
        if (resourceType != null && production > 0) {
            details.append(String.format("• Production %s: +%.1f/sem\n",
                    resourceType.getName(), production));
            if ("Nourriture".equals(resourceType.getName())) {
                details.append("• Réduit la faim de la population\n");
            }
        }
        detailsArea.setText(details.toString());
    }
    private double getSalaryForWorker(String buildingType) {
        // TODO Link to true salaryCalc
        String defaultJob = switch (buildingType.toLowerCase()) {
            case "main", "main building" -> "Fermier";
            case "aux", "auxiliary building" -> "Marchand";
            case "fort", "fort building" -> "Garde";
            default -> "Ouvrier";
        };
        return DATABASE.getSalaryForJob(defaultJob);
    }
    private double calculateMaxProduction(DATABASE.ResourceType resource, int workers) {
        if (building == null || resource == null || workers <= 0) {
            return 0.0;
        }
        double efficiency = UIHelpers.getBuildingEfficiency(building);
        return resource.getBaseValue() * workers * efficiency;
    }
    private void confirm(ActionEvent e) {
        selectedWorkerCount = (Integer) workerSpinner.getValue();
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

}
