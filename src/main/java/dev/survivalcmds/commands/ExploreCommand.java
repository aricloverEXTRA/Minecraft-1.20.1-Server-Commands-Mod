package dev.survivalcmds.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.survivalcmds.BackStorage;
import dev.survivalcmds.Msg;
import dev.survivalcmds.TeleportManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;

import java.util.Random;

public class ExploreCommand {

    private static final int MIN_DIST = 1000;
    private static final int MAX_DIST = 5000;
    private static final int MIN_WARMUP_TICKS = 100; // 5 seconds
    private static final int RESISTANCE_DURATION = 1800;
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

        // 30-second cooldown
        if (!TeleportManager.canUseExplore(player)) {
            double cd = TeleportManager.getRemainingExploreCooldown(player);
            Msg.err(player, "Teleport on cooldown! Wait §e" + String.format("%.1f", cd) + "s§c.");
            return 0;
        }

        ServerWorld world = player.getServerWorld();
        BlockPos origin = player.getBlockPos();

        // Pick ONE random target
        double angle = RANDOM.nextDouble() * 2 * Math.PI;
        int distance = MIN_DIST + RANDOM.nextInt(MAX_DIST - MIN_DIST + 1);

        int targetX = origin.getX() + (int) (Math.cos(angle) * distance);
        int targetZ = origin.getZ() + (int) (Math.sin(angle) * distance);
        ChunkPos chunkPos = new ChunkPos(targetX >> 4, targetZ >> 4);

        TeleportManager.beginWarmup(player);
        Msg.info(player, "§7Exploring in §e5§7+ seconds. Don't move while chunks generate!");

        startWarmup(player, world, origin, targetX, targetZ, chunkPos, MIN_WARMUP_TICKS, false);
        return 1;
    }

    private static void startWarmup(ServerPlayerEntity player,
                                    ServerWorld world,
                                    BlockPos origin,
                                    int targetX,
                                    int targetZ,
                                    ChunkPos chunkPos,
                                    int warmupTicksLeft,
                                    boolean warnedOnce) {

        if (player.getServer() == null) return;

        // Slow chunk loading
        world.getChunkManager().addTicket(
                ChunkTicketType.FORCED,
                chunkPos,
                1,
                chunkPos
        );

        scheduleNextTick(player.getServer(), () ->
                tickWarmup(player, world, origin, targetX, targetZ, chunkPos, warmupTicksLeft, warnedOnce));
    }

    private static void tickWarmup(ServerPlayerEntity player,
                                   ServerWorld world,
                                   BlockPos origin,
                                   int targetX,
                                   int targetZ,
                                   ChunkPos chunkPos,
                                   int warmupTicksLeft,
                                   boolean warnedOnce) {

        if (player == null || !player.isAlive()) {
            TeleportManager.cancelWarmup(player);
            return;
        }

        // Movement cancels
        if (TeleportManager.hasPlayerMoved(player)) {
            TeleportManager.cancelWarmup(player);
            Msg.err(player, "Teleport cancelled because you moved.");
            return;
        }

        boolean chunkReady = world.getChunkManager().isChunkLoaded(chunkPos.x, chunkPos.z);

        // Countdown every second
        if (warmupTicksLeft > 0 && warmupTicksLeft % 20 == 0) {
            int seconds = warmupTicksLeft / 20;
            Msg.info(player, "§7Teleporting in §e" + seconds + "§7 seconds...");
        }

        // Warmup finished AND chunk ready → teleport
        if (warmupTicksLeft <= 0 && chunkReady) {
            performTeleport(player, world, origin, targetX, targetZ);
            return;
        }

        // Warmup finished but chunk not ready → show ONCE
        if (warmupTicksLeft <= 0 && !chunkReady && !warnedOnce) {
            Msg.info(player, "§7Chunks are still generating, please wait...");
            warnedOnce = true;
        }

        // Continue ticking
        int nextTicks = warmupTicksLeft > 0 ? warmupTicksLeft - 1 : 0;

        boolean finalWarnedOnce = warnedOnce;

        scheduleNextTick(player.getServer(), () ->
                tickWarmup(player, world, origin, targetX, targetZ, chunkPos, nextTicks, finalWarnedOnce));
    }

    private static void performTeleport(ServerPlayerEntity player,
                                        ServerWorld world,
                                        BlockPos origin,
                                        int targetX,
                                        int targetZ) {

        TeleportManager.endWarmup(player);

        // Now that chunk is ready, find safe landing
        BlockPos surface = world.getTopPosition(
                Heightmap.Type.MOTION_BLOCKING,
                new BlockPos(targetX, 0, targetZ)
        );

        if (!isSafeLanding(world, surface)) {
            Msg.err(player, "Couldn't find a safe landing spot. Try again!");
            return;
        }

        BackStorage.save(player);

        player.teleport(world,
                surface.getX() + 0.5,
                surface.getY() + 1,
                surface.getZ() + 0.5,
                player.getYaw(),
                player.getPitch()
        );

        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, RESISTANCE_DURATION, 0, false, false));

        int dist = (int) Math.sqrt(
                Math.pow(surface.getX() - origin.getX(), 2) +
                Math.pow(surface.getZ() - origin.getZ(), 2)
        );

        Msg.ok(player, "§aExploring! Landed §e" + dist + " blocks §aaway. §8(" +
                surface.getX() + ", " + surface.getY() + ", " + surface.getZ() + "§8)");
        Msg.info(player, "§7You have §eResistance I §7for 90 seconds.");

        TeleportManager.applyExploreCooldown(player);
    }

    // ============================================================
    // SAFE LANDING CHECK
    // ============================================================

    private static boolean isSafeLanding(ServerWorld world, BlockPos surface) {
        BlockPos ground = surface.down();
        BlockState groundState = world.getBlockState(ground);

        // Must be solid
        if (!groundState.isSolidBlock(world, ground)) return false;

        // Must be full cube (no slabs, stairs, fences)
        if (!groundState.getBlock().isShapeFullCube(groundState.getCollisionShape(world, ground))) return false;

        // No fluids
        if (!groundState.getFluidState().isEmpty()) return false;

        // Avoid dangerous blocks
        if (groundState.isOf(Blocks.CACTUS)) return false;
        if (groundState.isOf(Blocks.MAGMA_BLOCK)) return false;
        if (groundState.isOf(Blocks.FIRE)) return false;
        if (groundState.isOf(Blocks.LAVA)) return false;
        if (groundState.isOf(Blocks.CAMPFIRE)) return false;
        if (groundState.isOf(Blocks.SOUL_CAMPFIRE)) return false;
        if (groundState.isOf(Blocks.SWEET_BERRY_BUSH)) return false;
        if (groundState.isOf(Blocks.POINTED_DRIPSTONE)) return false;

        // Feet must be air
        if (!world.getBlockState(surface).isAir()) return false;

        // Head must be air
        if (!world.getBlockState(surface.up()).isAir()) return false;

        return true;
    }

    private static void scheduleNextTick(MinecraftServer server, Runnable task) {
        server.execute(task); // runs once next tick
    }
}