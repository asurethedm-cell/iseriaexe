package com.iseria.service;

import com.iseria.domain.Rumor;
import com.iseria.domain.DATABASE;
import com.iseria.service.RumorService;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class RumorServiceImpl implements RumorService {
    private final Map<Long, Rumor> rumors = new HashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);
    private final RumorPersistenceService persistenceService = new RumorPersistenceService();

    // ✨ NOUVEAU : Chargement automatique au démarrage
    public RumorServiceImpl() {
        loadRumorsFromDisk();
    }

    // ✨ NOUVEAU : Méthodes de persistance
    public void loadRumorsFromDisk() {
        try {
            List<Rumor> loadedRumors = persistenceService.loadRumors();
            System.out.println("Chargement de " + loadedRumors.size() + " rumeurs depuis le disque");

            for (Rumor rumor : loadedRumors) {
                rumors.put(rumor.getId(), rumor);
                // Ajuster le compteur d'ID pour éviter les conflits
                if (rumor.getId() >= idCounter.get()) {
                    idCounter.set(rumor.getId() + 1);
                }
            }
        } catch (Exception e) {
            System.out.println("Aucune rumeur sauvegardée trouvée ou erreur de chargement");
        }
    }

    public void saveRumorsToDisk() {
        try {
            List<Rumor> allRumors = new ArrayList<>(rumors.values());
            persistenceService.saveRumors(allRumors);
            System.out.println("Sauvegarde de " + allRumors.size() + " rumeurs sur le disque");
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde des rumeurs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Rumor saveRumor(Rumor rumor) {
        rumors.put(rumor.getId(), rumor);
        saveRumorsToDisk();
        return rumor;
    }

    @Override
    public Rumor createFromTNCD(String type, String name, String content, LocalDateTime date) {
        Rumor rumor = new Rumor(type, name, content, date);
        rumor.setId(idCounter.getAndIncrement());
        rumor.setStatus(DATABASE.RumorStatus.APPROVED); // Migration Excel = validé
        return saveRumor(rumor);
    }

    @Override
    public Rumor submitRumor(String type, String name, String content, String userId) {
        Rumor rumor = new Rumor(type, name, content, LocalDateTime.now());
        rumor.setId(idCounter.getAndIncrement());
        rumor.setSubmittedBy(userId);
        rumor.setStatus(DATABASE.RumorStatus.PENDING);
        return saveRumor(rumor);
    }

    @Override
    public void approveRumor(Long rumorId, String adminId, Set<String> targetFactions) {
        Rumor rumor = findById(rumorId);
        if (rumor != null) {
            rumor.setStatus(DATABASE.RumorStatus.APPROVED);
            rumor.setValidatedBy(adminId);
            rumor.setValidationDate(LocalDateTime.now());
            rumor.setTargetFactions(targetFactions);
            saveRumor(rumor);
        }
    }

    @Override
    public void rejectRumor(Long rumorId, String adminId, String reason) {
        Rumor rumor = findById(rumorId);
        if (rumor != null) {
            rumor.setStatus(DATABASE.RumorStatus.REJECTED);
            rumor.setValidatedBy(adminId);
            rumor.setValidationDate(LocalDateTime.now());
            saveRumor(rumor);
        }
    }

    @Override
    public List<Rumor> findByType(String type) {
        return rumors.values().stream()
                .filter(rumor -> type.equals(rumor.getType()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Rumor> findByNameContaining(String nameFragment) {
        return rumors.values().stream()
                .filter(rumor -> rumor.getName().toLowerCase().contains(nameFragment.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Rumor> getAllRumors() {
        return new ArrayList<>(rumors.values());
    }
    public Long getNextId() {
        return idCounter.getAndIncrement();
    }


    @Override
    public List<Rumor> getPendingRumors() {
        return rumors.values().stream()
                .filter(rumor -> rumor.getStatus() == DATABASE.RumorStatus.PENDING)
                .collect(Collectors.toList());
    }

    @Override
    public Rumor findById(Long id) {
        return rumors.get(id);
    }
}
