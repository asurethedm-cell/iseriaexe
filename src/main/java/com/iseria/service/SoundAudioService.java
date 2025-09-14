package com.iseria.service;

import com.iseria.domain.IAudioService;
import com.iseria.infra.SoundTechs;

public class SoundAudioService implements IAudioService {

    @Override
    public void playClick() {
        SoundTechs.playSound();
    }

    @Override
    public void playTheme(String themeTag) {
        SoundTechs.playFactionTheme(themeTag);
    }

    @Override
    public void stop() {
        SoundTechs.stopMusic();
    }

    @Override
    public void playMainThemeAudio() {
        SoundTechs.playMainTheme();
    }

    @Override
    public void fadeIn() {
        SoundTechs.fadeInAudio();
    }

    @Override
    public void fadeOut() {
        SoundTechs.fadeOutAudio();
    }

    @Override
    public  void playRandomMainThemeAuto() { SoundTechs.playRandomMainThemeAuto();}

    // Nouvelles méthodes
    @Override
    public void playHexMusicMenu() {
        SoundTechs.HexMusicMenu();
    }

    @Override
    public void adjustVolume(int volume) {
        SoundTechs.adjustVolume(volume);
    }

    @Override
    public void setVolume(float volume) {
        SoundTechs.setVolume(volume);
    }

    @Override
    public boolean isPlaying() {
        return SoundTechs.clip != null && SoundTechs.clip.isRunning();
    }

    @Override
    public boolean isFading() {
        return SoundTechs.isFading;
    }
    public static int currentThemeIndex() {
        return SoundTechs.currentThemeIndex;
    }
}
