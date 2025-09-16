package com.iseria.service;

import com.iseria.domain.Rumor;
import com.iseria.domain.DATABASE;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface RumorService {
    // ✅ MÉTHODES COMPATIBLES TNCD
    Rumor createFromTNCD(String type, String name, String content, LocalDateTime date);
    List<Rumor> findByType(String type);
    List<Rumor> findByNameContaining(String nameFragment);

    // ✨ NOUVELLES MÉTHODES pour workflow admin
    Rumor submitRumor(String type, String name, String content, String userId);
    void approveRumor(Long rumorId, String adminId, Set<String> targetFactions);
    void rejectRumor(Long rumorId, String adminId, String reason);

    // Gestion
    Rumor saveRumor(Rumor rumor);
    List<Rumor> getAllRumors();
    List<Rumor> getPendingRumors();
    Rumor findById(Long id);

}

