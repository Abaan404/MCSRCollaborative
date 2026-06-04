package com.abaan404.mcsrcollaborative.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.server.PlayerAdvancements;

@Mixin(PlayerAdvancements.class)
public interface PlayerAdvancementsAccessor {
    @Invoker
    PlayerAdvancements.Data callAsData();
}
