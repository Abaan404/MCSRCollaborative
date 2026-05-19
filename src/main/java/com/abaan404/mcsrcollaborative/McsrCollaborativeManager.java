package com.abaan404.mcsrcollaborative;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.abaan404.mcsrcollaborative.events.EndCreditEvent;
import com.abaan404.mcsrcollaborative.events.PlayerTurns;
import com.abaan404.mcsrcollaborative.saved_data.SavedCompletionIGT;
import com.abaan404.mcsrcollaborative.saved_data.SavedCurrentPlayer;
import com.abaan404.mcsrcollaborative.utils.PlayerQueue;
import com.abaan404.mcsrcollaborative.utils.TextUtils;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.GameType;

public class McsrCollaborativeManager {
    public static McsrCollaborativeManager INSTANCE;

    private final PlayerQueue playerQueue;

    // private int playerQueueIdx = 0;
    private long duration = 0;
    private long timeout = 0;

    public McsrCollaborativeManager(List<NameAndId> players) {
        this.playerQueue = new PlayerQueue(players);
    }

    /**
     * See {@link PlayerQueue#getPlayers()}
     *
     * @return Te current queue
     */
    public List<NameAndId> getPlayerQueue() {
        return this.playerQueue.getPlayers();
    }

    /**
     * See {@link PlayerQueue#addPlayer(NameAndId)}. Resets internals if the current
     * player was updated.
     *
     * @param player The player's name and id.
     * @return If successful.
     */
    public boolean addPlayer(MinecraftServer server, NameAndId player) {
        NameAndId oldPlayer = this.playerQueue.getCurrentPlayer();
        boolean ret = this.playerQueue.addPlayer(player);

        // update config
        if (ret) {
            McsrCollaborative.CONFIG.setPlayers(this.getPlayerQueue());
        }

        // reset timer if current player changed
        if (!this.playerQueue.isCurrentPlayer(oldPlayer)) {
            this.duration = McsrCollaborative.CONFIG.getDuration() / 50;
            this.timeout = McsrCollaborative.CONFIG.getTimeout() / 50;

            // update storage
            SavedCurrentPlayer savedQueuedPlayer = SavedCurrentPlayer.getInstance(server);
            savedQueuedPlayer.setPlayer(this.playerQueue.getCurrentPlayer());
            savedQueuedPlayer.setTimeout(this.timeout);
            savedQueuedPlayer.setDuration(this.duration);
        }

        return ret;
    }

    /**
     * See {@link PlayerQueue#removePlayer(NameAndId)}. Resets internals if the
     * current player was updated.
     *
     * @param server The minecraft server.
     * @param player The player's name and id.
     * @return If successful.
     */
    public boolean removePlayer(MinecraftServer server, NameAndId player) {
        NameAndId oldPlayer = this.playerQueue.getCurrentPlayer();
        boolean ret = this.playerQueue.removePlayer(player);

        // update config
        if (ret) {
            McsrCollaborative.CONFIG.setPlayers(this.getPlayerQueue());
        }

        // reset timer if current player changed
        if (!this.playerQueue.isCurrentPlayer(oldPlayer)) {
            this.duration = McsrCollaborative.CONFIG.getDuration() / 50;
            this.timeout = McsrCollaborative.CONFIG.getTimeout() / 50;

            // update storage
            SavedCurrentPlayer savedQueuedPlayer = SavedCurrentPlayer.getInstance(server);
            savedQueuedPlayer.setPlayer(this.playerQueue.getCurrentPlayer());
            savedQueuedPlayer.setTimeout(this.timeout);
            savedQueuedPlayer.setDuration(this.duration);
        }

        return ret;
    }

    /**
     * See {@link PlayerQueue#setPlayer(NameAndId)}. Resets internals if the
     * current player was updated.
     *
     * @param player The player's name and id.
     */
    public boolean setPlayer(MinecraftServer server, NameAndId player) {
        // if added
        if (this.playerQueue.setPlayer(player)) {
            // update config
            McsrCollaborative.CONFIG.setPlayers(this.getPlayerQueue());

            // reset timer
            this.duration = McsrCollaborative.CONFIG.getDuration() / 50;
            this.timeout = McsrCollaborative.CONFIG.getTimeout() / 50;

            // update storage
            SavedCurrentPlayer savedQueuedPlayer = SavedCurrentPlayer.getInstance(server);
            savedQueuedPlayer.setPlayer(this.getCurrentPlayerNameAndId(server));
            savedQueuedPlayer.setTimeout(this.timeout);
            savedQueuedPlayer.setDuration(this.duration);
            return true;
        }

        return false;
    }

    /**
     * See {@link PlayerQueue#cyclePlayers()}. Resets internal counters and saves to
     * storage
     *
     * @param server The minecraft server
     */
    public void cycleNext(NameAndId player, MinecraftServer server) {
        Optional<ServerPlayer> player2 = this.getCurrentPlayer(server);

        this.playerQueue.cyclePlayers();

        // if online
        player2.ifPresent(p -> {
            // do not invoke END if there is no next player
            NameAndId nextNameAndId = this.getCurrentPlayerNameAndId(server);
            if (this.playerQueue.hasPlayer(nextNameAndId)) {
                PlayerTurns.END.invoker().onTurnEnd(p, nextNameAndId);
            }

            // disconnect
            p.connection.disconnect(TextUtils.disconnectTurnEnded());
        });

        // why are you here
        this.getCurrentPlayer(server).ifPresent(nextPlayer -> {
            nextPlayer.connection.disconnect(Component.literal("get out"));
        });

        // reset timer
        this.duration = McsrCollaborative.CONFIG.getDuration() / 50;
        this.timeout = McsrCollaborative.CONFIG.getTimeout() / 50;

        // update storage
        SavedCurrentPlayer savedQueuedPlayer = SavedCurrentPlayer.getInstance(server);
        savedQueuedPlayer.setPlayer(this.getCurrentPlayerNameAndId(server));
        savedQueuedPlayer.setDuration(this.duration);
        savedQueuedPlayer.setTimeout(this.timeout);
    }

    /**
     * Get the current player's uuid.
     *
     * @param server The server
     * @return The player's uuid.
     */
    public NameAndId getCurrentPlayerNameAndId(MinecraftServer server) {
        if (this.isEnded(server)) {
            return PlayerQueue.DEFAULT;
        }

        return this.playerQueue.getCurrentPlayer();
    }

    /**
     * Get the current player if online.
     *
     * @param server The server
     * @return The player's uuid.
     */
    public Optional<ServerPlayer> getCurrentPlayer(MinecraftServer server) {
        UUID uuid = this.getCurrentPlayerNameAndId(server).id();

        return Optional.ofNullable(server.getPlayerList().getPlayer(uuid));
    }

    /**
     * Test if this is the current player to join.
     *
     * @param player The player.
     * @return If it's their turn.
     */
    public boolean isCurrentPlayer(ServerPlayer player) {
        return this.getCurrentPlayerNameAndId(player.level().getServer()).id().equals(player.nameAndId().id());
    }

    /**
     * Get the current speedrun timer.
     *
     * @param server The server
     * @return the server igt
     */
    public long getInGameTime(MinecraftServer server) {
        if (!this.isEnded(server)) {
            return server.getLevel(ServerLevel.OVERWORLD).getGameTime();
        }

        SavedCompletionIGT savedIgt = SavedCompletionIGT.getInstance(server);
        return savedIgt.getInGameTime();
    }

    /**
     * Has the game ended.
     *
     * @param server the minecraft server.
     * @return If there was a stored in game time.
     */
    public boolean isEnded(MinecraftServer server) {
        SavedCompletionIGT savedIgt = SavedCompletionIGT.getInstance(server);
        return savedIgt.getInGameTime() > 0;
    }

    private void onServerStart(MinecraftServer server) {
        SavedCurrentPlayer savedQueuedPlayer = SavedCurrentPlayer.getInstance(server);

        // fresh instance
        if (!this.playerQueue.hasPlayer(savedQueuedPlayer.getPlayer())) {
            this.duration = McsrCollaborative.CONFIG.getDuration() / 50;
            this.timeout = McsrCollaborative.CONFIG.getTimeout() / 50;

            savedQueuedPlayer.setPlayer(this.getCurrentPlayerNameAndId(server));
            savedQueuedPlayer.setDuration(this.duration);
            savedQueuedPlayer.setTimeout(this.timeout);
            return;
        }

        // needs to cycle
        if (!this.playerQueue.isCurrentPlayer(savedQueuedPlayer.getPlayer())) {
            this.playerQueue.setPlayer(savedQueuedPlayer.getPlayer());
        }

        // restore state
        this.duration = savedQueuedPlayer.getDuration();
        this.timeout = savedQueuedPlayer.getTimeout();
    }

    private void onGameEnd(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();

        if (!this.isEnded(server)) {
            // save igt, sets as ended
            SavedCompletionIGT savedIgt = SavedCompletionIGT.getInstance(server);
            savedIgt.setInGameTime(server.getLevel(ServerLevel.OVERWORLD).getGameTime());
        }

        // set player to spectator
        player.setGameMode(GameType.SPECTATOR);
    }

    private void onPlayerJoin(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();

        // invoke events
        if (this.isCurrentPlayer(player)) {
            if (this.duration > 0) {
                PlayerTurns.RESUME.invoker().onTurnResume(player);
            } else {
                PlayerTurns.BEGIN.invoker().onTurnBegin(player);
            }

        } else {
            // let ops join without further processing
            if (server.getPlayerList().isOp(player.nameAndId())) {
                return;
            }

            // event ended, accept anyone as spectator
            if (this.isEnded(server)) {
                player.setGameMode(GameType.SPECTATOR);
                return;
            }

            // disconnect
            int idx = this.playerQueue.getCountTillTurn(player.nameAndId());
            player.connection.disconnect(TextUtils.disconnectTurnInvalid(player, idx));
        }
    }

    private void onPlayerLeave(ServerPlayer player) {
        // invoke events
        if (this.isCurrentPlayer(player) && this.duration > 0) {
            PlayerTurns.PAUSE.invoker().onTurnPause(player);
        }
    }

    private void tick(MinecraftServer server) {
        if (this.timeout > 0 && !this.isEnded(server)) {
            this.timeout--;

            SavedCurrentPlayer savedQueuedPlayer = SavedCurrentPlayer.getInstance(server);
            savedQueuedPlayer.setTimeout(this.timeout);

            // player didnt join, go next
            if (this.timeout <= 0) {
                this.cycleNext(this.getCurrentPlayerNameAndId(server), server);
                return;
            }
        }

        this.getCurrentPlayer(server).ifPresent(player -> {
            if (this.duration > 0) {
                this.duration--;

                SavedCurrentPlayer savedQueuedPlayer = SavedCurrentPlayer.getInstance(server);
                savedQueuedPlayer.setDuration(this.duration);

                PlayerTurns.TICK.invoker().onTurnTick(player, this.duration);

            } else {
                this.cycleNext(this.getCurrentPlayerNameAndId(server), server);
            }
        });
    }

    public static void initialize() {
        INSTANCE = new McsrCollaborativeManager(McsrCollaborative.CONFIG.getPlayers());

        ServerPlayerEvents.JOIN.register(INSTANCE::onPlayerJoin);
        ServerPlayerEvents.LEAVE.register(INSTANCE::onPlayerLeave);

        EndCreditEvent.EVENT.register(INSTANCE::onGameEnd);

        ServerLifecycleEvents.SERVER_STARTED.register(INSTANCE::onServerStart);
        ServerTickEvents.START_SERVER_TICK.register(INSTANCE::tick);
    }
}
