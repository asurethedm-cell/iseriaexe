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
        add(createStatsHeader(), BorderLayout.NORTH);
        add(createPopulationTable(), BorderLayout.CENTER);
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
        String[] columns = {"Personnel", "Nom", "Métier", "Salaire", "Consommation", "Affectation", "Localisation", "Gérer"};

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7;
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

        populationTable.getColumnModel().getColumn(0).setMinWidth(0);
        populationTable.getColumnModel().getColumn(0).setMaxWidth(0);
        populationTable.getColumnModel().getColumn(0).setWidth(0);

        populationTable.getColumn("Nom").setPreferredWidth(120);
        populationTable.getColumn("Métier").setPreferredWidth(100);
        populationTable.getColumn("Salaire").setPreferredWidth(80);
        populationTable.getColumn("Consommation").setPreferredWidth(80);
        populationTable.getColumn("Affectation").setPreferredWidth(120);
        populationTable.getColumn("Localisation").setPreferredWidth(100);
        populationTable.getColumn("Gérer").setPreferredWidth(100);

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
            tableModel.addRow(new Object[]{
                    personnel,                          // Hidden: HiredPersonnel object
                    personnel.name,                     // Column 1: Name
                    personnel.workerType.getJobName(),  // Column 2: Job
                    personnel.currentSalary,            // Column 3: Salary
                    personnel.foodConsumption,          // Column 4: Food
                    affectation,                        // Column 5: Assignment
                    localisation,                       // Column 6: Location
                    "Gérer"                             // Column 7: Button
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
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Licenciement Massif", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);

        JPanel filterPanel = createMassFireFilterPanel();

        DefaultTableModel massFireTableModel = new DefaultTableModel(
                new String[]{"Sélectionner", "Nom", "Métier", "Salaire", "Consommation", "Affectation"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };

        JTable massFireTable = new JTable(massFireTableModel);
        massFireTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        massFireTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        massFireTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        massFireTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        massFireTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        massFireTable.getColumnModel().getColumn(5).setPreferredWidth(120);

        JScrollPane scrollPane = new JScrollPane(massFireTable);
        scrollPane.setPreferredSize(new Dimension(750, 300));

        JLabel selectedCountLabel = new JLabel("Sélectionnés: 0");
        JLabel totalSavingsLabel = new JLabel("Économies: 0 Pos/sem");

        JPanel statsPanel = new JPanel(new FlowLayout());
        statsPanel.add(selectedCountLabel);
        statsPanel.add(new JLabel(" | "));
        statsPanel.add(totalSavingsLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout());

        JButton selectAllButton = new JButton("Tout Sélectionner");
        JButton selectNoneButton = new JButton("Tout Désélectionner");
        JButton applyFiltersButton = new JButton("Appliquer Filtres");
        JButton fireSelectedButton = new JButton("Licencier Sélectionnés");
        JButton cancelButton = new JButton("Annuler");

        fireSelectedButton.setBackground(new Color(220, 53, 69));
        fireSelectedButton.setForeground(Color.WHITE);
        applyFiltersButton.setBackground(new Color(0, 123, 255));
        applyFiltersButton.setForeground(Color.WHITE);

        buttonPanel.add(selectAllButton);
        buttonPanel.add(selectNoneButton);
        buttonPanel.add(applyFiltersButton);
        buttonPanel.add(fireSelectedButton);
        buttonPanel.add(cancelButton);

        dialog.add(filterPanel, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(statsPanel, BorderLayout.NORTH);
        southPanel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.add(southPanel, BorderLayout.SOUTH);

        setupMassFireActions(dialog, massFireTableModel, massFireTable,
                selectedCountLabel, totalSavingsLabel,
                selectAllButton, selectNoneButton, applyFiltersButton,
                fireSelectedButton, cancelButton, filterPanel);

        applyMassFireFilters(massFireTableModel, filterPanel);

        dialog.setVisible(true);
    }

    private JPanel createMassFireFilterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Filtres de Licenciement"));
        panel.setBackground(new Color(60, 60, 60, 200));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Métier:"), gbc);

        JComboBox<String> jobFilterCombo = new JComboBox<>();
        jobFilterCombo.setName("jobFilter");
        jobFilterCombo.addItem("Tous");

        List<PersonnelDataService.HiredPersonnel> allPersonnel =
                personnelService.getFactionPersonnel(currentFaction);
        Set<String> uniqueJobs = allPersonnel.stream()
                .map(p -> p.workerType.getJobName())
                .collect(Collectors.toSet());
        uniqueJobs.forEach(jobFilterCombo::addItem);

        gbc.gridx = 1;
        panel.add(jobFilterCombo, gbc);

        gbc.gridx = 2; gbc.gridy = 0;
        panel.add(new JLabel("Affectation:"), gbc);

        JComboBox<String> assignmentFilterCombo = new JComboBox<>();
        assignmentFilterCombo.setName("assignmentFilter");
        assignmentFilterCombo.addItem("Tous");
        assignmentFilterCombo.addItem("Non Assignés");
        assignmentFilterCombo.addItem("Assignés");

        gbc.gridx = 3;
        panel.add(assignmentFilterCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Salaire Min:"), gbc);

        JSpinner minSalarySpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 999.0, 0.1));
        minSalarySpinner.setName("minSalary");
        gbc.gridx = 1;
        panel.add(minSalarySpinner, gbc);

        gbc.gridx = 2;
        panel.add(new JLabel("Salaire Max:"), gbc);

        JSpinner maxSalarySpinner = new JSpinner(new SpinnerNumberModel(999.0, 0.0, 999.0, 0.1));
        maxSalarySpinner.setName("maxSalary");
        gbc.gridx = 3;
        panel.add(maxSalarySpinner, gbc);

        return panel;
    }

    private void setupMassFireActions(JDialog dialog, DefaultTableModel tableModel, JTable table,
                                      JLabel selectedCountLabel, JLabel totalSavingsLabel,
                                      JButton selectAllButton, JButton selectNoneButton,
                                      JButton applyFiltersButton, JButton fireSelectedButton,
                                      JButton cancelButton, JPanel filterPanel) {
        Runnable updateStats = () -> {
            int selectedCount = 0;
            double totalSavings = 0.0;
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                Boolean selected = (Boolean) tableModel.getValueAt(i, 0);
                if (selected != null && selected) {
                    selectedCount++;
                    String salaryStr = (String) tableModel.getValueAt(i, 3);
                    try {
                        totalSavings += Double.parseDouble(salaryStr);
                    } catch (NumberFormatException e) {
                    }
                }
            }

            selectedCountLabel.setText("Sélectionnés: " + selectedCount);
            totalSavingsLabel.setText(String.format("Économies: %.1f Pos/sem", totalSavings));
            fireSelectedButton.setEnabled(selectedCount > 0);
        };
        tableModel.addTableModelListener(e -> updateStats.run());
        selectAllButton.addActionListener(e -> {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                tableModel.setValueAt(true, i, 0);
            }
        });
        selectNoneButton.addActionListener(e -> {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                tableModel.setValueAt(false, i, 0);
            }
        });
        applyFiltersButton.addActionListener(e -> applyMassFireFilters(tableModel, filterPanel));
        fireSelectedButton.addActionListener(e -> executeMassFiring(dialog, tableModel));
        cancelButton.addActionListener(e -> dialog.dispose());
        fireSelectedButton.setEnabled(false);
    }

    private void applyMassFireFilters(DefaultTableModel tableModel, JPanel filterPanel) {
        JComboBox<String> jobFilter = (JComboBox<String>) findComponentByName(filterPanel, "jobFilter");
        JComboBox<String> assignmentFilter = (JComboBox<String>) findComponentByName(filterPanel, "assignmentFilter");
        JSpinner minSalarySpinner = (JSpinner) findComponentByName(filterPanel, "minSalary");
        JSpinner maxSalarySpinner = (JSpinner) findComponentByName(filterPanel, "maxSalary");

        String selectedJob = (String) jobFilter.getSelectedItem();
        String selectedAssignment = (String) assignmentFilter.getSelectedItem();
        Double minSalary = (Double) minSalarySpinner.getValue();
        Double maxSalary = (Double) maxSalarySpinner.getValue();

        tableModel.setRowCount(0);
        List<PersonnelDataService.HiredPersonnel> allPersonnel =
                personnelService.getFactionPersonnel(currentFaction);

        for (PersonnelDataService.HiredPersonnel personnel : allPersonnel) {
            if (!"Tous".equals(selectedJob) && !personnel.workerType.getJobName().equals(selectedJob)) {
                continue;
            }
            if (!"Tous".equals(selectedAssignment)) {
                if ("Non Assignés".equals(selectedAssignment) && personnel.isAssigned) continue;
                if ("Assignés".equals(selectedAssignment) && !personnel.isAssigned) continue;
            }
            if (personnel.currentSalary < minSalary || personnel.currentSalary > maxSalary) {
                continue;
            }
            String affectation = personnel.isAssigned ? personnel.assignedBuilding : "Non assigné";
            tableModel.addRow(new Object[]{
                    false, // Non sélectionné par défaut
                    personnel.name,
                    personnel.workerType.getJobName(),
                    String.format("%.1f", personnel.currentSalary),
                    String.format("%.1f", personnel.foodConsumption),
                    affectation
            });
        }
    }

    private void executeMassFiring(JDialog dialog, DefaultTableModel tableModel) {
        List<String> selectedNames = new ArrayList<>();
        List<PersonnelDataService.HiredPersonnel> selectedPersonnel = new ArrayList<>();

        List<PersonnelDataService.HiredPersonnel> allPersonnel =
                personnelService.getFactionPersonnel(currentFaction);

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Boolean selected = (Boolean) tableModel.getValueAt(i, 0);
            if (selected != null && selected) {
                String name = (String) tableModel.getValueAt(i, 1);
                selectedNames.add(name);
                allPersonnel.stream()
                        .filter(p -> p.name.equals(name))
                        .findFirst()
                        .ifPresent(selectedPersonnel::add);
            }
        }
        if (selectedPersonnel.isEmpty()) {
            JOptionPane.showMessageDialog(dialog,
                    "Aucun employé sélectionné.",
                    "Erreur", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String message = String.format(
                "Êtes-vous sûr de vouloir licencier %d employé(s) ?\n\n%s",
                selectedPersonnel.size(),
                String.join(", ", selectedNames.subList(0, Math.min(5, selectedNames.size()))) +
                        (selectedNames.size() > 5 ? "..." : "")
        );

        int confirm = JOptionPane.showConfirmDialog(dialog, message,
                "Confirmation de Licenciement Massif",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            int successCount = 0;
            for (PersonnelDataService.HiredPersonnel personnel : selectedPersonnel) {
                try {
                    personnelService.firePersonnel(personnel.personnelId);
                    successCount++;
                } catch (Exception e) {
                    System.err.println("Erreur lors du licenciement de " + personnel.name + ": " + e.getMessage());
                }
            }
            JOptionPane.showMessageDialog(dialog,
                    String.format("%d employé(s) licencié(s) avec succès.", successCount),
                    "Licenciement Terminé",
                    JOptionPane.INFORMATION_MESSAGE);

            dialog.dispose();
        }
    }
    private Component findComponentByName(Container parent, String name) {
        for (Component component : parent.getComponents()) {
            if (name.equals(component.getName())) {
                return component;
            }
            if (component instanceof Container) {
                Component found = findComponentByName((Container) component, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
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