package dev.maplesadventure.progression.status;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import dev.maplesadventure.progression.ProgressionAttachments;

public final class StatusResistanceCorrectionService {
    public static double offset(LivingEntity entity,StatusEffectType type,StatusResistance base) {
        if(entity instanceof Player) return 0;
        var state=entity.getExistingData(ProgressionAttachments.STATUS_RUNTIME).orElse(null);
        return StatusResistanceCorrections.get(base.correction()).offset(state==null?0:state.procCount(type));
    }
    public static void reset(LivingEntity entity) {
        entity.getExistingData(ProgressionAttachments.STATUS_RUNTIME).ifPresent(StatusRuntimeState::resetCorrections);
    }
    private StatusResistanceCorrectionService() {}
}
