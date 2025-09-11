package com.iseria.infra;

import javax.sound.sampled.*;
import javax.swing.*;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.Timer;

public class SoundTechs {

    public static Clip clip;

    public static FloatControl volumeControl;
    public static List<String> themes;
    public static int currentThemeIndex;
    public static boolean isFading = false;

    public static Map<String, List<String>> themesByFaction = new HashMap<>();
    public static Map<String, Integer> currentThemeIndexByFaction = new HashMap<>();


    public static void playSound() {
        URL soundURL = SoundTechs.class.getResource("/Sounds/button_click.wav");
        if (soundURL != null) {
            try {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundURL);
                Clip soundClip = AudioSystem.getClip();
                soundClip.open(audioStream);
                soundClip.start();
            } catch (Exception e) {
                System.out.println("Erreur lors de la lecture du click: " + e.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(null, "Le fichier audio 'button_click.wav' n'a pas été trouvé.", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void playMainTheme() {
        URL soundURL = SoundTechs.class.getResource("/Music/IseriaOST/WAV/Main2.wav");
        if (soundURL != null) {
            try (AudioInputStream MainMenuTheme = AudioSystem.getAudioInputStream(soundURL)) {
                clip = AudioSystem.getClip();
                clip.open(MainMenuTheme);
                volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                volumeControl.setValue(-30.0f); // Volume initial
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
                System.out.println("Erreur lors de la lecture de l'audio: " + e.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(null, "Le fichier audio n'a pas été trouvé.", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void HexMusicMenu() {
        themes = new ArrayList<>();
        themes.add("/Music/IseriaOST/WAV/theme.wav");
        for (int i = 2; i <= 10; i++) {
            themes.add("/Music/IseriaOST/WAV/theme" + i + ".wav");
        }

        Random rand = new Random();
        int nextThemeIndex;
        do {
            nextThemeIndex = rand.nextInt(themes.size());
        } while (nextThemeIndex == currentThemeIndex);
        playInitialTheme();
        startThemeChangeTimer();
    }

    public static void startThemeChangeTimer() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                playNextTheme();
            }
        }, 300000, 300000); // Play next theme every 5 minutes
    }

    public static void playNextTheme() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
            clip.close();
        }

        Random rand = new Random();
        int nextThemeIndex;
        do {
            nextThemeIndex = rand.nextInt(themes.size());
        } while (nextThemeIndex == currentThemeIndex);

        currentThemeIndex = nextThemeIndex;
        playHexAudio(themes.get(nextThemeIndex));
    }

    public static void stopMusic(){
        if (clip != null) {
        clip.stop();
        clip.flush();
        clip.close();
        clip = null;
    }
}

    public static void playHexAudio(String themeFilePath) {
        URL soundURL = SoundTechs.class.getResource(themeFilePath);
        if (soundURL != null) {
            try {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundURL);
                clip = AudioSystem.getClip();
                clip.open(audioStream);
                volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                setVolume(0.5f);
                clip.start();
            } catch (Exception e) {
                System.out.println("Error playing audio: " + e.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(null, "Audio file not found.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void playInitialTheme() {
        Random rand = new Random();
        currentThemeIndex = rand.nextInt(themes.size());
        playHexAudio(themes.get(currentThemeIndex));
    }

    public static void setVolume(float volume) {
        if (volumeControl != null) {
            float min = volumeControl.getMinimum();
            float max = volumeControl.getMaximum();
            float newValue = min + (max - min) * volume;
            volumeControl.setValue(newValue);

        }
    }

    public static void playFactionTheme(String factionTag) {
        themesByFaction.computeIfAbsent(factionTag, tag -> {
            List<String> themes = new ArrayList<>();
            for (int i = 1; i <= 6; i++) {
                themes.add("/Music/IseriaOST/WAV/" + tag + "theme" + i + ".wav");
            }
            return themes;
        });

        List<String> themes = themesByFaction.get(factionTag);
        int currentIndex = currentThemeIndexByFaction.getOrDefault(factionTag, -1);

        // Pick a new index different from the current one
        Random rand = new Random();
        int nextIndex;
        do {
            nextIndex = rand.nextInt(themes.size());
        } while (nextIndex == currentIndex);

        currentThemeIndexByFaction.put(factionTag, nextIndex);
        playFactionAudio(themes.get(nextIndex));
    }

    public static void playFactionAudio(String themeFilePath) {
        URL soundURL = SoundTechs.class.getResource(themeFilePath);
        if (soundURL != null) {
            try {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundURL);
                clip = AudioSystem.getClip();
                clip.open(audioStream);
                volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                setVolume(0.5f);
                clip.start();
            } catch (Exception e) {
                System.out.println("Error playing audio: " + e.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(null, "Audio file not found: " + themeFilePath, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void fadeOutAudio() {
        isFading = true;
        new Thread(() -> {
            try {
                // Gradually drop volume to minimum
                for (float vol = volumeControl.getValue(); vol > volumeControl.getMinimum(); vol -= 0.15f) {
                    volumeControl.setValue(vol);
                    Thread.sleep(5);
                }
                clip.stop();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                isFading = false;
            }
        }).start();
    }

    public static void fadeInAudio() {
        isFading = true;
        new Thread(() -> {
            if (clip == null) {
                playMainTheme();
            }
            try {
                clip.start();
                clip.loop(Clip.LOOP_CONTINUOUSLY);
                //TODO replace maxVolume by UserMaxVolume when extending UserOptions
                float maxVolume = -30f;

                for (float volume = -80.0f; volume < maxVolume; volume += 0.15f) {
                    volumeControl.setValue(volume);
                    Thread.sleep(5);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();

                }
                isFading = false;
            }).start();
    }

    public static void adjustVolume(int volume) {
        if (volumeControl != null) {
            float dbValue = (float) volume;
            volumeControl.setValue(dbValue);
        }
    }


}


