package com.iseria.ui;

import com.iseria.domain.*;
import com.iseria.service.*;

import javax.swing.SwingUtilities;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.*;

public class TradeMarketPanel extends JPanel {

    javax.swing.Timer refreshTimer;


    public TradeMarketPanel(EconomicDataService economicService,
                            IHexRepository hexRepository,
                            String currentFaction) {

        initializePanel();
        startMarketRefreshTimer();
    }

    private void initializePanel() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(new Color(50, 50, 50, 200));

        // Panel de titre
        JPanel titlePanel = createTitlePanel();
        add(titlePanel, BorderLayout.NORTH);

        // Contenu principal
        JPanel contentPanel = createContentPanel();
        add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel createTitlePanel() {
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(true);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        JLabel titleLabel = new JLabel("🏪 Live Market Data", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel, BorderLayout.CENTER);

        JLabel summaryLabel = createMarketSummaryLabel();
        titlePanel.add(summaryLabel, BorderLayout.SOUTH);

        return titlePanel;
    }

    private JPanel createContentPanel() {
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(true);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.BOTH;

        // Section des ressources tendance
        JPanel trendingPanel = createTrendingResourcesSection();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1.0; gbc.weighty = 0.3;
        contentPanel.add(trendingPanel, gbc);

        // Grille des prix en direct
        JPanel priceGridPanel = createLivePriceGrid();
        gbc.gridy = 1;
        gbc.weighty = 0.5;
        contentPanel.add(priceGridPanel, gbc);

        // Panel de contrôles
        JPanel controlPanel = createMarketControlSection();
        gbc.gridy = 2;
        gbc.weighty = 0.2;
        contentPanel.add(controlPanel, gbc);

        return contentPanel;
    }

    private JLabel createMarketSummaryLabel() {
        MarketDataService marketService = MarketDataService.getInstance();
        MarketDataService.MarketSummary summary = marketService.getMarketSummary();
        String summaryText = summary.getSummaryText();

        JLabel summaryLabel = new JLabel(summaryText, SwingConstants.CENTER);
        summaryLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        summaryLabel.setForeground(Color.LIGHT_GRAY);

        return summaryLabel;
    }

    private JPanel createTrendingResourcesSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2),
                "📈 Top Market Movers",
                0, 0,
                new Font("Arial", Font.BOLD, 16),
                Color.WHITE
        ));
        panel.setOpaque(true);
        panel.setBackground(new Color(0, 0, 0, 150));

        JPanel trendingGrid = new JPanel(new GridLayout(1, 5, 10, 10));
        trendingGrid.setOpaque(false);
        trendingGrid.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        MarketDataService marketService = MarketDataService.getInstance();
        List<MarketDataService.MarketResourceData> trending = marketService.getTrendingResources();

        for (int i = 0; i < Math.min(5, trending.size()); i++) {
            MarketDataService.MarketResourceData resource = trending.get(i);
            JPanel trendCard = createTrendingResourceCard(resource);
            trendingGrid.add(trendCard);
        }

        // Remplir les cases vides
        while (trendingGrid.getComponentCount() < 5) {
            JPanel emptyCard = createEmptyTrendCard();
            trendingGrid.add(emptyCard);
        }

        panel.add(trendingGrid, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTrendingResourceCard(MarketDataService.MarketResourceData resource) {
        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(true);
        card.setBackground(new Color(40, 40, 40, 200));
        card.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        // Icône
        JLabel iconLabel = new JLabel(
                DATABASE.ResourceType.getIconForResource(resource.category),
                SwingConstants.CENTER
        );
        iconLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        card.add(iconLabel, BorderLayout.NORTH);

        // Nom
        JLabel nameLabel = new JLabel(resource.name, SwingConstants.CENTER);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 10));
        nameLabel.setForeground(Color.WHITE);
        card.add(nameLabel, BorderLayout.CENTER);

        // Changement
        JLabel changeLabel = createChangeLabel(resource);
        card.add(changeLabel, BorderLayout.SOUTH);

        return card;
    }

    private JLabel createChangeLabel(MarketDataService.MarketResourceData resource) {
        double changePercent = resource.getChangePercent();
        String changeText = String.format("%.1f%%", changePercent);

        JLabel changeLabel = new JLabel(changeText, SwingConstants.CENTER);
        changeLabel.setFont(new Font("Arial", Font.BOLD, 11));

        if (changePercent > 0) {
            changeLabel.setForeground(Color.GREEN);
        } else if (changePercent < 0) {
            changeLabel.setForeground(Color.RED);
        } else {
            changeLabel.setForeground(Color.YELLOW);
        }

        return changeLabel;
    }

    private JPanel createEmptyTrendCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(true);
        card.setBackground(new Color(30, 30, 30, 150));
        card.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));

        JLabel emptyLabel = new JLabel("No Data", SwingConstants.CENTER);
        emptyLabel.setForeground(Color.DARK_GRAY);
        card.add(emptyLabel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createLivePriceGrid() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2),
                "💰 Live Resource Prices",
                0, 0,
                new Font("Arial", Font.BOLD, 16),
                Color.WHITE
        ));
        panel.setOpaque(true);
        panel.setBackground(new Color(0, 0, 0, 150));

        MarketDataService marketService = MarketDataService.getInstance();
        Map<String, MarketDataService.MarketResourceData> allMarketData = marketService.getAllMarketData();

        JPanel gridPanel = new JPanel();
        int resourceCount = allMarketData.size();
        int cols = 3;
        int rows = (int) Math.ceil((double) resourceCount / cols);

        gridPanel.setLayout(new GridLayout(rows, cols, 10, 10));
        gridPanel.setOpaque(false);
        gridPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        List<MarketDataService.MarketResourceData> sortedResources = allMarketData.values().stream()
                .sorted(Comparator.comparing((MarketDataService.MarketResourceData a) -> a.category)
                        .thenComparing(a -> a.name))
                .toList();

        for (MarketDataService.MarketResourceData resource : sortedResources) {
            JPanel priceCard = createLivePriceCard(resource);
            gridPanel.add(priceCard);
        }

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setPreferredSize(new Dimension(0, 300));
        scrollPane.setBorder(null);
        UI.styleScrollPane(scrollPane);
        UI.configureScrollSpeed(scrollPane, 20, 80);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createLivePriceCard(MarketDataService.MarketResourceData resource) {
        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(true);
        card.setBackground(new Color(50, 50, 50, 200));
        card.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 2, 2));
        infoPanel.setOpaque(false);

        // Nom avec icône
        JLabel nameLabel = new JLabel(
                DATABASE.ResourceType.getIconForResource(resource.category) + " " + resource.name,
                SwingConstants.CENTER
        );
        nameLabel.setFont(new Font("Arial", Font.BOLD, 11));
        nameLabel.setForeground(Color.WHITE);
        infoPanel.add(nameLabel);

        // Prix
        JLabel priceLabel = new JLabel(
                String.format("%.1f Po", resource.currentPrice),
                SwingConstants.CENTER
        );
        priceLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        priceLabel.setForeground(Color.CYAN);
        infoPanel.add(priceLabel);

        // Changement
        JLabel changeLabel = createDetailedChangeLabel(resource);
        infoPanel.add(changeLabel);

        card.add(infoPanel, BorderLayout.CENTER);
        return card;
    }

    private JLabel createDetailedChangeLabel(MarketDataService.MarketResourceData resource) {
        double changePercent = resource.getChangePercent();
        String trendIcon = getTrendIcon(resource.trend);
        String changeText = String.format("%s %.1f%%", trendIcon, changePercent);

        JLabel changeLabel = new JLabel(changeText, SwingConstants.CENTER);
        changeLabel.setFont(new Font("Arial", Font.PLAIN, 9));

        if (changePercent > 5) {
            changeLabel.setForeground(Color.GREEN);
        } else if (changePercent < -5) {
            changeLabel.setForeground(Color.RED);
        } else {
            changeLabel.setForeground(Color.LIGHT_GRAY);
        }

        return changeLabel;
    }

    private String getTrendIcon(String trend) {
        return switch (trend) {
            case "UP" -> "↗";
            case "DOWN" -> "↘";
            default -> "→";
        };
    }

    private JPanel createMarketControlSection() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2),
                "🎛️ Market Controls",
                0, 0,
                new Font("Arial", Font.BOLD, 16),
                Color.WHITE
        ));
        panel.setOpaque(true);
        panel.setBackground(new Color(0, 0, 0, 150));

        // Boutons selon les permissions
        if ("Admin".equals(Login.currentUser)) {
            JButton adminBtn = createMarketButton(
                    "🔧 Market Admin Panel",
                    new Color(33, 150, 243),
                    e -> openMarketAdminPanel()
            );
            panel.add(adminBtn);
        }

        JButton refreshBtn = createMarketButton(
                "🔄 Refresh Market Data",
                new Color(76, 175, 80),
                e -> refreshMarketData()
        );
        panel.add(refreshBtn);

        JButton reportBtn = createMarketButton(
                "📊 Market Report",
                new Color(156, 39, 176),
                e -> showMarketReport()
        );
        panel.add(reportBtn);

        return panel;
    }

    private JButton createMarketButton(String text, Color backgroundColor, ActionListener action) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.addActionListener(e -> {
            // Audio feedback si disponible
            action.actionPerformed(e);
        });
        return button;
    }

    // **MÉTHODES D'ACTION**
    private void openMarketAdminPanel() {
        SwingUtilities.invokeLater(() -> {
            try {
                MarketAdminPanel adminPanel = new MarketAdminPanel();
                adminPanel.setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error opening Market Admin Panel: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void refreshMarketData() {
        MarketDataService marketService = MarketDataService.getInstance();
        marketService.forceRefresh();
        updateMarketDisplay();
    }

    private void showMarketReport() {
        MarketDataService marketService = MarketDataService.getInstance();
        MarketDataService.MarketSummary summary = marketService.getMarketSummary();

        StringBuilder report = new StringBuilder();
        report.append("=== LIVE MARKET REPORT ===\n\n");
        report.append("Generated: ").append(java.time.LocalDateTime.now()).append("\n\n");
        report.append(summary.getSummaryText()).append("\n\n");
        report.append("=== TOP TRENDING RESOURCES ===\n");

        List<MarketDataService.MarketResourceData> trending = marketService.getTrendingResources();
        for (int i = 0; i < Math.min(10, trending.size()); i++) {
            MarketDataService.MarketResourceData resource = trending.get(i);
            report.append(String.format("%d. %s - %.1f Po (%.1f%%)\n",
                    i + 1, resource.name, resource.currentPrice, resource.getChangePercent()));
        }

        JTextArea textArea = new JTextArea(report.toString());
        textArea.setRows(20);
        textArea.setColumns(60);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(textArea);
        UI.styleScrollPane(scrollPane);

        JOptionPane.showMessageDialog(this, scrollPane,
                "📊 Live Market Report", JOptionPane.INFORMATION_MESSAGE);
    }

    // **MÉTHODES DE MISE À JOUR**
    private void startMarketRefreshTimer() {
        javax.swing.Timer refreshTimer = new  javax.swing.Timer(30000, e -> { // Refresh toutes les 30 secondes
            SwingUtilities.invokeLater(this::updateMarketDisplay);
        });
        refreshTimer.start();
    }

    private void updateMarketDisplay() {
        // Forcer la mise à jour des données de marché
        MarketDataService marketService = MarketDataService.getInstance();
        marketService.forceRefresh();

        // Mettre à jour récursivement tous les composants
        updatePanelData(this);
        revalidate();
        repaint();
    }

    private void updatePanelData(JPanel panel) {
        Component[] components = panel.getComponents();
        for (Component component : components) {
            if (component instanceof JLabel label) {
                String text = label.getText();
                if (text != null && text.contains("Market")) {
                    // Mettre à jour le résumé de marché
                    MarketDataService marketService = MarketDataService.getInstance();
                    MarketDataService.MarketSummary summary = marketService.getMarketSummary();
                    label.setText(summary.getSummaryText());
                }
            } else if (component instanceof JPanel) {
                updatePanelData((JPanel) component);
            }
        }
    }

    // **NETTOYAGE**
    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
    }
}
