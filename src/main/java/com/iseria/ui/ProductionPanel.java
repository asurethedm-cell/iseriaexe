package com.iseria.ui;

import com.iseria.domain.DATABASE;
import com.iseria.domain.IHexRepository;
import com.iseria.domain.SafeHexDetails;
import com.iseria.service.EconomicDataService;
import com.iseria.service.PersonnelDataService;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.iseria.ui.UIHelpers.hexImageLabels;

public class ProductionPanel extends JScrollPane
        implements EconomicDataService.EconomicDataObserver {
    private JPanel contentPanel;
    private static PersonnelDataService personnelService;
    private Map<String, SafeHexDetails> hexGrid;
    private String factionName;
    private IHexRepository repository;
    private EconomicDataService economicService;
    private WorkDetailsPopup workDetailsPopup;
    private Map<String, JPanel> hexPanels = new HashMap<>();
    private boolean showHexPreview = true;
    public ProductionPanel(Map<String, SafeHexDetails> hexGrid, String factionName,
                           IHexRepository repo, EconomicDataService economicService, PersonnelDataService personnelService) {
        super();
        this.hexGrid = hexGrid;
        this.factionName = factionName;
        this.repository = repo;
        this.economicService = economicService;
        this.personnelService = personnelService;
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
            getParent().add(workDetailsPopup, JLayeredPane.POPUP_LAYER);
        }
    }
    public void configureScrollSpeed() {

        int scrollSpeedMultiplier =10;
        addMouseWheelListener(e -> {
            JScrollBar scrollBar = getVerticalScrollBar();
            int currentValue = scrollBar.getValue();
            int scrollAmount = e.getScrollAmount() * scrollSpeedMultiplier;

            if (e.getWheelRotation() < 0) {
                scrollBar.setValue(currentValue - scrollAmount);
            } else {
                scrollBar.setValue(currentValue + scrollAmount);
            }
        });
    }
    public void refreshContent() {
        contentPanel.removeAll();
        hexPanels.clear();
        JPanel controlPanel = createControlPanel();
        contentPanel.add(controlPanel);

        hexGrid.entrySet().stream()
                .filter(entry -> factionName.equals(entry.getValue().getFactionClaim()))
                .sorted(Map.Entry.comparingByKey(this::compareHexKeys))
                .forEach(entry -> {
                    SafeHexDetails freshHex = repository.getHexDetails(entry.getKey());
                    if (freshHex != null) {
                        hexGrid.put(entry.getKey(), freshHex);
                    }

                    JPanel hexPanel = createEnhancedHexPanel(entry.getKey(),
                            freshHex != null ? freshHex : entry.getValue(),
                            this.repository, personnelService);
                    hexPanels.put(entry.getKey(), hexPanel);
                    contentPanel.add(hexPanel);
                    contentPanel.add(Box.createVerticalStrut(5));
                });
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private int compareHexKeys(String hex1, String hex2) {
        return compareAlphanumeric(hex1, hex2);
    }
    private int compareAlphanumeric(String s1, String s2) {
        try {
            String num1 = s1.replaceAll("\\D+", "");
            String num2 = s2.replaceAll("\\D+", "");

            if (!num1.isEmpty() && !num2.isEmpty()) {
                int n1 = Integer.parseInt(num1);
                int n2 = Integer.parseInt(num2);
                return Integer.compare(n1, n2);
            }
        } catch (NumberFormatException e) {}
        return s1.compareToIgnoreCase(s2);
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
    private JPanel createEnhancedHexPanel(String hexKey, SafeHexDetails hex, IHexRepository repo, PersonnelDataService personnelService) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(hexKey));
        panel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        if (showHexPreview) {
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            JPanel hexPreview = createHexPreview(hexKey, hex, repo);
            panel.add(hexPreview, gbc);
            gbc.fill = GridBagConstraints.CENTER;
            gbc.gridy++;
            gbc.gridwidth = 1;
        }

        String[] buildingTypes = {"main", "aux", "fort"};
        String[] buildingLabels = {"Principal", "Auxiliaire", "Fortification"};

        for (int i = 0; i < buildingTypes.length; i++) {
            gbc.gridx = i;
            if (!showHexPreview) gbc.gridy = 0;
            JPanel buildingPanel = createBuildingProductionPanel(hexKey, hex, buildingTypes[i], buildingLabels[i], personnelService);
            panel.add(buildingPanel, gbc);
        }

        return panel;
    }
    private JPanel createHexPreview(String hexKey, SafeHexDetails hex, IHexRepository repo) {

        JLabel hexImageLabel = hexImageLabels.computeIfAbsent(hexKey, key -> {
            BufferedImage hexSnapshot = HexSnapshotCache.getHexSnapshot(key, repo);
            JLabel hexLabel = new JLabel();
            if (hexSnapshot != null) {
                hexLabel.setIcon(new ImageIcon(hexSnapshot));
            }
            hexLabel.setPreferredSize(new Dimension(100, 100));
            return hexLabel;
        });

        JPanel preview = new JPanel(new GridBagLayout());
        preview.setBorder(BorderFactory.createTitledBorder("Aperçu Hexagone"));
        preview.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.CENTER;

        String buildingNames = UIHelpers.getBuildingNamesSafe(
                hex.getMainBuildingIndex(),
                hex.getAuxBuildingIndex(),
                hex.getFortBuildingIndex()
        );


        JLabel factionName = new JLabel(String.format("Faction: %s", hex.getFactionClaim()));
        factionName.setFont(factionName.getFont().deriveFont(Font.BOLD, 12f));
        factionName.setForeground(new Color(220, 53, 69)); // Rouge pour ressortir
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.CENTER;
        preview.add(factionName, gbc);


        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        preview.add(hexImageLabel, gbc);

        JLabel info = new JLabel(String.format("%s", buildingNames));
        info.setFont(info.getFont().deriveFont(Font.BOLD, 10f));
        info.setForeground(Color.BLACK);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        preview.add(info, gbc);

        return preview;
    }
    private JPanel createBuildingProductionPanel(String hexKey, SafeHexDetails hex,
                                                 String buildingType, String label, PersonnelDataService personnelService) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(label));
        panel.setBackground(new Color(211, 211, 211, 128));
        panel.setOpaque(true);


        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;

        int workerCount = hex.getWorkerCountByType(buildingType);
        gbc.gridx = 0; gbc.gridy = 0;

        JPanel workerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        workerPanel.setOpaque(false);

        JLabel workerLabel = new JLabel("Travailleurs " + workerCount);
        workerPanel.add(workerLabel);

        JButton pullButton = new JButton("↑");
        pullButton.setToolTipText("Retirer des workers");
        pullButton.addActionListener(e -> pullWorkers(hexKey, hex, buildingType));
        workerPanel.add(pullButton);

        JButton addButton = new JButton("↓");
        addButton.setToolTipText("Ajouter des workers");
        addButton.addActionListener(e -> addWorkers(hexKey, hex, buildingType));
        workerPanel.add(addButton);

        panel.add(workerPanel, gbc);

        gbc.gridy = 1;
        JCheckBox lockCheckbox = new JCheckBox("LOCK", hex.isSlotLocked(buildingType));
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

        gbc.gridy = 3;
        gbc.gridx = 0;  // Reset gridx
        gbc.gridwidth = 2;  // Span across 2 columns

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));  // 5px gap
        buttonPanel.setOpaque(false);

        JButton configButton = new JButton("Config");
        configButton.addActionListener(e -> openProductionDialog(hexKey, hex, buildingType, label, personnelService));
        buttonPanel.add(configButton);

        JButton livestockButton = new JButton("Élevage");
        livestockButton.setBackground(new Color(139, 69, 19));
        livestockButton.setForeground(Color.WHITE);
        livestockButton.setVisible(false);
        livestockButton.addActionListener(e -> openLivestockDialog(hexKey, hex));
        if(UIHelpers.isFarmBuilding(hex, buildingType)) {
            livestockButton.setVisible(true);
        }
        buttonPanel.add(livestockButton);

        panel.add(buttonPanel, gbc);
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
        DATABASE.ResourceType[] availableResources = getAvailableResourcesForBuilding(buildingType);

        if (availableResources.length > 0) {
            DATABASE.ResourceType resource = availableResources[0];
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

    private void openProductionDialog(String hexKey, SafeHexDetails hex, String buildingType, String buildingLabel, PersonnelDataService personnelService) {
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
        ProductionDialog dialog = new ProductionDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this),
                hexKey, buildingType, building.getBuildName(),
                hex.getWorkerCountByType(buildingType),
                economicService,
                personnelService,
                hex
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

