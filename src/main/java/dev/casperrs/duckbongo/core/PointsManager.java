package dev.casperrs.duckbongo.core;

import java.util.concurrent.atomic.AtomicLong;

public class PointsManager {
    private final AtomicLong points = new AtomicLong();
    public long add(long n) { return points.addAndGet(n); }
    public long get() { return points.get(); }
    public boolean spend(long n) {
        while (true) {
            long cur = points.get();
            if (cur < n) return false;
            if (points.compareAndSet(cur, cur - n)) return true;
        }
    }
}
