package dev.survivalcmds.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.survivalcmds.BackStorage;
import dev.survivalcmds.HomeStorage;
import dev.survivalcmds.Msg;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public class HomeCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("home")
                .executes(ctx -> execute(ctx.getSource()))
        );
    }

    private static int execute(ServerCommandSource source) {
        if (!source.isExecutedByPlayer()) return 0;
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        if (!HomeStorage.hasHome(player.getUuid())) {
            Msg.err(player, "You haven't set a home yet! Use §e/sethome §cto set one.");
            return 0;
        }

        HomeStorage.HomeLocation home = HomeStorage.getHome(player.getUuid());

        // Save current pos for /back
        BackStorage.save(player);

        // Get the target world
        ServerWorld world = source.getServer().getWorld(
                RegistryKey.of(RegistryKeys.WORLD, new Identifier(home.dimension()))
        );
        if (world == null) {
            Msg.err(player, "Your home dimension no longer exists!");
            return 0;
        }

        player.teleport(world, home.x(), home.y(), home.z(), player.getYaw(), player.getPitch());
        Msg.ok(player, "Teleported to your home! §8(" + home.formatted() + "§8)");
        return 1;
    }
}
