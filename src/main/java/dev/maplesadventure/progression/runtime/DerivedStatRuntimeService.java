package dev.maplesadventure.progression.runtime;

import dev.maplesadventure.progression.PlayerAttributeService;
import dev.maplesadventure.progression.PlayerAttributeState;
import dev.maplesadventure.progression.ProgressionAttachments;
import net.minecraft.server.level.ServerPlayer;

/** Event-driven, idempotent server authority for all implemented derived resources. */
public final class DerivedStatRuntimeService {
    public static RuntimeResourceSnapshot refresh(ServerPlayer player, DerivedStatRefreshReason reason) {
        PlayerAttributeState state = PlayerAttributeService.state(player);
        dev.maplesadventure.progression.spell.SpellScalingRuntimeService.refresh(player);
        RuntimeResourceSnapshot result = new RuntimeResourceSnapshot(
                DerivedStatIntegrationRegistry.refresh(DerivedRuntimeResource.HEALTH, player, state, reason),
                DerivedStatIntegrationRegistry.refresh(DerivedRuntimeResource.MANA, player, state, reason),
                DerivedStatIntegrationRegistry.refresh(DerivedRuntimeResource.STAMINA, player, state, reason));
        if (reason == DerivedStatRefreshReason.LOGIN) restoreLoginCheckpoint(player);
        return result;
    }

    public static RuntimeResourceSnapshot snapshot(ServerPlayer player) {
        PlayerAttributeState state = PlayerAttributeService.state(player);
        return new RuntimeResourceSnapshot(
                DerivedStatIntegrationRegistry.inspect(DerivedRuntimeResource.HEALTH, player, state),
                DerivedStatIntegrationRegistry.inspect(DerivedRuntimeResource.MANA, player, state),
                DerivedStatIntegrationRegistry.inspect(DerivedRuntimeResource.STAMINA, player, state));
    }

    /** Bonfire success only: never changes maximums or invents unavailable mod resources. */
    public static ResourceRestoreResult restoreToMaximum(ServerPlayer player) {
        double before = player.getHealth();
        player.setHealth(player.getMaxHealth());
        return new ResourceRestoreResult(ResourceRestoreResult.Value.measured(before, player.getHealth(), player.getMaxHealth()),
                DerivedStatIntegrationRegistry.restoreMaximum(DerivedRuntimeResource.MANA, player),
                DerivedStatIntegrationRegistry.restoreMaximum(DerivedRuntimeResource.STAMINA, player));
    }

    public static void clearTransientState() { DerivedStatIntegrationRegistry.clearFailures(); }

    public static void captureLogoutCheckpoint(ServerPlayer player) {
        PlayerResourceCheckpoint checkpoint = new PlayerResourceCheckpoint();
        // Vanilla health already persists exactly. These checkpoints cover optional mods whose
        // player runtime initializes current resources to max on a new login entity.
        for (DerivedRuntimeResource resource : new DerivedRuntimeResource[]{
                DerivedRuntimeResource.MANA, DerivedRuntimeResource.STAMINA}) {
            DerivedStatIntegrationRegistry.captureCurrentRatio(resource, player)
                    .ifPresent(ratio -> checkpoint.put(resource, ratio));
        }
        player.setData(ProgressionAttachments.RESOURCE_CHECKPOINT, checkpoint);
    }

    private static void restoreLoginCheckpoint(ServerPlayer player) {
        player.getExistingData(ProgressionAttachments.RESOURCE_CHECKPOINT).ifPresent(checkpoint -> {
            for (var entry : checkpoint.values().entrySet())
                DerivedStatIntegrationRegistry.restoreCurrentRatio(entry.getKey(), player, entry.getValue());
            // Consume once: later dimension/session refreshes must preserve the live runtime value.
            player.setData(ProgressionAttachments.RESOURCE_CHECKPOINT, new PlayerResourceCheckpoint());
        });
    }
    private DerivedStatRuntimeService() {}
}
