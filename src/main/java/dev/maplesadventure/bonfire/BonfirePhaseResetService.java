package dev.maplesadventure.bonfire;

import dev.maplesadventure.api.bonfire.*;
import dev.maplesadventure.multiplayer.encounter.EncounterResetReason;
import dev.maplesadventure.multiplayer.encounter.EncounterResetService;
import dev.maplesadventure.multiplayer.phase.PhaseManager;
import net.minecraft.resources.ResourceLocation;

/** Reset coordinator; never scans shared/prototype mobs or silently assumes ownership of addon entities. */
public final class BonfirePhaseResetService {
    static final ResourceLocation ENCOUNTERS = ResourceLocation.parse("maplesadventure:encounters");
    static MaplesBonfireRestResetParticipant encounters() {
        return new MaplesBonfireRestResetParticipant() {
            public ResourceLocation id() { return ENCOUNTERS; }
            public int priority() { return Integer.MIN_VALUE; }
            public void reset(MaplesBonfireContext context) {
                if (!PhaseManager.state(context.player()).phaseId().value().equals(context.phaseId())
                        || !EncounterResetService.resetForPlayer(context.player(), EncounterResetReason.BONFIRE))
                    throw new IllegalStateException("Bonfire phase owner changed before core reset");
            }
        };
    }
    public static boolean reset(MaplesBonfireContext context) {
        BonfireApiBridge.checkThread(context.player().server);
        return !BonfireApiBridge.REGISTRY.reset(context).contains(ENCOUNTERS);
    }
    private BonfirePhaseResetService() {}
}
