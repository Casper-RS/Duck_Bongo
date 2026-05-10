package dev.casperrs.duckbongo.app.skins;

import dev.casperrs.duckbongo.app.utils.ResourceUtils;

public record SkinSet(String duckPath, String waterPath) {

    public static final SkinSet DEFAULT =
            new SkinSet(DuckSkin.DEFAULT.resourcePath(), WaterSkin.DEFAULT.resourcePath());

    public SkinSet {
        duckPath  = ResourceUtils.normDuck(duckPath);
        waterPath = ResourceUtils.normWater(waterPath);
    }

    public SkinSet withDuck(String path)  { return new SkinSet(path, waterPath); }
    public SkinSet withWater(String path) { return new SkinSet(duckPath, path); }

    public DuckSkin  duck()  { return DuckSkin.fromPath(duckPath); }
    public WaterSkin water() { return WaterSkin.fromPath(waterPath); }
}
