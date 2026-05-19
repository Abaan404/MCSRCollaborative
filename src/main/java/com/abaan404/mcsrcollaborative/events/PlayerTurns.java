package com.abaan404.mcsrcollaborative.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

public interface PlayerTurns {
    Event<TurnBegin> BEGIN = EventFactory.createArrayBacked(TurnBegin.class,
            (listeners) -> (player) -> {
                for (TurnBegin listener : listeners) {
                    listener.onTurnBegin(player);
                }
            });

    Event<TurnDisconnect> PAUSE = EventFactory.createArrayBacked(TurnDisconnect.class,
            (listeners) -> (player) -> {
                for (TurnDisconnect listener : listeners) {
                    listener.onTurnPause(player);
                }
            });

    Event<TurnReconnect> RESUME = EventFactory.createArrayBacked(TurnReconnect.class,
            (listeners) -> (player) -> {
                for (TurnReconnect listener : listeners) {
                    listener.onTurnResume(player);
                }
            });

    Event<TurnEnd> END = EventFactory.createArrayBacked(TurnEnd.class,
            (listeners) -> (player, nextPlayer) -> {
                for (TurnEnd listener : listeners) {
                    listener.onTurnEnd(player, nextPlayer);
                }
            });

    Event<TurnTick> TICK = EventFactory.createArrayBacked(TurnTick.class,
            (listeners) -> (player, duration) -> {
                for (TurnTick listener : listeners) {
                    listener.onTurnTick(player, duration);
                }
            });

    @FunctionalInterface
    interface TurnBegin {
        void onTurnBegin(ServerPlayer player);
    }

    @FunctionalInterface
    interface TurnDisconnect {
        void onTurnPause(ServerPlayer player);
    }

    @FunctionalInterface
    interface TurnReconnect {
        void onTurnResume(ServerPlayer player);
    }

    @FunctionalInterface
    interface TurnEnd {
        void onTurnEnd(ServerPlayer player, NameAndId nextPlayer);
    }

    @FunctionalInterface
    interface TurnTick {
        void onTurnTick(ServerPlayer player, long duration);
    }
}
