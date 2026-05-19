package com.abaan404.mcsrcollaborative;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.casual.arcade.replay.recorder.settings.RecorderSettings;
import net.casual.arcade.replay.recorder.settings.SimpleRecorderSettings;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.players.NameAndId;

public class McsrCollaborativeConfig {

    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("mcsr-collaborative.json");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private Primitive primitive = new Primitive();

    public void load() {
        if (!Files.exists(PATH)) {
            save();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
            Primitive loaded = GSON.fromJson(reader, Primitive.class);
            if (loaded != null) {
                this.primitive = loaded;
            }
        } catch (IOException e) {
            McsrCollaborative.LOGGER.error("Failed to load config at {}: {}", PATH, e.getMessage());
        }
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this.primitive, writer);
            }
        } catch (IOException e) {
            McsrCollaborative.LOGGER.error("Failed to save config at {}: {}", PATH, e.getMessage());
        }
    }

    public int getMaxDuration() {
        return this.primitive.maxDuration;
    }

    public List<NameAndId> getPlayers() {
        return Collections.unmodifiableList(this.primitive.players);
    }

    public RecorderSettings getRecorderSettings() {
        return this.primitive.recorderSettings;
    }

    public Path getRecorderDirectory() {
        return Path.of(this.primitive.path);
    }

    private static class Primitive {
        public String path = "./mcsr-recordings/";
        public int maxDuration = 5 * 60 * 1000;
        public List<NameAndId> players = new ObjectArrayList<>();
        public SimpleRecorderSettings recorderSettings = new SimpleRecorderSettings();
    }
}
