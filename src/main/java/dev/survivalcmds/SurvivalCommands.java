package dev.survivalcmds;

import dev.survivalcmds.commands.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SurvivalCommands implements ModInitializer {

    public static final String MOD_ID = "survivalcommands";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("SurvivalCommands loaded!");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            BackCommand.register(dispatcher);
            SpawnCommand.register(dispatcher);
            HomeCommand.register(dispatcher);
            SetHomeCommand.register(dispatcher);
            FollowCommand.register(dispatcher);
            ExploreCommand.register(dispatcher);
            MenuCommand.register(dispatcher);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            HomeStorage.init(server);
            LOGGER.info("SurvivalCommands: Home storage initialized.");
        });

        // No tick listener needed anymore — follow is a one-time teleport
    }
}
