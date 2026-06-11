package dev.survivalcmds;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory storage for each player's last teleport position (/back).
 */
public class BackStorage {

    public record SavedPos(double x, double y, double z, float yaw, float pitch, String dimension) {}

    private static final Map<UUID, SavedPos> backPositions = new HashMap<>();

    /** Call this BEFORE teleporting so the player can return. */
    public static void save(ServerPlayerEntity player) {
        Vec3d pos = player.getPos();
        String dim = player.getServerWorld().getRegistryKey().getValue().toString();
        backPositions.put(player.getUuid(),
                new SavedPos(pos.x, pos.y, pos.z, player.getYaw(), player.getPitch(), dim));
    }

    public static boolean hasBack(UUID uuid) {
        return backPositions.containsKey(uuid);
    }

    /** Teleports the player to their saved back position. Returns false if none saved. */
    public static boolean teleportBack(ServerPlayerEntity player) {
        SavedPos saved = backPositions.get(player.getUuid());
        if (saved == null) return false;

        // Save current pos before going back (so /back toggles)
        SavedPos current = new SavedPos(
                player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch(),
                player.getServerWorld().getRegistryKey().getValue().toString()
        );

        ServerWorld world = player.getServer().getWorld(
                RegistryKey.of(RegistryKeys.WORLD, new Identifier(saved.dimension()))
        );
        if (world == null) {
            world = player.getServerWorld(); // fallback to current world
        }

        player.teleport(world, saved.x(), saved.y(), saved.z(), saved.yaw(), saved.pitch());
        backPositions.put(player.getUuid(), current); // swap so /back toggles
        return true;
    }
}
