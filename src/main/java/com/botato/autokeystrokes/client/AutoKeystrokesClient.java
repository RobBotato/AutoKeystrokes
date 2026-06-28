package com.botato.autokeystrokes.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import com.botato.autokeystrokes.mixin.IMinecraftClientAccessor;
import com.botato.autokeystrokes.mixin.LivingEntityAccessor;
import org.lwjgl.glfw.GLFW;

public class AutoKeystrokesClient implements ClientModInitializer {

    private static KeyMapping toggleKey, configKey, jumpToggleKey;
    private static HudModule selectedModule = null;

    private boolean leftWasDown, rightWasDown;
    private double rightClickAccumulator = 0.0;
    private double leftClickAccumulator = 0.0;

    private int tickCounter = 0;
    private boolean wasInAir = false;
    private boolean isFirstJump = false;
    private boolean wasSpacePressed = false;

    private final long[] keyLastDown = new long[7];

    @Override
    public void onInitializeClient() {
        HudConfig.load();

        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("autokeystrokes", "category"));
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.autokeystrokes.toggle_fastplace", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Q, cat));
        jumpToggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.autokeystrokes.toggle_fastjump", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, cat));
        configKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.autokeystrokes.config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, cat));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            HudConfig cfg = HudConfig.get();

            while (toggleKey.consumeClick()) { cfg.fastPlace = !cfg.fastPlace; HudConfig.save(); }
            while (jumpToggleKey.consumeClick()) { cfg.fastJump = !cfg.fastJump; HudConfig.save(); }
            if (configKey.consumeClick() && !(client.screen instanceof CPSConfigScreen)) client.setScreen(new CPSConfigScreen());

            if (client.screen != null) return;

            long win = client.getWindow().handle();
            long now = System.currentTimeMillis();

            boolean rDown = GLFW.glfwGetMouseButton(win, 1) == 1;
            if (cfg.fastPlace && rDown) {
                double clicksPerTick = ((cfg.targetCps - 2.0) + (Math.random() * 4.0)) / 20.0;
                rightClickAccumulator += clicksPerTick;
                while (rightClickAccumulator >= 1.0) {
                    ClickTracker.addRight(now);
                    rightClickAccumulator -= 1.0;
                }
            } else {
                rightClickAccumulator = 0.0;
                if (rDown && !rightWasDown) ClickTracker.addRight(now);
            }
            rightWasDown = rDown;

            boolean lDown = GLFW.glfwGetMouseButton(win, 0) == 1;
            if (cfg.fastPlace && cfg.leftClicking && lDown) {
                IMinecraftClientAccessor accessor = (IMinecraftClientAccessor) client;

                accessor.setAttackCooldown(0);
                client.options.keyAttack.setDown(true);
                accessor.invokeDoAttack();
                client.options.keyAttack.setDown(false);

                double lClicksPerTick = ((cfg.targetCps - 2.0) + (Math.random() * 4.0)) / 20.0;
                leftClickAccumulator += lClicksPerTick;
                while (leftClickAccumulator >= 1.0) {
                    ClickTracker.addLeft(now);
                    leftClickAccumulator -= 1.0;
                }
            } else {
                leftClickAccumulator = 0.0;
                if (lDown && !leftWasDown) ClickTracker.addLeft(now);
            }
            leftWasDown = lDown;

            boolean spaceHeldNow = client.options.keyJump.isDown();
            if (client.player.onGround()) {
                wasInAir = false;
            } else {
                if (!wasInAir) {
                    tickCounter = 0;
                    wasInAir = true;
                    isFirstJump = false;
                }
                if (tickCounter <= 10) tickCounter++;
            }

            if (cfg.fastJump && spaceHeldNow) {
                if (client.player.onGround() && !((LivingEntityAccessor)client.player).isJumping()) {
                    client.player.jumpFromGround();
                }
            }
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("autokeystrokes", "hud"), (drawContext, tickCounter) -> {
            HudConfig cfg = HudConfig.get();
            if (cfg.keysModule.visible) renderKeystrokes(drawContext, cfg.keysModule.x, cfg.keysModule.y, cfg.keysModule.scale, false);
            if (cfg.cpsModule.visible) renderCPS(drawContext, cfg.cpsModule.x, cfg.cpsModule.y, cfg.cpsModule.scale);
        });
    }

    private void renderCPS(GuiGraphicsExtractor context, float x, float y, float sc) {
        CpsRenderer.render(context, x, y, sc);
    }

    private void renderKeystrokes(GuiGraphicsExtractor context, float x, float y, float sc, boolean inGuiMode) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        HudConfig cfg = HudConfig.get();

        boolean inGui = mc.screen != null;
        boolean screenOpen = mc.screen instanceof Screen;
        boolean lmbDown = !inGuiMode && !inGui && GLFW.glfwGetMouseButton(mc.getWindow().handle(), 0) == 1;
        boolean rmbDown = !inGuiMode && !inGui && GLFW.glfwGetMouseButton(mc.getWindow().handle(), 1) == 1;
        if (cfg.fastPlace) {
            if (rmbDown) rmbDown = (System.currentTimeMillis() / 28) % 2 == 0;
            if (lmbDown && cfg.leftClicking) lmbDown = (System.currentTimeMillis() / 28) % 2 == 0;
        }

        boolean spaceHeld = mc.options.keyJump.isDown();
        boolean spaceVisualPressed = spaceHeld;
        if (cfg.fastJump && spaceHeld) {
            if (mc.player.onGround()) {
                if (isFirstJump) {
                    spaceVisualPressed = true;
                } else {
                    spaceVisualPressed = !(tickCounter <= 10 && tickCounter > 0);
                }
            } else {
                spaceVisualPressed = true;
            }
        }
        if (spaceHeld && !wasSpacePressed) isFirstJump = true;
        wasSpacePressed = spaceHeld;

        boolean[] down = {
                mc.options.keyUp.isDown(), mc.options.keyLeft.isDown(),
                mc.options.keyDown.isDown(), mc.options.keyRight.isDown(),
                lmbDown, rmbDown, spaceVisualPressed
        };
        float[] f = fades(down, cfg.keyFadeDelay, inGui);

        KeystrokesRenderer.render(context, x, y, sc, new KeystrokesRenderer.KeyStates(
                f[0], f[1], f[2], f[3], f[4], f[5], f[6],
                ClickTracker.leftCps(), ClickTracker.rightCps()));
    }

    private float[] fades(boolean[] down, int delayMs, boolean inGui) {
        float[] f = new float[down.length];
        long now = System.currentTimeMillis();
        for (int i = 0; i < down.length; i++) {
            if (inGui) {
                f[i] = down[i] ? 1f : 0f;
            } else if (down[i]) {
                keyLastDown[i] = now;
                f[i] = 1f;
            } else {
                f[i] = delayMs <= 0 ? 0f : Math.max(0f, (delayMs - (now - keyLastDown[i])) / (float) delayMs);
            }
        }
        return f;
    }

    public class CPSConfigScreen extends Screen {
        private boolean dragging = false;
        private double dragOffsetX, dragOffsetY;
        private SettingsPanel panel;
        private net.minecraft.client.gui.components.Button settingsButton;
        private net.minecraft.client.gui.components.Button resetButton;
        private Integer savedBlurriness;
        private float[] vGuide, hGuide;

        public CPSConfigScreen() { super(Component.literal("")); }

        @Override
        protected void init() {
            if (savedBlurriness == null)
                savedBlurriness = Minecraft.getInstance().options.menuBackgroundBlurriness().get();
            panel = new SettingsPanel();
            panel.layout(width, height);
            settingsButton = net.minecraft.client.gui.components.Button.builder(
                    Component.literal(panel.open ? "Done" : "Settings"), b -> {
                        panel.open = !panel.open;
                        settingsButton.setMessage(Component.literal(panel.open ? "Done" : "Settings"));
                        resetButton.visible = panel.open;
                        if (!panel.open) panel.editingField = null;
                    })
                .bounds(width / 2 - 40, height - 28, 80, 20)
                .build();
            addRenderableWidget(settingsButton);

            resetButton = net.minecraft.client.gui.components.Button.builder(
                    Component.literal("Reset to Defaults"), b -> {
                        HudConfig cfg = HudConfig.get();
                        cfg.resetStyleToDefaults();
                        float kw = KeystrokesRenderer.width() * cfg.keysModule.scale;
                        float kh = KeystrokesRenderer.height() * cfg.keysModule.scale;
                        float cw = CpsRenderer.width() * cfg.cpsModule.scale;
                        cfg.cpsModule.x = cfg.keysModule.x + kw / 2.0f - cw / 2.0f;
                        cfg.cpsModule.y = cfg.keysModule.y + kh + KeystrokesRenderer.GAP * cfg.keysModule.scale;
                        HudConfig.save();
                    })
                .bounds((int)panel.resetButtonX(), (int)panel.resetButtonY(), (int)panel.resetButtonW(), 20)
                .build();
            resetButton.visible = panel.open;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            HudConfig cfg = HudConfig.get();
            int blur = cfg.backgroundBlur >= 0 ? cfg.backgroundBlur
                    : (savedBlurriness != null ? savedBlurriness : Minecraft.getInstance().options.menuBackgroundBlurriness().get());
            Minecraft.getInstance().options.menuBackgroundBlurriness().set(blur);

            super.extractRenderState(context, mouseX, mouseY, delta);
            drawEditor(context, cfg.keysModule, "Keys", KeystrokesRenderer.width(), KeystrokesRenderer.height());
            drawEditor(context, cfg.cpsModule, "CPS", CpsRenderer.width(), CpsRenderer.height());

            if (dragging) {
                int gc = 0x66AAAAAA;
                if (vGuide != null) context.fill(Math.round(vGuide[0]), Math.round(vGuide[1]), Math.round(vGuide[0]) + 1, Math.round(vGuide[2]), gc);
                if (hGuide != null) context.fill(Math.round(hGuide[1]), Math.round(hGuide[0]), Math.round(hGuide[2]), Math.round(hGuide[0]) + 1, gc);
            }

            panel.render(context, width, height, mouseX, mouseY);
            if (panel.open) resetButton.extractRenderState(context, mouseX, mouseY, delta);
        }

        private void drawEditor(GuiGraphicsExtractor ctx, HudModule mod, String name, int w, int h) {
            boolean isSelected = (selectedModule == mod);
            ctx.fill(Math.round(mod.x), Math.round(mod.y),
                    Math.round(mod.x + w * mod.scale), Math.round(mod.y + h * mod.scale),
                    isSelected ? 0x66888888 : 0x22FFFFFF);

            String icon = mod.visible ? "X" : "O";
            int iconCol = mod.visible ? 0xFFFF5555 : 0xFF55FF55;
            ctx.pose().pushMatrix();
            ctx.pose().translate(mod.x + (w * mod.scale) + 2, mod.y);
            ctx.pose().scale(mod.scale * (name.equals("Keys") ? 0.9f : 0.6f), mod.scale * (name.equals("Keys") ? 0.9f : 0.6f));
            ctx.text(font, icon, 0, 0, iconCol, true);
            ctx.pose().popMatrix();

            if (isSelected) {
                String label = String.format("%.2f", mod.scale);
                ctx.pose().pushMatrix();
                float lblSc = 0.55f;
                int cX = (int)(mod.x + (w * mod.scale) / 2);
                ctx.pose().translate(cX - ((font.width(label) * lblSc)/2), mod.y + (h * mod.scale) + 2);
                ctx.pose().scale(lblSc, lblSc);
                ctx.text(font, label, 0, 0, 0xFFFFFFFF, true);
                ctx.pose().popMatrix();
            }

            if (name.equals("Keys")) renderKeystrokes(ctx, mod.x, mod.y, mod.scale, true);
            else renderCPS(ctx, mod.x, mod.y, mod.scale);
        }

        @Override
        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean bl) {
            double mx = event.x();
            double my = event.y();
            int btn = event.button();
            HudConfig cfg = HudConfig.get();

            if (panel.open && resetButton.mouseClicked(event, bl)) return true;
            if (super.mouseClicked(event, bl)) return true;
            if (panel.mouseClicked(mx, my)) return true;
            if (panel.editingField != null) { panel.commitColorEdit(); return true; }

            int cpsW = CpsRenderer.width();
            int cpsH = CpsRenderer.height();
            int kw = KeystrokesRenderer.width();
            int kh = KeystrokesRenderer.height();
            if (checkToggle(mx, my, cfg.keysModule, kw, 0.9f)) return true;
            if (checkToggle(mx, my, cfg.cpsModule, cpsW, 0.6f)) return true;
            if (cfg.keysModule.isHovered(mx, my, kw, kh)) { select(cfg.keysModule, btn, mx, my, kw, kh); return true; }
            if (cfg.cpsModule.isHovered(mx, my, cpsW, cpsH)) { select(cfg.cpsModule, btn, mx, my, cpsW, cpsH); return true; }
            selectedModule = null;
            return false;
        }

        private boolean checkToggle(double mx, double my, HudModule mod, int w, float scMod) {
            float tX = mod.x + (w * mod.scale) + 2;
            if (mx >= tX && mx <= tX + (8 * mod.scale * scMod) + 5 && my >= mod.y && my <= mod.y + 15) {
                mod.visible = !mod.visible; HudConfig.save(); return true;
            }
            return false;
        }

        private void select(HudModule mod, int btn, double mx, double my, int w, int h) {
            selectedModule = mod;
            if (btn == 0) {
                dragging = true;
                dragOffsetX = mx - mod.x;
                dragOffsetY = my - mod.y;
            }
            if (btn == 1) { mod.scale = 1.0f; mod.clamp(width, height, w, h); HudConfig.save(); }
        }

        @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
            double mx = event.x();
            double my = event.y();
            HudConfig cfg = HudConfig.get();
            if (panel.mouseDragged(mx, my)) return true;
            if (dragging && selectedModule != null) {
                boolean draggingKeys = (selectedModule == cfg.keysModule);
                HudModule oth = draggingKeys ? cfg.cpsModule : cfg.keysModule;
                int selRawW = draggingKeys ? KeystrokesRenderer.width() : CpsRenderer.width();
                int selRawH = draggingKeys ? KeystrokesRenderer.height() : CpsRenderer.height();
                int othRawW = draggingKeys ? CpsRenderer.width() : KeystrokesRenderer.width();
                int othRawH = draggingKeys ? CpsRenderer.height() : KeystrokesRenderer.height();
                float selW = selRawW * selectedModule.scale, selH = selRawH * selectedModule.scale;
                float othW = othRawW * oth.scale, othH = othRawH * oth.scale;

                selectedModule.x = (float)(mx - dragOffsetX);
                selectedModule.y = (float)(my - dragOffsetY);
                vGuide = null;
                hGuide = null;

                alignToModule(selectedModule, selW, selH, oth, othW, othH);
                selectedModule.clamp(width, height, selRawW, selRawH);
                resolveOverlap(selectedModule, selW, selH, oth, othW, othH);
                computeGuides(selectedModule, selW, selH, oth, othW, othH);
                return true;
            }
            return false;
        }

        private void alignToModule(HudModule sel, float selW, float selH, HudModule oth, float othW, float othH) {
            final float TH = 3f;
            float minGap = KeystrokesRenderer.GAP * Math.max(sel.scale, oth.scale);

            
            float[] selX = { sel.x, sel.x + selW / 2f, sel.x + selW, sel.x + selW, sel.x };
            float[] othX = { oth.x, oth.x + othW / 2f, oth.x + othW, oth.x - minGap, oth.x + othW + minGap };
            float best = TH; Float to = null;
            for (int i = 0; i < selX.length; i++) {
                float sx = selX[i], ox = othX[i];
                float d = Math.abs(ox - sx);
                if (d <= best) { best = d; to = sel.x + (ox - sx); }
            }
            if (to != null) sel.x = to;

            
            float[] selY = { sel.y, sel.y + selH / 2f, sel.y + selH, sel.y + selH, sel.y };
            float[] othY = { oth.y, oth.y + othH / 2f, oth.y + othH, oth.y - minGap, oth.y + othH + minGap };
            best = TH; to = null;
            for (int i = 0; i < selY.length; i++) {
                float sy = selY[i], oy = othY[i];
                float d = Math.abs(oy - sy);
                if (d <= best) { best = d; to = sel.y + (oy - sy); }
            }
            if (to != null) sel.y = to;
        }

        private void computeGuides(HudModule sel, float selW, float selH, HudModule oth, float othW, float othH) {
            final float EPS = 1.5f;
            float minGap = KeystrokesRenderer.GAP * Math.max(sel.scale, oth.scale);

            float[] selX = { sel.x, sel.x + selW / 2f, sel.x + selW, sel.x + selW, sel.x };
            float[] othX = { oth.x, oth.x + othW / 2f, oth.x + othW, oth.x - minGap, oth.x + othW + minGap };
            outerX:
            for (int i = 0; i < selX.length; i++) {
                float sx = selX[i], ox = othX[i];
                if (Math.abs(sx - ox) <= EPS) {
                    vGuide = new float[]{ ox, Math.min(sel.y, oth.y), Math.max(sel.y + selH, oth.y + othH) };
                    break outerX;
                }
            }

            float[] selY = { sel.y, sel.y + selH / 2f, sel.y + selH, sel.y + selH, sel.y };
            float[] othY = { oth.y, oth.y + othH / 2f, oth.y + othH, oth.y - minGap, oth.y + othH + minGap };
            outerY:
            for (int i = 0; i < selY.length; i++) {
                float sy = selY[i], oy = othY[i];
                if (Math.abs(sy - oy) <= EPS) {
                    hGuide = new float[]{ oy, Math.min(sel.x, oth.x), Math.max(sel.x + selW, oth.x + othW) };
                    break outerY;
                }
            }
        }

        private void resolveOverlap(HudModule sel, float selW, float selH, HudModule oth, float othW, float othH) {
            float minGap = KeystrokesRenderer.GAP * Math.max(sel.scale, oth.scale);
            float selL = sel.x, selR = sel.x + selW, selT = sel.y, selB = sel.y + selH;
            float othL = oth.x, othR = oth.x + othW, othT = oth.y, othB = oth.y + othH;
            
            if (!(selL < othR + minGap && selR + minGap > othL && selT < othB + minGap && selB + minGap > othT)) return;

            float bestCost = Float.MAX_VALUE, bestX = sel.x, bestY = sel.y;
            float nx = othL - selW - minGap;
            float costL = selR - othL + minGap;
            if (nx >= 0 && costL < bestCost) { bestCost = costL; bestX = nx; bestY = sel.y; }
            nx = othR + minGap;
            float costR = othR - selL + minGap;
            if (nx + selW <= width && costR < bestCost) { bestCost = costR; bestX = nx; bestY = sel.y; }
            float ny = othT - selH - minGap;
            float costT = selB - othT + minGap;
            if (ny >= 0 && costT < bestCost) { bestCost = costT; bestX = sel.x; bestY = ny; }
            ny = othB + minGap;
            float costB = othB - selT + minGap;
            if (ny + selH <= height && costB < bestCost) { bestCost = costB; bestX = sel.x; bestY = ny; }

            sel.x = bestX;
            sel.y = bestY;
        }

        @Override public boolean mouseScrolled(double mx, double my, double ha, double va) {
            HudConfig cfg = HudConfig.get();
            if (panel.mouseScrolled(mx, my, va)) return true;
            if (selectedModule != null) {
                selectedModule.scale = Mth.clamp(selectedModule.scale + (float)va * 0.01f, 0.4f, 4f);
                selectedModule.clamp(width, height, (selectedModule == cfg.keysModule ? KeystrokesRenderer.width() : CpsRenderer.width()), (selectedModule == cfg.keysModule ? KeystrokesRenderer.height() : CpsRenderer.height()));
                HudConfig.save(); return true;
            }
            return false;
        }

        @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
            panel.mouseReleased();
            if (event.button() == 0) { dragging = false; vGuide = null; hGuide = null; HudConfig.save(); }
            return super.mouseReleased(event);
        }
        @Override public boolean isPauseScreen() { return false; }

        @Override
        public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
            if (panel.open && panel.charTyped((char)event.codepoint())) return true;
            return super.charTyped(event);
        }

        @Override
        public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
            if (panel.open && panel.keyPressed(event.key())) return true;
            return super.keyPressed(event);
        }

        @Override
        public void onClose() {
            HudConfig.save();
            super.onClose();
        }

        @Override
        public void removed() {
            if (savedBlurriness != null)
                Minecraft.getInstance().options.menuBackgroundBlurriness().set(savedBlurriness);
            super.removed();
        }
    }
}
