package dev.survivalcmds;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * Manages teleport cooldowns and movement tracking.
 * Players must stand still for 5 seconds before teleporting.
 * If they move, the teleport is cancelled.
 */
public class TeleportCooldown {

    private static final long COOLDOWN_MS = 5000; // 5 seconds
    private static final double MOVEMENT_THRESHOLD = 0.1; // blocks moved to trigger cancel

    private static class TeleportRequest {
        UUID playerId;
        long startTime;
        Vec3d lastPosition;
        String teleportType;

        TeleportRequest(UUID playerId, Vec3d position, String teleportType) {
            this.playerId = playerId;
            this.startTime = System.currentTimeMillis();
            this.lastPosition = position;
            this.teleportType = teleportType;
        }
    }

    private static final Map<UUID, TeleportRequest> activeTeleports = new HashMap<>();

    /**
     * Starts a teleport request for a player.
     * Returns true if teleport can proceed, false if already teleporting.
     */
    public static boolean startTeleport(ServerPlayerEntity player, String teleportType) {
        UUID uuid = player.getUuid();
        
        if (activeTeleports.containsKey(uuid)) {
            Msg.err(player, "You already have a pending teleport!");
            return false;
        }

        activeTeleports.put(uuid, new TeleportRequest(uuid, player.getPos(), teleportType));
        Msg.info(player, "§7Teleport in progress... §8§oStay still or it will cancel!");
        return true;
    }

    /**
     * Checks if player's teleport is ready.
     * Returns true if cooldown complete, false if still waiting or cancelled.
     */
    public static boolean isTeleportReady(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        TeleportRequest request = activeTeleports.get(uuid);

        if (request == null) {
            return false;
        }

        // Check if player moved too much
        double distanceMoved = player.getPos().distanceTo(request.lastPosition);
        if (distanceMoved > MOVEMENT_THRESHOLD) {
            activeTeleports.remove(uuid);
            Msg.err(player, "§cTeleport cancelled - you moved!");
            return false;
        }

        // Check if cooldown expired
        long elapsed = System.currentTimeMillis() - request.startTime;
        if (elapsed >= COOLDOWN_MS) {
            activeTeleports.remove(uuid);
            return true;
        }

        // Still waiting
        long remaining = (COOLDOWN_MS - elapsed) / 1000;
        Msg.info(player, "§7Teleporting in " + remaining + " seconds...");
        return false;
    }

    /**
     * Cancels an active teleport.
     */
    public static void cancelTeleport(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        if (activeTeleports.remove(uuid) != null) {
            Msg.err(player, "§cTeleport cancelled!");
        }
    }

    /**
     * Clears all teleport requests (on logout, etc).
     */
    public static void clearTeleport(UUID uuid) {
        activeTeleports.remove(uuid);
    }
}
