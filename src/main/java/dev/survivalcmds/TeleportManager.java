package dev.survivalcmds;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.UUID;

public class TeleportManager {

    private static final long EXPLORE_COOLDOWN_MS = 30_000; // 30 seconds

    private static final HashMap<UUID, Long> exploreCooldowns = new HashMap<>();
    private static final HashMap<UUID, Vec3d> warmupStartPos = new HashMap<>();

    // ===== EXPLORE COOLDOWN =====

    public static boolean canUseExplore(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        long now = System.currentTimeMillis();

        Long until = exploreCooldowns.get(id);
        if (until == null) return true;
        return now >= until;
    }

    public static double getRemainingExploreCooldown(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        long now = System.currentTimeMillis();

        Long until = exploreCooldowns.get(id);
        if (until == null || now >= until) return 0.0;

        return (until - now) / 1000.0;
    }

    public static void applyExploreCooldown(ServerPlayerEntity player) {
        exploreCooldowns.put(player.getUuid(), System.currentTimeMillis() + EXPLORE_COOLDOWN_MS);
    }

    // ===== WARMUP (shared) =====

    public static void beginWarmup(ServerPlayerEntity player) {
        warmupStartPos.put(player.getUuid(), player.getPos());
    }

    public static boolean hasPlayerMoved(ServerPlayerEntity player) {
        Vec3d start = warmupStartPos.get(player.getUuid());
        if (start == null) return false;
        Vec3d now = player.getPos();
        return now.squaredDistanceTo(start) > 0.01;
    }

    public static void endWarmup(ServerPlayerEntity player) {
        warmupStartPos.remove(player.getUuid());
    }

    public static void cancelWarmup(ServerPlayerEntity player) {
        warmupStartPos.remove(player.getUuid());
    }
}
