package com.abaan404.mcsrcollaborative.queue;

import java.util.UUID;

import com.abaan404.mcsrcollaborative.McsrCollaborative;
import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class SavedQueuedPlayer extends SavedData {
    private NameAndId nameAndId = new NameAndId(new UUID(0L, 0L), "Mumbo");

    private static final Codec<SavedQueuedPlayer> CODEC = NameAndId.CODEC.xmap(
            SavedQueuedPlayer::new,
            SavedQueuedPlayer::getNameAndId);

    private static final SavedDataType<SavedQueuedPlayer> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(McsrCollaborative.MOD_ID, "saved_queued_player"),
            SavedQueuedPlayer::new,
            CODEC,
            null);

    public SavedQueuedPlayer() {
    }

    public SavedQueuedPlayer(NameAndId uuid) {
        this.nameAndId = uuid;
    }

    public NameAndId getNameAndId() {
        return this.nameAndId;
    }

    public void setNameAndId(NameAndId uuid) {
        this.nameAndId = uuid;
        setDirty();
    }

    public static SavedQueuedPlayer getSavedQueuedPlayer(MinecraftServer server) {
        // This could be either the overworld or another dimension.
        ServerLevel level = server.getLevel(ServerLevel.OVERWORLD);

        if (level == null) {
            return new SavedQueuedPlayer();
        }

        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}
