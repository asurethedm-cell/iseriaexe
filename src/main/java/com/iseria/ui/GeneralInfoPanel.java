package com.iseria.ui;

import javax.swing.*;
import java.awt.*;

public class GeneralInfoPanel {
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


}
