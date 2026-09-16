package dev.maplesadventure.progression.status;
import dev.maplesadventure.progression.weapon.*;
import dev.maplesadventure.progression.defense.LastWeaponDamageResolution;
import dev.maplesadventure.progression.defense.*;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** One MaplesAdventure Pre write, for both weapons and non-weapon frost vulnerability. */
public final class CombatDamageFinalizationService {
    public static void apply(LivingDamageEvent.Pre event) {
        double damage=event.getNewDamage();
        var context=WeaponDamagePolicy.context(event.getSource());
        CombatHitLifecycle.prepared(event.getEntity().getUUID(), event.getSource(), context);
        var typed=TypedDamageProviderRegistry.resolve(event.getEntity(),event.getSource(),damage,context);
        // Generic PvE is introduced for players only; non-weapon enemy behavior stays Round 10-compatible.
        if(typed.isPresent() && (context.isPresent() || event.getEntity() instanceof net.minecraft.server.level.ServerPlayer)) {
            var defense=TargetDefenseResolver.resolve(event.getEntity());
            var resolution=WeaponCombatResolutionService.resolve(damage,typed.get(),defense.view(),defense.pressure());
            damage=resolution.finalDamage();
            var owner=context.map(WeaponHitContext::owner).orElseGet(()->event.getSource().getEntity()==null?null:event.getSource().getEntity().getUUID());
            LastWeaponDamageResolution.record(owner,event.getEntity().getUUID(),defense.source(),resolution,
                    typed.get().sourceKind(),defense.pressure(),StatusRuntimeService.damageTaken(event.getEntity()));
        }
        // Frost's initial burst is issued before the debuff is activated.
        damage*=StatusRuntimeService.damageTaken(event.getEntity());
        float result=(float)damage;
        if(result!=event.getNewDamage()) event.setNewDamage(result);
    }
    private CombatDamageFinalizationService() {}
}
