package com.botato.autokeystrokes.mixin;

import net.minecraft.world.entity.LivingEntity;
import com.botato.autokeystrokes.client.HudConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {
    @Shadow private int noJumpDelay;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void removeJumpDelay(CallbackInfo info) {
        if (HudConfig.get().fastJump) {
            this.noJumpDelay = 0;
        }
    }
}
