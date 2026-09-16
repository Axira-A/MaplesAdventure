package dev.maplesadventure.progression.defense;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import dev.maplesadventure.config.*;

public final class TargetDefenseResolver {
    public record Resolved(ChannelDefenseView view, double pressure, String source) {}
    public static Resolved resolve(LivingEntity target) {
        if (target instanceof ServerPlayer player) {
            boolean enabled = PlayerDefenseConfig.ENABLED.get();
            return new Resolved(enabled ? PlayerDefenseService.snapshot(player) : EntityDefenseProfile.NONE,
                    PlayerDefenseConfig.PRESSURE.get(), enabled ? "PLAYER_BUILD" : "PLAYER_BUILD_DISABLED");
        }
        var enemy = EntityDefenseService.resolve(target);
        return new Resolved(enemy.profile(), EnemyDefenseConfig.DEFENSE_PRESSURE.get(), enemy.requestedId().toString());
    }
    private TargetDefenseResolver() {}
}
