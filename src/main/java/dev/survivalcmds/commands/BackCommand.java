package dev.survivalcmds.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.survivalcmds.BackStorage;
import dev.survivalcmds.Msg;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class BackCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("back")
                .executes(ctx -> execute(ctx.getSource()))
        );
    }

    private static int execute(ServerCommandSource source) {
        if (!source.isExecutedByPlayer()) return 0;
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        if (!BackStorage.hasBack(player.getUuid())) {
            Msg.err(player, "You have no back position saved!");
            return 0;
        }

        if (BackStorage.teleportBack(player)) {
            Msg.ok(player, "Teleported back!");
            return 1;
        }

        return 0;
    }
}
