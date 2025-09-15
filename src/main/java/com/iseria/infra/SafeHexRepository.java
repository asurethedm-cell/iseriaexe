package com.iseria.infra;

import com.iseria.domain.HexDetails;
import com.iseria.domain.IHexRepository;
import com.iseria.domain.SafeHexDetails;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SafeHexRepository implements IHexRepository {
    private static final String MAGIC_HEADER = "ISERIA_HEXREPO_V2.0";
    private static final String DATA_FILE = "hexData.dat";
    private static final String BACKUP_SUFFIX = ".backup";
    
    // ✅ THREAD-SAFE STORAGE
    private final ConcurrentHashMap<String, SafeHexDetails> hexGrid = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ScheduledExecutorService saveExecutor = Executors.newSingleThreadScheduledExecutor();
    
    // ✅ ATOMIC OPERATIONS
    private volatile boolean isInitialized = false;
    private volatile boolean isSaving = false;
    private final AtomicLong lastModified = new AtomicLong(0);
    
    public SafeHexRepository() {
        // Sauvegarde automatique toutes les 30 secondes si modifié
        saveExecutor.scheduleAtFixedRate(this::autoSave, 30, 30, TimeUnit.SECONDS);
        
        // Hook pour sauvegarde d'urgence à la fermeture
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            saveExecutor.shutdown();
            try {
                if (!saveExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    saveExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            forceSave();
        }));
        
        loadHexGridFromFile();
    }

    private void loadHexGridFromFile() {
        Path dataPath = Paths.get(DATA_FILE);
        Path backupPath = Paths.get(DATA_FILE + BACKUP_SUFFIX);

        // Essayer d'abord le fichier principal
        if (Files.exists(dataPath)) {
            if (tryLoadFromFile(dataPath)) {
                return;
            }
            System.err.println("Primary file corrupted, trying backup...");
        }

        // Essayer le backup
        if (Files.exists(backupPath)) {
            if (tryLoadFromFile(backupPath)) {
                System.out.println("Loaded from backup file");
                return;
            }
            System.err.println("Backup file also corrupted");
        }

        // Si aucun fichier valide, initialisation vide
        System.out.println("No valid data file found, starting with empty repository");
        hexGrid.clear();
    }

    @Override
    public Map<String, SafeHexDetails> loadSafeAll() {
        lock.readLock().lock();
        try {
            Map<String, SafeHexDetails> result = new HashMap<>();
            for (Map.Entry<String, SafeHexDetails> entry : hexGrid.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    result.put(entry.getKey(), entry.getValue().deepCopy());
                }
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void save(SafeHexDetails details) {

        hexGrid.put(details.getHexKey(), details);
        saveHexGridToFile();
    }
    public void saveHexGridToFile() {
        if (isSaving) {
            return; // Éviter les sauvegardes concurrentes
        }

        isSaving = true;
        Path dataPath = Paths.get(DATA_FILE);
        Path backupPath = Paths.get(DATA_FILE + BACKUP_SUFFIX);
        Path tempPath = Paths.get(DATA_FILE + ".tmp");

        lock.readLock().lock();
        try {
            // 1. Créer un snapshot des données
            Map<String, SafeHexDetails> snapshot = new HashMap<>();
            for (Map.Entry<String, SafeHexDetails> entry : hexGrid.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    snapshot.put(entry.getKey(), entry.getValue().deepCopy());
                }
            }

            // 2. Écrire dans un fichier temporaire
            writeSnapshotToFile(snapshot, tempPath);

            // 3. Backup de l'ancien fichier si il existe
            if (Files.exists(dataPath)) {
                Files.copy(dataPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // 4. Atomic move du temp vers le final
            Files.move(tempPath, dataPath, StandardCopyOption.ATOMIC_MOVE);

            lastModified.set(0); // Reset modification flag
            System.out.println("Hex data saved successfully (" + snapshot.size() + " hexes)");

        } catch (Exception e) {
            System.err.println("Error saving hex data: " + e.getMessage());
            e.printStackTrace();

            // Cleanup du fichier temporaire
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {}

        } finally {
            lock.readLock().unlock();
            isSaving = false;
        }
    }
    @Override
    public void addAllHexes(int rows, int cols) {
        if (isInitialized) {
            System.out.println("Repository already initialized, skipping addAllHexes");
            return;
        }
        
        lock.writeLock().lock();
        try {
            System.out.println("Initializing " + (rows * cols) + " hexes...");
            
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    String hexKey = "hex_" + col + "_" + row;
                    if (!hexGrid.containsKey(hexKey)) {
                        SafeHexDetails newHex = new SafeHexDetails(hexKey);
                        hexGrid.put(hexKey, newHex);
                    }
                }
            }
            
            isInitialized = true;
            markModified();
            System.out.println("Repository initialized with " + hexGrid.size() + " hexes");
            
        } finally {
            lock.writeLock().unlock();
        }
        
        // Sauvegarder immédiatement après l'initialisation
        forceSave();
    }
    @Override
    public void clearAllFactionClaims() {
        lock.writeLock().lock();
        try {
            for (SafeHexDetails hex : hexGrid.values()) {
                if (hex != null && !"Free".equals(hex.getFactionClaim())) {
                    hex.setFactionClaim("Free");
                }
            }
            markModified();
            System.out.println("All faction claims cleared");
        } finally {
            lock.writeLock().unlock();
        }
    }
    @Override
    public SafeHexDetails getSafeHexDetails(String hexKey) {
        if (hexKey == null || hexKey.trim().isEmpty()) {
            return null;
        }
        
        lock.readLock().lock();
        try {
            SafeHexDetails original = hexGrid.get(hexKey.trim());
            return original != null ? original.deepCopy() : null;
        } finally {
            lock.readLock().unlock();
        }
    }
    @Override
    public void updateSafeHexDetails(String hexKey, SafeHexDetails details) {
        if (hexKey == null || hexKey.trim().isEmpty() || details == null) {
            throw new IllegalArgumentException("Hex key and details cannot be null");
        }
        
        if (!hexKey.trim().equals(details.getHexKey())) {
            throw new IllegalArgumentException("Hex key mismatch: " + hexKey + " != " + details.getHexKey());
        }
        
        lock.writeLock().lock();
        try {
            // Sauvegarder une copie pour éviter les modifications externes
            hexGrid.put(hexKey.trim(), details.deepCopy());
            markModified();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int[] getHexPosition(String hexKey) {
        if (hexKey == null || !hexKey.startsWith("hex_")) {
            return null;
        }
        
        try {
            String[] parts = hexKey.split("_");
            if (parts.length != 3) return null;
            
            int col = Integer.parseInt(parts[1]);
            int row = Integer.parseInt(parts[2]);
            
            // Calcul position pixelaire selon votre système
            double unitSize = 70; // HEX_SIZE from Mondes.java
            double horiz = 1.5 * unitSize;
            double vert = Math.sqrt(3) * unitSize;
            
            double cx = col * horiz;
            double cy = row * vert + ((col & 1) == 1 ? vert * 0.5 : 0);
            
            return new int[]{(int)cx, (int)cy};
            
        } catch (NumberFormatException e) {
            System.err.println("Invalid hex key format: " + hexKey);
            return null;
        }
    }
    

    
    // ✅ GESTION DE SAUVEGRADE SÉCURISÉE
    private void markModified() {
        lastModified.set(System.currentTimeMillis());
    }
    
    private void autoSave() {
        if (lastModified.get() > 0 && !isSaving) {
            saveHexGridToFile();
        }
    }
    
    private void forceSave() {
        try {
            saveHexGridToFile();
        } catch (Exception e) {
            System.err.println("Emergency save failed: " + e.getMessage());
        }
    }
    

    
    private void writeSnapshotToFile(Map<String, SafeHexDetails> snapshot, Path filePath) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(filePath)))) {
            
            // Header magique
            oos.writeUTF(MAGIC_HEADER);
            
            // Timestamp
            oos.writeLong(System.currentTimeMillis());
            
            // Nombre d'hexes (pour validation)
            oos.writeInt(snapshot.size());
            
            // Calcul checksum du snapshot
            long checksum = calculateSnapshotChecksum(snapshot);
            oos.writeLong(checksum);
            
            // Écrire tous les hexes
            for (Map.Entry<String, SafeHexDetails> entry : snapshot.entrySet()) {
                oos.writeUTF(entry.getKey());
                oos.writeObject(entry.getValue());
            }
            
            // End marker
            oos.writeUTF("END_OF_DATA");
            oos.flush();
        }
    }

    private boolean tryLoadFromFile(Path filePath) {
        lock.writeLock().lock();
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(filePath)))) {
            
            // Vérifier header magique
            String header = ois.readUTF();
            if (!MAGIC_HEADER.equals(header)) {
                System.err.println("Invalid file header: " + header);
                return false;
            }
            
            // Lire metadata
            long timestamp = ois.readLong();
            int expectedCount = ois.readInt();
            long expectedChecksum = ois.readLong();
            
            // Lire les données
            Map<String, SafeHexDetails> loadedData = new HashMap<>();
            int loadedCount = 0;
            
            try {
                while (loadedCount < expectedCount) {
                    String hexKey = ois.readUTF();
                    SafeHexDetails hex = (SafeHexDetails) ois.readObject();
                    
                    if (hexKey != null && hex != null && hexKey.equals(hex.getHexKey())) {
                        loadedData.put(hexKey, hex);
                        loadedCount++;
                    } else {
                        System.err.println("Phantom hex detected: key=" + hexKey + ", hex=" + hex);
                        return false;
                    }
                }
                
                // Vérifier end marker
                String endMarker = ois.readUTF();
                if (!"END_OF_DATA".equals(endMarker)) {
                    System.err.println("Invalid end marker: " + endMarker);
                    return false;
                }
                
            } catch (EOFException e) {
                System.err.println("Premature end of file, expected " + expectedCount + " hexes, got " + loadedCount);
                return false;
            }
            
            // Vérifier checksum
            long actualChecksum = calculateSnapshotChecksum(loadedData);
            if (expectedChecksum != actualChecksum) {
                System.err.println("Checksum mismatch - data corruption detected");
                return false;
            }
            
            // Si tout est OK, remplacer les données
            hexGrid.clear();
            hexGrid.putAll(loadedData);
            
            System.out.println("Loaded " + hexGrid.size() + " hexes from " + filePath.getFileName() + 
                             " (timestamp: " + new Date(timestamp) + ")");
            
            isInitialized = !hexGrid.isEmpty();
            return true;
            
        } catch (Exception e) {
            System.err.println("Error loading from " + filePath.getFileName() + ": " + e.getMessage());
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private long calculateSnapshotChecksum(Map<String, SafeHexDetails> snapshot) {
        return snapshot.entrySet().stream()
            .filter(entry -> entry.getKey() != null && entry.getValue() != null)
            .mapToLong(entry -> Objects.hash(entry.getKey(), entry.getValue().getHexKey()))
            .sum();
    }
    
    // ✅ MÉTHODES DE DEBUGGING
    public void printRepositoryStats() {
        lock.readLock().lock();
        try {
            System.out.println("=== REPOSITORY STATISTICS ===");
            System.out.println("Total hexes: " + hexGrid.size());
            System.out.println("Initialized: " + isInitialized);
            System.out.println("Currently saving: " + isSaving);
            System.out.println("Last modified: " + (lastModified.get() > 0 ? new Date(lastModified.get()) : "Never"));
            
            // Compter par faction
            Map<String, Integer> factionCounts = new HashMap<>();
            int nullKeys = 0;
            int nullValues = 0;
            
            for (Map.Entry<String, SafeHexDetails> entry : hexGrid.entrySet()) {
                if (entry.getKey() == null) {
                    nullKeys++;
                    continue;
                }
                if (entry.getValue() == null) {
                    nullValues++;
                    continue;
                }
                
                String faction = entry.getValue().getFactionClaim();
                factionCounts.merge(faction, 1, Integer::sum);
            }
            
            System.out.println("Faction distribution:");
            factionCounts.forEach((faction, count) -> 
                System.out.println("  " + faction + ": " + count));
            
            if (nullKeys > 0) System.err.println("⚠️  NULL KEYS: " + nullKeys);
            if (nullValues > 0) System.err.println("⚠️  NULL VALUES: " + nullValues);
            
            System.out.println("===============================");
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public void validateIntegrity() {
        lock.readLock().lock();
        try {
            List<String> issues = new ArrayList<>();
            
            for (Map.Entry<String, SafeHexDetails> entry : hexGrid.entrySet()) {
                String mapKey = entry.getKey();
                SafeHexDetails hex = entry.getValue();
                
                if (mapKey == null) {
                    issues.add("Found null key in hexGrid");
                    continue;
                }
                
                if (hex == null) {
                    issues.add("Found null hex for key: " + mapKey);
                    continue;
                }
                
                if (!mapKey.equals(hex.getHexKey())) {
                    issues.add("Key mismatch: map=" + mapKey + ", hex=" + hex.getHexKey());
                }
                
                // Validation des données hex
                try {
                    hex.deepCopy(); // Test de copie
                } catch (Exception e) {
                    issues.add("Hex copy failed for " + mapKey + ": " + e.getMessage());
                }
            }
            
            if (issues.isEmpty()) {
                System.out.println("✅ Repository integrity check passed");
            } else {
                System.err.println("❌ Repository integrity issues found:");
                issues.forEach(issue -> System.err.println("  - " + issue));
            }
            
        } finally {
            lock.readLock().unlock();
        }
    }
}
