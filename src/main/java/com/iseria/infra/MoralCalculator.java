package com.iseria.infra;

import com.iseria.domain.DATABASE.MoralAction;

import java.util.List;

/** Pure calculation of moral sum from MoralActions. */
public class MoralCalculator {

    public static double sumMoral(List<MoralAction> selectedActions) {
        double total = 5; // Base moral
        double calTotal;
        for (MoralAction action : selectedActions) {
            if (action != null) {
                if (action.getBaseMoralModifier()!= 0){
                    total = total + action.getBaseMoralModifier();}
                total = total + action.getMoralEffect();
            }
        }

        return total;
    }

    // Méthode pour calculer l'instabilité totale
    public static int sumInstability(List<MoralAction> selectedActions) {
        return selectedActions.stream()
                .filter(action -> action != null)
                .mapToInt(MoralAction::getInstabilityEffect)
                .sum();
    }
}
