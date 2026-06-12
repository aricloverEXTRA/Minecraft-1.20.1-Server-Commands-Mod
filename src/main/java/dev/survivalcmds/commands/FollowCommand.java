package dev.survivalcmds.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.survivalcmds.Msg;
import dev.survivalcmds.follow.FollowManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class FollowCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {

        dispatcher.register(
            CommandManager.literal("follow")
                .executes(ctx -> executeMenu(ctx.getSource()))
        );

        dispatcher.register(
            CommandManager.literal("followrequest")
                .then(CommandManager.argument("player", StringArgumentType.word())
                    .executes(ctx -> executeRequest(
                        ctx.getSource(),
                        StringArgumentType.getString(ctx, "player"))))
        );

        dispatcher.register(
            CommandManager.literal("followaccept")
                .executes(ctx -> executeAccept(ctx.getSource()))
        );

        dispatcher.register(
            CommandManager.literal("followdeny")
                .executes(ctx -> executeDeny(ctx.getSource()))
        );
    }

    private static int executeMenu(ServerCommandSource source) {
        if (!source.isExecutedByPlayer()) return 0;
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        FollowManager.showMenu(player);
        return 1;
    }

    private static int executeRequest(ServerCommandSource source, String targetName) {
        if (!source.isExecutedByPlayer()) return 0;
        ServerPlayerEntity requester = source.getPlayer();
        if (requester == null) return 0;

        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(targetName);
        if (target == null) {
            Msg.err(requester, "Player §e" + targetName + " §cis no longer online.");
            return 0;
        }

        FollowManager.sendRequest(source.getServer(), requester, target);
        return 1;
    }

    private static int executeAccept(ServerCommandSource source) {
        if (!source.isExecutedByPlayer()) return 0;
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        if (!FollowManager.hasPendingRequest(player.getUuid())) {
            Msg.err(player, "You have no pending follow request.");
            return 0;
        }

        FollowManager.accept(source.getServer(), player);
        Msg.ok(player, "Teleporting to your friend!");

        return 1;
    }

    private static int executeDeny(ServerCommandSource source) {
        if (!source.isExecutedByPlayer()) return 0;
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        if (!FollowManager.hasPendingRequest(player.getUuid())) {
            Msg.err(player, "You have no pending follow request.");
            return 0;
        }

        FollowManager.deny(source.getServer(), player);
        return 1;
    }
}
