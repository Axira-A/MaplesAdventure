package dev.maplesadventure.flask;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.api.flask.FlaskKind;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Only pending, already-paid effects. Never persisted or transferred to a cloned player. */
final class FlaskRecoveryService {
    private record Recovery(ServerPlayer player, ResourceLocation dimension, FlaskKind kind,
                            FlaskRecoveryTimeline timeline) {}
    private static final List<Recovery> PENDING = new ArrayList<>();

    static void start(ServerPlayer player, FlaskKind kind, double amount) {
        PENDING.add(new Recovery(player, player.level().dimension().location(), kind,
                new FlaskRecoveryTimeline(amount)));
    }

    static void tick() {
        for (Recovery recovery : List.copyOf(PENDING)) {
            ServerPlayer player = recovery.player();
            if (!player.isAlive() || player.isRemoved() || player.hasDisconnected()
                    || !recovery.dimension().equals(player.level().dimension().location())) {
                PENDING.remove(recovery);
                continue;
            }
            double amount = recovery.timeline().nextAmount();
            try {
                if (recovery.kind() == FlaskKind.CRIMSON)
                    player.setHealth((float) Math.min(player.getMaxHealth(), player.getHealth() + amount));
                else FlaskManaBridge.restore(player, amount);
            } catch (RuntimeException | LinkageError error) {
                // Never retry an adapter which may have restored resources before throwing.
                PENDING.remove(recovery);
                MaplesAdventure.LOGGER.error("Flask gradual restoration failed for {}", player.getUUID(), error);
            }
            if (recovery.timeline().finished()) PENDING.remove(recovery);
        }
    }

    static void clear(ServerPlayer player) { PENDING.removeIf(recovery -> recovery.player() == player); }
    static void clear() { PENDING.clear(); }
    private FlaskRecoveryService() {}
}
