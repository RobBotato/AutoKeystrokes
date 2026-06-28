package com.botato.autokeystrokes.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public final class CpsRenderer {
    private static final int LINE_H = 9;
    private static final int SEP_PAD = 3; 
    private static final int SEPARATOR_ARGB = 0xFF000000; 

    private CpsRenderer() {}

    private record Seg(String text, int color, int padLeft, int padRight) {
        Seg(String text, int color) { this(text, color, 0, 0); }
        int totalWidth(TextRenderer tr) { return padLeft + tr.getWidth(text) + padRight; }
    }

    private static TextRenderer tr() {
        return MinecraftClient.getInstance().textRenderer;
    }

    private static List<Seg> segments(HudConfig c) {
        int textCol = c.cpsTextArgb;
        List<Seg> core = new ArrayList<>();
        core.add(new Seg(Integer.toString(ClickTracker.leftCps()), textCol));
        if (c.cpsRightClick) {
            core.add(new Seg("|", SEPARATOR_ARGB, SEP_PAD, SEP_PAD));
            core.add(new Seg(Integer.toString(ClickTracker.rightCps()), textCol));
        }

        List<Seg> inner = new ArrayList<>();
        if (c.cpsShowText) {
            if (c.cpsReverseText) {
                inner.add(new Seg("CPS ", textCol));
                inner.addAll(core);
            } else {
                inner.addAll(core);
                inner.add(new Seg(" CPS", textCol));
            }
        } else {
            inner.addAll(core);
        }

        List<Seg> full = new ArrayList<>();
        if (c.cpsBrackets) full.add(new Seg("[", c.cpsBracketArgb));
        full.addAll(inner);
        if (c.cpsBrackets) full.add(new Seg("]", c.cpsBracketArgb));
        return full;
    }

    public static int width() {
        HudConfig c = HudConfig.get();
        return c.cpsBgWidth;
    }

    public static int height() {
        HudConfig c = HudConfig.get();
        return c.cpsBgHeight;
    }

    public static void render(DrawContext ctx, float x, float y, float scale) {
        HudConfig c = HudConfig.get();
        TextRenderer tr = tr();
        int w = width();
        int h = height();

        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().scale(scale, scale, 1f);

        if (c.cpsBackground) ctx.fill(0, 0, w, h, c.cpsBgArgb);
        if (c.cpsBorder) drawBorder(ctx, 0, 0, w, h, c.cpsBorderThickness, c.cpsBorderArgb);

        List<Seg> segs = segments(c);
        int total = 0;
        for (Seg s : segs) total += s.totalWidth(tr);
        int sx = Math.round((w - total) / 2f);
        int ty = Math.round((h - LINE_H) / 2f);
        for (Seg s : segs) {
            sx += s.padLeft();
            ctx.drawText(tr, s.text(), sx, ty, s.color(), c.cpsTextShadow);
            sx += tr.getWidth(s.text()) + s.padRight();
        }

        ctx.getMatrices().pop();
    }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int t, int col) {
        ctx.fill(x, y, x + w, y + t, col);
        ctx.fill(x, y + h - t, x + w, y + h, col);
        ctx.fill(x, y + t, x + t, y + h - t, col);
        ctx.fill(x + w - t, y + t, x + w, y + h - t, col);
    }
}
