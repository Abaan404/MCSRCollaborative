package com.abaan404.mcsrcollaborative;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.abaan404.mcsrcollaborative.events.EndCreditEvent;
import com.abaan404.mcsrcollaborative.events.PlayerTurns;
import com.abaan404.mcsrcollaborative.saved_data.SavedCompletionIGT;
import com.abaan404.mcsrcollaborative.saved_data.SavedCurrentPlayer;
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

    private final List<NameAndId> playerQueue;

    private int playerQueueIdx = 0;
    private long duration = 0;
    private long timeout = 0;

    public McsrCollaborativeManager(List<NameAndId> players) {
        this.playerQueue = players;
    }

    /**
     * Peek the active player's uuid.
     *
     * @param server The server
     * @return The player's uuid.
     */
    public Optional<NameAndId> getCurrentPlayerNameAndId(MinecraftServer server) {
        if (this.playerQueue.isEmpty()) {
            return Optional.empty();
        }

        if (this.isEnded(server)) {
            return Optional.empty();
        }

        return Optional.of(this.playerQueue.get(this.playerQueueIdx));
    }

    /**
     * Peek the active player's uuid.
     *
     * @param server The server
     * @return The player's uuid.
     */
    public Optional<ServerPlayer> getCurrentPlayer(MinecraftServer server) {
        return this.getCurrentPlayerNameAndId(server)
                .flatMap(p -> Optional.ofNullable(server.getPlayerList().getPlayer(p.id())));
    }

    /**
     * Test if this is the active player to join.
     *
     * @param player The player.
     * @return If it's their turn.
     */
    public boolean isPlayer(ServerPlayer player) {
        UUID id = player.nameAndId().id();
        Optional<NameAndId> nameAndId = this.getCurrentPlayerNameAndId(player.level().getServer());

        if (nameAndId.isEmpty()) {
            return false;
        }

        return nameAndId.get().id().equals(id);
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

    /**
     * Cycle to the next player.
     */
    public void cycleNext(MinecraftServer server) {
        // update index and timeout
        this.playerQueueIdx = (this.playerQueueIdx + 1) % this.playerQueue.size();
        this.timeout = McsrCollaborative.CONFIG.getTimeout() / 50;
    }

    private void onServerStart(MinecraftServer server) {
        // restore queue on server start
        SavedCurrentPlayer savedPlayer = SavedCurrentPlayer.getInstance(server);

        for (int i = 0; i < this.playerQueue.size(); i++) {
            UUID id = this.playerQueue.get(i).id();

            if (id.equals(savedPlayer.getPlayer().id())) {
                this.playerQueueIdx = i;
                this.timeout = savedPlayer.getTimeout();
                return;
            }
        }

        this.playerQueueIdx = 0;
        this.timeout = McsrCollaborative.CONFIG.getTimeout() / 50;
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

            int idx = this.playerQueue.stream()
                    .map(nameAndId -> nameAndId.id())
                    .toList()
                    .indexOf(player.nameAndId().id());

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
        if (this.timeout > 0) {
            this.timeout--;

            // player didnt join, go next
            if (this.timeout <= 0) {
                this.cycleNext(server);
            }
        }

        this.getCurrentPlayer(server).ifPresent(player -> {
            if (this.duration > 0) {
                this.duration--;

                PlayerTurns.TICK.invoker().onTurnTick(player, this.duration);

            } else {
                this.cycleNext(server);
                PlayerTurns.END.invoker().onTurnEnd(player, this.getCurrentPlayerNameAndId(server));

                // disconnect
                player.connection.disconnect(TextUtils.disconnectTurnEnded());

                // save to persistent storage
                SavedCurrentPlayer savedQueuedPlayer = SavedCurrentPlayer.getInstance(server);
                savedQueuedPlayer.setPlayer(player.nameAndId());
                savedQueuedPlayer.setTimeout(this.timeout);

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
