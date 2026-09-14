package dev.maplesadventure.multiplayer.coop;

import dev.maplesadventure.multiplayer.phase.PhaseManager;
import dev.maplesadventure.multiplayer.phase.PhaseRole;
import dev.maplesadventure.multiplayer.invasion.InvasionSessionManager;
import dev.maplesadventure.multiplayer.encounter.EncounterManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Server-thread transient signs. A claimed sign disappears before session creation can race. */
public final class SummonSignManager {
    private static final Map<UUID, SummonSignRecord> BY_ID = new LinkedHashMap<>();
    private static final Map<UUID, UUID> BY_OWNER = new LinkedHashMap<>();

    public static synchronized void toggle(ServerPlayer player) { toggle(player, SummonSignType.COOP); }

    public static synchronized void toggle(ServerPlayer player, SummonSignType type) {
        if (findOwner(player.getUUID(), type).isPresent()) remove(player, type);
        else place(player, type);
    }

    public static synchronized boolean place(ServerPlayer player, SummonSignType type) {
        if (!canPlace(player)) {
            player.displayClientMessage(Component.translatable("summon.maplesadventure.failure.not_solo"), true);
            return false;
        }
        SummonSignValidator.PlacementResult placement = SummonSignValidator.resolvePlacement(player);
        if (!placement.valid()) {
            player.displayClientMessage(Component.translatable("summon.maplesadventure.failure.invalid_surface"), true);
            return false;
        }
        removeOwner(player.server, player.getUUID());
        SummonSignRecord sign = new SummonSignRecord(
                UUID.randomUUID(), player.getUUID(), player.level().dimension(), placement.position(),
                placement.supportPos(), player.getYRot(), System.currentTimeMillis(), type, SummonSignState.AVAILABLE
        );
        BY_ID.put(sign.signId(), sign);
        BY_OWNER.put(sign.ownerUuid(), sign.signId());
        SummonSignSyncService.broadcastUpsert(player.serverLevel(), sign);
        player.displayClientMessage(Component.translatable(type == SummonSignType.COOP
                ? "summon.maplesadventure.sign.placed" : "duel.maplesadventure.sign.placed"), true);
        return true;
    }

    public static synchronized boolean canPlace(ServerPlayer player) {
        return PhaseManager.state(player).role() == PhaseRole.SOLO
                && !CoopSessionManager.hasSession(player.getUUID())
                && !InvasionSessionManager.hasSession(player.getUUID())
                && !PendingReturnSavedData.get(player.server).contains(player.getUUID())
                && !dev.maplesadventure.multiplayer.invasion.PendingInvasionReturnSavedData.get(player.server)
                .contains(player.getUUID())
                && !EncounterManager.hasActiveBossAttempt(player.server, PhaseManager.state(player).phaseId());
    }

    public static synchronized boolean remove(ServerPlayer player, SummonSignType type) {
        SummonSignRecord sign = findOwner(player.getUUID(), type).orElse(null);
        if (sign == null) return false;
        removeById(player.server, sign.signId());
        player.displayClientMessage(Component.translatable(type == SummonSignType.COOP
                ? "summon.maplesadventure.sign.removed" : "duel.maplesadventure.sign.removed"), true);
        return true;
    }

    public static synchronized Optional<SummonSignRecord> find(UUID signId) {
        return Optional.ofNullable(BY_ID.get(signId));
    }

    /** Administrative test support; gameplay clients never choose signs by owner. */
    public static synchronized Optional<SummonSignRecord> findOwner(UUID ownerUuid) {
        UUID signId = BY_OWNER.get(ownerUuid);
        return Optional.ofNullable(signId == null ? null : BY_ID.get(signId));
    }

    public static synchronized Optional<SummonSignRecord> findOwner(UUID ownerUuid, SummonSignType type) {
        return findOwner(ownerUuid).filter(sign -> sign.type() == type);
    }

    public static synchronized Optional<SummonSignRecord> claim(MinecraftServer server, UUID signId) {
        SummonSignRecord sign = BY_ID.get(signId);
        if (sign == null || sign.state() != SummonSignState.AVAILABLE) return Optional.empty();
        SummonSignRecord claimed = sign.claimed();
        BY_ID.put(signId, claimed);
        BY_OWNER.remove(sign.ownerUuid(), sign.signId());
        SummonSignSyncService.broadcastRemove(server.getLevel(sign.dimension()), sign);
        return Optional.of(claimed);
    }

    public static synchronized void consumeClaimed(UUID signId) {
        SummonSignRecord sign = BY_ID.get(signId);
        if (sign != null && sign.state() == SummonSignState.CLAIMED) BY_ID.remove(signId);
    }

    public static synchronized void removeOwner(MinecraftServer server, UUID ownerUuid) {
        UUID signId = BY_OWNER.remove(ownerUuid);
        if (signId != null) removeById(server, signId);
    }

    public static synchronized List<SummonSignRecord> nearby(ServerPlayer player, double radius) {
        double radiusSqr = radius * radius;
        ArrayList<SummonSignRecord> result = new ArrayList<>();
        for (SummonSignRecord sign : BY_ID.values()) {
            if (sign.state() == SummonSignState.AVAILABLE
                    && sign.dimension().equals(player.level().dimension())
                    && sign.position().distanceToSqr(player.position()) <= radiusSqr) result.add(sign);
        }
        return result;
    }

    public static synchronized void maintenance(MinecraftServer server) {
        for (SummonSignRecord sign : List.copyOf(BY_ID.values())) {
            ServerPlayer owner = server.getPlayerList().getPlayer(sign.ownerUuid());
            boolean invalid = owner == null || !owner.isAlive()
                    || !owner.level().dimension().equals(sign.dimension())
                    || PhaseManager.state(owner).role() != PhaseRole.SOLO
                    || CoopSessionManager.hasSession(owner.getUUID())
                    || InvasionSessionManager.hasSession(owner.getUUID())
                    || EncounterManager.hasActiveBossAttempt(server, PhaseManager.state(owner).phaseId());
            if (!invalid && owner.serverLevel().isLoaded(sign.supportPos())) {
                invalid = !SummonSignValidator.supportIsValid(owner.serverLevel(), sign.supportPos(), sign.position());
            }
            if (invalid) removeById(server, sign.signId());
        }
    }

    public static synchronized void clear() {
        BY_ID.clear();
        BY_OWNER.clear();
    }

    private static void removeById(MinecraftServer server, UUID signId) {
        SummonSignRecord removed = BY_ID.remove(signId);
        if (removed == null) return;
        BY_OWNER.remove(removed.ownerUuid(), signId);
        SummonSignSyncService.broadcastRemove(server.getLevel(removed.dimension()), removed);
    }

    private SummonSignManager() {}
}
