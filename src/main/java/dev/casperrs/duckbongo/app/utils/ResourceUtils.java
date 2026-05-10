package dev.casperrs.duckbongo.app.utils;

import javafx.scene.image.Image;
import java.io.File;
import java.util.Objects;

public final class ResourceUtils {
    public static String normDuck(String p) {
        if (p == null || p.isBlank()) return "/assets/skin_parts/ducks/duck_default.png";
        if (p.startsWith("file:") || p.startsWith("/assets/")) return p;
        if (!p.contains("/")) return "/assets/skin_parts/ducks/" + p;
        return p;
    }
    public static String normWater(String p) {
        if (p == null || p.isBlank()) return "/assets/skin_parts/waters/water_default.png";
        if (p.startsWith("file:") || p.startsWith("/assets/")) return p;
        if (!p.contains("/")) return "/assets/skin_parts/waters/" + p;
        return p;
    }
    public static Image loadFlexible(Class<?> anchor, String path) {
        try {
            if (path != null) {
                var res = anchor.getResource(path);
                if (res != null) return new Image(res.toExternalForm(), 0, 0, true, true);
                if (path.startsWith("file:")) return new Image(path, 0, 0, true, true);
                File f = new File(path);
                if (f.exists()) return new Image(f.toURI().toString(), 0, 0, true, true);
            }
        } catch (Exception ignored) {}
        var res = Objects.requireNonNull(anchor.getResource("/assets/skin_parts/ducks/duck_default.png"));
        return new Image(res.toExternalForm(), 0, 0, true, true);
    }
    private ResourceUtils() {}
}
