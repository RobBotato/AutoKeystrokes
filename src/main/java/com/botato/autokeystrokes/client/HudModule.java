package com.botato.autokeystrokes.client;

public class HudModule {
    public float x, y, scale;
    public boolean visible;

    public HudModule(float x, float y, float scale, boolean visible) {
        this.x = x; this.y = y; this.scale = scale; this.visible = visible;
    }

    public boolean isHovered(double mx, double my, int w, int h) {
        return mx >= x && mx <= x + (w * scale) && my >= y && my <= y + (h * scale);
    }

    public void clamp(int screenW, int screenH, int modW, int modH) {
        this.x = Math.max(0, Math.min(x, screenW - (modW * scale)));
        this.y = Math.max(0, Math.min(y, screenH - (modH * scale)));
    }
}
