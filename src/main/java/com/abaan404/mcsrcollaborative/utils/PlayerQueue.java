package com.abaan404.mcsrcollaborative.utils;

import java.util.List;
import java.util.UUID;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.server.players.NameAndId;

public class PlayerQueue {
    private final Object2ObjectLinkedOpenHashMap<UUID, NameAndId> playerQueue = new Object2ObjectLinkedOpenHashMap<>();
    public static final NameAndId DEFAULT = NameAndId.createOffline("Nobody");

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
        if (this.playerQueue.isEmpty()) {
            return this.playerQueue.defaultReturnValue();
        }

        return this.playerQueue.getAndMoveToLast(this.playerQueue.firstKey());
    }

    /**
     * Cycles till the player is at the front of the queue. If they dont exist, add
     * them.
     *
     * @param player The player's name and id.
     * @return if the player updated
     */
    public boolean setPlayer(NameAndId player) {
        if (this.isCurrentPlayer(player)) {
            return false;
        }

        if (this.hasPlayer(player)) {
            while (!this.isCurrentPlayer(player)) {
                this.cyclePlayers();
            }
        } else {
            this.playerQueue.putAndMoveToFirst(player.id(), player);
        }

        return true;
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

    /**
     * Test if the player is the current player.
     *
     * @param player The player's name and id.
     * @return If theyre currently in front.
     */
    public boolean isCurrentPlayer(NameAndId player) {
        return this.getCurrentPlayer().id().equals(player.id());
    }

    /**
     * Get the number of turns till its this player's turn, -1 if not in the queue.
     *
     * @param player The player.
     * @return Their turn count.
     */
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
