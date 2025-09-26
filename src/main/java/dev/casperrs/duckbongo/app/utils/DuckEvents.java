package dev.casperrs.duckbongo.app.utils;

public interface DuckEvents {
    void onPositionChanged(float x, float y);
    void onDuckSkinChanged(String duckPath);
    void onWaterSkinChanged(String waterPath);
    void onServerIpSubmit(String ip);
}
