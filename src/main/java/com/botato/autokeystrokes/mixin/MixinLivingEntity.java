package com.botato.autokeystrokes.mixin;

import net.minecraft.entity.LivingEntity;
import com.botato.autokeystrokes.client.HudConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {
    @Shadow private int jumpingCooldown;

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void removeJumpDelay(CallbackInfo info) {
        if (HudConfig.get().fastJump) {
            this.jumpingCooldown = 0;
        }
    }
}
