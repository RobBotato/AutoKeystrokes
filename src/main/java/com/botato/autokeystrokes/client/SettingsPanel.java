package com.botato.autokeystrokes.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class SettingsPanel {
    public static final int PANEL_W = 230;
    private static final int PAD = 8;
    private static final int HEADER_H = 16;
    private static final int ROW_H = 14;
    private static final int SLIDER_H = 14;
    private static final int SLIDER_VALUE_W = 26;
    private static final int RESET_RESERVED = 28;

    private static final int PICKER_H = 104;
    private static final int SV_W = 120, SV_H = 80;
    private static final int STRIP_W = 12, STRIP_GAP = 6;
    private static final String[] PRESETS = {
            "#FFFFFF", "#AAAAAA", "#555555", "#000000",
            "#FF5555", "#FFAA00", "#FFFF55", "#55FF55",
            "#55FFFF", "#5555FF", "#AA00FF", "#FF55FF",
            "#6F000000", "#6FFFFFFF"
    };

    public static boolean open = false;
    public String editingField = null;
    public String editBuffer = "";

    private static int scroll = 0;
    private String draggingSlider = null;
    private int dsTrackX, dsTrackW;
    private boolean draggingScrollbar = false;
    private int scrollbarGrab;
    private String tooltip = null;

    private float pickH, pickS, pickV, pickA = 1f;
    private boolean editingHex = false;
    private enum PickDrag { NONE, SV, HUE, ALPHA }
    private PickDrag pickDrag = PickDrag.NONE;
    private int pkSvX, pkSvY, pkHueX, pkAlphaX, pkPresetX, pkHexX, pkHexY;

    private int x0, y0, x1, y1, viewBottom, screenW;

    public void layout(int sw, int sh) {
        screenW = sw;
        x0 = (sw - PANEL_W) / 2;
        x1 = x0 + PANEL_W;
        y0 = 24;
        y1 = sh - 46;
        viewBottom = y1 - RESET_RESERVED;
    }

    public int resetButtonX() { return x0 + PAD; }
    public int resetButtonY() { return y1 - 24; }
    public int resetButtonW() { return PANEL_W - PAD * 2; }

    private enum Kind { HEADER, TOGGLE, SLIDER, COLOR }
    private record Entry(String id, Kind kind, String label, String tooltip) {
        Entry(String id, Kind kind, String label) { this(id, kind, label, null); }
    }

    private List<Entry> entries() {
        HudConfig c = HudConfig.get();
        List<Entry> e = new ArrayList<>();
        e.add(new Entry("backgroundBlur", Kind.SLIDER, "Background Blur", "How much the game view blurs behind this editor"));
        e.add(new Entry(null, Kind.HEADER, "Gameplay"));
        e.add(new Entry("leftClicking", Kind.TOGGLE, "Left Clicking", "Also auto-click LMB while FastPlace is on"));
        e.add(new Entry("airClicking", Kind.TOGGLE, "Air Clicking", "If off, clicks need a block or entity target"));
        e.add(new Entry("targetCps", Kind.SLIDER, "CPS Rate", "Target clicks per second for FastPlace"));
        e.add(new Entry(null, Kind.HEADER, "Display"));
        e.add(new Entry("keysVisible", Kind.TOGGLE, "Show Keystrokes", "Show/hide the entire keystrokes overlay (same as the X/O toggle)"));
        e.add(new Entry("cpsVisible", Kind.TOGGLE, "Show CPS Counter", "Show/hide the entire CPS counter (same as the X/O toggle)"));
        e.add(new Entry("showClicks", Kind.TOGGLE, "Show Clicks"));
        if (c.showClicks) {
            e.add(new Entry("showLmbCps", Kind.TOGGLE, "Show LMB CPS"));
            e.add(new Entry("showRmbCps", Kind.TOGGLE, "Show RMB CPS"));
        }
        e.add(new Entry("showSpaceBar", Kind.TOGGLE, "Show Space Bar"));
        e.add(new Entry("showMovementKeys", Kind.TOGGLE, "Show Move Keys"));
        e.add(new Entry("arrowsMode", Kind.TOGGLE, "Arrows Mode"));
        e.add(new Entry(null, Kind.HEADER, "Style"));
        e.add(new Entry("scale", Kind.SLIDER, "Scale"));
        e.add(new Entry("boxSize", Kind.SLIDER, "Box Size"));
        e.add(new Entry("border", Kind.TOGGLE, "Border"));
        if (c.border) e.add(new Entry("borderThickness", Kind.SLIDER, "Thickness"));
        e.add(new Entry("textShadow", Kind.TOGGLE, "Text Shadow"));
        e.add(new Entry("keyFadeDelay", Kind.SLIDER, "Key Fade Delay", "How quickly key presses fade away after release (ms)"));
        e.add(new Entry(null, Kind.HEADER, "Colors"));
        e.add(new Entry("textColor", Kind.COLOR, "Text"));
        e.add(new Entry("textColorPressed", Kind.COLOR, "Text (Pressed)"));
        e.add(new Entry("bgColor", Kind.COLOR, "Background"));
        e.add(new Entry("bgColorPressed", Kind.COLOR, "BG (Pressed)"));
        e.add(new Entry("borderColor", Kind.COLOR, "Border"));
        e.add(new Entry(null, Kind.HEADER, "CPS Counter"));
        e.add(new Entry("cpsRightClick", Kind.TOGGLE, "Right Click CPS", "Show a second line with right-click CPS"));
        e.add(new Entry("cpsShowText", Kind.TOGGLE, "Show CPS Text", "Show the \"CPS\" label next to the number"));
        e.add(new Entry("cpsBrackets", Kind.TOGGLE, "Brackets", "Wrap the number in [ ]"));
        if (c.cpsBrackets) e.add(new Entry("cpsBracketColor", Kind.COLOR, "Bracket Color"));
        e.add(new Entry("cpsReverseText", Kind.TOGGLE, "Reverse Text", "Put the CPS label before the number"));
        e.add(new Entry("cpsScale", Kind.SLIDER, "Scale"));
        e.add(new Entry("cpsTextShadow", Kind.TOGGLE, "Text Shadow"));
        e.add(new Entry("cpsTextColor", Kind.COLOR, "Text"));
        e.add(new Entry("cpsBackground", Kind.TOGGLE, "Background"));
        if (c.cpsBackground) {
            e.add(new Entry("cpsBgWidth", Kind.SLIDER, "Width"));
            e.add(new Entry("cpsBgHeight", Kind.SLIDER, "Height"));
            e.add(new Entry("cpsBgColor", Kind.COLOR, "Background Color"));
        }
        e.add(new Entry("cpsBorder", Kind.TOGGLE, "Border"));
        if (c.cpsBorder) {
            e.add(new Entry("cpsBorderThickness", Kind.SLIDER, "Thickness"));
            e.add(new Entry("cpsBorderColor", Kind.COLOR, "Border Color"));
        }
        return e;
    }

    private static int rowHeight(Kind k) {
        return switch (k) {
            case HEADER -> HEADER_H;
            case SLIDER -> SLIDER_H;
            default -> ROW_H;
        };
    }

    private boolean isEditingColor(Entry e) {
        return e.kind() == Kind.COLOR && e.id().equals(editingField);
    }

    private int contentHeight() {
        int h = PAD;
        for (Entry e : entries()) {
            h += rowHeight(e.kind());
            if (isEditingColor(e)) h += PICKER_H;
        }
        return h + PAD;
    }

    private int scrollMax() {
        return Math.max(0, contentHeight() - (viewBottom - y0));
    }

    private int scrollbarThumbH() {
        int view = viewBottom - y0;
        int ch = contentHeight();
        if (ch <= view) return 0;
        return Math.max(10, view * view / ch);
    }

    private int scrollbarThumbY() {
        int view = viewBottom - y0;
        int barH = scrollbarThumbH();
        int max = scrollMax();
        return y0 + (max <= 0 ? 0 : Math.round((view - barH) * (scroll / (float) max)));
    }

    private void scrollToThumbTop(int thumbTop) {
        int range = (viewBottom - y0) - scrollbarThumbH();
        int max = scrollMax();
        if (range <= 0 || max <= 0) { scroll = 0; return; }
        float frac = Math.max(0f, Math.min(1f, (thumbTop - y0) / (float) range));
        scroll = Math.round(frac * max);
    }

    public void render(DrawContext ctx, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        if (!open) return;
        layout(screenWidth, screenHeight);
        tooltip = null;
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;

        ctx.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0xAA2A2A2A);
        ctx.fill(x0, y0, x1, y1, 0x26101010);

        ctx.enableScissor(x0, y0, x1, viewBottom);
        int y = y0 + PAD - scroll;
        for (Entry e : entries()) {
            int h = rowHeight(e.kind());
            if (y + h >= y0 && y <= viewBottom) {
                renderRow(ctx, tr, e, y, mouseX, mouseY);
            }
            y += h;
            if (isEditingColor(e)) {
                if (y + PICKER_H >= y0 && y <= viewBottom) renderPicker(ctx, tr, y);
                y += PICKER_H;
            }
        }
        ctx.disableScissor();

        int barH = scrollbarThumbH();
        if (barH > 0) {
            int barY = scrollbarThumbY();
            ctx.fill(x1 - 5, y0, x1 - 1, viewBottom, 0x18FFFFFF);
            ctx.fill(x1 - 5, barY, x1 - 1, barY + barH, draggingScrollbar ? 0xFFAAAAAA : 0xFF666666);
        }

        if (tooltip != null) {
            int w = tr.getWidth(tooltip) + 6;
            int tx = Math.min(mouseX + 8, screenW - w - 2);
            int ty = mouseY - 14;
            ctx.fill(tx - 1, ty - 1, tx + w + 1, ty + 12, 0xFF2A2A2A);
            ctx.fill(tx, ty, tx + w, ty + 11, 0xF0100010);
            ctx.drawText(tr, tooltip, tx + 3, ty + 2, 0xFFE0E0E0, false);
        }
    }

    private void renderRow(DrawContext ctx, TextRenderer tr, Entry e, int y, int mouseX, int mouseY) {
        int labelX = x0 + PAD;
        boolean rowHovered = mouseX >= x0 && mouseX <= x1 && mouseY >= y && mouseY < y + rowHeight(e.kind()) && mouseY < viewBottom;
        if (rowHovered && e.tooltip() != null) tooltip = e.tooltip();

        switch (e.kind()) {
            case HEADER -> {
                ctx.drawText(tr, e.label(), labelX, y + 4, 0xFFAAAAAA, false);
                ctx.fill(labelX + tr.getWidth(e.label()) + 4, y + 8, x1 - PAD, y + 9, 0xFF2A2A2A);
            }
            case TOGGLE -> {
                if (rowHovered) ctx.fill(x0 + 2, y, x1 - 2, y + ROW_H, 0x20FFFFFF);
                ctx.drawText(tr, e.label(), labelX, y + 3, 0xFFE0E0E0, false);
                boolean on = getToggle(e.id());
                int pillX = x1 - PAD - 20;
                int pillY = y + 3;
                ctx.fill(pillX, pillY, pillX + 20, pillY + 8, on ? 0xFF2E7D32 : 0xFF4A4A4A);
                int dotX = on ? pillX + 13 : pillX + 1;
                ctx.fill(dotX, pillY + 1, dotX + 6, pillY + 7, 0xFFFFFFFF);
            }
            case SLIDER -> {
                if (rowHovered) ctx.fill(x0 + 2, y, x1 - 2, y + ROW_H, 0x20FFFFFF);
                ctx.drawText(tr, e.label(), labelX, y + 3, 0xFFE0E0E0, false);
                String val = formatSliderValue(e.id());
                ctx.drawText(tr, val, x1 - PAD - tr.getWidth(val), y + 3, 0xFFAAAAAA, false);

                int[] tb = sliderTrack(e.label());
                int trackX = tb[0], trackW = tb[1];
                int trackY = y + 6;
                float[] r = sliderRange(e.id());
                float frac = (getSlider(e.id()) - r[0]) / (r[1] - r[0]);
                frac = Math.max(0f, Math.min(1f, frac));
                ctx.fill(trackX, trackY, trackX + trackW, trackY + 3, 0xFF000000);
                ctx.fill(trackX, trackY, trackX + (int) (trackW * frac), trackY + 3, 0xFF707070);
                int thumbX = trackX + (int) ((trackW - 4) * frac);
                ctx.fill(thumbX, trackY - 2, thumbX + 4, trackY + 5, 0xFFBEBEBE);
                ctx.fill(thumbX, trackY - 2, thumbX + 4, trackY - 1, 0xFFFFFFFF);
            }
            case COLOR -> {
                boolean editing = e.id().equals(editingField);
                if (rowHovered || editing) ctx.fill(x0 + 2, y, x1 - 2, y + ROW_H, editing ? 0x33FFFFFF : 0x20FFFFFF);
                ctx.drawText(tr, e.label(), labelX, y + 3, 0xFFE0E0E0, false);
                String hex = getColor(e.id());
                Integer argb = HexColor.parse(hex);
                int swX = x1 - PAD - 10;
                ctx.drawText(tr, hex, swX - 4 - tr.getWidth(hex), y + 3, 0xFF888888, false);
                ctx.fill(swX - 1, y + 2, swX + 9, y + 12, 0xFF666666);
                ctx.fill(swX, y + 3, swX + 8, y + 11, argb == null ? 0xFFFF00FF : argb);
            }
        }
    }

    public boolean mouseClicked(double mx, double my) {
        if (!open) return false;
        if (mx < x0 || mx > x1 || my < y0 || my > y1) return false;
        int barH = scrollbarThumbH();
        if (barH > 0 && mx >= x1 - 7 && my >= y0 && my <= viewBottom) {
            int thumbY = scrollbarThumbY();
            if (my >= thumbY && my < thumbY + barH) {
                scrollbarGrab = (int) (my - thumbY);
            } else {
                scrollbarGrab = barH / 2;
                scrollToThumbTop((int) (my - barH / 2));
            }
            draggingScrollbar = true;
            return true;
        }
        if (my >= viewBottom) return true;
        int y = y0 + PAD - scroll;
        for (Entry e : entries()) {
            int h = rowHeight(e.kind());
            if (my >= y && my < y + h) {
                rowClicked(e, y, mx, my);
                return true;
            }
            y += h;
            if (isEditingColor(e)) {
                if (my >= y && my < y + PICKER_H) { pickerClicked(mx, my, y); return true; }
                y += PICKER_H;
            }
        }
        return true;
    }

    private void rowClicked(Entry e, int rowY, double mx, double my) {
        switch (e.kind()) {
            case TOGGLE -> {
                boolean on = !getToggle(e.id());
                setToggle(e.id(), on);
                if (on) bumpSectionOrder(e.id());
                HudConfig.save();
            }
            case SLIDER -> {
                int[] tb = sliderTrack(e.label());
                int trackX = tb[0], trackW = tb[1];
                if (mx >= trackX - 2 && mx <= trackX + trackW + 2 && my >= rowY && my < rowY + SLIDER_H) {
                    draggingSlider = e.id();
                    dsTrackX = trackX;
                    dsTrackW = trackW;
                    dragSliderTo(e.id(), mx);
                }
            }
            case COLOR -> {
                if (e.id().equals(editingField)) commitColorEdit();
                else openColorEditor(e.id());
            }
            default -> {}
        }
    }

    private int[] sliderTrack(String label) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int trackX = x0 + PAD + tr.getWidth(label) + 6;
        int trackRight = x1 - PAD - SLIDER_VALUE_W;
        return new int[]{ trackX, Math.max(10, trackRight - trackX) };
    }

    private void dragSliderTo(String id, double mx) {
        float frac = (float) ((mx - dsTrackX) / dsTrackW);
        frac = Math.max(0f, Math.min(1f, frac));
        float[] r = sliderRange(id);
        setSlider(id, r[0] + frac * (r[1] - r[0]));
    }

    public boolean mouseDragged(double mx, double my) {
        if (!open) return false;
        if (draggingScrollbar) { scrollToThumbTop((int) (my - scrollbarGrab)); return true; }
        if (draggingSlider != null) { dragSliderTo(draggingSlider, mx); return true; }
        if (pickDrag != PickDrag.NONE) { pickerDragTo(mx, my); return true; }
        return false;
    }

    public void mouseReleased() {
        draggingSlider = null;
        pickDrag = PickDrag.NONE;
        draggingScrollbar = false;
    }

    private void openColorEditor(String id) {
        editingField = id;
        editingHex = false;
        pickDrag = PickDrag.NONE;
        Integer argb = HexColor.parse(getColor(id));
        setHsvaFromArgb(argb != null ? argb : 0xFFFFFFFF);
        editBuffer = currentHex();
    }

    public void commitColorEdit() {
        editingField = null;
        editingHex = false;
        editBuffer = "";
        pickDrag = PickDrag.NONE;
    }

    public boolean charTyped(char c) {
        if (editingField == null || !editingHex) return false;
        if (c == '#') {
            if (editBuffer.isEmpty()) editBuffer = "#";
        } else if (isHexDigit(c) && editBuffer.replace("#", "").length() < 8) {
            editBuffer += Character.toUpperCase(c);
        }
        applyHexLive();
        return true;
    }

    public boolean keyPressed(int keyCode) {
        if (editingField == null) return false;
        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!editingHex) return false;
                if (!editBuffer.isEmpty()) editBuffer = editBuffer.substring(0, editBuffer.length() - 1);
                applyHexLive();
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_ESCAPE -> {
                commitColorEdit();
                return true;
            }
            default -> { return false; }
        }
    }

    private void applyHexLive() {
        String norm = (editBuffer.startsWith("#") ? editBuffer : "#" + editBuffer).toUpperCase();
        Integer argb = HexColor.parse(norm);
        if (argb != null) {
            setHsvaFromArgb(argb);
            setColor(editingField, norm);
        }
    }

    private String currentHex() {
        return HexColor.format(ColorUtil.hsvaToArgb(pickH, pickS, pickV, pickA));
    }

    private void setHsvaFromArgb(int argb) {
        float[] hsva = ColorUtil.argbToHsva(argb);
        pickH = hsva[0]; pickS = hsva[1]; pickV = hsva[2]; pickA = hsva[3];
    }

    private void applyPickColor() {
        if (editingField == null) return;
        String hex = currentHex();
        editBuffer = hex;
        setColor(editingField, hex);
    }

    private void computePickerGeom(int top) {
        pkSvX = x0 + PAD;
        pkSvY = top + 4;
        pkHueX = pkSvX + SV_W + STRIP_GAP;
        pkAlphaX = pkHueX + STRIP_W + STRIP_GAP;
        pkPresetX = pkAlphaX + STRIP_W + 8;
        pkHexX = pkSvX;
        pkHexY = pkSvY + SV_H + 4;
    }

    private void renderPicker(DrawContext ctx, TextRenderer tr, int top) {
        computePickerGeom(top);

        for (int dx = 0; dx < SV_W; dx++) {
            float s = dx / (float) SV_W;
            ctx.fillGradient(pkSvX + dx, pkSvY, pkSvX + dx + 1, pkSvY + SV_H,
                    ColorUtil.hsvToRgb(pickH, s, 1f), 0xFF000000);
        }
        int svTx = pkSvX + Math.round(pickS * SV_W);
        int svTy = pkSvY + Math.round((1 - pickV) * SV_H);
        drawRing(ctx, svTx, svTy);

        for (int dy = 0; dy < SV_H; dy++) {
            ctx.fill(pkHueX, pkSvY + dy, pkHueX + STRIP_W, pkSvY + dy + 1, ColorUtil.hsvToRgb(dy / (float) SV_H, 1f, 1f));
        }
        int hueY = pkSvY + Math.round(pickH * SV_H);
        ctx.fill(pkHueX - 1, hueY - 1, pkHueX + STRIP_W + 1, hueY + 1, 0xFFFFFFFF);

        drawChecker(ctx, pkAlphaX, pkSvY, STRIP_W, SV_H);
        int opaque = ColorUtil.hsvToRgb(pickH, pickS, pickV);
        ctx.fillGradient(pkAlphaX, pkSvY, pkAlphaX + STRIP_W, pkSvY + SV_H, opaque, opaque & 0x00FFFFFF);
        int aY = pkSvY + Math.round((1 - pickA) * SV_H);
        ctx.fill(pkAlphaX - 1, aY - 1, pkAlphaX + STRIP_W + 1, aY + 1, 0xFFFFFFFF);

        int rows = (PRESETS.length + 1) / 2;
        int cellW = (x1 - PAD - pkPresetX) / 2, cellH = SV_H / rows;
        for (int i = 0; i < PRESETS.length; i++) {
            int px = pkPresetX + (i % 2) * cellW, py = pkSvY + (i / 2) * cellH;
            Integer pc = HexColor.parse(PRESETS[i]);
            ctx.fill(px, py, px + cellW - 3, py + cellH - 3, 0xFF000000);
            ctx.fill(px + 1, py + 1, px + cellW - 4, py + cellH - 4, pc == null ? 0xFFFF00FF : pc);
        }

        String hex = editingHex ? editBuffer : currentHex();
        Integer parsed = HexColor.parse(hex.startsWith("#") ? hex : "#" + hex);
        int hexCol = editingHex ? (parsed != null ? 0xFFFFFFFF : 0xFFFF5555) : 0xFFC0C0C0;
        ctx.drawText(tr, hex, pkHexX, pkHexY, hexCol, false);
        if (editingHex && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = pkHexX + tr.getWidth(hex) + 1;
            ctx.fill(cx, pkHexY - 1, cx + 1, pkHexY + 8, 0xFFFFFFFF);
        }
    }

    private void pickerClicked(double mx, double my, int top) {
        computePickerGeom(top);
        editingHex = false;
        if (inRect(mx, my, pkSvX, pkSvY, SV_W, SV_H)) { pickDrag = PickDrag.SV; updateSV(mx, my); return; }
        if (inRect(mx, my, pkHueX, pkSvY, STRIP_W, SV_H)) { pickDrag = PickDrag.HUE; updateHue(my); return; }
        if (inRect(mx, my, pkAlphaX, pkSvY, STRIP_W, SV_H)) { pickDrag = PickDrag.ALPHA; updateAlpha(my); return; }

        int rows = (PRESETS.length + 1) / 2;
        int cellW = (x1 - PAD - pkPresetX) / 2, cellH = SV_H / rows;
        for (int i = 0; i < PRESETS.length; i++) {
            int px = pkPresetX + (i % 2) * cellW, py = pkSvY + (i / 2) * cellH;
            if (inRect(mx, my, px, py, cellW - 3, cellH - 3)) {
                Integer pc = HexColor.parse(PRESETS[i]);
                if (pc != null) { setHsvaFromArgb(pc); applyPickColor(); }
                return;
            }
        }

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        if (my >= pkHexY - 1 && my < pkHexY + 9 && mx >= pkHexX && mx <= pkHexX + tr.getWidth(currentHex()) + 4) {
            editingHex = true;
            editBuffer = currentHex();
        }
    }

    private void pickerDragTo(double mx, double my) {
        switch (pickDrag) {
            case SV -> updateSV(mx, my);
            case HUE -> updateHue(my);
            case ALPHA -> updateAlpha(my);
            default -> {}
        }
    }

    private void updateSV(double mx, double my) {
        pickS = clamp01((float) ((mx - pkSvX) / SV_W));
        pickV = clamp01(1f - (float) ((my - pkSvY) / SV_H));
        applyPickColor();
    }

    private void updateHue(double my) {
        pickH = clamp01((float) ((my - pkSvY) / SV_H));
        applyPickColor();
    }

    private void updateAlpha(double my) {
        pickA = clamp01(1f - (float) ((my - pkSvY) / SV_H));
        applyPickColor();
    }

    private void drawRing(DrawContext ctx, int cx, int cy) {
        ctx.fill(cx - 3, cy - 3, cx + 4, cy - 2, 0xFFFFFFFF);
        ctx.fill(cx - 3, cy + 3, cx + 4, cy + 4, 0xFFFFFFFF);
        ctx.fill(cx - 3, cy - 2, cx - 2, cy + 3, 0xFFFFFFFF);
        ctx.fill(cx + 3, cy - 2, cx + 4, cy + 3, 0xFFFFFFFF);
    }

    private void drawChecker(DrawContext ctx, int x, int y, int w, int h) {
        int cs = 4;
        for (int dy = 0; dy < h; dy += cs)
            for (int dx = 0; dx < w; dx += cs) {
                boolean dark = ((dx / cs) + (dy / cs)) % 2 == 0;
                ctx.fill(x + dx, y + dy, Math.min(x + dx + cs, x + w), Math.min(y + dy + cs, y + h),
                        dark ? 0xFF999999 : 0xFF666666);
            }
    }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static float clamp01(float x) {
        return x < 0f ? 0f : (x > 1f ? 1f : x);
    }

    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!open || mx < x0 || mx > x1 || my < y0 || my > y1) return false;
        int max = Math.max(0, contentHeight() - (viewBottom - y0));
        scroll = Math.max(0, Math.min(max, scroll - (int) (amount * 12)));
        return true;
    }

    private boolean getToggle(String id) {
        HudConfig c = HudConfig.get();
        return switch (id) {
            case "leftClicking" -> c.leftClicking;
            case "airClicking" -> c.airClicking;
            case "keysVisible" -> c.keysModule.visible;
            case "cpsVisible" -> c.cpsModule.visible;
            case "showClicks" -> c.showClicks;
            case "showLmbCps" -> c.showLmbCps;
            case "showRmbCps" -> c.showRmbCps;
            case "showSpaceBar" -> c.showSpaceBar;
            case "showMovementKeys" -> c.showMovementKeys;
            case "arrowsMode" -> c.arrowsMode;
            case "border" -> c.border;
            case "textShadow" -> c.textShadow;
            case "cpsRightClick" -> c.cpsRightClick;
            case "cpsShowText" -> c.cpsShowText;
            case "cpsBrackets" -> c.cpsBrackets;
            case "cpsReverseText" -> c.cpsReverseText;
            case "cpsTextShadow" -> c.cpsTextShadow;
            case "cpsBackground" -> c.cpsBackground;
            case "cpsBorder" -> c.cpsBorder;
            default -> false;
        };
    }

    private static void bumpSectionOrder(String toggleId) {
        String section = switch (toggleId) {
            case "showMovementKeys" -> "movement";
            case "showClicks" -> "clicks";
            case "showSpaceBar" -> "space";
            default -> null;
        };
        if (section == null) return;
        var order = HudConfig.get().sectionOrder;
        order.remove(section);
        order.add(section);
    }

    private void setToggle(String id, boolean v) {
        HudConfig c = HudConfig.get();
        switch (id) {
            case "leftClicking" -> c.leftClicking = v;
            case "airClicking" -> c.airClicking = v;
            case "keysVisible" -> c.keysModule.visible = v;
            case "cpsVisible" -> c.cpsModule.visible = v;
            case "showClicks" -> c.showClicks = v;
            case "showLmbCps" -> c.showLmbCps = v;
            case "showRmbCps" -> c.showRmbCps = v;
            case "showSpaceBar" -> c.showSpaceBar = v;
            case "showMovementKeys" -> c.showMovementKeys = v;
            case "arrowsMode" -> c.arrowsMode = v;
            case "border" -> c.border = v;
            case "textShadow" -> c.textShadow = v;
            case "cpsRightClick" -> c.cpsRightClick = v;
            case "cpsShowText" -> c.cpsShowText = v;
            case "cpsBrackets" -> c.cpsBrackets = v;
            case "cpsReverseText" -> c.cpsReverseText = v;
            case "cpsTextShadow" -> c.cpsTextShadow = v;
            case "cpsBackground" -> c.cpsBackground = v;
            case "cpsBorder" -> c.cpsBorder = v;
        }
    }

    private static int currentMenuBlur() {
        return MinecraftClient.getInstance().options.getMenuBackgroundBlurrinessValue();
    }

    float[] sliderRange(String id) {
        return switch (id) {
            case "scale" -> new float[]{0.4f, 4.0f};
            case "boxSize" -> new float[]{12f, 32f};
            case "borderThickness" -> new float[]{1f, 3f};
            case "targetCps" -> new float[]{1f, 100f};
            case "keyFadeDelay" -> new float[]{0f, 500f};
            case "backgroundBlur" -> new float[]{0f, 10f};
            case "cpsScale" -> new float[]{0.4f, 4.0f};
            case "cpsBgWidth" -> new float[]{20f, 150f};
            case "cpsBgHeight" -> new float[]{9f, 40f};
            case "cpsBorderThickness" -> new float[]{1f, 3f};
            default -> new float[]{0f, 1f};
        };
    }

    float getSlider(String id) {
        HudConfig c = HudConfig.get();
        return switch (id) {
            case "scale" -> c.keysModule.scale;
            case "boxSize" -> c.boxSize;
            case "borderThickness" -> c.borderThickness;
            case "targetCps" -> c.targetCps;
            case "keyFadeDelay" -> c.keyFadeDelay;
            case "backgroundBlur" -> c.backgroundBlur >= 0 ? c.backgroundBlur : currentMenuBlur();
            case "cpsScale" -> c.cpsModule.scale;
            case "cpsBgWidth" -> c.cpsBgWidth;
            case "cpsBgHeight" -> c.cpsBgHeight;
            case "cpsBorderThickness" -> c.cpsBorderThickness;
            default -> 0f;
        };
    }

    void setSlider(String id, float v) {
        float[] r = sliderRange(id);
        v = Math.max(r[0], Math.min(r[1], v));
        HudConfig c = HudConfig.get();
        switch (id) {
            case "scale" -> c.keysModule.scale = v;
            case "boxSize" -> c.boxSize = Math.round(v);
            case "borderThickness" -> c.borderThickness = Math.round(v);
            case "targetCps" -> c.targetCps = Math.round(v);
            case "keyFadeDelay" -> c.keyFadeDelay = Math.round(v);
            case "backgroundBlur" -> c.backgroundBlur = Math.round(v);
            case "cpsScale" -> c.cpsModule.scale = v;
            case "cpsBgWidth" -> c.cpsBgWidth = Math.round(v);
            case "cpsBgHeight" -> c.cpsBgHeight = Math.round(v);
            case "cpsBorderThickness" -> c.cpsBorderThickness = Math.round(v);
        }
        HudConfig.save();
    }

    String formatSliderValue(String id) {
        HudConfig c = HudConfig.get();
        return switch (id) {
            case "scale" -> String.format("%.2f", c.keysModule.scale);
            case "boxSize" -> String.valueOf(c.boxSize);
            case "borderThickness" -> String.valueOf(c.borderThickness);
            case "targetCps" -> String.valueOf(c.targetCps);
            case "keyFadeDelay" -> c.keyFadeDelay + "ms";
            case "backgroundBlur" -> String.valueOf(c.backgroundBlur >= 0 ? c.backgroundBlur : currentMenuBlur());
            case "cpsScale" -> String.format("%.2f", c.cpsModule.scale);
            case "cpsBgWidth" -> String.valueOf(c.cpsBgWidth);
            case "cpsBgHeight" -> String.valueOf(c.cpsBgHeight);
            case "cpsBorderThickness" -> String.valueOf(c.cpsBorderThickness);
            default -> "";
        };
    }

    void setColor(String id, String hex) {
        if (HexColor.parse(hex) == null) return;
        HudConfig c = HudConfig.get();
        switch (id) {
            case "textColor" -> c.textColor = hex;
            case "textColorPressed" -> c.textColorPressed = hex;
            case "bgColor" -> c.bgColor = hex;
            case "bgColorPressed" -> c.bgColorPressed = hex;
            case "borderColor" -> c.borderColor = hex;
            case "cpsTextColor" -> c.cpsTextColor = hex;
            case "cpsBgColor" -> c.cpsBgColor = hex;
            case "cpsBorderColor" -> c.cpsBorderColor = hex;
            case "cpsBracketColor" -> c.cpsBracketColor = hex;
        }
        c.recomputeColors();
        HudConfig.save();
    }

    String getColor(String id) {
        HudConfig c = HudConfig.get();
        return switch (id) {
            case "textColor" -> c.textColor;
            case "textColorPressed" -> c.textColorPressed;
            case "bgColor" -> c.bgColor;
            case "bgColorPressed" -> c.bgColorPressed;
            case "borderColor" -> c.borderColor;
            case "cpsTextColor" -> c.cpsTextColor;
            case "cpsBgColor" -> c.cpsBgColor;
            case "cpsBorderColor" -> c.cpsBorderColor;
            case "cpsBracketColor" -> c.cpsBracketColor;
            default -> "#FFFFFF";
        };
    }
}
