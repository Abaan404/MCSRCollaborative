package com.abaan404.mcsrcollaborative.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

public interface EndCreditEvent {
    Event<EndCreditEvent> EVENT = EventFactory.createArrayBacked(EndCreditEvent.class,
            (listeners) -> (player) -> {
                for (EndCreditEvent listener : listeners) {
                    listener.onShowEndCredits(player);
                }
            });

    void onShowEndCredits(ServerPlayer player);
}
