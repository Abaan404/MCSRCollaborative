package com.abaan404.mcsrcollaborative;

import java.util.List;
import java.util.UUID;

import com.abaan404.mcsrcollaborative.events.PlayerTurns;
import com.abaan404.mcsrcollaborative.utils.TimeUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;

public class McsrCollaborativeCommands {

    private static int reload(CommandContext<CommandSourceStack> context) {
        if (!McsrCollaborative.CONFIG.load()) {
            context.getSource().sendFailure(Component.literal("Something went wrong..."));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal("Config reloaded!"), false);
        return 1;
    }

    private static int queueDump(CommandContext<CommandSourceStack> context) {
        List<NameAndId> players = McsrCollaborativeManager.INSTANCE.getPlayerQueue();

        CommandSourceStack source = context.getSource();

        if (players.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Queue is empty."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal(String.format("Queued players (%d):", players.size())), false);

        for (int i = 0; i < players.size(); i++) {
            NameAndId player = players.get(i);

            String str = String.format("%d) %s (%s)", i + 1, player.name(), player.id());
            source.sendSuccess(() -> Component.literal(str), false);
        }

        String durationStr = String.format("Duration: %s",
                TimeUtils.formatTime(McsrCollaborativeManager.INSTANCE.getDuration() * 50));
        source.sendSuccess(() -> Component.literal(durationStr), false);

        String timeoutStr = String.format("Timeout: %s",
                TimeUtils.formatTime(McsrCollaborativeManager.INSTANCE.getTimeout() * 50));
        source.sendSuccess(() -> Component.literal(timeoutStr), false);

        return 1;
    }

    private static int queueCycle(CommandContext<CommandSourceStack> context) {
        NameAndId player = McsrCollaborativeManager.INSTANCE.cycleNext(context.getSource().getServer());

        String str = String.format("Set current player to: %s (%s)", player.name(), player.id());
        context.getSource().sendSuccess(() -> Component.literal(str), false);

        return 1;
    }

    private static int queueAdd(CommandContext<CommandSourceStack> context) {
        NameAndId player = new NameAndId(
                UuidArgument.getUuid(context, "uuid"),
                StringArgumentType.getString(context, "name"));

        boolean ret = McsrCollaborativeManager.INSTANCE.addPlayer(context.getSource().getServer(), player);

        if (!ret) {
            context.getSource().sendFailure(Component.literal("Player was already added."));
            return 0;
        }

        String str = String.format("Added player: %s (%s)", player.name(), player.id());
        context.getSource().sendSuccess(() -> Component.literal(str), false);

        return 1;
    }

    private static int queueRemoveName(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");

        List<NameAndId> players = McsrCollaborativeManager.INSTANCE.getPlayerQueue();

        for (NameAndId player : players) {
            if (player.name().equals(name)) {
                McsrCollaborativeManager.INSTANCE.removePlayer(context.getSource().getServer(), player.id());

                String str = String.format("Removed player: %s (%s)", player.name(), player.id());
                context.getSource().sendSuccess(() -> Component.literal(str), false);

                return 1;
            }
        }

        context.getSource().sendFailure(Component.literal("Player was already removed or does not exist."));
        return 0;
    }

    private static int queueRemoveId(CommandContext<CommandSourceStack> context) {
        UUID id = UuidArgument.getUuid(context, "id");

        List<NameAndId> players = McsrCollaborativeManager.INSTANCE.getPlayerQueue();

        for (NameAndId player : players) {
            if (player.id().equals(id)) {
                McsrCollaborativeManager.INSTANCE.removePlayer(context.getSource().getServer(), player.id());

                String str = String.format("Removed player: %s (%s)", player.name(), player.id());
                context.getSource().sendSuccess(() -> Component.literal(str), false);

                return 1;
            }
        }

        context.getSource().sendFailure(Component.literal("Player was already removed or does not exist."));
        return 0;
    }

    private static int queueSetName(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");

        List<NameAndId> players = McsrCollaborativeManager.INSTANCE.getPlayerQueue();

        for (NameAndId player : players) {
            if (player.name().equals(name)) {
                boolean ret = McsrCollaborativeManager.INSTANCE.setPlayer(context.getSource().getServer(), player);

                if (!ret) {
                    context.getSource().sendFailure(Component.literal("Player was already set."));
                    return 0;
                }

                String str = String.format("Set player: %s (%s)", player.name(), player.id());
                context.getSource().sendSuccess(() -> Component.literal(str), false);

                return 1;
            }
        }

        context.getSource().sendFailure(Component.literal("Player does not exist."));
        return 0;
    }

    private static int queueSet(CommandContext<CommandSourceStack> context) {
        NameAndId player = new NameAndId(
                UuidArgument.getUuid(context, "uuid"),
                StringArgumentType.getString(context, "name"));

        boolean ret = McsrCollaborativeManager.INSTANCE.setPlayer(context.getSource().getServer(), player);

        if (!ret) {
            context.getSource().sendFailure(Component.literal("Player was already set."));
            return 0;
        }

        String str = String.format("Set player: %s (%s)", player.name(), player.id());
        context.getSource().sendSuccess(() -> Component.literal(str), false);

        return 1;
    }

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("mcsr-collaborative")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                    .then(Commands.literal("reload")
                            .executes(McsrCollaborativeCommands::reload))
                    .then(Commands.literal("queue")
                            .then(Commands.literal("dump")
                                    .executes(McsrCollaborativeCommands::queueDump))
                            .then(Commands.literal("cycle")
                                    .executes(McsrCollaborativeCommands::queueCycle))
                            .then(Commands.literal("add")
                                    .then(Commands.argument("name", StringArgumentType.string())
                                            .then(Commands.argument("uuid", UuidArgument.uuid())
                                                    .executes(McsrCollaborativeCommands::queueAdd))))
                            .then(Commands.literal("remove")
                                    .then(Commands.argument("name", StringArgumentType.string())
                                            .executes(McsrCollaborativeCommands::queueRemoveName))
                                    .then(Commands.argument("uuid", UuidArgument.uuid())
                                            .executes(McsrCollaborativeCommands::queueRemoveId)))
                            .then(Commands.literal("set")
                                    .then(Commands.argument("name", StringArgumentType.string())
                                            .executes(McsrCollaborativeCommands::queueSetName))
                                    .then(Commands.argument("name", StringArgumentType.string())
                                            .then(Commands.argument("uuid", UuidArgument.uuid())
                                                    .executes(McsrCollaborativeCommands::queueSet)))))
                    .then(Commands.literal("debug-events")
                            .then(Commands.literal("begin")
                                    .executes((context) -> {
                                        PlayerTurns.BEGIN.invoker().onTurnBegin(context.getSource().getPlayer());
                                        return 1;
                                    }))
                            .then(Commands.literal("end")
                                    .executes((context) -> {
                                        NameAndId next = McsrCollaborativeManager.INSTANCE
                                                .getCurrentPlayerNameAndId(context.getSource().getServer());

                                        PlayerTurns.END.invoker().onTurnEnd(context.getSource().getPlayer(), next);
                                        return 1;
                                    }))
                            .then(Commands.literal("pause")
                                    .executes((context) -> {
                                        PlayerTurns.PAUSE.invoker().onTurnPause(context.getSource().getPlayer());
                                        return 1;
                                    }))
                            .then(Commands.literal("resume")
                                    .executes((context) -> {
                                        PlayerTurns.RESUME.invoker().onTurnResume(context.getSource().getPlayer());
                                        return 1;
                                    }))));
        });
    }
}
