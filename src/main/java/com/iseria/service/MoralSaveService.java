package com.iseria.service;

import com.iseria.domain.Faction;
import com.iseria.domain.DATABASE;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MoralSaveService {
    private static final String SAVE_DIR = "Iseria_Data";
    private static final String FILE_EXTENSION = ".json";
    private final ObjectMapper mapper = new ObjectMapper();

    public static class MoralSaveData {
        public String username;
        public String factionId;
        public String factionType;
        public Map<String, String> selectedActions; // dropdown_1 -> action_name
        public String lastSaved;
        public double calculatedMoral;
        public int calculatedInstability;

        public MoralSaveData() {
            this.selectedActions = new HashMap<>();
            this.lastSaved = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }

    private Path getSaveDirectory() {
        String documentsPath = FileSystemView.getFileSystemView().getDefaultDirectory().getPath();
        Path saveDir = Paths.get(documentsPath, SAVE_DIR);

        try {
            if (!Files.exists(saveDir)) {
                Files.createDirectories(saveDir);
            }
        } catch (IOException e) {
            System.err.println("Erreur création répertoire de sauvegarde: " + e.getMessage());
        }

        return saveDir;
    }

    private String getSaveFileName(String username, String factionId) {
        return String.format("moral_%s_%s%s",
                username.toLowerCase(),
                factionId.toLowerCase(),
                FILE_EXTENSION);
    }

    public void saveMoralSelections(String username, Faction faction,
                                    Map<String, JComboBox<DATABASE.MoralAction>> dropdownMap,
                                    double moralTotal, int instabilityTotal) {

        MoralSaveData saveData = new MoralSaveData();
        saveData.username = username;
        saveData.factionId = faction.getId();
        saveData.factionType = faction.getFactionType();
        saveData.calculatedMoral = moralTotal;
        saveData.calculatedInstability = instabilityTotal;

        // Extraire les sélections des dropdowns
        for (Map.Entry<String, JComboBox<DATABASE.MoralAction>> entry : dropdownMap.entrySet()) {
            String dropdownKey = entry.getKey();
            DATABASE.MoralAction selected = (DATABASE.MoralAction) entry.getValue().getSelectedItem();

            if (selected != null) {
                saveData.selectedActions.put(dropdownKey, selected.name());
            }
        }

        Path saveFile = getSaveDirectory().resolve(getSaveFileName(username, faction.getId()));

        try {
            mapper.writeValue(saveFile.toFile(), saveData);
            System.out.println("Moral data saved to: " + saveFile.toString());
        } catch (IOException e) {
            System.err.println("Erreur sauvegarde moral data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public MoralSaveData loadMoralSelections(String username, Faction faction) {
        Path saveFile = getSaveDirectory().resolve(getSaveFileName(username, faction.getId()));

        if (!Files.exists(saveFile)) {
            return new MoralSaveData(); // Retourne un objet vide si pas de sauvegarde
        }

        try {
            return mapper.readValue(saveFile.toFile(), MoralSaveData.class);
        } catch (IOException e) {
            System.err.println("Erreur chargement moral data: " + e.getMessage());
            return new MoralSaveData();
        }
    }

    public void applyLoadedSelections(Map<String, JComboBox<DATABASE.MoralAction>> dropdownMap,
                                      MoralSaveData loadedData) {

        for (Map.Entry<String, String> entry : loadedData.selectedActions.entrySet()) {
            String dropdownKey = entry.getKey();
            String actionName = entry.getValue();

            JComboBox<DATABASE.MoralAction> dropdown = dropdownMap.get(dropdownKey);
            if (dropdown != null) {
                try {
                    DATABASE.MoralAction action = DATABASE.MoralAction.valueOf(actionName);
                    dropdown.setSelectedItem(action);
                } catch (IllegalArgumentException e) {
                    System.err.println("Action non trouvée: " + actionName);
                }
            }
        }
    }

    // Méthode pour préparer la future transition client-serveur
    public String exportToServerFormat(String username, Faction faction) {
        MoralSaveData data = loadMoralSelections(username, faction);

        try {
            ObjectNode serverData = mapper.createObjectNode();
            serverData.put("playerId", username);
            serverData.put("factionId", faction.getId());
            serverData.put("timestamp", data.lastSaved);
            serverData.put("moralTotal", data.calculatedMoral);
            serverData.put("instabilityTotal", data.calculatedInstability);

            ObjectNode actions = mapper.createObjectNode();
            for (Map.Entry<String, String> entry : data.selectedActions.entrySet()) {
                actions.put(entry.getKey(), entry.getValue());
            }
            serverData.set("selectedActions", actions);

            return mapper.writeValueAsString(serverData);
        } catch (Exception e) {
            System.err.println("Erreur export serveur: " + e.getMessage());
            return "{}";
        }
    }

    // Méthode pour la future réception des données serveur
    public MoralSaveData importFromServerFormat(String serverJson) {
        try {
            JsonNode serverData = mapper.readTree(serverJson);
            MoralSaveData data = new MoralSaveData();

            data.username = serverData.get("playerId").asText();
            data.factionId = serverData.get("factionId").asText();
            data.lastSaved = serverData.get("timestamp").asText();
            data.calculatedMoral = serverData.get("moralTotal").asDouble();
            data.calculatedInstability = serverData.get("instabilityTotal").asInt();

            JsonNode actions = serverData.get("selectedActions");
            if (actions != null) {
                actions.fields().forEachRemaining(entry -> {
                    data.selectedActions.put(entry.getKey(), entry.getValue().asText());
                });
            }

            return data;
        } catch (Exception e) {
            System.err.println("Erreur import serveur: " + e.getMessage());
            return new MoralSaveData();
        }
    }

    // Utilitaire pour lister toutes les sauvegardes d'un utilisateur
    public List<MoralSaveData> getAllUserSaves(String username) {
        List<MoralSaveData> saves = new ArrayList<>();
        Path saveDir = getSaveDirectory();

        try {
            Files.list(saveDir)
                    .filter(path -> path.getFileName().toString().startsWith("moral_" + username.toLowerCase()))
                    .forEach(path -> {
                        try {
                            saves.add(mapper.readValue(path.toFile(), MoralSaveData.class));
                        } catch (IOException e) {
                            System.err.println("Erreur lecture: " + path.getFileName());
                        }
                    });
        } catch (IOException e) {
            System.err.println("Erreur listage sauvegardes: " + e.getMessage());
        }

        return saves;
    }
}
