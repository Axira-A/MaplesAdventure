package dev.maplesadventure.api.flask;

import dev.maplesadventure.flask.*;
import dev.maplesadventure.progression.ProgressionAttachments;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

/** Stable read-only snapshots. Mutating operations require the logical server thread. */
public final class FlaskApi {
    public static boolean isUsingFlask(LivingEntity player) { return player.getExistingData(ProgressionAttachments.FLASK_USING).orElse(false); }
    public static FlaskSnapshot getState(Player player) {
        return player instanceof ServerPlayer server ? FlaskService.state(server) : player.getData(ProgressionAttachments.FLASK).value();
    }
    public static int getCrimsonRemaining(Player p) { return getState(p).crimsonRemaining(); }
    public static int getCrimsonAllocated(Player p) { return getState(p).crimsonAllocated(); }
    public static int getAshenRemaining(Player p) { return getState(p).ashenRemaining(); }
    public static int getAshenAllocated(Player p) { return getState(p).ashenAllocated(); }
    public static int getPotencyLevel(Player p) { return getState(p).potencyLevel(); }
    public static int getTotalCapacity(Player p) { return getState(p).totalCapacity(); }
    public static double getCrimsonHealAmount(Player p) { return FlaskRules.health(getPotencyLevel(p)); }
    public static double getAshenRestoreAmount(Player p) { return FlaskRules.mana(getPotencyLevel(p)); }
    public static void cancelUse(ServerPlayer p, FlaskCancelReason reason) { FlaskService.checkThread(p); FlaskUseController.cancel(p, reason); }
    public static void refill(ServerPlayer p, FlaskRechargeReason reason) { FlaskService.checkThread(p); FlaskService.refill(p, reason); }
    private FlaskApi() {}
}
