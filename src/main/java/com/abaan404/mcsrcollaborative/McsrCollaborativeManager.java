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
     * See {@link PlayerQueue#addPlayer(NameAndId)}
     *
     * @param player The player's name and id.
     * @return If successful.
     */
    public boolean addPlayer(NameAndId player) {
        return this.playerQueue.addPlayer(player);
    }

    /**
     * See {@link PlayerQueue#removePlayer(NameAndId)}
     *
     * @param player The player's name and id.
     * @return If successful.
     */
    public boolean removePlayer(NameAndId player) {
        return this.playerQueue.removePlayer(player);
    }

    /**
     * See {@link PlayerQueue#setPlayer(NameAndId)}
     *
     * @param player The player's name and id.
     */
    public void setPlayer(NameAndId player) {
        this.playerQueue.setPlayer(player);
    }

    /**
     * See {@link PlayerQueue#cyclePlayers()}. Resets internal counters.
     *
     * @param server The minecraft server
     */
    public void cycleNext(MinecraftServer server) {
        // update index and timeout
        this.playerQueue.cyclePlayers();
        this.duration = McsrCollaborative.CONFIG.getDuration() / 50;
        this.timeout = McsrCollaborative.CONFIG.getTimeout() / 50;

        // save to persistent storage
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
        this.playerQueue.getCurrentPlayer();

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
     * Test if this is the active player to join.
     *
     * @param player The player.
     * @return If it's their turn.
     */
    public boolean isPlayer(ServerPlayer player) {
        return this.getCurrentPlayerNameAndId(player.level().getServer())
                .id().equals(player.nameAndId().id());
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
        SavedCurrentPlayer savedPlayer = SavedCurrentPlayer.getInstance(server);

        UUID savedUuid = savedPlayer.getPlayer().id();

        // restore queue on server start
        if (this.playerQueue.hasPlayer(savedPlayer.getPlayer())) {
            while (!this.playerQueue.getCurrentPlayer().id().equals(savedUuid)) {
                this.playerQueue.cyclePlayers();
            }

            this.duration = savedPlayer.getDuration();
            this.timeout = savedPlayer.getTimeout();
        }

        // load defaults
        else {
            this.duration = McsrCollaborative.CONFIG.getDuration() / 50;
            this.timeout = McsrCollaborative.CONFIG.getTimeout() / 50;
        }
    }

    private void onGameEnd(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();

        if (!this.isEnded(server)) {
            // save igt
            SavedCompletionIGT savedIgt = SavedCompletionIGT.getInstance(server);
            savedIgt.setInGameTime(server.getLevel(ServerLevel.OVERWORLD).getGameTime());
        }

        // set player to spectator
        player.setGameMode(GameType.SPECTATOR);
    }

    private void onPlayerJoin(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();

        // invoke events
        if (this.isPlayer(player)) {
            if (this.duration > 0) {
                PlayerTurns.RESUME.invoker().onTurnResume(player);
            } else {
                this.duration = McsrCollaborative.CONFIG.getDuration() / 50;
                this.timeout = McsrCollaborative.CONFIG.getTimeout() / 50;
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
        if (this.isPlayer(player) && this.duration > 0) {
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
                this.cycleNext(server);
            }
        }

        this.getCurrentPlayer(server).ifPresent(player -> {
            if (this.duration > 0) {
                this.duration--;

                SavedCurrentPlayer savedQueuedPlayer = SavedCurrentPlayer.getInstance(server);
                savedQueuedPlayer.setDuration(this.duration);

                PlayerTurns.TICK.invoker().onTurnTick(player, this.duration);

            } else {
                this.cycleNext(server);

                // do not invoke END if there is no next player
                NameAndId nextNameAndId = this.getCurrentPlayerNameAndId(server);
                if (nextNameAndId != PlayerQueue.DEFAULT) {
                    PlayerTurns.END.invoker().onTurnEnd(player, this.getCurrentPlayerNameAndId(server));
                }

                // disconnect
                player.connection.disconnect(TextUtils.disconnectTurnEnded());

                // why are you here
                this.getCurrentPlayer(server).ifPresent(nextPlayer -> {
                    nextPlayer.connection.disconnect(Component.literal("get out"));
                });
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
