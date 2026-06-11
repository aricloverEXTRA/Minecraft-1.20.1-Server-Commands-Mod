package dev.survivalcmds.follow;

import dev.survivalcmds.BackStorage;
import dev.survivalcmds.Msg;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;

import java.util.*;

/**
 * Manages one-time follow teleport requests between players.
 *
 * Flow:
 *   1. Player A: /follow  → shows clickable menu of online players
 *   2. Player A clicks a name → sendRequest(A, B)
 *   3. Player B gets a chat menu with [✔ Accept] [✘ Deny]
 *   4. Player B: /followaccept  → B teleports to A (or A to B — A is requester, tp A to B)
 *      Player B: /followdeny    → request cancelled
 *
 * No persistent following — it's a single teleport.
 */
public class FollowManager {

    /** Pending requests: target UUID → requester UUID */
    private static final Map<UUID, UUID> pendingRequests = new HashMap<>();

    // ─── Show player menu ──────────────────────────────────────────────────────

    public static void showMenu(ServerPlayerEntity requester) {
        MinecraftServer server = requester.getServer();
        List<ServerPlayerEntity> others = server.getPlayerManager().getPlayerList()
                .stream()
                .filter(p -> !p.getUuid().equals(requester.getUuid()))
                .toList();

        if (others.isEmpty()) {
            Msg.err(requester, "No other players are online!");
            return;
        }

        Msg.send(requester, Msg.separator());
        Msg.send(requester, Text.literal("  §a§l👥 Follow a Player"));
        Msg.send(requester, Text.literal("  §7Click a name to send them a request:"));
        Msg.send(requester, Text.literal(""));

        for (ServerPlayerEntity target : others) {
            String name = target.getName().getString();
            MutableText line = Text.literal("    §8● ")
                    .append(Text.literal("§e" + name)
                            .styled(s -> s
                                    .withClickEvent(new ClickEvent(
                                            ClickEvent.Action.RUN_COMMAND,
                                            "/followrequest " + name))
                                    .withHoverEvent(new HoverEvent(
                                            HoverEvent.Action.SHOW_TEXT,
                                            Text.literal("§7Click to send a follow request to §e" + name)))));
            Msg.send(requester, line);
        }

        Msg.send(requester, Text.literal(""));
        Msg.send(requester, Msg.separator());
    }

    // ─── Send request ─────────────────────────────────────────────────────────

    public static void sendRequest(MinecraftServer server, ServerPlayerEntity requester, ServerPlayerEntity target) {
        if (target.getUuid().equals(requester.getUuid())) {
            Msg.err(requester, "You can't follow yourself.");
            return;
        }

        // Overwrite any previous pending request to this target
        pendingRequests.put(target.getUuid(), requester.getUuid());

        // Send the target a menu with Accept / Deny
        String requesterName = requester.getName().getString();

        Msg.send(target, Msg.separator());
        Msg.send(target, Text.literal("  §a§l👥 Follow Request"));
        Msg.send(target, Text.literal("  §e" + requesterName + " §7wants to teleport to you."));
        Msg.send(target, Text.literal(""));
        MutableText buttons = Text.literal("  ")
                .append(Text.literal("§a§l[ ✔ Accept ]")
                        .styled(s -> s
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/followaccept"))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Text.literal("§aTeleport " + requesterName + " to you")))))
                .append(Text.literal("   "))
                .append(Text.literal("§c§l[ ✘ Deny ]")
                        .styled(s -> s
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/followdeny"))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Text.literal("§cDecline the request")))));
        Msg.send(target, buttons);
        Msg.send(target, Text.literal(""));
        Msg.send(target, Msg.separator());

        Msg.info(requester, "Request sent to §e" + target.getName().getString() + "§7. Waiting for their response...");
    }

    // ─── Accept ───────────────────────────────────────────────────────────────

    public static void accept(MinecraftServer server, ServerPlayerEntity target) {
        UUID requesterId = pendingRequests.remove(target.getUuid());
        if (requesterId == null) {
            Msg.err(target, "No pending follow request.");
            return;
        }

        ServerPlayerEntity requester = server.getPlayerManager().getPlayer(requesterId);
        if (requester == null) {
            Msg.err(target, "That player is no longer online.");
            return;
        }

        // Save requester's position so they can /back
        BackStorage.save(requester);

        // Teleport requester to target
        requester.teleport(
                target.getServerWorld(),
                target.getX(), target.getY(), target.getZ(),
                requester.getYaw(), requester.getPitch());

        Msg.ok(requester, "§e" + target.getName().getString() + " §aaccepted! Teleported to them.");
        Msg.ok(target,    "§e" + requester.getName().getString() + " §ateleported to you.");
    }

    // ─── Deny ─────────────────────────────────────────────────────────────────

    public static void deny(MinecraftServer server, ServerPlayerEntity target) {
        UUID requesterId = pendingRequests.remove(target.getUuid());
        if (requesterId == null) {
            Msg.err(target, "No pending follow request.");
            return;
        }

        ServerPlayerEntity requester = server.getPlayerManager().getPlayer(requesterId);
        if (requester != null) {
            Msg.err(requester, "§e" + target.getName().getString() + " §cdeclined your follow request.");
        }
        Msg.info(target, "Follow request declined.");
    }

    public static boolean hasPendingRequest(UUID targetUuid) {
        return pendingRequests.containsKey(targetUuid);
    }
}
