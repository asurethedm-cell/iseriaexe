package com.iseria.ui;

import com.iseria.domain.*;
import com.iseria.infra.FactionRegistry;
import com.iseria.infra.MoralCalculator;
import com.iseria.service.EconomicDataService;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.iseria.ui.MainMenu.*;
import static com.iseria.ui.UI.HexSnapshotCache.mapBackground;

public class UIHelpers  extends JScrollPane{

    public static Map<String, JLabel> hexImageLabels = new HashMap<>();
    public static double  moralSum;
    public static <T extends DATABASE.JobBuilding> int addBuildingSection(
            String hexKeyLocal, String label, int buildingIndex, T[] buildingEnum,
            JPanel panel, GridBagConstraints gbc, int row, SafeHexDetails hd, IHexRepository repo) {

        T selectedBuilding = buildingEnum[buildingIndex];
        String buildingName = DATABASE.getBuildNameFromJobBuilding(selectedBuilding);


        JLabel hexImageLabel = hexImageLabels.computeIfAbsent(hexKeyLocal, key -> {
            BufferedImage hexSnapshot = UI.HexSnapshotCache.getHexSnapshot(key, repo);
            JLabel hexLabel = new JLabel();
            if (hexSnapshot != null) {
                hexLabel.setIcon(new ImageIcon(hexSnapshot));
            }
            hexLabel.setPreferredSize(new Dimension(100, 100));
            return hexLabel;
        });

        // Ajouter l'image seulement pour la première section (Main Building)
        if (label.equals("Main Building")) {
            gbc.gridx = 0; gbc.gridy = row;
            gbc.gridheight = 3; // S'étend sur les 3 rangées
            panel.add(hexImageLabel, gbc);
            gbc.gridheight = 1; // Reset
        }

        // Le nom de l'hex à côté
        gbc.gridx =1; gbc.gridy = row;
        panel.add(new JLabel(label + ":"), gbc);

        JButton buildingButton = new JButton(buildingName);
        buildingButton.setPreferredSize(new Dimension(100, 20));
        gbc.gridx++;
        panel.add(buildingButton, gbc);

        // Worker list (existing)
        java.util.List<String> savedWorkers = hd.getWorkers(label.toLowerCase());
        DefaultListModel<String> listModel = new DefaultListModel<>();
        savedWorkers.forEach(listModel::addElement);

        JList<String> workerList = new JList<>(listModel);
        workerList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (isSelected) {
                    label.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                } else {
                    label.setBorder(null);
                }
                return label;
            }
        });

        workerList.setOpaque(true);
        JScrollPane scrollPane = new JScrollPane(workerList);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.setPreferredSize(new Dimension(220, 19));
        gbc.gridx++;
        panel.add(scrollPane, gbc);

        int currentWorkerCount = hd.getWorkerCountByType(label);
        JLabel personnelLabel = new JLabel("Personnel: " + currentWorkerCount);
        personnelLabel.setFont(new Font("Arial", Font.BOLD, 12));
        personnelLabel.setHorizontalAlignment(SwingConstants.CENTER);
        personnelLabel.setPreferredSize(new Dimension(100, 20));
        personnelLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));



        gbc.gridx++;
        panel.add(personnelLabel, gbc);

        JButton validateButton = new JButton("Valider");
        validateButton.setPreferredSize(new Dimension(100, 20));
        gbc.gridx++;
        panel.add(validateButton, gbc);

        buildingButton.addActionListener(e -> {
            System.out.println("---- [BUILDING BUTTON CLICKED] ----");
            System.out.println("Enum: " + selectedBuilding);
            System.out.println("Tag: " + selectedBuilding.getTag());
            System.out.println("Name: " + DATABASE.getBuildNameFromJobBuilding(selectedBuilding));

            listModel.clear();
            Set<String> jobsForBuilding = DATABASE.getJobsForBuilding(selectedBuilding);

            if (jobsForBuilding.isEmpty()) {
                System.out.println("No specific jobs found for this building, using category fallback");
                String buildingType = selectedBuilding.getBuildName().toLowerCase();
                if (buildingType.contains("mine")) {
                    listModel.addElement("Compagnie de Mineur (Récolte)");
                } else if (buildingType.contains("ferme") || buildingType.contains("légume") || buildingType.contains("céréale")) {
                    listModel.addElement("Fermier (serf) (Récolte)");
                    listModel.addElement("Fermier (libre) (Récolte)");
                } else if (buildingType.contains("observatoire")) {
                    listModel.addElement("Astrologiste (Autre)");
                } else {
                    listModel.addElement("Aucun Travailleur");

                }
            } else {
                System.out.println("Found " + jobsForBuilding.size() + " specific jobs for this building:");
                for (String jobName : jobsForBuilding) {
                    String category = findJobCategory(jobName);
                    listModel.addElement(jobName + " (" + category + ")");
                    System.out.println(" - " + jobName + " (" + category + ")");
                }
            }

            System.out.println("Total jobs displayed: " + listModel.getSize());
            System.out.println("-----------------------------------");
        });

        validateButton.addActionListener(e -> {

            JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(panel);
            UI.WorkerSelectionDialog dialog = new UI.WorkerSelectionDialog(
                    parentFrame, label, buildingName, currentWorkerCount);
            if (" ".equals(scrollPane.getViewport()) || "Aucun Travailleur".equals(scrollPane.getViewport()))
            {
                // do nothing if there isnt any worker
            } else {
                dialog.setVisible(true);

                if (dialog.isConfirmed()) {
                    int newCount = dialog.getSelectedCount();
                    hd.setWorkerCountByType(label, newCount);
                    repo.updateHexDetails(hexKeyLocal, hd);

                    // Mettre à jour l'affichage
                    personnelLabel.setText("Personnel: " + newCount);
                    panel.revalidate();
                    panel.repaint();
                }
                int result = JOptionPane.showConfirmDialog(
                        panel,
                        "Confirmer l'affectation du travailleur ?",
                        "Confirmation",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

                if (result == JOptionPane.YES_OPTION) {
                    java.util.List<String> selected = workerList.getSelectedValuesList();
                    int firstVisibleIndex = workerList.getFirstVisibleIndex();
                    int lastVisibleIndex = workerList.getLastVisibleIndex();
                    java.util.List<String> visibleItems = new ArrayList<>();

                    if (firstVisibleIndex != -1 && lastVisibleIndex != -1) {
                        for (int i = firstVisibleIndex; i <= lastVisibleIndex; i++) {
                            visibleItems.add(listModel.getElementAt(i));
                        }
                    }

                    SafeHexDetails details = repo.getHexDetails(hexKeyLocal);
                    String hexKey = details.getHexKey();

                    java.util.List<String> workerSelected = selected.isEmpty() ? visibleItems : selected;

                    details.setWorkers(label.toLowerCase(), workerSelected);
                    details.lockSlot(label);

                    System.out.println("Selected: " + selected);
                    System.out.println("ScrollSelected: " + visibleItems);
                    System.out.println("Final Selection: " + workerSelected);

                    repo.updateHexDetails(hexKey, details);

                    // Disable controls
                    workerList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                    workerList.setFocusable(false);
                    workerList.setSelectionModel(new DefaultListSelectionModel() {
                        @Override public void setSelectionInterval(int index0, int index1) {}
                    });

                    scrollPane.addMouseWheelListener(InputEvent::consume);
                    scrollPane.setWheelScrollingEnabled(false);
                    scrollPane.getVerticalScrollBar().setEnabled(false);
                    scrollPane.getHorizontalScrollBar().setEnabled(false);

                    validateButton.setEnabled(false);
                    buildingButton.setEnabled(false);
                    workerList.setFocusable(false);

                    Login.waitNextTurn = true;
                    panel.revalidate();
                    panel.repaint();
                }}
        });

        boolean shouldLock =  hd.isSlotLocked(label); //TODO enable the following later on ||Login.waitNextTurn
        if (shouldLock) {
            validateButton.setEnabled(false);
            buildingButton.setEnabled(false);
            workerList.setFocusable(false);
            workerList.setSelectionModel(new DefaultListSelectionModel() {
                @Override public void setSelectionInterval(int i0, int i1) {}
            });
            scrollPane.addMouseWheelListener(InputEvent::consume);
            scrollPane.setWheelScrollingEnabled(false);
            scrollPane.getVerticalScrollBar().setEnabled(false);
            scrollPane.getHorizontalScrollBar().setEnabled(false);
            workerList.setBackground(new Color(114, 114, 114, 250));
        }

        return row + 1;
    }
    public static JLabel makeCell(String text, Color bg) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Bahnschrift", Font.BOLD, 15));
        label.setOpaque(true);
        label.setBackground(bg);
        label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        return label;
    }
    public static JLabel createMoralResultLabel(String text) {
        JLabel label = new JLabel(text);
        label.setMinimumSize(new Dimension(600, 40));
        label.setFont(new Font("Oswald", Font.PLAIN, 10));
        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setOpaque(true);
        if (!text.isEmpty()){label.setBackground(Color.lightGray);}
        label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        return label;
    }
    public static BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(UI.class.getResource(path));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    public static JPanel createMainMenu(String menuName, String backgroundImagePath) {
        BufferedImage img = loadImage(backgroundImagePath);
        JPanel menuPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (img != null) {
                    g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        menuPanel.setLayout(new BorderLayout());
        return menuPanel;
    }
    public static JPanel createFactionMenu(String backgroundImagePath, JLayeredPane factionContentPanel) {
        BufferedImage img = loadImage(backgroundImagePath);
        JPanel factionMenuPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (img != null) {
                    g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        factionMenuPanel.setLayout(null);
        factionMenuPanel.add(factionContentPanel);
        return factionMenuPanel;
    }
    public static DefaultListCellRenderer createMoralActionRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value instanceof DATABASE.MoralAction) {
                    DATABASE.MoralAction action = (DATABASE.MoralAction) value;
                    String effectText = action.getMoralEffect() >= 0 ? "+" + action.getMoralEffect() : String.valueOf(action.getMoralEffect());
                    label.setText(action.getName() + " (" + effectText + ")");

                    // Couleur selon le type d'action
                    switch (action.getType()) {
                        case UNIQUE -> label.setForeground(Color.BLUE);
                        case AUTO -> label.setForeground(Color.RED);
                        default -> label.setForeground(Color.BLACK);
                    }
                } else if (value == null) {
                    label.setText("Aucune action");
                    label.setForeground(Color.GRAY);
                }

                if (isSelected) {
                    label.setBackground(Color.LIGHT_GRAY);
                } else {
                    label.setBackground(Color.WHITE);
                }

                return label;
            }
        };
    }
    @SafeVarargs
    public static void attachLiveMoralUpdater(JLabel moralEndValue, JComboBox<DATABASE.MoralAction>... dropdowns) {
        ActionListener listener = e -> {
            try {
                java.util.List<DATABASE.MoralAction> selectedActions = new ArrayList<>();
                for (JComboBox<DATABASE.MoralAction> dropdown : dropdowns) {
                    DATABASE.MoralAction selected = (DATABASE.MoralAction) dropdown.getSelectedItem();
                    if (selected != null) {
                        selectedActions.add(selected);
                    }
                }

                moralSum = MoralCalculator.sumMoral(selectedActions);
                moralEndValue.setText(String.format("%.1f", moralSum));

                if (moralSum == 5.0) {
                    moralEndValue.setBackground(Color.lightGray);
                } else if (moralSum <= 4.0) {
                    moralEndValue.setBackground(new Color(207, 65, 7));
                } else {
                    moralEndValue.setBackground(new Color(65, 186, 61));
                }

            } catch (Exception ex) {
                System.err.println("Erreur lors du calcul du moral : " + ex.getMessage());
                ex.printStackTrace();
                moralSum = 5.0;
                moralEndValue.setText("5.0");
                moralEndValue.setBackground(Color.lightGray);
            }
        };

        for (JComboBox<DATABASE.MoralAction> dropdown : dropdowns) {
            dropdown.addActionListener(listener);
        }
    }
    public static Color getFactionColor(String factionId) {
        return FactionRegistry.getColorFor(factionId);
    }
    public static   JPanel createPopulationSummaryPanel(IHexRepository repo, String factionName) {
        JPanel summaryPanel = new JPanel(new GridBagLayout());
        summaryPanel.setOpaque(false);
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Résumé Population"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Calcul des totaux
        Map<String, SafeHexDetails> hexGrid = repo.loadSafeAll();
        int totalMainWorkers = 0;
        int totalAuxWorkers = 0;
        int totalFortWorkers = 0;
        int totalHexes = 0;

        for (Map.Entry<String, SafeHexDetails> entry : hexGrid.entrySet()) {
            SafeHexDetails hd = entry.getValue();
            if (factionName.equals(hd.getFactionClaim())) {
                totalHexes++;
                totalMainWorkers += hd.getMainWorkerCount();
                totalAuxWorkers += hd.getAuxWorkerCount();
                totalFortWorkers += hd.getFortWorkerCount();
            }
        }

        int totalWorkers = totalMainWorkers + totalAuxWorkers + totalFortWorkers;

        // Labels avec informations
        gbc.gridx = 0; gbc.gridy = 0;
        summaryPanel.add(new JLabel("Hexagones contrôlés: " + totalHexes), gbc);

        gbc.gridy = 1;
        summaryPanel.add(new JLabel("Travailleurs principaux: " + totalMainWorkers), gbc);

        gbc.gridy = 2;
        summaryPanel.add(new JLabel("Travailleurs auxiliaires: " + totalAuxWorkers), gbc);

        gbc.gridy = 3;
        summaryPanel.add(new JLabel("Travailleurs fortification: " + totalFortWorkers), gbc);

        // Label total avec popup hover
        gbc.gridy = 4;
        JLabel totalLabel = new JLabel("Total Personnel: " + totalWorkers);
        totalLabel.setFont(new Font("Arial", Font.BOLD, 14));
        totalLabel.setForeground(Color.BLACK);
        totalLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        totalLabel.setPreferredSize(new Dimension(200, 30));
        totalLabel.setHorizontalAlignment(SwingConstants.CENTER);


        final WorkDetailsPopup popup = new WorkDetailsPopup();
        totalLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                totalLabel.setBackground(Color.LIGHT_GRAY);
                totalLabel.setOpaque(true);

                // Use SwingUtilities.invokeLater to ensure component hierarchy is updated
                SwingUtilities.invokeLater(() -> {
                    // Find the appropriate parent container
                    Container targetParent = findBestParentForPopup(summaryPanel);

                    if (targetParent != null) {
                        // Remove popup if it's already added somewhere
                        Container currentParent = popup.getParent();
                        if (currentParent != null) {
                            currentParent.remove(popup);
                        }

                        // Add popup to the target parent
                        if (targetParent instanceof JLayeredPane) {
                            ((JLayeredPane) targetParent).add(popup, JLayeredPane.POPUP_LAYER);
                        } else {
                            targetParent.add(popup, 0); // Add at index 0 for visibility
                        }

                        // Force immediate layout update
                        targetParent.revalidate();
                        targetParent.repaint();

                        // Now show the popup details
                        showPopulationSummaryDetails(popup, repo, factionName, totalLabel);
                    } else {
                        System.err.println("Could not find suitable parent for popup");
                    }
                });
            }

            @Override
            public void mouseExited(MouseEvent e) {
                totalLabel.setOpaque(false);
                popup.hideDetails();

                // Remove popup from its parent
                SwingUtilities.invokeLater(() -> {
                    Container parent = popup.getParent();
                    if (parent != null) {
                        parent.remove(popup);
                        parent.revalidate();
                        parent.repaint();
                    }
                });
            }
        });
        summaryPanel.add(totalLabel, gbc);
        return summaryPanel;
    }
    private static JLabel createValueLabel(String value) {
        JLabel label = new JLabel(value);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        label.setForeground(Color.DARK_GRAY);
        return label;
    }
    private static String findJobCategory(String jobName) {
        if (jobName == null) return "Autre";

        return Arrays.stream(DATABASE.Workers.values())
                .filter(worker -> worker.getJobName().equalsIgnoreCase(jobName))
                .map(DATABASE.Workers::getCategory)
                .findFirst()
                .orElse("Autre");
    }
    private static DefaultListCellRenderer createMoralRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (isSelected) {
                    label.setBackground(Color.LIGHT_GRAY);
                    label.setForeground(Color.BLACK);
                } else {
                    label.setBackground(Color.WHITE);
                    label.setForeground(Color.BLACK);
                }
                return label;
            }
        };
    }
    private static JComboBox<String> createDropdown(IDataProvider data, String resourcePath, int column) {
        List<String> options = data.loadOptions(resourcePath, column);
        return new JComboBox<>(options.toArray(new String[0]));
    }
    private static void showPopulationSummaryDetails(WorkDetailsPopup popup,
                                                     IHexRepository repo,
                                                     String factionName,
                                                     JLabel anchorLabel) {
        StringBuilder sb = new StringBuilder("<html><body style='font-family:Arial;padding:5px;'>");
        sb.append("<b>Détail par hexagone :</b><ul>");
        Map<String, SafeHexDetails> hexGrid = repo.loadSafeAll();
        for (Map.Entry<String, SafeHexDetails> e : hexGrid.entrySet()) {
            SafeHexDetails hd = e.getValue();
            if (!factionName.equals(hd.getFactionClaim())) continue;
            sb.append("<li><b>").append(e.getKey()).append("</b>: total ")
                    .append(hd.getTotalWorkers()).append(" (M:")
                    .append(hd.getMainWorkerCount()).append(" A:")
                    .append(hd.getAuxWorkerCount()).append(" F:")
                    .append(hd.getFortWorkerCount()).append(")</li>");
        }
        sb.append("</ul></body></html>");
        popup.showDetails(sb.toString(), anchorLabel);
    }
    private static JLayeredPane getParentLayeredPane(Component component) {
        Container parent = component.getParent();
        while (parent != null) {
            if (parent instanceof JLayeredPane) {
                return (JLayeredPane) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }
    private static BufferedImage createHexagonalSnapshot(String hexKey, IHexRepository repo) {
        if (mapBackground == null) return null;

        Rectangle r = Mondes.getHexPixelBounds(hexKey, repo);
        BufferedImage square = mapBackground.getSubimage(
                clamp(r.x, 0, mapBackground.getWidth()  - r.width),
                clamp(r.y, 0, mapBackground.getHeight() - r.height),
                r.width, r.height  );


        BufferedImage hexImage = new BufferedImage(r.width, r.height  , BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = hexImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Path2D hexPath = createHexagonPath( r.width/2, r.height  /2,  r.width+r.height/2 - 2);
        g2d.setClip(hexPath);
        g2d.drawImage(square, 0, 0, null);
        g2d.setClip(null);
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(Color.BLACK);
        g2d.draw(hexPath);

        g2d.dispose();
        return hexImage;
    }
    private static Path2D createHexagonPath(double centerX, double centerY, double radius) {
        Path2D hexagon = new Path2D.Double();
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
    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(v, max));
    }
    public static void preloadHexSnapshots(Map<String, HexDetails> hexGrid, IHexRepository repo) {
        CompletableFuture.runAsync(() -> {
            for (String hexKey : hexGrid.keySet()) {
                UI.HexSnapshotCache.getHexSnapshot(hexKey, repo);
            }
        });
    }
    public static UI.EnhancedProductionPanel createEnhancedProductionPanel(Map<String, SafeHexDetails> hexGrid,
                                                                           String factionName, IHexRepository repo, EconomicDataService economicService) {
        return new UI.EnhancedProductionPanel(hexGrid, factionName, repo, economicService);
    }
    public static EconomicDataService initializeEconomicService(IHexRepository repo, String factionName) {
        economicService = new EconomicDataService(repo, factionName);
        return economicService;
    }
    public static void connectMoralPanelToEconomicService(UI.MoralPanelResult result, EconomicDataService economicService) {
        // Connect moral panel to update instability in economic service
        for (JComboBox<DATABASE.MoralAction> dropdown : result.dropdownMap.values()) {
            dropdown.addActionListener(e -> {
                // Calculate total instability from all selections
                int totalInstability = 0;
                for (JComboBox<DATABASE.MoralAction> cb : result.dropdownMap.values()) {
                    DATABASE.MoralAction selected = (DATABASE.MoralAction) cb.getSelectedItem();
                    if (selected != null) {
                        totalInstability += selected.getInstabilityEffect();
                    }
                }
                economicService.updateInstabilityFromMoral(totalInstability);
            });
        }
    }
    public static String getBuildingNamesSafe(int mainIdx, int auxIdx, int fortIdx) {
        String mainName = getEnumNameSafely(DATABASE.MainBuilding.values(), mainIdx, "None");
        String auxName = getEnumNameSafely(DATABASE.AuxBuilding.values(), auxIdx, "None");
        String fortName = getEnumNameSafely(DATABASE.FortBuilding.values(), fortIdx, "None");

        return String.format("Main: %s | Aux: %s | Fort: %s", mainName, auxName, fortName);
    }
    private static <T extends Enum<T>> String getEnumNameSafely(T[] enumValues, int index, String defaultValue) {
        try {
            if (index > 0 && index < enumValues.length) {
                T enumInstance = enumValues[index];

                if (enumInstance instanceof DATABASE.MainBuilding) {
                    return ((DATABASE.MainBuilding) enumInstance).getBuildName();
                } else if (enumInstance instanceof DATABASE.AuxBuilding) {
                    return ((DATABASE.AuxBuilding) enumInstance).getBuildName();
                } else if (enumInstance instanceof DATABASE.FortBuilding) {
                    return ((DATABASE.FortBuilding) enumInstance).getBuildName();
                }

                // Fallback to enum constant name if no getter available
                return enumInstance.name();
            }
        } catch (Exception e) {
            System.err.println("Error getting enum name for index " + index + ": " + e.getMessage());
        }
        return defaultValue;
    }
    private static Container findBestParentForPopup(Component component) {
        Container parent = component.getParent();

        // First, try to find a JLayeredPane (best option)
        while (parent != null) {
            if (parent instanceof JLayeredPane) {
                return parent;
            }
            parent = parent.getParent();
        }

        // If no JLayeredPane found, use the root pane
        parent = component.getParent();
        while (parent != null) {
            if (parent instanceof JRootPane) {
                return ((JRootPane) parent).getLayeredPane();
            }
            if (parent instanceof JFrame || parent instanceof JDialog) {
                return ((RootPaneContainer) parent).getRootPane().getLayeredPane();
            }
            parent = parent.getParent();
        }

        // Last resort: return the immediate parent
        return component.getParent();
    }
    public static DATABASE.JobBuilding getBuildingFromHex(SafeHexDetails hex, String buildingType) {
        try {
            return switch (buildingType.toLowerCase()) {
                case "main" -> {
                    int index = hex.getMainBuildingIndex();
                    if (index >= 0 && index < DATABASE.MainBuilding.values().length) {
                        yield DATABASE.MainBuilding.values()[index];
                    }
                    yield null;
                }
                case "aux" -> {
                    int index = hex.getAuxBuildingIndex();
                    if (index >= 0 && index < DATABASE.AuxBuilding.values().length) {
                        yield DATABASE.AuxBuilding.values()[index];
                    }
                    yield null;
                }
                case "fort" -> {
                    int index = hex.getFortBuildingIndex();
                    if (index >= 0 && index < DATABASE.FortBuilding.values().length) {
                        yield DATABASE.FortBuilding.values()[index];
                    }
                    yield null;
                }
                default -> null;
            };
        } catch (Exception e) {
            System.err.println("Error getting building from hex " + hex.getHexKey() +
                    ", type " + buildingType + ": " + e.getMessage());
            return null;
        }
    }
    public static double getBuildingEfficiency(DATABASE.JobBuilding building) {
        if (building instanceof DATABASE.MainBuilding) {
            return 1.0 + (((DATABASE.MainBuilding) building).getMainTier() * 0.2);
        } else if (building instanceof DATABASE.AuxBuilding) {
            return 1.0 + (((DATABASE.AuxBuilding) building).getAuxTier() * 0.2);
        }
        return 1.0;
    }
    public double calculateEstimatedProduction(DATABASE.JobBuilding building,
                                                DATABASE.ResourceType resource,
                                                int workers) {
        double baseProduction = resource.getBaseValue();
        double efficiency = getBuildingEfficiency(building);
        return baseProduction * workers * efficiency;
    }
    public static boolean isFarmBuilding(SafeHexDetails hex, String buildingType) {
        DATABASE.JobBuilding building = getBuildingFromHex(hex, buildingType);
        if (building == null) return false;

        String buildingName = building.getBuildName();
        return buildingName.contains("Ferme") ||
                buildingName.contains("pâturage") ||
                buildingName.contains("verger") ||
                buildingName.contains("camp de chasseurs");
    }
    public static boolean checkWaterProximity(String hexKey, IHexRepository repo) {
        try {
            BufferedImage hexSnapshot = UI.HexSnapshotCache.getHexSnapshot(hexKey, repo);
            if (hexSnapshot == null) return false;

            return analyzeImageForWater(hexSnapshot);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'analyse d'eau pour " + hexKey + ": " + e.getMessage());
            return false; // Fallback conservateur
        }
    }
    private static boolean analyzeImageForWater(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int waterPixelCount = 0;
        int validPixelCount = 0;

        // Image de debug (optionnel)
        BufferedImage debugImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D debugG2d = debugImage.createGraphics();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;

                // Seuil de transparence plus permissif
                if (alpha < 32) {
                    debugG2d.setColor(Color.BLACK); // Pixels transparents en noir
                    debugG2d.fillRect(x, y, 1, 1);
                    continue;
                }

                validPixelCount++;
                Color pixelColor = new Color(argb, true);

                if (isWaterColorImproved(pixelColor)) {
                    waterPixelCount++;
                    debugG2d.setColor(Color.RED); // Eau détectée en rouge
                } else {
                    debugG2d.setColor(pixelColor); // Couleur originale
                }
                debugG2d.fillRect(x, y, 1, 1);
            }
        }

        debugG2d.dispose();

        /* Sauvegarder l'image de debug
        try {
            ImageIO.write(debugImage, "png", new File("water_detection_debug.png"));
            System.out.println("Debug image saved: water_detection_debug.png");
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        if (validPixelCount == 0) return false;

        double waterPercentage = (double) waterPixelCount / validPixelCount;
        System.out.println("Water pixels: " + waterPixelCount + "/" + validPixelCount +
                " = " + String.format("%.4f", waterPercentage));
        return waterPercentage > 0.10;
    }

    private static boolean isWaterColorImproved(Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        // Convert to HSV for better color detection
        float[] hsv = Color.RGBtoHSB(r, g, b, null);
        float hue = hsv[0] * 360; // 0-360
        float saturation = hsv[1]; // 0-1
        float value = hsv[2]; // 0-1

        // Eau : teintes bleues/cyan (180-240°) avec saturation et luminosité décentes
        if (hue >= 180 && hue <= 240 && saturation > 0.3 && value > 0.3) {
            return true;
        }

        // Détection RGB spécifique pour votre image (eau turquoise/bleue claire)
        // Ajustez ces valeurs selon votre image spécifique
        if (b > r + 20 && b > g && b > 80 && b < 220) return true; // Bleu dominant
        if (b > 100 && g > 100 && r < 150 && (b + g) > 1.5 * r) return true; // Cyan/turquoise

        return false;
    }
}



