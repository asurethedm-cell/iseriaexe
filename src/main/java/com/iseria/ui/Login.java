package com.iseria.ui;

import com.iseria.domain.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;

public class Login extends JFrame implements ActionListener {


    private final IAudioService audio;
    private final IHexRepository repo;
    private BufferedImage backgroundImage;

    JButton LOGIN;
    JPanel panel;
    JLabel label1, label2;
    JTextField text1;
    JPasswordField text2;

    private JComponent text1Wrapper;
    private JComponent text2Wrapper;

    public static String currentUser;
    public static boolean awaitNextTurn;
    private static boolean placeForteEnabled = false;
    public static boolean mondeOpen = false;
    public static HashMap<String, String> users = new HashMap<>();

    public static boolean isPlaceForteEnabled() {
        return placeForteEnabled;
    }
    public static void setPlaceForteEnabled(boolean enabled) {
        placeForteEnabled = enabled;
    }

    public Login(IAudioService audio, IHexRepository repo) {

        this.audio = audio;
        this.repo = repo;
        awaitNextTurn = false;

        backgroundImage = loadRandomBackgroundImage();

        users.put("Admin", "");
        users.put("Bladjorn", "");
        users.put("Klowh", "");
        users.put("Atrociter", "");
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
        Login.this.toFront();
        this.requestFocus();
    }
    private JComponent createTransparentTextField(int columns) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(true);
        wrapper.setBackground(new Color(255, 255, 255, 255));
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        JTextField textField = new JTextField(columns);
        textField.setOpaque(false);
        textField.setBorder(null);
        textField.setFont(new Font("Bahnschrift", Font.PLAIN, 14));
        textField.setForeground(Color.BLACK);

        wrapper.add(textField, BorderLayout.CENTER);
        return wrapper;
    }
    private JComponent createTransparentPasswordField(int columns) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(true);
        wrapper.setBackground(new Color(255, 255, 255, 255));
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        JPasswordField passwordField = new JPasswordField(columns);
        passwordField.setOpaque(false);
        passwordField.setBorder(null);
        passwordField.setFont(new Font("Bahnschrift", Font.PLAIN, 14));
        passwordField.setForeground(Color.BLACK);

        wrapper.add(passwordField, BorderLayout.CENTER);
        return wrapper;
    }
    private void setupTransparentComponents() {

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

        text1Wrapper = createTransparentTextField(15);
        text2Wrapper = createTransparentPasswordField(15);

        text1 = (JTextField) text1Wrapper.getComponent(0);
        text2 = (JPasswordField) text2Wrapper.getComponent(0);

        LOGIN = new JButton("LOGIN");
        LOGIN.setFont(new Font("Bahnschrift", Font.BOLD, 16));
        LOGIN.setOpaque(true);
        LOGIN.setBackground(new Color(70, 130, 180, 200));
        LOGIN.setForeground(Color.WHITE);
        LOGIN.setFocusPainted(false);
        LOGIN.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));

        LOGIN.addActionListener(this);
        text1.addActionListener(e -> LOGIN.doClick());
        text2.addActionListener(e -> LOGIN.doClick());
    }
    private void setupWindow() {
        setTitle("Iseria LogIn");
        setSize(1600, 900);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;
        int windowWidth = 1600;
        int windowHeight = 900;
        int x = (screenWidth - windowWidth) / 2;
        int y = (screenHeight - windowHeight) / 2;
        this.setLocation(x, y);
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

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 20, 10, 10);
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(label1, gbc);


        gbc.gridx = 1; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 10, 10, 20);
        panel.add(text1Wrapper, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(10, 20, 10, 10);
        panel.add(label2, gbc);


        gbc.gridx = 1; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 10, 10, 20);
        panel.add(text2Wrapper, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(30, 20, 20, 20);
        panel.add(LOGIN, gbc);
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
            System.out.println("Image sélectionnée : " + selectedImage);
            BufferedImage image = ImageIO.read(Objects.requireNonNull(getClass().getResource(basePath + selectedImage)));
            System.out.println("Image de fond chargée avec succès : " + selectedImage);

            return image;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur lors du chargement aléatoire, fallback vers image par défaut");
            try {
                return ImageIO.read(Objects.requireNonNull(getClass().getResource("/RessourceGen/background_logger_0.jpg")));
            } catch (IOException fallbackEx) {
                System.err.println("Impossible de charger l'image par défaut");
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
            MainMenu mainMenu = new MainMenu(audio, repo);
            mainMenu.setVisible(true);
            mainMenu.toBack();
            new javax.swing.Timer(500, t1 -> {((javax.swing.Timer)t1.getSource()).stop(); mainMenu.toFront();}).start();
            new javax.swing.Timer(500, t2 -> {((javax.swing.Timer)t2.getSource()).stop(); this.dispose();}).start();

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
}
