package dev.casperrs.duckbongo.app.utils;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class SkinComposer {
    private static final int MAX = 64;
    private static final Map<String, Image> CACHE = new LinkedHashMap<>(MAX, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Image> e) { return size() > MAX; }
    };

    public static Image compose(Image duckImg, Image waterImg) {
        String key = Objects.toString(duckImg) + "|" + Objects.toString(waterImg);
        Image cached = CACHE.get(key);
        if (cached != null) return cached;

        double w = Math.max(duckImg.getWidth(),  waterImg.getWidth());
        double h = Math.max(duckImg.getHeight(), waterImg.getHeight());
        if (w <= 0 || h <= 0) { w = h = 256; } // safety

        Canvas c = new Canvas(w, h);
        GraphicsContext g = c.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        g.drawImage(waterImg, 0, 0);
        g.drawImage(duckImg, 0, 0);

        SnapshotParameters p = new SnapshotParameters();
        p.setFill(Color.TRANSPARENT);
        WritableImage out = new WritableImage((int) w, (int) h);
        Image snap = c.snapshot(p, out);
        CACHE.put(key, snap);
        return snap;
    }
    private SkinComposer() {}
}
