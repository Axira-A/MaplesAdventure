package dev.maplesadventure.progression.stamina;

import net.minecraft.server.level.ServerPlayer;

/** Optional combat-mod bridge. Core progression never links directly against Epic Fight classes. */
public interface StaminaRuntimeBridge {
    boolean available(ServerPlayer player);
    double current(ServerPlayer player);
    double maximum(ServerPlayer player);
    double regenMultiplier(ServerPlayer player);
    void setCurrent(ServerPlayer player, double value);

    /** True while the current combat animation/state should suppress stamina regeneration. */
    boolean actionBlocksRegen(ServerPlayer player);
}
