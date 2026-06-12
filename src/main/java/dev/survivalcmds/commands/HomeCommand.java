package dev.survivalcmds.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.survivalcmds.BackStorage;
import dev.survivalcmds.HomeStorage;
import dev.survivalcmds.Msg;
import dev.survivalcmds.TeleportManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public class HomeCommand {

    private static final int WARMUP_TICKS = 100;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("home")
                .executes(ctx -> execute(ctx.getSource()))
        );
    }

    private static int execute(ServerCommandSource source) {
        if (!source.isExecutedByPlayer()) return 0;

        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        if (!HomeStorage.hasHome(player.getUuid())) {
            Msg.err(player, "You haven't set a home yet! Use §e/sethome§c.");
            return 0;
        }

        HomeStorage.HomeLocation home = HomeStorage.getHome(player.getUuid());

        BackStorage.save(player);

        ServerWorld world = source.getServer().getWorld(
                RegistryKey.of(RegistryKeys.WORLD, new Identifier(home.dimension()))
        );
        if (world == null) {
            Msg.err(player, "Your home dimension no longer exists!");
            return 0;
        }

        TeleportManager.beginWarmup(player);
        Msg.info(player, "§7Teleporting home in §e5§7 seconds. Don't move!");

        startWarmup(player, world, home, WARMUP_TICKS);
        return 1;
    }

    private static void startWarmup(ServerPlayerEntity player, ServerWorld world, HomeStorage.HomeLocation home, int ticksLeft) {
        scheduleNextTick(player.getServer(), () -> tickWarmup(player, world, home, ticksLeft));
    }

    private static void tickWarmup(ServerPlayerEntity player, ServerWorld world, HomeStorage.HomeLocation home, int ticksLeft) {
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
            player.teleport(world, home.x(), home.y(), home.z(), player.getYaw(), player.getPitch());
            Msg.ok(player, "Teleported to your home! §8(" + home.formatted() + "§8)");
            TeleportManager.endWarmup(player);
            return;
        }

        if (ticksLeft % 20 == 0) {
            int seconds = ticksLeft / 20;
            Msg.info(player, "§7Teleporting in §e" + seconds + "§7 seconds...");
        }

        scheduleNextTick(player.getServer(), () -> tickWarmup(player, world, home, ticksLeft - 1));
    }

    private static void scheduleNextTick(MinecraftServer server, Runnable task) {
        server.execute(task);
    }
}
