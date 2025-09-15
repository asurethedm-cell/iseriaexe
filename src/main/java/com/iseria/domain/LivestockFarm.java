package com.iseria.domain;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class LivestockFarm implements Serializable {
    @Serial
    private static final long serialVersionUID = 2L;

    private Map<DATABASE.LivestockData, LivestockHerd> herds = new HashMap<>();
    private String hexKey;
    private boolean isAnnexe = true; // Commence comme annexe

    public static class LivestockHerd implements Serializable {
        private int nombreBetes = 0;
        private int nombreReproducteurs = 0;
        private double nourritureParSemaine = 0.0; // Investissement choisi
        private int semainesRestantes = 0; // Pour la maturation
        private boolean estEnProduction = false;
        private double productionHebdomadaire = 0.0;
        private Map<String, Double> sousProduits = new HashMap<>();

        // Constructeurs et getters/setters
        public LivestockHerd() {}

        // Getters et setters
        public int getNombreBetes() { return nombreBetes; }
        public void setNombreBetes(int nombreBetes) { this.nombreBetes = nombreBetes; }
        public int getNombreReproducteurs() { return nombreReproducteurs; }
        public void setNombreReproducteurs(int nombreReproducteurs) {
            this.nombreReproducteurs = nombreReproducteurs;
        }
        public double getNourritureParSemaine() { return nourritureParSemaine; }
        public void setNourritureParSemaine(double nourriture) {
            this.nourritureParSemaine = nourriture;
        }
        public int getSemainesRestantes() { return semainesRestantes; }
        public void setSemainesRestantes(int semaines) { this.semainesRestantes = semaines; }
        public boolean isEstEnProduction() { return estEnProduction; }
        public void setEstEnProduction(boolean production) { this.estEnProduction = production; }
        public double getProductionHebdomadaire() { return productionHebdomadaire; }
        public void setProductionHebdomadaire(double production) {
            this.productionHebdomadaire = production;
        }
        public Map<String, Double> getSousProduits() { return sousProduits; }

        public double calculerCoutInitial(DATABASE.LivestockData animalData) {
            double coutBetes = nombreBetes * animalData.getPrixUnite();
            double coutReproducteurs = nombreReproducteurs * animalData.getCoutReproducteur();
            return coutBetes + coutReproducteurs;
        }

        public double calculerProductionEstimee(DATABASE.LivestockData animalData) {
            if (!estEnProduction) return 0.0;

            // Calcul basé sur l'investissement en nourriture
            double ratioInvestissement = nourritureParSemaine /
                    (nombreBetes * animalData.getBesoinMax());

            // Production proportionnelle à l'investissement
            double productionBase = nombreBetes * animalData.getPrixUnite();
            return productionBase * ratioInvestissement * 0.5; // 50% de conversion
        }
    }

    public LivestockFarm(String hexKey) {
        this.hexKey = hexKey;
    }

    // Méthodes de gestion
    public boolean ajouterCheptel(DATABASE.LivestockData animalType, int nombre,
                                  double nourritureParSemaine) {
        if (getTotalAnimaux() + nombre > getCapaciteMaximale()) {
            return false; // Capacité dépassée
        }

        LivestockHerd herd = herds.computeIfAbsent(animalType, k -> new LivestockHerd());
        herd.setNombreBetes(herd.getNombreBetes() + nombre);

        // Calculer le nombre de reproducteurs nécessaires
        int reproducteursNecessaires = (nombre + 9) / 10; // 1 pour 10 bêtes
        herd.setNombreReproducteurs(herd.getNombreReproducteurs() + reproducteursNecessaires);

        // Définir l'investissement en nourriture
        herd.setNourritureParSemaine(nourritureParSemaine);
        herd.setSemainesRestantes(animalType.getToursPourMaturite());

        return true;
    }

    public int getTotalAnimaux() {
        return herds.values().stream()
                .mapToInt(LivestockHerd::getNombreBetes)
                .sum();
    }

    public int getCapaciteMaximale() {
        return isAnnexe ? 10 : Integer.MAX_VALUE;
    }

    public void progresserSemaine() {
        for (Map.Entry<DATABASE.LivestockData, LivestockHerd> entry : herds.entrySet()) {
            LivestockHerd herd = entry.getValue();
            DATABASE.LivestockData animalData = entry.getKey();

            if (herd.getSemainesRestantes() > 0) {
                herd.setSemainesRestantes(herd.getSemainesRestantes() - 1);
                if (herd.getSemainesRestantes() == 0) {
                    // Passage en production
                    herd.setEstEnProduction(true);
                    herd.setProductionHebdomadaire(herd.calculerProductionEstimee(animalData));
                }
            }
        }
    }

    // Conversion en ferme dédiée
    public void convertirEnFermeDediee() {
        this.isAnnexe = false;
    }

    // Getters/Setters
    public Map<DATABASE.LivestockData, LivestockHerd> getHerds() { return herds; }
    public boolean isAnnexe() { return isAnnexe; }
    public String getHexKey() { return hexKey; }

}