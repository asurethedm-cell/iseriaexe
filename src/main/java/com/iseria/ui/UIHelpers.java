package com.iseria.ui;

import com.iseria.domain.*;
import com.iseria.infra.FactionRegistry;
import com.iseria.infra.MoralCalculator;
import com.iseria.service.EconomicDataService;
import com.iseria.service.PersonnelDataService;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.iseria.domain.DATABASE.MoralAction.ActionType.UNIQUE;
import static com.iseria.ui.MainMenu.*;
import static com.iseria.ui.UI.HexSnapshotCache.mapBackground;


public class UIHelpers  extends JScrollPane{


    public static Map<String, JLabel> hexImageLabels = new HashMap<>();

    public static BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(Objects.requireNonNull(UI.class.getResource(path)));
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


    public static void attachIntelligentMoralUpdater(JLabel moralEndValue, UI.MoralPanelResult result) {
        ActionListener listener = e -> {
            try {
                List<DATABASE.MoralAction> selectedActions = new ArrayList<>();
                Set<DATABASE.MoralAction> uniqueActions = new HashSet<>();

                // Collecter toutes les sélections
                for (JComboBox<DATABASE.MoralAction> dropdown : result.dropdownMap.values()) {
                    DATABASE.MoralAction selected = (DATABASE.MoralAction) dropdown.getSelectedItem();
                    if (selected != null) {
                        selectedActions.add(selected);

                        // Vérifier les règles UNIQUE
                        if (selected.getType() == UNIQUE) {
                            if (uniqueActions.contains(selected)) {
                                // Conflit détecté - afficher avertissement
                                moralEndValue.setText("CONFLIT!");
                                moralEndValue.setForeground(Color.RED);
                                return;
                            }
                            uniqueActions.add(selected);
                        }
                    }
                }

                // Calculer le moral total
                double moralSum = MoralCalculator.sumMoral(selectedActions);
                moralEndValue.setText(String.format("%.1f", moralSum));

                // Couleur selon la valeur
                if (moralSum == 5.0) {
                    moralEndValue.setForeground(Color.LIGHT_GRAY);
                } else if (moralSum <= 4.0) {
                    moralEndValue.setForeground(new Color(207, 65, 7));
                } else {
                    moralEndValue.setForeground(new Color(65, 186, 61));
                }

            } catch (Exception ex) {
                System.err.println("Erreur lors du calcul du moral : " + ex.getMessage());
                moralEndValue.setText("ERREUR");
                moralEndValue.setForeground(Color.RED);
            }
        };

        // Attacher le listener à tous les dropdowns
        for (JComboBox<DATABASE.MoralAction> dropdown : result.dropdownMap.values()) {
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
                            targetParent.add(popup, JLayeredPane.POPUP_LAYER);
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

    private static BufferedImage createHexagonalSnapshot(String hexKey, IHexRepository repo) {
        if (mapBackground == null) return null;

        Rectangle r = Mondes.getHexPixelBounds(hexKey, repo);
        BufferedImage square = mapBackground.getSubimage(
                clamp(r.x, mapBackground.getWidth()  - r.width),
                clamp(r.y, mapBackground.getHeight() - r.height),
                r.width, r.height  );

        BufferedImage hexImage = new BufferedImage(r.width, r.height  , BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = hexImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Path2D hexPath = createHexagonPath( (double) r.width /2, (double) r.height /2,  r.width+ (double) r.height /2 - 2);
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
    private static int clamp(int v, int max) { return Math.max(0, Math.min(v, max)); }
    public static void preloadHexSnapshots(Map<String, SafeHexDetails> hexGrid, IHexRepository repo) {
        CompletableFuture.runAsync(() -> {
            for (String hexKey : hexGrid.keySet()) {
                UI.HexSnapshotCache.getHexSnapshot(hexKey, repo);
            }
        });
    }
    public static UI.ProductionPanel createEnhancedProductionPanel(Map<String, SafeHexDetails> hexGrid,
                                                                   String factionName, IHexRepository repo,
                                                                   EconomicDataService economicService, PersonnelDataService personnelService) {
        return new UI.ProductionPanel(hexGrid, factionName, repo, economicService, personnelService);
    }
    public static EconomicDataService initializeEconomicService(IHexRepository repo, String factionName) {
        economicService = new EconomicDataService(repo, factionName);
        return economicService;
    }
    public static void connectMoralPanelToEconomicService(UI.MoralPanelResult result, EconomicDataService economicService) {
        for (JComboBox<DATABASE.MoralAction> dropdown : result.dropdownMap.values()) {
            dropdown.addActionListener(e -> {
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
        String mainName = getEnumNameSafely(DATABASE.MainBuilding.values(), mainIdx);
        String auxName = getEnumNameSafely(DATABASE.AuxBuilding.values(), auxIdx);
        String fortName = getEnumNameSafely(DATABASE.FortBuilding.values(), fortIdx);

        return String.format("Main: %s | Aux: %s | Fort: %s", mainName, auxName, fortName);
    }
    private static <T extends Enum<T>> String getEnumNameSafely(T[] enumValues, int index) {
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
                return enumInstance.name();
            }
        } catch (Exception e) {
            System.err.println("Error getting enum name for index " + index + ": " + e.getMessage());
        }
        return "None";
    }
    private static Container findBestParentForPopup(Component component) {
        Container parent = component.getParent();
        while (parent != null) {
            if (parent instanceof JLayeredPane) {
                return parent;
            }
            parent = parent.getParent();
        }
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

    public static void logseparator() {
        System.out.println("=================================");
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
            return false;
        }
    }
    private static boolean analyzeImageForWater(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int waterPixelCount = 0;
        int validPixelCount = 0;

        BufferedImage debugImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D debugG2d = debugImage.createGraphics();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;

                if (alpha < 32) {
                    debugG2d.setColor(Color.BLACK); // Pixels transparents en noir
                    debugG2d.fillRect(x, y, 1, 1);
                    continue;
                }

                validPixelCount++;
                Color pixelColor = new Color(argb, true);

                if (isWaterColorImproved(pixelColor)) {
                    waterPixelCount++;
                    debugG2d.setColor(Color.RED);
                } else {
                    debugG2d.setColor(pixelColor);
                }
                debugG2d.fillRect(x, y, 1, 1);
            }
        }
        debugG2d.dispose();
        try {
            ImageIO.write(debugImage, "png", new File("water_detection_debug.png"));
            System.out.println("Debug image saved: water_detection_debug.png");
        } catch (IOException e) {
            e.printStackTrace();
        }
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

        // Eau : teintes bleues/cyan (180-240°)
        if (hue >= 180 && hue <= 240 && saturation > 0.3 && value > 0.3) {
            return true;
        }

        // Détection RGB spécifique pour eau turquoise/bleue claire
        if (b > r + 20 && b > g && b > 80 && b < 220) return true; // Bleu dominant
        return b > 100 && g > 100 && r < 150 && (b + g) > 1.5 * r; // Cyan/turquoise
    }

}



