package com.iseria.domain;

public enum ResourceState {
    // **ÉTATS DE BASE**
    ABUNDANT("Abondante", 1.0, 0.0, false),
    NORMAL("Normale", 1.0, 0.0, false),

    // **ÉTATS DE DÉPLÉTION (pour ressources finies)**
    DEPLETING_SLOW("En cours d'épuisement", 0.85, 0.02, true),
    DEPLETING_MODERATE("Épuisement modéré", 0.70, 0.05, true),
    DEPLETING_FAST("Épuisement rapide", 0.50, 0.10, true),
    NEARLY_EXHAUSTED("Presque épuisé", 0.25, 0.15, true),
    EXHAUSTED("Épuisé", 0.0, 0.0, true),

    // **ÉTATS D'EFFICACITÉ DÉGRADÉE (pour bâtiments de production)**
    EFFICIENCY_DECLINE_MINOR("Efficacité réduite", 0.90, 0.01, false),
    EFFICIENCY_DECLINE_MODERATE("Efficacité dégradée", 0.75, 0.03, false),
    EFFICIENCY_DECLINE_MAJOR("Efficacité très réduite", 0.60, 0.05, false),
    MAINTENANCE_REQUIRED("Maintenance requise", 0.40, 0.08, false),

    // **ÉTATS TEMPORAIRES**
    SEASONAL_BONUS("Bonus saisonnier", 1.25, 0.0, false),
    WEATHER_PENALTY("Pénalité météo", 0.80, 0.0, false),
    TECHNOLOGY_BOOST("Amélioration technologique", 1.15, 0.0, false);

    private final String displayName;
    private final double efficiencyMultiplier;
    private final double weeklyDegradation; // Dégradation par semaine
    private final boolean isFiniteResource; // True pour mines, faux pour fermes

    ResourceState(String displayName, double efficiencyMultiplier,
                  double weeklyDegradation, boolean isFiniteResource) {
        this.displayName = displayName;
        this.efficiencyMultiplier = efficiencyMultiplier;
        this.weeklyDegradation = weeklyDegradation;
        this.isFiniteResource = isFiniteResource;
    }

    public String getDisplayName() { return displayName; }
    public double getEfficiencyMultiplier() { return efficiencyMultiplier; }
    public double getWeeklyDegradation() { return weeklyDegradation; }
    public boolean isFiniteResource() { return isFiniteResource; }

    public ResourceState degradeOneLevel() {
        return switch (this) {
            case ABUNDANT -> NORMAL;
            case NORMAL -> isFiniteResource ? DEPLETING_SLOW : EFFICIENCY_DECLINE_MINOR;
            case DEPLETING_SLOW -> DEPLETING_MODERATE;
            case DEPLETING_MODERATE -> DEPLETING_FAST;
            case DEPLETING_FAST -> NEARLY_EXHAUSTED;
            case NEARLY_EXHAUSTED -> EXHAUSTED;
            case EFFICIENCY_DECLINE_MINOR -> EFFICIENCY_DECLINE_MODERATE;
            case EFFICIENCY_DECLINE_MODERATE -> EFFICIENCY_DECLINE_MAJOR;
            case EFFICIENCY_DECLINE_MAJOR -> MAINTENANCE_REQUIRED;
            default -> this; // États terminaux ou temporaires
        };
    }

    public boolean canDegrade() {
        return this != EXHAUSTED && this != MAINTENANCE_REQUIRED;
    }

    public boolean requiresMaintenance() {
        return this == MAINTENANCE_REQUIRED || this == EXHAUSTED;
    }

    public boolean isProductive() {
        return efficiencyMultiplier > 0;
    }
}