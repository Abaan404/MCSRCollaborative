package com.abaan404.mcsrcollaborative.processors;

import com.abaan404.mcsrcollaborative.events.PlayerTurns;
import com.abaan404.mcsrcollaborative.queue.PlayerQueue;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

public class ServerMotd {
    public static ServerMotd INSTANCE = new ServerMotd();

    private void updateMotd(MinecraftServer server, NameAndId player) {
        server.setMotd(String.format("Waiting for %s", player.name()));
    }

    private void onServerStarted(MinecraftServer server) {
        NameAndId player = PlayerQueue.INSTANCE.getPlayerNameAndId();
        this.updateMotd(server, player);
    }

    private void onPlayerTurnEnd(ServerPlayer player, NameAndId nextPlayer) {
        this.updateMotd(player.level().getServer(), nextPlayer);
    }

    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(INSTANCE::onServerStarted);

        PlayerTurns.END.register(INSTANCE::onPlayerTurnEnd);
    }
}
