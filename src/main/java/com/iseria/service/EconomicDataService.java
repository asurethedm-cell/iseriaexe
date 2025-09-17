package com.iseria.service;

import com.iseria.domain.*;
import com.iseria.ui.Login;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class EconomicDataService {

    public static class EconomicData {
        public double tresorerie = 20000.0;
        public double revenus = 0.0;
        public double depenses = 0.0;
        public int populationTotale = 0;
        public double consommationNourriture = 0.0;
        public double productionNourriture = 0.0;
        public double faim = 0.0; // NOUVEAU: Niveau de faim de la population
        public double instabilite = 0.0; // Depuis choix moraux + faim
        public double agressivite = 0.0; // Depuis buildings non-free
        public Map<String, Double> ressources = new HashMap<>();
        public Map<String, Double> productionRessources = new HashMap<>();
        public Map<String, Double> salaires = new HashMap<>();
        public Map<String, Integer> jobCounts = new HashMap<>();


        public EconomicData() {
            for (DATABASE.ResourceType resource : DATABASE.ResourceType.values()) {
                ressources.put(resource.getName(), 0.0);
                productionRessources.put(resource.getName(), 0.0);
            }
            initializeSalaries();
        }

        private void initializeSalaries() {
            Set<String> categories = Arrays.stream(DATABASE.Workers.values())
                    .map(DATABASE.Workers::getCategory)
                    .collect(Collectors.toSet());
            for (String category : categories) {
                salaires.put(category, 0.0);
            }
            salaires.put("Commandement", 0.0);
            salaires.put("Infanterie", 0.0);
            salaires.put("Cavalerie", 0.0);
            salaires.put("Archers", 0.0);
            salaires.put("Siège", 0.0);
        }

        public void calculateFaim() {
            faim = Math.max(0, consommationNourriture - productionNourriture);
            if (faim > 0) {
                instabilite += faim * 0.5; // 0.5% d'instabilité par point de faim
            }
        }
    }

    // OBSERVATEURS
    public interface EconomicDataObserver {
        void onEconomicDataChanged(EconomicData data);
        void onResourceProductionChanged(String hexKey, Map<String, Double> production);
        void onWorkerCountChanged(String hexKey, String buildingType, int count);
    }

    // DONNÉES
    private EconomicData economicData = new EconomicData();
    private List<EconomicDataObserver> observers = new CopyOnWriteArrayList<>();
    private IHexRepository hexRepository;
    private String factionName;
    private Faction faction;
    private MoralSaveService moralSaveService = new MoralSaveService();
    private LogisticsService logisticsService;

    public EconomicDataService(IHexRepository hexRepository, String factionName) {
        this.hexRepository = hexRepository;
        this.factionName = factionName;
        this.logisticsService  = new LogisticsService(hexRepository);
        calculateInitialData();
    }
    public EconomicDataService(IHexRepository hexRepository, String factionName, Faction faction) {
        this.hexRepository = hexRepository;
        this.factionName = factionName;
        this.faction = faction;
        calculateInitialData();
        this.logisticsService = new LogisticsService(hexRepository);
        loadSavedEconomicState();
    }

    public EconomicData getEconomicData() { return economicData; }
    public void addObserver(EconomicDataObserver observer) {
        observers.add(observer);
    }
    public void removeObserver(EconomicDataObserver observer) {
        observers.remove(observer);
    }
    private void notifyObservers() {
        for (EconomicDataObserver observer : observers) {
            observer.onEconomicDataChanged(economicData);
        }
    }
    private void loadSavedEconomicState() {
        if (faction != null) {
            try {
                MoralSaveService.MoralSaveData savedData = moralSaveService.loadMoralSelections(
                        getCurrentUser(), faction);
                if (savedData.calculatedInstability > 0) {
                    economicData.instabilite = savedData.calculatedInstability;
                    System.out.println("Loaded saved instability: " + savedData.calculatedInstability);
                }
            } catch (Exception e) {
                System.err.println("Could not load saved economic state: " + e.getMessage());
            }
        }
    }
    private String getCurrentUser() {
        return Login.currentUser;
    }
    public void calculateInitialData() {
        Map<String, SafeHexDetails> hexGrid = hexRepository.loadSafeAll();
        economicData.populationTotale = 0;
        economicData.agressivite = 0;
        economicData.jobCounts.clear();

        for (String resource : economicData.productionRessources.keySet()) {
            economicData.productionRessources.put(resource, 0.0);
        }

        for (Map.Entry<String, SafeHexDetails> entry : hexGrid.entrySet()) {
            SafeHexDetails hex = entry.getValue();
            if (factionName.equals(hex.getFactionClaim())) {
                economicData.populationTotale += hex.getTotalWorkers();
                if (hex.getMainBuildingIndex() > 0) {
                    economicData.agressivite += 1.0;
                }
                calculateHexProduction(entry.getKey(), hex);
                updateJobCounts(hex);
            }
        }

        calculateFoodConsumption();
        calculateSalaryCosts();
        economicData.calculateFaim();
        notifyObservers();
    }

    private void calculateHexProduction(String hexKey, SafeHexDetails hex) {
        ProductionCalculationService productionService = new ProductionCalculationService();
        Map<String, Double> hexProduction = new HashMap<>();

        HexResourceData resourceData = hexResourceStates.get(hexKey);
        if (resourceData == null) {
            resourceData = new HexResourceData(hexKey);
            hexResourceStates.put(hexKey, resourceData);
        }

        for (String buildingType : Arrays.asList("main", "aux", "fort")) {
            String savedResource = hex.getSelectedResourceType(buildingType);
            Double savedProduction = hex.getSelectedResourceProduction(buildingType);

            if (savedResource != null && savedProduction != null && savedProduction > 0) {
                DATABASE.ResourceType resourceType = DATABASE.ResourceType.lookupByName(savedResource);
                if (resourceType != null) {
                    // **NOUVEAU** - Appliquer le multiplicateur d'état
                    double effectiveMultiplier = resourceData.getEffectiveProductionMultiplier(
                            resourceType, buildingType);

                    double adjustedProduction = savedProduction * effectiveMultiplier;
                    hexProduction.put(savedResource, adjustedProduction);

                    System.out.println(String.format("Production %s: %.1f -> %.1f (multiplicateur: %.2f)",
                            savedResource, savedProduction, adjustedProduction, effectiveMultiplier));
                }
            }
        }
        for (Map.Entry<String, Double> entry : hexProduction.entrySet()) {
            String resourceName = entry.getKey();
            double production = entry.getValue();

            economicData.productionRessources.merge(resourceName, production, Double::sum);
            if (isFood(resourceName)) {
                economicData.productionNourriture += production;
            }
        }
         calculateLivestockProduction(hexKey, hex);
    }
    private void calculateProductionWithLogistics(String hexKey, SafeHexDetails hex) {
        // Production locale immédiate (comme avant)
        Map<String, Double> localProduction = calculateLocalProduction(hex);

        // Pour chaque ressource produite, calculer le temps de transport
        StorageWarehouse nearestWarehouse = logisticsService.findNearestWarehouse(hexKey);

        if (nearestWarehouse != null) {
            for (Map.Entry<String, Double> entry : localProduction.entrySet()) {
                String resource = entry.getKey();
                double quantity = entry.getValue();

                int transportTime = logisticsService.calculateTransportTime(
                        hexKey, nearestWarehouse.getHexKey(), resource, quantity
                );

                // La ressource arrivera au warehouse dans 'transportTime' tours
                scheduleResourceDelivery(resource, quantity, transportTime, nearestWarehouse);
            }
        }
    }
    private void scheduleResourceDelivery(String resource, double quantity,
                                          int transportTime, StorageWarehouse warehouse) {

        if (warehouse.canStore(resource, quantity)) {
            warehouse.addResource(resource, quantity);
            System.out.println(String.format(
                    "Livraison programmée: %s x%.1f vers %s dans %d jours",
                    resource, quantity, warehouse.getHexKey(), transportTime
            ));
        } else {
            System.out.println("⚠️ Entrepôt saturé - livraison impossible!");
        }
    }
    private Map<String, Double> calculateLocalProduction(SafeHexDetails hex) {
        Map<String, Double> production = new HashMap<>();

        ProductionCalculationService service = new ProductionCalculationService();
        return service.calculateHexProduction(hex);
    }
    private boolean isFood(String resourceName) {
        DATABASE.ResourceType resource = DATABASE.ResourceType.lookupByName(resourceName);
        return resource != null && "Nourriture".equals(resource.getCategory());
    }
    private void calculateLivestockProduction(String hexKey, SafeHexDetails hex) {
        LivestockFarm farm = hex.getLivestockFarm();
        if (farm == null) return;

        for (Map.Entry<DATABASE.LivestockData, LivestockFarm.LivestockHerd> entry : farm.getHerds().entrySet()) {
            DATABASE.LivestockData animalData = entry.getKey();
            LivestockFarm.LivestockHerd herd = entry.getValue();

            if (herd.isEstEnProduction()) {
                double production = herd.getProductionHebdomadaire();
                economicData.productionRessources.merge("nourriture", production, Double::sum);
                economicData.productionNourriture += production;

                calculateAnimalByproducts(animalData, herd);

                double consommation = herd.getNourritureParSemaine() * herd.getNombreBetes();
                economicData.consommationNourriture += consommation;
            }
            int bergersNecessaires = (herd.getNombreBetes() + animalData.getAnimauxParBerger() - 1)
                    / animalData.getAnimauxParBerger();
            economicData.jobCounts.merge("Berger", bergersNecessaires, Integer::sum);
        }
    }
    private void calculateAnimalByproducts(DATABASE.LivestockData animalData, LivestockFarm.LivestockHerd herd) {
        String animalName = animalData.getName().toLowerCase();

        switch (animalName) {
            case "vache":
                double laitProduction = herd.getNombreBetes() * 2.0;
                economicData.productionRessources.merge("lait", laitProduction, Double::sum);
                break;
            case "mouton", "chevre":
                double laineProduction = herd.getNombreBetes() * 0.5;
                economicData.productionRessources.merge("laine_brute", laineProduction, Double::sum);
                break;
            case "poule":
                double oeufsProduction = herd.getNombreBetes() * 5.0;
                economicData.productionRessources.merge("oeufs_de_poule", oeufsProduction, Double::sum);
                break;
            case "canard":
                double oeufsCanardProduction = herd.getNombreBetes() * 3.0;
                economicData.productionRessources.merge("oeufs_de_canard", oeufsCanardProduction, Double::sum);
                break;
        }
    }
    public void progressLivestockWeek() {
        Map<String, SafeHexDetails> hexGrid = hexRepository.loadSafeAll();

        for (SafeHexDetails hex : hexGrid.values()) {
            if (factionName.equals(hex.getFactionClaim()) && hex.getLivestockFarm() != null) {
                hex.getLivestockFarm().progresserSemaine();
                hexRepository.updateHexDetails(hex.getHexKey(), hex);
            }
        }
        calculateInitialData();
    }

    private void updateJobCounts(SafeHexDetails hex) {
        if (hex.getMainWorkerCount() > 0) {
            economicData.jobCounts.merge("Fermier", hex.getMainWorkerCount(), Integer::sum);
        }
        if (hex.getAuxWorkerCount() > 0) {
            economicData.jobCounts.merge("Marchand", hex.getAuxWorkerCount(), Integer::sum);
        }
        if (hex.getFortWorkerCount() > 0) {
            economicData.jobCounts.merge("Garde", hex.getFortWorkerCount(), Integer::sum);
        }
    }
    private void calculateFoodConsumption() {
        economicData.consommationNourriture = DATABASE.calculateTotalFoodConsumption(economicData.jobCounts);
    }
    private void calculateSalaryCosts() {
        for (String key : economicData.salaires.keySet()) {
            economicData.salaires.put(key, 0.0);
        }
        for (Map.Entry<String, Integer> entry : economicData.jobCounts.entrySet()) {
            String jobName = entry.getKey();
            int count = entry.getValue();
            double jobSalary = DATABASE.getSalaryForJob(jobName);
            double totalSalary = jobSalary * count;
            String category = findJobCategory(jobName);
            if (category != null) {
                economicData.salaires.merge(category, totalSalary, Double::sum);
            }
        }
        economicData.depenses = economicData.salaires.values()
                .stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }
    private String findJobCategory(String jobName) {
        return Arrays.stream(DATABASE.Workers.values())
                .filter(worker -> worker.getJobName().equals(jobName))
                .map(DATABASE.Workers::getCategory)
                .findFirst()
                .orElse("Autre");
    }
    public void updateInstabilityFromMoral(double instabilityValue) {
        economicData.instabilite = instabilityValue;
        economicData.calculateFaim();
        notifyObservers();
    }
    public void updateWorkerCount(String hexKey, String buildingType, int newCount) {
        calculateInitialData();
        for (EconomicDataObserver observer : observers) {
            observer.onWorkerCountChanged(hexKey, buildingType, newCount);
        }
    }
    public void updateResourceProduction(String hexKey, Map<String, Double> production) {
        for (Map.Entry<String, Double> entry : production.entrySet()) {
            economicData.productionRessources.merge(
                    entry.getKey(),
                    entry.getValue(),
                    Double::sum
            );
            if ("Nourriture".equals(entry.getKey())) {
                economicData.productionNourriture += entry.getValue();
            }
        }

        economicData.calculateFaim();
        notifyObservers();

        for (EconomicDataObserver observer : observers) {
            observer.onResourceProductionChanged(hexKey, production);
        }
    }
    public void updateSalary(String category, double amount) {
        economicData.salaires.put(category, amount);
        economicData.depenses = economicData.salaires.values()
                .stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        notifyObservers();
    }
    public double getFaimLevel() {
        return economicData.faim;
    }
    public boolean isPopulationHungry() {
        return economicData.faim > 0;
    }
    public double getFaimImpactOnInstability() {
        return economicData.faim * 0.5;
    }
    public List<String> getFaimReductionSuggestions() {
        List<String> suggestions = new ArrayList<>();
        if (economicData.faim > 0) {
            suggestions.add("Augmenter la production de nourriture");
            suggestions.add("Construire plus de bâtiments agricoles");
            suggestions.add("Réduire la population si nécessaire");
        }
        return suggestions;
    }
    public String getEconomicReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== RAPPORT ÉCONOMIQUE ===\n");
        report.append("Trésorerie: ").append(String.format("%.2f Po", economicData.tresorerie)).append("\n");
        report.append("Population: ").append(economicData.populationTotale).append("\n");
        report.append("Instabilité: ").append(String.format("%.1f%%", economicData.instabilite)).append("\n");
        report.append("Agressivité: ").append(String.format("%.0f", economicData.agressivite)).append("\n");
        report.append("\n=== ALIMENTATION ===\n");
        report.append("Production: ").append(String.format("%.1f", economicData.productionNourriture)).append("\n");
        report.append("Consommation: ").append(String.format("%.1f", economicData.consommationNourriture)).append("\n");
        report.append("Faim: ").append(String.format("%.1f", economicData.faim));
        if (economicData.faim > 0) {
            report.append(" ⚠️ POPULATION AFFAMÉE");
        } else {
            report.append(" ✅ Population nourrie");
        }
        return report.toString();
    }
    public String getFactionName() {
        return factionName;
    }
    public IHexRepository getHexRepository() {
        return hexRepository;
    }
    public EconomicData simulateChanges(Map<String, Object> changes) {
        EconomicData simulation = new EconomicData();
        simulation.tresorerie = economicData.tresorerie;
        simulation.populationTotale = economicData.populationTotale;
        simulation.productionNourriture = economicData.productionNourriture;
        simulation.consommationNourriture = economicData.consommationNourriture;
        simulation.instabilite = economicData.instabilite;

        for (Map.Entry<String, Object> entry : changes.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            switch (key) {
                case "population":
                    simulation.populationTotale = ((Number) value).intValue();
                    simulation.consommationNourriture = DATABASE.calculateTotalFoodConsumption(economicData.jobCounts);
                    break;
                case "production_nourriture":
                    simulation.productionNourriture = ((Number) value).doubleValue();
                    break;
            }
        }

        simulation.calculateFaim();
        return simulation;
    }
    public LogisticsService getLogisticsService() {
        return logisticsService;
    }
    public double getResourceProductionModifier(String hexKey, DATABASE.ResourceType resource) {
        // Logique pour récupérer les modificateurs de production spécifiques à l'hex
        // Par exemple: bonus de terrain, technologies, effets de faction
        return 1.0; // TODO Placeholder
    }
    public double getFactionProductionBonus(String hexKey, DATABASE.ResourceType resource) {
        // Logique pour calculer les bonus de faction
        // Par exemple: bonus raciaux, technologies recherchées
        return 1.0; // TODO Placeholder
    }
    public double getResourceMarketValue(DATABASE.ResourceType resource) {
        // Valeur de marché actuelle (peut fluctuer selon l'économie du jeu)
        // Au lieu d'utiliser resource.getBaseValue() statique
        return resource.getBaseValue(); // TODO Placeholder
    }

    private final Map<String, HexResourceData> hexResourceStates = new HashMap<>();

    // **NOUVELLE MÉTHODE** - Initialiser les états des ressources
    private void initializeResourceStates() {
        Map<String, SafeHexDetails> hexGrid = hexRepository.loadSafeAll();

        for (Map.Entry<String, SafeHexDetails> entry : hexGrid.entrySet()) {
            String hexKey = entry.getKey();
            SafeHexDetails hex = entry.getValue();

            if (factionName.equals(hex.getFactionClaim())) {
                HexResourceData resourceData = new HexResourceData(hexKey);

                // Initialiser les états pour les ressources produites
                for (String buildingType : Arrays.asList("main", "aux", "fort")) {
                    String resourceName = hex.getSelectedResourceType(buildingType);
                    if (resourceName != null) {
                        DATABASE.ResourceType resourceType = DATABASE.ResourceType.lookupByName(resourceName);
                        if (resourceType != null) {
                            // Déclencher l'initialisation de l'état
                            resourceData.getResourceState(resourceType);
                            resourceData.getBuildingState(buildingType);
                        }
                    }
                }

                hexResourceStates.put(hexKey, resourceData);
            }
        }
    }

    // **NOUVELLE MÉTHODE** - Progression hebdomadaire
    public void advanceWeek() {
        for (HexResourceData resourceData : hexResourceStates.values()) {
            resourceData.progressWeek();
        }

        // Recalculer les données économiques après progression
        calculateInitialData();
        notifyObservers();
    }

    // **NOUVELLES MÉTHODES PUBLIQUES**
    public HexResourceData getHexResourceData(String hexKey) {
        return hexResourceStates.get(hexKey);
    }

    public List<String> getMaintenanceAlerts() {
        List<String> alerts = new ArrayList<>();

        for (Map.Entry<String, HexResourceData> entry : hexResourceStates.entrySet()) {
            String hexKey = entry.getKey();
            HexResourceData resourceData = entry.getValue();

            List<String> hexRecommendations = resourceData.getMaintenanceRecommendations();
            for (String recommendation : hexRecommendations) {
                alerts.add(hexKey + ": " + recommendation);
            }
        }

        return alerts;
    }

    public boolean performHexMaintenance(String hexKey, DATABASE.ResourceType resource, double cost) {
        HexResourceData resourceData = hexResourceStates.get(hexKey);
        if (resourceData != null && economicData.tresorerie >= cost) {
            boolean success = resourceData.performMaintenance(resource, cost);
            if (success) {
                economicData.tresorerie -= cost;
                calculateInitialData(); // Recalculer après maintenance
                notifyObservers();
            }
            return success;
        }
        return false;
    }



    public class ProductionCalculationService {

        public Map<String, Double> calculateHexProduction(SafeHexDetails hex) {
            Map<String, Double> production = new HashMap<>();

            // Production du bâtiment principal
            if (hex.getMainBuildingIndex() > 0) {
                DATABASE.MainBuilding mainBuilding = DATABASE.MainBuilding.values()[hex.getMainBuildingIndex()];
                production.putAll(calculateBuildingProduction(mainBuilding, hex.getMainWorkerCount()));
            }

            // Production du bâtiment auxiliaire
            if (hex.getAuxBuildingIndex() > 0) {
                DATABASE.AuxBuilding auxBuilding = DATABASE.AuxBuilding.values()[hex.getAuxBuildingIndex()];
                production.putAll(calculateBuildingProduction(auxBuilding, hex.getAuxWorkerCount()));
            }

            return production;
        }

        public Map<String, Double> calculateBuildingProduction(DATABASE.JobBuilding building, int workers) {
            Map<String, Double> production = new HashMap<>();

            if (workers <= 0) return production;
            List<DATABASE.ResourceType> possibleResources = DATABASE.getResourcesForBuilding(building);

            for (DATABASE.ResourceType resource : possibleResources) {
                double baseProduction = resource.getBaseValue();
                double efficiency = getBuildingEfficiency(building);
                double totalProduction = baseProduction * workers * efficiency;

                if (totalProduction > 0) {
                    production.put(resource.getName(), totalProduction);
                }
            }

            return production;
        }

        public double getBuildingEfficiency(DATABASE.JobBuilding building) {
            // Calculer l'efficacité selon le tier du bâtiment
            if (building instanceof DATABASE.MainBuilding) {
                return 1.0 + (((DATABASE.MainBuilding) building).getMainTier() * 0.2);
            } else if (building instanceof DATABASE.AuxBuilding) {
                return 1.0 + (((DATABASE.AuxBuilding) building).getAuxTier() * 0.2);
            }
            return 1.0;
        }
    }
}