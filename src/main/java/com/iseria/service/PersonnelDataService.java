package com.iseria.service;

import com.iseria.domain.*;
import com.iseria.ui.PersonnelRecruitmentPanel;

import javax.swing.*;
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
        public final long hireDate; // **NOUVEAU**

        public HiredPersonnel(String personnelId, DATABASE.Workers workerType, String name,
                              String assignedBuilding, String assignedHex, double currentSalary,
                              double foodConsumption, boolean isAssigned, long hireDate) {
            this.personnelId = personnelId;
            this.workerType = workerType;
            this.name = name;
            this.assignedBuilding = assignedBuilding;
            this.assignedHex = assignedHex;
            this.currentSalary = currentSalary;
            this.foodConsumption = foodConsumption;
            this.isAssigned = isAssigned;
            this.hireDate = hireDate;
        }
    }

    private final Map<String, List<HiredPersonnel>> factionPersonnel = new HashMap<>();
    private final List<PersonnelObserver> observers = new CopyOnWriteArrayList<>();
    private final String saveFilePath;
    private EconomicDataService economicService;
    private PersonnelDataService PersonnelsaveService;

    public interface PersonnelObserver {
        void onPersonnelHired(HiredPersonnel personnel);
        void onPersonnelFired(String personnelId);
        void onPersonnelAssigned(String personnelId, String hexKey, String buildingType);
    }
    public void setEconomicDataService(EconomicDataService economicService) {
        this.economicService = economicService;
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

        double totalCost = workerType.getCurrentSalary() * quantity;

        if (economicService != null) {
            EconomicDataService.EconomicData economicData = economicService.getEconomicData();
            if (economicData.tresorerie < totalCost) {
                PersonnelRecruitmentPanel.noFunds = true;
                return false;
            }
            economicData.tresorerie -= totalCost;
        }

        // Créer le personnel
        for (int i = 0; i < quantity; i++) {
            String personnelId = UUID.randomUUID().toString();
            String generatedName = generateWorkerName(workerType);

            HiredPersonnel newPersonnel = new HiredPersonnel(
                    personnelId, workerType, generatedName,
                    null, null, // Non assigné au début
                    workerType.getCurrentSalary(),
                    workerType.getCurrentFoodConsumption(),
                    false,
                    System.currentTimeMillis()
            );

            personnelList.add(newPersonnel);
            notifyPersonnelHired(newPersonnel);
        }

        if (economicService != null) {
            economicService.getEconomicData().populationTotale += quantity;
            double additionalFoodConsumption = workerType.getCurrentFoodConsumption() * quantity;
            economicService.getEconomicData().consommationNourriture += additionalFoodConsumption;
            String category = workerType.getCategory();
            double currentCategorySalary = economicService.getEconomicData().salaires.getOrDefault(category, 0.0);
            economicService.getEconomicData().salaires.put(category, currentCategorySalary + totalCost);
            String jobName = workerType.getJobName();
            int currentCount = economicService.getEconomicData().jobCounts.getOrDefault(jobName, 0);
            economicService.getEconomicData().jobCounts.put(jobName, currentCount + quantity);
            economicService.getEconomicData().calculateFaim();
            economicService.getEconomicData().depenses = economicService.getEconomicData().salaires.values()
                    .stream().mapToDouble(Double::doubleValue).sum();
        }

        savePersonnelData();
        return true;
    }

    public boolean assignPersonnelToBuilding(String personnelId, String hexKey,
                                             String buildingType, DATABASE.JobBuilding building) {
        HiredPersonnel personnel = findPersonnelById(personnelId);
        if (personnel == null || personnel.isAssigned) {
            return false;
        }

        // Vérifier compatibilité
        if (!canJobWorkInBuilding(personnel.workerType.getJobName(), building)) {
            return false;
        }

        // Vérifier capacité maximale
        if (getAssignedCountForBuilding(hexKey, buildingType) >= building.getMaxWorker()) {
            return false;
        }

        // **EFFECTUER L'ASSIGNATION**
        updatePersonnelAssignment(personnelId, hexKey, buildingType, true);

        // **INTÉGRATION ÉCONOMIQUE**
        if (economicService != null) {
            economicService.updateWorkerCount(hexKey, buildingType,
                    getAssignedCountForBuilding(hexKey, buildingType));
        }

        notifyPersonnelAssigned(personnelId, hexKey, buildingType);
        savePersonnelData();
        return true;
    }
    public boolean reassignPersonnel(String personnelId, String newHexKey, String newBuildingType,
                                     DATABASE.JobBuilding newBuilding) {
        HiredPersonnel personnel = findPersonnelById(personnelId);
        if (personnel == null || !personnel.isAssigned) {
            return false;
        }

        // Désassigner de l'ancienne position
        String oldHex = personnel.assignedHex;
        String oldBuilding = personnel.assignedBuilding;

        unassignPersonnel(personnelId);

        // Assigner à la nouvelle position
        boolean success = assignPersonnelToBuilding(personnelId, newHexKey, newBuildingType, newBuilding);

        if (success) {
            System.out.println(String.format("Personnel %s reassigné de %s:%s vers %s:%s",
                    personnel.name, oldHex, oldBuilding, newHexKey, newBuildingType));
        } else {
            // Rollback - reassigner à l'ancienne position
            // (implementation spécifique selon vos besoins)
        }

        return success;
    }
    public boolean unassignPersonnel(String personnelId) {
        HiredPersonnel personnel = findPersonnelById(personnelId);
        if (personnel == null || !personnel.isAssigned) {
            return false;
        }

        String oldHex = personnel.assignedHex;
        String oldBuildingType = personnel.assignedBuilding;

        // Effectuer la désassignation
        updatePersonnelAssignment(personnelId, null, null, false);

        // **INTÉGRATION ÉCONOMIQUE**
        if (economicService != null && oldHex != null && oldBuildingType != null) {
            economicService.updateWorkerCount(oldHex, oldBuildingType,
                    getAssignedCountForBuilding(oldHex, oldBuildingType));
        }

        savePersonnelData();
        return true;
    }
    private boolean canJobWorkInBuilding(String jobName, DATABASE.JobBuilding building) {
        // Logique de compatibilité métier-bâtiment
        if (building instanceof DATABASE.MainBuilding) {
            return Arrays.asList("Fermier libre", "Bûcheron", "Compagnie de Mineur").contains(jobName);
        } else if (building instanceof DATABASE.AuxBuilding) {
            return Arrays.asList("Artisan", "Meunier", "Cuisinier").contains(jobName);
        } else if (building instanceof DATABASE.FortBuilding) {
            return Arrays.asList("Garde", "Archer").contains(jobName);
        }
        return false;
    }

    private void updatePersonnelAssignment(String personnelId, String hexKey,
                                           String buildingType, boolean assigned) {
        // Trouver et modifier le personnel (nécessite refactorisation pour rendre mutable)
        for (List<HiredPersonnel> personnelList : factionPersonnel.values()) {
            for (int i = 0; i < personnelList.size(); i++) {
                HiredPersonnel p = personnelList.get(i);
                if (p.personnelId.equals(personnelId)) {
                    // Créer nouvelle instance avec assignation mise à jour
                    HiredPersonnel updated = new HiredPersonnel(
                            p.personnelId, p.workerType, p.name,
                            assigned ? buildingType : null,
                            assigned ? hexKey : null,
                            p.currentSalary, p.foodConsumption, assigned, p.hireDate
                    );
                    personnelList.set(i, updated);
                    return;
                }
            }
        }
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