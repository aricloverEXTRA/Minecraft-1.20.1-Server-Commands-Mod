package dev.survivalcmds.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.survivalcmds.BackStorage;
import dev.survivalcmds.Msg;
import dev.survivalcmds.TeleportManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SpawnCommand {

    private static final int WARMUP_TICKS = 100;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("spawn")
                .executes(ctx -> execute(ctx.getSource()))
        );
    }

    private static int execute(ServerCommandSource source) {
        if (!source.isExecutedByPlayer()) return 0;

        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        BackStorage.save(player);

        ServerWorld overworld = source.getServer().getWorld(World.OVERWORLD);
        if (overworld == null) {
            Msg.err(player, "Could not find the overworld!");
            return 0;
        }

        BlockPos spawnPos = overworld.getSpawnPos();

        TeleportManager.beginWarmup(player);
        Msg.info(player, "§7Teleporting to spawn in §e5§7 seconds. Don't move!");

        startWarmup(player, overworld, spawnPos, WARMUP_TICKS);
        return 1;
    }

    private static void startWarmup(ServerPlayerEntity player, ServerWorld world, BlockPos pos, int ticksLeft) {
        scheduleNextTick(player.getServer(), () -> tickWarmup(player, world, pos, ticksLeft));
    }

    private static void tickWarmup(ServerPlayerEntity player, ServerWorld world, BlockPos pos, int ticksLeft) {
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
            player.teleport(world,
                    pos.getX() + 0.5,
                    pos.getY(),
                    pos.getZ() + 0.5,
                    player.getYaw(), player.getPitch());

            Msg.ok(player, "Teleported to world spawn!");
            TeleportManager.endWarmup(player);
            return;
        }

        if (ticksLeft % 20 == 0) {
            int seconds = ticksLeft / 20;
            Msg.info(player, "§7Teleporting in §e" + seconds + "§7 seconds...");
        }

        scheduleNextTick(player.getServer(), () -> tickWarmup(player, world, pos, ticksLeft - 1));
    }

    private static void scheduleNextTick(MinecraftServer server, Runnable task) {
        server.execute(task);
    }
}
