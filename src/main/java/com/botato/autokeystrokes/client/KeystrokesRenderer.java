package com.botato.autokeystrokes.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class KeystrokesRenderer {
    public static final int GAP = 1;

    private KeystrokesRenderer() {}

    public record KeyStates(float w, float a, float s, float d,
                            float lmb, float rmb, float space,
                            int leftCps, int rightCps) {}

    public static int width() {
        return HudConfig.get().boxSize * 3 + GAP * 2;
    }

    public static int height() {
        HudConfig c = HudConfig.get();
        int total = 0, visible = 0;
        for (String section : c.sectionOrder) {
            int h = sectionHeight(c, section);
            if (h < 0) continue;
            if (visible > 0) total += GAP;
            total += h;
            visible++;
        }
        return Math.max(total, 10);
    }

    private static int sectionHeight(HudConfig c, String section) {
        return switch (section) {
            case "movement" -> c.showMovementKeys ? c.boxSize * 2 + GAP : -1;
            case "clicks" -> c.showClicks ? mouseRowHeight() : -1;
            case "space" -> c.showSpaceBar ? spaceHeight() : -1;
            default -> -1;
        };
    }

    private static int mouseRowHeight() {
        HudConfig c = HudConfig.get();
        return c.boxSize + ((c.showLmbCps || c.showRmbCps) ? 7 : 0);
    }

    private static int spaceHeight() {
        return Math.round(HudConfig.get().boxSize * 0.5f);
    }

    public static void render(DrawContext ctx, float x, float y, float scale, KeyStates k) {
        HudConfig c = HudConfig.get();
        int w = width();

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);
        ctx.getMatrices().scale(scale, scale);

        int yCur = 0;
        boolean first = true;
        for (String section : c.sectionOrder) {
            int h = sectionHeight(c, section);
            if (h < 0) continue;
            if (!first) yCur += GAP;
            first = false;
            switch (section) {
                case "movement" -> drawMovement(ctx, c, yCur, k);
                case "clicks" -> drawClicks(ctx, c, w, yCur, k);
                case "space" -> drawSpace(ctx, c, w, yCur, k);
            }
            yCur += h;
        }

        ctx.getMatrices().popMatrix();
    }

    private static void drawMovement(DrawContext ctx, HudConfig c, int y, KeyStates k) {
        int b = c.boxSize;
        String up = c.arrowsMode ? "↑" : "W";
        String left = c.arrowsMode ? "←" : "A";
        String down = c.arrowsMode ? "↓" : "S";
        String right = c.arrowsMode ? "→" : "D";
        drawKey(ctx, up, null, b + GAP, y, b, b, k.w());
        int y2 = y + b + GAP;
        drawKey(ctx, left, null, 0, y2, b, b, k.a());
        drawKey(ctx, down, null, b + GAP, y2, b, b, k.s());
        drawKey(ctx, right, null, (b + GAP) * 2, y2, b, b, k.d());
    }

    private static void drawClicks(DrawContext ctx, HudConfig c, int w, int y, KeyStates k) {
        int mouseW = (w - GAP) / 2;
        int mouseW2 = w - GAP - mouseW; 
        drawKey(ctx, "LMB", c.showLmbCps ? k.leftCps() + " CPS" : null, 0, y, mouseW, mouseRowHeight(), k.lmb());
        drawKey(ctx, "RMB", c.showRmbCps ? k.rightCps() + " CPS" : null, mouseW + GAP, y, mouseW2, mouseRowHeight(), k.rmb());
    }

    private static void drawSpace(DrawContext ctx, HudConfig c, int w, int y, KeyStates k) {
        int sh = spaceHeight();
        int bg = ColorUtil.lerpArgb(c.bgArgb, c.bgPressedArgb, k.space());
        int line = ColorUtil.lerpArgb(c.textArgb, c.textPressedArgb, k.space());
        ctx.fill(0, y, w, y + sh, bg);
        if (c.border) drawBorder(ctx, 0, y, w, sh);
        int lineX0 = c.boxSize + GAP;     
        int lineX1 = lineX0 + c.boxSize;  
        int lineY = y + sh / 2 - 1;
        ctx.fill(lineX0, lineY, lineX1, lineY + 1, line);
    }

    private static void drawKey(DrawContext ctx, String label, String subLabel, int x, int y, int w, int h, float fade) {
        HudConfig c = HudConfig.get();
        int bg = ColorUtil.lerpArgb(c.bgArgb, c.bgPressedArgb, fade);
        int txt = ColorUtil.lerpArgb(c.textArgb, c.textPressedArgb, fade);
        ctx.fill(x, y, x + w, y + h, bg);
        if (c.border) drawBorder(ctx, x, y, w, h);

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        if (subLabel == null) {
            int lx = Math.round(x + (w - tr.getWidth(label)) / 2f);
            ctx.drawText(tr, label, lx, y + (h - 9) / 2 + 1, txt, c.textShadow);
        } else {
            float s2 = 0.7f;
            float gap = 1f;
            float groupH = 9 + gap + 9 * s2;
            float top = y + (h - groupH) / 2f;
            int lx = Math.round(x + (w - tr.getWidth(label)) / 2f);
            ctx.drawText(tr, label, lx, Math.round(top), txt, c.textShadow);
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(x + (w - tr.getWidth(subLabel) * s2) / 2f, top + 9 + gap);
            ctx.getMatrices().scale(s2, s2);
            ctx.drawText(tr, subLabel, 0, 0, txt, c.textShadow);
            ctx.getMatrices().popMatrix();
        }
    }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h) {
        HudConfig c = HudConfig.get();
        int t = c.borderThickness;
        int col = c.borderArgb;
        ctx.fill(x, y, x + w, y + t, col);
        ctx.fill(x, y + h - t, x + w, y + h, col);
        ctx.fill(x, y + t, x + t, y + h - t, col);
        ctx.fill(x + w - t, y + t, x + w, y + h - t, col);
    }
}
