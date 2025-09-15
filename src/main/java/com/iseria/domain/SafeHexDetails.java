
package com.iseria.domain;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SafeHexDetails implements Serializable {
    @Serial
    private static final long serialVersionUID = 2L; // Incrementé pour forcer migration
    private static final String VALIDATION_KEY = "ISERIA_HEX_V2";
    
    // ✅ CORE FIELDS - Thread-safe
    private volatile String hexKey;
    private volatile String factionClaim = "Free";
    private volatile int mainBuildingIndex = 0;
    private volatile int auxBuildingIndex = 0;
    private volatile int fortBuildingIndex = 0;
    
    // ✅ WORKERS - Thread-safe
    private volatile int mainWorkerCount = 0;
    private volatile int auxWorkerCount = 0; 
    private volatile int fortWorkerCount = 0;
    
    // ✅ COLLECTIONS - Thread-safe
    private final Map<String, String> selectedResourceTypes = new ConcurrentHashMap<>();
    private final Map<String, Double> selectedResourceProductions = new ConcurrentHashMap<>();
    private final Set<String> discoveredByFaction = ConcurrentHashMap.newKeySet();
    private final List<TransportVehicle> assignedVehicles = Collections.synchronizedList(new ArrayList<>());
    private Map<String,List<String>> buildingWorkers = new ConcurrentHashMap<>();
    private Map<String,Boolean> lockedSlots = new ConcurrentHashMap<>();

    // ✅ LOGISTICS DATA
    private volatile LogisticsHexData logisticsData;
    private volatile LivestockFarm livestockFarm;
    
    // ✅ VALIDATION CONSTRUCTOR
    public SafeHexDetails (String hexKey) {
        if (hexKey == null || hexKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Hex key cannot be null or empty");
        }
        this.hexKey = hexKey.trim();
        initializeDefaults();
        validate();
    }
    
    private void initializeDefaults() {
        // Initialisation thread-safe des collections
        selectedResourceTypes.put("main", null);
        selectedResourceTypes.put("aux", null);
        selectedResourceTypes.put("fort", null);
        lockedSlots.put("main", false);
        lockedSlots.put("aux",  false);
        lockedSlots.put("fort", false);
        selectedResourceProductions.put("main", 0.0);
        selectedResourceProductions.put("aux", 0.0);
        selectedResourceProductions.put("fort", 0.0);
        buildingWorkers.put("main", new ArrayList<>());
        buildingWorkers.put("aux",  new ArrayList<>());
        buildingWorkers.put("fort", new ArrayList<>());

        this.logisticsData = new LogisticsHexData();
        this.livestockFarm = new LivestockFarm(hexKey);
    }
    
    // ✅ VALIDATION METHOD
    private void validate() {
        if (hexKey == null || hexKey.trim().isEmpty()) {
            throw new IllegalStateException("Invalid hex state: key cannot be null/empty");
        }
        if (mainBuildingIndex < 0 || auxBuildingIndex < 0 || fortBuildingIndex < 0) {
            throw new IllegalStateException("Invalid building indices");
        }
        if (mainWorkerCount < 0 || auxWorkerCount < 0 || fortWorkerCount < 0) {
            throw new IllegalStateException("Invalid worker counts");
        }
    }
    
    // ✅ COPY CONSTRUCTOR POUR SNAPSHOTS
    private SafeHexDetails(SafeHexDetails other) {
        this.hexKey = other.hexKey;
        this.factionClaim = other.factionClaim;
        this.mainBuildingIndex = other.mainBuildingIndex;
        this.auxBuildingIndex = other.auxBuildingIndex;
        this.fortBuildingIndex = other.fortBuildingIndex;
        this.mainWorkerCount = other.mainWorkerCount;
        this.auxWorkerCount = other.auxWorkerCount;
        this.fortWorkerCount = other.fortWorkerCount;
        
        // Deep copy des collections
        this.selectedResourceTypes.putAll(other.selectedResourceTypes);
        this.selectedResourceProductions.putAll(other.selectedResourceProductions);
        this.discoveredByFaction.addAll(other.discoveredByFaction);
        
        synchronized(other.assignedVehicles) {
            this.assignedVehicles.addAll(other.assignedVehicles);
        }
        
        this.logisticsData = new LogisticsHexData();
        this.livestockFarm = new LivestockFarm(other.hexKey);
    }
    
    // ✅ THREAD-SAFE DEEP COPY
    public SafeHexDetails deepCopy() {
        return new SafeHexDetails(this);
    }
    
    // ✅ SÉRIALISATION SÉCURISÉE
    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        validate(); // Validation avant sérialisation
        
        out.writeUTF(VALIDATION_KEY); // Header magique
        out.writeUTF(hexKey);
        out.writeUTF(factionClaim);
        out.writeInt(mainBuildingIndex);
        out.writeInt(auxBuildingIndex);
        out.writeInt(fortBuildingIndex);
        out.writeInt(mainWorkerCount);
        out.writeInt(auxWorkerCount);
        out.writeInt(fortWorkerCount);
        
        // Collections thread-safe
        out.writeObject(new HashMap<>(selectedResourceTypes));
        out.writeObject(new HashMap<>(selectedResourceProductions));
        out.writeObject(new HashSet<>(discoveredByFaction));
        
        synchronized(assignedVehicles) {
            out.writeObject(new ArrayList<>(assignedVehicles));
        }
        
        out.writeObject(logisticsData);
        out.writeObject(livestockFarm);
        
        out.writeLong(calculateChecksum()); // Checksum pour validation
    }
    
    // ✅ DÉSÉRIALISATION SÉCURISÉE
    @Serial
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        String validationKey = in.readUTF();
        if (!VALIDATION_KEY.equals(validationKey)) {
            throw new InvalidObjectException("Invalid validation key: " + validationKey);
        }
        
        hexKey = in.readUTF();
        factionClaim = in.readUTF();
        mainBuildingIndex = in.readInt();
        auxBuildingIndex = in.readInt();
        fortBuildingIndex = in.readInt();
        mainWorkerCount = in.readInt();
        auxWorkerCount = in.readInt();
        fortWorkerCount = in.readInt();
        
        Map<String, String> tempResourceTypes = (Map<String, String>) in.readObject();
        Map<String, Double> tempResourceProductions = (Map<String, Double>) in.readObject();
        Set<String> tempDiscovered = (Set<String>) in.readObject();
        List<TransportVehicle> tempVehicles = (List<TransportVehicle>) in.readObject();
        
        logisticsData = (LogisticsHexData) in.readObject();
        livestockFarm = (LivestockFarm) in.readObject();
        
        long expectedChecksum = in.readLong();
        
        // Reconstruction thread-safe des collections
        selectedResourceTypes.clear();
        selectedResourceTypes.putAll(tempResourceTypes);
        
        selectedResourceProductions.clear();  
        selectedResourceProductions.putAll(tempResourceProductions);
        
        discoveredByFaction.clear();
        discoveredByFaction.addAll(tempDiscovered);
        
        assignedVehicles.clear();
        assignedVehicles.addAll(tempVehicles);
        
        validate(); // Validation après désérialisation
        
        long actualChecksum = calculateChecksum();
        if (expectedChecksum != actualChecksum) {
            throw new InvalidObjectException("Data corruption detected in hex: " + hexKey);
        }
    }
    
    // ✅ CHECKSUM POUR VALIDATION
    private long calculateChecksum() {
        return Objects.hash(
            hexKey, factionClaim, 
            mainBuildingIndex, auxBuildingIndex, fortBuildingIndex,
            mainWorkerCount, auxWorkerCount, fortWorkerCount,
            selectedResourceTypes.size(), selectedResourceProductions.size(),
            discoveredByFaction.size(), assignedVehicles.size()
        );
    }
    
    // ✅ VEHICLE MANAGEMENT - THREAD-SAFE
    public boolean addVehicle(TransportVehicle vehicle) {
        if (vehicle == null || vehicle.getAssignedHexKey() == null) {
            return false;
        }
        synchronized(assignedVehicles) {
            return assignedVehicles.add(vehicle);
        }
    }
    
    public boolean removeVehicle(TransportVehicle vehicle) {
        synchronized(assignedVehicles) {
            return assignedVehicles.remove(vehicle);
        }
    }
    
    public void clearVehicles() {
        synchronized(assignedVehicles) {
            assignedVehicles.clear();
        }
    }
    
    public List<TransportVehicle> getAssignedVehicles() {
        synchronized(assignedVehicles) {
            return new ArrayList<>(assignedVehicles);
        }
    }
    
    //FACTION DISCOVERY - THREAD-SAFE
    public boolean isDiscoveredBy(String factionId) {
        return factionId != null && discoveredByFaction.contains(factionId);
    }
    public Set<String> getDiscoveredByFaction() {
        return new HashSet<>(discoveredByFaction);
    }
    public void setDiscoveredByFaction(Set<String> factions) {
        discoveredByFaction.clear();
        if (factions != null) {
            discoveredByFaction.addAll(factions);
        }
    }
    //GETTERS/SETTERS STANDARDS avec validation
    public String getHexKey() { return hexKey; }
    
    public String getFactionClaim() { return factionClaim; }
    public void setFactionClaim(String factionClaim) {
        this.factionClaim = (factionClaim != null) ? factionClaim : "Free";
    }
    
    // Building indices avec validation
    public int getMainBuildingIndex() { return mainBuildingIndex; }
    public void setMainBuildingIndex(int index) {
        if (index < 0) throw new IllegalArgumentException("Building index cannot be negative");
        this.mainBuildingIndex = index;
    }
    
    public int getAuxBuildingIndex() { return auxBuildingIndex; }
    public void setAuxBuildingIndex(int index) {
        if (index < 0) throw new IllegalArgumentException("Building index cannot be negative");
        this.auxBuildingIndex = index;
    }
    
    public int getFortBuildingIndex() { return fortBuildingIndex; }
    public void setFortBuildingIndex(int index) {
        if (index < 0) throw new IllegalArgumentException("Building index cannot be negative");
        this.fortBuildingIndex = index;
    }
    
    // Worker counts avec validation
    public int getMainWorkerCount() { return mainWorkerCount; }
    public void setMainWorkerCount(int count) {
        if (count < 0) throw new IllegalArgumentException("Worker count cannot be negative");
        this.mainWorkerCount = count;
    }
    
    public int getAuxWorkerCount() { return auxWorkerCount; }
    public void setAuxWorkerCount(int count) {
        if (count < 0) throw new IllegalArgumentException("Worker count cannot be negative");
        this.auxWorkerCount = count;
    }
    
    public int getFortWorkerCount() { return fortWorkerCount; }
    public void setFortWorkerCount(int count) {
        if (count < 0) throw new IllegalArgumentException("Worker count cannot be negative");
        this.fortWorkerCount = count;
    }
    
    public int getTotalWorkers() {
        return mainWorkerCount + auxWorkerCount + fortWorkerCount;
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
    public void lockSlot(String slot)                  { lockedSlots.put(slot, true); }
    public boolean isSlotLocked(String slot)           { return lockedSlots.getOrDefault(slot, false); }

    // Logistics data
    public LogisticsHexData getLogisticsData() { 
        return logisticsData != null ? logisticsData : new LogisticsHexData();
    }
    public void setLogisticsData(LogisticsHexData logisticsData) {
        this.logisticsData = logisticsData != null ? logisticsData : new LogisticsHexData();
    }
    
    // Resource management thread-safe
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

    public Map<String, List<String>> getBuildingWorkers() {
        return new HashMap<>(buildingWorkers);
    }
    public void setBuildingWorkers(Map<String, List<String>> workers) {
        buildingWorkers.clear();
        buildingWorkers.putAll(workers);
    }

    public Map<String, Boolean> getLockedSlots() {
        return new HashMap<>(lockedSlots);}
    public void setLockedSlots(Map<String, Boolean> slots) {
        lockedSlots.clear();
        lockedSlots.putAll(slots);
    }

    public LivestockFarm getLivestockFarm() { return livestockFarm; }
    public void setLivestockFarm(LivestockFarm farm) {
        this.livestockFarm = farm != null ? farm : new LivestockFarm(hexKey);
    }



    @Override
    public String toString() {
        return String.format("HexDetails{key='%s', faction='%s', main=%d, aux=%d, fort=%d, workers=%d}", 
            hexKey, factionClaim, mainBuildingIndex, auxBuildingIndex, fortBuildingIndex, getTotalWorkers());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SafeHexDetails)) return false;
        SafeHexDetails that = (SafeHexDetails) o;
        return Objects.equals(hexKey, that.hexKey);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(hexKey);
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
