package dev.maplesadventure.api.bonfire;

import net.minecraft.resources.ResourceLocation;

/**
 * Server-thread reset extension for explicitly owned world state. Never reset shared entities or another
 * phase. Lower priority runs first; ties use lexical ID order. Exceptions are logged and isolated.
 * Register during initialization/common setup, before the first server starts.
 */
public interface MaplesBonfireRestResetParticipant {
    ResourceLocation id();
    int priority();
    void reset(MaplesBonfireContext context);
}
