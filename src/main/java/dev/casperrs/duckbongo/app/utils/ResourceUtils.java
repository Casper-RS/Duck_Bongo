package dev.casperrs.duckbongo.app.utils;

import dev.casperrs.duckbongo.app.skins.DuckSkin;
import dev.casperrs.duckbongo.app.skins.WaterSkin;

public final class ResourceUtils {

    public static String normDuck(String path) {
        if (path == null || path.isBlank()) return DuckSkin.DEFAULT.resourcePath();
        return DuckSkin.fromPath(path).resourcePath();
    }

    public static String normWater(String path) {
        if (path == null || path.isBlank()) return WaterSkin.DEFAULT.resourcePath();
        return WaterSkin.fromPath(path).resourcePath();
    }

    private ResourceUtils() {}
}
