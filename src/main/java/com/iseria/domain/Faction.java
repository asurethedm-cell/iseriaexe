package com.iseria.domain;

import java.awt.Color;

public class Faction {
    private final String id; // e.g. "anima"
    private final String displayName; // e.g. "Anima"
    private final String backgroundImage; // resource path
    private final String emblemImage; // resource path
    private final String profileImage; // resource path (for diplomacy)
    private final Color factionColor; // UI color
    private final String[] musicThemes; // array of music file paths
    private final String factionType;

    public Faction(String id, String displayName, String backgroundImage,
                   String emblemImage, String profileImage, Color factionColor, String factionType,
                   String... musicThemes) {
        this.id = id;
        this.displayName = displayName;
        this.backgroundImage = backgroundImage;
        this.emblemImage = emblemImage;
        this.profileImage = profileImage;
        this.factionColor = factionColor;
        this.factionType = factionType;
        this.musicThemes = musicThemes;
    }

    // ALL GETTERS
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getBackgroundImage() { return backgroundImage; }
    public String getEmblemImage() { return emblemImage; }
    public String getProfileImage() { return profileImage; }
    public Color getFactionColor() { return factionColor; }
    public String getFactionType(){ return factionType; }
    public String[] getMusicThemes() { return musicThemes; }

    // Convenience methods
    public boolean hasMusic() { return musicThemes.length > 0; }

    @Override
    public String toString() {
        return displayName;
    }
}
