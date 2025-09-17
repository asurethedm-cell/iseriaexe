package com.iseria.ui;

import com.iseria.domain.*;
import com.iseria.domain.DATABASE;
import com.iseria.service.*;
import com.iseria.infra.FactionRegistry;
import com.iseria.service.MarketDataService;

import javax.swing.*;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.net.URL;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.iseria.ui.LoadingWindow.splash;


public class MainMenu extends JFrame implements ActionListener {


    JLabel nomMenu = new JLabel();
    static JSlider volumeSlider = new JSlider(JSlider.VERTICAL, -50, 4, -20);
    JLabel volumeSliderName = new JLabel("VOLUME");
    JLayeredPane layeredPane = new JLayeredPane();
    public static JCheckBox Mute = new JCheckBox();
    JButton Menu1 = new JButton("Java.Mondes");
    JButton Menu2 = new JButton("Faction");
    JButton Menu3 = new JButton("Combats");
    JButton Menu4 = new JButton("Règles");
    JButton Menu5 = new JButton("Quitter");
    JButton factionMenu1 = new JButton();
    JButton factionMenu1_1 = new JButton();
    JButton factionMenu1_1A = new JButton();
    JButton factionMenu2 = new JButton();
    JButton factionMenu3 = new JButton();
    JButton factionMenu4 = new JButton();
    JButton miscButton = new JButton("Miscellaneous");
    JButton economyButton = new JButton("Economy");
    JButton productionButton = new JButton("Production");
    JButton updatePanel = new JButton("Update");
    JButton backToGeneralButton = new JButton("Back");
    JButton backButton = new JButton("Retour");
    JButton logisticsButton = new JButton("Logistics");
    ImageIcon MuteIconON;
    ImageIcon MuteIconOFF;
    ImageIcon ButtonCadreSign;
    private BufferedImage overlayImg;
    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private final File notesFile;
    public static boolean isFactionMenuOpen = false;
    public        boolean prodIsShowed;
    public        boolean ecoIsShowed;
    public        boolean logIsShowed;

    static CardLayout generalCardLayout = new CardLayout();
    static JPanel generalPanel = new JPanel(generalCardLayout);
    static JLayeredPane factionLayeredPane = new JLayeredPane();
    static JPanel factionContentPanel = new JPanel(new CardLayout());

    public static Faction currentUserFaction;
    private final IAudioService audio;
    private UI.ProductionPanel enhancedProductionPanelInstance;
    private PersonnelDataService personnelService;
    public static EconomicDataService economicService;

    MainMenu(IAudioService audio, IHexRepository repo) {
        this.audio = audio;
        currentUserFaction = FactionRegistry.getFactionForUser(Login.currentUser);
        System.out.println("User is " + Login.currentUser);
        System.out.println("Faction is " + currentUserFaction.getDisplayName());
        String factionBackgroundImage = currentUserFaction.getBackgroundImage();
        String factionLogPath = currentUserFaction.getEmblemImage();

        initializePersonnelService();
        MoralDataService moralService = new EnumMoralDataService();
        economicService = UIHelpers.initializeEconomicService(repo, currentUserFaction.getDisplayName());
//================================================INITIAL=============================================================\\
        ToolTipManager.sharedInstance().setInitialDelay(100);
        AtomicBoolean factionTheme = new AtomicBoolean(false);
        this.setTitle("Iseria");
        this.setSize(1600, 900);

        // Centered on launch
        int screenWidth = this.getToolkit().getScreenSize().width;
        int screenHeight = this.getToolkit().getScreenSize().height;
        int windowWidth = this.getWidth();
        int windowHeight = this.getHeight();
        int x = (screenWidth - windowWidth) / 2;
        int y = (screenHeight - windowHeight) / 2;
        this.setLocation(x, y);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);

        ImageIcon Icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/Icon.png")));
        this.setIconImage(Icon.getImage());
        if (Icon.getImageLoadStatus() == MediaTracker.ERRORED) {
            System.out.println("Erreur lors du chargement de l'icône.");
        }

        try {
            overlayImg = ImageIO.read(Objects.requireNonNull(MainMenu.class.getClassLoader().getResource("RessourceGen/ButtonMenuG2.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBounds(0, 0, getWidth(), getHeight());
        cardPanel.setOpaque(false);
        ButtonCadreSign = resizeIcon(new ImageIcon(Objects.requireNonNull(getClass().getResource("/RessourceGen/ButtonSign.png"))),
                250, 150);


//================================================FACTION SCREEN======================================================\\

                // Faction Panel Container with CardLayout in JLayer for switching

                factionLayeredPane.setOpaque(false);
                factionLayeredPane.setBackground(Color.BLUE);
                factionLayeredPane.setBounds(25, 5, 1100, 800);
                backToGeneralButton.setFont(new Font("Oswald", Font.BOLD, 15));
                backToGeneralButton.setBounds(975, 6, 75, 25);
                backToGeneralButton.setVisible(false);
                updatePanel.setFont(new Font("Oswald", Font.BOLD, 15));
                updatePanel.setBounds(825, 6, 125, 25);
                updatePanel.setVisible(false);
                factionContentPanel.setBounds(50, 5, 1000, 800);
                factionContentPanel.setOpaque(false);



                generalPanel.setOpaque(false);

                JPanel generalMenuPanel = new JPanel(new GridBagLayout());
                generalMenuPanel.setOpaque(false);
                generalMenuPanel.setBackground(new Color(0, 0, 0, 0));
                generalMenuPanel.setPreferredSize(new Dimension(1000, 800));
                GridBagConstraints gMPgbc = new GridBagConstraints();
                gMPgbc.gridx = 0;
                gMPgbc.gridy = 0;
                gMPgbc.anchor = GridBagConstraints.LINE_START;
                gMPgbc.weightx = 1;
                gMPgbc.fill = GridBagConstraints.HORIZONTAL;
                gMPgbc.insets = new Insets(1, 0, 5, 0);
                generalMenuPanel.add(miscButton, gMPgbc);
                gMPgbc.gridx = 1;
                gMPgbc.weightx = 1;
                gMPgbc.fill = GridBagConstraints.HORIZONTAL;
                generalMenuPanel.add(economyButton, gMPgbc);
                gMPgbc.gridx = 2;
                gMPgbc.weightx = 1;
                gMPgbc.fill = GridBagConstraints.HORIZONTAL;
                generalMenuPanel.add(productionButton, gMPgbc);
                gMPgbc.gridx = 3;
                gMPgbc.weightx = 1;
                gMPgbc.fill = GridBagConstraints.HORIZONTAL;
                generalMenuPanel.add(logisticsButton, gMPgbc);
                gMPgbc.gridx = 0;
                gMPgbc.gridy = 1;
                gMPgbc.gridwidth = 4;
                gMPgbc.fill = GridBagConstraints.BOTH;
                gMPgbc.weightx = 1.0;
                gMPgbc.weighty = 1.0;
                gMPgbc.insets = new Insets(0, 0, 0, 0);

//=====================================================OPENING PANEL==================================================\\
                JTextArea myNoteArea = new JTextArea(5, 30);
                JPanel gIP = UI.createGeneralInfoPanel(myNoteArea);
                generalMenuPanel.add(gIP, gMPgbc);

//====================================================================================================================\\

//==================================================Miscellaneous Panel===============================================\\



                UI.MoralPanelResult result = UI.createModernMoralPanel(moralService, currentUserFaction);

                JPanel miscellaneousPanel = new JPanel(new GridBagLayout());
                miscellaneousPanel.setPreferredSize(new Dimension(1000, 1200));
                GridBagConstraints mPgbc = new GridBagConstraints();
                JPanel moralPanel = result.panel;
                mPgbc.anchor = GridBagConstraints.PAGE_START;
                mPgbc.gridy = 0;
                mPgbc.gridx = 0;
                mPgbc.weightx = 1;
                moralPanel.setPreferredSize(new Dimension(980, 400));
                miscellaneousPanel.add(moralPanel, mPgbc);
                RumorService rumorService = new RumorServiceImpl();
                RumorDisplayPanel rumorDisplayPanel = new RumorDisplayPanel();
                java.util.List<Rumor> userFactionRumors = rumorService.getApprovedRumorsForFaction(currentUserFaction.getDisplayName());
                if ("Admin".equals(Login.currentUser)) {
                // Panel admin avec bouton d'accès au panel de gestion
                JPanel adminRumorPanel = createAdminRumorAccessPanel();
                mPgbc.gridy = 1;
                mPgbc.weighty = 1;
                miscellaneousPanel.add(adminRumorPanel, mPgbc);
                } else {
                JScrollPane rumorScrollPane = new JScrollPane(rumorDisplayPanel);
                rumorScrollPane.setPreferredSize(new Dimension(980, 300));
                UI.styleScrollPane(rumorScrollPane);
                UI.configureScrollSpeed(rumorScrollPane,20,80);
                rumorScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.white, 2),
                "Rumeurs circulants chez : " + currentUserFaction.getDisplayName(),
                0, 0,
                new Font("Arial", Font.BOLD, 14),
                Color.white));
                rumorScrollPane.setBackground(new Color(50, 50, 50, 200));
                rumorDisplayPanel.displayRumors(userFactionRumors);
                mPgbc.gridy = 1;
                mPgbc.weighty = 1;
                miscellaneousPanel.add(rumorScrollPane, mPgbc);}
                miscellaneousPanel.setOpaque(false);
                miscellaneousPanel.setBackground(Color.black);
//====================================================================================================================\\

//====================================================Market Panel====================================================\\

        MarketManagementPanel marketPanel = new MarketManagementPanel(
             economicService, personnelService, repo, currentUserFaction.getId()) ;


//====================================================================================================================\\

//===============================================Economic Panel=======================================================\\


        UI.EnhancedEconomicPanel enhancedEconomyPanel = new UI.EnhancedEconomicPanel(economicService, repo);
        JPanel populationSummary = UIHelpers.createPopulationSummaryPanel(repo, currentUserFaction.getDisplayName());
        JPanel combinedEcoPanel = new JPanel(new GridBagLayout());
        combinedEcoPanel.setOpaque(false);
        GridBagConstraints ecoGbc = new GridBagConstraints();
        ecoGbc.gridx = 0; ecoGbc.gridy = 0;
        ecoGbc.weightx = 1.0; ecoGbc.weighty = 0.3;
        ecoGbc.fill = GridBagConstraints.BOTH;
        ecoGbc.insets = new Insets(5, 5, 5, 5);
        combinedEcoPanel.add(populationSummary, ecoGbc);

        ecoGbc.gridy = 1; ecoGbc.weighty = 0.7;
        combinedEcoPanel.add(enhancedEconomyPanel, ecoGbc);

        JScrollPane economyScrollPane = new JScrollPane(combinedEcoPanel);
        economyScrollPane.setOpaque(false);
        UI.styleScrollPane(economyScrollPane);
        UI.configureScrollSpeed(economyScrollPane,20,80);
        economyScrollPane.getViewport().setOpaque(false);

//====================================================================================================================\\

//=============================================== Production Panel====================================================\\

        Map<String, SafeHexDetails> hexProdGrid = repo.loadSafeAll();
        enhancedProductionPanelInstance = UIHelpers.createEnhancedProductionPanel(
                hexProdGrid, currentUserFaction.getId(), repo, economicService);
        UI.styleScrollPane(enhancedProductionPanelInstance);

//===================+================================================================================================\\
//================================================Logistics Panel=====================================================\\
        LogisticsService logisticsService = economicService.getLogisticsService();
        LogisticsPanel logisticsPanel = new LogisticsPanel(logisticsService, repo);
        JScrollPane logisticsScrollPane = new JScrollPane(logisticsPanel);
        UI.styleScrollPane(logisticsScrollPane);
        UI.configureScrollSpeed(logisticsScrollPane,20,80);
        logisticsScrollPane.setOpaque(false);
        logisticsScrollPane.getViewport().setOpaque(false);


//==================================================Moral Connection==================================================\\

        UIHelpers.connectMoralPanelToEconomicService(result, economicService);

//===================================================================================================================\\

                generalPanel.add(generalMenuPanel, "General");
                generalPanel.add(miscellaneousPanel, "Miscellaneous");
                generalPanel.add(economyScrollPane, "Economy");
                generalPanel.add(enhancedProductionPanelInstance, "Production");
                generalPanel.add(logisticsScrollPane, "Logistics");


                miscButton.addActionListener(e -> {
                    generalCardLayout.show(generalPanel, "Miscellaneous");
                    backToGeneralButton.setVisible(true);
                });
                economyButton.addActionListener(e -> {
                    generalCardLayout.show(generalPanel, "Economy");
                    ecoIsShowed = true;
                    backToGeneralButton.setVisible(true);
                    economicService.calculateInitialData();
                    updatePanel.setVisible(true);
                });
                productionButton.addActionListener(e -> {
                    generalCardLayout.show(generalPanel, "Production");
                    prodIsShowed = true;
                    economicService.calculateInitialData();
                    backToGeneralButton.setVisible(true);
                    updatePanel.setVisible(true);
                });
                logisticsButton.addActionListener(e -> {
                    generalCardLayout.show(generalPanel, "Logistics");
                    logIsShowed = true;
                    backToGeneralButton.setVisible(true);
                    updatePanel.setVisible(true);
                    if (logisticsService != null) { logisticsService.printTransportNetwork();}
                });
                backToGeneralButton.addActionListener(e -> {
                    generalCardLayout.show(generalPanel, "General");
                    prodIsShowed = false;
                    ecoIsShowed = false;
                    backToGeneralButton.setVisible(false);
                    updatePanel.setVisible(false);
                });
                updatePanel.addActionListener(e -> {
                    if (ecoIsShowed) economicService.calculateInitialData();
                    if (prodIsShowed)enhancedProductionPanelInstance.refreshContent();
                    if (logIsShowed) logisticsService.initializeNetwork();
                });


                JPanel diplomacyPanel = new JPanel();
                JPanel militaryPanel = new JPanel();
                JPanel studyPanel = new JPanel();
                CardLayout factionCardLayout = (CardLayout) factionContentPanel.getLayout();
                factionContentPanel.add(generalPanel, "General");
                factionContentPanel.add(diplomacyPanel, "Diplomacy");
                factionContentPanel.add(militaryPanel, "Military");
                factionContentPanel.add(studyPanel, "Study");
                factionContentPanel.add(marketPanel, "Market");
                factionLayeredPane.add(factionContentPanel, JLayeredPane.DEFAULT_LAYER);
                factionLayeredPane.add(updatePanel, JLayeredPane.POPUP_LAYER);
                factionLayeredPane.add(backToGeneralButton, JLayeredPane.POPUP_LAYER);

                EmblemPrinting emblem = new EmblemPrinting(factionLogPath);
                emblem.setOpaque(false);
                emblem.setBounds((int) (getWidth() * 0.72), (int) (getHeight() * 0.49), 288, 435);
                emblem.setVisible(false);


//\\================================================================================================================//\\

                 // Add all menus to the cardPanel
                cardPanel.add(UIHelpers.createMainMenu("Menu Principal", "/RessourceGen/BGimage.png"), "Menu1");
                cardPanel.add(UIHelpers.createFactionMenu(factionBackgroundImage, factionLayeredPane), "Menu2");
                cardPanel.add(UIHelpers.createMainMenu("Combat", "/RessourceGen/bg_combat.jpg"), "Menu3");
                cardPanel.add(UIHelpers.createMainMenu("Règles", "/RessourceGen/bg_regles.jpg"), "Menu4");

                JPanel factionButtonPanel = new JPanel();
                factionButtonPanel.add(factionMenu1);
                factionButtonPanel.add(factionMenu2);
                factionButtonPanel.add(factionMenu3);
                factionButtonPanel.add(factionMenu4);


                backButton.setHorizontalTextPosition(SwingConstants.CENTER);
                backButton.setFont(new Font("Oswald", Font.BOLD, 25));
                backButton.setBackground(new Color(0, 0, 0, 0));
                backButton.setBorder(null);
                backButton.setOpaque(false);
                backButton.setIcon(ButtonCadreSign);
                backButton.setContentAreaFilled(false);
                backButton.setVisible(false);

                setMenuButtonProperties(Menu1, "Mondes", ButtonCadreSign);
                setMenuButtonProperties(Menu2, "Factions", ButtonCadreSign);
                setMenuButtonProperties(Menu3, "Combat", ButtonCadreSign);
                setMenuButtonProperties(Menu4, "Règles", ButtonCadreSign);
                setMenuButtonProperties(Menu5, "Quitter", ButtonCadreSign);
                setMenuButtonProperties(factionMenu1, "Général", ButtonCadreSign);
                setMenuButtonProperties(factionMenu1_1, "Marché",ButtonCadreSign);
                setMenuButtonProperties(factionMenu1_1A, "AdminM",ButtonCadreSign);
                setMenuButtonProperties(factionMenu2, "Diplomatie", ButtonCadreSign);
                setMenuButtonProperties(factionMenu3, "Militaire", ButtonCadreSign);
                setMenuButtonProperties(factionMenu4, "Etude", ButtonCadreSign);

                Mute.addActionListener(this);
                backButton.addActionListener(e -> {
                    audio.playClick();
                    if (factionTheme.get() && (!Login.currentUser.equals("t") && !Login.currentUser.equals("Admin") && !Login.mondeOpen)) {
                        audio.stop();
                        factionTheme.set(false);
                        new Thread(() -> {
                            try {
                                Thread.sleep(100);
                                audio.fadeIn();
                                } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }).start();

                    } else {
                        factionTheme.set(false);
                        audio.stop();
                        audio.playHexMusicMenu();
                    }

                    isFactionMenuOpen = false;
                    cardLayout.show(cardPanel, "Menu1");
                    backButton.setVisible(false);
                    emblem.setVisible(false);
                    Menu1.setVisible(true);
                    Menu2.setVisible(true);
                    Menu3.setVisible(true);
                    Menu4.setVisible(true);
                    Menu5.setVisible(true);
                    volumeSlider.setVisible(true);
                    volumeSliderName.setVisible(true);
                        Mute.setVisible(true);
                });
                Menu1.addActionListener(e -> {
                if(!Login.mondeOpen){
                cardLayout.show(cardPanel, "Menu1");
                Menu5.setVisible(true);
                factionMenu1.setVisible(false);
                audio.playClick();
                LoadingWindow.showSplash(repo, audio);
                        new Timer(100, evt -> {
                            if (LoadingWindow.readyToClose) {
                                splash.dispose();
                                LoadingWindow.mondesWindow.toFront();
                                LoadingWindow.mondesWindow.requestFocus();
                                ((Timer)evt.getSource()).stop();
                            }
                        }).start();
                Login.mondeOpen = true;
                    }
               });
                Menu2.addActionListener(e -> {
                    isFactionMenuOpen = true;
                    logisticsService.ensureNetworkInitialized();
                    factionTheme.set(true);
                    audio.playClick();
                    cardLayout.show(cardPanel, "Menu2");
                    backButton.setVisible(true);
                    Menu5.setVisible(false);
                    if(Login.mondeOpen){audio.stop();}
                       else { audio.fadeOut();}
                        new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                                audio.playTheme(currentUserFaction.getId());
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }).start();
                        emblem.setVisible(true);

                    Menu1.setVisible(false);
                    if ("Admin".equals(Login.currentUser)) { factionMenu1_1A.setVisible(true);}
                    else { factionMenu1_1.setVisible(true); }
                    Menu2.setVisible(false);
                    factionMenu2.setVisible(true);
                    Menu3.setVisible(false);
                    factionMenu3.setVisible(true);
                    Menu4.setVisible(false);
                    factionMenu4.setVisible(true);
                    volumeSlider.setVisible(false);
                    volumeSliderName.setVisible(false);
                    Mute.setVisible(false);
                });
                Menu3.addActionListener(e -> {
                    audio.playClick();
                    cardLayout.show(cardPanel, "Menu3");
                    backButton.setVisible(true);
                    Menu5.setVisible(false);
                });
                Menu4.addActionListener(e -> {
                    audio.playClick();
                    cardLayout.show(cardPanel, "Menu4");
                    backButton.setVisible(true);
                    Menu5.setVisible(false);
                });
                Menu5.addActionListener(e -> {
                    audio.playClick();
                    audio.stop();
                    System.exit(0);

                });

                factionMenu1.addActionListener(e -> {
                    audio.playClick();
                    factionCardLayout.show(factionContentPanel, "General"); factionMenu1.setVisible(false); factionMenu1_1.setVisible(true);

                });
                factionMenu1_1.addActionListener(e -> {
                    audio.playClick(); factionMenu1.setVisible(true);
                    factionCardLayout.show(factionContentPanel, "Market");
                });
                factionMenu1_1A.addActionListener(e -> {
                audio.playClick(); factionMenu1.setVisible(true);
                factionCardLayout.show(factionContentPanel, "Market");
                openMarketAdminPanel();
                });

                factionMenu2.addActionListener(e -> audio.playClick());
                factionMenu3.addActionListener(e -> audio.playClick());
                factionMenu4.addActionListener(e -> audio.playClick());

                MuteIconON = resizeIcon(new ImageIcon(Objects.requireNonNull(getClass().getResource("/MuteIconON.png"))), 50, 50);
                MuteIconOFF = resizeIcon(new ImageIcon(Objects.requireNonNull(getClass().getResource("/MuteIconOFF.png"))), 50, 50);
                Mute.setFocusable(false);
                Mute.setIcon(MuteIconOFF);
                Mute.setSelectedIcon(MuteIconON);
                Mute.setBackground(new Color(0, 0, 0, 0));
                Mute.setIconTextGap(0);
                Mute.setOpaque(false);

                volumeSlider.setOpaque(false);
                volumeSlider.setFocusable(false);
                volumeSlider.setPaintLabels(false);
                volumeSlider.setMajorTickSpacing(100);
                volumeSlider.setMinorTickSpacing(100);
                volumeSlider.setPaintTrack(true);
                volumeSlider.setPaintTicks(false);
                volumeSliderName.setFont(new Font("Oswald", Font.BOLD, 15));
                volumeSliderName.setForeground(Color.WHITE);
                volumeSlider.addChangeListener(e -> audio.adjustVolume(volumeSlider.getValue()));
                this.setLayout(null);

                JLabel overlayLabel = new JLabel(new ImageIcon(overlayImg));
                overlayLabel.setBounds(1050, 45, overlayImg.getWidth(), overlayImg.getHeight());

                layeredPane.add(overlayLabel, Integer.valueOf(1));
                layeredPane.add(cardPanel, Integer.valueOf(1));
                layeredPane.add(emblem, Integer.valueOf(2));
                layeredPane.add(Menu1, Integer.valueOf(2));
                layeredPane.add(Menu2, Integer.valueOf(2));
                layeredPane.add(Menu3, Integer.valueOf(2));
                layeredPane.add(Menu4, Integer.valueOf(2));
                layeredPane.add(Menu5, Integer.valueOf(2));
                layeredPane.add(factionMenu1, Integer.valueOf(2));
                layeredPane.add(factionMenu1_1, Integer.valueOf(2));
                layeredPane.add(factionMenu1_1A, Integer.valueOf(2));
                layeredPane.add(factionMenu2, Integer.valueOf(2));
                layeredPane.add(factionMenu3, Integer.valueOf(2));
                layeredPane.add(factionMenu4, Integer.valueOf(2));
                layeredPane.add(backButton, Integer.valueOf(2));
                layeredPane.add(nomMenu, Integer.valueOf(3));
                layeredPane.add(Mute, Integer.valueOf(3));
                layeredPane.add(volumeSlider, Integer.valueOf(3));
                layeredPane.add(volumeSliderName, Integer.valueOf(3));
                layeredPane.revalidate();
                layeredPane.repaint();

                this.addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent e) {
                        updatePositions();
                    }
                });


                String user = Login.currentUser.toLowerCase();
                String documentsPath = FileSystemView.getFileSystemView().getDefaultDirectory().getPath();
                String saveDirPath = documentsPath + File.separator + "Iseria_Divers";
                String notesFilename = "notes_" + user + ".txt";
                File saveDir = new File(saveDirPath);
                notesFile = new File(saveDir, notesFilename);
                loadNotes(myNoteArea);
                setupAutoSave(myNoteArea);
                setContentPane(layeredPane);
                audio.playRandomMainThemeAuto();

    }

    private ImageIcon resizeIcon(ImageIcon icon, int width, int height) {
        Image img = icon.getImage();
        Image resizedImage = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(resizedImage);
    }
    public void setMenuButtonProperties(JButton button, String text, Icon icon) {
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setText(text);
        button.setFont(new Font("Oswald", Font.BOLD, 25));
        button.setIcon(icon);
        button.setBackground(new Color(0, 0, 0, 0));
        button.setBorder(null);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        if (button.getText().equals("Général")) {
            button.setVisible(false);
        }
        if (button.getText().equals("Diplomatie")) {
            button.setVisible(false);
        }
        if (button.getText().equals("Etude")) {
            button.setVisible(false);
        }
        if (button.getText().equals("Militaire")) {
            button.setVisible(false);
        }
        if (button.getText().equals("Marché")) {
            button.setVisible(false);
        }
    }
    private void updatePositions() {
        int frameWidth = getWidth();
        int frameHeight = getHeight();

        // Update the position of each button based on window size
        Menu1.setBounds((int) (frameWidth * 0.75), (int) (frameHeight * .10), 150, 100);
        Menu2.setBounds((int) (frameWidth * 0.75), (int) (frameHeight * .18), 150, 100);
        Menu3.setBounds((int) (frameWidth * 0.75), (int) (frameHeight * .26), 150, 100);
        Menu4.setBounds((int) (frameWidth * 0.75), (int) (frameHeight * .34), 150, 100);
        Menu5.setBounds((int) (frameWidth * 0.75), (int) (frameHeight * .42), 150, 100);
        factionMenu1.setBounds((int) (frameWidth * 0.75), (int) (frameHeight * .10), 150, 100);
        factionMenu1_1.setBounds((int) (frameWidth * 0.75), (int) (frameHeight * .10), 150, 100);
        factionMenu1_1A.setBounds((int) (frameWidth * 0.75), (int) (frameHeight * .10), 150, 100);
        factionMenu2.setBounds((int) (frameWidth * 0.75), (int) (frameHeight * .18), 150, 100);
        factionMenu3.setBounds((int) (frameWidth * 0.75), (int) (frameHeight * .26), 150, 100);
        factionMenu4.setBounds((int) (frameWidth * 0.75), (int) (frameHeight * .34), 150, 100);
        backButton.setBounds((int) (frameWidth * 0.75), (int) (frameHeight * .42), 150, 100);
        Mute.setBounds((int) (frameWidth * 0.05), (int) (frameHeight * 0.85), 50, 50);

        volumeSlider.setBounds((int) (frameWidth * 0.00003), (int) (frameHeight * 0.81), 150, 100);
        volumeSliderName.setBounds((int) (frameWidth * 0.05), (int) (frameHeight * 0.83), 100, 25);

    }
    public static String getCurrentFactionId() {
        return currentUserFaction != null ? currentUserFaction.getId() : "Free";
    }
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == Mute) {
            boolean isFading = false;
            if (!isFading) {
                if (Mute.isSelected()) {
                    System.out.println("sound off");
                    audio.fadeOut();
                } else {
                    System.out.println("sound on");
                    audio.fadeIn();}}}
    }
    private void setupAutoSave(JTextArea area) {
        area.getDocument().addDocumentListener(new DocumentListener() {
            private void autoSave() {
                saveNotes(area);
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                autoSave();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                autoSave();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                autoSave();
            }
        });
    }
    private void loadNotes(JTextArea area) {
        if (!notesFile.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(notesFile))) {
            area.read(reader, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void saveNotes(JTextArea area) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(notesFile))) {
            area.write(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static EconomicDataService getEconomicService() {
        return economicService;
    }

    private void openMarketAdminPanel() {
        audio.playClick();
        SwingUtilities.invokeLater(() -> {
            try {
                MarketAdminPanel adminPanel = new MarketAdminPanel();
                adminPanel.setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error opening Market Admin Panel: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    private JPanel createAdminRumorAccessPanel() {
        JPanel adminPanel = new JPanel(new BorderLayout());
        adminPanel.setOpaque(false);
        adminPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.RED, 3),
                "🛡️ Panel Administrateur - Gestion des Rumeurs",
                0, 0,
                new Font("Arial", Font.BOLD, 16),
                Color.RED));

        JLabel adminWelcome = new JLabel(
                "<html><center>" +
                        "<h2>🛡️ Mode Administrateur Activé</h2>" +
                        "<p>Vous avez accès à la gestion complète des rumeurs de toutes les factions.</p>" +
                        "</center></html>",
                SwingConstants.CENTER);
        adminWelcome.setForeground(Color.BLACK);
        adminPanel.add(adminWelcome, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setOpaque(false);

        JButton openAdminBtn = new JButton("🗂️ Ouvrir Panel de Gestion");
        openAdminBtn.setFont(new Font("Arial", Font.BOLD, 14));
        openAdminBtn.setBackground(new Color(220, 20, 60));
        openAdminBtn.setForeground(Color.WHITE);
        openAdminBtn.setPreferredSize(new Dimension(250, 40));
        openAdminBtn.addActionListener(e -> openAdminRumorPanel());

        JButton quickStatsBtn = new JButton("📊 Statistiques");
        quickStatsBtn.setFont(new Font("Arial", Font.BOLD, 14));
        quickStatsBtn.setBackground(new Color(70, 130, 180));
        quickStatsBtn.setForeground(Color.WHITE);
        quickStatsBtn.setPreferredSize(new Dimension(150, 40));
        quickStatsBtn.addActionListener(e -> audio.playClick());

        buttonPanel.add(openAdminBtn);
        buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(quickStatsBtn);

        adminPanel.add(buttonPanel, BorderLayout.SOUTH);

        return adminPanel;
    }
    private void initializePersonnelService() {
        try {
            // Créer le répertoire de sauvegarde s'il n'existe pas
            String user = Login.currentUser.toLowerCase();
            String documentsPath = FileSystemView.getFileSystemView().getDefaultDirectory().getPath();
            String saveDirPath = documentsPath + File.separator + "IseriaDivers" + File.separator + user;

            File saveDir = new File(saveDirPath);
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

            // Initialiser le service
            personnelService = new PersonnelDataService(saveDirPath);
            System.out.println("PersonnelDataService initialized successfully");

        } catch (Exception e) {
            System.err.println("Error initializing PersonnelDataService: " + e.getMessage());
            e.printStackTrace();

            // Fallback - service basique
            personnelService = new PersonnelDataService(System.getProperty("user.home"));
        }
    }
    private void openAdminRumorPanel() {
        audio.playClick();
        RumorService rumorService = new RumorServiceImpl();
        AdminRumorManagementPanel adminPanel = new AdminRumorManagementPanel(rumorService);
        adminPanel.setVisible(true);
    }
}

class EmblemPrinting extends JPanel {

    public BufferedImage FactionEmblem;

    public EmblemPrinting(String imagePath) {

        try {
            URL imageUrl = getClass().getResource(imagePath);
            System.out.println("Emblem Image URL: " + imageUrl);
            if (imageUrl != null) {
                FactionEmblem = ImageIO.read(imageUrl);
            } else {
                System.out.println("Error: Image not found at path: " + imagePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (FactionEmblem != null) {
            g.drawImage(FactionEmblem, 0, 0, getWidth(), getHeight(), this); // Draw image
        } else {
            System.out.println("Error: Faction emblem is null");
        }
    }}






