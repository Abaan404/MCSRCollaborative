package com.abaan404.mcsrcollaborative.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.abaan404.mcsrcollaborative.events.EndCreditEvent;

import net.minecraft.server.level.ServerPlayer;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Inject(method = "showEndCredits", at = @At("RETURN"))
    public void showEndCredits(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        EndCreditEvent.EVENT.invoker().onShowEndCredits(player);
    }
}
