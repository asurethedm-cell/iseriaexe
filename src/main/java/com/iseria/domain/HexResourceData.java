package com.iseria.domain;

import java.util.*;

public class HexResourceData {

    public static class ResourceStateInfo {
        public ResourceState currentState;
        public double remainingYields; // Pour ressources finies (en années de production)
        public double cumulativeDegradation; // Accumulation de dégradation
        public int weeksSinceLastMaintenance;
        public Map<String, Double> modifiers; // Bonus/malus temporaires

        public ResourceStateInfo() {
            this.currentState = ResourceState.NORMAL;
            this.remainingYields = 50.0; // 50 ans par défaut pour mines
            this.cumulativeDegradation = 0.0;
            this.weeksSinceLastMaintenance = 0;
            this.modifiers = new HashMap<>();
        }

        public ResourceStateInfo(ResourceState initialState, double remainingYields) {
            this.currentState = initialState;
            this.remainingYields = remainingYields;
            this.cumulativeDegradation = 0.0;
            this.weeksSinceLastMaintenance = 0;
            this.modifiers = new HashMap<>();
        }
    }

    private final String hexKey;
    private final Map<DATABASE.ResourceType, ResourceStateInfo> resourceStates;
    private final Map<String, ResourceStateInfo> buildingStates; // États par bâtiment

    public HexResourceData(String hexKey) {
        this.hexKey = hexKey;
        this.resourceStates = new HashMap<>();
        this.buildingStates = new HashMap<>();
    }

    // **MÉTHODES DE GESTION D'ÉTAT**
    public ResourceStateInfo getResourceState(DATABASE.ResourceType resource) {
        return resourceStates.computeIfAbsent(resource,
                r -> new ResourceStateInfo(getInitialStateForResource(r),
                        getInitialYieldsForResource(r)));
    }

    public ResourceStateInfo getBuildingState(String buildingType) {
        return buildingStates.computeIfAbsent(buildingType,
                b -> new ResourceStateInfo(ResourceState.NORMAL, 0));
    }

    private ResourceState getInitialStateForResource(DATABASE.ResourceType resource) {
        // Logique pour déterminer l'état initial selon le type de ressource
        if (resource.getCategory().equals("Minerais")) {
            return ResourceState.ABUNDANT; // Mines commencent riches
        } else if (resource.getCategory().equals("Nourriture")) {
            return ResourceState.NORMAL; // Fermes commencent normales
        }
        return ResourceState.NORMAL;
    }

    private double getInitialYieldsForResource(DATABASE.ResourceType resource) {
        // Années de production restantes pour ressources finies
        if (resource.getCategory().equals("Minerais")) {
            return 30.0 + Math.random() * 40.0; // 30-70 ans
        } else if (resource.getCategory().equals("Gemmes")) {
            return 15.0 + Math.random() * 25.0; // 15-40 ans
        }
        return Double.MAX_VALUE; // Ressources renouvelables
    }

    // **MÉTHODES DE PROGRESSION TEMPORELLE**
    public void progressWeek() {
        // Progression des ressources
        for (Map.Entry<DATABASE.ResourceType, ResourceStateInfo> entry : resourceStates.entrySet()) {
            DATABASE.ResourceType resource = entry.getKey();
            ResourceStateInfo stateInfo = entry.getValue();

            progressResourceWeek(resource, stateInfo);
        }

        // Progression des bâtiments
        for (Map.Entry<String, ResourceStateInfo> entry : buildingStates.entrySet()) {
            String buildingType = entry.getKey();
            ResourceStateInfo stateInfo = entry.getValue();

            progressBuildingWeek(buildingType, stateInfo);
        }
    }

    private void progressResourceWeek(DATABASE.ResourceType resource, ResourceStateInfo stateInfo) {
        ResourceState currentState = stateInfo.currentState;

        if (currentState.isFiniteResource()) {
            // Progression pour ressources finies (mines)
            double weeklyConsumption = calculateWeeklyResourceConsumption(resource);
            stateInfo.remainingYields -= (weeklyConsumption / 52.0); // Conversion semaine -> année

            // Dégradation naturelle
            stateInfo.cumulativeDegradation += currentState.getWeeklyDegradation();

            // Vérifier transitions d'état
            if (stateInfo.remainingYields <= 0) {
                stateInfo.currentState = ResourceState.EXHAUSTED;
            } else if (stateInfo.remainingYields < 2.0) {
                stateInfo.currentState = ResourceState.NEARLY_EXHAUSTED;
            } else if (stateInfo.remainingYields < 5.0) {
                stateInfo.currentState = ResourceState.DEPLETING_FAST;
            } else if (stateInfo.remainingYields < 10.0) {
                stateInfo.currentState = ResourceState.DEPLETING_MODERATE;
            } else if (stateInfo.remainingYields < 20.0) {
                stateInfo.currentState = ResourceState.DEPLETING_SLOW;
            }
        } else {
            // Progression pour ressources renouvelables (fermes)
            stateInfo.weeksSinceLastMaintenance++;

            // Dégradation d'efficacité graduelle
            if (stateInfo.weeksSinceLastMaintenance > 52) { // 1 an sans maintenance
                stateInfo.cumulativeDegradation += 0.005; // 0.5% par semaine après 1 an

                // Transitions d'état basées sur la dégradation cumulative
                if (stateInfo.cumulativeDegradation > 0.4) {
                    stateInfo.currentState = ResourceState.MAINTENANCE_REQUIRED;
                } else if (stateInfo.cumulativeDegradation > 0.25) {
                    stateInfo.currentState = ResourceState.EFFICIENCY_DECLINE_MAJOR;
                } else if (stateInfo.cumulativeDegradation > 0.15) {
                    stateInfo.currentState = ResourceState.EFFICIENCY_DECLINE_MODERATE;
                } else if (stateInfo.cumulativeDegradation > 0.05) {
                    stateInfo.currentState = ResourceState.EFFICIENCY_DECLINE_MINOR;
                }
            }
        }
    }

    private void progressBuildingWeek(String buildingType, ResourceStateInfo stateInfo) {
        stateInfo.weeksSinceLastMaintenance++;

        // Dégradation des bâtiments basée sur l'utilisation
        double utilizationRate = getBuildingUtilizationRate(buildingType);
        double degradationRate = 0.001 * utilizationRate; // Base degradation

        stateInfo.cumulativeDegradation += degradationRate;

        // Transitions d'état
        if (stateInfo.cumulativeDegradation > 0.6) {
            stateInfo.currentState = ResourceState.MAINTENANCE_REQUIRED;
        } else if (stateInfo.cumulativeDegradation > 0.4) {
            stateInfo.currentState = ResourceState.EFFICIENCY_DECLINE_MAJOR;
        } else if (stateInfo.cumulativeDegradation > 0.25) {
            stateInfo.currentState = ResourceState.EFFICIENCY_DECLINE_MODERATE;
        } else if (stateInfo.cumulativeDegradation > 0.1) {
            stateInfo.currentState = ResourceState.EFFICIENCY_DECLINE_MINOR;
        }
    }

    // **MÉTHODES DE CALCUL**
    public double getEffectiveProductionMultiplier(DATABASE.ResourceType resource, String buildingType) {
        ResourceStateInfo resourceState = getResourceState(resource);
        ResourceStateInfo buildingState = getBuildingState(buildingType);

        double resourceMultiplier = resourceState.currentState.getEfficiencyMultiplier();
        double buildingMultiplier = buildingState.currentState.getEfficiencyMultiplier();

        // Appliquer les modificateurs temporaires
        double temporaryMultiplier = 1.0;
        for (double modifier : resourceState.modifiers.values()) {
            temporaryMultiplier *= modifier;
        }
        for (double modifier : buildingState.modifiers.values()) {
            temporaryMultiplier *= modifier;
        }

        return resourceMultiplier * buildingMultiplier * temporaryMultiplier;
    }

    private double calculateWeeklyResourceConsumption(DATABASE.ResourceType resource) {
        // Calcul basé sur la production actuelle de cette ressource dans cet hex
        // TODO: Intégrer avec ProductionCalculationService
        return 1.0; // Placeholder
    }

    private double getBuildingUtilizationRate(String buildingType) {
        // Taux d'utilisation du bâtiment (0.0 - 1.0)
        // TODO: Calculer basé sur le nombre de workers assignés vs capacité max
        return 0.8; // Placeholder
    }

    // **MÉTHODES DE MAINTENANCE**
    public boolean performMaintenance(DATABASE.ResourceType resource, double maintenanceCost) {
        ResourceStateInfo stateInfo = getResourceState(resource);

        if (!stateInfo.currentState.requiresMaintenance()) {
            return false; // Pas de maintenance nécessaire
        }

        // Réinitialiser l'état après maintenance
        stateInfo.cumulativeDegradation = Math.max(0, stateInfo.cumulativeDegradation - 0.5);
        stateInfo.weeksSinceLastMaintenance = 0;

        // Améliorer l'état
        if (stateInfo.cumulativeDegradation < 0.05) {
            stateInfo.currentState = ResourceState.NORMAL;
        } else if (stateInfo.cumulativeDegradation < 0.15) {
            stateInfo.currentState = ResourceState.EFFICIENCY_DECLINE_MINOR;
        } else if (stateInfo.cumulativeDegradation < 0.25) {
            stateInfo.currentState = ResourceState.EFFICIENCY_DECLINE_MODERATE;
        } else {
            stateInfo.currentState = ResourceState.EFFICIENCY_DECLINE_MAJOR;
        }

        return true;
    }

    public boolean performBuildingMaintenance(String buildingType, double maintenanceCost) {
        ResourceStateInfo stateInfo = getBuildingState(buildingType);

        // Logique similaire à performMaintenance
        stateInfo.cumulativeDegradation = Math.max(0, stateInfo.cumulativeDegradation - 0.6);
        stateInfo.weeksSinceLastMaintenance = 0;
        stateInfo.currentState = ResourceState.NORMAL;

        return true;
    }

    // **MÉTHODES D'AMÉLIORATION**
    public void applyTechnologyUpgrade(String technologyName, double effectMultiplier, int durationWeeks) {
        ResourceStateInfo stateInfo = new ResourceStateInfo(); // Pour l'exemple, appliquer globalement
        stateInfo.modifiers.put("tech_" + technologyName, effectMultiplier);

        // TODO: Programmer la suppression automatique après durationWeeks
    }

    public void applySeasonalEffect(String season, double effectMultiplier) {
        // Appliquer les effets saisonniers
        for (ResourceStateInfo stateInfo : resourceStates.values()) {
            stateInfo.modifiers.put("season_" + season, effectMultiplier);
        }
    }

    // **MÉTHODES D'INFORMATION**
    public List<String> getMaintenanceRecommendations() {
        List<String> recommendations = new ArrayList<>();

        for (Map.Entry<DATABASE.ResourceType, ResourceStateInfo> entry : resourceStates.entrySet()) {
            ResourceStateInfo stateInfo = entry.getValue();
            if (stateInfo.currentState.requiresMaintenance()) {
                recommendations.add("Maintenance requise pour " + entry.getKey().getName());
            }
        }

        for (Map.Entry<String, ResourceStateInfo> entry : buildingStates.entrySet()) {
            ResourceStateInfo stateInfo = entry.getValue();
            if (stateInfo.currentState.requiresMaintenance()) {
                recommendations.add("Maintenance requise pour le bâtiment " + entry.getKey());
            }
        }

        return recommendations;
    }

    public String getDetailedStatusReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== RAPPORT D'ÉTAT - ").append(hexKey).append(" ===\n\n");

        report.append("RESSOURCES:\n");
        for (Map.Entry<DATABASE.ResourceType, ResourceStateInfo> entry : resourceStates.entrySet()) {
            DATABASE.ResourceType resource = entry.getKey();
            ResourceStateInfo stateInfo = entry.getValue();

            report.append(String.format("- %s: %s (Efficacité: %.0f%%)\n",
                    resource.getName(),
                    stateInfo.currentState.getDisplayName(),
                    stateInfo.currentState.getEfficiencyMultiplier() * 100));

            if (stateInfo.currentState.isFiniteResource()) {
                report.append(String.format("  Réserves estimées: %.1f années\n",
                        stateInfo.remainingYields));
            }

            if (stateInfo.weeksSinceLastMaintenance > 26) {
                report.append(String.format("  Dernière maintenance: %d semaines\n",
                        stateInfo.weeksSinceLastMaintenance));
            }
        }

        report.append("\nBÂTIMENTS:\n");
        for (Map.Entry<String, ResourceStateInfo> entry : buildingStates.entrySet()) {
            String buildingType = entry.getKey();
            ResourceStateInfo stateInfo = entry.getValue();

            report.append(String.format("- %s: %s (Efficacité: %.0f%%)\n",
                    buildingType,
                    stateInfo.currentState.getDisplayName(),
                    stateInfo.currentState.getEfficiencyMultiplier() * 100));
        }

        return report.toString();
    }
}