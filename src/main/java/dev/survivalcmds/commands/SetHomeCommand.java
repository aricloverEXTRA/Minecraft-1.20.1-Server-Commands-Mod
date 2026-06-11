package dev.survivalcmds.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.survivalcmds.HomeStorage;
import dev.survivalcmds.Msg;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

public class SetHomeCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("sethome")
                .executes(ctx -> execute(ctx.getSource()))
        );
    }

    private static int execute(ServerCommandSource source) {
        if (!source.isExecutedByPlayer()) return 0;
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        Vec3d pos = player.getPos();
        String dim = player.getServerWorld().getRegistryKey().getValue().toString();

        HomeStorage.setHome(player.getUuid(), pos.x, pos.y, pos.z, dim);

        Msg.ok(player, "§aHome set! §8(§7" +
                String.format("%.1f, %.1f, %.1f", pos.x, pos.y, pos.z) +
                " in " + Msg.dim(dim) + "§8)");
        return 1;
    }
}
