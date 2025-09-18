package com.iseria.ui;

import com.iseria.domain.DATABASE;
import com.iseria.domain.IHexRepository;
import com.iseria.domain.SafeHexDetails;
import com.iseria.service.EconomicDataService;
import com.iseria.service.MoralSaveService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.*;

import static com.iseria.ui.MainMenu.currentUserFaction;

public class EconomicPanel extends JPanel

        implements EconomicDataService.EconomicDataObserver {
    private static MoralSaveService moralSaveService = new MoralSaveService();
    private EconomicDataService economicService;
    private JLabel tresorerieLabel, populationLabel, instabiliteLabel, agressiviteLabel, faimLabel;
    private JPanel resourcePanel, salaryPanel;
    private IHexRepository hexRepository;

    public EconomicPanel(EconomicDataService economicService, IHexRepository hexRepository) {
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

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        add(createGestionSocialePanel(), gbc);

        gbc.gridy++;
        add(createTresoreriePanel(), gbc);

        gbc.gridy++;
        salaryPanel = new JPanel(new GridBagLayout());
        TitledBorder salaryPanelBorder = BorderFactory.createTitledBorder("Salaires");
        salaryPanelBorder.setTitleColor(Color.WHITE);
        salaryPanel.setBorder(salaryPanelBorder);
        salaryPanel.setOpaque(true);
        salaryPanel.setBackground(new Color(50, 50, 50, 200));
        add(salaryPanel, gbc);

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

        return Color.BLACK;
    }
    private boolean isRecentlyAdded(String resourceName) {return false; }
    private boolean hasProductionIncreased(String resourceName, double currentProduction) { return false; }

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

