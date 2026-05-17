package com.abaan404.mcsrcollaborative.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public abstract class TextUtils {
    private TextUtils() {
    }

    public static Component disconnectTurnInvalid(ServerPlayer player, int idx) {
        if (idx > 0) {
            return Component.literal(String.format("Not yet! You're waiting for %d player(s) to play.", idx));
        } else {
            return Component.literal("Not yet! Join the MCSR Collaborative event on the discord server to play.");
        }

    }

    public static Component disconnectTurnComplete() {
        return Component.literal("Thank you for playing! Check back soon for your next turn.");
    }

    public static Component actionBarDuration(long totalDuration, long duration) {
        return Component.empty()
                .append(Component.literal(String.format("IGT %s", TimeUtils.formatTime(totalDuration)))
                        .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD))
                .append(Component.literal(" | "))
                .append(Component.literal(String.format("Time Left %s", TimeUtils.formatTime(duration)))
                        .withStyle(ChatFormatting.GREEN));
    }
}
