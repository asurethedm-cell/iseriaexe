package com.iseria.domain;

import java.io.Serial;
import java.util.*;
import java.io.Serializable;

public class StorageWarehouse implements Serializable {

    @Serial
    private static final long serialVersionUID = 2L;
    private final String hexKey;
    private final DATABASE.MainBuilding buildingType;
    private final int tier;
    private final int maxCapacity;
    private final Map<String, Double> currentStorage = new HashMap<>();

    public StorageWarehouse(String hexKey, DATABASE.MainBuilding building) {
        this.hexKey = hexKey;
        this.buildingType = building;
        this.tier = building.getMainTier();
        this.maxCapacity = calculateCapacity();
    }

    private int calculateCapacity() {
        // Capacités selon le tableau de l'utilisateur
        return switch (tier) {
            case 0 -> 100;  // T0: 100
            case 1 -> 250;  // T1: 250
            case 2 -> 500;  // T2: 500
            default -> 100;
        };
    }

    public boolean canStore(String resourceType, double quantity) {
        double currentTotal = currentStorage.values().stream()
                .mapToDouble(Double::doubleValue).sum();
        return (currentTotal + quantity) <= maxCapacity;
    }

    public boolean addResource(String resourceType, double quantity) {
        if (!canStore(resourceType, quantity)) {
            return false;
        }

        currentStorage.merge(resourceType, quantity, Double::sum);
        return true;
    }

    public double removeResource(String resourceType, double quantity) {
        double available = currentStorage.getOrDefault(resourceType, 0.0);
        double toRemove = Math.min(available, quantity);

        if (toRemove > 0) {
            currentStorage.put(resourceType, available - toRemove);
            if (currentStorage.get(resourceType) <= 0) {
                currentStorage.remove(resourceType);
            }
        }

        return toRemove;
    }

    public int getAvailableSpace() {
        double used = currentStorage.values().stream()
                .mapToDouble(Double::doubleValue).sum();
        return (int) (maxCapacity - used);
    }

    // Getters
    public String getHexKey() { return hexKey; }
    public int getTier() { return tier; }
    public int getMaxCapacity() { return maxCapacity; }
    public Map<String, Double> getCurrentStorage() {
        return new HashMap<>(currentStorage);
    }
}