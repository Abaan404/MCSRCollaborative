package com.abaan404.mcsrcollaborative.saved_data;

import com.abaan404.mcsrcollaborative.McsrCollaborative;
import com.abaan404.mcsrcollaborative.utils.PlayerQueue;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class SavedCurrentPlayer extends SavedData {
    private NameAndId player = PlayerQueue.DEFAULT;
    private long duration = 0;
    private long timeout = 0;

    public static final Codec<SavedCurrentPlayer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            NameAndId.CODEC.fieldOf("name_and_id").forGetter(SavedCurrentPlayer::getPlayer),
            Codec.LONG.fieldOf("duration").forGetter(SavedCurrentPlayer::getDuration),
            Codec.LONG.fieldOf("timeout").forGetter(SavedCurrentPlayer::getTimeout))
            .apply(instance, SavedCurrentPlayer::new));

    private static final SavedDataType<SavedCurrentPlayer> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(McsrCollaborative.MOD_ID, "saved_queued_player"),
            SavedCurrentPlayer::new,
            CODEC,
            null);

    public SavedCurrentPlayer() {
    }

    public SavedCurrentPlayer(NameAndId nameAndId, long duration, long timeout) {
        this.player = nameAndId;
        this.duration = duration;
        this.timeout = timeout;
    }

    public NameAndId getPlayer() {
        return this.player;
    }

    public long getDuration() {
        return this.duration;
    }


    public long getTimeout() {
        return this.timeout;
    }

    public void setPlayer(NameAndId nameAndId) {
        this.player = nameAndId;
        setDirty();
    }

    public void setDuration(long duration) {
        this.duration = duration;
        setDirty();
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
        setDirty();
    }

    public static SavedCurrentPlayer getInstance(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }
}
