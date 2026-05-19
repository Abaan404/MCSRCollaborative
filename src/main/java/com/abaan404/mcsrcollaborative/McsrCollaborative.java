package com.abaan404.mcsrcollaborative;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abaan404.mcsrcollaborative.config.Config;
import com.abaan404.mcsrcollaborative.processors.ActionBar;
import com.abaan404.mcsrcollaborative.processors.PlayerDataTransfer;
import com.abaan404.mcsrcollaborative.processors.PlayerRecorder;
import com.abaan404.mcsrcollaborative.processors.ServerMotd;
import com.abaan404.mcsrcollaborative.processors.ServerPauser;
import com.abaan404.mcsrcollaborative.queue.PlayerQueue;

public class McsrCollaborative implements ModInitializer {
	public static final String MOD_ID = "mcsr-collaborative";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Config CONFIG = new Config();

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // load configs before anything else
        CONFIG.load();

        PlayerQueue.initialize();

        ActionBar.initialize();
        PlayerDataTransfer.initialize();
        PlayerRecorder.initialize();
        ServerMotd.initialize();
        ServerPauser.initialize();
    }
}
