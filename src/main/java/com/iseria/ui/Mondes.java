package com.iseria.ui;

import com.iseria.domain.*;
import com.iseria.domain.DATABASE;
import com.iseria.infra.FactionRegistry;
import com.iseria.service.EconomicDataService;
import com.iseria.service.LogisticsService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.awt.geom.Path2D;
import java.awt.geom.AffineTransform;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static com.iseria.ui.UIHelpers.configureScrollSpeed;
import static com.iseria.ui.UIHelpers.styleScrollPane;
import static javax.swing.BorderFactory.createLineBorder;

public class Mondes extends JFrame {

    private BufferedImage mapmonde;
    JLayeredPane layeredPane;
    private double imgWidth, imgHeight;
    private static double imgX, imgY;
    public static double zoomFactor = 1.0;
    double panelWidth;
    double panelHeight;
    private int lastMouseX, lastMouseY;
    public static final int HEX_SIZE = 70;
    private Cursor MapDragging;
    private JPanel detailsPanel = new JPanel();
    private JPanel moredetailsPanel = new JPanel();
    JLabel title;
    JLabel title2;
    private JLabel claimIconLabel;
    private JPanel MAPMONDE;
    private String currentViewFactionId = null; // null = vue normale
    private List<String> playerFactionList = new ArrayList<>();
    private int currentFactionViewIndex = -1; // -1 = vue normale
    private JButton factionViewToggle = new JButton();
    private final AtomicBoolean isSaving = new AtomicBoolean(false);
    private JButton quitButton = new JButton("Quit");
    private JButton repaintButton = new JButton("repaint");
    private JButton resetButton = new JButton("resetClaims");
    private JButton MainLabel = new JButton();
    private JButton AuxLabel = new JButton();
    private JButton FortLabel = new JButton();
    private JButton SaveButton = new JButton();
    private JButton MoreDetail = new JButton();
    private JButton Claim = new JButton();
    private int hexMainCap = getHexMainCapFromDATABASE();
    private static final double ZOOM_MIN = 0.3;
    private static final double ZOOM_MAX = 3.0;
    private JDialog iconMenu;
    private JPanel iconPanel;
    Map<Integer, ImageIcon> originalMainIcon = new HashMap<>();
    Map<Integer, ImageIcon> originalAuxIcon = new HashMap<>();
    Map<Integer, ImageIcon> originalFortIcon = new HashMap<>();
    public int currentIconMainIndex = 0;
    public int currentIconAuxIndex = 0;
    public int currentIconFortIndex = 0;
    private final Path2D unitHexagon = new Path2D.Double();
    private Map<Integer, ImageIcon> scaledIconsMain = new HashMap<>();
    private Map<Integer, ImageIcon> scaledIconsAux = new HashMap<>();
    private Map<Integer, ImageIcon> scaledIconsFort = new HashMap<>();
    private Map<Point, String> hexLabels = new HashMap<>();
    String hexKey;
    IAudioService audio;
    IHexRepository repo;
    Point labelclick;
    private final ConcurrentHashMap<String, SafeHexDetails> hexCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Color> factionColorCache = new ConcurrentHashMap<>();
    private boolean cacheInitialized = false;
    public boolean isAdmin = "Admin".equals(Login.currentUser);
    private Faction chosen;
    private boolean detailViewOpen = false;
    private final ConcurrentHashMap<String, Float> alphaCache = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();


    Mondes(IAudioService audio, IHexRepository repo, boolean performHeavyInit) {
        this.audio = audio;
        this.repo = repo;
        setupBasicComponents();
        setupWindowProperties();
        setupBasicPanels();
        Mapmonddemouse();
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ESCAPE -> {
                        if (detailViewOpen) closeDetailView();
                        else {
                            audio.fadeOut();
                            dispose();
                            Login.mondeOpen = false;
                            audio.stop();
                            new javax.swing.Timer(1000, t -> {
                                ((javax.swing.Timer)t.getSource()).stop();
                                if (!MainMenu.isFactionMenuOpen) audio.fadeIn();
                            }).start();
                        }
                    }
                    case KeyEvent.VK_C -> {
                        if ("Admin".equals(Login.currentUser)) {
                            Collection<Faction> allFactions = FactionRegistry.all();
                            Faction[] factionArray = allFactions.toArray(new Faction[0]);

                            Faction selectedFaction = (Faction) JOptionPane.showInputDialog(
                                    detailsPanel, "Choisissez la faction :", "Admin Override",
                                    JOptionPane.QUESTION_MESSAGE, null, factionArray, factionArray);

                            if (selectedFaction != null) {
                                chosen = selectedFaction;
                                doClaimHex();
                            }
                        }
                        else { doClaimHex();}

                    }
                    case KeyEvent.VK_M -> MoreDetailedView();
                    case KeyEvent.VK_S -> saveHexDetails();
                }
            }
        });
        if (performHeavyInit) {
            // Ancien comportement pour compatibilité
            performAllInitialization();
        }
    }

    void Mapmonddemouse()  {
        MAPMONDE.addMouseWheelListener(e -> {
            if (detailsPanel.isVisible() || moredetailsPanel.isVisible()) {
                return;
            }
            double oldZoom = zoomFactor;
            int notches = e.getWheelRotation();
            double zoomStep = 0.1;
            if (notches < 0) {
                zoomFactor += zoomStep;  // Zoom in by adding 5%
            } else {
                zoomFactor -= zoomStep;  // Zoom out by subtracting 5%
            }
            zoomFactor = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoomFactor));
            zoomFactor = Math.round(zoomFactor * 100.0) / 100.0;

            double mouseX = e.getX();
            double mouseY = e.getY();
            imgX = mouseX - ((mouseX - imgX) * (zoomFactor / oldZoom));
            imgY = mouseY - ((mouseY - imgY) * (zoomFactor / oldZoom));

            double minX = panelWidth - (imgWidth * zoomFactor);
            imgX = Math.min(0, Math.max(imgX, minX));

            double minY = panelHeight - (imgHeight * zoomFactor);
            imgY = Math.min(0, Math.max(imgY, minY));
            MAPMONDE.repaint();

        });

        MAPMONDE.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if ((detailsPanel.isVisible() && detailsPanel.getBounds().contains(e.getPoint())) || (moredetailsPanel.isVisible() && moredetailsPanel.getBounds().contains(e.getPoint()))) {
                    return;
                }
                if (SwingUtilities.isLeftMouseButton(e)) {
                    int dx = e.getX() - lastMouseX;
                    int dy = e.getY() - lastMouseY;

                    lastMouseX = e.getX();
                    lastMouseY = e.getY();

                    imgX += dx;
                    imgY += dy;

                    double minX = panelWidth - (imgWidth * zoomFactor);
                    double minY = panelHeight - (imgHeight * zoomFactor);
                    imgX = Math.min(0, Math.max(imgX, minX));
                    imgY = Math.min(0, Math.max(imgY, minY));
                    MAPMONDE.repaint();

                }
            }
        });

        MAPMONDE.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (detailsPanel.isVisible() && detailsPanel.getBounds().contains(e.getPoint())) {
                    return;
                }
                if (e.getButton() == MouseEvent.BUTTON1) {
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                    setCursor(MapDragging);

                } else if (((!detailsPanel.isVisible() || !moredetailsPanel.isVisible()
                        || !detailsPanel.getBounds().contains(e.getPoint()))) && (e.getButton() == MouseEvent.BUTTON3)) {
                    audio.playClick();
                    handleHexClick(e.getX(), e.getY());
                    String label = hexLabels.get(labelclick);
                    if (label == null) return;
                    if (e.isControlDown() && Login.currentUser.equals("Admin")) {
                        setVisibilityForAllPlayerFactions(hexKey);
                        return;
                    }
                    onHexClicked(hexKey);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
                quitButton.repaint();
            }

        });
    }

    private void setVisibilityForAllPlayerFactions(String hexKey) {
        if (hexKey == null) {
            JOptionPane.showMessageDialog(this,
                    "⚠️ Aucun hexagone sélectionné",
                    "Erreur", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Collection<Faction> allFactions = FactionRegistry.all();
        Set<String> playerFactionIds = allFactions.stream()
                .filter(Faction::getIsPlayer)
                .map(Faction::getId)
                .collect(Collectors.toSet());
        SafeHexDetails hex = repo.getHexDetails(hexKey);
        Set<String> currentDiscovered = new HashSet<>(hex.getDiscoveredByFaction());
        currentDiscovered.addAll(playerFactionIds);
        hex.setDiscoveredByFaction(currentDiscovered);


        repo.updateHexDetails(hexKey, hex.deepCopy());
        cacheLock.writeLock().lock();
        try { hexCache.put(hexKey, hex.deepCopy());} finally { cacheLock.writeLock().unlock(); }
        alphaCache.clear();
        SwingUtilities.invokeLater(() -> MAPMONDE.repaint());
        System.out.println("🔍 Visibilité automatique accordée sur " + hexKey + " pour: " + playerFactionIds);
    }

    void DetailedView() {
        if (detailViewOpen) {
            closeDetailView();
        }

        detailViewOpen = true;
        detailsPanel.removeAll();
        for (ActionListener al : Claim.getActionListeners()) {
            Claim.removeActionListener(al);
        }
        for (ActionListener al : SaveButton.getActionListeners()) {
            SaveButton.removeActionListener(al);
        }

        /// System.out.println(hexKey);
        SafeHexDetails details = repo.getHexDetails(hexKey);
        currentIconMainIndex = details.getMainBuildingIndex();
        currentIconAuxIndex = details.getAuxBuildingIndex();
        currentIconFortIndex = details.getFortBuildingIndex();

        ImageIcon hexIcon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/hex4.png")));

        JPanel buildingPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(hexIcon.getImage(), -10, 150, 850, 597, this);
            }
        };
        Faction faction = FactionRegistry.getFactionId(details.getFactionClaim());
        ImageIcon originalClaimIcon =
                new ImageIcon(Objects.requireNonNull(getClass().getResource(faction.getEmblemImage()))
                );
        Image scaledClaimImage = originalClaimIcon.getImage().getScaledInstance(
                180,
                256,
                Image.SCALE_SMOOTH
        );
        ImageIcon claimIcon = new ImageIcon(scaledClaimImage);
        claimIconLabel = new JLabel(claimIcon);
        claimIconLabel.setBounds(50, 50, claimIcon.getIconWidth(), claimIcon.getIconHeight());
        MainLabel = detailedViewButton("", originalMainIcon.get(details.getMainBuildingIndex()), 50, 325, originalMainIcon.get(currentIconMainIndex).getIconWidth(), originalMainIcon.get(currentIconMainIndex).getIconHeight());
        AuxLabel = detailedViewButton("", originalAuxIcon.get(details.getAuxBuildingIndex()), 275, 100, originalAuxIcon.get(currentIconAuxIndex).getIconWidth(), originalAuxIcon.get(currentIconAuxIndex).getIconHeight());
        FortLabel = detailedViewButton("", originalFortIcon.get(details.getFortBuildingIndex()), 400, 325, originalFortIcon.get(currentIconFortIndex).getIconWidth(), originalFortIcon.get(currentIconFortIndex).getIconHeight());

        SaveButton.setFont(new Font("Oswald", Font.BOLD, 15));
        SaveButton.setText("Save Details");
        SaveButton.setBounds(275, 700, 250, 75);
        SaveButton.setOpaque(true);
        SaveButton.setBackground(Color.lightGray);
        SaveButton.setBorderPainted(false);
        SaveButton.setForeground(Color.BLACK);
        SaveButton.setContentAreaFilled(true);
        SaveButton.setFocusable(false);
        SaveButton.addActionListener(this::saveButtonTrigger);

        MoreDetail.setFont(new Font("Oswald", Font.BOLD, 15));
        MoreDetail.setText("More Details");
        MoreDetail.setBounds(0, 700, 250, 75);
        MoreDetail.setOpaque(true);
        MoreDetail.setBackground(Color.lightGray);
        MoreDetail.setBorderPainted(false);
        MoreDetail.setForeground(Color.BLACK);
        MoreDetail.setContentAreaFilled(true);
        MoreDetail.setFocusable(false);
        MoreDetail.addActionListener(this::MoreDetaiButtonTrigger);

        Claim.setFont(new Font("Oswald", Font.BOLD, 15));
        Claim.setText("Claim The Hex");
        Claim.setBounds(540, 700, 250, 75);
        Claim.setOpaque(true);
        Claim.setBackground(Color.lightGray);
        Claim.setBorderPainted(false);
        Claim.setForeground(Color.BLACK);
        Claim.setContentAreaFilled(true);
        Claim.setFocusable(false);
        Claim.addActionListener(e -> {
            if ("Admin".equals(Login.currentUser)) {
                Collection<Faction> allFactions = FactionRegistry.all();
                Faction[] factionArray = allFactions.toArray(new Faction[0]);
                Faction selectedFaction = (Faction) JOptionPane.showInputDialog(
                        detailsPanel, "Choisissez la faction :", "Admin Override",
                        JOptionPane.QUESTION_MESSAGE, null, factionArray, factionArray);
                if (selectedFaction != null) {
                    chosen = selectedFaction;
                    doClaimHex();
                }
            } else {
                doClaimHex();
            }
        });


        ImageIcon closeIcon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/close.png")));
        Image scaledImage = closeIcon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
        closeIcon = new ImageIcon(scaledImage);
        JButton closeButton = detailedViewButton("X", closeIcon, 770, -10, 50, 50);
        MainLabel.addActionListener(this::MainBuidindButtonMenu);
        AuxLabel.addActionListener(this::AuxilliaryButtonMenu);
        FortLabel.addActionListener(this::FortificationButtonMenu);

        buildingPanel.setLayout(null);
        buildingPanel.setBounds(0, 0, 800, 750);
        buildingPanel.setOpaque(false);
        buildingPanel.setBackground(new Color(0, 0, 0, 0));
        buildingPanel.setBorder(createLineBorder(UIHelpers.getFactionColor(details.getFactionClaim()), 5));
        title = new JLabel("Hexagon " + hexKey);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Oswald", Font.BOLD, 25));
        title.setBounds(270, 0, 500, 50);

        buildingPanel.add(title);
        buildingPanel.add(AuxLabel);
        buildingPanel.add(MainLabel);
        buildingPanel.add(FortLabel);
        buildingPanel.add(claimIconLabel);
        buildingPanel.add(SaveButton);
        buildingPanel.add(MoreDetail);
        buildingPanel.add(Claim);
        buildingPanel.add(closeButton);

        detailsPanel.add(buildingPanel);
        detailsPanel.setVisible(true);
        revalidate();

    }

    void MoreDetailedView() {
        detailsPanel.setVisible(false);
        moredetailsPanel.removeAll();

        JPanel moreBuildingPanel = new JPanel();
        JButton LessDetail = getJButton();

        JButton adminFowSet = new JButton();
        adminFowSet.setFont(new Font("Oswald", Font.BOLD, 15));
        adminFowSet.setText("adminFowSet");
        adminFowSet.setBounds(630, 700, 200, 75);
        adminFowSet.setOpaque(true);
        adminFowSet.setBackground(Color.lightGray);
        adminFowSet.setBorderPainted(false);
        adminFowSet.setForeground(Color.BLACK);
        adminFowSet.setContentAreaFilled(true);
        adminFowSet.setFocusable(false);
        adminFowSet.setVisible(false);
        adminFowSet.addActionListener(e -> {
            audio.playClick();
            openFowManagementDialog(hexKey);
        });

        JButton LogisticsDetail = getButton();
        JButton ProductionDetail = getProductionDetail();

        ImageIcon closeIcon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/close.png")));
        Image scaledImage = closeIcon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
        closeIcon = new ImageIcon(scaledImage);
        JButton closeButton = detailedViewButton("X", closeIcon, 770, -10, 50, 50);

        moreBuildingPanel.setLayout(null);
        moreBuildingPanel.setBounds(0, 0, 800, 750);
        moreBuildingPanel.setOpaque(true);
        moreBuildingPanel.setBackground(new Color(72, 108, 104, 255));

        title2 = new JLabel("Hexagon " + hexKey + " - Details");
        title2.setForeground(Color.WHITE);
        title2.setFont(new Font("Oswald", Font.BOLD, 25));
        title2.setBounds(270, 0, 500, 50);
        moreBuildingPanel.add(title2);

        SafeHexDetails details = repo.getHexDetails(hexKey);
        moredetailsPanel.setBorder(createLineBorder(UIHelpers.getFactionColor(details.getFactionClaim()), 5));

        JTextArea hexInfoArea = new JTextArea();
        hexInfoArea.setEditable(false);
        hexInfoArea.setOpaque(true);
        hexInfoArea.setBackground(new Color(40, 40, 40, 200));
        hexInfoArea.setForeground(Color.WHITE);
        hexInfoArea.setFont(new Font("Courier New", Font.PLAIN, 12));

        String info = "=== INFORMATIONS HEXAGONE ===\n" +
                "Position: " + hexKey + "\n" +
                "Faction: " + details.getFactionClaim() + "\n" +
                "Population totale: " + details.getTotalWorkers() + "\n\n" +
                "=== BÂTIMENTS ===\n" +
                "Principal: " + UIHelpers.getBuildingNamesSafe(
                details.getMainBuildingIndex(), details.getAuxBuildingIndex(), details.getFortBuildingIndex()) +
                "\n\n" +
                "=== LOGISTIQUE ===\n" +
                "Véhicules assignés: " + details.getAssignedVehicles().size() + "\n" +
                "Routes disponibles: " + (details.getLogisticsData().hasRoad() ? "Oui" : "Non") + "\n" +
                "Accès maritime: " + (details.getLogisticsData().hasSea() ? "Oui" : "Non") + "\n" +
                "Accès fluvial: " + (details.getLogisticsData().hasRiver() ? "Oui" : "Non") + "\n";

        hexInfoArea.setText(info);

        JScrollPane infoScrollPane = new JScrollPane(hexInfoArea);
        infoScrollPane.setBounds(50, 60, 700, 600);
        moreBuildingPanel.add(infoScrollPane);

        moreBuildingPanel.add(LessDetail);
        moreBuildingPanel.add(adminFowSet);
        if (Login.currentUser.equals("Admin")) {
            adminFowSet.setVisible(true);
        }
        moreBuildingPanel.add(LogisticsDetail);
        moreBuildingPanel.add(ProductionDetail);

        moredetailsPanel.add(closeButton);

        moredetailsPanel.add(moreBuildingPanel);
        moredetailsPanel.setVisible(true);
    }

    private JButton getProductionDetail() {
        JButton ProductionDetail = new JButton();
        ProductionDetail.setFont(new Font("Oswald", Font.BOLD, 15));
        ProductionDetail.setText("Production");
        ProductionDetail.setBounds(420, 700, 200, 75);
        ProductionDetail.setOpaque(true);
        ProductionDetail.setBackground(new Color(34, 139, 34));
        ProductionDetail.setBorderPainted(false);
        ProductionDetail.setForeground(Color.WHITE);
        ProductionDetail.setContentAreaFilled(true);
        ProductionDetail.setFocusable(false);
        ProductionDetail.addActionListener(e -> openProductionForHex(hexKey));
        return ProductionDetail;
    }

    private JButton getButton() {
        JButton LogisticsDetail = new JButton();
        LogisticsDetail.setFont(new Font("Oswald", Font.BOLD, 15));
        LogisticsDetail.setText("Logistics");
        LogisticsDetail.setBounds(210, 700, 200, 75);
        LogisticsDetail.setOpaque(true);
        LogisticsDetail.setBackground(new Color(70, 130, 180)); // Steel blue
        LogisticsDetail.setBorderPainted(false);
        LogisticsDetail.setForeground(Color.WHITE);
        LogisticsDetail.setContentAreaFilled(true);
        LogisticsDetail.setFocusable(false);
        LogisticsDetail.addActionListener(e -> openLogisticsForHex(hexKey));
        return LogisticsDetail;
    }

    private JButton getJButton() {
        JButton LessDetail = new JButton();
        LessDetail.setFont(new Font("Oswald", Font.BOLD, 15));
        LessDetail.setText("Less Details");
        LessDetail.setBounds(0, 700, 200, 75);
        LessDetail.setOpaque(true);
        LessDetail.setBackground(Color.lightGray);
        LessDetail.setBorderPainted(false);
        LessDetail.setForeground(Color.BLACK);
        LessDetail.setContentAreaFilled(true);
        LessDetail.setFocusable(false);
        LessDetail.addActionListener(e -> {
            moredetailsPanel.setVisible(false);
            detailsPanel.setVisible(true);
        });
        return LessDetail;
    }

    private void openLogisticsForHex(String hexKey) {
        audio.playClick();
        JDialog logisticsDialog = new JDialog(this, "Logistique - " + hexKey, true);
        logisticsDialog.setSize(1000, 600);
        logisticsDialog.setLocationRelativeTo(this);

        EconomicDataService economicService = MainMenu.getEconomicService();
        LogisticsService logisticsService = economicService.getLogisticsService();
        if (logisticsService == null) {
            logisticsService = new LogisticsService(repo);
        }
        LogisticsPanel logisticsPanel = new LogisticsPanel(logisticsService, repo);
        logisticsPanel.selectHex(hexKey);

        logisticsDialog.add(logisticsPanel);
        logisticsDialog.setVisible(true);
    }

    private void openProductionForHex(String hexKey) {
        audio.playClick();

        JDialog productionDialog = new JDialog(this, "Production - " + hexKey, true);
        productionDialog.setSize(800, 500);
        productionDialog.setLocationRelativeTo(this);

        SafeHexDetails details = repo.getHexDetails(hexKey);
        EconomicDataService economicService = MainMenu.getEconomicService();

        JPanel productionPanel = new JPanel(new BorderLayout());
        productionPanel.setBackground(new Color(40, 40, 40));

        JTextArea productionInfo = new JTextArea();
        productionInfo.setEditable(false);
        productionInfo.setBackground(new Color(30, 30, 30));
        productionInfo.setForeground(Color.WHITE);
        productionInfo.setFont(new Font("Courier New", Font.PLAIN, 12));

        StringBuilder prodInfo = new StringBuilder();
        prodInfo.append("=== PRODUCTION HEXAGONE ").append(hexKey).append(" ===\n\n");

        if (economicService != null) {
            EconomicDataService.ProductionCalculationService prodService =
                    economicService.new ProductionCalculationService();
            Map<String, Double> production = prodService.calculateHexProduction(details);
            if (production.isEmpty()) {
                prodInfo.append("Aucune production active\n");
            } else {
                prodInfo.append("Production hebdomadaire:\n");
                for (Map.Entry<String, Double> entry : production.entrySet()) {
                    prodInfo.append(String.format("- %s: %.1f unités\n",
                            entry.getKey(), entry.getValue()));
                }
            }
        }
        prodInfo.append("\n=== DÉTAILS BÂTIMENTS ===\n");
        prodInfo.append("Main Building: Index ").append(details.getMainBuildingIndex())
                .append(" (").append(details.getMainWorkerCount()).append(" workers)\n");
        prodInfo.append("Aux Building: Index ").append(details.getAuxBuildingIndex())
                .append(" (").append(details.getAuxWorkerCount()).append(" workers)\n");
        prodInfo.append("Fort Building: Index ").append(details.getFortBuildingIndex())
                .append(" (").append(details.getFortWorkerCount()).append(" workers)\n");

        productionInfo.setText(prodInfo.toString());

        productionPanel.add(new JScrollPane(productionInfo), BorderLayout.CENTER);
        productionDialog.add(productionPanel);
        productionDialog.setVisible(true);
    }

    void openIconMenu(JButton targetButton) {
        final String finalDialogTitle;
        final int selectedIndex;
        Map<Integer, ImageIcon> selectedIcons;

        if (targetButton == MainLabel) {
            finalDialogTitle = "Main Building";
            selectedIndex = currentIconMainIndex;
            selectedIcons = scaledIconsMain;
        } else if (targetButton == AuxLabel) {
            finalDialogTitle = "Auxiliary Building";
            selectedIndex = currentIconAuxIndex;
            selectedIcons = scaledIconsAux;
        } else {
            finalDialogTitle = "Fortifications";
            selectedIndex = currentIconFortIndex;
            selectedIcons = scaledIconsFort;
        }

        iconMenu = new JDialog(this, finalDialogTitle, true);
        iconMenu.setSize(800, 700);

        iconPanel = new JPanel();
        iconPanel.setLayout(new BoxLayout(iconPanel, BoxLayout.Y_AXIS));

        for (Map.Entry<Integer, ImageIcon> entry : selectedIcons.entrySet()) {
            final int index = entry.getKey();
            ImageIcon icon = entry.getValue();

            String iconDescription = getBuildingDescription(finalDialogTitle, index);
            String buildingName = getBuildingName(finalDialogTitle, index);

            JPanel itemPanel = new JPanel(new BorderLayout());
            itemPanel.setMaximumSize(new Dimension(780, 120));
            itemPanel.setPreferredSize(new Dimension(780, 120));
            itemPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            JPanel iconContainer = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setPreferredSize(new Dimension(100, 100));
            iconLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            if (index == selectedIndex) {
                iconLabel.setBorder(createLineBorder(Color.RED, 3));
            }
            iconLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectIcon(iconLabel, index, finalDialogTitle);
                    updateSelectionBorder(iconPanel, iconLabel);
                }
            });

            iconContainer.add(iconLabel);

            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JLabel nameLabel = new JLabel("<html><b>" + buildingName + "</b></html>");
            nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
            nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel descLabel = new JLabel("<html><div style='width: 400px;'>" + iconDescription + "</div></html>");
            descLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            descLabel.setVerticalAlignment(SwingConstants.TOP);
            descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            textPanel.add(nameLabel);
            textPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            textPanel.add(descLabel);

            itemPanel.add(iconContainer, BorderLayout.WEST);
            itemPanel.add(textPanel, BorderLayout.CENTER);

            itemPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));

            iconPanel.add(itemPanel);
        }
        JScrollPane scrollPane = new JScrollPane(iconPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(780, 600));
        configureScrollSpeed(scrollPane, 80,20);
        styleScrollPane(scrollPane);
        iconMenu.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton confirmButton = new JButton("Confirm");
        JButton cancelButton = new JButton("Cancel");

        confirmButton.addActionListener(e -> {
            if (finalDialogTitle.equals("Main Building")) {
                confirmMainSelection(targetButton);
            } else if (finalDialogTitle.equals("Auxiliary Building")) {
                confirmAuxSelection(targetButton);
            } else {
                confirmFortSelection(targetButton);
            }
        });

        cancelButton.addActionListener(this::cancelSelection);
        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);
        iconMenu.add(buttonPanel, BorderLayout.SOUTH);

        iconMenu.setLocationRelativeTo(this);
        iconMenu.setVisible(true);
    }

    void resetIndexValue() {
        currentIconMainIndex = 0;
        currentIconAuxIndex = 0;
        currentIconFortIndex = 0;
    }

    public void saveHexDetails() {
        if (!isSaving.compareAndSet(false, true)) {
            System.out.println("Save already in progress, skipping...");
            return;
        }

        if (hexKey == null || hexKey.trim().isEmpty()) {
            System.err.println("Cannot save - no hex selected");
            isSaving.set(false);
            return;
        }

        try {
            System.out.println("Saving details for " + hexKey);

            SafeHexDetails details;
            cacheLock.readLock().lock();
            try {
                details = hexCache.get(hexKey);
                if (details == null) {
                    System.err.println(" Hex not found in cache: " + hexKey);
                    return;
                }
                details = details.deepCopy();
            } finally {
                cacheLock.readLock().unlock();
            }

            boolean mainChanged = details.getMainBuildingIndex() != currentIconMainIndex;
            boolean auxChanged = details.getAuxBuildingIndex() != currentIconAuxIndex;
            boolean fortChanged = details.getFortBuildingIndex() != currentIconFortIndex;

            String newFactionClaim = isAdmin && chosen != null ? chosen.getId() : MainMenu.getCurrentFactionId();
            boolean claimChanged = !Objects.equals(details.getFactionClaim(), newFactionClaim);

            if (!(mainChanged || auxChanged || fortChanged || claimChanged)) {
                System.out.println("📋 No changes detected, skipping save");
                return;
            }
            if (mainChanged) {
                details.setMainBuildingIndex(currentIconMainIndex);
                System.out.println("🔧 Main building changed: " + currentIconMainIndex);
            }
            if (auxChanged) {
                details.setAuxBuildingIndex(currentIconAuxIndex);
                System.out.println("🔧 Aux building changed: " + currentIconAuxIndex);
            }
            if (fortChanged) {
                details.setFortBuildingIndex(currentIconFortIndex);
                System.out.println("🔧 Fort building changed: " + currentIconFortIndex);
            }
            if (claimChanged) {
                details.setFactionClaim(newFactionClaim);
                System.out.println("🏴 Faction claim changed: " + newFactionClaim);
                Set<String> discovered = details.getDiscoveredByFaction();
                discovered.add(newFactionClaim);
                details.setDiscoveredByFaction(discovered);
            }
            repo.updateHexDetails(hexKey, details);

            cacheLock.writeLock().lock();
            try {
                hexCache.put(hexKey, details.deepCopy());
                alphaCache.entrySet().removeIf(entry -> entry.getKey().contains(hexKey.split("_")[1]) ||
                        entry.getKey().contains(hexKey.split("_")[2]));
            } finally {
                cacheLock.writeLock().unlock();
            }
            System.out.println("Saved successfully: " + details);
            SwingUtilities.invokeLater(() -> MAPMONDE.repaint());

        } catch (Exception e) {
            System.err.println("Error saving hex details: " + e.getMessage());
            e.printStackTrace();

        } finally {
            isSaving.set(false);
        }
    }

    void MainBuidindButtonMenu(ActionEvent e) {openIconMenu(MainLabel);}
    void AuxilliaryButtonMenu(ActionEvent e) {openIconMenu(AuxLabel);}
    void FortificationButtonMenu(ActionEvent e) {openIconMenu(FortLabel);}

    void MoreDetaiButtonTrigger(ActionEvent e) {MoreDetailedView();}
    void saveButtonTrigger(ActionEvent e) {
        System.out.println("Save button clicked, label: " + hexKey);
        saveHexDetails();
    }

    void confirmMainSelection(JButton targetButton) {
        ImageIcon origIcon = originalMainIcon.get(currentIconMainIndex);
        targetButton.setIcon(origIcon);
        targetButton.setSize(origIcon.getIconWidth(), origIcon.getIconHeight());
        targetButton.revalidate();
        targetButton.repaint();
        iconMenu.dispose();
    }
    void confirmAuxSelection(JButton targetButton) {
        System.out.println("Confirming Aux Selection, currentIconAuxIndex: " + currentIconAuxIndex); // Debug print
        ImageIcon selectedIcon = originalAuxIcon.get(currentIconAuxIndex);
        targetButton.setIcon(selectedIcon);
        targetButton.setSize(selectedIcon.getIconWidth(), selectedIcon.getIconHeight());
        targetButton.revalidate();
        targetButton.repaint();
        iconMenu.dispose();
    }

    void confirmFortSelection(JButton targetButton) {
        ImageIcon selectedIcon = originalFortIcon.get(currentIconFortIndex);
        targetButton.setIcon(selectedIcon);
        targetButton.setSize(selectedIcon.getIconWidth(), selectedIcon.getIconHeight());
        targetButton.revalidate();
        targetButton.repaint();
        iconMenu.dispose();
    }
    void cancelSelection(ActionEvent e) {
        iconMenu.dispose();
    }
    private String getBuildingDescription(String buildingType, int index) {
        try {
            switch (buildingType) {
                case "Main Building":
                    if (index < DATABASE.MainBuilding.values().length) {
                        return DATABASE.MainBuilding.values()[index].getMainDescription();
                    }
                    break;
                case "Auxiliary Building":
                    if (index < DATABASE.AuxBuilding.values().length) {
                        return DATABASE.AuxBuilding.values()[index].getAuxDescription();
                    }
                    break;
                case "Fortifications":
                    if (index < DATABASE.FortBuilding.values().length) {
                        return DATABASE.FortBuilding.values()[index].getFortDescription();
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("Erreur récupération description : " + e.getMessage());
        }
        return "Description non disponible";
    }
    private String getBuildingName(String buildingType, int index) {
        try {
            switch (buildingType) {
                case "Main Building":
                    if (index < DATABASE.MainBuilding.values().length) {
                        return DATABASE.MainBuilding.values()[index].getBuildName();
                    }
                    break;
                case "Auxiliary Building":
                    if (index < DATABASE.AuxBuilding.values().length) {
                        return DATABASE.AuxBuilding.values()[index].getBuildName();
                    }
                    break;
                case "Fortifications":
                    if (index < DATABASE.FortBuilding.values().length) {
                        return DATABASE.FortBuilding.values()[index].getBuildName();
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("Erreur récupération nom : " + e.getMessage());
        }
        return "Bâtiment inconnu";
    }
        private void updateSelectionBorder(JPanel parentPanel, JLabel selectedLabel) {
        for (Component component : parentPanel.getComponents()) {
            if (component instanceof JPanel) {
                JPanel itemPanel = (JPanel) component;
                Component iconContainer = itemPanel.getComponent(0);
                if (iconContainer instanceof JPanel) {
                    JPanel iconPanel = (JPanel) iconContainer;
                    for (Component icon : iconPanel.getComponents()) {
                        if (icon instanceof JLabel) {
                            JLabel iconLabel = (JLabel) icon;
                            if (iconLabel == selectedLabel) {
                                iconLabel.setBorder(createLineBorder(Color.RED, 3));
                            } else {
                                iconLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                            }
                        }
                    }
                }
            }
        }
    }
    void quittingwithbutton() {
        audio.playClick();
        audio.fadeOut();
        dispose();
        Login.mondeOpen = false;
    }
    void repaintwithbutton(ActionEvent e) {
        alphaCache.clear();
        MAPMONDE.repaint();
    }
    void selectIcon(JLabel iconLabel, int index, String buildingType) {
        for (Component comp : iconPanel.getComponents()) {
            if (comp instanceof JLabel label) {
                label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            }
        }
        System.out.println("Clicked on: " + buildingType + " icon with index: " + index);
        audio.playClick();
        iconLabel.setBorder(createLineBorder(Color.RED, 2));
        if (buildingType.equals("Main Building")) {
            currentIconMainIndex = index;
        } else if (buildingType.equals("Auxiliary Building")) {
            currentIconAuxIndex = index;
        } else {
            currentIconFortIndex = index;
        }
        System.out.println("Updated indices -> Java.Main: " + currentIconMainIndex + ", Aux: " + currentIconAuxIndex + ", Fort: " + currentIconFortIndex);

    }
    JButton detailedViewButton(String text, ImageIcon icon, int x, int y, int w, int h) {
        JButton button = new JButton(text);
        button.setFont(new Font("Oswald", Font.BOLD, 20));
        if (icon != null) {
            button.setIcon(icon);
        }
        button.setBounds(x, y, w, h);
        button.setOpaque(false);
        button.setBackground(new Color(0, 0, 0, 0));
        button.setBorderPainted(false);
        button.setForeground(new Color(0, 0, 0, 0));
        button.setContentAreaFilled(false);
        button.addActionListener(e -> {
            audio.playClick();
            if (Objects.equals(button.getText(), "X")) {
                closeDetailView();
            }
        });
        return button;
    }
    void setInitialView() {
        String targetHexName = "hex_19_23";
        int[] position = repo.getHexPosition(targetHexName);
        int targetX = position[0];
        int targetY = position[1];
        imgX = -targetX + (panelWidth / 2);
        imgY = -targetY + (panelHeight / 2);
        double minX = panelWidth - (imgWidth * zoomFactor);
        double minY = panelHeight - (imgHeight * zoomFactor);
        imgX = Math.max(minX, Math.min(imgX, 0));
        imgY = Math.max(minY, Math.min(imgY, 0));
        repaint();
    }

    void handleHexClick(int x, int y) {
        double colEstimate = (x - imgX) / ((3.0 / 2.0) * HEX_SIZE * zoomFactor);
        double rowEstimate = (y - imgY) / (Math.sqrt(3) * HEX_SIZE * zoomFactor);
        int baseCol = (int) Math.floor(colEstimate);
        int baseRow = (int) Math.floor(rowEstimate);
        for (int dCol = -1; dCol <= 1; dCol++) {
            for (int dRow = -1; dRow <= 1; dRow++) {
                int col = baseCol + dCol;
                int row = baseRow + dRow;

                if (col < 0 || col >= 100 || row < 0 || row >= 50) continue;
                if (isPointInHexagon(col, row, x, y)) {
                    labelclick = new Point(col, row);
                    UIHelpers.logseparator();
                    hexKey = "hex_" + labelclick.x + "_" + labelclick.y;
                    SafeHexDetails details = repo.getHexDetails(hexKey);
                    String currentFactionId = MainMenu.getCurrentFactionId();
                    System.out.println("hex cliqué : " + hexKey);
                    System.out.println("Claim : " + details.getFactionClaim());
                    System.out.println("Distance : " + isObscuredHex(col, row, currentFactionId));
                    UIHelpers.logseparator();
                    return;
                }
            }
        }
    }

    private boolean isPointInHexagon(int hexCol, int hexRow, int pointX, int pointY) {

        double hexCenterX = imgX + hexCol * ((3.0 / 2.0) * HEX_SIZE * zoomFactor);
        double hexCenterY = imgY + hexRow * (Math.sqrt(3) * HEX_SIZE * zoomFactor)
                + ((hexCol & 1) == 1 ? (Math.sqrt(3) * HEX_SIZE * zoomFactor) / 2 : 0);
        double relX = pointX - hexCenterX;
        double relY = pointY - hexCenterY;

        double hexRadius = HEX_SIZE * zoomFactor;
        if (relX * relX + relY * relY > hexRadius * hexRadius) {
            return false;
        }
        double abs_relX = Math.abs(relX);
        double abs_relY = Math.abs(relY);
        double apothem = hexRadius * Math.sqrt(3) / 2;
        if (abs_relX <= hexRadius / 2) {
            return abs_relY <= apothem;
        }
        double maxY = apothem - Math.sqrt(3) * (abs_relX - hexRadius / 2);
        return abs_relY <= maxY;
    }

    public static Rectangle getHexPixelBounds(String hexKey, IHexRepository repo) {
        int[] gridPos = repo.getHexPosition(hexKey);
        double rawCX = gridPos[0], rawCY = gridPos[1];
        double zx = zoomFactor;
        double zy = zoomFactor;
        double px = imgX + rawCX * zx;
        double py = imgY + rawCY * zy;
        int size = HEX_SIZE;
        int w = (int) (size * zx);
        int h = (int) (size * zy);
        return new Rectangle((int) (px - w / 2.0), (int) (py - h / 2.0), w, h);
    }

    public void initializeCache() {
        cacheLock.writeLock().lock();
        try {
            System.out.println("Initializing hex cache...");
            Map<String, SafeHexDetails> allHexes = repo.loadSafeAll();
            hexCache.clear();
            factionColorCache.clear();
            int validHexes = 0;
            int invalidHexes = 0;
            for (Map.Entry<String, SafeHexDetails> entry : allHexes.entrySet()) {
                String key = entry.getKey();
                SafeHexDetails details = entry.getValue();
                if (key == null || key.trim().isEmpty()) {
                    System.err.println("⚠️ Null/empty key detected, skipping");
                    invalidHexes++;
                    continue;
                }
                if (details == null) {
                    System.err.println("⚠️ Null HexDetails for key: " + key);
                    invalidHexes++;
                    continue;
                }
                if (!key.equals(details.getHexKey())) {
                    System.err.println("⚠️ Key mismatch: " + key + " != " + details.getHexKey());
                    invalidHexes++;
                    continue;
                }
                hexCache.put(key, details.deepCopy());
                String faction = details.getFactionClaim();
                if (!factionColorCache.containsKey(faction)) {
                    factionColorCache.put(faction, UIHelpers.getFactionColor(faction));
                }
                validHexes++;
            }
            System.out.println("Cache initialized - Valid: " + validHexes + ", Invalid: " + invalidHexes);
            cacheInitialized = true;
        } catch (Exception e) {
            System.err.println("Cache initialization failed: " + e.getMessage());
            e.printStackTrace();
            cacheInitialized = false;
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    private final Map<String, Image> emblemCache = new HashMap<>();
    private int getHexMainCapFromDATABASE() {
        int max = 0;
        for (DATABASE.MainBuilding building : DATABASE.MainBuilding.values()) {
            String tag = building.toString();
            if (tag.startsWith("Main")) {
                try {
                    int number = Integer.parseInt(tag.replaceAll("[^0-9]", ""));
                    if (number > max) {
                        max = number;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return max;
    }
    private void doClaimHex() {
        if (hexKey == null) {
            JOptionPane.showMessageDialog(this,
                    "Aucun hexagone sélectionné",
                    "Erreur", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SafeHexDetails details = repo.getHexDetails(hexKey);
        String newClaim;
        if (isAdmin && chosen != null) {
            newClaim = chosen.getId();
            System.out.println("Admin claimed: " + newClaim);
        } else {
            newClaim = MainMenu.getCurrentFactionId();
        }

        UIHelpers.logseparator();
        System.out.println("doClaimHex - hexKey: " + hexKey);
        System.out.println("newClaim: " + newClaim + " || Previous: " + details.getFactionClaim());
        UIHelpers.logseparator();

        if (!Objects.equals(details.getFactionClaim(), newClaim)) {
            ClaimValidationResult validationResult = validateClaimAttempt(details, newClaim);
            if (!validationResult.isValid()) {
                JOptionPane.showMessageDialog(this,
                        validationResult.getErrorMessage(),
                        "Claim Refusé", JOptionPane.WARNING_MESSAGE);
                return;
            }
            applyClaimConsequences(details, newClaim, validationResult);
            details.setFactionClaim(newClaim);
            Set<String> discovered = details.getDiscoveredByFaction();
            if (discovered == null) {
                discovered = new HashSet<>();
            }
            discovered.add(newClaim);

            repo.updateHexDetails(hexKey, details.deepCopy());

            cacheLock.writeLock().lock();
            try {
                hexCache.put(hexKey, details.deepCopy());
            } finally {
                cacheLock.writeLock().unlock();
            }
            alphaCache.clear();
            SwingUtilities.invokeLater(() -> MAPMONDE.repaint());
            if (detailViewOpen) {
                updateClaimIconInUI(newClaim);
            }
            if (validationResult.hasConsequences()) {
                JOptionPane.showMessageDialog(this,
                        "Hexagone conquis !\n" + validationResult.getSuccessMessage(),
                        "Conquête Réussie", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Hexagone réclamé avec succès !",
                        "Claim Réussi", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "Hexagone déjà claimé par " + newClaim,
                    "Information", JOptionPane.INFORMATION_MESSAGE);
        }
    }


    private void updateClaimIconInUI(String newFactionId) {
        try {
            Faction faction = FactionRegistry.getFactionId(newFactionId);
            ImageIcon updatedIcon = new ImageIcon(Objects.requireNonNull(
                    getClass().getResource(faction.getEmblemImage())));

            Image scaledImage = updatedIcon.getImage().getScaledInstance(180, 256, Image.SCALE_SMOOTH);
            claimIconLabel.setIcon(new ImageIcon(scaledImage));
            JPanel buildingPanel = (JPanel) detailsPanel.getComponent(0);
            buildingPanel.setBorder(createLineBorder(UIHelpers.getFactionColor(newFactionId), 5));

            detailsPanel.revalidate();
            detailsPanel.repaint();
        } catch (Exception e) {
            System.err.println("Erreur mise à jour UI claim: " + e.getMessage());
        }
    }

    private void closeDetailView() {
        if (detailViewOpen) {
            detailsPanel.setVisible(false);
            detailsPanel.removeAll();
            moredetailsPanel.setVisible(false);
            moredetailsPanel.removeAll();

            if (iconMenu != null && iconMenu.isDisplayable()) {
                iconMenu.dispose();
                iconMenu = null;
            }
            for (ActionListener al : Claim.getActionListeners()) {
                Claim.removeActionListener(al);
            }
            for (ActionListener al : SaveButton.getActionListeners()) {
                SaveButton.removeActionListener(al);
            }
            detailViewOpen = false;
        }
    }

    public void preloadAllImages() {
        SwingWorker<Void, String> imageLoader = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                System.out.println("Loading building icons...");
                ExecutorService executor = Executors.newFixedThreadPool(3);
                Future<?> mainTask = executor.submit(() -> loadMainIcons());
                Future<?> auxTask = executor.submit(() -> loadAuxIcons());
                Future<?> fortTask = executor.submit(() -> loadFortIcons());
                mainTask.get();
                auxTask.get();
                fortTask.get();
                executor.shutdown();
                System.out.println("Images loaded successfully");
                return null;
            }
        };
        imageLoader.execute();
    }

    private void renderHexWithFow(Graphics2D g2d, AffineTransform orig,
                                  double cx, double cy, double unitSize,
                                  SafeHexDetails data, int col, int row) {
        String viewingFactionId = (isAdmin && currentViewFactionId != null)
                ? currentViewFactionId
                : MainMenu.getCurrentFactionId();

        boolean isDiscovered = data.isDiscoveredBy(viewingFactionId);

        g2d.setTransform(orig);
        g2d.translate(cx, cy);
        g2d.scale(unitSize, unitSize);

        if (isAdmin && currentViewFactionId == null) {
            renderNormalHex(g2d, data);
        } else if (isDiscovered) {
            renderNormalHex(g2d, data);
        } else {
            float alpha = calculateDistanceAlpha(col, row, viewingFactionId);
            if (Float.isNaN(alpha)) {
                renderHiddenHex(g2d);
            }
            if (alpha < 0.9f) {
                renderProgressiveHex(g2d, alpha);
            } else {
                renderObscuredHex(g2d);
            }
        }
        g2d.setTransform(orig);
        if (isAdmin && currentViewFactionId == null) {
            drawHexLabelNormal(g2d, col, row, cx, cy, unitSize, data);
        } else if (isDiscovered) {
            drawHexLabelNormal(g2d, col, row, cx, cy, unitSize, data);
        } else {
            float alpha = calculateDistanceAlpha(col, row, viewingFactionId);
            if (!Float.isNaN(alpha)) {
                if (alpha > 0f && alpha < 0.9f) {
                    drawHexLabelFogged(g2d, col, row, cx, cy, alpha);
                } else {
                    drawHexLabelObscured(g2d, col, row, cx, cy);
                }
            }
        }
    }

    private float calculateDistanceAlpha(int col, int row, String factionId) {
        String key = col + "_" + row + "_" + factionId;
        if (alphaCache.containsKey(key)) {
            return alphaCache.get(key);
        }
        int minDistance = Integer.MAX_VALUE;
        for (Map.Entry<String, SafeHexDetails> entry : hexCache.entrySet()) {
            if (entry.getValue().isDiscoveredBy(factionId)) {
                String[] coords = entry.getKey().split("_");
                int hexCol = Integer.parseInt(coords[1]);
                int hexRow = Integer.parseInt(coords[2]);
                int distance = Math.abs(col - hexCol) + Math.abs(row - hexRow);
                minDistance = Math.min(minDistance, distance);
                if (minDistance == 0) break;
            }
        }
        float alpha;
        if (minDistance == 0) alpha = 0.0f;
        else if (minDistance == 1) alpha = 0.5f;
        else if (minDistance == 2) alpha = 0.7f;
        else if (minDistance == 3) alpha = 0.9f;
        else alpha = Float.NaN; // trop loin
        alphaCache.put(key, alpha);
        return alpha;
    }

    private void renderProgressiveHex(Graphics2D g2d, float alpha) {
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.setColor(Color.BLACK);
        g2d.fill(unitHexagon);
        g2d.setComposite(AlphaComposite.SrcOver);
        g2d.setColor(Color.DARK_GRAY);
        g2d.draw(unitHexagon);
    }

    private void renderNormalHex(Graphics2D g2d, SafeHexDetails data) {
        Color colr = UIHelpers.getFactionColor(data.getFactionClaim());
        if (!"Free".equals(data.getFactionClaim())) {
            g2d.setColor(new Color(colr.getRed(), colr.getGreen(), colr.getBlue(), 64));
            g2d.fill(unitHexagon);
        }
        g2d.setColor(Color.BLACK);
        g2d.draw(unitHexagon);
    }

    private void drawHexLabelNormal(Graphics2D g2d, int col, int row,
                                    double cx, double cy, double unitSize, SafeHexDetails data) {
        g2d.setFont(new Font("Arial", Font.BOLD, Math.max(8, (int) (14 * zoomFactor))));

        String label = "(" + col + "," + row + ")";
        hexLabels.put(new Point(col, row), label);

        FontMetrics fm = g2d.getFontMetrics();
        int textW = fm.stringWidth(label);
        int textH = fm.getAscent();
        int tx = (int) (cx - textW / 2.0);
        int ty = (int) (cy + textH / 2.0);

        String factionId = data.getFactionClaim();
        Color color = FactionRegistry.getColorFor(factionId);
        g2d.setColor(color);
        g2d.drawString(label, tx, ty);

        drawEmblemIfNotFree(g2d, data, cx, cy, unitSize, textW);
    }

    private void drawHexLabelFogged(Graphics2D g2d,
                                    int col, int row, double cx, double cy, float alpha) {
        String label = "(" + col + "," + row + ")";
        hexLabels.put(new Point(col, row), label);
        int grayLevel = (int) (255 * (1 - alpha));
        grayLevel = Math.max(50, Math.min(grayLevel, 200));
        Color foggedColor = new Color(grayLevel, grayLevel, grayLevel);
        g2d.setFont(new Font("Arial", Font.BOLD, Math.max(8, (int) (14 * zoomFactor))));
        FontMetrics fm = g2d.getFontMetrics();
        int textW = fm.stringWidth(label);
        int textH = fm.getAscent();
        int tx = (int) (cx - textW / 2.0);
        int ty = (int) (cy + textH / 2.0);
        g2d.setColor(foggedColor);
        g2d.drawString(label, tx, ty);
    }

    private void renderObscuredHex(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.fill(unitHexagon);
        g2d.setColor(Color.DARK_GRAY);
        g2d.draw(unitHexagon);
    }

    private void drawHexLabelObscured(Graphics2D g2d, int col, int row,
                                      double cx, double cy) {
        g2d.setFont(new Font("Arial", Font.BOLD, Math.max(8, (int) (14 * zoomFactor))));

        String label = "???";
        hexLabels.put(new Point(col, row), label);

        FontMetrics fm = g2d.getFontMetrics();
        int textW = fm.stringWidth(label);
        int textH = fm.getAscent();
        int tx = (int) (cx - textW / 2.0);
        int ty = (int) (cy + textH / 2.0);
        g2d.setColor(Color.WHITE);
        g2d.drawString(label, tx, ty);
    }

    private void renderHiddenHex(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.fill(unitHexagon);
        g2d.setColor(Color.BLACK);
        g2d.draw(unitHexagon);
    }

    private void drawEmblemIfNotFree(Graphics2D g2d, SafeHexDetails data,
                                     double cx, double cy, double unitSize, int textW) {
        String factionId = data.getFactionClaim();
        Faction faction = FactionRegistry.getFactionId(factionId);

        if (!"Free".equals(faction.getId())) {
            String emblemPath = faction.getEmblemImage();
            Image raw = emblemCache.computeIfAbsent(emblemPath, p -> {
                try {
                    return ImageIO.read(Objects.requireNonNull(getClass().getResource(p)));
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            });
            if (raw != null) {
                double hexW = 2 * unitSize;
                int eW = (int) (hexW * 0.2);
                double aspect = (double) raw.getHeight(null) / raw.getWidth(null);
                int eH = (int) (eW * aspect);
                int ex = (int) (cx + textW / 2.0 + 5);
                int ey = (int) (cy - eH / 2.0);
                g2d.drawImage(raw, ex, ey, eW, eH, null);
            }
        }
    }

    public void loadMainIcons() {
        for (int i = 0; i <= hexMainCap; i++) {
            URL resourceUrl = getClass().getResource("/hexmain/main" + i + ".png");
            if (resourceUrl != null) {
                ImageIcon originalIcon = new ImageIcon(resourceUrl);
                originalMainIcon.put(i, originalIcon);
                Image scaled100 = originalIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                scaledIconsMain.put(i, new ImageIcon(scaled100));
            }
        }
    }
    public void loadAuxIcons() {
        int hexAuxCap = 2;
        for (int i = 0; i <= hexAuxCap; i++) {
            URL resourceUrl = getClass().getResource("/hexaux/Aux" + i + ".png");
            if (resourceUrl != null) {
                ImageIcon originalIcon = new ImageIcon(resourceUrl);
                originalAuxIcon.put(i, originalIcon);
                Image scaled100 = originalIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                scaledIconsAux.put(i, new ImageIcon(scaled100));
            }
        }
    }
    public void loadFortIcons() {
        int hexFortCap = 5;
        for (int i = 0; i <= hexFortCap; i++) {
            URL resourceUrl = getClass().getResource("/hexfort/fort" + i + ".png");
            if (resourceUrl != null) {
                ImageIcon originalIcon = new ImageIcon(resourceUrl);
                originalFortIcon.put(i, originalIcon);
                Image scaled100 = originalIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                scaledIconsFort.put(i, new ImageIcon(scaled100));
            }
        }
    }

    private void openFowManagementDialog(String hexKey) {
        Collection<Faction> allFactions = FactionRegistry.all();
        String[] playerFactionIds = allFactions.stream()
                .filter(Faction::getIsPlayer)
                .map(Faction::getId)
                .toArray(String[]::new);


        SafeHexDetails hex = repo.getHexDetails(hexKey);
        Set<String> discovered = new HashSet<>(hex.getDiscoveredByFaction());

        JPanel panel = new JPanel(new GridLayout(0, 2));
        Map<String, JCheckBox> map = new HashMap<>();

        for (String fid : playerFactionIds) {
            JCheckBox cb = new JCheckBox(fid, discovered.contains(fid));
            map.put(fid, cb);
            panel.add(cb);
        }

        int res = JOptionPane.showConfirmDialog(
                this, panel,
                "Gérer Fog of War - " + hexKey,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (res == JOptionPane.OK_OPTION) {
            discovered.clear();
            for (Map.Entry<String, JCheckBox> entry : map.entrySet()) {
                if (entry.getValue().isSelected()) {
                    discovered.add(entry.getKey());
                }
            }
            hex.setDiscoveredByFaction(discovered);
            repo.updateHexDetails(hexKey, hex);

            JOptionPane.showMessageDialog(
                    this,
                    "✅ Visibilité mise à jour pour " + discovered.size() + " factions.",
                    "Succès",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    public void initializePlayerFactionList() {
        Collection<Faction> allFactions = FactionRegistry.all();
        playerFactionList = allFactions.stream()
                .filter(Faction::getIsPlayer)
                .map(Faction::getId)
                .collect(Collectors.toList());

        System.out.println("🎮 Factions disponibles pour la vue: " + playerFactionList);
    }

    private void toggleFactionView() {
        if (playerFactionList.isEmpty()) {
            return;
        }
        currentFactionViewIndex++;

        if (currentFactionViewIndex >= playerFactionList.size()) {
            currentFactionViewIndex = -1;
            currentViewFactionId = null;
            factionViewToggle.setText("View: Normal");
            System.out.println("🔄 [ADMIN] Vue basculée vers: Normal (vue admin)");
        } else {
            currentViewFactionId = playerFactionList.get(currentFactionViewIndex);
            factionViewToggle.setText("View: " + currentViewFactionId);
            System.out.println("🔄 [ADMIN] Vue basculée vers: " + currentViewFactionId);
        }
        audio.playClick();
        alphaCache.clear();
        MAPMONDE.repaint();

    }

    private boolean isObscuredHex(int col, int row, String factionId) {
        float alpha = calculateDistanceAlpha(col, row, factionId);
        return alpha >= 0.9f && alpha <= 1.0f;
    }

    private void onHexClicked(String hexKey) {
        SafeHexDetails details = repo.getHexDetails(hexKey);
        String[] parts = hexKey.split("_");
        if (parts.length == 3) {
            int col = Integer.parseInt(parts[1]);
            int row = Integer.parseInt(parts[2]);
            boolean isDiscovered = details.isDiscoveredBy(MainMenu.getCurrentFactionId());
            boolean isNearButFar = isObscuredHex(col, row, MainMenu.getCurrentFactionId());
            if (isAdmin || isDiscovered) {
                if (detailViewOpen) closeDetailView();
                resetIndexValue();
                DetailedView();
                return;
            }
            if (!isNearButFar) {
                JOptionPane.showMessageDialog(MAPMONDE,
                        "🌫️ Exploration.Placeholder",
                        "Zone inconnue", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(MAPMONDE,
                        "🌫️ Territoire inexploré\nVous devez d'abord explorer les zones précédentes.",
                        "Zone inconnue", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void setupBasicComponents() {
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60 * i);
            double xx = Math.cos(angle);
            double yy = Math.sin(angle);
            if (i == 0) {
                unitHexagon.moveTo(xx, yy);
            } else {
                unitHexagon.lineTo(xx, yy);
            }
        }
        unitHexagon.closePath();

        setupWindowProperties();
        setupBasicPanels();
    }
    private void  setupWindowProperties(){
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;
        int windowWidth = 1600;
        int windowHeight = 900;
        int x = (screenWidth - windowWidth) / 2;
        int y = (screenHeight - windowHeight) / 2;

        setTitle("Carte Du Jeu");
        setSize(windowWidth, windowHeight);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        setLocation(x, y);

        setFocusable(true);
        requestFocusInWindow();

        try {
            ImageIcon Icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/Icon.png")));
            setIconImage(Icon.getImage());
        } catch (Exception e) {
            System.err.println("Impossible de charger l'icône de fenêtre");
        }
    }
    private void setupBasicPanels(){layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(getWidth(), getHeight()));
        setContentPane(layeredPane);
        layeredPane.setLayout(null);

        try {
            mapmonde = ImageIO.read(Objects.requireNonNull(getClass().getResource("/Iseria.png")));
            imgWidth = mapmonde.getWidth();
            imgHeight = mapmonde.getHeight();

            BufferedImage MousePressed = ImageIO.read(Objects.requireNonNull(getClass().getResource("/iconmousemap2.png")));
            MapDragging = Toolkit.getDefaultToolkit().createCustomCursor(MousePressed, new Point(8, 8), "MapDragging");
        } catch (IOException e) {
            System.err.println("Erreur chargement ressources images: " + e.getMessage());
        }

        double horizontalcenter = 6.809;
        double verticalcenter = 1.9826;
        panelWidth = layeredPane.getWidth();
        imgX = (panelWidth - imgWidth) / horizontalcenter;
        panelHeight = layeredPane.getHeight();
        imgY = (panelHeight - imgHeight) / verticalcenter;

        MAPMONDE = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                if (cacheInitialized) {
                    renderFullMap(g);
                } else {
                    g.setColor(Color.BLACK);
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(Color.WHITE);
                    g.drawString("Chargement en cours...", getWidth()/2 - 50, getHeight()/2);
                }
            }
        };

        MAPMONDE.setBounds(0, 0, getWidth(), getHeight());
        MAPMONDE.setOpaque(false);

        moredetailsPanel.setLayout(new BorderLayout());
        moredetailsPanel.setBounds(0, 75, 800, 900);
        moredetailsPanel.setOpaque(false);
        moredetailsPanel.setVisible(false);

        detailsPanel.setLayout(new BorderLayout());
        detailsPanel.setBounds(0, 75, 800, 900);
        detailsPanel.setBackground(new Color(72, 108, 104, 255));
        detailsPanel.setOpaque(true);
        detailsPanel.setVisible(false);

        setupBasicButtons();
    }
    private void setupBasicButtons() {
        quitButton.setBounds(1450, 800, 80, 30);
        quitButton.addActionListener(e -> {
            quittingwithbutton();
            Login.mondeOpen = false;
        });

        repaintButton.setBounds(1450, 750, 80, 30);
        repaintButton.addActionListener(this::repaintwithbutton);

        resetButton.setBounds(1450, 700, 80, 30);
        resetButton.addActionListener(e -> repo.clearAllFactionClaims());

        factionViewToggle.setText("View: Normal");

        factionViewToggle.setBounds(1450, 650, 130, 30);
        factionViewToggle.setFont(new Font("Arial", Font.BOLD, 10));
        factionViewToggle.setVisible(false);
        factionViewToggle.addActionListener(e -> toggleFactionView());
    }
    private void renderFullMap(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        if (mapmonde != null) {
            int w = (int) (imgWidth * zoomFactor);
            int h = (int) (imgHeight * zoomFactor);
            g2d.drawImage(mapmonde, (int) imgX, (int) imgY, w, h, null);
        }
        renderHexagons(g2d);
        g2d.dispose();
    }
    private void renderHexagons(Graphics2D g2d){

        double unitSize = HEX_SIZE * zoomFactor;
        double horiz = 1.5 * unitSize;
        double vert = Math.sqrt(3) * unitSize;

        float stroke = 0.01f;
        g2d.setStroke(new BasicStroke(stroke, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        AffineTransform orig = g2d.getTransform();

        int ROWS = 50;
        int COLS = 100;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                double cx = imgX + col * horiz;
                double cy = imgY + row * vert + ((col & 1) == 1 ? vert * 0.5 : 0);

                String key = "hex_" + col + "_" + row;
                SafeHexDetails data = hexCache.get(key);
                if (data == null) continue;

                renderHexWithFow(g2d, orig, cx, cy, unitSize, data, col, row);
            }
        }

        g2d.dispose();
    }
    private void performAllInitialization() {
        initializePlayerFactionList();
        repo.addAllHexes(50, 100);
        initializeCache();
        cacheInitialized = true;
        preloadAllImages();
        loadMainIcons();

        finalizeUISetup();
    }
    public void finalizeUISetup() {

        layeredPane.add(MAPMONDE, Integer.valueOf(1));
        layeredPane.add(detailsPanel, Integer.valueOf(2));
        layeredPane.add(moredetailsPanel, Integer.valueOf(2));
        layeredPane.add(quitButton, Integer.valueOf(3));
        layeredPane.add(repaintButton, Integer.valueOf(3));
        layeredPane.add(resetButton, Integer.valueOf(3));
        layeredPane.add(factionViewToggle, Integer.valueOf(3));

        if (Login.currentUser.equals("Admin")) {
            factionViewToggle.setVisible(true);
        }

        if (!MainMenu.isFactionMenuOpen && Login.mondeOpen && LoadingWindow.audioOkStart) {
            audio.playHexMusicMenu();
        }

        SwingUtilities.invokeLater(() -> {
            panelWidth = layeredPane.getWidth();
            panelHeight = layeredPane.getHeight();
            repaint();
            setInitialView();
        });
    }


    private static class ClaimValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final String successMessage;
        private final boolean hasConsequences;
        private final boolean requiresDestruction;

        public ClaimValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.successMessage = null;
            this.hasConsequences = false;
            this.requiresDestruction = false;
        }

        public ClaimValidationResult(boolean valid, String successMessage, boolean hasConsequences, boolean requiresDestruction) {
            this.valid = valid;
            this.errorMessage = null;
            this.successMessage = successMessage;
            this.hasConsequences = hasConsequences;
            this.requiresDestruction = requiresDestruction;
        }

        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        public String getSuccessMessage() { return successMessage; }
        public boolean hasConsequences() { return hasConsequences; }
        public boolean requiresDestruction() { return requiresDestruction; }
    }
    private ClaimValidationResult validateClaimAttempt(SafeHexDetails details, String newClaimerFactionId) {
        String currentOwner = details.getFactionClaim();
        Faction ownerFaction = FactionRegistry.getFactionId(currentOwner);
        if ("Free".equals(currentOwner) || currentOwner == null) {
            return new ClaimValidationResult(true, "Territoire libre réclamé.", false, false);
        }
        boolean currentOwnerIsPlayer = isPlayerFaction(currentOwner);
        boolean newClaimerIsPlayer = isPlayerFaction(newClaimerFactionId);
        if (currentOwnerIsPlayer && newClaimerIsPlayer) {
            boolean hasBuildings = hasAnyBuildings(details);

            if (hasBuildings) {
                StringBuilder buildingInfo = new StringBuilder();
                buildingInfo.append("Faction : ").append(ownerFaction).append("\n");
                buildingInfo.append("Hexagone protégé par les Conventions des Maisonnées\n" + "Déclarez la guerre pour concquérir un hexagone exploité\n");

                if (details.getMainBuildingIndex() > 0) {
                    String buildingName = getBuildingName("Main Building", details.getMainBuildingIndex());
                    buildingInfo.append("• Bâtiment Principal: ").append(buildingName).append("\n");
                }
                if (details.getAuxBuildingIndex() > 0) {
                    String buildingName = getBuildingName("Auxiliary Building", details.getAuxBuildingIndex());
                    buildingInfo.append("• Bâtiment Auxiliaire: ").append(buildingName).append("\n");
                }
                if (details.getFortBuildingIndex() > 0) {
                    String buildingName = getBuildingName("Fortifications", details.getFortBuildingIndex());
                    buildingInfo.append("• Fortification: ").append(buildingName).append("\n");
                }

                buildingInfo.append("\nVous devez d'abord détruire la garnison de ces bâtiments pour conquérir cet hexagone.");

                return new ClaimValidationResult(false, buildingInfo.toString());
            } else {
                return new ClaimValidationResult(true,
                        "Territoire de " + ownerFaction.getDisplayName() + " conquis (aucun bâtiment présent).",
                        true, false);
            }
        }
        if (isAdmin) {
            return new ClaimValidationResult(true, "Claim administrateur appliqué.", true, false);
        }
        if (!currentOwnerIsPlayer && newClaimerIsPlayer) {
            boolean hasBuildings = hasAnyBuildings(details);
            if (hasBuildings) {
                return new ClaimValidationResult(true,
                        "Territoire Séculaire conquis. Bâtiments détruits lors de la conquête.",
                        true, true);
            } else {
                return new ClaimValidationResult(true,
                        "Territoire Séculaire réclamé.",
                        true, false);
            }
        }
        return new ClaimValidationResult(true, "Claim autorisé.", false, false);
    }
    private boolean isPlayerFaction(String factionId) {
        if (factionId == null || "Free".equals(factionId)) {
            return false;
        }
        try {
            Faction faction = FactionRegistry.getFactionId(factionId);
            return faction != null && faction.getIsPlayer();
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification du type de faction: " + e.getMessage());
            return false;
        }
    }
    private boolean hasAnyBuildings(SafeHexDetails details) {
        return details.getMainBuildingIndex() > 0 ||
                details.getAuxBuildingIndex() > 0 ||
                details.getFortBuildingIndex() > 0;
    }
    private void applyClaimConsequences(SafeHexDetails details, String newClaimerFactionId, ClaimValidationResult result) {
        if (result.requiresDestruction()) {
            System.out.println("Destruction des bâtiments lors de la conquête...");
            if (details.getMainBuildingIndex() > 0) {
                System.out.println("Destruction: " + getBuildingName("Main Building", details.getMainBuildingIndex()));
                details.setMainBuildingIndex(0);
                details.setMainWorkerCount(0);
            }
            if (details.getAuxBuildingIndex() > 0) {
                System.out.println("Destruction: " + getBuildingName("Auxiliary Building", details.getAuxBuildingIndex()));
                details.setAuxBuildingIndex(0);
                details.setAuxWorkerCount(0);
            }
            if (details.getFortBuildingIndex() > 0) {
                System.out.println("Destruction: " + getBuildingName("Fortifications", details.getFortBuildingIndex()));
                details.setFortBuildingIndex(0);
                details.setFortWorkerCount(0);
            }
        }

        if (result.hasConsequences()) {
            // TODO ajouter d'autres effets:
            // - Perte de moral pour la faction perdante
            // - Bonus de moral pour la faction conquérante
            // - Modifications économiques
            // - Événements spéciaux

            System.out.println("Conséquences appliquées pour le claim de " + newClaimerFactionId);
        }
    }

}

