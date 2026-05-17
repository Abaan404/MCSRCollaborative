package com.abaan404.mcsrcollaborative.queue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.abaan404.mcsrcollaborative.McsrCollaborative;
import com.abaan404.mcsrcollaborative.events.PlayerTurns;
import com.abaan404.mcsrcollaborative.utils.TextUtils;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

public class PlayerQueue {
    public static PlayerQueue INSTANCE;

    private final List<NameAndId> playerQueue;

    private int playerQueueIdx = 0;
    private long duration = 0;

    public PlayerQueue(List<NameAndId> players) {
        this.playerQueue = players;
    }

    /**
     * Peek the active player's uuid.
     *
     * @return The player's uuid.
     */
    public NameAndId getPlayerNameAndId() {
        if (this.playerQueue.isEmpty()) {
            throw new IllegalStateException("PlayerQueue is empty. Is the config filled correctly?");
        }

        return this.playerQueue.get(this.playerQueueIdx);
    }

    /**
     * Peek the active player if they're online.
     *
     * @param server The minecraft server.
     * @return The server player.
     */
    public Optional<ServerPlayer> getPlayer(MinecraftServer server) {
        UUID id = this.getPlayerNameAndId().id();
        return Optional.ofNullable(server.getPlayerList().getPlayer(id));
    }

    /**
     * Set the active player uuid.
     *
     * @param playerId the player's uuid.
     * @return If the currently tracked uuid was updated.
     */
    public boolean setPlayerId(UUID playerId) {
        for (int i = 0; i < this.playerQueue.size(); i++) {
            UUID id = this.playerQueue.get(i).id();

            if (id.equals(playerId)) {
                this.playerQueueIdx = i;
                return true;
            }
        }

        return false;
    }

    /**
     * Test if this is the active player to join.
     *
     * @param player The player.
     * @return If it's their turn.
     */
    public boolean isPlayer(ServerPlayer player) {
        UUID id = player.nameAndId().id();
        return this.getPlayerNameAndId().id().equals(id);
    }

    private void onServerStart(MinecraftServer server) {
        // restore queue on server start
        NameAndId savedUuid = SavedQueuedPlayer.getSavedQueuedPlayer(server).getNameAndId();

        if (!this.setPlayerId(savedUuid.id())) {
            McsrCollaborative.LOGGER.info("Could not restore previous uuid, ignore if this is the first launch.");
        }
    }

    private void onPlayerJoin(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();

        if (this.isPlayer(player)) {
            if (this.duration > 0) {
                PlayerTurns.RESUME.invoker().onTurnResume(player);
            } else {
                this.duration = McsrCollaborative.CONFIG.getMaxDuration() / 50;
                PlayerTurns.BEGIN.invoker().onTurnBegin(player);
            }

        } else {
            // let ops join without further processing
            if (server.getPlayerList().isOp(player.nameAndId())) {
                return;
            }

            int idx = this.playerQueue.stream()
                    .map(nameAndId -> nameAndId.id())
                    .toList()
                    .indexOf(player.nameAndId().id());

            player.connection.disconnect(TextUtils.disconnectTurnInvalid(player, idx));
        }
    }

    private void onPlayerLeave(ServerPlayer player) {
        if (this.isPlayer(player) && this.duration > 0) {
            PlayerTurns.PAUSE.invoker().onTurnPause(player);
        }
    }

    private void tick(MinecraftServer server) {
        // only tick while the player is online
        Optional<ServerPlayer> player = this.getPlayer(server);

        player.ifPresent(p -> {
            if (this.duration > 0) {
                this.duration--;

                PlayerTurns.TICK.invoker().onTurnTick(p, this.duration);
            }

            // turn ended, cycle next player
            else {
                // cycle to next
                this.playerQueueIdx = (this.playerQueueIdx + 1) % this.playerQueue.size();
                PlayerTurns.END.invoker().onTurnEnd(p, this.getPlayerNameAndId());

                p.connection.disconnect(TextUtils.disconnectTurnComplete());

                // save to persistent storage
                SavedQueuedPlayer savedQueuedPlayer =  SavedQueuedPlayer.getSavedQueuedPlayer(server);
                savedQueuedPlayer.setNameAndId(this.getPlayerNameAndId());
            }
        });
    }

    public static void initialize() {
        INSTANCE = new PlayerQueue(McsrCollaborative.CONFIG.getPlayers());

        ServerPlayerEvents.JOIN.register(INSTANCE::onPlayerJoin);
        ServerPlayerEvents.LEAVE.register(INSTANCE::onPlayerLeave);

        ServerLifecycleEvents.SERVER_STARTED.register(INSTANCE::onServerStart);
        ServerTickEvents.START_SERVER_TICK.register(INSTANCE::tick);
    }
}
