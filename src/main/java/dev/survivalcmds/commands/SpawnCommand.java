package dev.survivalcmds.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.survivalcmds.BackStorage;
import dev.survivalcmds.Msg;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SpawnCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("spawn")
                .executes(ctx -> execute(ctx.getSource()))
        );
    }

    private static int execute(ServerCommandSource source) {
        if (!source.isExecutedByPlayer()) {
            source.sendError(net.minecraft.text.Text.literal("Only players can use this."));
            return 0;
        }

        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        // Save current position for /back
        BackStorage.save(player);

        // Get the overworld and its spawn point
        ServerWorld overworld = source.getServer().getWorld(World.OVERWORLD);
        if (overworld == null) {
            Msg.err(player, "Could not find the overworld!");
            return 0;
        }

        BlockPos spawnPos = overworld.getSpawnPos();
        player.teleport(overworld,
                spawnPos.getX() + 0.5,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5,
                player.getYaw(), player.getPitch());

        Msg.ok(player, "Teleported to world spawn! §8(" + spawnPos.getX() + ", " + spawnPos.getY() + ", " + spawnPos.getZ() + ")");
        return 1;
    }
}
