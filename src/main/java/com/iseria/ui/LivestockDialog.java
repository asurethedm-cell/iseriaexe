package com.iseria.ui;

import com.iseria.domain.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;


public class LivestockDialog extends JDialog {
    private SafeHexDetails hex;
    private DATABASE.LivestockData selectedAnimalType;
    private JSpinner animalCountSpinner;
    private JSpinner feedingSpinner;
    private JTextArea detailsArea;
    private JComboBox<DATABASE.LivestockData> animalTypeCombo;
    private boolean confirmed = false;
    IHexRepository repo;
    public LivestockDialog(JFrame parent, SafeHexDetails hex) {
        super(parent, "Gestion de l'Élevage - " + hex.getHexKey(), true);
        this.hex = hex;
        initializeDialog();
    }

    private void initializeDialog() {
        setSize(900, 800);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel titleLabel = new JLabel("Élevage - Hexagone: " + hex.getHexKey());
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);

        gbc.gridwidth = 1; gbc.gridy++;
        JLabel animalLabel = new JLabel("Type d'Animal:");
        animalLabel.setFont(new Font("Arial", Font.BOLD, 12));
        gbc.gridx = 0;
        mainPanel.add(animalLabel, gbc);

        animalTypeCombo = new JComboBox<>(DATABASE.LivestockData.values());
        animalTypeCombo.addActionListener(this::onAnimalTypeChanged);
        gbc.gridx = 1;
        mainPanel.add(animalTypeCombo, gbc);

        gbc.gridy++; gbc.gridx = 0;
        JLabel countLabel = new JLabel("Nombre d'animaux:");
        mainPanel.add(countLabel, gbc);

        animalCountSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        animalCountSpinner.addChangeListener(e -> updateDetails());
        gbc.gridx = 1;
        mainPanel.add(animalCountSpinner, gbc);

        gbc.gridy++; gbc.gridx = 0;
        JLabel feedLabel = new JLabel("Nourriture/semaine (SdN):");
        mainPanel.add(feedLabel, gbc);

        feedingSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 100.0, 0.1));
        feedingSpinner.addChangeListener(e -> updateDetails());
        gbc.gridx = 1;
        mainPanel.add(feedingSpinner, gbc);

        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0; gbc.weighty = 1.0;

        detailsArea = new JTextArea(15, 60);
        detailsArea.setEditable(false);
        detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane scrollPane = new JScrollPane(detailsArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Détails de l'Élevage"));
        mainPanel.add(scrollPane, gbc);

        gbc.gridy++; gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0; gbc.weighty = 0;
        JPanel buttonPanel = new JPanel(new FlowLayout());

        JButton confirmButton = new JButton("Établir l'Élevage");
        confirmButton.setBackground(new Color(76, 175, 80));
        confirmButton.setForeground(Color.WHITE);
        confirmButton.addActionListener(this::confirm);

        JButton cancelButton = new JButton("Annuler");
        cancelButton.setBackground(new Color(244, 67, 54));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);
        mainPanel.add(buttonPanel, gbc);

        add(mainPanel);
        updateDetails();
    }

    private void onAnimalTypeChanged(ActionEvent e) {
        selectedAnimalType = (DATABASE.LivestockData) animalTypeCombo.getSelectedItem();
        if (selectedAnimalType != null) {
            SpinnerNumberModel model = (SpinnerNumberModel) feedingSpinner.getModel();
            model.setMinimum(selectedAnimalType.getBesoinMin());
            model.setMaximum(selectedAnimalType.getBesoinMax());
            model.setValue(selectedAnimalType.getBesoinMin());
        }
        updateDetails();
    }

    private void updateDetails() {
        if (selectedAnimalType == null) return;

        StringBuilder details = new StringBuilder();
        int nombreAnimaux = (Integer) animalCountSpinner.getValue();
        double nourritureParSemaine = (Double) feedingSpinner.getValue();

        details.append("=== INFORMATIONS GÉNÉRALES ===\n");
        details.append("Animal: ").append(selectedAnimalType.getName()).append("\n");
        details.append("Condition: ").append(selectedAnimalType.getCondition()).append("\n");
        details.append("Prix unitaire: ").append(selectedAnimalType.getPrixUnite()).append(" Po\n");
        details.append("Temps de croissance: ").append(selectedAnimalType.getToursPourMaturite()).append(" semaines\n");

        details.append("\n=== COÛTS INITIAUX ===\n");
        double coutAnimaux = nombreAnimaux * selectedAnimalType.getPrixUnite();
        int reproducteurs = (nombreAnimaux + 9) / 10;
        double coutReproducteurs = reproducteurs * selectedAnimalType.getCoutReproducteur();
        double coutTotal = coutAnimaux + coutReproducteurs;

        details.append("Coût des animaux: ").append(String.format("%.2f Po", coutAnimaux)).append("\n");
        details.append("Reproducteurs nécessaires: ").append(reproducteurs).append("\n");
        details.append("Coût des reproducteurs: ").append(String.format("%.2f Po", coutReproducteurs)).append("\n");
        details.append("COÛT TOTAL: ").append(String.format("%.2f Po", coutTotal)).append("\n");

        details.append("\n=== ALIMENTATION ===\n");
        details.append("Nourriture par animal/semaine: ").append(String.format("%.2f SdN", nourritureParSemaine)).append("\n");
        details.append("Nourriture totale/semaine: ").append(String.format("%.2f SdN", nourritureParSemaine * nombreAnimaux)).append("\n");
        details.append("Coût alimentaire/semaine: ").append(String.format("%.2f Po", nourritureParSemaine * nombreAnimaux * 5.1)).append("\n");

        details.append("\n=== PERSONNEL REQUIS ===\n");
        int bergersNecessaires = (nombreAnimaux + selectedAnimalType.getAnimauxParBerger() - 1) / selectedAnimalType.getAnimauxParBerger();
        details.append("Bergers nécessaires: ").append(bergersNecessaires).append("\n");
        if (selectedAnimalType.isAttirePredateurs()) {
            details.append("Chiens de garde recommandés: ").append(bergersNecessaires).append("\n");
        }

        details.append("\n=== PRODUCTION ESTIMÉE ===\n");
        if (selectedAnimalType.getToursPourMaturite() > 0) {
            details.append("Temps avant production: ").append(selectedAnimalType.getToursPourMaturite()).append(" semaines\n");

            // Estimation de production basée sur l'investissement
            double ratioInvestissement = nourritureParSemaine / selectedAnimalType.getBesoinMax();
            double productionEstimee = nombreAnimaux * selectedAnimalType.getPrixUnite() * ratioInvestissement * 0.3;

            details.append("Production estimée/semaine: ").append(String.format("%.2f Po", productionEstimee)).append("\n");
            details.append("Retour sur investissement: ");
            if (productionEstimee > 0) {
                int semainesRetour = (int) (coutTotal / productionEstimee);
                details.append(semainesRetour).append(" semaines\n");
            } else {
                details.append("N/A\n");
            }
        }

        // Vérification des conditions
        details.append("\n=== VÉRIFICATIONS ===\n");
        if (selectedAnimalType.canEstablishIn(hex, repo)) {
            details.append("Conditions remplies pour cet hexagone\n");
        } else {
            details.append("Conditions non remplies: ").append(selectedAnimalType.getCondition()).append("\n");
        }

        if (hex.getLivestockFarm().getTotalAnimaux() + nombreAnimaux <= hex.getLivestockFarm().getCapaciteMaximale()) {
            details.append("Capacité suffisante\n");
        } else {
            details.append("Capacité insuffisante (max: ").append(hex.getLivestockFarm().getCapaciteMaximale()).append(")\n");
        }

        detailsArea.setText(details.toString());
    }

    private void confirm(ActionEvent e) {
        if (selectedAnimalType == null) return;

        int nombreAnimaux = (Integer) animalCountSpinner.getValue();
        double nourritureParSemaine = (Double) feedingSpinner.getValue();

        if (!selectedAnimalType.canEstablishIn(hex, repo)) {
            JOptionPane.showMessageDialog(this,
                    "Impossible d'établir cet élevage ici: " + selectedAnimalType.getCondition(),
                    "Conditions non remplies", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (hex.getLivestockFarm().ajouterCheptel(selectedAnimalType, nombreAnimaux, nourritureParSemaine)) {
            confirmed = true;
            JOptionPane.showMessageDialog(this,
                    "Élevage établi avec succès!",
                    "Succès", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Impossible d'ajouter cet élevage (capacité insuffisante)",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isConfirmed() { return confirmed; }
}
