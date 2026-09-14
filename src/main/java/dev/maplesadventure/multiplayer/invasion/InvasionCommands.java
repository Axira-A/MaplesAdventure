package dev.maplesadventure.multiplayer.invasion;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class InvasionCommands {
    public static void register() { NeoForge.EVENT_BUS.register(new InvasionCommands()); }
    @SubscribeEvent public void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("maplesadventure").then(tree()));
        event.getDispatcher().register(Commands.literal("ma").then(tree()));
    }
    private static LiteralArgumentBuilder<CommandSourceStack> tree() {
        return Commands.literal("invasion")
                .then(Commands.literal("seek").executes(context -> seek(context.getSource().getPlayerOrException())))
                .then(Commands.literal("cancel").executes(context -> cancel(context.getSource().getPlayerOrException())))
                .then(Commands.literal("status").executes(context -> status(context.getSource(), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).requires(source -> source.hasPermission(2))
                                .executes(context -> status(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("end").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> end(context.getSource(), EntityArgument.getPlayer(context, "player")))));
    }
    private static int seek(ServerPlayer player) {
        if (InvasionQueueManager.isQueued(player.getUUID())) return 1;
        InvasionQueueManager.toggle(player);
        return InvasionQueueManager.isQueued(player.getUUID()) ? 1 : 0;
    }
    private static int cancel(ServerPlayer player) {
        if (!InvasionQueueManager.isQueued(player.getUUID())) return 0;
        InvasionQueueManager.toggle(player); return 1;
    }
    private static int status(CommandSourceStack source, ServerPlayer player) {
        InvasionSession session = InvasionSessionManager.session(player.getUUID()).orElse(null);
        boolean queued = InvasionQueueManager.isQueued(player.getUUID());
        source.sendSuccess(() -> session == null
                ? Component.literal("player=" + player.getGameProfile().getName() + " queued=" + queued + " no invasion session")
                : Component.literal("session=" + session.sessionId() + " state=" + session.state()
                + " host=" + session.hostUuid() + " cooperator=" + session.cooperatorUuid()
                + " invader=" + session.invaderUuid() + " phase=" + session.hostPhaseId()), false);
        return session != null || queued ? 1 : 0;
    }
    private static int end(CommandSourceStack source, ServerPlayer player) {
        boolean result = InvasionSessionManager.endForPlayer(player.getUUID(), InvasionSessionManager.EndReason.ADMIN_FORCED);
        if (result) source.sendSuccess(() -> Component.literal("Invasion ended"), true);
        else source.sendFailure(Component.literal("Player is not a host or invader in an active invasion"));
        return result ? 1 : 0;
    }
    private InvasionCommands() {}
}
