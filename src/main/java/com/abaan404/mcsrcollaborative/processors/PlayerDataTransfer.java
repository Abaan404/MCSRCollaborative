package com.abaan404.mcsrcollaborative.processors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

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

    private void onPlayerEnd(MinecraftServer server, NameAndId player, NameAndId nextPlayer) {
        if (McsrCollaborativeManager.INSTANCE.isEnded(server)) {
            return;
        }

        Path playerDirPath = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);

        if (server.getPlayerList().getPlayer(nextPlayer.id()) != null) {
            McsrCollaborative.LOGGER.error("Next player online, Skipping transferring player save file");
            return;
        }

        Optional.ofNullable(server.getPlayerList().getPlayer(player.id()))
                .ifPresentOrElse(p -> {
                    ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(
                            p.problemPath(), McsrCollaborative.LOGGER);
                    try (reporter) {
                        TagValueOutput output = TagValueOutput.createWithContext(reporter, p.registryAccess());
                        p.saveWithoutId(output);
                        CompoundTag dataToStore = output.buildResult();

                        Path tmpFile = Files.createTempFile(playerDirPath, nextPlayer.id() + "-", ".dat");
                        NbtIo.writeCompressed(dataToStore, tmpFile);

                        Path realFile = playerDirPath.resolve(nextPlayer.id() + ".dat");
                        Path oldFile = playerDirPath.resolve(nextPlayer.id() + ".dat_old");
                        Util.safeReplaceFile(realFile, tmpFile, oldFile);

                    } catch (Exception var11) {
                        McsrCollaborative.LOGGER.warn("Failed to save player data for {}", player.name());
                    }
                }, () -> {
                    try {
                        Path prevFile = playerDirPath.resolve(player.id() + ".dat");

                        Path realFile = playerDirPath.resolve(nextPlayer.id() + ".dat");
                        Path oldFile = playerDirPath.resolve(nextPlayer.id() + ".dat_old");
                        Util.safeReplaceFile(realFile, prevFile, oldFile);
                    } catch (Exception var11) {
                        McsrCollaborative.LOGGER.warn("Failed to save player data for {}", player.name());
                    }
                });
    }

    public static void initialize() {
        PlayerTurns.END.register(INSTANCE::onPlayerEnd);
    }
}
