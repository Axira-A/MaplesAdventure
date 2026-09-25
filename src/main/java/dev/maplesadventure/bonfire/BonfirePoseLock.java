package dev.maplesadventure.bonfire;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Temporary pose ownership, never a persisted invulnerable/noPhysics flag. */
public final class BonfirePoseLock {
    public static boolean locked(Entity entity) {
        if (entity instanceof ServerPlayer player) return BonfireSessionService.isBusy(player);
        return entity.level().isClientSide() && entity instanceof Player player && player.isLocalPlayer()
                && dev.maplesadventure.client.bonfire.BonfireClient.busy();
    }

    public static void stopMotion(Player player) {
        player.setDeltaMovement(Vec3.ZERO);
        player.setSprinting(false);
        player.fallDistance = 0;
    }

    private BonfirePoseLock() {}
}
