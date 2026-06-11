package dev.survivalcmds;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

/**
 * Shared formatting helpers for all command feedback.
 */
public class Msg {

    private static final String PREFIX_STR = "§8[§aSurvivalCmds§8]§r ";

    /** Green success message with mod prefix */
    public static void ok(ServerPlayerEntity player, String message) {
        player.sendMessage(Text.literal(PREFIX_STR + "§a" + message), false);
    }

    /** Red error message with mod prefix */
    public static void err(ServerPlayerEntity player, String message) {
        player.sendMessage(Text.literal(PREFIX_STR + "§c" + message), false);
    }

    /** Gray info message with mod prefix */
    public static void info(ServerPlayerEntity player, String message) {
        player.sendMessage(Text.literal(PREFIX_STR + "§7" + message), false);
    }

    /** Gold highlight message with mod prefix */
    public static void gold(ServerPlayerEntity player, String message) {
        player.sendMessage(Text.literal(PREFIX_STR + "§6" + message), false);
    }

    /** Send a raw Text object */
    public static void send(ServerPlayerEntity player, Text text) {
        player.sendMessage(text, false);
    }

    /** Build a clickable [Accept] button */
    public static MutableText acceptButton(String command) {
        return Text.literal(" §a§l[✔ Accept]")
                .styled(s -> s
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.literal("§aClick to accept"))));
    }

    /** Build a clickable [Deny] button */
    public static MutableText denyButton(String command) {
        return Text.literal(" §c§l[✘ Deny]")
                .styled(s -> s
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.literal("§cClick to deny"))));
    }

    /** Separator line */
    public static Text separator() {
        return Text.literal("§8§m────────────────────────§r");
    }

    public static String dim(String key) {
        return switch (key) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "The End";
            default -> key.replace("minecraft:", "");
        };
    }
}
