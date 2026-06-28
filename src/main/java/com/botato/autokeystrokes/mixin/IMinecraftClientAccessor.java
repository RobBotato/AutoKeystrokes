package com.botato.autokeystrokes.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface IMinecraftClientAccessor {

    @Accessor("missTime")
    void setAttackCooldown(int cooldown);

    @Invoker("startAttack")
    boolean invokeDoAttack();
}