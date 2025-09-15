package com.iseria.ui;

import com.iseria.domain.IAudioService;

import com.iseria.domain.IDataProvider;
import com.iseria.domain.IHexRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.util.List;


public class LoadingWindow extends JWindow {
    private static final Logger log = LogManager.getLogger(LoadingWindow.class);
    private JLabel statusLabel;
    private final IHexRepository repo;
    private final IAudioService audio;
    private final IDataProvider data;
    public static boolean readyToClose;
    public static boolean audioOkStart;
    public static Mondes mondesWindow;

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
        ExtendedLoadingWorker loader = new ExtendedLoadingWorker();
        loader.execute();
    }
    public static LoadingWindow splash;
    public static void showSplash(IHexRepository repo, IAudioService audio, IDataProvider data) {
            try {
                BufferedImage img = ImageIO.read(LoadingWindow.class.getResource("/RessourceGen/bg_loading.jpg"));
                splash = new LoadingWindow(img, repo, audio, data);
                splash.setVisible(true);
            } catch (Exception e) {
            e.printStackTrace(); e.getMessage();
            System.out.println();
            System.out.println("fallback: launch directly if image fails");
        }
    }
    class ExtendedLoadingWorker extends SwingWorker<Void, String> {


        @Override
        protected Void doInBackground() throws Exception {
            publish("Chargement des données du repository...");
            repo.loadSafeAll();

            publish("Génération de la carte hexagonale...");
            repo.addAllHexes(50, 100);

            // Pré-créer Mondes avec constructeur minimal
            SwingUtilities.invokeAndWait(() -> {
                mondesWindow = new Mondes(data, audio, repo, false); // false = pas d'init lourde
            });

            publish("Initialisation des caches...");
            mondesWindow.initializeCache();
            mondesWindow.initializePlayerFactionList();
            Thread.sleep(500);

            publish("Préchargement des images...");
            mondesWindow.preloadAllImages();
            audio.fadeOut();
            Thread.sleep(500);

            publish("Chargement des icônes principales...");
            mondesWindow.loadMainIcons();
            Thread.sleep(500);

            publish("Chargement des icônes auxiliaires...");
            mondesWindow.loadAuxIcons();
            Thread.sleep(500);

            publish("Chargement des icônes de fortification...");
            mondesWindow.loadFortIcons();
            Thread.sleep(500);

            publish("Initialisation terminée !");
            audioOkStart = true;
            return null;
        }
        @Override
        protected void process(List<String> chunks) {
            statusLabel.setText(chunks.get(chunks.size() - 1));
        }
        @Override
        protected void done() {
            SwingUtilities.invokeLater(() -> {
                mondesWindow.finalizeUISetup();
                mondesWindow.setVisible(true);
                mondesWindow.toBack();
                new javax.swing.Timer(500, e -> {
                    ((javax.swing.Timer)e.getSource()).stop();
                    readyToClose = true;
                    audioOkStart = false;
                }).start();
            });
        }

    }
}
