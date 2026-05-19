package com.abaan404.mcsrcollaborative.utils;

import java.util.List;
import java.util.UUID;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.server.players.NameAndId;

public class PlayerQueue {
    private final Object2ObjectLinkedOpenHashMap<UUID, NameAndId> playerQueue = new Object2ObjectLinkedOpenHashMap<>();
    public static NameAndId DEFAULT = NameAndId.createOffline("Nobody");

    public PlayerQueue(List<NameAndId> players) {
        for (NameAndId player : players) {
            this.playerQueue.put(player.id(), player);
        }

        this.playerQueue.defaultReturnValue(DEFAULT);
    }

    /**
     * Get the current player at the head of the queue.
     *
     * @return The player's name and id.
     */
    public NameAndId getCurrentPlayer() {
        if (this.playerQueue.isEmpty()) {
            return this.playerQueue.defaultReturnValue();
        }

        return this.playerQueue.firstEntry().getValue();
    }

    /**
     * Add a player to the queue.
     *
     * @param player The player's name and id.
     * @return If it was successful.
     */
    public boolean addPlayer(NameAndId player) {
        if (this.playerQueue.containsKey(player.id())) {
            return false;
        }

        this.playerQueue.putAndMoveToLast(player.id(), player);

        return true;
    }

    /**
     * Remove a player from the queue.
     *
     * @param player The player's name and id.
     * @return If it was successful.
     */
    public boolean removePlayer(NameAndId player) {
        if (!this.playerQueue.containsKey(player.id())) {
            return false;
        }

        this.playerQueue.remove(player.id());

        return true;
    }

    /**
     * Get the current queue.
     *
     * @return A readonly view of the queue.
     */
    public List<NameAndId> getPlayers() {
        return this.playerQueue.values().stream()
                .toList();
    }

    /**
     * Cycle the current player to behind the queue
     *
     * @return The previous player.
     */
    public NameAndId cyclePlayers() {
        return this.playerQueue.getAndMoveToLast(this.playerQueue.firstKey());
    }

    /**
     * Set a player to the front of the queue. If they dont exist, add them.
     *
     * @param player The player's name and id.
     */
    public void setPlayer(NameAndId player) {
        this.playerQueue.putAndMoveToFirst(player.id(), player);
    }

    /**
     * Test if the player is in the queue.
     *
     * @param player The player's name and id.
     * @return If theyre in the queue.
     */
    public boolean hasPlayer(NameAndId player) {
        return this.playerQueue.containsKey(player.id());
    }

    public int getCountTillTurn(NameAndId player) {
        int idx = 0;

        for (UUID id : this.playerQueue.keySet()) {
            if (id.equals(player.id())) {
                return idx;
            }
            idx++;
        }

        return -1;
    }
}
