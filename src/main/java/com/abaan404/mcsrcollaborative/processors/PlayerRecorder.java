package com.abaan404.mcsrcollaborative.processors;

import java.nio.file.Path;
import com.abaan404.mcsrcollaborative.McsrCollaborative;
import com.abaan404.mcsrcollaborative.McsrCollaborativeManager;
import com.abaan404.mcsrcollaborative.events.PlayerTurns;

import net.casual.arcade.replay.io.ReplayFormat;
import net.casual.arcade.replay.recorder.ReplayRecorder;
import net.casual.arcade.replay.recorder.ReplayRecorder.StartingMode;
import net.casual.arcade.replay.recorder.player.ReplayPlayerRecorders;
import net.casual.arcade.replay.recorder.settings.RecorderSettings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

public class PlayerRecorder {
    public static final PlayerRecorder INSTANCE = new PlayerRecorder();

    private static final ReplayFormat FORMAT = ReplayFormat.Flashback;

    private void onPlayerBegin(ServerPlayer player) {
        if (McsrCollaborativeManager.INSTANCE.isEnded(player.level().getServer())) {
            return;
        }

        RecorderSettings settings = McsrCollaborative.CONFIG.getRecorderSettings();
        Path directory = McsrCollaborative.CONFIG.getRecorderDirectory();

        if (!ReplayPlayerRecorders.has(player)) {
            ReplayPlayerRecorders.create(player, directory, FORMAT, settings)
                    .start(StartingMode.Start);
        }
    }

    private void onPlayerPause(ServerPlayer player) {
        ReplayPlayerRecorders.recorders().forEach(ReplayRecorder::pause);
    }

    private void onPlayerResume(ServerPlayer player) {
        ReplayPlayerRecorders.recorders().forEach(ReplayRecorder::resume);
    }

    private void onPlayerEnd(MinecraftServer server, NameAndId player, NameAndId nextPlayer) {
        ReplayPlayerRecorders.recorders().forEach(ReplayRecorder::pause);
    }

    private void onGameFinalize(MinecraftServer server) {
        ReplayPlayerRecorders.recorders().forEach(ReplayRecorder::stop);
    }

    public static void initialize() {
        PlayerTurns.BEGIN.register(INSTANCE::onPlayerBegin);
        PlayerTurns.PAUSE.register(INSTANCE::onPlayerPause);
        PlayerTurns.RESUME.register(INSTANCE::onPlayerResume);
        PlayerTurns.END.register(INSTANCE::onPlayerEnd);
        PlayerTurns.FINALIZE.register(INSTANCE::onGameFinalize);
    }
}
