package com.iseria.ui;

import com.iseria.domain.*;
import com.iseria.domain.DATABASE;

import com.iseria.infra.FactionRegistry;
import com.iseria.service.EconomicDataService;
import com.iseria.service.LogisticsService;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.awt.geom.Path2D;
import java.awt.geom.AffineTransform;


import static com.iseria.ui.MainMenu.*;
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

    boolean isSaving = false;
    private JButton quitButton = new JButton("Quit");
    private JButton repaintButton = new JButton("repaint");
    private JButton MainLabel = new JButton();
    private JButton AuxLabel = new JButton();
    private JButton FortLabel = new JButton();
    private JButton SaveButton = new JButton();
    private JButton MoreDetail = new JButton();
    private JButton PrintGrid = new JButton();
    private JButton Claim = new JButton();
    private int hexMainCap = getHexMainCapFromDATABASE();
    private int hexAuxCap = 2;
    private int hexFortCap = 5;
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
    private final Path2D unitHexagon;
    private Map<Integer, ImageIcon> scaledIconsMain = new HashMap<>();
    private Map<Integer, ImageIcon> scaledIconsAux = new HashMap<>();
    private Map<Integer, ImageIcon> scaledIconsFort = new HashMap<>();
    private Map<Point, String> hexLabels = new HashMap<>();
    String hexKey;
    IDataProvider data;
    IAudioService audio;
    IHexRepository repo;
    Point labelclick;
    private Map<String, HexDetails> hexCache = new HashMap<>();
    private Map<String, Color> factionColorCache = new HashMap<>();
    private boolean cacheInitialized = false;
    public  boolean isAdmin = "Admin".equals(Login.currentUser);
    private Faction chosen;
    String emblemPath = FactionRegistry.getEmblemPathFor(getCurrentFactionId());
    private boolean detailViewOpen = false;


    Mondes(IDataProvider data, IAudioService audio, IHexRepository repo) {
        this.data = data;
        this.audio = audio;
        this.repo = repo;

        HexDetails details = repo.getHexDetails(hexKey);
        unitHexagon = new Path2D.Double();
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
        Rectangle2D bounds = unitHexagon.getBounds2D();
        System.out.println("Unit hexagon bounds: " + bounds);
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

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ESCAPE -> {
                        if (detailViewOpen) closeDetailView();
                        else { dispose(); Login.mondeOpen = false; }
                    }
                    case KeyEvent.VK_C -> ClaimButtonKeyTrigger(e);
                    case KeyEvent.VK_M -> MoreDetailedView();
                    case KeyEvent.VK_S -> saveHexDetails();
                }
            }
        });

        setFocusable(true);
        requestFocusInWindow();


        layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(windowWidth, windowHeight));
        setContentPane(layeredPane);
        layeredPane.setLayout(null);

        try {
            mapmonde = ImageIO.read(Objects.requireNonNull(getClass().getResource("/Iseria.png")));
            imgWidth = mapmonde.getWidth();
            imgHeight = mapmonde.getHeight();
            BufferedImage MousePressed = ImageIO.read(getClass().getResource("/iconmousemap2.png"));
            MapDragging = Toolkit.getDefaultToolkit().createCustomCursor(MousePressed, new Point(8, 8), "MapDragging");
        } catch (IOException e) {
            e.printStackTrace();
        }

        MAPMONDE = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponents(g);
                Graphics2D g2d = (Graphics2D)g.create();

                // High-quality rendering
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                        RenderingHints.VALUE_STROKE_PURE);

                // Draw background map
                if (mapmonde != null) {
                    int w = (int)(imgWidth * zoomFactor);
                    int h = (int)(imgHeight * zoomFactor);
                    g2d.drawImage(mapmonde, (int)imgX, (int)imgY, w, h, null);
                }

                // Compute sizes
                double unitSize = HEX_SIZE * zoomFactor;
                double horiz = 1.5 * unitSize;
                double vert  = Math.sqrt(3) * unitSize;

                // One stroke for all borders
                float stroke = 0.01f;
                g2d.setStroke(new BasicStroke(stroke, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

                // Save original transform
                AffineTransform orig = g2d.getTransform();

                // Loop rows/cols
                int ROWS = 50;
                int COLS = 100;



                for (int row = 0; row < ROWS; row++) {
                    for (int col = 0; col < COLS; col++) {
                        // Pixel center of this hex
                        double cx = imgX + col * horiz;
                        double cy = imgY + row * vert + ((col & 1) == 1 ? vert * 0.5 : 0);

                        // Fetch data
                        String key = "hex_" + col + "_" + row;
                        HexDetails data = hexCache.get(key);
                        if (data == null) continue;

                        // Reset and position
                        g2d.setTransform(orig);
                        g2d.translate(cx, cy);
                        g2d.scale(unitSize, unitSize);

                        // Fill (semi-transparent)
                        Color colr = UIHelpers.getFactionColor(data.getFactionClaim());
                        if (!"Free".equals(data.getFactionClaim())) {
                            g2d.setColor(new Color(colr.getRed(), colr.getGreen(), colr.getBlue(), 64));
                            g2d.fill(unitHexagon);
                        }

                        // Draw border
                        g2d.setColor(Color.BLACK);
                        g2d.draw(unitHexagon);
                    }
                }

                // Restore for labels
                g2d.setTransform(orig);
                g2d.setFont(new Font("Arial", Font.BOLD, Math.max(8, (int)(14 * zoomFactor))));

                // Draw labels *after* borders so they’re always on top
                for (int row = 0; row < ROWS; row++) {
                    for (int col = 0; col < COLS; col++) {
                        String key = "hex_" + col + "_" + row;
                        if (!hexCache.containsKey(key)) continue;
                        double cx = imgX + col * horiz;
                        double cy = imgY + row * vert + ((col & 1) == 1 ? vert * 0.5 : 0);
                        drawHexLabelCached(g2d, col, row, cx, cy, unitSize);
                    }
                }

                g2d.dispose();
            }
        };

        double horizontalcenter = 6.809;
        double verticalcenter = 1.9826;

        panelWidth = layeredPane.getWidth();
        imgX = (panelWidth - imgWidth) / horizontalcenter;
        panelHeight = layeredPane.getHeight();
        imgY = (panelHeight - imgHeight) / verticalcenter;

        MAPMONDE.setBounds(0, 0, windowWidth, windowHeight);
        MAPMONDE.setOpaque(false);
        Mapmonddemouse();

        moredetailsPanel.setLayout(new BorderLayout());
        moredetailsPanel.setBounds(0, 75, 800, 900);
        moredetailsPanel.setOpaque(false);
        moredetailsPanel.setVisible(false);

        detailsPanel.setLayout(new BorderLayout());
        detailsPanel.setBounds(0, 75, 800, 900);
        detailsPanel.setBackground(new Color(72, 108, 104, 255));
        detailsPanel.setOpaque(true);
        detailsPanel.setVisible(false);

        quitButton.setBounds(1450, 800, 80, 30);
        quitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                quittingwithbutton(e);
                Login.mondeOpen = false;
            }

        });
        repaintButton.setBounds(1450, 750, 80, 30);
        repaintButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                repaintwithbutton(e);
            }

        });
        JButton resetButton = new JButton("resetClaims");
        resetButton.setBounds(1450,700,80,30);
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                repo.clearAllFactionClaims();
            }
        });

        repo.addAllHexes(100, 50);
        initializeCache();
        cacheInitialized = true;
        preloadAllImages();
        loadMainIcons();


        layeredPane.add(MAPMONDE, Integer.valueOf(1));
        layeredPane.add(detailsPanel, Integer.valueOf(2));
        layeredPane.add(moredetailsPanel, Integer.valueOf(2));
        layeredPane.add(quitButton, Integer.valueOf(3));
        layeredPane.add(repaintButton, Integer.valueOf(3));
        layeredPane.add(resetButton, Integer.valueOf(3));
        setVisible(true);
        if (!MainMenu.isFactionMenuOpen && Login.mondeOpen) {
            audio.playHexMusicMenu();
        }


        SwingUtilities.invokeLater(() -> {
            panelWidth = layeredPane.getWidth();
            panelHeight = layeredPane.getHeight();
            repaint();
            setInitialView();

        });


    }

    void Mapmonddemouse() {
        MAPMONDE.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
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

            }
        });

        MAPMONDE.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {  //Mapdrag
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

                } else if (((!detailsPanel.isVisible() || !moredetailsPanel.isVisible() || !detailsPanel.getBounds().contains(e.getPoint()))
                ) && (e.getButton() == MouseEvent.BUTTON3)) {

                    audio.playClick();
                    handleHexClick(e.getX(), e.getY());
                    String label = hexLabels.get(labelclick);
                    if (label == null) {
                        // clicked empty hex—ignore
                        return;
                    }
                    if (detailViewOpen) {
                        closeDetailView();
                    }
                    resetIndexValue();
                    DetailedView();
                }

            }

            @Override
            public void mouseReleased(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
                quitButton.repaint();
            }

        });
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

            //System.out.println(hexKey);
            HexDetails details = repo.getHexDetails(hexKey);
            currentIconMainIndex = details.getMainBuildingIndex();
            currentIconAuxIndex = details.getAuxBuildingIndex();
            currentIconFortIndex = details.getFortBuildingIndex();

        ImageIcon hexIcon = new ImageIcon(getClass().getResource("/hex4.png"));

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
        );        Image scaledClaimImage = originalClaimIcon.getImage().getScaledInstance(
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
        SaveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveButtonTrigger(e);
            }
        });

        MoreDetail.setFont(new Font("Oswald", Font.BOLD, 15));
        MoreDetail.setText("More Details");
        MoreDetail.setBounds(0, 700, 250, 75);
        MoreDetail.setOpaque(true);
        MoreDetail.setBackground(Color.lightGray);
        MoreDetail.setBorderPainted(false);
        MoreDetail.setForeground(Color.BLACK);
        MoreDetail.setContentAreaFilled(true);
        MoreDetail.setFocusable(false);
        MoreDetail.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MoreDetaiButtonTrigger(e);
            }
        });

        Claim.setFont(new Font("Oswald", Font.BOLD, 15));
        Claim.setText("Claim The Hex");
        Claim.setBounds(540, 700, 250, 75);
        Claim.setOpaque(true);
        Claim.setBackground(Color.lightGray);
        Claim.setBorderPainted(false);
        Claim.setForeground(Color.BLACK);
        Claim.setContentAreaFilled(true);
        Claim.setFocusable(false);
        Claim.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if ("Admin".equals(Login.currentUser)) {

                    Collection<Faction> allFactions = FactionRegistry.all();
                    Faction[] factionArray = allFactions.toArray(new Faction[0]);

                    Faction selectedFaction = (Faction) JOptionPane.showInputDialog(
                            detailsPanel,
                            "Choisissez la faction :",
                            "Admin Override",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            factionArray,
                            factionArray
                    );

                    if (selectedFaction != null) {
                        chosen = selectedFaction;
                        details.setFactionClaim(chosen.getId());
                        ClaimButtonTrigger(e); //AdminTrigger
                        ImageIcon updatedIcon = new ImageIcon(Objects.requireNonNull(getClass().getResource(chosen.getEmblemImage())));
                        Image scaledUpdatedImageChosen = updatedIcon.getImage().getScaledInstance(
                                180,
                                256,
                                Image.SCALE_SMOOTH
                        );
                        saveHexDetails();
                        SwingUtilities.invokeLater(() -> {
                            claimIconLabel.setIcon(new ImageIcon(scaledUpdatedImageChosen));
                            buildingPanel.setBorder(BorderFactory.createLineBorder(chosen.getFactionColor(), 5));
                            detailsPanel.revalidate();
                            detailsPanel.repaint();
                        });


                    }
                }
                else {
                String userFactionId = MainMenu.getCurrentFactionId();
                Faction userFaction = MainMenu.getCurrentUserFaction();
                String emblemPath = userFaction.getEmblemImage();
                ClaimButtonTrigger(e);

                if (userFactionId != null) {
                    ImageIcon updatedIcon = new ImageIcon(Objects.requireNonNull(getClass().getResource(emblemPath)));
                    Image scaledUpdatedImage = updatedIcon.getImage().getScaledInstance(
                            180,
                            256,
                            Image.SCALE_SMOOTH
                    );

                    saveHexDetails();
                    SwingUtilities.invokeLater(() -> {
                        claimIconLabel.setIcon(new ImageIcon(scaledUpdatedImage));
                        buildingPanel.setBorder(createLineBorder(
                                UIHelpers.getFactionColor(details.getFactionClaim()), 5));
                        claimIconLabel.revalidate();
                        claimIconLabel.repaint();
                        buildingPanel.repaint();
                        buildingPanel.revalidate();
                        detailsPanel.repaint();
                        detailsPanel.updateUI();
                    });



                    }
                }
            }
        });


        ImageIcon closeIcon = new ImageIcon(getClass().getResource("/close.png"));
        Image scaledImage = closeIcon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
        closeIcon = new ImageIcon(scaledImage);
        JButton closeButton = detailedViewButton("X", closeIcon, 770, -10, 50, 50);


        MainLabel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainBuidindButtonMenu(e);
            }
        });

        AuxLabel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AuxilliaryButtonMenu(e);
            }
        });

        FortLabel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FortificationButtonMenu(e);
            }
        });


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

    void MoreDetailedView(){
        detailsPanel.setVisible(false);
        moredetailsPanel.removeAll();

        JPanel moreBuildingPanel = new JPanel();

        // Boutons de navigation
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

        // NOUVEAU: Bouton Logistics
        JButton LogisticsDetail = new JButton();
        LogisticsDetail.setFont(new Font("Oswald", Font.BOLD, 15));
        LogisticsDetail.setText("🚚 Logistics");
        LogisticsDetail.setBounds(210, 700, 200, 75);
        LogisticsDetail.setOpaque(true);
        LogisticsDetail.setBackground(new Color(70, 130, 180)); // Steel blue
        LogisticsDetail.setBorderPainted(false);
        LogisticsDetail.setForeground(Color.WHITE);
        LogisticsDetail.setContentAreaFilled(true);
        LogisticsDetail.setFocusable(false);
        LogisticsDetail.addActionListener(e -> {
            openLogisticsForHex(hexKey);
        });

        // Bouton Production (existant ou nouveau)
        JButton ProductionDetail = new JButton();
        ProductionDetail.setFont(new Font("Oswald", Font.BOLD, 15));
        ProductionDetail.setText("🏭 Production");
        ProductionDetail.setBounds(420, 700, 200, 75);
        ProductionDetail.setOpaque(true);
        ProductionDetail.setBackground(new Color(34, 139, 34)); // Forest green
        ProductionDetail.setBorderPainted(false);
        ProductionDetail.setForeground(Color.WHITE);
        ProductionDetail.setContentAreaFilled(true);
        ProductionDetail.setFocusable(false);
        ProductionDetail.addActionListener(e -> {
            openProductionForHex(hexKey);
        });

        ImageIcon closeIcon = new ImageIcon(getClass().getResource("/close.png"));
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

        HexDetails details = repo.getHexDetails(hexKey);
        moredetailsPanel.setBorder(createLineBorder(UIHelpers.getFactionColor(details.getFactionClaim()), 5));

        // Informations de l'hexagone
        JTextArea hexInfoArea = new JTextArea();
        hexInfoArea.setEditable(false);
        hexInfoArea.setOpaque(true);
        hexInfoArea.setBackground(new Color(40, 40, 40, 200));
        hexInfoArea.setForeground(Color.WHITE);
        hexInfoArea.setFont(new Font("Courier New", Font.PLAIN, 12));

        // Remplir les informations
        StringBuilder info = new StringBuilder();
        info.append("=== INFORMATIONS HEXAGONE ===\n");
        info.append("Position: ").append(hexKey).append("\n");
        info.append("Faction: ").append(details.getFactionClaim()).append("\n");
        info.append("Population totale: ").append(details.getTotalWorkers()).append("\n\n");

        info.append("=== BÂTIMENTS ===\n");
        info.append("Principal: ").append(UIHelpers.getBuildingNamesSafe(
                details.getMainBuildingIndex(), details.getAuxBuildingIndex(), details.getFortBuildingIndex())).append("\n\n");

        info.append("=== LOGISTIQUE ===\n");
        info.append("Véhicules assignés: ").append(details.getAssignedVehicles().size()).append("\n");
        info.append("Routes disponibles: ").append(details.getLogisticsData().hasRoad() ? "Oui" : "Non").append("\n");
        info.append("Accès maritime: ").append(details.getLogisticsData().hasSea() ? "Oui" : "Non").append("\n");
        info.append("Accès fluvial: ").append(details.getLogisticsData().hasRiver() ? "Oui" : "Non").append("\n");

        hexInfoArea.setText(info.toString());

        JScrollPane infoScrollPane = new JScrollPane(hexInfoArea);
        infoScrollPane.setBounds(50, 60, 700, 600);
        moreBuildingPanel.add(infoScrollPane);

        moreBuildingPanel.add(LessDetail);
        moreBuildingPanel.add(adminFowSet);
        if(Login.currentUser.equals("Admin")){adminFowSet.setVisible(true);}
        moreBuildingPanel.add(LogisticsDetail);
        moreBuildingPanel.add(ProductionDetail);

        moredetailsPanel.add(closeButton);

        moredetailsPanel.add(moreBuildingPanel);
        moredetailsPanel.setVisible(true);
    }
    private void openLogisticsForHex(String hexKey) {
        audio.playClick();

        // Créer une nouvelle fenêtre pour la logistique
        JDialog logisticsDialog = new JDialog(this, "Logistique - " + hexKey, true);
        logisticsDialog.setSize(1000, 600);
        logisticsDialog.setLocationRelativeTo(this);

        // Créer le service logistique si nécessaire
        EconomicDataService economicService = MainMenu.getEconomicService();
        LogisticsService logisticsService = economicService.getLogisticsService();

        if (logisticsService == null) {
            logisticsService = new LogisticsService(repo);
        }

        // Créer le panel logistique
        LogisticsPanel logisticsPanel = new LogisticsPanel(logisticsService, repo);

        // Sélectionner automatiquement l'hexagone
        logisticsPanel.selectHex(hexKey);

        logisticsDialog.add(logisticsPanel);
        logisticsDialog.setVisible(true);
    }
    private void openProductionForHex(String hexKey) {
        audio.playClick();

        // Ouvrir une fenêtre de détail production pour cet hex
        JDialog productionDialog = new JDialog(this, "Production - " + hexKey, true);
        productionDialog.setSize(800, 500);
        productionDialog.setLocationRelativeTo(this);

        HexDetails details = repo.getHexDetails(hexKey);
        EconomicDataService economicService = MainMenu.getEconomicService();

        // Panel d'informations de production
        JPanel productionPanel = new JPanel(new BorderLayout());
        productionPanel.setBackground(new Color(40, 40, 40));

        JTextArea productionInfo = new JTextArea();
        productionInfo.setEditable(false);
        productionInfo.setBackground(new Color(30, 30, 30));
        productionInfo.setForeground(Color.WHITE);
        productionInfo.setFont(new Font("Courier New", Font.PLAIN, 12));

        // Calculer la production de cet hex
        StringBuilder prodInfo = new StringBuilder();
        prodInfo.append("=== PRODUCTION HEXAGONE ").append(hexKey).append(" ===\n\n");

        // Utiliser le service de production
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
        Map<Integer, ImageIcon> iconList;

        if (targetButton == MainLabel) {
            finalDialogTitle = "Main Building";
            selectedIndex = currentIconMainIndex;
            selectedIcons = scaledIconsMain;
            iconList = originalMainIcon;
        } else if (targetButton == AuxLabel) {
            finalDialogTitle = "Auxiliary Building";
            selectedIndex = currentIconAuxIndex;
            selectedIcons = scaledIconsAux;
            iconList = originalAuxIcon;
        } else {
            finalDialogTitle = "Fortifications";
            selectedIndex = currentIconFortIndex;
            selectedIcons = scaledIconsFort;
            iconList = originalFortIcon;
        }

        iconMenu = new JDialog(this, finalDialogTitle, true);
        iconMenu.setSize(600, 250);

        iconPanel = new JPanel();
        iconPanel.setLayout(new BoxLayout(iconPanel, BoxLayout.X_AXIS));

        for (Map.Entry<Integer, ImageIcon> entry : selectedIcons.entrySet()) {
            final int index = entry.getKey();
            ImageIcon icon = entry.getValue();

            JLabel iconLabel = new JLabel(icon);
            iconLabel.setPreferredSize(new Dimension(100, 100));
            iconLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            if (index == selectedIndex) {
                iconLabel.setBorder(createLineBorder(Color.RED, 2));
            }

            iconLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectIcon(iconLabel, index, finalDialogTitle);
                }
            });

            iconPanel.add(iconLabel);
        }

        JScrollPane scrollPane = new JScrollPane(iconPanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(600, 250));

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

        cancelButton.addActionListener(e -> cancelSelection(e));

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        iconMenu.add(buttonPanel, BorderLayout.SOUTH);
        iconMenu.setLocationRelativeTo(this);
        iconMenu.setVisible(true);


    }

    void resetIndexValue() {
        currentIconMainIndex = 0;
        currentIconAuxIndex=0;
        currentIconFortIndex=0;
}

    public void saveHexDetails() {

        if (isSaving) return;
        isSaving = true;

        HexDetails details = repo.getHexDetails(hexKey);
        System.out.println("===========================================");
        System.out.println("Saving details for " +hexKey );
        System.out.println("===========================================");
        if (details == null) {
            System.out.println("Creating new Java.HexDetails for " + hexKey);
            details = new HexDetails(hexKey);
        } else {
            System.out.println("Updating existing Java.HexDetails for " + hexKey + " : ");
        }


        boolean MainChanged = details.getMainBuildingIndex() != currentIconMainIndex;
        boolean AuxChanged = details.getAuxBuildingIndex() != currentIconAuxIndex;
        boolean FortChanged = details.getFortBuildingIndex() != currentIconFortIndex;
        String currentClaim = details.getFactionClaim();
        boolean ClaimChanged = !Objects.equals(currentClaim, currentUserFaction.getDisplayName());
        System.out.println("======================MainChanged = " + MainChanged+ " =====================");
        System.out.println("======================AuxChanged = " + AuxChanged+ " =====================");
        System.out.println("======================currentClaim = " + currentClaim+ " =====================");
        System.out.println("======================ClaimChanged = " + ClaimChanged+ " =====================");


        boolean changed = MainChanged || AuxChanged || FortChanged || ClaimChanged;

        if (changed) {
            if (MainChanged) {
                details.setMainBuildingIndex(currentIconMainIndex);
                System.out.println("Main changed: " + currentIconMainIndex);
            }
            if (AuxChanged) {
                details.setAuxBuildingIndex(currentIconAuxIndex);
                System.out.println("Aux changed: " + currentIconAuxIndex);
            }
            if (FortChanged) {
                details.setFortBuildingIndex(currentIconFortIndex);
                System.out.println("Fort changed: " + currentIconFortIndex);
            }
            if (ClaimChanged) {
                String userFactionId = MainMenu.getCurrentFactionId();
                if(!isAdmin){details.setFactionClaim(userFactionId); System.out.println("Claim changed: " + userFactionId);}
                else{details.setFactionClaim(chosen.getId()); System.out.println("Claim changed: " + chosen.getId());}
            }

            System.out.println("Saved: " + details);
            repo.updateHexDetails(hexKey, details);
        } else {
            System.out.println("No changes detected, skipping save.");
        }
        repaint();
        isSaving = false;
    }

    void MainBuidindButtonMenu(ActionEvent e) {openIconMenu(MainLabel);}

    void AuxilliaryButtonMenu(ActionEvent e){ openIconMenu(AuxLabel);}

    void FortificationButtonMenu(ActionEvent e){ openIconMenu(FortLabel);}
    void MoreDetaiButtonTrigger(ActionEvent e) {MoreDetailedView(); }

    void saveButtonTrigger(ActionEvent e){System.out.println("Save button clicked, label: " + hexKey); saveHexDetails(); }

    void confirmMainSelection(JButton targetButton) {
        ImageIcon origIcon  = originalMainIcon.get(currentIconMainIndex);
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

    void cancelSelection(ActionEvent e) {iconMenu.dispose();}

    void quittingwithbutton(ActionEvent e){
        audio.playClick();
        audio.stop(); dispose();}

    void repaintwithbutton(ActionEvent e){MAPMONDE.repaint();}

    void selectIcon(JLabel iconLabel, int index, String buildingType) {
         for (Component comp : iconPanel.getComponents()) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
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
        button.setBackground(new Color(0,0,0,0));
        button.setBorderPainted(false);
        button.setForeground(new Color(0,0,0,0));
        button.setContentAreaFilled(false);
        button.addActionListener(e -> {
            audio.playClick();
            if (button.getText() == "X") {
                closeDetailView();

            }
        });
        return button;


    }

    void setInitialView() {
        String targetHexName = "hex_19_23";
        // Get pixel position
        int[] position = repo.getHexPosition(targetHexName);
        int targetX = position[0];
        int targetY = position[1];
        // Center the view on the hex
        imgX = -targetX + (panelWidth / 2);
        imgY = -targetY + (panelHeight / 2);
        // Ensure the map stays within bounds
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
                    System.out.println("===========================================");
                    hexKey = "hex_" + labelclick.x + "_" + labelclick.y;
                    System.out.println("hex cliqué : " + hexKey);
                    HexDetails details = repo.getHexDetails(hexKey);
                    System.out.println("Claim : "+details.getFactionClaim());
                    System.out.println("===========================================");
                    System.out.println(" ");
                    System.out.println("===========================================");

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

    private void drawHexLabelCached(Graphics2D g2d,
                                    int col, int row,
                                    double cx, double cy,
                                    double unitSize) {
        // Draw coordinate label
        String label = "(" + col + "," + row + ")";
        hexLabels.put(new Point(col, row), label);
        HexDetails details = hexCache.get("hex_" + col + "_" + row);
        if (details == null) return;

        // Text metrics
        FontMetrics fm = g2d.getFontMetrics();
        int textW = fm.stringWidth(label);
        int textH = fm.getAscent();
        int tx = (int)(cx - textW / 2.0);
        int ty = (int)(cy + textH / 2.0);

        String factionId = details.getFactionClaim();
        Color color = FactionRegistry.getColorFor(factionId);
        g2d.setColor(color);
        g2d.drawString(label, tx, ty);

        // Draw emblem icon if not Free
        Faction faction = FactionRegistry.getFactionId(factionId);
        if (!"Free".equals(faction.getId())) {
            String emblemPath = faction.getEmblemImage();
            Image raw = emblemCache.computeIfAbsent(emblemPath, p -> {
                try {
                    return ImageIO.read(getClass().getResource(p));
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            });
            if (raw != null) {
                // Scale and position emblem next to label
                double hexW = 2 * unitSize;
                int eW = (int)(hexW * 0.2);
                double aspect = (double) raw.getHeight(null) / raw.getWidth(null);
                int eH = (int)(eW * aspect);
                int ex = (int)(cx + textW / 2.0 + 5);
                int ey = (int)(cy - eH / 2.0);
                g2d.drawImage(raw, ex, ey, eW, eH, null);
            }
        }
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
        return new Rectangle((int)(px - w/2.0), (int)(py - h/2.0), w, h);
    }



    private void initializeCache() {
        // Load all hex data once
        Map<String, HexDetails> allHexes = repo.loadAll();
        hexCache.clear();

        // Pre-calculate faction colors
        for (HexDetails details : allHexes.values()) {
            String faction = details.getFactionClaim();
            if (!factionColorCache.containsKey(faction)) {
                factionColorCache.put(faction, UIHelpers.getFactionColor(faction));
            }
        }
        hexCache.putAll(allHexes);
    }

    public void invalidateHexCache() {
        cacheInitialized = false;
        hexCache.clear();
        factionColorCache.clear();
    }

    private final Map<String, Image> emblemCache = new HashMap<>();

    private int getHexMainCapFromDATABASE() {
        int max = 0;
        for (DATABASE.MainBuilding building : DATABASE.MainBuilding.values()) {
            String tag = building.toString(); // or building.getTag() if you add a getter
            if (tag.startsWith("Main")) {
                try {
                    int number = Integer.parseInt(tag.replaceAll("[^0-9]", ""));
                    if (number > max) {
                        max = number;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return max;
    }

    public void ClaimButtonTrigger(ActionEvent e) {
        HexDetails details = repo.getHexDetails(hexKey);
        String userFactionId = MainMenu.getCurrentFactionId();
        String newClaim;

        if(isAdmin){newClaim = chosen.getId();
            System.out.println("admin claimed : "+newClaim);}
        else{newClaim=userFactionId;}
        System.out.println("===========================================");
        System.out.println("newClaim :"+ newClaim +"|| Previous Claim : "+details.getFactionClaim());
        System.out.println("===========================================");
        if (!Objects.equals(details.getFactionClaim(), newClaim)) {
            details.setFactionClaim(newClaim);
            System.out.println("Faction claim updated to: " + newClaim);
            repo.updateHexDetails(hexKey, details);
        } else {
            System.out.println("Faction claim unchanged.");
        }
    }
    public void ClaimButtonKeyTrigger(KeyEvent e) {
        HexDetails details = repo.getHexDetails(hexKey);
        String userFactionId = MainMenu.getCurrentFactionId();
        String newClaim;

        if(isAdmin){newClaim = chosen.getId();
            System.out.println("admin claimed : "+newClaim);}
        else{newClaim=userFactionId;}
        System.out.println("===========================================");
        System.out.println("newClaim :"+ newClaim +"|| Previous Claim : "+details.getFactionClaim());
        System.out.println("===========================================");
        if (!Objects.equals(details.getFactionClaim(), newClaim)) {
            details.setFactionClaim(newClaim);
            System.out.println("Faction claim updated to: " + newClaim);
            repo.updateHexDetails(hexKey, details);
        } else {
            System.out.println("Faction claim unchanged.");
        }
    }
    private void closeDetailView() {
        if (detailViewOpen) {
            detailsPanel.setVisible(false);
            detailsPanel.removeAll();
            moredetailsPanel.setVisible(false);
            moredetailsPanel.removeAll();

            // Close any open icon menus
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
            System.out.println("Detail view closed");
        }
    }

    private void preloadAllImages() {
        SwingWorker<Void, String> imageLoader = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                System.out.println("Loading building icons...");

                // Load all icon types in parallel using ExecutorService
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

    private void loadMainIcons() {
        for (int i = 0; i <= hexMainCap; i++) {
            URL resourceUrl = getClass().getResource("/hexmain/main" + i + ".png");
            if (resourceUrl != null) {
                ImageIcon originalIcon = new ImageIcon(resourceUrl);
                originalMainIcon.put(i,originalIcon);

                Image scaled100 = originalIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                scaledIconsMain.put(i, new ImageIcon(scaled100));
            }
        }
    }
    private void loadAuxIcons() {
        for (int i = 0; i <= hexAuxCap; i++) {
            URL resourceUrl = getClass().getResource("/hexaux/Aux" + i + ".png");
            if (resourceUrl != null) {
                ImageIcon originalIcon = new ImageIcon(resourceUrl);
                originalAuxIcon.put(i,originalIcon);

                // Pre-scale commonly used sizes
                Image scaled100 = originalIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                scaledIconsAux.put(i, new ImageIcon(scaled100));
            }
        }
    }
    private void loadFortIcons() {
        for (int i = 0; i <= hexFortCap; i++) {
            URL resourceUrl = getClass().getResource("/hexfort/fort" + i + ".png");
            if (resourceUrl != null) {
                ImageIcon originalIcon = new ImageIcon(resourceUrl);
                originalFortIcon.put(i,originalIcon);

                // Pre-scale commonly used sizes
                Image scaled100 = originalIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                scaledIconsFort.put(i, new ImageIcon(scaled100));
            }
        }
    }
    private void openFowManagementDialog(String hexKey) {
        // Récupérer toutes les factions
        Collection<Faction> all = FactionRegistry.all();
        String[] factionIds = all.stream().map(Faction::getId).toArray(String[]::new);

        // Récupérer état actuel
        HexDetails hex = repo.getHexDetails(hexKey);
        Set<String> discovered = new HashSet<>(hex.getDiscoveredByFaction()); // nouvelle méthode a implémenter

        // Liste de checkboxes
        JPanel panel = new JPanel(new GridLayout(0, 2));
        Map<String,JCheckBox> map = new HashMap<>();
        for (String fid : factionIds) {
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
            // Appliquer les choix
            discovered.clear();
            for (Map.Entry<String,JCheckBox> entry : map.entrySet()) {
                if (entry.getValue().isSelected()) {
                    discovered.add(entry.getKey());
                }
            }
            // Mettre à jour HexDetails
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
}
