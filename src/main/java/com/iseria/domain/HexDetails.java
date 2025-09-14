package com.iseria.domain;

import com.iseria.service.LogisticsService;

import java.io.*;
import java.util.*;

/** Pure domain model for a map hex. No UI, no I/O. */
public class HexDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    private String hexKey;
    private int mainBuildingIndex;
    private int auxBuildingIndex;
    private int fortBuildingIndex;
    private String factionClaim;
    private Map<String,List<String>> buildingWorkers = new HashMap<>();
    private Map<String,Boolean> lockedSlots = new HashMap<>();
    private int mainWorkerCount;
    private int auxWorkerCount;
    private int fortWorkerCount;
    private Map<String, String> selectedResourceTypes = new HashMap<>();
    private Map<String, Double> selectedResourceProductions = new HashMap<>();
    private LivestockFarm livestockFarm;
    private List<TransportVehicle> assignedVehicles = new ArrayList<>();
    private LogisticsHexData logisticsData = new LogisticsHexData();
    private Set<String> discoveredByFaction = new HashSet<>();

    public HexDetails(String hexKey) {
        this.hexKey = hexKey;
        this.mainBuildingIndex = 0;
        this.auxBuildingIndex = 0;
        this.fortBuildingIndex = 0;
        this.factionClaim = "Free";
        buildingWorkers.put("main", new ArrayList<>());
        buildingWorkers.put("aux",  new ArrayList<>());
        buildingWorkers.put("fort", new ArrayList<>());
        lockedSlots.put("main", false);
        lockedSlots.put("aux",  false);
        lockedSlots.put("fort", false);
        this.mainWorkerCount = 0;
        this.auxWorkerCount = 0;
        this.fortWorkerCount = 0;
        selectedResourceTypes.put("main", null);
        selectedResourceTypes.put("aux", null);
        selectedResourceTypes.put("fort", null);
        selectedResourceProductions.put("main", 0.0);
        selectedResourceProductions.put("aux", 0.0);
        selectedResourceProductions.put("fort", 0.0);
        this.livestockFarm = new LivestockFarm(hexKey);
        this.assignedVehicles = new ArrayList<>();
        this.discoveredByFaction = new HashSet<>();
    }

    public HexDetails(HexDetails other) {
        this.hexKey = other.hexKey;
        //Tracker for the Mystery Null Hex insert bug
        if (other.hexKey == null) {
            System.err.println("❌ Constructeur de copie HexDetails avec other.hexKey == null !");
            Thread.dumpStack();
        }
        this.mainBuildingIndex = other.mainBuildingIndex;
        this.auxBuildingIndex  = other.auxBuildingIndex;
        this.fortBuildingIndex = other.fortBuildingIndex;
        this.factionClaim      = other.factionClaim;
        this.buildingWorkers = new HashMap<>(other.buildingWorkers);
        this.lockedSlots     = new HashMap<>(other.lockedSlots);
        this.mainWorkerCount = other.mainWorkerCount ;
        this.auxWorkerCount = other.auxWorkerCount;
        this.fortWorkerCount = other.fortWorkerCount;
        this.selectedResourceTypes = new HashMap<>(other.selectedResourceTypes);
        this.selectedResourceProductions = new HashMap<>(other.selectedResourceProductions);
        this.livestockFarm = new LivestockFarm(hexKey);
        this.assignedVehicles = new ArrayList<>(other.assignedVehicles);
        this.discoveredByFaction = new HashSet<>(other.discoveredByFaction);
    }

//===============================================GETTER SETTER========================================================\\


    public String getHexKey()                    { return hexKey; }
    public int    getMainBuildingIndex()         { return mainBuildingIndex; }
    public int    getAuxBuildingIndex()          { return auxBuildingIndex; }
    public int    getFortBuildingIndex()         { return fortBuildingIndex; }
    public String getFactionClaim()              { return factionClaim != null ? factionClaim : "Free"; }
    public Map<String,List<String>> getBuildingWorkers() { return buildingWorkers; }
    public Map<String,Boolean>      getLockedSlots()     { return lockedSlots; }
    public int    getMainWorkerCount() { return mainWorkerCount; }
    public int    getAuxWorkerCount() { return auxWorkerCount; }
    public int    getFortWorkerCount() { return fortWorkerCount; }
    public void   setMainBuildingIndex(int idx)  { mainBuildingIndex = idx; }
    public void   setAuxBuildingIndex(int idx)   { auxBuildingIndex  = idx; }
    public void   setFortBuildingIndex(int idx)  { fortBuildingIndex = idx; }
    public void   setFactionClaim(String claim)  { factionClaim      = claim; }
    public void   setMainWorkerCount(int count) { this.mainWorkerCount = Math.max(0, count); }
    public void   setAuxWorkerCount(int count) { this.auxWorkerCount = Math.max(0, count); }
    public void   setFortWorkerCount(int count) { this.fortWorkerCount = Math.max(0, count); }
    public String getSelectedResourceType(String buildingType) {
        return selectedResourceTypes.get(buildingType.toLowerCase());
    }
    public void setSelectedResourceType(String buildingType, String resourceTypeName) {
        selectedResourceTypes.put(buildingType.toLowerCase(), resourceTypeName);
    }
    public Double getSelectedResourceProduction(String buildingType) {
        return selectedResourceProductions.getOrDefault(buildingType.toLowerCase(), 0.0);
    }
    public void setSelectedResourceProduction(String buildingType, double production) {
        selectedResourceProductions.put(buildingType.toLowerCase(), production);
    }
    public int getWorkerCountByType(String buildingType) {
        return switch (buildingType.toLowerCase()) {
            case "main", "main building" -> mainWorkerCount;
            case "aux", "auxiliary building" -> auxWorkerCount;
            case "fort", "fort building" -> fortWorkerCount;
            default -> 0;
        };
    }
    public void setWorkerCountByType(String buildingType, int count) {
        switch (buildingType.toLowerCase()) {
            case "main", "main building" -> setMainWorkerCount(count);
            case "aux", "auxiliary building" -> setAuxWorkerCount(count);
            case "fort", "fort building" -> setFortWorkerCount(count);
        }
    }
    public LivestockFarm getLivestockFarm() { return livestockFarm; }
    public void setLivestockFarm(LivestockFarm livestockFarm) {
        this.livestockFarm = livestockFarm;
    }
    public void addVehicle(TransportVehicle vehicle) {
        if (assignedVehicles == null) {
            assignedVehicles = new ArrayList<>();
        }
        assignedVehicles.add(vehicle);
    }
    public int getVehicleCount() {
        return assignedVehicles != null ? assignedVehicles.size() : 0;
    }
    public List<TransportVehicle> getAssignedVehicles() {
        return new ArrayList<>(assignedVehicles);
    }
    public List<TransportVehicle> getVehiclesByType(TransportVehicle.VehicleType type) {
        if (assignedVehicles == null) return new ArrayList<>();

        return assignedVehicles.stream()
                .filter(v -> v.getType() == type)
                .collect(java.util.stream.Collectors.toList());
    }
    public int getTransportCapacity(String resourceType) {
        return assignedVehicles.stream()
                .mapToInt(v -> v.getCapacityForResource(resourceType))
                .sum();
    }
    public LogisticsHexData getLogisticsData() {
        return logisticsData;
    }
    public void setLogisticsData(LogisticsHexData logisticsData) {
        this.logisticsData = logisticsData;
    }
    public List<String> getWorkers(String type) {
        List<String> list = buildingWorkers.get(type);
        if (list == null || list.isEmpty()) {
            return Collections.singletonList(" ");
        }
        return new ArrayList<>(list);
    }
    public void setWorkers(String type, List<String> workers) {
        buildingWorkers.put(type, new ArrayList<>(workers));
    }
    public int getTotalWorkers() {
        return mainWorkerCount + auxWorkerCount + fortWorkerCount;
    }
    public Set<String> getDiscoveredByFaction() {
        return discoveredByFaction;
    }
    public void setDiscoveredByFaction(Set<String> factions) {
        this.discoveredByFaction = new HashSet<>(factions);
    }

//====================================================================================================================\\


    public void lockSlot(String slot)                  { lockedSlots.put(slot, true); }
    public boolean isSlotLocked(String slot)           { return lockedSlots.getOrDefault(slot, false); }
    public boolean removeVehicle(TransportVehicle vehicle) {
        if (assignedVehicles != null) {
            return assignedVehicles.remove(vehicle);
        }
        return false;
    }
    public void clearVehicles() {
        if (assignedVehicles == null) {
            assignedVehicles = new ArrayList<>();
        } else {
            assignedVehicles.clear();
        }
    }




    @Override public boolean equals(Object obj) {  if (this == obj) return true;
        if (!(obj instanceof HexDetails)) return false;
        HexDetails other = (HexDetails) obj;
        return Objects.equals(this.hexKey, other.hexKey);
    }
    @Override public int     hashCode()       {return Objects.hashCode(hexKey);}
    @Override public String  toString()       { /* human‐readable */ return super.toString(); }


    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (hexKey == null) {
            System.err.println("❌ HexDetails désérialisé avec hexKey == null !");
            Thread.dumpStack();
        }
        if (selectedResourceTypes == null) {
            selectedResourceTypes = new HashMap<>();
            selectedResourceTypes.put("main", null);
            selectedResourceTypes.put("aux", null);
            selectedResourceTypes.put("fort", null);
        }
        if (selectedResourceProductions == null) {
            selectedResourceProductions = new HashMap<>();
            selectedResourceProductions.put("main", 0.0);
            selectedResourceProductions.put("aux", 0.0);
            selectedResourceProductions.put("fort", 0.0);
        }
        if (livestockFarm == null) {
            livestockFarm = new LivestockFarm(hexKey);
        }
        if (assignedVehicles == null) {
            assignedVehicles = new ArrayList<>();
        }
        if (logisticsData == null) {
            logisticsData = new LogisticsHexData();
        }
        if (discoveredByFaction == null) {
            discoveredByFaction = new HashSet<>();
        }
    }

    public boolean isDiscoveredBy(String factionId) {
        if (factionId != null && factionId.equals(this.factionClaim)) {
            return true;
        }
        return discoveredByFaction != null && discoveredByFaction.contains(factionId);
    }    public boolean canEstablishLivestock(DATABASE.LivestockData animalType, IHexRepository repo) {
        return animalType.canEstablishIn(this, repo) && livestockFarm.getTotalAnimaux() < 10;
    }
    public double calculateFoodProductionForLivestock() {
        return getSelectedResourceProduction("main") +
                getSelectedResourceProduction("aux");
    }
    public boolean removeVehicleByInfo(String vehicleInfo) {
        if (assignedVehicles == null) return false;

        return assignedVehicles.removeIf(vehicle -> {
            String info = String.format("%s (Speed: %.1fx)",
                    vehicle.getType().name(),
                    vehicle.getSpeedMultiplier());
            return info.equals(vehicleInfo);
        });
    }

    public static class LogisticsHexData implements Serializable {
        private boolean hasRoad = false;
        private boolean hasRiver = false;
        private boolean hasSea = false;
        private double transportEfficiency = 1.0;

        // Getters et setters
        public boolean hasRoad() { return hasRoad; }
        public void setHasRoad(boolean hasRoad) { this.hasRoad = hasRoad; }
        public boolean hasRiver() { return hasRiver; }
        public void setHasRiver(boolean hasRiver) { this.hasRiver = hasRiver; }
        public boolean hasSea() { return hasSea; }
        public void setHasSea(boolean hasSea) { this.hasSea = hasSea; }
        public double getTransportEfficiency() { return transportEfficiency; }
        public void setTransportEfficiency(double efficiency) { this.transportEfficiency = efficiency; }

    }

}
