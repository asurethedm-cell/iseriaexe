package com.iseria.ui;

import java.awt.GraphicsEnvironment;
import com.iseria.domain.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

public class Login extends JFrame implements ActionListener {

    private final IDataProvider data;
    private final IAudioService audio;
    private final IHexRepository repo;
    private BufferedImage backgroundImage; // ✨ NOUVEAU: Image de fond

    JButton SUBMIT;
    JPanel panel;
    JLabel label1, label2;
    JTextField text1;
    JPasswordField text2;

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


        try {
            backgroundImage = ImageIO.read(getClass().getResource("/RessourceGen/background_logger.jpg"));
            System.out.println("Image de fond chargée avec succès");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Impossible de charger l'image de fond: /RessourceGen/background_logger.jpg");
        }

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
        layoutComponents();
    }


    private void setupTransparentComponents() {
        label1 = new JLabel("Username:");
        label1.setFont(new Font("Arial", Font.BOLD, 16));
        label1.setForeground(Color.WHITE);
        label1.setOpaque(true);
        label1.setBackground(new Color(100, 100, 100, 150));
        label1.setHorizontalAlignment(SwingConstants.CENTER);
        label1.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        label2 = new JLabel("Password:");
        label2.setFont(new Font("Arial", Font.BOLD, 16));
        label2.setForeground(Color.WHITE);
        label2.setOpaque(true);
        label2.setBackground(new Color(100, 100, 100, 180));
        label2.setHorizontalAlignment(SwingConstants.CENTER);
        label2.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        text1 = new JTextField(15);
        text1.setFont(new Font("Arial", Font.PLAIN, 14));
        text1.setOpaque(true);
        text1.setBackground(new Color(220, 220, 220, 180));
        text1.setForeground(Color.BLACK);
        text1.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        text2 = new JPasswordField(15);
        text2.setFont(new Font("Arial", Font.PLAIN, 14));
        text2.setOpaque(true);
        text2.setBackground(new Color(220, 220, 220, 180));
        text2.setForeground(Color.BLACK);
        text2.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        // Bouton avec style semi-transparent
        SUBMIT = new JButton("SUBMIT");
        SUBMIT.setFont(new Font("Arial", Font.BOLD, 16));
        SUBMIT.setOpaque(true);
        SUBMIT.setBackground(new Color(70, 130, 180, 200));
        SUBMIT.setForeground(Color.WHITE);
        SUBMIT.setFocusPainted(false);
        SUBMIT.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));

        // Actions
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

        JLabel titleLabel = new JLabel("ISERIA");
        titleLabel.setFont(new Font("Bahnschrift", Font.BOLD, 48));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setOpaque(true);
        titleLabel.setBackground(new Color(0, 0, 0, 100));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(100, 20, 50, 20); // Plus d'espace en haut
        panel.add(titleLabel, gbc);


        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 20, 10, 10);
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(label1, gbc);

        // Username field
        gbc.gridx = 1; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 10, 10, 20);
        panel.add(text1, gbc);

        // Password label
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(10, 20, 10, 10);
        panel.add(label2, gbc);

        // Password field
        gbc.gridx = 1; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 10, 10, 20);
        panel.add(text2, gbc);

        // Submit button
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(30, 20, 20, 20);
        panel.add(SUBMIT, gbc);
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
            // ✨ Message d'erreur avec style personnalisé
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
        public static void printFonts() {
            // Obtenir l'environnement graphique local
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

            // Récupérer tous les noms de familles de polices
            String[] fontNames = ge.getAvailableFontFamilyNames();

            System.out.println("=== POLICES DISPONIBLES ===");
            for (String fontName : fontNames) {
                System.out.println(fontName);
            }

            System.out.println("\nNombre total de polices : " + fontNames.length);
        }
    }
}
