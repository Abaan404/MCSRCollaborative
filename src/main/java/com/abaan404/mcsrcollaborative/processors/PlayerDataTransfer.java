package com.abaan404.mcsrcollaborative.processors;

import java.nio.file.Files;
import java.nio.file.Path;

import com.abaan404.mcsrcollaborative.McsrCollaborative;
import com.abaan404.mcsrcollaborative.McsrCollaborativeManager;
import com.abaan404.mcsrcollaborative.events.PlayerTurns;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueOutput;

public class PlayerDataTransfer {
    public static PlayerDataTransfer INSTANCE = new PlayerDataTransfer();

    private void onPlayerTurnEnd(ServerPlayer player, NameAndId nextPlayer) {
        if (McsrCollaborativeManager.INSTANCE.isEnded(player.level().getServer())) {
            return;
        }

        MinecraftServer server = player.level().getServer();
        Path playerDirPath = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);

        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(player.problemPath(),
                McsrCollaborative.LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, player.registryAccess());
            player.saveWithoutId(output);
            CompoundTag dataToStore = output.buildResult();

            Path tmpFile = Files.createTempFile(playerDirPath, nextPlayer.id() + "-", ".dat");
            NbtIo.writeCompressed(dataToStore, tmpFile);

            Path realFile = playerDirPath.resolve(nextPlayer.id() + ".dat");
            Path oldFile = playerDirPath.resolve(nextPlayer.id() + ".dat_old");
            Util.safeReplaceFile(realFile, tmpFile, oldFile);

        } catch (Exception var11) {
            McsrCollaborative.LOGGER.warn("Failed to save player data for {}", player.getPlainTextName());
        }
    }

    public static void initialize() {
        PlayerTurns.END.register(INSTANCE::onPlayerTurnEnd);
    }
}
