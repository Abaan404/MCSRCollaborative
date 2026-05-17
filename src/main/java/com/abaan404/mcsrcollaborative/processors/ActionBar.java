package com.abaan404.mcsrcollaborative.processors;

import com.abaan404.mcsrcollaborative.events.PlayerTurns;
import com.abaan404.mcsrcollaborative.utils.TextUtils;

import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class ActionBar {
    public static ActionBar INSTANCE = new ActionBar();

    private void tick(ServerPlayer player, long duration) {
        MinecraftServer server = player.level().getServer();
        long inGameTimeMs = server.getLevel(ServerLevel.OVERWORLD).getGameTime() * 50;
        long durationMs = duration * 50;

        ClientboundSetActionBarTextPacket packet = new ClientboundSetActionBarTextPacket(
                TextUtils.actionBarDuration(inGameTimeMs, durationMs));

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.connection.send(packet);
        }
    }

    public static void initialize() {
        PlayerTurns.TICK.register(INSTANCE::tick);
    }
}
