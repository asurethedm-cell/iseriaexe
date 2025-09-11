package com.iseria.infra;

import com.iseria.domain.Faction;
import java.awt.Color;
import java.util.*;

public class FactionRegistry {


    private static final Map<String, Faction> FACTIONS = Map.of(
            "Anima", new Faction(
                    "Anima",
                    "Anima",
                    "/RessourceGen/bg_faction_anima.jpg",
                    "/Emblems/Joueurs/comp_anima_emblem.png",
                    "/profile_anima.png",
                    new Color(244, 127, 6, 255),
                    "Artisane",
                    "/Music/IseriaOST/WAV/animatheme1.wav",
                    "/Music/IseriaOST/WAV/animatheme2.wav",
                    "/Music/IseriaOST/WAV/animatheme3.wav",
                    "/Music/IseriaOST/WAV/animatheme4.wav",
                    "/Music/IseriaOST/WAV/animatheme5.wav",
                    "/Music/IseriaOST/WAV/animatheme6.wav"
            ),

            "decimus", new Faction(
                    "decimus",
                    "decimus",
                    "/RessourceGen/bg_faction_decimus.jpg",
                    "/Emblems/Joueurs/decimus_emblem.png",
                    "/profile_decimus.png",
                    new Color(94, 155, 207, 255),
                    "Religieuse",
                    "/Music/IseriaOST/WAV/decimustheme1.wav",
                    "/Music/IseriaOST/WAV/decimustheme2.wav",
                    "/Music/IseriaOST/WAV/decimustheme3.wav",
                    "/Music/IseriaOST/WAV/decimustheme4.wav",
                    "/Music/IseriaOST/WAV/decimustheme5.wav",
                    "/Music/IseriaOST/WAV/decimustheme6.wav"
            ),

            "DDL", new Faction(
                    "DDL",
                    "Donjon Du Loot",
                    "/bg_DDL.jpg",
                    "/Emblems/Neutral/DDL.png",
                    "/profile_DDL.png",
                    new Color(255, 222, 128, 250),
                    "Neutre DDL",
                    "/Music/IseriaOST/WAV/DDLtheme1.wav"
            ),

            "Free", new Faction(
                    "Free",
                    "Libre",
                    "/bg_faction_free.jpg",
                    "/Emblems/Neutral/free.png",
                    "/profile_free.png",
                    new Color(0, 0, 0, 250),
                    "n/a"
            ),

            "Admin", new Faction(
                    "Admin",
                    "Administrateur",
                    "/RessourceGen/bg_faction_admin.png",
                    "/Emblems/Neutral/admin.png",
                    "/profile_admin.png",
                    Color.GRAY,
                    "n/a"

            ),
            "MT01", new Faction(
                    "MT01",
                    "Murloc Tribe Frelelbleiawh",
                    "/Neutral/MT01.png",
                    "/Emblems/Neutral/MT01.png",
                    "/profile_MT01.png",
                    new Color(78, 255, 219,255),
                    "n/a",
                    "/Music/IseriaOST/WAV/MTtheme1.wav"


            )
    );

    // USER-TO-FACTION MAPPING
    private static final Map<String, String> USER_FACTION_MAP = Map.of(
            "Bladjorn", "Anima",
            "Klowh", "decimus",
            "Admin", "Admin",
            "t", "Free"
    );

    // PUBLIC API
    public static Faction getFactionId(String id) {
        return FACTIONS.getOrDefault(id, FACTIONS.get("Free"));
    }

    public static Collection<Faction> all() {
        return FACTIONS.values();
    }

    public static Set<String> getAllIds() {
        return FACTIONS.keySet();
    }

    public static Faction getFactionForUser(String username) {
        String factionId = USER_FACTION_MAP.getOrDefault(username, "Free");
        return getFactionId(factionId);
    }

    public static boolean exists(String id) {
        return FACTIONS.containsKey(id);
    }

    // Convenience methods for common UI operations
    public static Color getColorFor(String factionId) {
        return getFactionId(factionId).getFactionColor();
    }

    public static String getEmblemPathFor(String factionId) {
        return getFactionId(factionId).getEmblemImage();
    }

    public static String getBackgroundFor(String factionId) {
        return getFactionId(factionId).getBackgroundImage();
    }
}
