package com.iseria.ui;

import com.iseria.domain.*;
import com.iseria.infra.*;
import com.iseria.service.SoundAudioService;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            IDataProvider dataProvider = new ExcelDataProvider();
            IAudioService audioService = new SoundAudioService();
            IHexRepository hexRepository = new FileHexRepository();

            var hexes = hexRepository.loadAll();
            System.out.println("Loaded " + hexes.size() + " hexes");
            new Login(dataProvider, audioService, hexRepository).setVisible(true);
        });
    }
}
