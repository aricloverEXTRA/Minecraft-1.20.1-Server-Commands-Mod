package dev.survivalcmds.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.survivalcmds.Msg;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
public class MenuCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("menu")
                .executes(ctx -> execute(ctx.getSource()))
        );
    }

    private static int execute(ServerCommandSource source) {
        if (!source.isExecutedByPlayer()) return 0;
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        Msg.send(player, Msg.separator());
        Msg.send(player, Text.literal("  §a§l✦ Survival Commands"));
        Msg.send(player, Msg.separator());
        Msg.send(player, runLine  ("🏠", "/sethome",  "Set home to your current spot",    "/sethome"));
        Msg.send(player, runLine  ("🏠", "/home",     "Teleport to your home",             "/home"));
        Msg.send(player, runLine  ("✦",  "/spawn",   "Teleport to world spawn",            "/spawn"));
        Msg.send(player, runLine  ("🧭", "/explore", "Random teleport ~1000 blocks away",  "/explore"));
        Msg.send(player, runLine  ("👥", "/follow",  "Teleport to another player",         "/follow"));
        Msg.send(player, runLine  ("↩️", "/back",     "Return to your previous location",   "/back"));
        Msg.send(player, Msg.separator());
        Msg.send(player, Text.literal("  §8§oClick any command to run it!"));

        return 1;
    }

    /** A line with a clickable command that runs on click */
    private static Text runLine(String icon, String cmd, String desc, String runCmd) {
        return Text.literal("  " + icon + " ")
                .append(Text.literal(cmd)
                        .styled(s -> s
                                .withColor(Formatting.AQUA)
                                .withBold(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, runCmd))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Text.literal("§7Click to run §f" + cmd)))))
                .append(Text.literal("  §7" + desc));
    }
}
