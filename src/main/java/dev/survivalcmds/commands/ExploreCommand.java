package dev.survivalcmds.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.survivalcmds.BackStorage;
import dev.survivalcmds.Msg;
import dev.survivalcmds.TeleportManager;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.Random;

public class ExploreCommand {

    private static final int MIN_DIST = 1000;      // 1K radius
    private static final int MAX_DIST = 5000;      // 5K radius
    private static final int MAX_TRIES = 50;       // More attempts for reliability
    private static final Random RANDOM = new Random();
    private static final int RESISTANCE_DURATION = 1800; // 90 seconds in ticks (20 ticks/sec)

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

        // Check cooldown and movement
        if (!TeleportManager.startTeleport(player)) {
            double cooldown = TeleportManager.getRemainingCooldown(player);
            if (cooldown > 0) {
                Msg.err(player, "Teleport on cooldown! Wait §e" + String.format("%.1f", cooldown) + "s§c.");
            } else {
                Msg.err(player, "You must stand still to use this command!");
            }
            return 0;
        }

        ServerWorld world = player.getServerWorld();
        BlockPos origin = player.getBlockPos();

        // Find safe grass landing spot
        BlockPos dest = findGrassLanding(world, origin);

        if (dest == null) {
            Msg.err(player, "Couldn't find a safe landing spot. Try again!");
            return 0;
        }

        // Teleport and complete cooldown
        player.teleport(world, dest.getX() + 0.5, dest.getY() + 1, dest.getZ() + 0.5,
                player.getYaw(), player.getPitch());
        BackStorage.save(player);

        // Give resistance effect for safety (90 seconds)
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, RESISTANCE_DURATION, 0, false, false));

        int dist = (int) Math.sqrt(
                Math.pow(dest.getX() - origin.getX(), 2) +
                Math.pow(dest.getZ() - origin.getZ(), 2));

        Msg.ok(player, "§aExploring! Landed §e" + dist + " blocks §aaway. §8(" +
                dest.getX() + ", " + dest.getY() + ", " + dest.getZ() + "§8)");
        Msg.info(player, "§7You have §eResistance I §7for 90 seconds. Stay safe!");
        
        TeleportManager.completeTeleport(player);
        return 1;
    }

    /**
     * Optimized search: Finds a grass block to stand on within 1K-5K radius.
     * Checks surface for grass specifically to avoid water/lava/leaves.
     */
    private static BlockPos findGrassLanding(ServerWorld world, BlockPos origin) {
        for (int attempt = 0; attempt < MAX_TRIES; attempt++) {
            // Random angle and distance in 1K-5K range
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            int distance = MIN_DIST + RANDOM.nextInt(MAX_DIST - MIN_DIST + 1);

            int targetX = origin.getX() + (int) (Math.cos(angle) * distance);
            int targetZ = origin.getZ() + (int) (Math.sin(angle) * distance);

            // Get the highest solid block
            BlockPos surface = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING,
                    new BlockPos(targetX, 0, targetZ));

            // Check if the block below surface is grass
            BlockPos groundBlock = surface.down();
            if (!world.getBlockState(groundBlock).getBlock().equals(Blocks.GRASS_BLOCK)) {
                continue; // Not grass, skip
            }

            // Verify player can stand here (2 air blocks above)
            if (!world.getBlockState(surface).isAir()) continue;
            if (!world.getBlockState(surface.up()).isAir()) continue;

            // Extra safety: check no lava/water nearby
            if (world.getBlockState(groundBlock).getFluidState().isEmpty()) {
                return surface; // Found safe grass landing!
            }
        }
        return null;
    }
}
