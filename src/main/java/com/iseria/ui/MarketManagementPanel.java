package com.iseria.ui;

import com.iseria.domain.*;
import com.iseria.service.*;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class MarketManagementPanel extends JPanel {
    private final EconomicDataService economicService;
    private final PersonnelDataService personnelService;
    private final IHexRepository hexRepository;
    private final String currentFaction;

    private JTabbedPane tabbedPane;
    private TradeMarketPanel tradePanel;
    private PersonnelRecruitmentPanel recruitmentPanel;
    private PopulationManagementPanel populationPanel;

    public MarketManagementPanel(EconomicDataService economicService,
                                 PersonnelDataService personnelService,
                                 IHexRepository hexRepository,
                                 String currentFaction) {
        this.economicService = economicService;
        this.personnelService = personnelService;
        this.hexRepository = hexRepository;
        this.currentFaction = currentFaction;

        initializePanel();
    }

    private void initializePanel() {
        setLayout(new BorderLayout());


        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(50, 50, 50, 200));
        tabbedPane.setTabPlacement(JTabbedPane.TOP);

        // Onglet 1: Market principal (existant)
        tradePanel = new TradeMarketPanel(economicService, hexRepository, currentFaction);
        tabbedPane.addTab("🏪 Commerce", tradePanel);

        // Onglet 2: Recrutement de personnel
        recruitmentPanel = new PersonnelRecruitmentPanel(personnelService, economicService, currentFaction);
        tabbedPane.addTab("👥 Recrutement", recruitmentPanel);

        // Onglet 3: Gestion population totale
        populationPanel = new PopulationManagementPanel(personnelService, economicService, currentFaction);
        tabbedPane.addTab("📊 Population", populationPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // Panel d'information globale
        add(createGlobalInfoPanel(), BorderLayout.SOUTH);
    }

    private JPanel createGlobalInfoPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        panel.setBackground(new Color(40, 40, 40, 200));

        // Affichage rapide des stats globales
        JLabel populationTotal = new JLabel("Population: 0");
        JLabel budgetDisponible = new JLabel("Budget: 0 Po");
        JLabel maintenanceTotal = new JLabel("Maintenance: 0 Po/sem");

        panel.add(populationTotal);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(budgetDisponible);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(maintenanceTotal);

        return panel;
    }
}