package dev.survivalcmds.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.survivalcmds.BackStorage;
import dev.survivalcmds.Msg;
import dev.survivalcmds.TeleportManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class BackCommand {

    private static final int WARMUP_TICKS = 100; // 5 seconds

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

        TeleportManager.beginWarmup(player);
        Msg.info(player, "§7Teleporting back in §e5§7 seconds. Don't move!");

        startWarmup(player, WARMUP_TICKS);
        return 1;
    }

    private static void startWarmup(ServerPlayerEntity player, int ticksLeft) {
        scheduleNextTick(player.getServer(), () -> tickWarmup(player, ticksLeft));
    }

    private static void tickWarmup(ServerPlayerEntity player, int ticksLeft) {
        if (player == null || !player.isAlive()) {
            TeleportManager.cancelWarmup(player);
            return;
        }

        if (TeleportManager.hasPlayerMoved(player)) {
            TeleportManager.cancelWarmup(player);
            Msg.err(player, "Teleport cancelled because you moved.");
            return;
        }

        if (ticksLeft <= 0) {
            if (BackStorage.teleportBack(player)) {
                Msg.ok(player, "Teleported back!");
            } else {
                Msg.err(player, "Failed to teleport back.");
            }
            TeleportManager.endWarmup(player);
            return;
        }

        if (ticksLeft % 20 == 0) {
            int seconds = ticksLeft / 20;
            Msg.info(player, "§7Teleporting in §e" + seconds + "§7 seconds...");
        }

        scheduleNextTick(player.getServer(), () -> tickWarmup(player, ticksLeft - 1));
    }

    private static void scheduleNextTick(MinecraftServer server, Runnable task) {
        server.execute(task);
    }
}
