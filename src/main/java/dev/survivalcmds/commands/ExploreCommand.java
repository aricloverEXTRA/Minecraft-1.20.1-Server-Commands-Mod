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
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class ExploreCommand {

    private static final int MIN_DIST = 1000;
    private static final int MAX_DIST = 5000;
    private static final int MAX_TRIES = 50;
    private static final int RESISTANCE_DURATION = 1800; // 90s
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

        // Cooldown + movement check
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

        // Start async search
        findLandingAsync(player, world, origin, 0);

        Msg.info(player, "§7Searching for a safe location...");
        return 1;
    }

    /**
     * Asynchronous recursive search for a safe landing spot.
     */
    private static void findLandingAsync(ServerPlayerEntity player, ServerWorld world, BlockPos origin, int attempt) {
        if (attempt >= MAX_TRIES) {
            Msg.err(player, "Couldn't find a safe landing spot. Try again!");
            TeleportManager.cancelTeleport(player);
            return;
        }

        // Pick random location
        double angle = RANDOM.nextDouble() * 2 * Math.PI;
        int distance = MIN_DIST + RANDOM.nextInt(MAX_DIST - MIN_DIST + 1);

        int targetX = origin.getX() + (int) (Math.cos(angle) * distance);
        int targetZ = origin.getZ() + (int) (Math.sin(angle) * distance);

        ChunkPos chunkPos = new ChunkPos(targetX >> 4, targetZ >> 4);

        // Load chunk asynchronously
        CompletableFuture<?> future = world.getChunkManager().getChunkFuture(
                chunkPos.x, chunkPos.z,
                ChunkStatus.FULL,
                true
        );

        future.thenAcceptAsync(chunk -> {
            if (chunk == null) {
                findLandingAsync(player, world, origin, attempt + 1);
                return;
            }

            // Now safe to query heightmap
            BlockPos surface = world.getTopPosition(
                    Heightmap.Type.MOTION_BLOCKING,
                    new BlockPos(targetX, 0, targetZ)
            );

            BlockPos ground = surface.down();

            // Must be grass
            if (!world.getBlockState(ground).isOf(Blocks.GRASS_BLOCK)) {
                findLandingAsync(player, world, origin, attempt + 1);
                return;
            }

            // Must have 2 air blocks above
            if (!world.getBlockState(surface).isAir() ||
                !world.getBlockState(surface.up()).isAir()) {
                findLandingAsync(player, world, origin, attempt + 1);
                return;
            }

            // No liquids
            if (!world.getBlockState(ground).getFluidState().isEmpty()) {
                findLandingAsync(player, world, origin, attempt + 1);
                return;
            }

            // Safe spot found — teleport on main thread
            world.getServer().execute(() -> completeTeleport(player, world, origin, surface));
        });
    }

    private static void completeTeleport(ServerPlayerEntity player, ServerWorld world, BlockPos origin, BlockPos dest) {
        if (player == null || !player.isAlive()) return;

        BackStorage.save(player);

        player.teleport(world,
                dest.getX() + 0.5,
                dest.getY() + 1,
                dest.getZ() + 0.5,
                player.getYaw(),
                player.getPitch()
        );

        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, RESISTANCE_DURATION, 0, false, false));

        int dist = (int) Math.sqrt(
                Math.pow(dest.getX() - origin.getX(), 2) +
                Math.pow(dest.getZ() - origin.getZ(), 2)
        );

        Msg.ok(player, "§aExploring! Landed §e" + dist + " blocks §aaway. §8(" +
                dest.getX() + ", " + dest.getY() + ", " + dest.getZ() + "§8)");
        Msg.info(player, "§7You have §eResistance I §7for 90 seconds.");

        TeleportManager.completeTeleport(player);
    }
}

