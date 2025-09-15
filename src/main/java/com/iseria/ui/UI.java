package com.iseria.ui;

import com.iseria.domain.*;
import com.iseria.domain.DATABASE;
import com.iseria.infra.MoralCalculator;
import com.iseria.service.*;


import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.iseria.ui.MainMenu.*;
import static com.iseria.ui.UIHelpers.*;


public class UI {

    private static EconomicDataService economicService;

    private static MoralSaveService moralSaveService = new MoralSaveService();

    public static JPanel createGeneralInfoPanel(JTextArea myNoteArea) {
        JPanel gIP = new JPanel(new GridBagLayout());
        gIP.setOpaque(false);
        gIP.setBackground(Color.orange);


        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.PAGE_START;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 0, 5, 5);

        JLabel prochainTour = new JLabel("Prochain Tour :");
        prochainTour.setFont(new Font("Bahnschrift", Font.BOLD, 15));
        prochainTour.setForeground(Color.GREEN);
        gIP.add(prochainTour, gbc);

        gbc.gridx = 1;
        JLabel prochainTourDATA = new JLabel("SoonTM");
        prochainTourDATA.setFont(new Font("Bahnschrift", Font.BOLD, 15));
        prochainTourDATA.setForeground(Color.GREEN);
        gIP.add(prochainTourDATA, gbc);

        gbc.insets = new Insets(5, 0, 5, 5);
        gbc.gridx = 1;
        gbc.gridy = 2;
        JLabel mesNotes = new JLabel("Mes Notes : ");
        mesNotes.setFont(new Font("Bahnschrift", Font.BOLD, 15));
        mesNotes.setHorizontalAlignment(SwingConstants.CENTER);
        mesNotes.setForeground(Color.BLACK);
        gIP.add(mesNotes, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridheight = gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(5, 5, 5, 0);

        myNoteArea.setLineWrap(true);
        myNoteArea.setWrapStyleWord(true);
        myNoteArea.setOpaque(true);
        myNoteArea.setBackground(new Color(211, 211, 211, 128));

        JScrollPane scroll = new JScrollPane(myNoteArea);
        scroll.setBorder(BorderFactory.createLineBorder(Color.BLACK, 5));
        scroll.setOpaque(false);


        gIP.add(scroll, gbc);

        return gIP;
    }

    // Dans UI.java, remplacer createMoralResultLabel par cette version améliorée
    private static JTextPane createMoralResultTextPane(String text) {
        JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setEditable(false);
        textPane.setOpaque(true);
        textPane.setBackground(new Color(240, 240, 240));
        textPane.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        textPane.setFont(new Font("Arial", Font.PLAIN, 12));
        textPane.setText(formatTextForDisplay(text));
        return textPane;
    }

    private static String formatTextForDisplay(String text) {
        if (text == null || text.isEmpty()) {
            return "<html><body>← effect will show here</body></html>";
        }

        // Remplacer les \n par des <br> pour l'HTML
        String formatted = text.replace("\n", "<br>");
        return "<html><body style='padding: 5px;'>" + formatted + "</body></html>";
    }

    // Version mise à jour de createMoralPanel
    public static MoralPanelResult createMoralPanel(MoralDataService moralService, Faction currentUserFaction, String currentUser) {
        Map<String, JComboBox<DATABASE.MoralAction>> dropdownMap = new HashMap<>();
        JPanel moralPanel = new JPanel(new GridBagLayout());
        moralPanel.setPreferredSize(new Dimension(1000, 400)); // Augmenté la hauteur
        moralPanel.setOpaque(false);
        moralPanel.setBackground(Color.lightGray);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.PAGE_START;

        // Header
        JLabel title = new JLabel("Moral");
        title.setFont(new Font("Bahnschrift", Font.BOLD, 25));
        gbc.gridx = 2; gbc.gridy = 0;
        moralPanel.add(title, gbc);

        // Column headers
        gbc.gridheight = 1;
        gbc.ipady = 10;
        gbc.anchor = GridBagConstraints.BASELINE;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel type = makeCell("Type :", Color.ORANGE);
        gbc.gridx = 1; gbc.gridy = 1;
        type.setPreferredSize(new Dimension(200,25));
        moralPanel.add(type, gbc);

        JLabel effect = makeCell("Effets :", Color.ORANGE);
        gbc.gridx = 2;
        effect.setPreferredSize(new Dimension(500,25));
        moralPanel.add(effect, gbc);

        JLabel tirage = makeCell("Tiré le :", Color.ORANGE);
        gbc.gridx = 3;
        tirage.setPreferredSize(new Dimension(100,25));
        moralPanel.add(tirage, gbc);

        JLabel fin = makeCell("Fin le :", Color.ORANGE);
        gbc.gridx = 4;
        fin.setPreferredSize(new Dimension(100,25));
        fin.setFont(new Font("Bahnschrift", Font.BOLD, 15));
        fin.setBackground(Color.orange);
        fin.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        moralPanel.add(fin, gbc);

        // Dropdowns + Result TextPanes (au lieu de Labels)
        JComboBox<DATABASE.MoralAction>[] dropdowns = new JComboBox[5];
        JTextPane[] resultTextPanes = new JTextPane[5]; // Changé de JLabel à JTextPane

        List<DATABASE.MoralAction> availableActions = moralService.getAvailableActions(currentUserFaction);

        gbc.anchor = GridBagConstraints.PAGE_START;
        gbc.fill = GridBagConstraints.BOTH; // Changé pour BOTH

        for (int i = 0; i < 5; i++) {
            String key = "dropdown_" + (i + 1);

            // Créer dropdown avec les actions disponibles pour la faction
            dropdowns[i] = new JComboBox<>();
            dropdowns[i].addItem(null); // Option vide
            availableActions.forEach(dropdowns[i]::addItem);

            dropdowns[i].setPreferredSize(new Dimension(200, 30));
            dropdowns[i].setFocusable(false);
            dropdowns[i].setRenderer(createMoralActionRenderer());
            dropdownMap.put(key, dropdowns[i]);

            gbc.gridx = 1;
            gbc.gridy = i + 2;
            gbc.weightx = 0.3;
            gbc.weighty = 0.2; // Ajouté du poids vertical
            moralPanel.add(dropdowns[i], gbc);

            // Utiliser JTextPane au lieu de JLabel
            resultTextPanes[i] = createMoralResultTextPane("← effect will show here");
            resultTextPanes[i].setPreferredSize(new Dimension(500, 60)); // Hauteur fixe

            int finalI = i;
            dropdowns[i].addActionListener(e -> {
                DATABASE.MoralAction selectedAction = (DATABASE.MoralAction) dropdowns[finalI].getSelectedItem();
                if (selectedAction != null) {
                    resultTextPanes[finalI].setText(formatTextForDisplay(selectedAction.getDescription()));
                    resultTextPanes[finalI].setToolTipText(selectedAction.getLoreDescription());
                } else {
                    resultTextPanes[finalI].setText(formatTextForDisplay("← effect will show here"));
                    resultTextPanes[finalI].setToolTipText(null);
                }
            });

            gbc.gridx = 2;
            gbc.gridy = i + 2;
            gbc.weightx = 0.7;
            moralPanel.add(resultTextPanes[finalI], gbc);
        }

        // Total label
        JLabel totalTitle = createMoralResultLabel("Moral Total : ");
        totalTitle.setPreferredSize(new Dimension(200, 28));
        totalTitle.setFont(new Font("Oswald", Font.BOLD, 20));
        totalTitle.setHorizontalAlignment(SwingConstants.CENTER);
        totalTitle.setBackground(Color.ORANGE);
        gbc.gridx = 1; gbc.gridy = 4; // Ajusté la position
        gbc.weighty = 0;
        moralPanel.add(totalTitle, gbc);

        JLabel totalValue = createMoralResultLabel(" ");
        totalValue.setHorizontalAlignment(SwingConstants.CENTER);
        totalValue.setFont(new Font("Oswald", Font.BOLD, 20));
        gbc.gridx = 2;

        attachLiveMoralUpdater(totalValue, dropdowns);
        moralPanel.add(totalValue, gbc);

        MoralPanelResult result = new MoralPanelResult(moralPanel, dropdownMap);

        // Charger les sélections sauvegardées
        MoralSaveService.MoralSaveData loadedData = moralSaveService.loadMoralSelections(currentUser, currentUserFaction);

        moralSaveService.applyLoadedSelections(dropdownMap, loadedData);

        // Auto-sauvegarde à chaque changement
        for (JComboBox<DATABASE.MoralAction> dropdown : dropdowns) {
            dropdown.addActionListener(e -> {
                // Calculer les totaux actuels
                List<DATABASE.MoralAction> selectedActions = new ArrayList<>();
                for (JComboBox<DATABASE.MoralAction> cb : dropdowns) {
                    DATABASE.MoralAction selected = (DATABASE.MoralAction) cb.getSelectedItem();
                    if (selected != null) {
                        selectedActions.add(selected);
                    }
                }

                double moral = MoralCalculator.sumMoral(selectedActions);
                int instability = MoralCalculator.sumInstability(selectedActions);

                // Sauvegarder
                moralSaveService.saveMoralSelections(currentUser, currentUserFaction, dropdownMap, moral, instability);
            });
        }    return result;

    }

    public static JPanel createRumorPanel(RumorDataService rumorService, String excelResourcePath, String currentUser) {
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);
        container.setBackground(new Color(0, 0, 0, 0));
        container.setPreferredSize(new Dimension(1000, 400));

        JPanel rumorPanel = new JPanel(new GridBagLayout());
        rumorPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();

        // Title
        JLabel title = new JLabel("Rumeurs");
        title.setFont(new Font("Bahnschrift", Font.BOLD, 25));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 4;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setPreferredSize(new Dimension(900, 35));
        rumorPanel.add(title, gbc);

        // Column headers
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.PAGE_START;
        gbc.gridy = 1; gbc.gridwidth = 1;

        JLabel typeLabel = makeCell("Type", Color.ORANGE);
        typeLabel.setPreferredSize(new Dimension(50, 40));
        typeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0; rumorPanel.add(typeLabel, gbc);

        JLabel nameLabel = makeCell("Nom", Color.ORANGE);
        nameLabel.setPreferredSize(new Dimension(150, 40));
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 1; rumorPanel.add(nameLabel, gbc);

        JLabel contentLabel = makeCell("On dit que ...", Color.ORANGE);
        contentLabel.setPreferredSize(new Dimension(625, 40));
        contentLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 2; rumorPanel.add(contentLabel, gbc);

        JLabel dateLabel = makeCell("Date", Color.ORANGE);
        dateLabel.setHorizontalAlignment(SwingConstants.CENTER);
        dateLabel.setPreferredSize(new Dimension(50, 40));
        gbc.gridx = 3; rumorPanel.add(dateLabel, gbc);

        // Load rumors using service
        List<RumorDataService.RumorEntry> rumors = rumorService.loadRumors(excelResourcePath);
        int rowCount = 2;

        for (RumorDataService.RumorEntry rumor : rumors) {
            gbc.gridy = rowCount++;

            JLabel typeCell = createRumorCell(rumor.type, 50);
            gbc.gridx = 0; rumorPanel.add(typeCell, gbc);

            JLabel nameCell = createRumorCell(rumor.name, 150);
            gbc.gridx = 1; rumorPanel.add(nameCell, gbc);

            JLabel contentCell = createRumorCell(rumor.content, 625);
            gbc.gridx = 2; rumorPanel.add(contentCell, gbc);

            JLabel dateCell = createRumorCell(rumor.date, 50);
            gbc.gridx = 3; rumorPanel.add(dateCell, gbc);
        }

        JScrollPane scrollPane = new JScrollPane(rumorPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0,0,0,0)));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBackground(new Color(0, 0, 0, 0));
        scrollPane.getViewport().setBackground(new Color(0, 0, 0, 0));
        container.add(scrollPane, BorderLayout.CENTER);

        return container;
    }

    public static JLabel createRumorCell(String text, int width) {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setBackground(Color.white);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        label.setPreferredSize(new Dimension(width, 40));
        label.setFont(new Font("Oswald", Font.PLAIN, 12));
        return label;
    }

    public static class MoralPanelResult {
        public JPanel panel;
        public Map<String, JComboBox<DATABASE.MoralAction>> dropdownMap;

        public MoralPanelResult(JPanel panel, Map<String, JComboBox<DATABASE.MoralAction>> dropdownMap) {
            this.panel = panel;
            this.dropdownMap = dropdownMap;
        }
    }

    public static class WorkerSelectionDialog extends JDialog {
        private JSpinner workerSpinner;
        private int selectedCount = 0;
        private boolean confirmed = false;
        private final String buildingType;
        private final String buildingName;

        public WorkerSelectionDialog(JFrame parent, String buildingType, String buildingName, int currentWorkers) {
            super(parent, "Affecter du personnel", true);
            this.buildingType = buildingType;
            this.buildingName = buildingName;
            initializeDialog(currentWorkers);
        }

        private void initializeDialog(int currentWorkers) {
            setSize(400, 200);
            setLocationRelativeTo(getParent());
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            JPanel mainPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            // Title
            JLabel titleLabel = new JLabel("Personnel pour " + buildingName);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            gbc.insets = new Insets(10, 10, 20, 10);
            mainPanel.add(titleLabel, gbc);

            // Current workers display
            JLabel currentLabel = new JLabel("Personnel actuel: " + currentWorkers);
            gbc.gridy = 1; gbc.gridwidth = 1;
            gbc.insets = new Insets(5, 10, 5, 10);
            mainPanel.add(currentLabel, gbc);

            // Worker spinner
            JLabel spinnerLabel = new JLabel("Nouveau personnel:");
            gbc.gridx = 0; gbc.gridy = 2;
            mainPanel.add(spinnerLabel, gbc);

            workerSpinner = new JSpinner(new SpinnerNumberModel(currentWorkers, 0, 50, 1));
            workerSpinner.setPreferredSize(new Dimension(100, 30));
            gbc.gridx = 1;
            mainPanel.add(workerSpinner, gbc);

            // Buttons
            JPanel buttonPanel = new JPanel(new FlowLayout());
            JButton confirmButton = new JButton("Confirmer");
            JButton cancelButton = new JButton("Annuler");

            confirmButton.addActionListener(e -> {
                selectedCount = (Integer) workerSpinner.getValue();
                confirmed = true;
                dispose();
            });

            cancelButton.addActionListener(e -> {
                confirmed = false;
                dispose();
            });

            buttonPanel.add(confirmButton);
            buttonPanel.add(cancelButton);

            gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
            gbc.insets = new Insets(20, 10, 10, 10);
            mainPanel.add(buttonPanel, gbc);

            add(mainPanel);
        }

        public int getSelectedCount() { return selectedCount; }
        public boolean isConfirmed() { return confirmed; }
    }

    public static class HexSnapshotCache {
        private static final Map<String, BufferedImage> hexImageCache = new ConcurrentHashMap<>();
        public static BufferedImage mapBackground; // Image de fond de la carte

        static {
            // Initialiser l'image de fond au démarrage
            try {
                mapBackground = ImageIO.read(UI.class.getResource("/Iseria.png"));
            } catch (IOException e) {
                System.err.println("Erreur chargement carte: " + e.getMessage());
            }
        }

        /**
         * Retourne l'image hexagonale pour la clé donnée avec cache
         */
        public static BufferedImage getHexSnapshot(String hexKey, IHexRepository repo) {
            return hexImageCache.computeIfAbsent(hexKey, key -> generateHexSnapshot(key, repo));
        }

        private static BufferedImage generateHexSnapshot(String hexKey, IHexRepository repo) {
            if (mapBackground == null) return null;

            // 1. Récupérer les coordonnées pixel de l'hexagone
            Rectangle bounds = getHexPixelBounds(hexKey, repo);

            // 2. Extraire la zone rectangulaire
            BufferedImage square = extractSquareRegion(bounds);

            // 3. Appliquer le masque hexagonal
            return createHexagonalSnapshot(square);
        }

        private static Rectangle getHexPixelBounds(String hexKey, IHexRepository repo) {
            int[] pos = repo.getHexPosition(hexKey);
            int size = 100; // Taille souhaitée pour le snapshot

            // Centrer le rectangle sur la position de l'hex
            return new Rectangle(
                    pos[0] - size/2,
                    pos[1] - size/2,
                    size,
                    size
            );
        }

        private static BufferedImage extractSquareRegion(Rectangle bounds) {
            int x = Math.max(0, Math.min(bounds.x, mapBackground.getWidth() - bounds.width));
            int y = Math.max(0, Math.min(bounds.y, mapBackground.getHeight() - bounds.height));
            int w = Math.min(bounds.width, mapBackground.getWidth() - x);
            int h = Math.min(bounds.height, mapBackground.getHeight() - y);

            return mapBackground.getSubimage(x, y, w, h);
        }

        private static BufferedImage createHexagonalSnapshot(BufferedImage square) {
            int size = square.getWidth();
            BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = result.createGraphics();
                Path2D hexPath = createHexagonPath(size);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setClip(hexPath);
            g2d.drawImage(square, 0, 0, null);

            g2d.dispose();
            return result;
        }

        private static Path2D createHexagonPath(int size) {
            Path2D hexagon = new Path2D.Double();
            double radius = size / 2.0;
            double centerX = size / 2.0;
            double centerY = size / 2.0;

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

        public static void clearCache() {
            hexImageCache.clear();
        }
    }

    // Enhanced Economic Panel with instability from saved state
    public static class EnhancedEconomicPanel extends JPanel
            implements EconomicDataService.EconomicDataObserver {

        private EconomicDataService economicService;
        private JLabel tresorerieLabel, populationLabel, instabiliteLabel, agressiviteLabel, faimLabel;
        private JPanel resourcePanel, salaryPanel;
        private IHexRepository hexRepository;
        private LogisticsPanel logisticsPanel;


        public EnhancedEconomicPanel(EconomicDataService economicService, IHexRepository hexRepository) {
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

            // Gestion Sociale section with "faim" parameter
            gbc.gridx = 0; gbc.gridy = 0;
            gbc.fill = GridBagConstraints.BOTH;
            add(createGestionSocialePanel(), gbc);

            // Trésorerie
            gbc.gridy++;
            add(createTresoreriePanel(), gbc);

            // Salaries
            gbc.gridy++;
            salaryPanel = new JPanel(new GridBagLayout());
            TitledBorder salaryPanelBorder = BorderFactory.createTitledBorder("Salaires");
            salaryPanelBorder.setTitleColor(Color.WHITE);
            salaryPanel.setBorder(salaryPanelBorder);
            salaryPanel.setOpaque(true);
            salaryPanel.setBackground(new Color(50, 50, 50, 200));
            add(salaryPanel, gbc);

            // Resources
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

            // Population
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

            // Instabilité
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

            // Agressivité
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

            // Faim - NEW PARAMETER
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

            // Calculate and display "faim" based on food production vs consumption
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

                // Info tooltip avec sources
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

            return Color.BLACK; // Fallback
        }

        private boolean isRecentlyAdded(String resourceName) {
            return false;
        }
        private boolean hasProductionIncreased(String resourceName, double currentProduction) {
            return false;
        }
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

    // Enhanced Production Panel with merged functionality
    public static class EnhancedProductionPanel extends JScrollPane
            implements EconomicDataService.EconomicDataObserver {
        private JPanel contentPanel;
        private Map<String, SafeHexDetails> hexGrid;
        private String factionName;
        private IHexRepository repository;
        private EconomicDataService economicService;
        private WorkDetailsPopup workDetailsPopup;
        private Map<String, JPanel> hexPanels = new HashMap<>();
        private boolean showHexPreview = true;
        public EnhancedProductionPanel(Map<String, SafeHexDetails> hexGrid, String factionName,
                                       IHexRepository repo, EconomicDataService economicService) {
            super();
            this.hexGrid = hexGrid;
            this.factionName = factionName;
            this.repository = repo;
            this.economicService = economicService;

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
                ((JLayeredPane) getParent()).add(workDetailsPopup, JLayeredPane.POPUP_LAYER);
            }
        }
        public void configureScrollSpeed() {

            int scrollSpeedMultiplier =10;
            addMouseWheelListener(e -> {
                JScrollBar scrollBar = getVerticalScrollBar();
                int currentValue = scrollBar.getValue();
                int scrollAmount = e.getScrollAmount() * scrollSpeedMultiplier;

                if (e.getWheelRotation() < 0) {
                    // Scroll up
                    scrollBar.setValue(currentValue - scrollAmount);
                } else {
                    // Scroll down
                    scrollBar.setValue(currentValue + scrollAmount);
                }
            });
        }
        public void refreshContent() {
            contentPanel.removeAll();
            hexPanels.clear();

            // Add hex preview toggle
            JPanel controlPanel = createControlPanel();
            contentPanel.add(controlPanel);

            for (Map.Entry<String, SafeHexDetails> entry : hexGrid.entrySet()) {
                SafeHexDetails hex = entry.getValue();

                if (factionName.equals(hex.getFactionClaim())) {

                    JPanel hexPanel = createEnhancedHexPanel(entry.getKey(), hex, this.repository);
                    hexPanels.put(entry.getKey(), hexPanel);
                    contentPanel.add(hexPanel);
                    contentPanel.add(Box.createVerticalStrut(5));
                }
            }

            contentPanel.revalidate();
            contentPanel.repaint();
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
        private JPanel createEnhancedHexPanel(String hexKey, SafeHexDetails hex, IHexRepository repo) {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createTitledBorder(hexKey));
            panel.setOpaque(false);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;

            // Hex preview (if enabled)
            if (showHexPreview) {
                gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3;
                JPanel hexPreview = createHexPreview(hexKey, hex, repo);
                panel.add(hexPreview, gbc);
                gbc.gridy++;
                gbc.gridwidth = 1;
            }

            // Building sections
            String[] buildingTypes = {"main", "aux", "fort"};
            String[] buildingLabels = {"Principal", "Auxiliaire", "Fortification"};

            for (int i = 0; i < buildingTypes.length; i++) {
                gbc.gridx = i;
                if (!showHexPreview) gbc.gridy = 0;
                JPanel buildingPanel = createBuildingProductionPanel(hexKey, hex, buildingTypes[i], buildingLabels[i]);
                panel.add(buildingPanel, gbc);
            }

            return panel;
        }
        private JPanel createHexPreview(String hexKey, SafeHexDetails hex, IHexRepository repo) {

            JLabel hexImageLabel = hexImageLabels.computeIfAbsent(hexKey, key -> {
                BufferedImage hexSnapshot = UI.HexSnapshotCache.getHexSnapshot(key, repo);
                JLabel hexLabel = new JLabel();
                if (hexSnapshot != null) {
                    hexLabel.setIcon(new ImageIcon(hexSnapshot));
                }
                hexLabel.setPreferredSize(new Dimension(100, 100));
                return hexLabel;
            });

            JPanel preview = new JPanel(new FlowLayout());
            preview.setBorder(BorderFactory.createTitledBorder("Aperçu Hexagone"));
            preview.setOpaque(false);

            String buildingNames = UIHelpers.getBuildingNamesSafe(
                    hex.getMainBuildingIndex(),
                    hex.getAuxBuildingIndex(),
                    hex.getFortBuildingIndex()
            );

            JLabel info = new JLabel(String.format("Faction: %s | %s",
                    hex.getFactionClaim(), buildingNames));;
            preview.add(info);
            preview.add(hexImageLabel);

            return preview;
        }
        private JPanel createBuildingProductionPanel(String hexKey, SafeHexDetails hex,
                                                     String buildingType, String label) {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createTitledBorder(label));
            panel.setBackground(new Color(211, 211, 211, 128));
            panel.setOpaque(true);


            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.anchor = GridBagConstraints.WEST;

            // Workers count with pull ability
            int workerCount = hex.getWorkerCountByType(buildingType);
            gbc.gridx = 0; gbc.gridy = 0;

            JPanel workerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
            workerPanel.setOpaque(false);

            JLabel workerLabel = new JLabel("👥 " + workerCount);
            workerPanel.add(workerLabel);

            // Pull workers button
            JButton pullButton = new JButton("↑");
            pullButton.setToolTipText("Retirer des workers");
            pullButton.addActionListener(e -> pullWorkers(hexKey, hex, buildingType));
            workerPanel.add(pullButton);

            // Add workers button
            JButton addButton = new JButton("↓");
            addButton.setToolTipText("Ajouter des workers");
            addButton.addActionListener(e -> addWorkers(hexKey, hex, buildingType));
            workerPanel.add(addButton);

            panel.add(workerPanel, gbc);

            // Lock mechanism
            gbc.gridy = 1;
            JCheckBox lockCheckbox = new JCheckBox("🔒", hex.isSlotLocked(buildingType));
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

            // Resource production info using DATABASE resources
            gbc.gridy = 2;
            String resourceInfo = getResourceProductionInfo(hexKey, buildingType);
            JLabel resourceLabel = new JLabel(resourceInfo);

            panel.add(resourceLabel, gbc);

            // Configuration button
            gbc.gridy = 3;
            JButton configButton = new JButton("Config");
            configButton.addActionListener(e -> openProductionDialog(hexKey, hex, buildingType, label));
            panel.add(configButton, gbc);

            gbc.gridy = 3; gbc.gridx++;
            JButton livestockButton = new JButton("Élevage");
            livestockButton.setPreferredSize(new Dimension(120, 30));
            livestockButton.setBackground(new Color(139, 69, 19)); // Couleur terre
            livestockButton.setForeground(Color.WHITE);
            livestockButton.setVisible(false);
            livestockButton.addActionListener(e -> openLivestockDialog(hexKey, hex));
            if(UIHelpers.isFarmBuilding(hex, buildingType)) {livestockButton.setVisible(true);}
            panel.add(livestockButton, gbc);

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
            // Use DATABASE resources instead of hardcoded values
            DATABASE.ResourceType[] availableResources = getAvailableResourcesForBuilding(buildingType);

            if (availableResources.length > 0) {
                DATABASE.ResourceType resource = availableResources[0]; // Default to first available
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
        private String getDetailedResourceInfo(String hexKey, SafeHexDetails hex, String buildingType) {
            StringBuilder details = new StringBuilder();
            details.append("<html><body>");
            details.append("<h3>").append(hexKey).append(" - ").append(buildingType.toUpperCase()).append("</h3>");

            DATABASE.ResourceType[] resources = getAvailableResourcesForBuilding(buildingType);
            details.append("<p><b>Ressources disponibles:</b></p><ul>");

            for (DATABASE.ResourceType resource : resources) {
                details.append("<li>").append(resource.getName()).append(" (").append(resource.getCategory()).append(") - Base: ").append(resource.getBaseValue()).append("</li>");
            }

            details.append("</ul>");
            details.append("<p><b>Workers:</b> ").append(hex.getWorkerCountByType(buildingType)).append("</p>");
            details.append("<p><b>Verrouillé:</b> ").append(hex.isSlotLocked(buildingType) ? "Oui" : "Non").append("</p>");
            details.append("</body></html>");

            return details.toString();
        }
        private void openProductionDialog(String hexKey, SafeHexDetails hex, String buildingType, String buildingLabel) {
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

            // Créer et afficher le dialog avec l'hex
            ProductionDialog dialog = new ProductionDialog(
                    (JFrame) SwingUtilities.getWindowAncestor(this),
                    hexKey, buildingType, building.getBuildName(),
                    hex.getWorkerCountByType(buildingType),
                    economicService,
                    hex  // ← NOUVEAU paramètre
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
                // Actualiser l'affichage et les données économiques
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


}