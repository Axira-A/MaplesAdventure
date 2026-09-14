package dev.maplesadventure.multiplayer.encounter.boss;

import dev.maplesadventure.multiplayer.coop.CoopSessionManager;
import dev.maplesadventure.multiplayer.encounter.EncounterDefinition;
import dev.maplesadventure.multiplayer.encounter.EncounterSavedData;
import dev.maplesadventure.multiplayer.encounter.EncounterStatus;
import dev.maplesadventure.multiplayer.encounter.EncounterType;
import dev.maplesadventure.multiplayer.encounter.PhaseEncounterKey;
import dev.maplesadventure.multiplayer.phase.PhaseId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Runtime-only, phase-scoped boss bars. Attempt SavedData remains the authority. */
public final class PhaseBossBarService {
    private static final Map<Key, ServerBossEvent> BARS = new HashMap<>();

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 5 != 0) return;
        EncounterSavedData data = EncounterSavedData.get(server);
        Set<Key> active = new HashSet<>();
        for (var entry : data.runtimeSnapshot().entrySet()) {
            EncounterDefinition definition = data.definition(entry.getKey().encounterId()).orElse(null);
            var state = entry.getValue();
            BossAttemptState attempt = state.bossAttempt();
            if (definition == null || definition.type() != EncounterType.BOSS
                    || state.status() != EncounterStatus.ACTIVE || attempt == null || !attempt.hasPrimary()) continue;
            ServerLevel level = server.getLevel(definition.dimension());
            Entity entity = level == null ? null : level.getEntity(attempt.primaryBossUuid());
            if (!(entity instanceof LivingEntity boss) || !boss.isAlive()) continue;
            BossParticipantState.migrateLegacyAttempt(server, entry.getKey().phaseId(), definition.encounterId());
            Key key = new Key(entry.getKey().phaseId(), definition.encounterId(), attempt.attemptId());
            active.add(key);
            ServerBossEvent bar = BARS.computeIfAbsent(key, ignored -> new ServerBossEvent(
                    boss.getDisplayName(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS));
            bar.setName(boss.getDisplayName());
            bar.setProgress(Mth.clamp(boss.getHealth() / Math.max(1.0F, boss.getMaxHealth()), 0.0F, 1.0F));
            syncViewers(server, level, entry.getKey().phaseId(), definition.encounterId(), bar);
        }
        BARS.entrySet().removeIf(entry -> {
            if (active.contains(entry.getKey())) return false;
            entry.getValue().removeAllPlayers();
            return true;
        });
    }

    public static void remove(PhaseId phase, net.minecraft.resources.ResourceLocation encounterId) {
        BARS.entrySet().removeIf(entry -> {
            if (!entry.getKey().phase.equals(phase) || !entry.getKey().encounterId.equals(encounterId)) return false;
            entry.getValue().removeAllPlayers();
            return true;
        });
    }

    public static void clear() {
        BARS.values().forEach(ServerBossEvent::removeAllPlayers);
        BARS.clear();
    }

    private static void syncViewers(MinecraftServer server, ServerLevel bossLevel, PhaseId phase,
                                    net.minecraft.resources.ResourceLocation encounterId, ServerBossEvent bar) {
        Set<ServerPlayer> desired = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isAlive() && !player.isSpectator() && player.level() == bossLevel
                    && CoopSessionManager.isFormalMember(player, phase)
                    && BossParticipantState.isEntered(server, phase, encounterId, player.getUUID())) desired.add(player);
        }
        for (ServerPlayer current : Set.copyOf(bar.getPlayers())) if (!desired.contains(current)) bar.removePlayer(current);
        for (ServerPlayer player : desired) if (!bar.getPlayers().contains(player)) bar.addPlayer(player);
    }

    private record Key(PhaseId phase, net.minecraft.resources.ResourceLocation encounterId, UUID attemptId) {}
    private PhaseBossBarService() {}
}
