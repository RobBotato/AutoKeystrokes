package com.botato.autokeystrokes.client;

public final class ColorUtil {
    private ColorUtil() {}

    public static int hsvToRgb(float h, float s, float v) {
        return hsvaToArgb(h, s, v, 1f);
    }

    public static int hsvaToArgb(float h, float s, float v, float a) {
        h = wrap01(h); s = clamp01(s); v = clamp01(v); a = clamp01(a);
        float r, g, b;
        float hp = h * 6f;
        int i = (int) Math.floor(hp) % 6;
        if (i < 0) i += 6;
        float f = hp - (float) Math.floor(hp);
        float p = v * (1f - s);
        float q = v * (1f - s * f);
        float t = v * (1f - s * (1f - f));
        switch (i) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        int ai = Math.round(a * 255f);
        int ri = Math.round(r * 255f);
        int gi = Math.round(g * 255f);
        int bi = Math.round(b * 255f);
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }

    public static float[] argbToHsva(int argb) {
        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float v = max;
        float s = (max == 0f) ? 0f : d / max;
        float h;
        if (d == 0f) {
            h = 0f;
        } else if (max == r) {
            h = ((g - b) / d) % 6f;
        } else if (max == g) {
            h = (b - r) / d + 2f;
        } else {
            h = (r - g) / d + 4f;
        }
        h /= 6f;
        if (h < 0f) h += 1f;
        return new float[]{h, s, v, a};
    }

    public static int lerpArgb(int from, int to, float t) {
        t = clamp01(t);
        int a = lerpChannel(from >>> 24, to >>> 24, t);
        int r = lerpChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int g = lerpChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = lerpChannel(from & 0xFF, to & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerpChannel(int from, int to, float t) {
        return Math.round(from + (to - from) * t);
    }

    private static float clamp01(float x) {
        return x < 0f ? 0f : (x > 1f ? 1f : x);
    }

    private static float wrap01(float x) {
        x %= 1f;
        return x < 0f ? x + 1f : x;
    }
}
