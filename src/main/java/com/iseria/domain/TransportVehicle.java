package com.iseria.domain;

import java.io.Serializable;
import java.util.*;

public class TransportVehicle  implements Serializable {
    private static final long serialVersionUID = 1L;
    public enum VehicleType {
        NONE(0, Map.of()),
        CHARRETTE(1, Map.of(
                "bois", 2, "pierre", 4, "minerais", 5,
                "lingots", 6, "nourriture", 6, "cuir", 6
        )),
        CHARIOT(1, Map.of(
                "bois", 3, "pierre", 5, "minerais", 7,
                "lingots", 8, "nourriture", 8, "cuir", 8
        )),
        WAGON(1, Map.of(
                "bois", 5, "pierre", 7, "minerais", 9,
                "lingots", 10, "nourriture", 10, "cuir", 10
        )),
        BATEAU_RIVIERE(1.5, Map.of()), // +150% sur rivière
        BATEAU_MER(3.0, Map.of());     // +300% sur mer

        private final double speedMultiplier;
        private final Map<String, Integer> baseCapacities;

        VehicleType(double speedMultiplier, Map<String, Integer> capacities) {
            this.speedMultiplier = speedMultiplier;
            this.baseCapacities = capacities;
        }
        public double getSpeedMultiplier() {
            return speedMultiplier;
        }
        public int getCapacityFor(String resourceType) {
            return baseCapacities.getOrDefault(resourceType, 1);
        }
    }

    public static final TransportVehicle DEFAULT =
            new TransportVehicle(VehicleType.NONE, "");

    private final VehicleType type;
    private final String assignedHexKey;
    private final Map<String, Integer> customCapacities = new HashMap<>();

    public TransportVehicle(VehicleType type, String assignedHexKey) {
        this.type = type;
        this.assignedHexKey = assignedHexKey;
    }
    public int getCapacityForResource(String resourceType) {
        // Logique pour mapper les ResourceType vers les catégories de transport
        String category = mapResourceToTransportCategory(resourceType);
        return customCapacities.getOrDefault(category,
                type.getCapacityFor(category));
    }
    public VehicleType getType() {
        return type;
    }
    public String getAssignedHexKey() {
        return assignedHexKey;
    }
    public double getSpeedMultiplier() {
        return type.getSpeedMultiplier();
    }
    private String mapResourceToTransportCategory(String resourceType) {
        DATABASE.ResourceType resource = DATABASE.ResourceType.lookupByName(resourceType);
        if (resource == null) return "other";

        return switch (resource.getCategory()) {
            case "Nourriture", "Bloc Nourritures" -> "nourriture";
            case "Minerais" -> "minerais";
            case "Lingot" -> "lingots";
            case "Transformé" -> {
                if (resourceType.contains("bois") || resourceType.contains("planche")) {
                    yield "bois";
                } else if (resourceType.contains("cuir")) {
                    yield "cuir";
                } else if (resourceType.contains("pierre")) {
                    yield "pierre";
                }
                yield "other";
            }
            default -> "other";
        };
    }
}