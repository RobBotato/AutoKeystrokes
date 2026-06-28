package com.botato.autokeystrokes.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import com.botato.autokeystrokes.client.HudConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftClient {
    @Shadow private int rightClickDelay;
    @Shadow protected int missTime;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (HudConfig.get().fastPlace) {
            this.rightClickDelay = 0;
            if (HudConfig.get().leftClicking) {
                this.missTime = 0;
            }
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void onStartUseItem(CallbackInfo ci) {
        Minecraft client = (Minecraft) (Object) this;
        if (HudConfig.get().fastPlace && !HudConfig.get().airClicking) {
            if (client.hitResult == null || client.hitResult.getType() == HitResult.Type.MISS) {
                ci.cancel();
            }
        }
    }
}
