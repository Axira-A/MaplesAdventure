package dev.maplesadventure.multiplayer.coop;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class CoopCommands {
    public static void register() { NeoForge.EVENT_BUS.register(new CoopCommands()); }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("maplesadventure")
                .then(Commands.literal("summon")
                        .then(Commands.literal("leave")
                                .executes(context -> leave(context.getSource().getPlayerOrException())))
                        .then(Commands.literal("dismiss")
                                .executes(context -> dismiss(context.getSource().getPlayerOrException())))
                        .then(Commands.literal("status")
                                .executes(context -> status(context.getSource(), context.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player()).requires(source -> source.hasPermission(2))
                                        .executes(context -> status(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                        .then(Commands.literal("forceend").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> forceEnd(context.getSource(),
                                                EntityArgument.getPlayer(context, "player")))))
                        .then(Commands.literal("debugstart").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("host", EntityArgument.player())
                                        .then(Commands.argument("cooperator", EntityArgument.player())
                                                .executes(context -> debugStart(context.getSource(),
                                                        EntityArgument.getPlayer(context, "host"),
                                                        EntityArgument.getPlayer(context, "cooperator"))))))));
    }

    private static int leave(ServerPlayer player) {
        if (CoopSessionManager.leave(player)) return 1;
        player.displayClientMessage(Component.translatable("summon.maplesadventure.failure.not_cooperator"), true);
        return 0;
    }

    private static int dismiss(ServerPlayer player) {
        if (CoopSessionManager.dismiss(player)) return 1;
        player.displayClientMessage(Component.translatable("summon.maplesadventure.failure.not_host"), true);
        return 0;
    }

    private static int status(CommandSourceStack source, ServerPlayer player) {
        CoopSession session = CoopSessionManager.session(player.getUUID()).orElse(null);
        source.sendSuccess(() -> session == null
                ? Component.literal(player.getGameProfile().getName() + ": no active co-op session")
                : Component.literal("session=" + session.sessionId() + " host=" + session.hostUuid()
                        + " cooperator=" + session.cooperatorUuid() + " phase=" + session.phaseId()
                        + " state=" + session.state()), false);
        return session == null ? 0 : 1;
    }

    private static int forceEnd(CommandSourceStack source, ServerPlayer player) {
        boolean ended = CoopSessionManager.endForPlayer(player.getUUID(), CoopSessionManager.EndReason.ADMIN_FORCED);
        if (ended) source.sendSuccess(() -> Component.literal("Ended co-op session for "
                + player.getGameProfile().getName()), true);
        else source.sendFailure(Component.literal("Player has no active co-op session"));
        return ended ? 1 : 0;
    }

    private static int debugStart(CommandSourceStack source, ServerPlayer host, ServerPlayer cooperator) {
        if (host == cooperator) {
            source.sendFailure(Component.literal("Host and cooperator must be different players"));
            return 0;
        }
        SummonSignManager.removeOwner(source.getServer(), cooperator.getUUID());
        SummonSignManager.toggle(cooperator);
        SummonSignRecord sign = SummonSignManager.findOwner(cooperator.getUUID()).orElse(null);
        if (sign == null) {
            source.sendFailure(Component.literal("Could not place a validated summon sign for cooperator"));
            return 0;
        }
        CoopSessionManager.SummonResult result = CoopSessionManager.summon(host, sign.signId());
        if (result == CoopSessionManager.SummonResult.SUCCESS) {
            source.sendSuccess(() -> Component.literal("Started validated co-op session: host="
                    + host.getGameProfile().getName() + " cooperator=" + cooperator.getGameProfile().getName()), true);
            return 1;
        }
        source.sendFailure(Component.literal("Co-op debug start rejected: " + result));
        return 0;
    }

    private CoopCommands() {}
}
