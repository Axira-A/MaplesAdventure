package dev.maplesadventure.multiplayer.encounter.boss;

import dev.maplesadventure.multiplayer.coop.CoopSessionManager;
import dev.maplesadventure.multiplayer.encounter.EncounterDefinition;
import dev.maplesadventure.multiplayer.encounter.EncounterSavedData;
import dev.maplesadventure.multiplayer.encounter.EncounterStatus;
import dev.maplesadventure.multiplayer.encounter.PhaseEncounterState;
import dev.maplesadventure.multiplayer.phase.PhaseId;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative fog-gate entry state for formal members of one boss attempt. */
public final class BossParticipantState {
    public static boolean initialize(MinecraftServer server, PhaseId phase, ResourceLocation encounterId,
                                     UUID enteringPlayer) {
        Context context = context(server, phase, encounterId);
        if (context == null) return false;
        context.attempt.setEntryState(enteringPlayer, BossParticipantEntryState.ENTERED);
        CoopSessionManager.sessionForPhase(phase).ifPresent(session -> {
            if (!session.hostUuid().equals(enteringPlayer))
                context.attempt.setEntryState(session.hostUuid(), BossParticipantEntryState.OUTSIDE);
            if (!session.cooperatorUuid().equals(enteringPlayer))
                context.attempt.setEntryState(session.cooperatorUuid(), BossParticipantEntryState.OUTSIDE);
        });
        context.data.changed();
        return true;
    }

    public static boolean markEntered(MinecraftServer server, PhaseId phase, ResourceLocation encounterId,
                                      ServerPlayer player) {
        Context context = context(server, phase, encounterId);
        if (context == null || !CoopSessionManager.isFormalMember(player, phase)) return false;
        context.attempt.setEntryState(player.getUUID(), BossParticipantEntryState.ENTERED);
        context.data.changed();
        return true;
    }

    public static BossParticipantEntryState entryState(MinecraftServer server, PhaseId phase,
                                                        ResourceLocation encounterId, UUID player) {
        Context context = context(server, phase, encounterId);
        return context == null ? BossParticipantEntryState.OUTSIDE : context.attempt.entryState(player);
    }

    public static boolean isEntered(MinecraftServer server, PhaseId phase, ResourceLocation encounterId, UUID player) {
        return entryState(server, phase, encounterId, player) == BossParticipantEntryState.ENTERED;
    }

    /** One-time migration for ACTIVE attempts saved before participant entry state existed. */
    public static void migrateLegacyAttempt(MinecraftServer server, PhaseId phase, ResourceLocation encounterId) {
        Context context = context(server, phase, encounterId);
        if (context == null || !context.attempt.participants().isEmpty()) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (CoopSessionManager.isFormalMember(player, phase))
                context.attempt.setEntryState(player.getUUID(), BossParticipantEntryState.ENTERED);
        }
        if (!context.attempt.participants().isEmpty()) context.data.changed();
    }

    private static Context context(MinecraftServer server, PhaseId phase, ResourceLocation encounterId) {
        EncounterSavedData data = EncounterSavedData.get(server);
        EncounterDefinition definition = data.definition(encounterId).orElse(null);
        PhaseEncounterState state = definition == null ? null : data.existingState(phase, encounterId).orElse(null);
        BossAttemptState attempt = state == null ? null : state.bossAttempt();
        if (state == null || state.status() != EncounterStatus.ACTIVE || attempt == null) return null;
        return new Context(data, attempt);
    }

    private record Context(EncounterSavedData data, BossAttemptState attempt) {}
    private BossParticipantState() {}
}
