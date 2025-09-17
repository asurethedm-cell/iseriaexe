package com.iseria.ui;

import com.iseria.domain.*;
import com.iseria.service.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class PopulationManagementPanel extends JPanel implements PersonnelDataService.PersonnelObserver {

    private final PersonnelDataService personnelService;
    private final EconomicDataService economicService;
    private final String currentFaction;

    private JTable populationTable;
    private DefaultTableModel tableModel;
    private JLabel totalPopulationLabel;
    private JLabel totalSalaryLabel;
    private JLabel totalFoodLabel;

    public PopulationManagementPanel(PersonnelDataService personnelService,
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
        setLayout(new BorderLayout());
        setBackground(new Color(50, 50, 50, 200));

        // **HEADER AVEC STATISTIQUES**
        add(createStatsHeader(), BorderLayout.NORTH);

        // **TABLE PRINCIPALE**
        add(createPopulationTable(), BorderLayout.CENTER);

        // **PANEL DE CONTRÔLES**
        add(createControlPanel(), BorderLayout.SOUTH);
    }

    private JPanel createStatsHeader() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("📊 Statistiques Globales"));
        panel.setBackground(new Color(60, 60, 60, 200));

        totalPopulationLabel = new JLabel("Population: 0", SwingConstants.CENTER);
        totalPopulationLabel.setFont(new Font("Arial", Font.BOLD, 14));
        totalPopulationLabel.setForeground(Color.WHITE);

        totalSalaryLabel = new JLabel("Salaires: 0 Po/sem", SwingConstants.CENTER);
        totalSalaryLabel.setFont(new Font("Arial", Font.BOLD, 14));
        totalSalaryLabel.setForeground(Color.CYAN);

        totalFoodLabel = new JLabel("Nourriture: 0/sem", SwingConstants.CENTER);
        totalFoodLabel.setFont(new Font("Arial", Font.BOLD, 14));
        totalFoodLabel.setForeground(Color.ORANGE);

        JLabel efficiencyLabel = new JLabel("Efficacité: 100%", SwingConstants.CENTER);
        efficiencyLabel.setFont(new Font("Arial", Font.BOLD, 14));
        efficiencyLabel.setForeground(Color.GREEN);

        panel.add(totalPopulationLabel);
        panel.add(totalSalaryLabel);
        panel.add(totalFoodLabel);
        panel.add(efficiencyLabel);

        return panel;
    }

    private JScrollPane createPopulationTable() {
        // **MODIFICATION:** Add "Personnel" as first hidden column
        String[] columns = {"Personnel", "Nom", "Métier", "Salaire", "Consommation", "Affectation", "Localisation", "Gérer"};

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7; // Only "Gérer" column is editable
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return PersonnelDataService.HiredPersonnel.class; // Hidden
                if (columnIndex == 3 || columnIndex == 4) return Double.class;
                if (columnIndex == 7) return JButton.class;
                return String.class;
            }
        };

        populationTable = new JTable(tableModel);

        // **HIDE the Personnel column**
        populationTable.getColumnModel().getColumn(0).setMinWidth(0);
        populationTable.getColumnModel().getColumn(0).setMaxWidth(0);
        populationTable.getColumnModel().getColumn(0).setWidth(0);

        // Set other column widths...
        populationTable.getColumn("Nom").setPreferredWidth(120);
        populationTable.getColumn("Métier").setPreferredWidth(100);
        populationTable.getColumn("Salaire").setPreferredWidth(80);
        populationTable.getColumn("Consommation").setPreferredWidth(80);
        populationTable.getColumn("Affectation").setPreferredWidth(120);
        populationTable.getColumn("Localisation").setPreferredWidth(100);
        populationTable.getColumn("Gérer").setPreferredWidth(100);

        // Renderer pour la colonne "Gérer"
        populationTable.getColumn("Gérer").setCellRenderer(new ButtonRenderer());
        populationTable.getColumn("Gérer").setCellEditor(new ManageButtonEditor());

        JScrollPane scrollPane = new JScrollPane(populationTable);
        scrollPane.setPreferredSize(new Dimension(900, 400));

        return scrollPane;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        panel.setBackground(new Color(60, 60, 60, 200));

        JButton refreshButton = new JButton("🔄 Actualiser");
        refreshButton.addActionListener(e -> refreshData());
        panel.add(refreshButton);

        JButton exportButton = new JButton("📊 Exporter");
        exportButton.addActionListener(e -> exportPopulationData());
        panel.add(exportButton);

        JButton massFireButton = new JButton("⚠️ Licenciement Massif");
        massFireButton.setBackground(new Color(220, 53, 69));
        massFireButton.setForeground(Color.WHITE);
        massFireButton.addActionListener(e -> showMassFireDialog());
        panel.add(massFireButton);

        return panel;
    }

    private void refreshData() {
        tableModel.setRowCount(0);
        List<PersonnelDataService.HiredPersonnel> allPersonnel =
                personnelService.getFactionPersonnel(currentFaction);

        double totalSalary = 0;
        double totalFood = 0;
        int totalCount = 0;

        for (PersonnelDataService.HiredPersonnel personnel : allPersonnel) {
            String affectation = personnel.isAssigned ? personnel.assignedBuilding : "Non assigné";
            String localisation = personnel.isAssigned ? personnel.assignedHex : "-";

            // **MODIFICATION:** Store the HiredPersonnel object in column 0 (hidden)
            tableModel.addRow(new Object[]{
                    personnel,                          // Hidden: HiredPersonnel object
                    personnel.name,                     // Column 1: Name
                    personnel.workerType.getJobName(),  // Column 2: Job
                    personnel.currentSalary,            // Column 3: Salary
                    personnel.foodConsumption,          // Column 4: Food
                    affectation,                        // Column 5: Assignment
                    localisation,                       // Column 6: Location
                    "Gérer"                            // Column 7: Button
            });

            totalSalary += personnel.currentSalary;
            totalFood += personnel.foodConsumption;
            totalCount++;
        }

        updateStatsLabels(totalCount, totalSalary, totalFood);
    }

    private void updateStatsLabels(int totalCount, double totalSalary, double totalFood) {
        totalPopulationLabel.setText("Population: " + totalCount);
        totalSalaryLabel.setText(String.format("Salaires: %.1f Po/sem", totalSalary));
        totalFoodLabel.setText(String.format("Nourriture: %.1f/sem", totalFood));
    }

    private void exportPopulationData() {
        // TODO: Implémenter l'export CSV/Excel
        JOptionPane.showMessageDialog(this,
                "Export des données population (à implémenter)",
                "Export", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showMassFireDialog() {
        // Dialog pour licenciement massif avec filtres
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Licenciement Massif", true);
        // TODO: Implémenter interface de licenciement massif
        dialog.setVisible(true);
    }

    // **CLASSES INTERNES**
    private class ButtonRenderer extends JButton implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            setText("Gérer");
            return this;
        }
    }

    private class ManageButtonEditor extends DefaultCellEditor {
        private JButton button;
        private boolean isPushed;
        private int editingRow;

        public ManageButtonEditor() {
            super(new JCheckBox());
            button = new JButton("Gérer");
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            isPushed = true;
            editingRow = row;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed && editingRow >= 0) {
                showPersonnelManagementDialog(editingRow);
            }
            isPushed = false;
            return "Gérer";
        }
    }

    private void showPersonnelManagementDialog(int row) {
        String personnelName = (String) tableModel.getValueAt(row, 1);
        String[] options = {"Réassigner", "Licencier", "Annuler"};

        int choice = JOptionPane.showOptionDialog(this,
                "Que voulez-vous faire avec " + personnelName + " ?",
                "Gestion du Personnel",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[2]);

        switch (choice) {
            case 0: // Réassigner
                showReassignmentDialog(row);
                break;
            case 1: // Licencier
                confirmAndFirePersonnel(row);
                break;
            // case 2: Annuler - ne rien faire
        }
    }

    private void showReassignmentDialog(int row) {
        // TODO: Implémenter dialog de réassignation
        JOptionPane.showMessageDialog(this, "Dialog de réassignation (à implémenter)");
    }

    private void confirmAndFirePersonnel(int row) {
        // **GET the HiredPersonnel object from hidden column 0**
        PersonnelDataService.HiredPersonnel personnel =
                (PersonnelDataService.HiredPersonnel) tableModel.getValueAt(row, 0);

        String personnelName = personnel.name;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Êtes-vous sûr de vouloir licencier " + personnelName + " ?",
                "Confirmation de Licenciement",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            personnelService.firePersonnel(personnel.personnelId);
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