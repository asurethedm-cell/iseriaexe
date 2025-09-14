package com.iseria.ui;

import java.awt.GraphicsEnvironment;
import com.iseria.domain.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;

import static com.iseria.ui.Login.ListJavaFonts.printFonts;

public class Login extends JFrame implements ActionListener {

    private final IDataProvider data;
    private final IAudioService audio;
    private final IHexRepository repo;
    private BufferedImage backgroundImage;

    JButton SUBMIT;
    JPanel panel;
    JLabel label1, label2;
    JTextField text1;
    JPasswordField text2;

    // ✨ NOUVEAU: References pour les wrappers
    private JComponent text1Wrapper;
    private JComponent text2Wrapper;

    public static String currentUser;
    public static boolean waitNextTurn;
    private static boolean placeForteEnabled = false;
    public static boolean mondeOpen = false;
    public static HashMap<String, String> users = new HashMap<>();

    public static boolean isPlaceForteEnabled() {
        return placeForteEnabled;
    }
    public static void setPlaceForteEnabled(boolean enabled) {
        placeForteEnabled = enabled;
    }

    public Login(IDataProvider data, IAudioService audio, IHexRepository repo) {
        this.data = data;
        this.audio = audio;
        this.repo = repo;
        waitNextTurn = false;
        System.out.println("Turn paused : " + waitNextTurn);

        backgroundImage = loadRandomBackgroundImage();

        users.put("Admin", "admin");
        users.put("Bladjorn", "orange");
        users.put("Klowh", "poing");
        users.put("t", "t");

        panel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        panel.setOpaque(true);

        setupTransparentComponents();
        setupWindow();
        //ListJavaFonts.printFonts(); //Prints for Logo Police Choice
        layoutComponents();
    }
    private JComponent createTransparentTextField(int columns) {
        // Panel wrapper avec fond semi-transparent
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(true);
        wrapper.setBackground(new Color(255, 255, 255, 255)); // Gris clair semi-transparent
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        // TextField transparent à l'intérieur
        JTextField textField = new JTextField(columns);
        textField.setOpaque(false); // ✨ Transparent pour voir le wrapper
        textField.setBorder(null);  // ✨ Pas de bordure interne
        textField.setFont(new Font("Bahnschrift", Font.PLAIN, 14));
        textField.setForeground(Color.BLACK);

        wrapper.add(textField, BorderLayout.CENTER);
        return wrapper;
    }
    private JComponent createTransparentPasswordField(int columns) {
        // Panel wrapper avec fond semi-transparent
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(true);
        wrapper.setBackground(new Color(255, 255, 255, 255)); // Gris clair semi-transparent
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        // PasswordField transparent à l'intérieur
        JPasswordField passwordField = new JPasswordField(columns);
        passwordField.setOpaque(false); // ✨ Transparent pour voir le wrapper
        passwordField.setBorder(null);  // ✨ Pas de bordure interne
        passwordField.setFont(new Font("Bahnschrift", Font.PLAIN, 14));
        passwordField.setForeground(Color.BLACK);

        wrapper.add(passwordField, BorderLayout.CENTER);
        return wrapper;
    }
    private void setupTransparentComponents() {
        // Labels (inchangés)
        label1 = new JLabel("Username:");
        label1.setFont(new Font("Bahnschrift", Font.BOLD, 16));
        label1.setForeground(Color.WHITE);
        label1.setOpaque(true);
        label1.setBackground(new Color(100, 100, 100, 150));
        label1.setHorizontalAlignment(SwingConstants.CENTER);
        label1.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        label2 = new JLabel("Password:");
        label2.setFont(new Font("Bahnschrift", Font.BOLD, 16));
        label2.setForeground(Color.WHITE);
        label2.setOpaque(true);
        label2.setBackground(new Color(100, 100, 100, 150));
        label2.setHorizontalAlignment(SwingConstants.CENTER);
        label2.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        // ✨ NOUVEAU: Créer les wrappers transparents
        text1Wrapper = createTransparentTextField(15);
        text2Wrapper = createTransparentPasswordField(15);

        // ✨ NOUVEAU: Extraire les références des champs pour les événements
        text1 = (JTextField) ((JPanel) text1Wrapper).getComponent(0);
        text2 = (JPasswordField) ((JPanel) text2Wrapper).getComponent(0);

        // Bouton (inchangé)
        SUBMIT = new JButton("SUBMIT");
        SUBMIT.setFont(new Font("Bahnschrift", Font.BOLD, 16));
        SUBMIT.setOpaque(true);
        SUBMIT.setBackground(new Color(70, 130, 180, 200));
        SUBMIT.setForeground(Color.WHITE);
        SUBMIT.setFocusPainted(false);
        SUBMIT.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));

        // ✨ Actions (inchangées)
        SUBMIT.addActionListener(this);
        text2.addActionListener(e -> SUBMIT.doClick());
    }
    private void setupWindow() {
        setTitle("Iseria Logger");
        setSize(1600, 900);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        ImageIcon Icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/Icon.png")));
        setIconImage(Icon.getImage());
        if (Icon.getImageLoadStatus() == MediaTracker.ERRORED) {
            System.out.println("Erreur lors du chargement de l'icône.");
        }

        setContentPane(panel);
    }
    private void layoutComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 20, 20, 20);
        gbc.anchor = GridBagConstraints.CENTER;

        // Titre
        JLabel titleLabel = new JLabel("ISERIA");
        titleLabel.setFont(new Font("Baskerville Old Face", Font.BOLD, 48));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setOpaque(true);
        titleLabel.setBackground(new Color(0, 0, 0, 100));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(100, 20, 50, 20);
        panel.add(titleLabel, gbc);

        // Username label
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 20, 10, 10);
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(label1, gbc);


        gbc.gridx = 1; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 10, 10, 20);
        panel.add(text1Wrapper, gbc);

        // Password label
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(10, 20, 10, 10);
        panel.add(label2, gbc);


        gbc.gridx = 1; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 10, 10, 20);
        panel.add(text2Wrapper, gbc);

        // Submit button
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(30, 20, 20, 20);
        panel.add(SUBMIT, gbc);
    }
    private BufferedImage loadRandomBackgroundImage() {
        try {
            String basePath = "/RessourceGen/";
            String[] imageFiles = {
                    "background_logger_0.jpg",
                    "background_logger_1.jpg",
                    "background_logger_2.jpg",
                    "background_logger_3.jpg",

            };
            Random random = new Random();
            String selectedImage = imageFiles[random.nextInt(imageFiles.length)];
            System.out.println("🎲 Image sélectionnée : " + selectedImage);
            BufferedImage image = ImageIO.read(getClass().getResource(basePath + selectedImage));
            System.out.println("✅ Image de fond chargée avec succès : " + selectedImage);

            return image;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Erreur lors du chargement aléatoire, fallback vers image par défaut");
            try {
                return ImageIO.read(getClass().getResource("/RessourceGen/background_logger_0.jpg"));
            } catch (IOException fallbackEx) {
                System.err.println("❌ Impossible de charger l'image par défaut");
                return null;
            }
        }
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        String username = text1.getText();
        String password = new String(text2.getPassword());

        if (users.containsKey(username) && users.get(username).equals(password)) {
            currentUser = username;
            MainMenu mainMenu = new MainMenu(data, audio, repo);
            mainMenu.setVisible(true);
            this.dispose();
        } else {
            JOptionPane optionPane = new JOptionPane(
                    "Incorrect login or password",
                    JOptionPane.ERROR_MESSAGE
            );
            JDialog dialog = optionPane.createDialog(this, "Error");
            dialog.setModal(true);
            dialog.setVisible(true);
        }
    }

    public static class ListJavaFonts {

        /**
         * ✨ NOUVEAU: Affiche une fenêtre avec toutes les polices disponibles
         */
        public static void printFonts() {
            SwingUtilities.invokeLater(() -> {
                FontPreviewWindow window = new FontPreviewWindow();
                window.setVisible(true);
            });
        }

        /**
         * ✨ NOUVEAU: Fenêtre de prévisualisation des polices
         */
        private static class FontPreviewWindow extends JFrame {

            public FontPreviewWindow() {
                setupWindow();
                createFontTable();
            }

            private void setupWindow() {
                setTitle("🔤 Polices Disponibles - Iseria Font Preview");
                setSize(1000, 700);
                setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                setLocationRelativeTo(null);

                // Icône de la fenêtre (optionnel)
                try {
                    setIconImage(new ImageIcon(getClass().getResource("/Icon.png")).getImage());
                } catch (Exception e) {
                    // Ignorer si l'icône n'existe pas
                }
            }

            private void createFontTable() {
                // Récupérer toutes les polices disponibles
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                String[] fontNames = ge.getAvailableFontFamilyNames();

                // Créer les colonnes
                String[] columnNames = {"Nom de la Police", "Aperçu : ISERIA"};

                // Créer le modèle de données
                Object[][] data = new Object[fontNames.length][2];
                for (int i = 0; i < fontNames.length; i++) {
                    data[i][0] = fontNames[i];
                    data[i][1] = "ISERIA"; // Le texte sera stylisé par le renderer
                }

                // Créer la JTable avec un modèle personnalisé
                JTable table = new JTable(data, columnNames) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false; // Aucune cellule n'est éditable
                    }
                };

                // ✨ Renderer personnalisé pour la colonne "Aperçu"
                table.getColumnModel().getColumn(1).setCellRenderer(new FontPreviewRenderer(fontNames));

                // ✨ Renderer pour la colonne "Nom" (police simple)
                table.getColumnModel().getColumn(0).setCellRenderer(new FontNameRenderer());

                // Configuration de la table
                table.setRowHeight(35); // Hauteur suffisante pour le texte
                table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                table.getTableHeader().setReorderingAllowed(false);

                // Largeurs des colonnes
                table.getColumnModel().getColumn(0).setPreferredWidth(300);  // Nom
                table.getColumnModel().getColumn(1).setPreferredWidth(500);  // Aperçu

                // Style de la table
                table.setGridColor(Color.LIGHT_GRAY);
                table.setShowGrid(true);
                table.setBackground(Color.WHITE);
                table.setSelectionBackground(new Color(230, 230, 250));

                // Header styling
                table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
                table.getTableHeader().setBackground(new Color(70, 130, 180));
                table.getTableHeader().setForeground(Color.WHITE);

                // ScrollPane
                JScrollPane scrollPane = new JScrollPane(table);
                scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                // Panel d'informations en bas
                JPanel infoPanel = createInfoPanel(fontNames.length);

                // Layout principal
                setLayout(new BorderLayout());
                add(scrollPane, BorderLayout.CENTER);
                add(infoPanel, BorderLayout.SOUTH);
            }

            private JPanel createInfoPanel(int fontCount) {
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                panel.setBackground(new Color(245, 245, 245));
                panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                JLabel infoLabel = new JLabel("📊 Total : " + fontCount + " polices disponibles");
                infoLabel.setFont(new Font("Arial", Font.BOLD, 12));
                infoLabel.setForeground(new Color(70, 130, 180));

                panel.add(infoLabel);
                return panel;
            }
        }

        /**
         * ✨ Renderer pour la colonne des noms de polices
         */
        private static class FontNameRenderer extends DefaultTableCellRenderer {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {

                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Police standard pour le nom
                setFont(new Font("Arial", Font.PLAIN, 12));
                setHorizontalAlignment(SwingConstants.LEFT);

                if (isSelected) {
                    setBackground(table.getSelectionBackground());
                    setForeground(table.getSelectionForeground());
                } else {
                    setBackground(table.getBackground());
                    setForeground(Color.BLACK);
                }

                return c;
            }
        }

        /**
         * ✨ Renderer personnalisé pour afficher "ISERIA" dans chaque police
         */
        private static class FontPreviewRenderer extends DefaultTableCellRenderer {
            private final String[] fontNames;

            public FontPreviewRenderer(String[] fontNames) {
                this.fontNames = fontNames;
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {

                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                try {
                    // Utiliser la police de cette ligne
                    String fontName = fontNames[row];
                    Font previewFont = new Font(fontName, Font.BOLD, 16);
                    setFont(previewFont);

                    // Centrer le texte
                    setHorizontalAlignment(SwingConstants.CENTER);

                    // Couleurs
                    if (isSelected) {
                        setBackground(table.getSelectionBackground());
                        setForeground(new Color(70, 130, 180)); // Bleu pour contraste
                    } else {
                        setBackground(table.getBackground());
                        setForeground(new Color(50, 50, 50)); // Gris foncé pour lisibilité
                    }

                } catch (Exception e) {
                    // Si la police ne peut pas être chargée, utiliser Arial
                    setFont(new Font("Arial", Font.BOLD, 16));
                    setText("ISERIA (erreur police)");
                }

                return c;
            }
        }
    }

}
