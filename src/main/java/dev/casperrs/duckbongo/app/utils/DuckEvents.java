package dev.casperrs.duckbongo.app.utils;

public interface DuckEvents {
    void onPositionChanged(float x, float y);
    // Called when the user releases the duck (final pose). This should be sent immediately (unthrottled).
    void onPositionSettled(float x, float y);
    // Dragging a remote duck
    void onOtherMoved(int targetId, float x, float y);
    void onOtherSettled(int targetId, float x, float y);
    void onDuckSkinChanged(String duckPath);
    void onWaterSkinChanged(String waterPath);
    void onServerIpSubmit(String ip);
}
