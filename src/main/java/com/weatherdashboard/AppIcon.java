package com.weatherdashboard;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class AppIcon {
    private AppIcon() {}

    public static Image sun(int size) {
        BufferedImage buffered = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = buffered.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setComposite(java.awt.AlphaComposite.Clear);
        g.fillRect(0, 0, size, size);
        g.setComposite(java.awt.AlphaComposite.SrcOver);

        double center = size / 2.0;
        double coreRadius = size * 0.22;
        double rayInner = coreRadius * 1.15;
        double rayOuter = size * 0.46;
        float rayWidth = Math.max(1.5f, size / 14f);

        g.setColor(new Color(245, 158, 11));
        g.setStroke(new BasicStroke(rayWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45.0);
            int x1 = (int) (center + rayInner * Math.cos(angle));
            int y1 = (int) (center + rayInner * Math.sin(angle));
            int x2 = (int) (center + rayOuter * Math.cos(angle));
            int y2 = (int) (center + rayOuter * Math.sin(angle));
            g.drawLine(x1, y1, x2, y2);
        }

        int diameter = (int) (coreRadius * 2);
        int origin = (int) (center - coreRadius);
        g.setColor(new Color(251, 191, 36));
        g.fillOval(origin, origin, diameter, diameter);
        g.setColor(new Color(245, 158, 11));
        g.drawOval(origin, origin, diameter, diameter);

        g.dispose();
        return toFxImage(buffered);
    }

    private static Image toFxImage(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.setArgb(x, y, source.getRGB(x, y));
            }
        }
        return image;
    }
}
