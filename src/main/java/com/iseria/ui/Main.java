package com.iseria.ui;

import com.iseria.domain.*;
import com.iseria.infra.*;
import com.iseria.service.SoundAudioService;
import org.apache.commons.io.output.ByteArrayOutputStream;

import javax.swing.*;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import static com.iseria.ui.Main.SerializationDiagnostic.diagnoseCurrentState;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            IDataProvider dataProvider = new ExcelDataProvider();
            IAudioService audioService = new SoundAudioService();
            IHexRepository hexRepository = HexRepositoryFactory.create();;


            diagnoseCurrentState(hexRepository);
            var hexes = hexRepository.loadAll();
            System.out.println("Loaded " + hexes.size() + " hexes");
            new Login(dataProvider, audioService, hexRepository).setVisible(true);
        });

    }
    public class SerializationDiagnostic {
        public static void diagnoseCurrentState(IHexRepository repo) {
            System.out.println("🔍 DIAGNOSTIC SERIALIZATION ISSUES");

            Map<String, HexDetails> allHexes = repo.loadAll();
            int totalHexes = allHexes.size();
            int validHexes = 0;
            int nullKeys = 0;
            int nullValues = 0;
            int keyMismatches = 0;

            for (Map.Entry<String, HexDetails> entry : allHexes.entrySet()) {
                String key = entry.getKey();
                HexDetails hex = entry.getValue();

                if (key == null) {
                    nullKeys++;
                    System.err.println("❌ NULL KEY found!");
                    continue;
                }

                if (hex == null) {
                    nullValues++;
                    System.err.println("❌ NULL VALUE for key: " + key);
                    continue;
                }

                if (!key.equals(hex.getHexKey())) {
                    keyMismatches++;
                    System.err.println("❌ KEY MISMATCH: map='" + key + "', hex='" + hex.getHexKey() + "'");
                    continue;
                }

                validHexes++;
            }

            System.out.println("📊 RESULTS:");
            System.out.println("  Total entries: " + totalHexes);
            System.out.println("  Valid hexes: " + validHexes);
            System.out.println("  NULL keys: " + nullKeys);
            System.out.println("  NULL values: " + nullValues);
            System.out.println("  Key mismatches: " + keyMismatches);

            if (nullKeys > 0 || nullValues > 0 || keyMismatches > 0) {
                System.err.println("🚨 PHANTOM HEXES DETECTED!");
            } else {
                System.out.println("✅ No phantom hexes detected");
            }

            // Test basic serialization
            testBasicSerialization(allHexes);
        }

        private static void testBasicSerialization(Map<String, HexDetails> hexes) {
            System.out.println("🧪 Testing basic serialization...");

            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);

                oos.writeObject(hexes);
                oos.flush();
                oos.close();

                byte[] serializedData = baos.toByteArray();
                System.out.println("✅ Serialization successful, size: " + serializedData.length + " bytes");

                // Test deserialization
                ByteArrayInputStream bais = new ByteArrayInputStream(serializedData);
                ObjectInputStream ois = new ObjectInputStream(bais);

                @SuppressWarnings("unchecked")
                Map<String, HexDetails> deserialized = (Map<String, HexDetails>) ois.readObject();
                ois.close();

                System.out.println("✅ Deserialization successful, size: " + deserialized.size());

            } catch (Exception e) {
                System.err.println("❌ Basic serialization test failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
