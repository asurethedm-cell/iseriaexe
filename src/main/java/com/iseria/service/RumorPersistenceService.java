package com.iseria.service;

import com.iseria.domain.Rumor;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RumorPersistenceService {
    private static final String RUMORS_FILE = "rumors.dat";

    public void saveRumors(List<Rumor> rumors) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(RUMORS_FILE))) {
            oos.writeObject(rumors);
            System.out.println("✅ Sauvegarde réussie: " + rumors.size() + " rumeurs");
        } catch (IOException e) {
            System.err.println("❌ Erreur sauvegarde: " + e.getMessage());
            throw new RuntimeException("Échec sauvegarde des rumeurs", e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Rumor> loadRumors() {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(RUMORS_FILE))) {
            List<Rumor> loaded = (List<Rumor>) ois.readObject();
            System.out.println("✅ Chargement réussi: " + loaded.size() + " rumeurs");
            return loaded;
        } catch (FileNotFoundException e) {
            System.out.println("ℹ️ Aucun fichier de sauvegarde trouvé");
            return new ArrayList<>(); // ✨ Retourner liste vide au lieu d'exception
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("❌ Erreur chargement: " + e.getMessage());
            throw new RuntimeException("Échec chargement des rumeurs", e);
        }
    }
}
