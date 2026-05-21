package com.abaan404.mcsrcollaborative.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public abstract class TextUtils {
    private TextUtils() {
    }

    public static Component disconnectTurnFinalized() {
        return Component.literal("Thank you for playing!");
    }

    public static Component disconnectTurnEnded() {
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

    public static Component actionBarDuration(long totalDuration) {
        return Component.empty()
                .append(Component.literal(String.format("IGT %s", TimeUtils.formatTime(totalDuration)))
                        .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
    }
}
