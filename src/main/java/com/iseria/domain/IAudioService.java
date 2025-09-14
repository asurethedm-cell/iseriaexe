package com.iseria.domain;
public interface IAudioService {
    void playClick();
    void playTheme(String themeTag);
    void stop();
    void playMainThemeAudio();
    void fadeIn();
    void fadeOut();
    void playHexMusicMenu();
    void playRandomMainThemeAuto();
    void adjustVolume(int volume);
    void setVolume(float volume);

    // Optionnel : pour un meilleur contrôle
    boolean isPlaying();
    boolean isFading();

}
