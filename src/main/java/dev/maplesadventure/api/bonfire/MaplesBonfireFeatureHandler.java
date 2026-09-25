package dev.maplesadventure.api.bonfire;

import net.minecraft.resources.ResourceLocation;

/**
 * Register during mod initialization/common setup, before the first server starts.
 * Availability must be a side-effect-free query. Both callbacks run on the logical server thread.
 * Execution is authorized against the current RESTING session and current configuration, not a client flag.
 * Exceptions are logged and isolated. Do not retain the context or start another rest transaction.
 */
public interface MaplesBonfireFeatureHandler {
    ResourceLocation id();
    String translationKey();
    int order();
    boolean isAvailable(MaplesBonfireContext context);
    void execute(MaplesBonfireContext context);
}
