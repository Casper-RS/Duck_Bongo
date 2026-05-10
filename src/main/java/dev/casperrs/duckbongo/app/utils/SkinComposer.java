package dev.casperrs.duckbongo.app.utils;

import dev.casperrs.duckbongo.app.skins.SkinSet;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SkinComposer {

    private static final int CACHE_LIMIT = 64;
    private static final Map<String, Image> CACHE =
            new LinkedHashMap<>(CACHE_LIMIT, 0.75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, Image> e) {
                    return size() > CACHE_LIMIT;
                }
            };

    public static Image compose(Class<?> anchor, SkinSet skins) {
        String key = skins.duckPath() + "|" + skins.waterPath();
        Image cached = CACHE.get(key);
        if (cached != null) return cached;

        Image duck  = ResourceUtils.loadFlexible(anchor, skins.duckPath());
        Image water = ResourceUtils.loadFlexible(anchor, skins.waterPath());

        double w = Math.max(duck.getWidth(),  water.getWidth());
        double h = Math.max(duck.getHeight(), water.getHeight());
        if (w <= 0 || h <= 0) { w = h = 256; }

        Canvas c = new Canvas(w, h);
        GraphicsContext g = c.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        g.drawImage(water, 0, 0);
        g.drawImage(duck,  0, 0);

        SnapshotParameters p = new SnapshotParameters();
        p.setFill(Color.TRANSPARENT);
        WritableImage out = new WritableImage((int) w, (int) h);
        Image snap = c.snapshot(p, out);
        CACHE.put(key, snap);
        return snap;
    }

    private SkinComposer() {}
}
