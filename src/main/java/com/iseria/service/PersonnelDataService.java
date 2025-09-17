package com.iseria.service;

import com.iseria.domain.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.io.*;
import java.util.stream.Collectors;

public class PersonnelDataService {

    public static class HiredPersonnel {
        public final String personnelId;
        public final DATABASE.Workers workerType;
        public final String name;
        public final String assignedBuilding;
        public final String assignedHex;
        public final double currentSalary;
        public final double foodConsumption;
        public final boolean isAssigned;

        public HiredPersonnel(String personnelId, DATABASE.Workers workerType, String name,
                              String assignedBuilding, String assignedHex, double currentSalary,
                              double foodConsumption, boolean isAssigned) {
            this.personnelId = personnelId;
            this.workerType = workerType;
            this.name = name;
            this.assignedBuilding = assignedBuilding;
            this.assignedHex = assignedHex;
            this.currentSalary = currentSalary;
            this.foodConsumption = foodConsumption;
            this.isAssigned = isAssigned;
        }
    }

    private final Map<String, List<HiredPersonnel>> factionPersonnel = new HashMap<>();
    private final List<PersonnelObserver> observers = new CopyOnWriteArrayList<>();
    private final String saveFilePath;

    public interface PersonnelObserver {
        void onPersonnelHired(HiredPersonnel personnel);
        void onPersonnelFired(String personnelId);
        void onPersonnelAssigned(String personnelId, String hexKey, String buildingType);
    }

    public PersonnelDataService(String saveDirectory) {
        this.saveFilePath = saveDirectory + "/personnel_data.json";
        loadPersonnelData();
    }

    // **MÉTHODES DE RECRUTEMENT**
    public List<DATABASE.Workers> getAvailableWorkersForRecruitment(String factionId) {
        return Arrays.stream(DATABASE.Workers.values())
                .filter(worker -> !worker.isFactionSpecific() ||
                        worker.getWorkerFactionId().equals(factionId))
                .collect(Collectors.toList());
    }

    public boolean hirePersonnel(String factionId, DATABASE.Workers workerType, int quantity) {
        List<HiredPersonnel> personnelList = factionPersonnel.computeIfAbsent(factionId, k -> new ArrayList<>());

        for (int i = 0; i < quantity; i++) {
            String personnelId = UUID.randomUUID().toString();
            String generatedName = generateWorkerName(workerType);

            HiredPersonnel newPersonnel = new HiredPersonnel(
                    personnelId, workerType, generatedName,
                    null, null, // Non assigné au début
                    workerType.getCurrentSalary(),
                    workerType.getCurrentFoodConsumption(),
                    false
            );

            personnelList.add(newPersonnel);
            notifyPersonnelHired(newPersonnel);
        }

        savePersonnelData();
        return true;
    }

    // **MÉTHODES D'ASSIGNATION**
    public boolean assignPersonnelToBuilding(String personnelId, String hexKey,
                                             String buildingType, DATABASE.JobBuilding building) {
        HiredPersonnel personnel = findPersonnelById(personnelId);
        if (personnel == null || personnel.isAssigned) {
            return false;
        }

        // Vérifier si le worker peut travailler dans ce bâtiment
        if (!DATABASE.canJobWorkInBuilding(personnel.workerType.getJobName(), building)) {
            return false;
        }

        // Vérifier la capacité maximale du bâtiment
        if (getAssignedCountForBuilding(hexKey, buildingType) >= building.getMaxWorker()) {
            return false;
        }

        // Effectuer l'assignation
        updatePersonnelAssignment(personnelId, hexKey, buildingType, true);
        notifyPersonnelAssigned(personnelId, hexKey, buildingType);

        savePersonnelData();
        return true;
    }

    public boolean firePersonnel(String personnelId) {
        for (List<HiredPersonnel> personnelList : factionPersonnel.values()) {
            personnelList.removeIf(p -> p.personnelId.equals(personnelId));
        }

        notifyPersonnelFired(personnelId);
        savePersonnelData();
        return true;
    }

    // **MÉTHODES DE REQUÊTE**
    public List<HiredPersonnel> getFactionPersonnel(String factionId) {
        return new ArrayList<>(factionPersonnel.getOrDefault(factionId, Collections.emptyList()));
    }

    public List<HiredPersonnel> getUnassignedPersonnel(String factionId) {
        return getFactionPersonnel(factionId).stream()
                .filter(p -> !p.isAssigned)
                .collect(Collectors.toList());
    }

    public List<HiredPersonnel> getAssignedPersonnelForHex(String factionId, String hexKey) {
        return getFactionPersonnel(factionId).stream()
                .filter(p -> p.isAssigned && hexKey.equals(p.assignedHex))
                .collect(Collectors.toList());
    }

    public int getAssignedCountForBuilding(String hexKey, String buildingType) {
        return (int) factionPersonnel.values().stream()
                .flatMap(List::stream)
                .filter(p -> p.isAssigned &&
                        hexKey.equals(p.assignedHex) &&
                        buildingType.equals(p.assignedBuilding))
                .count();
    }

    // **MÉTHODES PRIVÉES**
    private String generateWorkerName(DATABASE.Workers workerType) {
        String[] firstNames = {"Jean", "Marie", "Pierre", "Anne", "Louis", "Élise", "Henri", "Claire"};
        String[] lastNames = {"Martin", "Bernard", "Thomas", "Petit", "Robert", "Richard", "Durand", "Dubois"};

        Random random = new Random();
        String firstName = firstNames[random.nextInt(firstNames.length)];
        String lastName = lastNames[random.nextInt(lastNames.length)];

        return firstName + " " + lastName;
    }

    private HiredPersonnel findPersonnelById(String personnelId) {
        return factionPersonnel.values().stream()
                .flatMap(List::stream)
                .filter(p -> p.personnelId.equals(personnelId))
                .findFirst()
                .orElse(null);
    }

    private void updatePersonnelAssignment(String personnelId, String hexKey,
                                           String buildingType, boolean assigned) {
        // Cette méthode devrait mettre à jour l'objet HiredPersonnel
        // (nécessite une refactorisation pour rendre les champs mutables)
    }

    // **SAUVEGARDE ET CHARGEMENT**
    private void savePersonnelData() {
        try (FileWriter writer = new FileWriter(saveFilePath)) {
            // Implémentation JSON simple ou utilisation de Jackson/Gson
            // Sauvegarder factionPersonnel dans le fichier
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde du personnel: " + e.getMessage());
        }
    }

    private void loadPersonnelData() {
        File file = new File(saveFilePath);
        if (!file.exists()) return;

        try (FileReader reader = new FileReader(file)) {
            // Charger les données depuis le fichier JSON
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement du personnel: " + e.getMessage());
        }
    }

    // **OBSERVATEURS**
    public void addObserver(PersonnelObserver observer) {
        observers.add(observer);
    }

    private void notifyPersonnelHired(HiredPersonnel personnel) {
        observers.forEach(obs -> obs.onPersonnelHired(personnel));
    }

    private void notifyPersonnelFired(String personnelId) {
        observers.forEach(obs -> obs.onPersonnelFired(personnelId));
    }

    private void notifyPersonnelAssigned(String personnelId, String hexKey, String buildingType) {
        observers.forEach(obs -> obs.onPersonnelAssigned(personnelId, hexKey, buildingType));
    }
}