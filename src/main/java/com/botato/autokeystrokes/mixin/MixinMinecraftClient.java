package com.botato.autokeystrokes.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.HitResult;
import com.botato.autokeystrokes.client.HudConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {
    @Shadow private int itemUseCooldown;
    @Shadow private int attackCooldown;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (HudConfig.get().fastPlace) {
            this.itemUseCooldown = 0;
            if (HudConfig.get().leftClicking) {
                this.attackCooldown = 0;
            }
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void onDoItemUse(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (HudConfig.get().fastPlace && !HudConfig.get().airClicking) {
            if (client.crosshairTarget == null || client.crosshairTarget.getType() == HitResult.Type.MISS) {
                ci.cancel();
            }
        }
    }
}
