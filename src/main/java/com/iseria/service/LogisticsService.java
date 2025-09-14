package com.iseria.service;

import com.iseria.domain.*;
import com.iseria.ui.MainMenu;

import java.util.*;

public class LogisticsService {
    private final Map<String, List<Route>> transportNetwork = new HashMap<>();
    private final Map<String, StorageWarehouse> warehouses = new HashMap<>();
    private final IHexRepository hexRepository;

    public LogisticsService(IHexRepository hexRepository) {
        this.hexRepository = hexRepository;
        initializeNetwork();
    }
    private void initializeNetwork() {
        Map<String, HexDetails> hexGrid = hexRepository.loadAll();

        // 🔧 DEBUG: Vérifier le contenu du hexGrid
        System.out.println("🔍 Initialisation réseau logistique...");
        System.out.println("Nombre d'hexagones total: " + hexGrid.size());

        // ✅ NOUVEAU: Obtenir la faction du joueur connecté
        String currentFactionId = MainMenu.getCurrentFactionId();
        if (currentFactionId == null) {
            System.err.println("❌ Impossible d'obtenir la faction du joueur connecté!");
            return;
        }

        // ✅ NOUVEAU: Filtrer seulement les hexagones de la faction du joueur
        Map<String, HexDetails> playerHexes = new HashMap<>();
        int nullKeyCount = 0;

        for (Map.Entry<String, HexDetails> entry : hexGrid.entrySet()) {
            String hexKey = entry.getKey();
            HexDetails details = entry.getValue();

            if (hexKey == null) {
                nullKeyCount++;
                System.err.println("⚠️ Clé null détectée dans hexGrid!");
                continue;
            }


            // ✅ NOUVEAU: Vérifier si l'hexagone appartient à la faction du joueur
            if (details != null && currentFactionId.equals(details.getFactionClaim())) {
                playerHexes.put(hexKey, details);
                List<TransportVehicle> savedVehicles = details.getAssignedVehicles();
                if (!savedVehicles.isEmpty()) {
                    getVehiclesByHex().put(hexKey, new ArrayList<>(savedVehicles));
                    System.out.println("🚚 Rechargé " + savedVehicles.size() +
                            " véhicules pour " + hexKey);}

                // Afficher quelques exemples pour debug
                if (playerHexes.size() <= 10 || Math.random() < 0.1) {
                    System.out.println("Hex de faction trouvé: " + hexKey);
                }
            }
        }

        if (nullKeyCount > 0) {
            System.err.println("❌ " + nullKeyCount + " clés null trouvées dans hexGrid!");
        }

        System.out.println("Hexagones de la faction " + currentFactionId + ": " + playerHexes.size());

        if (playerHexes.isEmpty()) {
            System.out.println("⚠️ Aucun hexagone trouvé pour la faction " + currentFactionId);
            return;
        }


        for (String hexKey : playerHexes.keySet()) {
            List<String> neighbors = getNeighboringHexes(hexKey);
            for (String neighbor : neighbors) {
                // ✅ NOUVEAU: Vérifier que le voisin appartient aussi à la faction du joueur
                if (neighbor != null && playerHexes.containsKey(neighbor)) {
                    createRoute(hexKey, neighbor);
                }
            }
        }

        // ✅ MODIFIÉ: Initialiser les entrepôts seulement pour les hexagones du joueur
        initializeWarehouses(playerHexes);

        System.out.println("✅ Réseau logistique initialisé pour la faction " + currentFactionId + ":");
        System.out.println("- Routes créées: " + transportNetwork.size());
        System.out.println("- Entrepôts trouvés: " + warehouses.size());
    }
    private void initializeWarehouses(Map<String, HexDetails> playerHexes) {
        for (Map.Entry<String, HexDetails> entry : playerHexes.entrySet()) {
            String hexKey = entry.getKey();
            HexDetails hex = entry.getValue();

            if (hexKey != null && hex != null && isWarehouseBuilding(hex)) {
                try {
                    DATABASE.MainBuilding building = DATABASE.MainBuilding.values()[hex.getMainBuildingIndex()];
                    warehouses.put(hexKey, new StorageWarehouse(hexKey, building));
                    System.out.println("📦 Entrepôt créé en " + hexKey + " pour faction " + hex.getFactionClaim());
                } catch (Exception e) {
                    System.err.println("Erreur création entrepôt " + hexKey + ": " + e.getMessage());
                }
            }
        }
    }
    public int calculateTransportTime(String fromHex, String toHex,
                                      String resourceType, double quantity) {
        System.out.println("🚛 Calcul transport: " + fromHex + " → " + toHex +
                " (" + quantity + " " + resourceType + ")");

        // 🆕 NOUVEAU : Distance de base (toujours possible)
        int baseDistance = calculateDistance(fromHex, toHex);
        if (baseDistance == Integer.MAX_VALUE) {
            System.out.println("❌ Hexagones non connectés");
            return Integer.MAX_VALUE; // Vraiment impossible
        }

        // 🆕 Temps de base : 1 jour par tuile (règle de base)
        int baseTime = Math.max(1, baseDistance);
        double speedMultiplier = 1.0;

        System.out.println("📏 Distance de base: " + baseDistance + " tuiles");
        System.out.println("⏰ Temps de base: " + baseTime + " jours");

        // 🆕 Chercher route explicite pour les BONUS seulement
        Route route = findRoute(fromHex, toHex);
        if (route != null && route.hasRoad()) {
            speedMultiplier += 1.0; // +100% si route
            System.out.println("🛣️ Bonus route: +100% vitesse");
        }

        // 🆕 Bonus véhicule selon les règles
        TransportVehicle vehicle = getBestVehicleForResource(fromHex, resourceType);
        if (vehicle.getType() != TransportVehicle.VehicleType.NONE) {
            System.out.println("🚚 Véhicule utilisé: " + vehicle.getType().name());

            if (isLandVehicle(vehicle.getType())) {
                speedMultiplier += 1.0; // +100% pour véhicules terrestres
                System.out.println("⚡ Bonus véhicule terrestre: +100% vitesse");
            }

            // 🆕 Bonus bateaux selon terrain de la route
            if (route != null) {
                if (vehicle.getType() == TransportVehicle.VehicleType.BATEAU_RIVIERE && route.hasRiver()) {
                    speedMultiplier += 1.5; // +150% sur rivière
                    System.out.println("🚤 Bonus bateau rivière: +150% vitesse");
                } else if (vehicle.getType() == TransportVehicle.VehicleType.BATEAU_MER && route.hasSea()) {
                    speedMultiplier += 3.0; // +300% sur mer
                    System.out.println("🚢 Bonus bateau mer: +300% vitesse");
                }
            }
        } else {
            System.out.println("🚶 Pas de véhicule - transport à pied");
        }

        // 🆕 Bonus bâtiments producteurs près de l'eau (selon règles)
        double buildingBonus = calculateBuildingBonus(fromHex, toHex);
        if (buildingBonus > 0) {
            speedMultiplier += buildingBonus;
            System.out.println("🏭 Bonus bâtiments: +" + (buildingBonus * 100) + "% vitesse");
        }
        int vehicleCapacity = vehicle.getCapacityForResource(resourceType);
        int trips = Math.max(1, (int) Math.ceil(quantity / vehicleCapacity));
        int finalTime = Math.max(1, (int) (baseTime / speedMultiplier * trips));

        System.out.println("📊 Résumé transport:");
        System.out.println("  - Multiplicateur vitesse: " + String.format("%.2f", speedMultiplier));
        System.out.println("  - Capacité véhicule: " + vehicleCapacity + " " + resourceType);
        System.out.println("  - Voyages nécessaires: " + trips);
        System.out.println("  - Temps total: " + finalTime + " jours");

        return finalTime;
    }

    private boolean isLandVehicle(TransportVehicle.VehicleType type) {
        return type == TransportVehicle.VehicleType.CHARRETTE ||
                type == TransportVehicle.VehicleType.CHARIOT ||
                type == TransportVehicle.VehicleType.WAGON;
    }
    private double calculateBuildingBonus(String fromHex, String toHex) {
        try {
            HexDetails fromDetails = hexRepository.getHexDetails(fromHex);
            HexDetails toDetails = hexRepository.getHexDetails(toHex);

            double bonus = 0.0;

            // Vérifier bâtiments producteurs sur le trajet
            if (fromDetails != null && hasProducerBuilding(fromDetails)) {
                if (fromDetails.getLogisticsData().hasRiver()) {
                    bonus += 1.5; // +150% rivière
                } else if (fromDetails.getLogisticsData().hasSea()) {
                    bonus += 3.0; // +300% mer
                }
            }

            if (toDetails != null && hasProducerBuilding(toDetails)) {
                if (toDetails.getLogisticsData().hasRiver()) {
                    bonus += 1.5; // +150% rivière
                } else if (toDetails.getLogisticsData().hasSea()) {
                    bonus += 3.0; // +300% mer
                }
            }

            return Math.min(bonus, 3.0); // Plafonner le bonus total

        } catch (Exception e) {
            System.err.println("Erreur calcul bonus bâtiments: " + e.getMessage());
            return 0.0;
        }
    }
    private boolean hasProducerBuilding(HexDetails hex) {
        // À adapter selon votre logique de bâtiments producteurs
        return hex.getMainBuildingIndex() > 0 ||
                hex.getAuxBuildingIndex() > 0;
    }
    public StorageWarehouse findNearestWarehouse(String hexKey) {
        return warehouses.values().stream()
                .min((w1, w2) -> Integer.compare(
                        calculateDistance(hexKey, w1.getHexKey()),
                        calculateDistance(hexKey, w2.getHexKey())
                ))
                .orElse(null);
    }

    public boolean assignVehicle(String hexKey, TransportVehicle vehicle) {
        HexDetails hex = hexRepository.getHexDetails(hexKey);
        if (hex != null) {
            hex.addVehicle(vehicle);
            hexRepository.updateHexDetails(hexKey, hex);
            return true;
        }
        return false;
    }

    private TransportVehicle getBestVehicleForResource(String hexKey, String resourceType) {
        HexDetails hex = hexRepository.getHexDetails(hexKey);
        if (hex == null) return TransportVehicle.DEFAULT;

        List<TransportVehicle> vehicles = hex.getAssignedVehicles();
        if (vehicles.isEmpty()) {
            return TransportVehicle.DEFAULT;
        }

        return vehicles.stream()
                .max((v1, v2) -> Integer.compare(
                        v1.getCapacityForResource(resourceType),
                        v2.getCapacityForResource(resourceType)
                ))
                .orElse(TransportVehicle.DEFAULT);
    }

    private boolean isWarehouseBuilding(HexDetails hex) {
        if (hex.getMainBuildingIndex() == 0) return false;
        try {
            DATABASE.MainBuilding building = DATABASE.MainBuilding.values()[hex.getMainBuildingIndex()];
            return building.getBuildName().toLowerCase().contains("entrepôt");
        } catch (Exception e) {
            return false;
        }
    }

    private Route findRoute(String fromHex, String toHex) {
        List<Route> routes = transportNetwork.get(fromHex);
        if (routes == null) return null;

        return routes.stream()
                .filter(route -> route.getToHex().equals(toHex))
                .findFirst()
                .orElse(null);
    }

    // 🔧 CORRECTION MAJEURE : Gérer le format (row,col) de vos hexagones
    private int calculateDistance(String fromHex, String toHex) {
        if (fromHex == null || toHex == null) {
            System.err.println("❌ Hexagone null: from=" + fromHex + ", to=" + toHex);
            return Integer.MAX_VALUE;
        }

        try {
            int[] fromCoord = parseHexKey(fromHex);
            int[] toCoord = parseHexKey(toHex);

            if (fromCoord == null || toCoord == null) {
                System.err.println("❌ Impossible de parser les coordonnées: " + fromHex + " -> " + toHex);
                return Integer.MAX_VALUE;
            }

            // Distance hexagonale correcte
            return calculateHexDistance(fromCoord, toCoord);
        } catch (Exception e) {
            System.err.println("Erreur calcul distance " + fromHex + " -> " + toHex + ": " + e.getMessage());
            return Integer.MAX_VALUE;
        }
    }

    // 🆕 NOUVELLE MÉTHODE : Parser le format de vos hexagones
    private int[] parseHexKey(String hexKey) {
        if (hexKey == null || hexKey.trim().isEmpty()) {
            return null;
        }

        try {
            // Format attendu: "(row,col)" d'après votre image
            if (hexKey.startsWith("(") && hexKey.endsWith(")")) {
                String coords = hexKey.substring(1, hexKey.length() - 1);
                String[] parts = coords.split(",");
                if (parts.length == 2) {
                    int row = Integer.parseInt(parts[0].trim());
                    int col = Integer.parseInt(parts[1].trim());
                    return new int[]{row, col};
                }
            }

            // Format alternatif: "row,col" sans parenthèses
            if (hexKey.contains(",")) {
                String[] parts = hexKey.split(",");
                if (parts.length == 2) {
                    int row = Integer.parseInt(parts[0].trim());
                    int col = Integer.parseInt(parts[1].trim());
                    return new int[]{row, col};
                }
            }

            // Format alternatif: utiliser IHexRepository si disponible
            int[] repoCoords = hexRepository.getHexPosition(hexKey);
            if (repoCoords != null) {
                return repoCoords;
            }

        } catch (NumberFormatException e) {
            System.err.println("Format de coordonnées invalide: " + hexKey);
        }

        return null;
    }

    private int calculateHexDistance(int[] from, int[] to) {
        // Distance Manhattan pour simplifier (vous pouvez améliorer plus tard)
        return Math.abs(from[0] - to[0]) + Math.abs(from[1] - to[1]);
    }

    // 🔧 CORRECTION : Adaptation pour le format (row,col)
    private List<String> getNeighboringHexes(String hexKey) {
        List<String> neighbors = new ArrayList<>();

        if (hexKey == null) {
            System.err.println("❌ getNeighboringHexes appelé avec hexKey null!");
            return neighbors;
        }

        try {
            int[] currentPos = parseHexKey(hexKey);
            if (currentPos == null) {
                System.err.println("❌ Impossible de parser hexKey: " + hexKey);
                return neighbors;
            }

            int row = currentPos[0];
            int col = currentPos[1];

            // Les 6 voisins hexagonaux (adaptez selon votre grille)
            int[][] hexDirections = {
                    {-1, 0}, {-1, 1},  // Nord-Ouest, Nord-Est
                    {0, -1}, {0, 1},   // Ouest, Est
                    {1, -1}, {1, 0}    // Sud-Ouest, Sud-Est
            };

            for (int[] direction : hexDirections) {
                int newRow = row + direction[0];
                int newCol = col + direction[1];

                // Construire la clé du voisin selon votre format
                String neighborKey = "(" + newRow + "," + newCol + ")";

                // Vérifier que ce voisin existe
                if (hexRepository.getHexDetails(neighborKey) != null) {
                    neighbors.add(neighborKey);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur recherche voisins pour " + hexKey + ": " + e.getMessage());
        }

        return neighbors;
    }

    private void createRoute(String fromHex, String toHex) {
        if (fromHex == null || toHex == null) {
            System.err.println("❌ Tentative de création de route avec hex null");
            return;
        }

        HexDetails fromHexDetails = hexRepository.getHexDetails(fromHex);
        HexDetails toHexDetails = hexRepository.getHexDetails(toHex);

        boolean hasRoad = (fromHexDetails != null && fromHexDetails.getLogisticsData().hasRoad()) ||
                (toHexDetails != null && toHexDetails.getLogisticsData().hasRoad());

        boolean hasRiver = (fromHexDetails != null && fromHexDetails.getLogisticsData().hasRiver()) ||
                (toHexDetails != null && toHexDetails.getLogisticsData().hasRiver());

        boolean hasSea = (fromHexDetails != null && fromHexDetails.getLogisticsData().hasSea()) ||
                (toHexDetails != null && toHexDetails.getLogisticsData().hasSea());

        int distance = calculateDistance(fromHex, toHex);
        Route route = new Route(fromHex, toHex, distance, hasRoad, hasRiver, hasSea);
        transportNetwork.computeIfAbsent(fromHex, k -> new ArrayList<>()).add(route);
    }

    // Méthodes publiques pour debug
    public void printTransportNetwork() {
        System.out.println("=== RÉSEAU DE TRANSPORT ===");
        System.out.println("Hexagones connectés: " + transportNetwork.size());

        Map<String, List<TransportVehicle>> vehiclesByHex = getVehiclesByHex();
        System.out.println("Véhicules par hexagone (depuis HexDetails):");
        for (Map.Entry<String, List<TransportVehicle>> entry : vehiclesByHex.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue().size() + " véhicules");
        }

        System.out.println("Entrepôts:");
        for (StorageWarehouse warehouse : warehouses.values()) {
            System.out.println(warehouse.getHexKey() + ": Capacité " + warehouse.getMaxCapacity());
        }
    }

    public Map<String, StorageWarehouse> getWarehouses() {
        return new HashMap<>(warehouses);
    }
    public Map<String, List<TransportVehicle>> getVehiclesByHex() {
        String currentFactionId = MainMenu.getCurrentFactionId();
        if (currentFactionId == null) return new HashMap<>();

        Map<String, List<TransportVehicle>> result = new HashMap<>();
        Map<String, HexDetails> allHexes = hexRepository.loadAll();

        for (Map.Entry<String, HexDetails> entry : allHexes.entrySet()) {
            HexDetails hex = entry.getValue();
            if (hex != null && currentFactionId.equals(hex.getFactionClaim())) {
                List<TransportVehicle> vehicles = hex.getAssignedVehicles();
                if (!vehicles.isEmpty()) {
                    result.put(entry.getKey(), new ArrayList<>(vehicles));
                }
            }
        }
        return result;
    }
    public boolean removeVehicleFromHex(String hexKey, String vehicleInfo) {
        HexDetails hex = hexRepository.getHexDetails(hexKey);
        if (hex != null) {
            List<TransportVehicle> vehicles = hex.getAssignedVehicles();
            boolean removed = vehicles.removeIf(vehicle -> {
                String info = String.format("%s (Speed: %.1fx)",
                        vehicle.getType().name(),
                        vehicle.getSpeedMultiplier());
                return info.equals(vehicleInfo);
            });

            if (removed) {
                // ✅ NOUVEAU: Effacer et remettre la liste modifiée
                hex.clearVehicles();
                vehicles.forEach(hex::addVehicle);
                hexRepository.updateHexDetails(hexKey, hex);
                return true;
            }
        }
        return false;
    }
}
