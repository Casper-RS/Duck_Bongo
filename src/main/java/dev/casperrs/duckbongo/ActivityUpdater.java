package dev.casperrs.duckbongo;

@FunctionalInterface
public interface ActivityUpdater {
    void update(String details, String state);
}
