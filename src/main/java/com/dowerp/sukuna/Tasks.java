package com.dowerp.sukuna;

import java.util.ArrayList;
import java.util.List;

public final class Tasks {
    private static final List<Task> ACTIVE = new ArrayList<>();
    private static final List<Task> PENDING = new ArrayList<>();

    private Tasks() {
    }

    public static void add(Task task) {
        PENDING.add(task);
    }

    public static void tick() {
        if (!PENDING.isEmpty()) {
            ACTIVE.addAll(PENDING);
            PENDING.clear();
        }
        ACTIVE.removeIf(task -> {
            try {
                return task.tick();
            } catch (Exception e) {
                SukunaMod.LOGGER.error("Task failed", e);
                return true;
            }
        });
    }

    public static void clear() {
        ACTIVE.clear();
        PENDING.clear();
    }
}
