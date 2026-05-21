package com.abaan404.mcsrcollaborative.processors;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.abaan404.mcsrcollaborative.McsrCollaborative;
import com.abaan404.mcsrcollaborative.McsrCollaborativeManager;
import com.abaan404.mcsrcollaborative.events.PlayerTurns;
import com.abaan404.mcsrcollaborative.utils.MemberService;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;

public class DiscordLink extends ListenerAdapter {
    private static DiscordLink INSTANCE = new DiscordLink();

    private JDA api = null;
    private MinecraftServer server = null;

    @Override
    public void onReady(ReadyEvent event) {
        this.api = event.getJDA();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            return;
        }

        switch (event.getName()) {
            case "signup":
                signup(event);
                break;
            case "signout":
                signout(event);
                break;
            default:
                event.reply("That command does not exist!")
                        .setEphemeral(true)
                        .queue();
        }
    }

    private void signup(SlashCommandInteractionEvent event) {
        if (server == null) {
            event.reply("Server is still starting!").queue();
            return;
        }

        event.deferReply(true).queue(hook -> {
            String id = event.getMember().getId();
            MemberService.getMemberByDiscordId(id).orTimeout(5, TimeUnit.SECONDS).thenAcceptAsync((member) -> {
                member.asNameAndId().ifPresentOrElse(nameAndId -> {
                    if (!McsrCollaborativeManager.INSTANCE.addPlayer(this.server, nameAndId)) {
                        hook.sendMessage("You are already participating!").queue();
                        return;
                    }

                    hook.sendMessage("You have been added!").queue();
                }, () -> {
                    hook.sendMessage("Please link your account first!").queue();
                });
            }).exceptionally(throwable -> {
                McsrCollaborative.LOGGER.error("Error: ", throwable);
                hook.sendMessage("An error occurred, please contact staff for help").queue();
                return null;
            });
        });
    }

    private void signout(SlashCommandInteractionEvent event) {
        if (server == null) {
            event.reply("Server is still starting!").queue();
            return;
        }

        event.deferReply(true).queue(hook -> {
            String id = event.getMember().getId();
            MemberService.getMemberByDiscordId(id).orTimeout(5, TimeUnit.SECONDS).thenAcceptAsync((member) -> {
                member.asNameAndId().ifPresentOrElse(nameAndId -> {
                    if (!McsrCollaborativeManager.INSTANCE.removePlayer(this.server, nameAndId.id())) {
                        hook.sendMessage("You are already not participating!").queue();
                        return;
                    }

                    hook.sendMessage("You have been removed!").queue();
                }, () -> {
                    hook.sendMessage("Please link your account first!").queue();
                });
            }).exceptionally(throwable -> {
                McsrCollaborative.LOGGER.error("Error: ", throwable);
                hook.sendMessage("An error occurred, please contact staff for help").queue();
                return null;
            });
        });
    }

    private void onPlayerEnd(MinecraftServer server, NameAndId player, NameAndId nextPlayer) {
        if (this.api == null) {
            return;
        }

        TextChannel channel = this.api.getChannelById(TextChannel.class, McsrCollaborative.CONFIG.getBotChannel());

        if (channel == null) {
            return;
        }

        CompletableFuture<MemberService.MemberInfo> memberFuture;
        if (nextPlayer.name().startsWith(".")) {
            memberFuture = MemberService.getMemberByBedrockId(String.valueOf(nextPlayer.id().getLeastSignificantBits()));
        } else {
            memberFuture = MemberService.getMemberByJavaId(nextPlayer.id().toString().replaceAll("-", ""));
        }
        memberFuture.orTimeout(5, TimeUnit.SECONDS).thenAccept((member) ->
                channel.getGuild().retrieveMemberById(member.id).queue(discordMember ->
                        channel.sendMessage(new MessageCreateBuilder()
                                        .setContent(McsrCollaborative.CONFIG.getBotMessage()
                                                .replace("%player%", discordMember.getAsMention()))
                                        .build())
                                .queue()));
    }

    public static void initialize() {
        EnumSet<GatewayIntent> intents = EnumSet.of(GatewayIntent.GUILD_MEMBERS);
        JDA jda = JDABuilder.createLight(McsrCollaborative.CONFIG.getBotToken(), intents)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .addEventListeners(INSTANCE)
                .build();

        CommandListUpdateAction commands = jda.updateCommands();

        commands.addCommands(
                Commands.slash("signup", "Sign up to the MCSR Collaborative event")
                        .setContexts(InteractionContextType.GUILD),
                Commands.slash("signout", "Sign out of the MCSR Collaborative event")
                        .setContexts(InteractionContextType.GUILD));

        commands.queue();

        PlayerTurns.END.register(INSTANCE::onPlayerEnd);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> INSTANCE.server = server);
    }
}
