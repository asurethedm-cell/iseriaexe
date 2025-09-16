package com.iseria.service;

import com.iseria.domain.Rumor;
import com.iseria.domain.DATABASE;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RumorCacheService {
    // Cache principal par faction
    private final Map<String, List<Rumor>> factionRumorsCache = new ConcurrentHashMap<>();

    // Cache secondaire par type
    private final Map<String, List<Rumor>> rumorsByTypeCache = new ConcurrentHashMap<>();

    public List<Rumor> getRumorsByType(String type, String factionId) {
        return factionRumorsCache.getOrDefault(factionId, Collections.emptyList())
                .stream()
                .filter(rumor -> type.equals(rumor.getType()))
                .collect(Collectors.toList());
    }

    public List<Rumor> getRumorsForFaction(String factionId) {
        return factionRumorsCache.getOrDefault(factionId, Collections.emptyList())
                .stream()
                .filter(rumor -> rumor.getStatus() == DATABASE.RumorStatus.APPROVED)
                .sorted(Comparator.comparing(Rumor::getDate).reversed())
                .collect(Collectors.toList());
    }

    public void addRumorToCache(String factionId, Rumor rumor) {
        factionRumorsCache.computeIfAbsent(factionId, k -> new ArrayList<>()).add(rumor);
    }

    public void clearCache() {
        factionRumorsCache.clear();
        rumorsByTypeCache.clear();
    }
}
