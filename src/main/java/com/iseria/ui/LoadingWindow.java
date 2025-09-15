package com.iseria.ui;

import com.iseria.domain.IAudioService;

import com.iseria.domain.IDataProvider;
import com.iseria.domain.IHexRepository;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.List;


public class LoadingWindow extends JWindow {
    private JLabel statusLabel;
    private final IHexRepository repo;
    private final IAudioService audio;
    private final IDataProvider data;

    public LoadingWindow(BufferedImage fullImage, IHexRepository repo, IAudioService audio, IDataProvider data) {
        this.data = data;
        this.repo = repo;
        this.audio = audio;


        setSize(800, 500);
        setLocationRelativeTo(null); // center on screen


        Image scaled = fullImage.getScaledInstance(800, 500, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(800, 500, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(scaled, 0, 0, null);
        g2d.dispose();

        // Background label
        JLabel bgLabel = new JLabel(new ImageIcon(resized));
        bgLabel.setLayout(new BorderLayout());
        setContentPane(bgLabel);

        // Status label in bottom-right
        statusLabel = new JLabel("Chargement...", SwingConstants.RIGHT);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        statusPanel.setOpaque(false);
        statusPanel.add(statusLabel);

        bgLabel.add(statusPanel, BorderLayout.SOUTH);

        // Start the background worker
        runLoader();
    }

    private void runLoader() {
        SwingWorker<Void, String> loader = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                publish("Chargement des hexagones...");
                repo.loadSafeAll(); // your actual loading logic
                publish("Initialisation terminée");
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                statusLabel.setText(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }

                SwingUtilities.invokeLater( () -> {
                    Mondes mondesWindow = new Mondes(data, audio, repo);
                    dispose(); // close splash
                    mondesWindow.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            audio.stop();
                        }
                        @Override
                        public void windowClosed(WindowEvent e) {
                            // Restore main menu volume fade-in
                            /*MainMenu main =  obtain reference or call fadeInAudio() statically ;
                            main.*/
                            audio.fadeIn();
                        }
                    });
                });
            }

        };
        loader.execute();
    }


    public static void showSplash(IHexRepository repo, IAudioService audio, IDataProvider data) {
            try {
                BufferedImage img = ImageIO.read(LoadingWindow.class.getResource("/RessourceGen/bg_loading.jpg"));
                LoadingWindow splash = new LoadingWindow(img, repo, audio, data);
                splash.setVisible(true);
            } catch (Exception e) {
            e.printStackTrace(); e.getMessage();
            System.out.println();
            System.out.println("fallback: launch directly if image fails");
            SwingUtilities.invokeLater(() -> new Mondes(data, audio, repo));
        }
    }
}
