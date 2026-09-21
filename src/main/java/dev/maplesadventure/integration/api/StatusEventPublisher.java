package dev.maplesadventure.integration.api;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import dev.maplesadventure.api.event.*;
import dev.maplesadventure.api.status.*;
import dev.maplesadventure.progression.status.*;

/** Internal notifications for every authoritative source, not just facade callers. */
public final class StatusEventPublisher {
    public static StatusView view(LivingEntity target,StatusEffectType type) {
        return StatusApiBridge.snapshot(target,MaplesStatusType.valueOf(type.name()));
    }
    public static void applied(LivingEntity target,StatusView before,StatusSourceContext source,double amount,boolean proc) {
        var after=StatusApiBridge.snapshot(target,before.type());
        var owner=Optional.ofNullable(source.attackerUUID());
        ApiNotifications.post(new StatusBuildupEvent(target,before,after,owner,amount));
        if(proc) ApiNotifications.post(new StatusProcEvent(target,before,after,owner));
    }
    public static void cleared(LivingEntity target,StatusView before,StatusClearEvent.Reason reason) {
        var after=StatusApiBridge.snapshot(target,before.type());
        // Removing an already-empty internal entry is not another externally visible cure.
        if(!before.equals(after)) ApiNotifications.post(new StatusClearEvent(target,before,after,reason));
    }
    private StatusEventPublisher() {}
}
