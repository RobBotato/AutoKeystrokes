package com.botato.autokeystrokes.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class HudConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static final List<String> SECTIONS = List.of("movement", "clicks", "space");

    private static HudConfig instance = new HudConfig();

    static List<String> sanitizeOrder(List<String> order) {
        List<String> out = new ArrayList<>();
        if (order != null) for (String s : order) if (SECTIONS.contains(s) && !out.contains(s)) out.add(s);
        for (String s : SECTIONS) if (!out.contains(s)) out.add(s);
        return out;
    }

    public HudModule keysModule = new HudModule(0, 0, 0.75f, true);
    public HudModule cpsModule = new HudModule(10, 100, 0.75f, true);

    public boolean fastPlace = false;
    public boolean fastJump = false;
    public boolean airClicking = false;
    public boolean leftClicking = true;
    public int targetCps = 25;

    public boolean showClicks = true;
    public boolean showLmbCps = false;
    public boolean showRmbCps = false;
    public boolean showSpaceBar = true;
    public boolean showMovementKeys = true;
    public boolean arrowsMode = false;
    public List<String> sectionOrder = new ArrayList<>(SECTIONS);

    public int boxSize = 18;
    public boolean border = false;
    public int borderThickness = 1;
    public boolean textShadow = false;
    public int keyFadeDelay = 0;
    public int backgroundBlur = -1;

    public String textColor = "#FFFFFFFF";
    public String textColorPressed = "#FF000000";
    public String bgColor = "#6F000000";
    public String bgColorPressed = "#6FFFFFFF";
    public String borderColor = "#FFFFFFFF";

    public transient int textArgb, textPressedArgb, bgArgb, bgPressedArgb, borderArgb;

    public boolean cpsShowText = true;
    public boolean cpsBrackets = false;
    public boolean cpsBackground = true;
    public boolean cpsBorder = false;
    public boolean cpsTextShadow = true;
    public boolean cpsRightClick = true;
    public boolean cpsReverseText = false;

    public int cpsBgWidth = 56;
    public int cpsBgHeight = 18;
    public int cpsBorderThickness = 1;

    public String cpsTextColor = "#FFFFFFFF";
    public String cpsBgColor = "#6F000000";
    public String cpsBorderColor = "#9F000000";
    public String cpsBracketColor = "#FFFFFFFF";

    public transient int cpsTextArgb, cpsBgArgb, cpsBorderArgb, cpsBracketArgb;

    public HudConfig() {
        recomputeColors();
    }

    public static HudConfig get() {
        return instance;
    }

    public void recomputeColors() {
        textArgb = parseOr(textColor, 0xFFFFFFFF);
        if (HexColor.parse(textColor) == null) textColor = "#FFFFFFFF";
        textPressedArgb = parseOr(textColorPressed, 0xFF000000);
        if (HexColor.parse(textColorPressed) == null) textColorPressed = "#FF000000";
        bgArgb = parseOr(bgColor, 0x6F000000);
        if (HexColor.parse(bgColor) == null) bgColor = "#6F000000";
        bgPressedArgb = parseOr(bgColorPressed, 0x6FFFFFFF);
        if (HexColor.parse(bgColorPressed) == null) bgColorPressed = "#6FFFFFFF";
        borderArgb = parseOr(borderColor, 0xFFFFFFFF);
        if (HexColor.parse(borderColor) == null) borderColor = "#FFFFFFFF";
        cpsTextArgb = parseOr(cpsTextColor, 0xFFFFFFFF);
        if (HexColor.parse(cpsTextColor) == null) cpsTextColor = "#FFFFFFFF";
        cpsBgArgb = parseOr(cpsBgColor, 0x6F000000);
        if (HexColor.parse(cpsBgColor) == null) cpsBgColor = "#6F000000";
        cpsBorderArgb = parseOr(cpsBorderColor, 0x9F000000);
        if (HexColor.parse(cpsBorderColor) == null) cpsBorderColor = "#9F000000";
        cpsBracketArgb = parseOr(cpsBracketColor, 0xFFFFFFFF);
        if (HexColor.parse(cpsBracketColor) == null) cpsBracketColor = "#FFFFFFFF";
    }

    private static int parseOr(String hex, int fallback) {
        Integer v = HexColor.parse(hex);
        return v != null ? v : fallback;
    }

    public void resetStyleToDefaults() {
        HudConfig d = new HudConfig();
        keysModule.x = d.keysModule.x;
        keysModule.y = d.keysModule.y;
        keysModule.scale = d.keysModule.scale;
        cpsModule.scale = d.cpsModule.scale;
        leftClicking = d.leftClicking;
        airClicking = d.airClicking;
        targetCps = d.targetCps;
        showClicks = d.showClicks;
        showLmbCps = d.showLmbCps;
        showRmbCps = d.showRmbCps;
        showSpaceBar = d.showSpaceBar;
        showMovementKeys = d.showMovementKeys;
        arrowsMode = d.arrowsMode;
        sectionOrder = new ArrayList<>(SECTIONS);
        boxSize = d.boxSize;
        border = d.border;
        borderThickness = d.borderThickness;
        textShadow = d.textShadow;
        keyFadeDelay = d.keyFadeDelay;
        backgroundBlur = d.backgroundBlur;
        textColor = d.textColor;
        textColorPressed = d.textColorPressed;
        bgColor = d.bgColor;
        bgColorPressed = d.bgColorPressed;
        borderColor = d.borderColor;
        cpsShowText = d.cpsShowText;
        cpsBrackets = d.cpsBrackets;
        cpsBackground = d.cpsBackground;
        cpsBorder = d.cpsBorder;
        cpsTextShadow = d.cpsTextShadow;
        cpsRightClick = d.cpsRightClick;
        cpsReverseText = d.cpsReverseText;
        cpsBgWidth = d.cpsBgWidth;
        cpsBgHeight = d.cpsBgHeight;
        cpsBorderThickness = d.cpsBorderThickness;
        cpsTextColor = d.cpsTextColor;
        cpsBgColor = d.cpsBgColor;
        cpsBorderColor = d.cpsBorderColor;
        cpsBracketColor = d.cpsBracketColor;
        recomputeColors();
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static HudConfig fromJson(String json) {
        try {
            JsonElement el = JsonParser.parseString(json);
            if (el.isJsonArray()) {
                return fromLegacyArray(el.getAsJsonArray());
            }
            HudConfig c = GSON.fromJson(el, HudConfig.class);
            if (c == null) return new HudConfig();
            if (c.keysModule == null) c.keysModule = new HudModule(0, 0, 0.75f, true);
            if (c.cpsModule == null) c.cpsModule = new HudModule(10, 100, 0.75f, true);
            c.sectionOrder = sanitizeOrder(c.sectionOrder);
            c.recomputeColors();
            return c;
        } catch (Exception e) {
            return new HudConfig();
        }
    }

    private static HudConfig fromLegacyArray(JsonArray d) {
        HudConfig c = new HudConfig();
        if (d.size() >= 10) {
            c.cpsModule = new HudModule(d.get(0).getAsFloat(), d.get(1).getAsFloat(), d.get(2).getAsFloat(), d.get(3).getAsFloat() == 1);
            c.keysModule = new HudModule(d.get(4).getAsFloat(), d.get(5).getAsFloat(), d.get(6).getAsFloat(), d.get(7).getAsFloat() == 1);
            c.airClicking = d.get(8).getAsFloat() == 1;
            c.leftClicking = d.get(9).getAsFloat() == 1;
            if (d.size() >= 11) c.targetCps = (int) d.get(10).getAsFloat();
        }
        return c;
    }

    private static File file() {
        return new File(FabricLoader.getInstance().getConfigDir().toFile(), "autokeystrokes_config.json");
    }

    public static void load() {
        File f = file();
        if (!f.exists()) return;
        try {
            instance = fromJson(Files.readString(f.toPath()));
        } catch (Exception e) {
            instance = new HudConfig();
        }
    }

    public static void save() {
        try (FileWriter w = new FileWriter(file())) {
            w.write(instance.toJson());
        } catch (Exception ignored) {}
    }
}
