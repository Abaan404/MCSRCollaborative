package com.abaan404.mcsrcollaborative.processors;

import java.util.EnumSet;

import com.abaan404.mcsrcollaborative.McsrCollaborative;
import com.abaan404.mcsrcollaborative.events.PlayerTurns;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;

public class DiscordLink extends ListenerAdapter {
    private static DiscordLink INSTANCE = new DiscordLink();

    private JDA api = null;

    @Override
    public void onReady(ReadyEvent event) {
        this.api = event.getJDA();
    }

    private void onPlayerEnd(MinecraftServer server, NameAndId player, NameAndId nextPlayer) {
        if (this.api == null) {
            return;
        }

        TextChannel channel = this.api.getChannelById(TextChannel.class, McsrCollaborative.CONFIG.getBotChannel());

        if (channel == null) {
            return;
        }

        channel.sendMessage(new MessageCreateBuilder()
                .setContent(McsrCollaborative.CONFIG.getBotMessage()
                        .replace("%player%", nextPlayer.name()))
                .build())
                .queue();
    }

    public static void initialize() {
        EnumSet<GatewayIntent> intents = EnumSet.noneOf(GatewayIntent.class);
        JDABuilder.createLight(McsrCollaborative.CONFIG.getBotToken(), intents)
                .addEventListeners(INSTANCE)
                .build();

        PlayerTurns.END.register(INSTANCE::onPlayerEnd);
    }
}
