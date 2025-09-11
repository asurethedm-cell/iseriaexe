package com.iseria.ui;


import com.iseria.domain.*;
import com.iseria.infra.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

import static com.iseria.ui.MainMenu.getCurrentFactionId;


public class Login extends JFrame implements ActionListener {
    private final IDataProvider data;
    private final IAudioService audio;
    private final IHexRepository repo;
    JButton SUBMIT;
    JPanel panel;
    JLabel label1, label2;
    final JTextField text1;
    final JPasswordField text2;
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
    String emblemPath = FactionRegistry.getEmblemPathFor(getCurrentFactionId());
    public Login(IDataProvider data, IAudioService audio, IHexRepository repo) {
        this.data = data;
        this.audio = audio;
        this.repo = repo;


        waitNextTurn = false;
        System.out.println("Turn paused : " + waitNextTurn);
        users.put("Admin", "admin");
        users.put("Bladjorn", "orange");
        users.put("Klowh", "poing");
        users.put("t", "t");

        label1 = new JLabel("Username:");
        text1 = new JTextField(15);
        label2 = new JLabel("Password:");
        text2 = new JPasswordField(15);
        SUBMIT = new JButton("SUBMIT");

        panel = new JPanel(new GridLayout(3, 1));

        ImageIcon Icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/Icon.png")));
        this.setIconImage(Icon.getImage());
        if (Icon.getImageLoadStatus() == MediaTracker.ERRORED) {
            System.out.println("Erreur lors du chargement de l'icône.");
        }
        panel.add(label1);
        panel.add(text1);
        panel.add(label2);
        panel.add(text2);
        panel.add(SUBMIT);

        add(panel, BorderLayout.CENTER);
        SUBMIT.addActionListener(this);
        text2.addActionListener(e -> SUBMIT.doClick());
        setTitle("Iseria Logger");

        this.setSize(350, 150);


        int screenWidth = this.getToolkit().getScreenSize().width;
        int screenHeight = this.getToolkit().getScreenSize().height;
        int windowWidth = this.getWidth();
        int windowHeight = this.getHeight();
        int x = (screenWidth - windowWidth) / 2;
        int y = (screenHeight - windowHeight) / 2;
        this.setLocation(x, y);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);
        this.setVisible(true);
    }


    public void actionPerformed(ActionEvent e) {

        String username = text1.getText();
        String password = new String(text2.getPassword());

        if (users.containsKey(username) && users.get(username).equals(password)) {
            if (users.containsKey(username) && users.get(username).equals(password)) {
                currentUser = username;
                MainMenu mainMenu = new MainMenu(data, audio, repo); // Pass dependencies
                mainMenu.setVisible(true);
                this.dispose();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Incorrect login or password", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }





}


