package com.dowerp.sukuna;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Adaptation {
    public static final float MAX_REDUCTION = 0.75F;
    public static final int LIFETIME_TICKS = 400;

    public record Result(float multiplier, boolean stepped, boolean completed, int stage) {
    }

    private static final class Entry {
        int stage;
        long last = Long.MIN_VALUE / 2;
        long completedAt = -1L;
    }

    private final Map<String, Entry> map = new HashMap<>();

    public Result hit(String key, long now) {
        int steps = SukunaMod.config.adaptationSteps;
        int delay = SukunaMod.config.adaptationDelayTicks;
        Entry e = map.computeIfAbsent(key, k -> new Entry());
        if (e.completedAt >= 0L && now - e.completedAt >= LIFETIME_TICKS) {
            e.stage = 0;
            e.completedAt = -1L;
            e.last = now;
        }
        float progress = Math.min(1.0F, (float) e.stage / steps);
        float multiplier = 1.0F - MAX_REDUCTION * progress;
        boolean stepped = false;
        boolean completed = false;
        if (e.stage < steps && now - e.last >= delay) {
            e.stage++;
            e.last = now;
            stepped = true;
            if (e.stage >= steps) {
                completed = true;
                e.completedAt = now;
            }
        }
        return new Result(multiplier, stepped, completed, e.stage);
    }

    public List<String> expire(long now) {
        List<String> broken = new ArrayList<>();
        for (Map.Entry<String, Entry> en : map.entrySet()) {
            Entry e = en.getValue();
            if (e.completedAt >= 0L && now - e.completedAt >= LIFETIME_TICKS) {
                e.stage = 0;
                e.completedAt = -1L;
                e.last = now;
                broken.add(en.getKey());
            }
        }
        return broken;
    }

    public int total() {
        int sum = 0;
        for (Entry e : map.values()) {
            sum += e.stage;
        }
        return sum;
    }

    public void reset() {
        map.clear();
    }
}
