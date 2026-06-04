package com.abaan404.mcsrcollaborative.processors;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Optional;

import com.abaan404.mcsrcollaborative.McsrCollaborative;
import com.abaan404.mcsrcollaborative.McsrCollaborativeManager;
import com.abaan404.mcsrcollaborative.events.PlayerTurns;
import com.abaan404.mcsrcollaborative.mixin.PlayerAdvancementsAccessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.FileUtil;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueOutput;

public class PlayerDataTransfer {
    public static PlayerDataTransfer INSTANCE = new PlayerDataTransfer();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private void onPlayerEnd(MinecraftServer server, NameAndId player, NameAndId nextPlayer) {
        if (McsrCollaborativeManager.INSTANCE.isEnded(server)) {
            return;
        }

        if (server.getPlayerList().getPlayer(nextPlayer.id()) != null) {
            McsrCollaborative.LOGGER.error("Next player online, Skipping transferring player save file");
            return;
        }

        PlayerList playerList = server.getPlayerList();

        // data
        Path playerDataPath = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);

        Optional.ofNullable(playerList.getPlayer(player.id()))
                .map(p -> {
                    ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(
                            p.problemPath(), McsrCollaborative.LOGGER);
                    try (reporter) {
                        TagValueOutput output = TagValueOutput.createWithContext(reporter, p.registryAccess());
                        p.saveWithoutId(output);
                        return output.buildResult();

                    } catch (Exception e) {
                        McsrCollaborative.LOGGER.warn("Failed to serialize player data for {}", p.getName());
                        return null;
                    }
                })
                .or(() -> {
                    try {
                        Path prevFile = playerDataPath.resolve(player.id() + ".dat");
                        return Optional.of(NbtIo.readCompressed(prevFile, NbtAccounter.unlimitedHeap()));
                    } catch (Exception e) {
                        McsrCollaborative.LOGGER.warn("Failed to read player data for {}", player.name());
                        return Optional.empty();
                    }
                })
                .ifPresent(data -> {
                    try {
                        Path tmpFile = Files.createTempFile(playerDataPath, nextPlayer.id() + "-", ".dat");
                        NbtIo.writeCompressed(data, tmpFile);

                        Path realFile = playerDataPath.resolve(nextPlayer.id() + ".dat");
                        Path oldFile = playerDataPath.resolve(nextPlayer.id() + ".dat_old");
                        Util.safeReplaceFile(realFile, tmpFile, oldFile);
                    } catch (Exception e) {
                        McsrCollaborative.LOGGER.warn("Failed to save player data for {}", nextPlayer.name());
                    }
                });

        // advancements
        DataFixer dataFixer = server.getFixerUpper();
        Codec<PlayerAdvancements.Data> codec = DataFixTypes.ADVANCEMENTS.wrapCodec(
                PlayerAdvancements.Data.CODEC,
                dataFixer,
                1343);

        Path playerAdvancementsPath = server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR);

        Optional.ofNullable(playerList.getPlayer(player.id()))
                .map(p -> ((PlayerAdvancementsAccessor) playerList.getPlayerAdvancements(p)).callAsData())
                .or(() -> {
                    Path playerSavePath = playerAdvancementsPath.resolve(player.id() + ".json");

                    if (!Files.isRegularFile(playerSavePath, new LinkOption[0])) {
                        return Optional.empty();
                    }

                    try {
                        Reader reader = Files.newBufferedReader(playerSavePath, StandardCharsets.UTF_8);

                        try {
                            JsonElement json = StrictJsonParser.parse(reader);
                            PlayerAdvancements.Data data = (PlayerAdvancements.Data) codec
                                    .parse(JsonOps.INSTANCE, json)
                                    .getOrThrow(JsonParseException::new);

                            return Optional.of(data);
                        } catch (Throwable var6) {
                            if (reader != null) {
                                try {
                                    reader.close();
                                } catch (Throwable var5) {
                                    var6.addSuppressed(var5);
                                }
                            }

                            throw var6;
                        }

                    } catch (JsonIOException | IOException var7) {
                        McsrCollaborative.LOGGER.error("Couldn't access player advancements in {}", playerSavePath,
                                var7);
                    } catch (JsonParseException var8) {
                        McsrCollaborative.LOGGER.error("Couldn't parse player advancements in {}", playerSavePath,
                                var8);
                    }

                    return Optional.empty();
                })
                .ifPresent(data -> {
                    JsonElement json = codec.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
                    Path playerSavePath = playerAdvancementsPath.resolve(nextPlayer.id() + ".json");

                    try {
                        FileUtil.createDirectoriesSafe(playerSavePath.getParent());
                        Writer outputWriter = Files.newBufferedWriter(playerSavePath, StandardCharsets.UTF_8);

                        try {
                            GSON.toJson(json, GSON.newJsonWriter(outputWriter));
                        } catch (Throwable var6) {
                            if (outputWriter != null) {
                                try {
                                    outputWriter.close();
                                } catch (Throwable var5) {
                                    var6.addSuppressed(var5);
                                }
                            }

                            throw var6;
                        }

                        if (outputWriter != null) {
                            outputWriter.close();
                        }
                    } catch (JsonIOException | IOException var7) {
                        McsrCollaborative.LOGGER.error("Couldn't save player advancements to {}", playerSavePath, var7);
                    }
                });
    }

    public static void initialize() {
        PlayerTurns.END.register(INSTANCE::onPlayerEnd);
    }
}
