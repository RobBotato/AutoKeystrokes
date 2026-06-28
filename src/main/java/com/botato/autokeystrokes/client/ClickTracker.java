package com.botato.autokeystrokes.client;

import java.util.ArrayList;
import java.util.List;

public final class ClickTracker {
    private static final List<Long> LEFT = new ArrayList<>();
    private static final List<Long> RIGHT = new ArrayList<>();

    private ClickTracker() {}

    public static void addLeft(long timeMs) { LEFT.add(timeMs); }
    public static void addRight(long timeMs) { RIGHT.add(timeMs); }

    public static int leftCps() { prune(LEFT); return LEFT.size(); }
    public static int rightCps() { prune(RIGHT); return RIGHT.size(); }

    private static void prune(List<Long> list) {
        long now = System.currentTimeMillis();
        list.removeIf(t -> now - t > 1000);
    }
}
