package com.abaan404.mcsrcollaborative.saved_data;

import com.abaan404.mcsrcollaborative.McsrCollaborative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class SavedCurrentPlayer extends SavedData {
    private NameAndId player = NameAndId.createOffline("Mumbo");
    private long timeout = 0;

    public static final Codec<SavedCurrentPlayer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            NameAndId.CODEC.fieldOf("name_and_id").forGetter(SavedCurrentPlayer::getPlayer),
            Codec.LONG.fieldOf("time_of_day").forGetter(SavedCurrentPlayer::getTimeout))
            .apply(instance, SavedCurrentPlayer::new));

    private static final SavedDataType<SavedCurrentPlayer> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(McsrCollaborative.MOD_ID, "saved_queued_player"),
            SavedCurrentPlayer::new,
            CODEC,
            null);

    public SavedCurrentPlayer() {
    }

    public SavedCurrentPlayer(NameAndId nameAndId, long nextTimeout) {
        this.player = nameAndId;
        this.timeout = nextTimeout;
    }

    public NameAndId getPlayer() {
        return this.player;
    }

    public long getTimeout() {
        return this.timeout;
    }

    public void setPlayer(NameAndId nameAndId) {
        this.player = nameAndId;
        setDirty();
    }

    public void setTimeout(long nextTimeout) {
        this.timeout = nextTimeout;
        setDirty();
    }

    public static SavedCurrentPlayer getInstance(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }
}
