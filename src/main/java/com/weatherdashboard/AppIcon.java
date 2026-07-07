package com.weatherdashboard;

import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public final class AppIcon {
    private static final String SUN = "\u2600";

    private AppIcon() {}

    public static Image sun(int size) {
        Canvas canvas = new Canvas(size, size);
        var gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, size, size);
        gc.setFill(Color.web("#f59e0b"));
        gc.setFont(Font.font("Segoe UI Emoji", size * 0.72));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);
        gc.fillText(SUN, size / 2.0, size / 2.0);
        WritableImage image = new WritableImage(size, size);
        canvas.snapshot(null, image);
        return image;
    }
}
