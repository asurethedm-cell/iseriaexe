package com.iseria.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iseria.domain.DATABASE;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iseria.infra.AppConfig;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏪 Market Data Service - Singleton for Global Market Access
 * Thread-safe service for dynamic resource pricing throughout the application
 */
public class MarketDataService {

    private static volatile MarketDataService INSTANCE;
    private final Map<String, MarketResourceData> marketPrices = new ConcurrentHashMap<>();
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private static final String SAVE_DIR = "Iseria_Data";
    private final String MARKET_DATA_FILE = "market_data.json";
    private LocalDateTime lastRefresh = LocalDateTime.now();
    private final long REFRESH_INTERVAL_MINUTES = 5;
    private ObjectMapper mapper;

    // 📊 Market Resource Data Structure
    public static class MarketResourceData {
        public String name;
        public String category;
        public double basePrice;
        public double currentPrice;
        public double volatility;
        public String trend;
        public LocalDateTime lastUpdate;

        public MarketResourceData(String name, String category, double basePrice, double currentPrice) {
            this.name = name;
            this.category = category;
            this.basePrice = basePrice;
            this.currentPrice = currentPrice;
            this.volatility = 0.1;
            this.trend = "STABLE";
            this.lastUpdate = LocalDateTime.now();
        }

        public double getPriceMultiplier() {
            return currentPrice / basePrice;
        }

        public double getChangePercent() {
            return ((currentPrice - basePrice) / basePrice) * 100;
        }
    }

    private MarketDataService() {
        initializeMarketData();
    }

    public static MarketDataService getInstance() {
        if (INSTANCE == null) {
            synchronized (MarketDataService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MarketDataService();
                }
            }
        }
        return INSTANCE;
    }

    private void initializeMarketData() {
        if (!loadFromFile()) {
            initializeFromDatabase();
        }
    }

    private void initializeFromDatabase() {
        for (DATABASE.ResourceType type : DATABASE.ResourceType.values()) {
                double startPrice = (type.getTransformed()
                        ? type.getvalueIfTransformed()
                        : type.getBaseValue());
            MarketResourceData m = new MarketResourceData(
                    type.getName(),
                    type.getCategory(),
                    type.getBaseValue(),
                    startPrice
            );
            marketPrices.put(type.getName().toLowerCase(), m);
        }

        System.out.println("Market initialized with "
                + marketPrices.size() + " resources from DATABASE");
    }

    private boolean loadFromFile() {
        Path saveFile = getSaveDirectory().resolve(getSaveFileName(MARKET_DATA_FILE));
        File marketFile = new File(MARKET_DATA_FILE);
        if (!Files.exists(saveFile)) {
            return false;
        }

        try {
            JsonNode root = jsonMapper.readTree(marketFile);
            JsonNode resourcesNode = root.get("resources");

            if (resourcesNode != null) {
                resourcesNode.fieldNames().forEachRemaining(resourceKey -> {
                    JsonNode resourceNode = resourcesNode.get(resourceKey);

                    MarketResourceData marketData = new MarketResourceData(
                            resourceNode.get("name").asText(),
                            resourceNode.get("category").asText(),
                            resourceNode.get("baseValue").asDouble(),
                            resourceNode.get("currentPrice").asDouble()
                    );

                    marketData.volatility = resourceNode.get("volatility").asDouble();
                    marketData.trend = resourceNode.get("trend").asText();

                    marketPrices.put(resourceKey, marketData);
                });

                lastRefresh = LocalDateTime.now();
                System.out.println("Market data loaded from file: " + marketPrices.size() + " resources");
                return true;
            }
        } catch (IOException e) {
            System.err.println("Error loading market data: " + e.getMessage());
        }

        return false;
    }

    // PUBLIC API METHODS
    public double getCurrentPrice(String resourceName) {
        refreshIfStale();

        MarketResourceData data = marketPrices.get(resourceName.toLowerCase());
        if (data != null) {
            return data.currentPrice;
        }

        // 🔄 Fallback to DATABASE
        return getDatabasePrice(resourceName);
    }
    public double getPriceMultiplier(String resourceName) {
        refreshIfStale();

        MarketResourceData data = marketPrices.get(resourceName.toLowerCase());
        if (data != null) {
            return data.getPriceMultiplier();
        }

        return 1.0; // No change
    }
    public Map<String, MarketResourceData> getAllMarketData() {
        refreshIfStale();
        return new HashMap<>(marketPrices);
    }
    public void updatePrice(String resourceName, double newPrice) {
        MarketResourceData data = marketPrices.get(resourceName.toLowerCase());
        if (data != null) {
            data.currentPrice = newPrice;
            data.lastUpdate = LocalDateTime.now();

            // Update trend based on change
            double change = data.getChangePercent();
            if (change > 5) data.trend = "UP";
            else if (change < -5) data.trend = "DOWN";
            else data.trend = "STABLE";
        }
    }
    public List<MarketResourceData> getTrendingResources() {
        refreshIfStale();

        return marketPrices.values().stream()
                .sorted((a, b) -> Double.compare(
                        Math.abs(b.getChangePercent()),
                        Math.abs(a.getChangePercent())))
                .limit(10)
                .toList();
    }
    public MarketSummary getMarketSummary() {
        refreshIfStale();

        double totalResources = marketPrices.size();
        long upTrend = marketPrices.values().stream()
                .mapToLong(d -> "UP".equals(d.trend) ? 1 : 0).sum();
        long downTrend = marketPrices.values().stream()
                .mapToLong(d -> "DOWN".equals(d.trend) ? 1 : 0).sum();

        double avgChange = marketPrices.values().stream()
                .mapToDouble(MarketResourceData::getChangePercent)
                .average().orElse(0.0);

        return new MarketSummary(totalResources, upTrend, downTrend, avgChange);
    }
    private Path getSaveDirectory() {
        String documentsPath = FileSystemView.getFileSystemView().getDefaultDirectory().getPath();
        Path saveDir = Paths.get(documentsPath, SAVE_DIR);

        try {
            if (!Files.exists(saveDir)) {
                Files.createDirectories(saveDir);
            }
        } catch (IOException e) {
            System.err.println("Erreur création répertoire de sauvegarde: " + e.getMessage());
        }

        return saveDir;
    }
    private String getSaveFileName(String fileName) {
        return MARKET_DATA_FILE;
    }
    public static class MarketSummary {
        public final double totalResources;
        public final long upTrending;
        public final long downTrending;
        public final double averageChange;

        public MarketSummary(double total, long up, long down, double avgChange) {
            this.totalResources = total;
            this.upTrending = up;
            this.downTrending = down;
            this.averageChange = avgChange;
        }

        public String getSummaryText() {
            return String.format("Market: %.0f resources | 📈 %d up | 📉 %d down | Avg: %.1f%%",
                    totalResources, upTrending, downTrending, averageChange);
        }
    }

    // UTILITY METHODS

    private void refreshIfStale() {
        if (LocalDateTime.now().minusMinutes(REFRESH_INTERVAL_MINUTES).isAfter(lastRefresh)) {
            loadFromFile();
        }
    }
    private double getDatabasePrice(String resourceName) {
        for (DATABASE.ResourceType resource : DATABASE.ResourceType.values()) {
            if (resource.getName().equalsIgnoreCase(resourceName)) {
                return resource.getBaseValue();
            }
        }

        return 10.0; // Default fallback price
    }
    public void forceRefresh() {
        loadFromFile();
    }
    public boolean hasMarketData(String resourceName) {
        return marketPrices.containsKey(resourceName.toLowerCase());
    }
    public Set<String> getAvailableCategories() {
        return marketPrices.values().stream()
                .map(data -> data.category)
                .collect(java.util.stream.Collectors.toSet());
    }
    public void saveToFile() {
        try {
            // ensure our save‐dir exists
            String documentsPath = FileSystemView.getFileSystemView().getDefaultDirectory().getPath();
            Path dir = Paths.get(documentsPath, SAVE_DIR);
            Files.createDirectories(dir);
            File out = dir.resolve(MARKET_DATA_FILE).toFile();
            // existing JSON writer logic
            ObjectNode root = mapper.createObjectNode();
            root.put("timestamp", LocalDateTime.now().toString());
            ObjectNode resources = mapper.createObjectNode();
            for (MarketResourceData d : marketPrices.values()) {
                ObjectNode res = mapper.createObjectNode();
                res.put("name", d.name);
                res.put("category", d.category);
                res.put("baseValue", d.basePrice);
                res.put("currentPrice", d.currentPrice);
                res.put("volatility", d.volatility);
                res.put("trend", d.trend);
                resources.set(d.name.toLowerCase(), res);
            }
            root.set("resources", resources);
            mapper.writerWithDefaultPrettyPrinter().writeValue(out, root);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}