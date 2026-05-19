package com.abaan404.mcsrcollaborative.saved_data;

import com.abaan404.mcsrcollaborative.McsrCollaborative;
import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class SavedCompletionIGT extends SavedData {
    private long inGameTime = 0;

    private static final Codec<SavedCompletionIGT> CODEC = Codec.LONG.xmap(
            SavedCompletionIGT::new,
            SavedCompletionIGT::getInGameTime);

    private static final SavedDataType<SavedCompletionIGT> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(McsrCollaborative.MOD_ID, "saved_igt"),
            SavedCompletionIGT::new,
            CODEC,
            null);

    public SavedCompletionIGT() {
    }

    public SavedCompletionIGT(long inGameTime) {
        this.inGameTime = inGameTime;
    }

    public long getInGameTime() {
        return this.inGameTime;
    }

    public void setInGameTime(long inGameTime) {
        this.inGameTime = inGameTime;
        setDirty();
    }

    public static SavedCompletionIGT getInstance(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }
}
