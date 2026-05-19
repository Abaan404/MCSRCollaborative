package com.abaan404.mcsrcollaborative.processors;

import com.abaan404.mcsrcollaborative.McsrCollaborativeManager;
import com.abaan404.mcsrcollaborative.events.PlayerTurns;
import com.abaan404.mcsrcollaborative.utils.TextUtils;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class ActionBar {
    public static ActionBar INSTANCE = new ActionBar();

    private void tick(ServerPlayer player, long duration) {
        MinecraftServer server = player.level().getServer();
        long inGameTimeMs = McsrCollaborativeManager.INSTANCE.getInGameTime(server) * 50;
        long durationMs = duration * 50;

        ClientboundSetActionBarTextPacket packet = new ClientboundSetActionBarTextPacket(
                TextUtils.actionBarDuration(inGameTimeMs, durationMs));

        player.connection.send(packet);
    }

    private void tickServer(MinecraftServer server) {
        long inGameTimeMs = McsrCollaborativeManager.INSTANCE.getInGameTime(server) * 50;

        ClientboundSetActionBarTextPacket packet = new ClientboundSetActionBarTextPacket(
                TextUtils.actionBarDuration(inGameTimeMs));

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!McsrCollaborativeManager.INSTANCE.isPlayer(p)) {
                p.connection.send(packet);
            }
        }
    }

    public static void initialize() {
        PlayerTurns.TICK.register(INSTANCE::tick);

        ServerTickEvents.END_SERVER_TICK.register(INSTANCE::tickServer);
    }
}
