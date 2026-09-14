package dev.maplesadventure.progression.upgrade;

import net.minecraft.server.level.ServerPlayer;

/** Implemented by optional access adapters; formulas and transactions never depend on it. */
@FunctionalInterface
public interface UpgradeAccessValidator {
    boolean validate(ServerPlayer player, UpgradeAccessContext context);
}
