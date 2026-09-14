package dev.maplesadventure.progression;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.config.ProgressionConfig;
import dev.maplesadventure.multiplayer.phase.PhaseManager;
import dev.maplesadventure.multiplayer.phase.PhaseRole;
import dev.maplesadventure.soul.ExperiencePoints;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import dev.maplesadventure.progression.runtime.DerivedStatRefreshReason;
import dev.maplesadventure.progression.runtime.DerivedStatRuntimeService;
import dev.maplesadventure.progression.encumbrance.EncumbranceRuntimeService;

/** Sole server mutation boundary for player-owned progression. */
public final class PlayerAttributeService {
    private static final Set<UUID> REPORTED_CORRECTIONS = ConcurrentHashMap.newKeySet();

    public static PlayerAttributeState state(ServerPlayer player) {
        PlayerAttributeState state = player.getData(ProgressionAttachments.PLAYER_ATTRIBUTES);
        int cap = hardCap();
        boolean exceedsConfiguredCap = state.values().values().stream().anyMatch(value -> value > cap);
        if (!state.correctedOnLoad() && !exceedsConfiguredCap) return state;
        if (REPORTED_CORRECTIONS.add(player.getUUID())) {
            MaplesAdventure.LOGGER.warn("Corrected progression data for {} from dataVersion={} to dataVersion={} hardCap={}",
                    player.getGameProfile().getName(), state.sourceVersion(),
                    PlayerAttributeMigration.CURRENT_VERSION, cap);
        }
        PlayerAttributeState normalized = state.clampToCap(cap);
        player.setData(ProgressionAttachments.PLAYER_ATTRIBUTES, normalized);
        return normalized;
    }

    public static int get(ServerPlayer player, Attribute attribute) { return state(player).get(attribute); }
    public static int level(ServerPlayer player) { return AttributeProgression.level(state(player)); }
    public static long nextLevelCost(ServerPlayer player) { return AttributeProgression.costForNextLevel(level(player)); }

    public static boolean canIncrease(ServerPlayer player, Attribute attribute, UpgradeContext context) {
        return contextAllowed(player, context) && get(player, attribute) < hardCap();
    }

    /** Administrative mutation; this never grants or refunds XP. */
    public static PlayerAttributeState set(ServerPlayer player, Attribute attribute, int value,
                                           UpgradeContext context) {
        if (context != UpgradeContext.ADMIN) return state(player);
        PlayerAttributeState before = state(player);
        PlayerAttributeState after = before.with(attribute, value, hardCap());
        if (after.get(attribute) != before.get(attribute)) commit(player, after);
        return after;
    }

    /** Administrative mutation; this never grants or refunds XP. */
    public static PlayerAttributeState add(ServerPlayer player, Attribute attribute, int amount,
                                           UpgradeContext context) {
        long requested = (long) get(player, attribute) + amount;
        int bounded = requested > Integer.MAX_VALUE ? Integer.MAX_VALUE
                : requested < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) requested;
        return set(player, attribute, bounded, context);
    }

    public static PlayerAttributeState reset(ServerPlayer player, UpgradeContext context) {
        if (context != UpgradeContext.ADMIN) return state(player);
        PlayerAttributeState reset = PlayerAttributeState.defaultsState();
        commit(player, reset);
        return reset;
    }

    public static UpgradeResult increase(ServerPlayer player, Attribute attribute, UpgradeContext context) {
        return upgradeOne(player, attribute, context);
    }

    public static UpgradeResult upgradeOne(ServerPlayer player, Attribute attribute, UpgradeContext context) {
        PlayerAttributeState before = state(player);
        int xpBefore = ExperiencePoints.capture(player);
        if (context == UpgradeContext.ACCESS || !contextAllowed(player, context))
            return result(UpgradeResult.Status.INVALID_CONTEXT, attribute, 0L, xpBefore, xpBefore, before);
        if (before.get(attribute) >= hardCap())
            return result(UpgradeResult.Status.AT_CAP, attribute, 0L, xpBefore, xpBefore, before);

        long cost = AttributeProgression.costForNextLevel(AttributeProgression.level(before));
        if (cost < 0L || cost > Integer.MAX_VALUE)
            return result(UpgradeResult.Status.COST_OUT_OF_RANGE, attribute, cost, xpBefore, xpBefore, before);
        int verifiedExperience = ExperiencePoints.capture(player);
        if (verifiedExperience < cost)
            return result(UpgradeResult.Status.INSUFFICIENT_EXPERIENCE, attribute, cost,
                    verifiedExperience, verifiedExperience, before);

        int remaining = verifiedExperience - (int) cost;
        PlayerAttributeState after = before.with(attribute, before.get(attribute) + 1, hardCap());
        try {
            paidCommit(player, verifiedExperience, before, remaining, after);
            return result(UpgradeResult.Status.SUCCESS, attribute, cost, verifiedExperience, remaining, after);
        } catch (RuntimeException failure) {
            MaplesAdventure.LOGGER.error("Rolled back failed attribute upgrade for {}", player.getUUID(), failure);
            return result(UpgradeResult.Status.TRANSACTION_FAILED, attribute, cost,
                    verifiedExperience, verifiedExperience, before);
        }
    }

    public static AttributeSnapshot snapshot(ServerPlayer player) { return AttributeSnapshot.of(player); }

    /** A single debit and attachment write, after validating the unforgeable server rest capability. */
    public static BatchUpgradeStatus upgradeBatch(ServerPlayer player, java.util.Map<Attribute, Integer> deltas,
            UpgradeContext context, dev.maplesadventure.progression.upgrade.UpgradeSession session,
            long baselineRevision) {
        if (context != UpgradeContext.ACCESS) return BatchUpgradeStatus.INVALID_CONTEXT;
        var authorization = dev.maplesadventure.progression.upgrade.UpgradeAccessService
                .validateForUpgrade(player, session, baselineRevision);
        if (authorization != BatchUpgradeStatus.SUCCESS) return authorization;
        PlayerAttributeState before = state(player);
        int oldXp = ExperiencePoints.capture(player);
        if (deltas == null || deltas.isEmpty() || deltas.size() > Attribute.values().length)
            return BatchUpgradeStatus.INVALID_DELTA;
        for (var entry : deltas.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() < 0 || entry.getValue() > 94)
                return BatchUpgradeStatus.INVALID_DELTA;
            if ((long) before.get(entry.getKey()) + entry.getValue() > hardCap()) return BatchUpgradeStatus.AT_CAP;
        }
        var plan = LevelUpPreviewCalculator.calculate(before, deltas, oldXp, hardCap(),
                ProgressionConfig.XP_COST_MULTIPLIER.get());
        if (plan.points() == 0) return BatchUpgradeStatus.INVALID_DELTA;
        if (plan.totalCost() > Integer.MAX_VALUE || plan.remainingXp() < 0)
            return BatchUpgradeStatus.INSUFFICIENT_EXPERIENCE;
        try {
            paidCommit(player, oldXp, before, (int) plan.remainingXp(), plan.state());
            return BatchUpgradeStatus.SUCCESS;
        } catch (RuntimeException failure) {
            MaplesAdventure.LOGGER.error("Rolled back batch upgrade for {}", player.getUUID(), failure);
            return BatchUpgradeStatus.TRANSACTION_FAILED;
        }
    }

    private static void syncExperience(ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(
                player.experienceProgress, player.totalExperience, player.experienceLevel));
    }

    private static void paidCommit(ServerPlayer player, int oldXp, PlayerAttributeState before,
                                   int newXp, PlayerAttributeState after) {
        AttributeUpgradeTransaction.commit(new AttributeUpgradeTransaction.Writer() {
            public void experience(int points) { ExperiencePoints.setExact(player, points); }
            public void attributes(PlayerAttributeState state) {
                player.setData(ProgressionAttachments.PLAYER_ATTRIBUTES, state);
                DerivedStatRuntimeService.refresh(player, DerivedStatRefreshReason.ATTRIBUTE_CHANGE);
                EncumbranceRuntimeService.refresh(player);
            }
            public void sync() { AttributeSyncService.sync(player); syncExperience(player); }
        }, oldXp, before, newXp, after);
    }

    static void commit(ServerPlayer player, PlayerAttributeState state) {
        player.setData(ProgressionAttachments.PLAYER_ATTRIBUTES, state.cleanCopy());
        DerivedStatRuntimeService.refresh(player, DerivedStatRefreshReason.ATTRIBUTE_CHANGE);
        EncumbranceRuntimeService.refresh(player);
        AttributeSyncService.sync(player);
    }

    static void forget(UUID playerId) { REPORTED_CORRECTIONS.remove(playerId); }
    static void clearTransientState() { REPORTED_CORRECTIONS.clear(); }

    private static int hardCap() {
        return Math.clamp(ProgressionConfig.ATTRIBUTE_HARD_CAP.get(), PlayerAttributeMigration.MINIMUM_VALUE,
                PlayerAttributeMigration.ABSOLUTE_HARD_CAP);
    }

    private static boolean contextAllowed(ServerPlayer player, UpgradeContext context) {
        if (context == null || !player.isAlive() || player.isSpectator()) return false;
        if (context == UpgradeContext.ADMIN) return true;
        PhaseRole role = PhaseManager.state(player).role();
        return role == PhaseRole.SOLO || role == PhaseRole.HOST;
    }

    private static UpgradeResult result(UpgradeResult.Status status, Attribute attribute, long cost,
                                        int before, int after, PlayerAttributeState state) {
        return new UpgradeResult(status, attribute, cost, before, after, state.cleanCopy());
    }

    private PlayerAttributeService() {}
}
