package dev.survivalcmds;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persists per-player home positions to disk in the world's data folder.
 */
public class HomeStorage {

    public record HomeLocation(double x, double y, double z, String dimension) {
        public String formatted() {
            return String.format("%.1f, %.1f, %.1f in %s", x, y, z,
                    dimension.replace("minecraft:", "").replace("_", " "));
        }
    }

    private static final Map<UUID, HomeLocation> homes = new HashMap<>();
    private static File dataFile;

    public static void init(MinecraftServer server) {
        File dataDir = new File(server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).toFile(), "data");
        dataDir.mkdirs();
        dataFile = new File(dataDir, "survivalcmds_homes.dat");
        load();
    }

    public static void setHome(UUID uuid, double x, double y, double z, String dimension) {
        homes.put(uuid, new HomeLocation(x, y, z, dimension));
        save();
    }

    public static HomeLocation getHome(UUID uuid) {
        return homes.get(uuid);
    }

    public static boolean hasHome(UUID uuid) {
        return homes.containsKey(uuid);
    }

    private static void load() {
        if (dataFile == null || !dataFile.exists()) return;
        try {
            NbtCompound root = NbtIo.read(dataFile.toPath());
            if (root == null) return;
            NbtCompound playersTag = root.getCompound("homes");
            for (String key : playersTag.getKeys()) {
                NbtCompound entry = playersTag.getCompound(key);
                homes.put(UUID.fromString(key), new HomeLocation(
                        entry.getDouble("x"), entry.getDouble("y"), entry.getDouble("z"),
                        entry.getString("dim")
                ));
            }
        } catch (IOException e) {
            SurvivalCommands.LOGGER.error("Failed to load homes: {}", e.getMessage());
        }
    }

    private static void save() {
        if (dataFile == null) return;
        try {
            NbtCompound root = new NbtCompound();
            NbtCompound playersTag = new NbtCompound();
            for (Map.Entry<UUID, HomeLocation> entry : homes.entrySet()) {
                NbtCompound e = new NbtCompound();
                e.putDouble("x", entry.getValue().x());
                e.putDouble("y", entry.getValue().y());
                e.putDouble("z", entry.getValue().z());
                e.putString("dim", entry.getValue().dimension());
                playersTag.put(entry.getKey().toString(), e);
            }
            root.put("homes", playersTag);
            NbtIo.write(root, dataFile.toPath());
        } catch (IOException e) {
            SurvivalCommands.LOGGER.error("Failed to save homes: {}", e.getMessage());
        }
    }
}
