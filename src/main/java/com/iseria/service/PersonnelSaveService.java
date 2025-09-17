package com.iseria.service;

import java.io.*;
import java.util.*;

public class PersonnelSaveService {

    public static class PersonnelSaveData {
        public Map<String, List<SavedPersonnel>> factionPersonnel = new HashMap<>();

        public static class SavedPersonnel {
            public String personnelId;
            public String workerTypeName;
            public String name;
            public String assignedBuilding;
            public String assignedHex;
            public double currentSalary;
            public double foodConsumption;
            public boolean isAssigned;
            public long hireDate;

            // Constructeurs et getters/setters
        }
    }

    public void savePersonnelData(String username, String factionId, PersonnelSaveData data) {
        String fileName = getPersonnelFileName(username, factionId);

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(data);
            System.out.println("Personnel data saved: " + fileName);
        } catch (IOException e) {
            System.err.println("Erreur sauvegarde personnel: " + e.getMessage());
        }
    }

    public PersonnelSaveData loadPersonnelData(String username, String factionId) {
        String fileName = getPersonnelFileName(username, factionId);
        File file = new File(fileName);

        if (!file.exists()) {
            return new PersonnelSaveData();
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
            return (PersonnelSaveData) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erreur chargement personnel: " + e.getMessage());
            return new PersonnelSaveData();
        }
    }

    private String getPersonnelFileName(String username, String factionId) {
        String documentsPath = System.getProperty("user.home") + File.separator + "Documents";
        String saveDir = documentsPath + File.separator + "IseriaDivers" + File.separator + username.toLowerCase();

        File dir = new File(saveDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        return saveDir + File.separator + "personnel_" + factionId + ".dat";
    }
}
