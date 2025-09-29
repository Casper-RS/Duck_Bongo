package dev.casperrs.duckbongo.app.utils;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class SkinComposer {
    // Reduce cache size and use weak references to allow garbage collection
    private static final int MAX_CACHE_SIZE = 16;
    private static final Map<String, WeakReference<Image>> CACHE = new LinkedHashMap<String, WeakReference<Image>>(MAX_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, WeakReference<Image>> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    // Reuse canvas and parameters to reduce object creation
    private static final ThreadLocal<Canvas> canvasCache = ThreadLocal.withInitial(() -> new Canvas(256, 256));
    private static final ThreadLocal<SnapshotParameters> snapshotParamsCache = ThreadLocal.withInitial(() -> {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return params;
    });

    public static Image compose(Image duckImg, Image waterImg) {
        if (duckImg == null || waterImg == null) {
            return null;
        }

        String key = Objects.toString(duckImg) + "|" + Objects.toString(waterImg);

        // Check cache with weak reference
        WeakReference<Image> cachedRef = CACHE.get(key);
        if (cachedRef != null) {
            Image cached = cachedRef.get();
            if (cached != null) {
                return cached;
            }
        }

        double w = Math.max(duckImg.getWidth(), waterImg.getWidth());
        double h = Math.max(duckImg.getHeight(), waterImg.getHeight());
        if (w <= 0 || h <= 0) {
            w = h = 256; // safety
        }

        // Get or create canvas from thread-local cache
        Canvas canvas = canvasCache.get();
        canvas.setWidth(w);
        canvas.setHeight(h);

        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        g.drawImage(waterImg, 0, 0, w, h);
        g.drawImage(duckImg, 0, 0, w, h);

        // Create new image and cache it
        WritableImage out = new WritableImage((int) w, (int) h);
        Image snap = canvas.snapshot(snapshotParamsCache.get(), out);

        // Store weak reference
        CACHE.put(key, new WeakReference<>(snap));

        // Clean up any null references in the cache
        CACHE.entrySet().removeIf(entry -> entry.getValue().get() == null);

        return snap;
    }

    /**
     * Clears the image cache to free up memory
     */
    public static void clearCache() {
        CACHE.clear();
    }

    private SkinComposer() {}
}
