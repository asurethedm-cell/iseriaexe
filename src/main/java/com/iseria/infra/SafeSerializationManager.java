package com.iseria.infra;

import com.iseria.domain.HexDetails;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Gestionnaire central de sérialisation sécurisée pour l'application Iseria.
 * Élimine les "phantom objects", EOF exceptions, et corruption de données.
 */
public class SafeSerializationManager {
    private static final String MAGIC_HEADER = "ISERIA_SERIALIZE_V3.0";
    private static final int BUFFER_SIZE = 8192;
    
    private final ReentrantLock serializationLock = new ReentrantLock();
    
    /**
     * Conteneur thread-safe pour encapsuler les données de jeu
     */
    public static class GameStateContainer implements Serializable {
        @Serial
        private static final long serialVersionUID = 2L;
        
        private final String magicHeader;
        private final long timestamp;
        private final String dataType;
        private final Object data;
        private final long checksum;
        private final boolean isComplete;
        
        public GameStateContainer(String dataType, Object data) {
            this.magicHeader = MAGIC_HEADER;
            this.timestamp = System.currentTimeMillis();
            this.dataType = dataType;
            this.data = data;
            this.checksum = calculateChecksum(data);
            this.isComplete = true;
        }
        
        private long calculateChecksum(Object obj) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(obj);
                oos.flush();
                
                CRC32 crc = new CRC32();
                crc.update(baos.toByteArray());
                return crc.getValue();
                
            } catch (IOException e) {
                System.err.println("Warning: Could not calculate checksum - " + e.getMessage());
                return 0L;
            }
        }
        
        // Validation lors de la désérialisation
        private Object readResolve() throws InvalidObjectException {
            if (!MAGIC_HEADER.equals(magicHeader)) {
                throw new InvalidObjectException("Invalid magic header: " + magicHeader);
            }
            if (!isComplete) {
                throw new InvalidObjectException("Incomplete data container");
            }
            if (data == null) {
                throw new InvalidObjectException("Null data in container");
            }
            
            // Vérification du checksum si possible
            long actualChecksum = calculateChecksum(data);
            if (checksum != 0L && actualChecksum != checksum) {
                System.err.println("Warning: Checksum mismatch - data may be corrupted");
            }
            
            return this;
        }
        
        public Object getData() { return data; }
        public String getDataType() { return dataType; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * Sauvegarde sécurisée avec atomic operations
     */
    public <T> boolean saveSecurely(T object, Path filePath, String dataType) {
        if (!serializationLock.tryLock()) {
            System.err.println("Serialization in progress, skipping save");
            return false;
        }
        
        try {
            if (object == null) {
                throw new IllegalArgumentException("Cannot serialize null object");
            }
            
            Path directory = filePath.getParent();
            if (directory != null) {
                Files.createDirectories(directory);
            }
            
            // Fichiers temporaires pour atomic operations
            Path tempFile = filePath.resolveSibling(filePath.getFileName() + ".tmp");
            Path backupFile = filePath.resolveSibling(filePath.getFileName() + ".backup");
            
            // Encapsulation des données
            GameStateContainer container = new GameStateContainer(dataType, object);
            
            // Écriture dans fichier temporaire
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(tempFile), BUFFER_SIZE))) {
                
                // Header de validation supplémentaire
                oos.writeUTF(MAGIC_HEADER);
                oos.writeLong(System.currentTimeMillis());
                
                // Données encapsulées
                oos.writeObject(container);
                
                // Marqueur de fin
                oos.writeUTF("END_OF_STREAM");
                oos.flush();
            }
            
            // Backup de l'ancien fichier
            if (Files.exists(filePath)) {
                Files.copy(filePath, backupFile, StandardCopyOption.REPLACE_EXISTING);
            }
            
            // Atomic move du temporary vers le final
            Files.move(tempFile, filePath, StandardCopyOption.ATOMIC_MOVE);
            
            System.out.println("✅ Saved " + dataType + " to " + filePath.getFileName());
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error saving " + dataType + ": " + e.getMessage());
            e.printStackTrace();
            
            // Cleanup du fichier temporaire
            try {
                Path tempFile = filePath.resolveSibling(filePath.getFileName() + ".tmp");
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {}
            
            return false;
            
        } finally {
            serializationLock.unlock();
        }
    }
    
    /**
     * Chargement sécurisé avec validation complète
     */
    @SuppressWarnings("unchecked")
    public <T> T loadSecurely(Path filePath, String expectedDataType, Class<T> expectedClass) {
        serializationLock.lock();
        try {
            if (!Files.exists(filePath)) {
                System.out.println("File does not exist: " + filePath);
                return null;
            }
            
            // Essayer le fichier principal
            T result = attemptLoad(filePath, expectedDataType, expectedClass);
            if (result != null) {
                return result;
            }
            
            // Essayer le backup si échec
            Path backupFile = filePath.resolveSibling(filePath.getFileName() + ".backup");
            if (Files.exists(backupFile)) {
                System.out.println("Primary file failed, trying backup...");
                result = attemptLoad(backupFile, expectedDataType, expectedClass);
                if (result != null) {
                    System.out.println("✅ Loaded from backup file");
                    return result;
                }
            }
            
            System.err.println("❌ All load attempts failed for " + filePath.getFileName());
            return null;
            
        } finally {
            serializationLock.unlock();
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> T attemptLoad(Path filePath, String expectedDataType, Class<T> expectedClass) {
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(filePath), BUFFER_SIZE))) {
            
            // Vérifier header
            String header = ois.readUTF();
            if (!MAGIC_HEADER.equals(header)) {
                System.err.println("Invalid file header: " + header);
                return null;
            }
            
            // Timestamp (pour info)
            long timestamp = ois.readLong();
            
            // Lire le container
            Object containerObj = ois.readObject();
            if (!(containerObj instanceof GameStateContainer)) {
                System.err.println("Invalid container type: " + containerObj.getClass());
                return null;
            }
            
            GameStateContainer container = (GameStateContainer) containerObj;
            
            // Validation du type de données
            if (!expectedDataType.equals(container.getDataType())) {
                System.err.println("Data type mismatch: expected " + expectedDataType + 
                                 ", got " + container.getDataType());
                return null;
            }
            
            // Vérifier marqueur de fin
            String endMarker = ois.readUTF();
            if (!"END_OF_STREAM".equals(endMarker)) {
                System.err.println("Invalid end marker: " + endMarker);
                return null;
            }
            
            // Validation du type de classe
            Object data = container.getData();
            if (!expectedClass.isInstance(data)) {
                System.err.println("Class type mismatch: expected " + expectedClass + 
                                 ", got " + data.getClass());
                return null;
            }
            
            System.out.println("✅ Loaded " + expectedDataType + " from " + filePath.getFileName() + 
                             " (timestamp: " + new java.util.Date(timestamp) + ")");
            
            return (T) data;
            
        } catch (EOFException e) {
            System.err.println("❌ Premature end of file: " + filePath.getFileName());
            return null;
            
        } catch (Exception e) {
            System.err.println("❌ Error loading " + filePath.getFileName() + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Validation d'intégrité d'un fichier sans le charger complètement
     */
    public boolean validateFileIntegrity(Path filePath) {
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(filePath)))) {
            
            // Vérifier header
            String header = ois.readUTF();
            if (!MAGIC_HEADER.equals(header)) {
                return false;
            }
            
            // Skip timestamp
            ois.readLong();
            
            // Vérifier que le container peut être lu
            Object container = ois.readObject();
            if (!(container instanceof GameStateContainer)) {
                return false;
            }
            
            // Vérifier marqueur de fin
            String endMarker = ois.readUTF();
            if (!"END_OF_STREAM".equals(endMarker)) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Nettoyage des fichiers temporaires et backup anciens
     */
    public void cleanupOldFiles(Path directory, int maxBackupsToKeep) {
        try {
            Files.list(directory)
                .filter(path -> path.toString().endsWith(".tmp"))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        System.out.println("Cleaned temp file: " + path.getFileName());
                    } catch (IOException e) {
                        System.err.println("Could not delete temp file: " + path);
                    }
                });
            
            // Optionnel: nettoyer les vieux backups
            Files.list(directory)
                .filter(path -> path.toString().endsWith(".backup"))
                .sorted((p1, p2) -> {
                    try {
                        return Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1));
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .skip(maxBackupsToKeep)
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        System.out.println("Cleaned old backup: " + path.getFileName());
                    } catch (IOException e) {
                        System.err.println("Could not delete backup: " + path);
                    }
                });
                
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }
    
    /**
     * Informations de diagnostic sur un fichier
     */
    public void printFileInfo(Path filePath) {
        System.out.println("=== FILE INFO: " + filePath.getFileName() + " ===");
        
        if (!Files.exists(filePath)) {
            System.out.println("File does not exist");
            return;
        }
        
        try {
            System.out.println("Size: " + Files.size(filePath) + " bytes");
            System.out.println("Modified: " + Files.getLastModifiedTime(filePath));
            System.out.println("Readable: " + Files.isReadable(filePath));
            System.out.println("Writable: " + Files.isWritable(filePath));
            System.out.println("Integrity: " + (validateFileIntegrity(filePath) ? "✅ Valid" : "❌ Invalid"));
            
        } catch (IOException e) {
            System.err.println("Error reading file info: " + e.getMessage());
        }
        
        System.out.println("=====================================");
    }
    // Dans votre repository ou service principal
    public class ImprovedGameStateManager {
        private final SafeSerializationManager serializer = new SafeSerializationManager();

        // Sauvegarde des données hex
        public boolean saveHexData(Map<String, HexDetails> hexData) {
            Path filePath = Paths.get("gamedata", "hexes.dat");
            return serializer.saveSecurely(hexData, filePath, "HEX_DATA");
        }

        // Chargement des données hex
        @SuppressWarnings("unchecked")
        public Map<String, HexDetails> loadHexData() {
            Path filePath = Paths.get("gamedata", "hexes.dat");
            return serializer.loadSecurely(filePath, "HEX_DATA", Map.class);
        }

        // Validation périodique
        public void validateDataIntegrity() {
            Path dataDir = Paths.get("gamedata");
            try {
                Files.list(dataDir)
                        .filter(path -> path.toString().endsWith(".dat"))
                        .forEach(serializer::printFileInfo);
            } catch (IOException e) {
                System.err.println("Error listing data files: " + e.getMessage());
            }
        }

        // Nettoyage périodique
        public void performMaintenance() {
            Path dataDir = Paths.get("gamedata");
            serializer.cleanupOldFiles(dataDir, 3); // Garder 3 backups max
        }
    }
}

