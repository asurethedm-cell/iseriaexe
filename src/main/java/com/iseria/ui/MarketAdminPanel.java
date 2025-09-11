package com.iseria.ui;

import com.iseria.infra.AppConfig;
import com.iseria.service.MarketDataService;
import com.iseria.service.MarketDataService.MarketResourceData;
import com.iseria.service.EconomicDataService;
import com.iseria.domain.DATABASE;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;
import java.util.Properties;

public class MarketAdminPanel extends JFrame {
    private final MarketDataService marketService = MarketDataService.getInstance();
    private final EconomicDataService economicService;
    private JTable table;
    private DefaultTableModel model;
    private Timer autoSaveTimer;
    private TableRowSorter<DefaultTableModel> sorter;
    public MarketAdminPanel(EconomicDataService ecoService) {
        this.economicService = ecoService;
        setTitle("Market Admin Panel");
        setSize(1200, 800);
        setDefaultCloseOperation(HIDE_ON_CLOSE);

        initTable();
        initControls();
        loadData();
        startAutoSave();
    }

    private void initTable() {
        String[] cols = { "Icon", "Resource", "Category", "Base Price", "Current Price", "Change %", "Trend" };
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return col == 4; }
            @Override public Class<?> getColumnClass(int col) {
                return switch (col) {
                    case 3, 4 -> Double.class;
                    case 5    -> String.class;
                    default   -> String.class;
                };
            }
        };

        table = new JTable(model);

        // Enhanced emoji font detection with multiple fallbacks
        Font emojiFont = createEmojiFont();

        // Apply emoji font to Icon column with enhanced renderer
        table.getColumnModel().getColumn(0).setCellRenderer(new EmojiTableCellRenderer(emojiFont));

        // Apply same emoji font to Trend column
        table.getColumnModel().getColumn(6).setCellRenderer(new EmojiTableCellRenderer(emojiFont));

        // Percentage renderer (color coding) - unchanged
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (value != null) {
                    try {
                        double pct = Double.parseDouble(value.toString().replace("%", ""));
                        setForeground(pct > 5 ? Color.GREEN : pct < -5 ? Color.RED : Color.DARK_GRAY);
                    } catch (NumberFormatException ignored) { }
                }
                return this;
            }
        });

        // Enable sorting and other configurations
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        table.getTableHeader().setReorderingAllowed(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.getModel().addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 4) {
                updatePrice(e.getFirstRow());
                marketService.saveToFile();
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    // Enhanced font creation with multiple fallbacks
    private Font createEmojiFont() {
        String os = System.getProperty("os.name").toLowerCase();
        String[] fontNames;

        if (os.contains("mac")) {
            fontNames = new String[]{
                    "Apple Color Emoji", "Apple Symbols", "Helvetica", "Dialog"
            };
        } else if (os.contains("win")) {
            fontNames = new String[]{
                    "Segoe UI Emoji", "Segoe UI Symbol", "Segoe UI", "Dialog"
            };
        } else {
            // Linux and others
            fontNames = new String[]{
                    "Noto Color Emoji", "Noto Emoji", "DejaVu Sans", "Dialog"
            };
        }

        // Try each font until we find one that's available
        for (String fontName : fontNames) {
            Font font = new Font(fontName, Font.PLAIN, 18);
            if (font.getFamily().equals(fontName)) {
                System.out.println("Using emoji font: " + fontName);
                return font;
            }
        }

        System.out.println("No emoji font found, using default Dialog");
        return new Font("Dialog", Font.PLAIN, 18);
    }

    // Custom renderer for emoji cells
    private static class EmojiTableCellRenderer extends DefaultTableCellRenderer {
        private final Font emojiFont;

        public EmojiTableCellRenderer(Font emojiFont) {
            this.emojiFont = emojiFont;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(emojiFont);

            // Enable anti-aliasing for better emoji rendering
            putClientProperty("html.disable", Boolean.FALSE);

            return this;
        }
    }

    private void initControls() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton resetBtn = new JButton("🔄 Reset Prices");
        resetBtn.addActionListener(e -> confirmReset());
        panel.add(resetBtn);

        JButton crashBtn = new JButton("💥 Crash");
        crashBtn.addActionListener(e -> applyMarketEvent(-0.3));
        panel.add(crashBtn);

        JButton boomBtn = new JButton("🚀 Boom");
        boomBtn.addActionListener(e -> applyMarketEvent(+0.4));
        panel.add(boomBtn);

        // ✅ ADD REFRESH BUTTON
        JButton refreshBtn = new JButton("🔄 Refresh Data");
        refreshBtn.addActionListener(e -> {
            loadData();
            JOptionPane.showMessageDialog(this, "✅ Market data refreshed!",
                    "Refresh Complete", JOptionPane.INFORMATION_MESSAGE);
        });
        panel.add(refreshBtn);

        add(panel, BorderLayout.NORTH);
    }

    private void loadData() {
        model.setRowCount(0);

        for (MarketResourceData d : marketService.getAllMarketData().values()) {
            double change = d.getChangePercent();
            String trend = change > 5 ? "⬆️" : change < -5 ? "⬇️" : "➡️";

            // ✅ USE WORKING EMOJI METHOD instead of DATABASE.ResourceType.getIconForResource
            String emoji = getResourceIcon(d.category);

            model.addRow(new Object[]{
                    emoji,                                    // ✅ Fixed emoji display
                    d.name,
                    d.category,
                    d.basePrice,
                    d.currentPrice,
                    String.format("%.1f%%", change),        // Keep as formatted string
                    trend
            });
        }

        // ✅ REFRESH SORTING after data load
        if (sorter != null) {
            sorter.allRowsChanged();
        }
    }

    // ✅ ADD THE WORKING EMOJI METHOD from MainMenu.java
    private String getResourceIcon(String category) {
        return switch (category.toLowerCase()) {
            case "joyaux", "joyaux taillé" -> "💎";
            case "gemmes", "gemmes taillé" -> "💍";
            case "luxe", "luxe taillé" -> "👑";
            case "lingot" -> "🥇";
            case "animaux" -> "🐎";
            case "culture" -> "🌾";
            case "luxueuse" -> "🍷";
            case "nourriture" -> "🍞";
            case "nourriture transformé" -> "🍽️";
            case "sous-produits" -> "🥛";
            case "transformé" -> "🔨";
            case "artisanat" -> "🎨";
            default -> "📦";
        };
    }

    private void updatePrice(int row) {
        // CONVERT MODEL ROW TO VIEW ROW (important for sorting)
        int modelRow = table.convertRowIndexToModel(row);
        String name = (String) model.getValueAt(modelRow, 1);
        double oldPrice = marketService.getCurrentPrice(name);
        double newPrice = (Double) model.getValueAt(modelRow, 4);

        try {
            validatePrice(newPrice);
            marketService.updatePrice(name, newPrice);
            System.out.println("Price updated: {" + name + "} from {" + oldPrice + "} to {" + newPrice + "}");
            loadData(); // refresh only if needed
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Invalid Price", JOptionPane.ERROR_MESSAGE);
            model.setValueAt(oldPrice, modelRow, 4);
        }
    }

    private void validatePrice(double p) {
        if (p < 0) throw new IllegalArgumentException("Price cannot be negative.");
        if (p > 1_000_000) throw new IllegalArgumentException("Price too high.");
    }

    private void confirmReset() {
        int ans = JOptionPane.showConfirmDialog(this,
                "Reset all prices to base?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (ans == JOptionPane.YES_OPTION) {
            marketService.getAllMarketData().values()
                    .forEach(d -> marketService.updatePrice(d.name, d.basePrice));
            loadData();
        }
    }

    private void applyMarketEvent(double avgChange) {
        marketService.getAllMarketData().values().forEach(d -> {
            double newP = d.currentPrice * (1 + avgChange);
            marketService.updatePrice(d.name, newP);
        });
        loadData();
        JOptionPane.showMessageDialog(this,
                avgChange < 0 ? "💥 Market Crash!" : "🚀 Market Boom!",
                "Event", JOptionPane.INFORMATION_MESSAGE);
    }

    private void startAutoSave() {
        int interval = Integer.parseInt(
                AppConfig.get("market.auto.save.interval", "300000"));
        autoSaveTimer = new Timer(interval, e -> {
            marketService.forceRefresh();
            System.out.println("Auto-saved market data");
        });
        autoSaveTimer.start();
    }

    // CLEANUP ON CLOSE
    @Override
    public void dispose() {
        if (autoSaveTimer != null) {
            autoSaveTimer.stop();
        }
        super.dispose();
    }
}