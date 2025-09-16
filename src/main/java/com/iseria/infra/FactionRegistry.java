package com.iseria.infra;

import com.iseria.domain.Faction;
import java.awt.Color;
import java.util.*;

public class FactionRegistry {


    private static final Map<String, Faction> FACTIONS = Map.of(
            "Anima", new Faction(
                    "Anima",
                    "Compagnons d'Anima",
                    true,
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
                    "/Music/IseriaOST/WAV/animatheme6.wav"),

            "decimus", new Faction(
                    "decimus",
                    "Poing de Décimus",
                    true,
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
                    "/Music/IseriaOST/WAV/decimustheme6.wav"),

            "kalminhouse", new Faction(
                    "kalmin",
                    "Maison Kalmin",
                    true,
                    "/RessourceGen/bg_faction_kalmin.jpg",
                    "/Emblems/Joueurs/kalmin_emblem.png",
                    "/profile_kalmin.png",
                    new Color(40, 89, 16, 255),
                    "Marchande",
                    "/Music/IseriaOST/WAV/kalmintheme1.wav",
                    "/Music/IseriaOST/WAV/kalmintheme2.wav",
                    "/Music/IseriaOST/WAV/kalmintheme3.wav",
                    "/Music/IseriaOST/WAV/kalmintheme4.wav",
                    "/Music/IseriaOST/WAV/kalmintheme5.wav",
                    "/Music/IseriaOST/WAV/kalmintheme6.wav"),

            "arsmagicka", new Faction(
                    "arsmagicka",
                    "Ars Magicka",
                    true,
                    "/RessourceGen/bg_faction_arsmagicka.jpg",
                    "/Emblems/Joueurs/arsmagicka_emblem.png",
                    "/profile_arsmagicka.png",
                    new Color(214, 198, 72, 255),
                    "Sage",
                    "/Music/IseriaOST/WAV/arsmagickatheme1.wav",
                    "/Music/IseriaOST/WAV/arsmagickatheme2.wav",
                    "/Music/IseriaOST/WAV/arsmagickatheme3.wav",
                    "/Music/IseriaOST/WAV/arsmagickatheme4.wav",
                    "/Music/IseriaOST/WAV/arsmagickatheme5.wav",
                    "/Music/IseriaOST/WAV/arsmagickatheme6.wav"),

            "cdi", new Faction(
                    "cdi",
                    "Caste des Intouchables",
                    true,
                    "/RessourceGen/bg_faction_cdi.jpg",
                    "/Emblems/Joueurs/cdi.png",
                    "/profile_cdi.png",
                    new Color(161, 76, 26, 255),
                    "Rebuts",
                    "/Music/IseriaOST/WAV/cditheme1.wav",
                    "/Music/IseriaOST/WAV/cditheme2.wav",
                    "/Music/IseriaOST/WAV/cditheme3.wav",
                    "/Music/IseriaOST/WAV/cditheme4.wav",
                    "/Music/IseriaOST/WAV/cditheme5.wav",
                    "/Music/IseriaOST/WAV/cditheme6.wav"),

            "origine", new Faction(
                    "origine",
                    "Le Cercle D'Origines",
                    true,
                    "/RessourceGen/bg_faction_origine.jpg",
                    "/Emblems/Joueurs/origine.png",
                    "/profile_origine.png",
                    new Color(109, 96, 223, 255),
                    "Rebuts",
                    "/Music/IseriaOST/WAV/originetheme1.wav",
                    "/Music/IseriaOST/WAV/originetheme2.wav",
                    "/Music/IseriaOST/WAV/originetheme3.wav",
                    "/Music/IseriaOST/WAV/originetheme4.wav",
                    "/Music/IseriaOST/WAV/originetheme5.wav",
                    "/Music/IseriaOST/WAV/originetheme6.wav"),

            "DDL", new Faction(
                    "DDL",
                    "Donjon Du Loot",
                    false,
                    "/bg_DDL.jpg",
                    "/Emblems/Neutral/DDL.png",
                    "/profile_DDL.png",
                    new Color(255, 222, 128, 250),
                    "Neutre DDL",
                    "/Music/IseriaOST/WAV/DDLtheme1.wav"),

            "Free", new Faction(
                    "Free",
                    "Libre",
                    false,
                    "/bg_faction_free.jpg",
                    "/Emblems/Neutral/free.png",
                    "/profile_free.png",
                    new Color(0, 0, 0, 250),
                    "n/a"),

            "Admin", new Faction(
                    "Admin",
                    "Le Monde",
                    false,
                    "/RessourceGen/bg_faction_admin.png",
                    "/Emblems/Neutral/admin.png",
                    "/profile_admin.png",
                    Color.GRAY,

                    "n/a"),
            "MT01", new Faction(
                    "MT01",
                    "Murloc Tribe Frelelbleiawh",
                    false,
                    "/Neutral/MT01.png",
                    "/Emblems/Neutral/MT01.png",
                    "/profile_MT01.png",
                    new Color(78, 255, 219,255),
                    "n/a",
                    "/Music/IseriaOST/WAV/MTtheme1.wav")
    );

    // USER-TO-FACTION MAPPING
    private static final Map<String, String> USER_FACTION_MAP = Map.of(
            "Bladjorn", "Anima",
            "Klowh", "decimus",
            "Atrociter", "arsmagicka",
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
    public static Boolean getIsPlayer() {return getIsPlayer();}
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
