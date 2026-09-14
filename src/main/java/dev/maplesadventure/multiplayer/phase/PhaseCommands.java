package dev.maplesadventure.multiplayer.phase;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.maplesadventure.multiplayer.phase.mob.ModPhaseAttachments;
import dev.maplesadventure.multiplayer.phase.mob.PhaseMobManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Permission-level 2 management/debug tools; not a summon/session user interface. */
public final class PhaseCommands {
    public static void register() { NeoForge.EVENT_BUS.register(new PhaseCommands()); }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> mob = Commands.literal("mob");
        mob.then(Commands.literal("zombie")
                .then(Commands.argument("phasePlayer", EntityArgument.player())
                        .executes(context -> spawnZombie(context.getSource(),
                                EntityArgument.getPlayer(context, "phasePlayer")))));
        mob.then(Commands.literal("info")
                .executes(context -> mobInfo(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.argument("entity", EntityArgument.entity())
                        .executes(context -> mobInfo(context.getSource(),
                                EntityArgument.getEntity(context, "entity")))));
        mob.then(Commands.literal("clear").executes(context -> clearMobs(context.getSource())));

        LiteralArgumentBuilder<CommandSourceStack> phase = Commands.literal("phase")
                .requires(source -> source.hasPermission(2));
        phase.then(Commands.literal("info")
                .executes(context -> info(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> info(context.getSource(), EntityArgument.getPlayer(context, "player")))));
        phase.then(Commands.literal("join")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("targetPlayer", EntityArgument.player())
                                .executes(context -> join(context.getSource(),
                                        EntityArgument.getPlayer(context, "player"),
                                        EntityArgument.getPlayer(context, "targetPlayer"))))));
        phase.then(Commands.literal("solo")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> solo(context.getSource(),
                                EntityArgument.getPlayer(context, "player")))));
        phase.then(Commands.literal("relation")
                .then(Commands.argument("playerA", EntityArgument.player())
                        .then(Commands.argument("playerB", EntityArgument.player())
                                .executes(context -> relation(context.getSource(),
                                        EntityArgument.getPlayer(context, "playerA"),
                                        EntityArgument.getPlayer(context, "playerB"))))));
        phase.then(mob);
        event.getDispatcher().register(Commands.literal("maplesadventure").then(phase));
    }

    private static int info(CommandSourceStack source, ServerPlayer player) {
        PlayerPhaseState state = PhaseManager.state(player);
        source.sendSuccess(() -> Component.literal("player=" + player.getGameProfile().getName()
                + " phaseId=" + state.phaseId() + " role=" + state.role()), false);
        return 1;
    }

    private static int join(CommandSourceStack source, ServerPlayer player, ServerPlayer target) {
        dev.maplesadventure.multiplayer.invasion.InvasionSessionManager.endForPlayer(
                player.getUUID(), dev.maplesadventure.multiplayer.invasion.InvasionSessionManager.EndReason.ADMIN_FORCED);
        dev.maplesadventure.multiplayer.coop.CoopSessionManager.endForPlayer(
                player.getUUID(), dev.maplesadventure.multiplayer.coop.CoopSessionManager.EndReason.ADMIN_FORCED);
        PlayerPhaseState state = PhaseManager.join(player, target);
        PhaseSyncService.changed(player);
        source.sendSuccess(() -> Component.literal(player.getGameProfile().getName() + " joined phase "
                + state.phaseId() + " (target=" + target.getGameProfile().getName() + ")"), true);
        return 1;
    }

    private static int solo(CommandSourceStack source, ServerPlayer player) {
        dev.maplesadventure.multiplayer.invasion.InvasionSessionManager.endForPlayer(
                player.getUUID(), dev.maplesadventure.multiplayer.invasion.InvasionSessionManager.EndReason.ADMIN_FORCED);
        dev.maplesadventure.multiplayer.coop.CoopSessionManager.endForPlayer(
                player.getUUID(), dev.maplesadventure.multiplayer.coop.CoopSessionManager.EndReason.ADMIN_FORCED);
        PlayerPhaseState state = PhaseManager.assignSolo(player);
        PhaseSyncService.changed(player);
        source.sendSuccess(() -> Component.literal(player.getGameProfile().getName() + " restored to solo phase "
                + state.phaseId()), true);
        return 1;
    }

    private static int relation(CommandSourceStack source, ServerPlayer first, ServerPlayer second) {
        boolean same = PhaseRelations.samePhase(first, second);
        source.sendSuccess(() -> Component.literal("samePhase=" + same
                + " canSee=" + PhaseRelations.canSee(first, second)
                + " canDamage=" + PhaseRelations.canDamage(first, second)
                + " canInteract=" + PhaseRelations.canInteract(first, second)
                + " canCollide=" + PhaseRelations.canCollide(first, second)
                + " canTarget=" + PhaseRelations.canTarget(first, second)), false);
        return 1;
    }

    private static int spawnZombie(CommandSourceStack source, ServerPlayer phasePlayer) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer executor = source.getPlayerOrException();
        PhaseId phaseId = PhaseManager.state(phasePlayer).phaseId();
        var zombie = PhaseMobManager.spawnZombie(executor, phaseId).orElse(null);
        if (zombie == null) {
            source.sendFailure(Component.literal("No safe loaded position for a prototype phase zombie"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Spawned prototype zombie uuid=" + zombie.getUUID()
                + " entityId=" + zombie.getId() + " phase=" + phaseId
                + " group=" + zombie.getData(ModPhaseAttachments.MOB_PHASE).instanceGroupId()
                + " pos=" + zombie.position()), true);
        return 1;
    }

    private static int mobInfo(CommandSourceStack source, ServerPlayer player) {
        var zombie = PhaseMobManager.findForInspection(player).orElse(null);
        if (zombie == null) {
            source.sendFailure(Component.literal("No prototype phase zombie within 16 blocks"));
            return 0;
        }
        return mobInfo(source, zombie);
    }

    private static int mobInfo(CommandSourceStack source, Entity entity) {
        if (!(entity instanceof Zombie zombie)) {
            source.sendFailure(Component.literal("Target is not a zombie"));
            return 0;
        }
        var existing = zombie.getExistingData(ModPhaseAttachments.MOB_PHASE);
        if (existing.isEmpty() || !existing.get().prototype()) {
            source.sendFailure(Component.literal("Target is not a MaplesAdventure prototype phase zombie"));
            return 0;
        }
        var state = existing.get();
        String target = zombie.getTarget() == null
                ? "none"
                : zombie.getTarget().getName().getString() + "/" + zombie.getTarget().getUUID();
        source.sendSuccess(() -> Component.literal("uuid=" + zombie.getUUID() + " entityId=" + zombie.getId()
                + " type=minecraft:zombie phase=" + state.phaseId() + " prototype=" + state.prototype()
                + " group=" + state.instanceGroupId() + " health=" + zombie.getHealth()
                + " target=" + target + " pos=" + zombie.position()), false);
        return 1;
    }

    private static int clearMobs(CommandSourceStack source) {
        int removed = PhaseMobManager.clearPrototypes(source.getServer());
        source.sendSuccess(() -> Component.literal("Removed " + removed + " prototype phase zombie(s)"), true);
        return removed;
    }

    private PhaseCommands() {}
}
