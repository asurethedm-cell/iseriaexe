package com.iseria.ui;

import com.iseria.domain.DATABASE;
import com.iseria.domain.Rumor;
import com.iseria.service.RumorService;

import javax.swing.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class AdminRumorPanel extends JPanel {
    private RumorService rumorService;

    public AdminRumorPanel(RumorService rumorService) {
        this.rumorService = rumorService;
    }

    private void approveRumor(Rumor rumor) {
        // Le format TNCD reste inchangé
        // Seules les métadonnées changent
        rumor.setStatus(DATABASE.RumorStatus.APPROVED);
        rumor.setValidatedBy(Login.currentUser);
        rumor.setValidationDate(LocalDateTime.now());

        // Sélection des cibles sans modifier TNCD
        Set<String> targets = showTargetSelectionDialog();
        rumor.setTargetFactions(targets);

        rumorService.saveRumor(rumor);

        // ✅ Les utilisateurs voient toujours le format TNCD classique
        broadcastToFactions(rumor, targets);
    }

    // Méthodes helper manquantes
    private Set<String> showTargetSelectionDialog() {
        // TODO: Implémenter dialogue de sélection des factions
        return new HashSet<>();
    }

    private void broadcastToFactions(Rumor rumor, Set<String> targets) {
        // TODO: Implémenter diffusion aux factions ciblées
        System.out.println("Broadcasting rumor to: " + targets);
    }
}
