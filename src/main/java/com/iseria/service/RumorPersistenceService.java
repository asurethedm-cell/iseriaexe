package com.iseria.service;

import com.iseria.domain.Rumor;

import java.io.*;
import java.util.List;

public class RumorPersistenceService {
    private static final String RUMORS_FILE = "rumors.dat";

    public void saveRumors(List<Rumor> rumors) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(RUMORS_FILE))) {
            oos.writeObject(rumors);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Rumor> loadRumors() throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(RUMORS_FILE))) {
            return (List<Rumor>) ois.readObject();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}