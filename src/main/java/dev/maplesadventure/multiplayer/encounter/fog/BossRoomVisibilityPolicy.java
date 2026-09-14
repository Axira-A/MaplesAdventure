package dev.maplesadventure.multiplayer.encounter.fog;

import dev.maplesadventure.multiplayer.encounter.EncounterSavedData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** @deprecated Boss UI now uses persisted per-attempt fog-gate entry state. */
@Deprecated(forRemoval = false)
public final class BossRoomVisibilityPolicy {
    public static boolean isViewerInsideBossRoom(ServerPlayer player, ResourceLocation encounterId) {
        var definition = EncounterSavedData.get(player.server).definition(encounterId).orElse(null);
        return definition != null && definition.dimension().equals(player.level().dimension())
                && dev.maplesadventure.multiplayer.encounter.boss.BossParticipantState.isEntered(
                player.server, dev.maplesadventure.multiplayer.phase.PhaseManager.state(player).phaseId(),
                encounterId, player.getUUID());
    }
    private BossRoomVisibilityPolicy() {}
}
