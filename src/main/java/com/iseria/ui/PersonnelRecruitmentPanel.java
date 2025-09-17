package com.iseria.ui;

import com.iseria.domain.*;
import com.iseria.service.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class PersonnelRecruitmentPanel extends JPanel implements PersonnelDataService.PersonnelObserver {

    private final PersonnelDataService personnelService;
    private final EconomicDataService economicService;
    private final String currentFaction;
    public static boolean noFunds = false;

    private JTable availableWorkersTable;
    private DefaultTableModel availableTableModel;
    private JTable unassignedPersonnelTable;
    private DefaultTableModel unassignedTableModel;

    public PersonnelRecruitmentPanel(PersonnelDataService personnelService,
                                     EconomicDataService economicService,
                                     String currentFaction) {
        this.personnelService = personnelService;
        this.economicService = economicService;
        this.currentFaction = currentFaction;

        personnelService.addObserver(this);
        initializePanel();
        refreshData();
    }

    private void initializePanel() {
        setLayout(new GridBagLayout());
        setBackground(new Color(50, 50, 50, 200));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;

        // **SECTION RECRUTEMENT**
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1.0; gbc.weighty = 0.4;
        add(createRecruitmentSection(), gbc);
    }

    private JPanel createRecruitmentSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("👥 Recrutement de Personnel"));
        panel.setBackground(new Color(60, 60, 60, 200));

        // Table des workers disponibles
        String[] columns = {"Métier", "Catégorie", "Salaire", "Nourriture", "Bâtiments", "Recruter"};
        availableTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Seule la colonne "Recruter" est éditable
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 5 ? JButton.class : String.class;
            }
        };

        availableWorkersTable = new JTable(availableTableModel);
        availableWorkersTable.setRowHeight(35);
        availableWorkersTable.getColumn("Recruter").setCellRenderer(new ButtonRenderer());
        availableWorkersTable.getColumn("Recruter").setCellEditor(new RecruitButtonEditor());

        JScrollPane scrollPane = new JScrollPane(availableWorkersTable);
        scrollPane.setPreferredSize(new Dimension(800, 200));

        panel.add(scrollPane, BorderLayout.CENTER);

        // Panel de contrôle
        panel.add(createRecruitmentControlPanel(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createRecruitmentControlPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        panel.setBackground(new Color(60, 60, 60, 200));

        JLabel budgetLabel = new JLabel("Budget disponible: " +
                String.format("%.0f Po", economicService.getEconomicData().tresorerie));
        budgetLabel.setForeground(Color.WHITE);
        panel.add(budgetLabel);

        panel.add(Box.createHorizontalStrut(20));

        JButton refreshButton = new JButton("🔄 Actualiser");
        refreshButton.addActionListener(e -> refreshData());
        panel.add(refreshButton);

        return panel;
    }

    private void refreshData() {

        availableTableModel.setRowCount(0);
        List<DATABASE.Workers> availableWorkers = personnelService.getAvailableWorkersForRecruitment(currentFaction);

        for (DATABASE.Workers worker : availableWorkers) {
            String buildingNames = worker.getWorkBuildings().stream()
                    .map(DATABASE.JobBuilding::getBuildName)
                    .limit(2)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("Aucun");

            availableTableModel.addRow(new Object[]{
                    worker.getJobName(),
                    worker.getCategory(),
                    String.format("%.1f Po", worker.getCurrentSalary()),
                    String.format("%.1f", worker.getCurrentFoodConsumption()),
                    buildingNames,
                    "Recruter"
            });
        }
    }

    private static class ButtonRenderer extends JButton implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    private class RecruitButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;
        private boolean isPushed;
        private int editingRow;

        public RecruitButtonEditor() {
            super(new JCheckBox());
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            isPushed = true;
            editingRow = row;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed && editingRow >= 0) {
                // Récupérer le worker sélectionné et déclencher le recrutement
                List<DATABASE.Workers> availableWorkers =
                        personnelService.getAvailableWorkersForRecruitment(currentFaction);

                if (editingRow < availableWorkers.size()) {
                    DATABASE.Workers selectedWorker = availableWorkers.get(editingRow);

                    // Dialog pour choisir la quantité
                    String quantityStr = JOptionPane.showInputDialog(
                            PersonnelRecruitmentPanel.this,
                            "Combien de " + selectedWorker.getJobName() + " recruter ?",
                            "Recrutement",
                            JOptionPane.QUESTION_MESSAGE
                    );
                    try {
                        int quantity = Integer.parseInt(quantityStr);
                        if (quantity > 0) {
                            personnelService.hirePersonnel(currentFaction, selectedWorker, quantity);
                            if(noFunds){
                                JOptionPane.showMessageDialog(PersonnelRecruitmentPanel.this,
                                "Budget MENSUEL INSUFFISANT pour recruter " + quantity + " " + selectedWorker.getJobName(),
                                "Manque Trésorerie", JOptionPane.WARNING_MESSAGE);
                            }
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(PersonnelRecruitmentPanel.this,
                                "Quantité invalide", "Erreur", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
            isPushed = false;
            return label;
        }
    }

    // **IMPLÉMENTATION DES OBSERVATEURS**
    @Override
    public void onPersonnelHired(PersonnelDataService.HiredPersonnel personnel) {
        SwingUtilities.invokeLater(this::refreshData);
    }

    @Override
    public void onPersonnelFired(String personnelId) {
        SwingUtilities.invokeLater(this::refreshData);
    }

    @Override
    public void onPersonnelAssigned(String personnelId, String hexKey, String buildingType) {
        SwingUtilities.invokeLater(this::refreshData);
    }
}