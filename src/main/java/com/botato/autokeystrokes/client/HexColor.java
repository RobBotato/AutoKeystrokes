package com.botato.autokeystrokes.client;

public final class HexColor {
    private HexColor() {}

    public static Integer parse(String s) {
        if (s == null) return null;
        String hex = s.startsWith("#") ? s.substring(1) : s;
        if (hex.length() != 6 && hex.length() != 8) return null;
        try {
            long v = Long.parseLong(hex, 16);
            if (hex.length() == 6) v |= 0xFF000000L;
            return (int) v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String format(int argb) {
        if ((argb >>> 24) == 0xFF) return String.format("#%06X", argb & 0xFFFFFF);
        return String.format("#%08X", argb);
    }
}
