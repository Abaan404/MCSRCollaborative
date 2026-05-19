package com.abaan404.mcsrcollaborative.saved_data;

import com.abaan404.mcsrcollaborative.McsrCollaborative;
import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class SavedCurrentPlayer extends SavedData {
    private NameAndId player = NameAndId.createOffline("Mumbo");

    private static final Codec<SavedCurrentPlayer> CODEC = NameAndId.CODEC.xmap(
            SavedCurrentPlayer::new,
            SavedCurrentPlayer::getPlayer);

    private static final SavedDataType<SavedCurrentPlayer> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(McsrCollaborative.MOD_ID, "saved_queued_player"),
            SavedCurrentPlayer::new,
            CODEC,
            null);

    public SavedCurrentPlayer() {
    }

    public SavedCurrentPlayer(NameAndId nameAndId) {
        this.player = nameAndId;
    }

    public NameAndId getPlayer() {
        return this.player;
    }

    public void setPlayer(NameAndId nameAndId) {
        this.player = nameAndId;
        setDirty();
    }

    public static SavedCurrentPlayer getInstance(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }
}
