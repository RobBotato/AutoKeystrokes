package com.botato.autokeystrokes.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface IMinecraftClientAccessor {

    @Accessor("attackCooldown")
    void setAttackCooldown(int cooldown);

    @Invoker("doAttack")
    boolean invokeDoAttack();
}