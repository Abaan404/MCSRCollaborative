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

    private Root root = new Root();

    public boolean load() {
        if (!Files.exists(PATH)) {
            save();
        }

        try (BufferedReader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
            Root loaded = GSON.fromJson(reader, Root.class);
            if (loaded != null) {
                this.root = loaded;
            }
        } catch (IOException e) {
            McsrCollaborative.LOGGER.error("Failed to load config at {}: {}", PATH, e.getMessage());
            return false;
        }

        return true;
    }

    public boolean save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this.root, writer);
            }
        } catch (IOException e) {
            McsrCollaborative.LOGGER.error("Failed to save config at {}: {}", PATH, e.getMessage());
            return false;
        }

        return true;
    }

    public int getDuration() {
        return this.root.duration;
    }

    public long getTimeout() {
        return this.root.timeout;
    }

    public void setPlayers(List<NameAndId> players) {
        this.root.players = players;
        this.save();
    }

    public List<NameAndId> getPlayers() {
        return Collections.unmodifiableList(this.root.players);
    }

    public RecorderSettings getRecorderSettings() {
        return this.root.recorderSettings;
    }

    public Path getRecorderDirectory() {
        return Path.of(this.root.path);
    }

    public String getBotToken() {
        return this.root.bot.token;
    }

    public String getBotChannel() {
        return this.root.bot.channel;
    }

    public String getBotMessage() {
        return this.root.bot.message;
    }

    private static class Root {
        public String path = "./mcsr-recordings/";
        public int duration = 5 * 60 * 1000;
        public long timeout = 24 * 60 * 60 * 1000;
        public List<NameAndId> players = new ObjectArrayList<>();
        public SimpleRecorderSettings recorderSettings = new SimpleRecorderSettings();
        public Bot bot = new Bot();
    }

    private static class Bot {
        public String token = "<BOT_TOKEN>";
        public String channel = "<BOT_CHANNEL>";
        public String message = "It's %player%'s turn!";
    }
}
