package com.abaan404.mcsrcollaborative.processors;

import com.abaan404.mcsrcollaborative.events.PlayerTurns;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class ServerPauser {
    public static ServerPauser INSTANCE = new ServerPauser();

    private void setPause(MinecraftServer server, boolean frozen) {
        // freeze immediately on startup
        server.tickRateManager().setFrozen(frozen);
    }

    private void pauseServer(ServerPlayer player) {
        this.setPause(player.level().getServer(), true);
    }

    private void resumeServer(ServerPlayer player) {
        this.setPause(player.level().getServer(), false);
    }

    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> INSTANCE.setPause(server, true));

        PlayerTurns.BEGIN.register(INSTANCE::resumeServer);
        PlayerTurns.PAUSE.register(INSTANCE::pauseServer);
        PlayerTurns.RESUME.register(INSTANCE::resumeServer);
        PlayerTurns.END.register((server, _, _) -> INSTANCE.setPause(server, true));
        PlayerTurns.FINALIZE.register(server -> INSTANCE.setPause(server, true));
    }
}
