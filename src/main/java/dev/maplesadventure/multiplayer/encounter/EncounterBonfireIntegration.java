package dev.maplesadventure.multiplayer.encounter;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/** Stable MaplesAdventure boundary called only after Bonfires has committed a successful rest. */
public final class EncounterBonfireIntegration {
    private static final java.util.Map<ServerPlayer, Integer> LAST_REST = new java.util.WeakHashMap<>();
    public static boolean isLoaded() { return ModList.get().isLoaded("bonfires"); }
    public static void onSuccessfulRest(ServerPlayer player, net.minecraft.core.BlockPos pos) {
        Integer previous = LAST_REST.put(player, player.server.getTickCount());
        if (previous != null && previous == player.server.getTickCount()) return;
        // Check upgrade restrictions BEFORE rest resets an active encounter, not after.
        dev.maplesadventure.integration.bonfires.BonfiresUpgradeAdapter.onSuccessfulRest(player, pos);
        EncounterResetService.resetForPlayer(player, EncounterResetReason.BONFIRE);
    }
    private EncounterBonfireIntegration() {}
}
