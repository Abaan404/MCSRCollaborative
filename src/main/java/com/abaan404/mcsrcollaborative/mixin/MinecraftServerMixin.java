package com.abaan404.mcsrcollaborative.mixin;

import java.util.EnumSet;
import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.abaan404.mcsrcollaborative.McsrCollaborativeManager;
import com.abaan404.mcsrcollaborative.utils.TimeUtils;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @ModifyReturnValue(method = "buildServerStatus", at = @At("RETURN"))
    private ServerStatus buildServerStatus(ServerStatus original) {
        MinecraftServer server = (MinecraftServer) (Object) this;

        String playerName = McsrCollaborativeManager.INSTANCE.getCurrentPlayer(server).name();
        String inGameTime = TimeUtils.formatTime(McsrCollaborativeManager.INSTANCE.getInGameTime(server) * 50,
                EnumSet.complementOf(EnumSet.of(TimeUtils.Selector.HOURS)),
                EnumSet.complementOf(EnumSet.of(TimeUtils.Selector.MILLISECONDS)));

        MutableComponent newDescription = Component.empty();
        original.description().visit((style, contents) -> {
            String replaced = contents
                    .replace("%player%", playerName)
                    .replace("%time%", inGameTime);

            newDescription.append(Component.literal(replaced).withStyle(style));
            return Optional.empty();
        }, Style.EMPTY);

        return new ServerStatus(
                newDescription,
                original.players(),
                original.version(),
                original.favicon(),
                original.enforcesSecureChat());
    }
}
