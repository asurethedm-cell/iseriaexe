package com.iseria.service;

import com.iseria.domain.*;
import com.iseria.ui.Login;
import com.iseria.ui.MainMenu;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.io.*;
import java.util.stream.Collectors;

public class PersonnelDataService {

    // **STRUCTURES DE DONNÉES INTÉGRÉES**
    private static class PersonnelSaveData implements Serializable {
        @Serial
        private static final long serialVersionUID = 2L;
        public Map<String, List<SavedPersonnel>> factionPersonnel = new HashMap<>();

        private static class SavedPersonnel implements Serializable {
            @Serial
            private static final long serialVersionUID = 2L;
            public String personnelId;
            public String workerTypeName;
            public String name;
            public String assignedBuilding;
            public String assignedHex;
            public double currentSalary;
            public double foodConsumption;
            public boolean isAssigned;
            public long hireDate;

            public SavedPersonnel() {}
        }
    }

    // **CLASSE HIREDPERSONNEL (RUNTIME)**
    public static class HiredPersonnel {
        public final String personnelId;
        public final DATABASE.Workers workerType;
        public final String name;
        public final String assignedBuilding;
        public final String assignedHex;
        public final double currentSalary;
        public final double foodConsumption;
        public final boolean isAssigned;
        public final long hireDate;

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

    public interface PersonnelObserver {
        void onPersonnelHired(HiredPersonnel personnel);
        void onPersonnelFired(String personnelId);
        void onPersonnelAssigned(String personnelId, String hexKey, String buildingType);
    }

    private final String saveDirectory;
    private final Map<String, List<HiredPersonnel>> factionPersonnel = new HashMap<>();
    private final List<PersonnelObserver> observers = new CopyOnWriteArrayList<>();
    private EconomicDataService economicService;

    public PersonnelDataService(String saveDirectory) {
        this.saveDirectory = saveDirectory;
        loadPersonnelData();
    }

    public void setEconomicDataService(EconomicDataService economicService) {
        this.economicService = economicService;
    }

    public void hirePersonnel(String factionId, DATABASE.Workers workerType, int quantity) {
        List<HiredPersonnel> personnelList = factionPersonnel.computeIfAbsent(factionId, k -> new ArrayList<>());

        double totalCost = workerType.getCurrentSalary() * quantity;

        if (economicService != null) {
            EconomicDataService.EconomicData economicData = economicService.getEconomicData();
            if (economicData.tresorerie < totalCost) {
                System.out.println("Budget insuffisant pour recruter " + quantity + " " + workerType.getJobName());
                return;
            }
            economicData.tresorerie -= totalCost;
        }

        for (int i = 0; i < quantity; i++) {
            String personnelId = UUID.randomUUID().toString();
            String generatedName = generateWorkerName(workerType);

            HiredPersonnel newPersonnel = new HiredPersonnel(
                    personnelId, workerType, generatedName,
                    null, null,
                    workerType.getCurrentSalary(),
                    workerType.getCurrentFoodConsumption(),
                    false,
                    System.currentTimeMillis()
            );

            personnelList.add(newPersonnel);
            notifyPersonnelHired(newPersonnel);
        }

        if (economicService != null) {
            updateEconomicData(workerType, quantity, totalCost);
        }

        savePersonnelData();
    }

    public boolean assignPersonnelToBuilding(String personnelId, String hexKey,
                                             String buildingType, DATABASE.JobBuilding building) {
        HiredPersonnel personnel = findPersonnelById(personnelId);
        if (personnel == null || personnel.isAssigned) return false;

        if (!canJobWorkInBuilding(personnel.workerType.getJobName(), building)) return false;
        if (getAssignedCountForBuilding(hexKey, buildingType) >= building.getMaxWorker()) return false;

        updatePersonnelAssignment(personnelId, hexKey, buildingType, true);

        if (economicService != null) {
            economicService.updateWorkerCount(hexKey, buildingType,
                    getAssignedCountForBuilding(hexKey, buildingType));
        }

        notifyPersonnelAssigned(personnelId, hexKey, buildingType);
        savePersonnelData();
        return true;
    }

    public void unassignPersonnel(String personnelId) {
        HiredPersonnel personnel = findPersonnelById(personnelId);
        if (personnel == null || !personnel.isAssigned) return;

        String oldHex = personnel.assignedHex;
        String oldBuildingType = personnel.assignedBuilding;

        updatePersonnelAssignment(personnelId, null, null, false);

        if (economicService != null && oldHex != null && oldBuildingType != null) {
            economicService.updateWorkerCount(oldHex, oldBuildingType,
                    getAssignedCountForBuilding(oldHex, oldBuildingType));
        }

        savePersonnelData();
    }

    public boolean firePersonnel(String personnelId) {
        for (List<HiredPersonnel> personnelList : factionPersonnel.values()) {
            personnelList.removeIf(p -> p.personnelId.equals(personnelId));
        }

        notifyPersonnelFired(personnelId);
        savePersonnelData();
        return true;
    }

    public List<DATABASE.Workers> getAvailableWorkersForRecruitment(String factionId) {
        return Arrays.stream(DATABASE.Workers.values()).collect(Collectors.toList());
    }

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

    private void savePersonnelData() {
        try {
            String user = Login.currentUser;
            String factionId = MainMenu.getCurrentFactionId();
            String fileName = "personnel_" + factionId + ".dat";
            String tempFileName = fileName + ".tmp";

            PersonnelSaveData saveData = new PersonnelSaveData();

            for (var entry : factionPersonnel.entrySet()) {
                List<PersonnelSaveData.SavedPersonnel> savedList = new ArrayList<>();
                for (HiredPersonnel p : entry.getValue()) {
                    PersonnelSaveData.SavedPersonnel saved = new PersonnelSaveData.SavedPersonnel();
                    saved.personnelId = p.personnelId;
                    saved.workerTypeName = p.workerType.getJobName();
                    saved.name = p.name;
                    saved.assignedBuilding = p.assignedBuilding;
                    saved.assignedHex = p.assignedHex;
                    saved.currentSalary = p.currentSalary;
                    saved.foodConsumption = p.foodConsumption;
                    saved.isAssigned = p.isAssigned;
                    saved.hireDate = p.hireDate;
                    savedList.add(saved);
                }
                saveData.factionPersonnel.put(entry.getKey(), savedList);
            }


            try (FileOutputStream fos = new FileOutputStream(fileName);
                 ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeObject(saveData);
                oos.flush();
                fos.getChannel().force(true);
                System.out.println("Personnel data saved: " + fileName);
            }
        } catch (Exception e) {
            System.err.println("Erreur sauvegarde personnel: " + e.getMessage());
        }
    }

    private void loadPersonnelData() {
        try {
            String user = Login.currentUser;
            String factionId = MainMenu.getCurrentFactionId();
            String fileName = "personnel_" + factionId + ".dat";
            File file = new File(fileName);

            if (!file.exists() || file.length() == 0) {
                PersonnelSaveData empty = new PersonnelSaveData();
                savePersonnelDataDirect(fileName, empty);
                System.out.println("PersonnelDataService: fichier créé: " + fileName);
                return;
            }

            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
                PersonnelSaveData saveData = (PersonnelSaveData) ois.readObject();

                for (var entry : saveData.factionPersonnel.entrySet()) {
                    List<HiredPersonnel> personnelList = new ArrayList<>();
                    for (PersonnelSaveData.SavedPersonnel saved : entry.getValue()) {
                        DATABASE.Workers worker = DATABASE.Workers.getByName(saved.workerTypeName);
                        if (worker != null) {
                            HiredPersonnel personnel = new HiredPersonnel(
                                    saved.personnelId, worker, saved.name,
                                    saved.assignedBuilding, saved.assignedHex,
                                    saved.currentSalary, saved.foodConsumption,
                                    saved.isAssigned, saved.hireDate
                            );
                            personnelList.add(personnel);
                        }
                    }
                    factionPersonnel.put(entry.getKey(), personnelList);
                }

                System.out.println("PersonnelDataService: données chargées depuis " + fileName);

            } catch (Exception e) {
                System.err.println("Fichier corrompu, suppression: " + fileName);
                file.delete();
                factionPersonnel.clear();
            }

        } catch (Exception e) {
            System.err.println("Erreur chargement personnel: " + e.getMessage());
            factionPersonnel.clear();
        }
    }

    private void savePersonnelDataDirect(String fileName, PersonnelSaveData data) {
        try (FileOutputStream fos = new FileOutputStream(fileName);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(data);
            oos.flush();
            fos.getChannel().force(true); // force l’écriture sur disque
            System.out.println("Personnel data saved: " + fileName);
        } catch (IOException e) {
            System.err.println("Erreur sauvegarde personnel: " + e.getMessage());
        }}

    // **MÉTHODES HELPER**
    private void updateEconomicData(DATABASE.Workers workerType, int quantity, double totalCost) {
        EconomicDataService.EconomicData economicData = economicService.getEconomicData();

        economicData.populationTotale += quantity;
        economicData.consommationNourriture += workerType.getCurrentFoodConsumption() * quantity;

        String category = workerType.getCategory();
        double currentCategorySalary = economicData.salaires.getOrDefault(category, 0.0);
        economicData.salaires.put(category, currentCategorySalary + totalCost);

        String jobName = workerType.getJobName();
        int currentCount = economicData.jobCounts.getOrDefault(jobName, 0);
        economicData.jobCounts.put(jobName, currentCount + quantity);

        economicData.calculateFaim();
        economicData.depenses = economicData.salaires.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private boolean canJobWorkInBuilding(String jobName, DATABASE.JobBuilding building) {
        if (building instanceof DATABASE.MainBuilding) {
            return Arrays.asList("Fermier libre", "Bûcheron", "Compagnie de Mineur").contains(jobName);
        } else if (building instanceof DATABASE.AuxBuilding) {
            return Arrays.asList("Artisan", "Meunier", "Cuisinier").contains(jobName);
        } else if (building instanceof DATABASE.FortBuilding) {
            return Arrays.asList("Garde", "Archer").contains(jobName);
        }
        return false;
    }

    private void updatePersonnelAssignment(String personnelId, String hexKey, String buildingType, boolean assigned) {
        for (List<HiredPersonnel> personnelList : factionPersonnel.values()) {
            for (int i = 0; i < personnelList.size(); i++) {
                HiredPersonnel p = personnelList.get(i);
                if (p.personnelId.equals(personnelId)) {
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

    private HiredPersonnel findPersonnelById(String personnelId) {
        return factionPersonnel.values().stream()
                .flatMap(List::stream)
                .filter(p -> p.personnelId.equals(personnelId))
                .findFirst()
                .orElse(null);
    }

    private String generateWorkerName(DATABASE.Workers workerType) {
        String[] firstNames = {"Jean", "Marie", "Pierre", "Anne", "Louis", "Élise", "Henri", "Claire"};
        String[] lastNames = {"Martin", "Bernard", "Thomas", "Petit", "Robert", "Richard", "Durand", "Dubois"};

        Random random = new Random();
        String firstName = firstNames[random.nextInt(firstNames.length)];
        String lastName = lastNames[random.nextInt(lastNames.length)];

        return firstName + " " + lastName;
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
