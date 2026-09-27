package dev.maplesadventure.flask;

import dev.maplesadventure.api.flask.*;
import dev.maplesadventure.bonfire.BonfireSessionService;
import dev.maplesadventure.progression.ProgressionAttachments;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/** Transient active actions only, not player persistence. Iterates active drinkers, never all players. */
public final class FlaskUseController {
    private static final Map<UUID, Use> ACTIVE = new HashMap<>();
    private static final ResourceLocation SLOW = ResourceLocation.parse("maplesadventure:flask_drinking");
    private static final class Use {
        final ServerPlayer player; final FlaskKind kind; final InteractionHand hand; final ItemStack stack;
        final ResourceLocation dimension; final FlaskUseTimeline clock = new FlaskUseTimeline();
        boolean applied;
        Use(ServerPlayer p, FlaskKind k, InteractionHand h) { player = p; kind = k; hand = h; stack = p.getItemInHand(h); dimension = p.level().dimension().location(); }
    }
    public static boolean start(ServerPlayer player, FlaskKind kind, InteractionHand hand) {
        FlaskService.checkThread(player);
        if (ACTIVE.containsKey(player.getUUID()) || !allowed(player) || player.isUsingItem()
                || (kind == FlaskKind.ASHEN && !FlaskManaBridge.available()) || FlaskService.state(player).remaining(kind) == 0) return false;
        Use use = new Use(player, kind, hand);
        ACTIVE.put(player.getUUID(), use); // reserve before extension callbacks, including reentrant starts
        FlaskEvents.Start event = new FlaskEvents.Start(player, kind);
        if (!FlaskService.post(event) || event.isCanceled() || ACTIVE.get(player.getUUID()) != use || !allowed(player)) {
            cancel(player, FlaskCancelReason.INVALID_STATE); return false;
        }
        player.setData(ProgressionAttachments.FLASK_USING, true);
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) { speed.removeModifier(SLOW); speed.addTransientModifier(new AttributeModifier(SLOW, -0.4, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)); }
        player.setSprinting(false); player.startUsingItem(hand); FlaskService.sync(player);
        return true;
    }
    private static boolean allowed(ServerPlayer p) {
        return p.isAlive() && !p.isSpectator() && !p.isPassenger() && !BonfireSessionService.isBusy(p)
                && !dev.maplesadventure.progression.status.StatusControlLockService.locked(p);
    }
    public static int kindId(ServerPlayer player) {
        Use use = ACTIVE.get(player.getUUID()); return use == null ? 0 : use.kind.ordinal() + 1;
    }
    public static void tick(MinecraftServer server) {
        // Run before new effect commits: the six increments occur on ticks 15 through 20.
        FlaskRecoveryService.tick();
        for (Use use : List.copyOf(ACTIVE.values())) {
            ServerPlayer p = use.player;
            if (p.hasDisconnected() || !allowed(p) || !p.level().dimension().location().equals(use.dimension)) {
                cancel(p, p.isAlive() && !p.hasDisconnected() && p.level().dimension().location().equals(use.dimension)
                        && dev.maplesadventure.progression.status.StatusControlLockService.locked(p)
                        ? FlaskCancelReason.STAGGER : FlaskCancelReason.INVALID_STATE); continue;
            }
            if (p.getItemInHand(use.hand) != use.stack) { cancel(p, FlaskCancelReason.ITEM_CHANGED); continue; }
            p.setSprinting(false);
            use.clock.tick();
            if (use.clock.claimEffect()) {
                var state = FlaskService.state(p);
                if (state.remaining(use.kind) <= 0) { cancel(p, FlaskCancelReason.INVALID_STATE); continue; }
                double amount = use.kind == FlaskKind.CRIMSON ? FlaskRules.health(state.potencyLevel()) : FlaskRules.mana(state.potencyLevel());
                var event = new FlaskEvents.Effect(p, use.kind, amount);
                if (!FlaskService.post(event) || event.isCanceled() || ACTIVE.get(p.getUUID()) != use) { cancel(p, FlaskCancelReason.INVALID_STATE); continue; }
                // Re-read after callbacks. Charge committed once; a post-effect interruption never refunds it.
                state = FlaskService.state(p);
                if (state.remaining(use.kind) == 0) { cancel(p, FlaskCancelReason.INVALID_STATE); continue; }
                FlaskService.write(p, state.consume(use.kind)); use.applied = true;
                FlaskRecoveryService.start(p, use.kind, event.amount());
                FlaskPresentation.effect(p, use.kind); FlaskService.sync(p);
            }
            if (use.clock.finished() && ACTIVE.get(p.getUUID()) == use) {
                cleanup(use); FlaskService.post(new FlaskEvents.Finish(p, use.kind));
            }
        }
    }
    public static void cancel(ServerPlayer player, FlaskCancelReason reason) {
        FlaskService.checkThread(player);
        // Animation interruption does not revoke an already committed effect.
        if (reason != FlaskCancelReason.DAMAGE && reason != FlaskCancelReason.STAGGER
                && reason != FlaskCancelReason.ITEM_CHANGED) FlaskRecoveryService.clear(player);
        Use use = ACTIVE.get(player.getUUID());
        if (use != null) { cleanup(use); FlaskService.post(new FlaskEvents.Cancel(player, use.kind, reason, use.applied)); }
    }
    private static void cleanup(Use use) {
        ACTIVE.remove(use.player.getUUID(), use);
        use.player.setData(ProgressionAttachments.FLASK_USING, false);
        var speed = use.player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SLOW);
        use.player.stopUsingItem();
        if (!use.player.hasDisconnected()) FlaskService.sync(use.player);
    }
    public static void clear() { for (Use use : List.copyOf(ACTIVE.values())) cleanup(use); ACTIVE.clear(); FlaskRecoveryService.clear(); }
    private FlaskUseController() {}
}
