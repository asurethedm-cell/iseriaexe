package com.iseria.infra;

import com.iseria.domain.HexDetails;
import com.iseria.domain.IHexRepository;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileHexRepository implements IHexRepository {
    private Map<String, HexDetails> hexGrid = new HashMap<>();
    private static final String SAVE_FILE = "hexgrid.dat";
    private final File file = new File(SAVE_FILE);

    public FileHexRepository() {
        hexGrid = loadHexGridFromFile();
    }

    @SuppressWarnings("unchecked")
    private Map<String, HexDetails> loadHexGridFromFile() {
        if (!file.exists()) return new HashMap<>();

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            if (obj instanceof Map) {
                Map<String, HexDetails> loaded = (Map<String, HexDetails>) obj;

                // 🔍 LOG & CLEANUP
                loaded.keySet().forEach(key -> {
                    if (key == null) {
                        //System.err.println("❌ Clé null chargée depuis hexgrid.dat");
                        Thread.dumpStack();
                    }
                });
                loaded.entrySet().removeIf(e -> e.getKey() == null);
                return loaded;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new HashMap<>();
    }

    @Override
    public Map<String, HexDetails> loadAll() {
        return new HashMap<>(hexGrid);
    }

    @Override
    public void save(HexDetails details) {

        hexGrid.put(details.getHexKey(), details);
        saveHexGridToFile();
    }

    private void saveHexGridToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(hexGrid);
        } catch (IOException e) {
            System.err.println("Error saving hex grid: " + e.getMessage());
        }
    }
    public void addHex(String hexName) {
        if (hexName == null) {
            System.err.println("❌ addHex(null) appelé !");
            Thread.dumpStack();
            return;
        }
        if (!hexGrid.containsKey(hexName)) {
            hexGrid.put(hexName, new HexDetails(hexName));
        }
    }

    public void addAllHexes(int rows, int cols) {
        boolean hasNewHexes = false;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                String hexName = "hex_" + col + "_" + row;
                if (hexName == null) { // théoriquement impossible
                    System.err.println("❌ hexName généré null dans addAllHexes");
                }
                if (!hexGrid.containsKey(hexName)) {
                    hexGrid.put(hexName, new HexDetails(hexName));
                    hasNewHexes = true;
                }
            }
        }
        if (hasNewHexes) {
            saveHexGridToFile();
        }
    }

    public void clearAllFactionClaims() {
        for (HexDetails details : hexGrid.values()) {
            details.setFactionClaim("Free");
        }
        saveHexGridToFile();
    }

    public HexDetails getHexDetails(String hexName) {
        HexDetails details = hexGrid.get(hexName);
        if (details == null) {
            details = new HexDetails(hexName);
            hexGrid.put(hexName, details);
            saveHexGridToFile();
        }
        return details;
    }

    private final ScheduledExecutorService saveExecutor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean isDirty = new AtomicBoolean(false);
    private ScheduledFuture<?> pendingSave;

    @Override
    public void updateHexDetails(String hexKey, HexDetails details) {
        String hexName = hexKey;
        if (hexName == null) {
            System.err.println("❌ Tentative de updateHexDetails(null, HexDetails)");
            new Exception().printStackTrace(System.err);
            return;
        }
        synchronized (hexGrid) {
            hexGrid.put(hexKey, new HexDetails(details));
        }
        isDirty.set(true);
        if (pendingSave != null) pendingSave.cancel(false);
        pendingSave = saveExecutor.schedule(this::saveHexGridToFileAsync,
                300, TimeUnit.MILLISECONDS);
    }

    private void saveHexGridToFileAsync() {
        if (!isDirty.getAndSet(false)) return;
        Map<String,HexDetails> snapshot;
        synchronized(hexGrid) {
            snapshot = new HashMap<>(hexGrid);
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {
            oos.writeObject(snapshot);
            System.out.println("✅ Sauvegarde asynchrone terminée");
        } catch (IOException e) {
            System.err.println("❌ Erreur sauvegarde: " + e.getMessage());
        }
    }

    public int[] getHexPosition(String hexName) {
        String[] parts = hexName.split("_");
        int row = Integer.parseInt(parts[2]);
        int col = Integer.parseInt(parts[1]);

        double hexSize = 70; // from Mondes.HEX_SIZE
        double zoomFactor = 1.0;
        double horizontalSpacing = (3.0 / 2.0) * hexSize * zoomFactor;
        double verticalSpacing = Math.sqrt(3) * hexSize * zoomFactor;

        int pixelX = (int) (col * horizontalSpacing);
        int pixelY = (int) (row * verticalSpacing);

        if (col % 2 == 1) {
            pixelY += verticalSpacing / 2;
        }

        return new int[]{pixelX, pixelY};
    }

}