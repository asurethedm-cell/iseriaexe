package com.iseria.domain;
import java.io.Serial;
import java.io.Serializable;

public class Route implements Serializable {

    @Serial
    private static final long serialVersionUID = 2L;
    private final String fromHex;
    private final String toHex;
    private final int distance;
    private final boolean hasRoad;
    private final boolean hasRiver;
    private final boolean hasSea;

    public Route(String fromHex, String toHex, int distance,
                 boolean hasRoad, boolean hasRiver, boolean hasSea) {
        this.fromHex = fromHex;
        this.toHex = toHex;
        this.distance = distance;
        this.hasRoad = hasRoad;
        this.hasRiver = hasRiver;
        this.hasSea = hasSea;
    }

    public double getSpeedMultiplier(TransportVehicle vehicle) {
        double multiplier = 1.0;

        // Bonus route : +100%
        if (hasRoad) {
            multiplier += 1.0;
        }

        // Bonus véhicule : +100% pour véhicules terrestres
        if (vehicle.getType() != TransportVehicle.VehicleType.NONE) {
            if (vehicle.getType() == TransportVehicle.VehicleType.BATEAU_RIVIERE && hasRiver) {
                multiplier += 1.5; // +150% sur rivière
            } else if (vehicle.getType() == TransportVehicle.VehicleType.BATEAU_MER && hasSea) {
                multiplier += 3.0; // +300% sur mer
            } else if (isLandVehicle(vehicle.getType())) {
                multiplier += 1.0; // +100% pour charrettes, chariots, wagons
            }
        }

        return multiplier;
    }

    private boolean isLandVehicle(TransportVehicle.VehicleType type) {
        return type == TransportVehicle.VehicleType.CHARRETTE ||
                type == TransportVehicle.VehicleType.CHARIOT ||
                type == TransportVehicle.VehicleType.WAGON;
    }

    // Getters
    public String getFromHex() { return fromHex; }
    public String getToHex() { return toHex; }
    public int getDistance() { return distance; }
    public boolean hasRoad() { return hasRoad; }
    public boolean hasRiver() { return hasRiver; }
    public boolean hasSea() { return hasSea; }
}