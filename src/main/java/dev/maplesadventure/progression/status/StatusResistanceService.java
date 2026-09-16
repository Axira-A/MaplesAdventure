package dev.maplesadventure.progression.status;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
public final class StatusResistanceService {
    public static StatusResistance resolve(LivingEntity target,StatusEffectType type) {
        if(target instanceof Player) return new StatusResistance(dev.maplesadventure.config.StatusConfig.PLAYER_THRESHOLDS.get(type).get(),false,1);
        return dev.maplesadventure.progression.defense.EntityDefenseService.resolve(target).profile().statusResistances().getOrDefault(type,StatusResistance.DEFAULT);
    }
    private StatusResistanceService() {}
}
