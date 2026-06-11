package dev.survivalcmds.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.survivalcmds.BackStorage;
import dev.survivalcmds.Msg;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.Random;

public class ExploreCommand {

    private static final int MIN_DIST  = 800;
    private static final int MAX_DIST  = 1200;
    private static final int MAX_TRIES = 20;
    private static final Random RANDOM = new Random();

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("explore")
                .executes(ctx -> execute(ctx.getSource()))
        );
    }

    private static int execute(ServerCommandSource source) {
        if (!source.isExecutedByPlayer()) return 0;
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        ServerWorld world = player.getServerWorld();

        // Save current position for /back
        BackStorage.save(player);

        // Try to find a safe surface landing spot
        BlockPos dest = findSafeLanding(world, player.getBlockPos());

        if (dest == null) {
            Msg.err(player, "Couldn't find a safe landing spot. Try again!");
            return 0;
        }

        player.teleport(world, dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5,
                player.getYaw(), player.getPitch());

        int dist = (int) Math.sqrt(
                Math.pow(dest.getX() - player.getBlockX(), 2) +
                Math.pow(dest.getZ() - player.getBlockZ(), 2));

        Msg.ok(player, "§aExploring! Landed §e" + dist + " blocks §aaway. §8(" +
                dest.getX() + ", " + dest.getY() + ", " + dest.getZ() + "§8)");
        return 1;
    }

    /**
     * Picks a random direction and distance between MIN_DIST–MAX_DIST,
     * then finds the top solid block at that XZ position.
     * Retries up to MAX_TRIES times to avoid landing in water or lava.
     */
    private static BlockPos findSafeLanding(ServerWorld world, BlockPos origin) {
        for (int attempt = 0; attempt < MAX_TRIES; attempt++) {
            // Random angle and distance
            double angle    = RANDOM.nextDouble() * 2 * Math.PI;
            int    distance = MIN_DIST + RANDOM.nextInt(MAX_DIST - MIN_DIST);

            int targetX = origin.getX() + (int) (Math.cos(angle) * distance);
            int targetZ = origin.getZ() + (int) (Math.sin(angle) * distance);

            // Get the highest non-air block (surface)
            BlockPos surface = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(targetX, 0, targetZ));

            // surface.getY() is the first air block above ground, step down one
            BlockPos landingBlock = surface.down();

            // Skip if landing in liquid (water, lava)
            if (!world.getBlockState(landingBlock).getFluidState().isEmpty()) continue;

            // Skip if the air above isn't clear (shouldn't happen with MOTION_BLOCKING but safety check)
            if (!world.getBlockState(surface).isAir()) continue;

            // Check there's at least 2 blocks of air headroom
            if (!world.getBlockState(surface.up()).isAir()) continue;

            return surface; // Stand on the surface position (first air block = feet position)
        }
        return null; // All attempts failed
    }
}
