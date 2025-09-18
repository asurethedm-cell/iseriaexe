package com.iseria.ui;

import com.iseria.domain.IHexRepository;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class HexSnapshotCache {
    private static final Map<String, BufferedImage> hexImageCache = new ConcurrentHashMap<>();
    public static BufferedImage mapBackground; // Image de fond de la carte
    static {
        try {
            mapBackground = ImageIO.read(Objects.requireNonNull(HexSnapshotCache.class.getResource("/Iseria.png")));
        } catch (IOException e) {
            System.err.println("Erreur chargement carte: " + e.getMessage());
        }
    }
    public static BufferedImage getHexSnapshot(String hexKey, IHexRepository repo) {
        return hexImageCache.computeIfAbsent(hexKey, key -> generateHexSnapshot(key, repo));
    }

    private static BufferedImage generateHexSnapshot(String hexKey, IHexRepository repo) {
        if (mapBackground == null) return null;
        Rectangle bounds = getHexPixelBounds(hexKey, repo);
        BufferedImage square = extractSquareRegion(bounds);
        return createHexagonalSnapshot(square);
    }

    private static Rectangle getHexPixelBounds(String hexKey, IHexRepository repo){
        int[] pos = repo.getHexPosition(hexKey);
        int size = 100;
        return new Rectangle(
                pos[0] - size/2,
                pos[1] - size/2,
                size,
                size
        );
    }

    private static BufferedImage extractSquareRegion(Rectangle bounds) {
        int x = Math.max(0, Math.min(bounds.x, mapBackground.getWidth() - bounds.width));
        int y = Math.max(0, Math.min(bounds.y, mapBackground.getHeight() - bounds.height));
        int w = Math.min(bounds.width, mapBackground.getWidth() - x);
        int h = Math.min(bounds.height, mapBackground.getHeight() - y);

        return mapBackground.getSubimage(x, y, w, h);
    }

    private static BufferedImage createHexagonalSnapshot(BufferedImage square) {
        int size = square.getWidth();
        BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = result.createGraphics();
        Path2D hexPath = createHexagonPath(size);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setClip(hexPath);
        g2d.drawImage(square, 0, 0, null);

        g2d.dispose();
        return result;
    }

    private static Path2D createHexagonPath(int size) {
        Path2D hexagon = new Path2D.Double();
        double radius = size / 2.0;
        double centerX = size / 2.0;
        double centerY = size / 2.0;

        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60 * i);
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);

            if (i == 0) {
                hexagon.moveTo(x, y);
            } else {
                hexagon.lineTo(x, y);
            }
        }
        hexagon.closePath();
        return hexagon;
    }
}