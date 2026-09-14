package dev.maplesadventure.multiplayer.echo;

import dev.maplesadventure.multiplayer.phase.PhaseRelations;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Permission-level 2 deterministic test hooks; automatic scheduling uses the same send path. */
public final class EchoCommands {
    public static void register() { NeoForge.EVENT_BUS.register(new EchoCommands()); }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("maplesadventure")
                .then(Commands.literal("phase").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("echo")
                                .then(Commands.literal("trigger")
                                        .then(Commands.argument("viewer", EntityArgument.player())
                                                .then(Commands.argument("source", EntityArgument.player())
                                                        .executes(context -> trigger(context.getSource(),
                                                                EntityArgument.getPlayer(context, "viewer"),
                                                                EntityArgument.getPlayer(context, "source"))))))
                                .then(Commands.literal("status")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> status(context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"))))))));
    }

    private static int trigger(CommandSourceStack command, ServerPlayer viewer, ServerPlayer source) {
        if (viewer == source) return fail(command, "viewer and source must differ");
        if (viewer.level() != source.level()) return fail(command, "players are not in the same dimension");
        if (PhaseRelations.canSee(viewer, source)) return fail(command, "players are in a visible/same phase relation");
        if (!EchoSyncService.sendPlayback(viewer, source, true)) return fail(command, "source has insufficient delayed history");
        command.sendSuccess(() -> Component.literal("Residual Echo sent: viewer="
                + viewer.getGameProfile().getName() + " source=" + source.getGameProfile().getName()), false);
        return 1;
    }

    private static int status(CommandSourceStack command, ServerPlayer player) {
        command.sendSuccess(() -> Component.literal("player=" + player.getGameProfile().getName()
                + " recordedFrames=" + PlayerEchoRecorder.frameCount(player.getUUID())
                + " nextEchoTick=" + EchoScheduler.nextEligible(player.getUUID())), false);
        return 1;
    }
    private static int fail(CommandSourceStack command, String reason) {
        command.sendFailure(Component.literal("Residual Echo rejected: " + reason));
        return 0;
    }
    private EchoCommands() {}
}
