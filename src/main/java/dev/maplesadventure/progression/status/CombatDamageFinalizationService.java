package dev.maplesadventure.progression.status;
import dev.maplesadventure.progression.weapon.*;
import dev.maplesadventure.progression.defense.LastWeaponDamageResolution;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** One MaplesAdventure Pre write, for both weapons and non-weapon frost vulnerability. */
public final class CombatDamageFinalizationService {
    public static void apply(LivingDamageEvent.Pre event) {
        double damage=event.getNewDamage();
        var context=WeaponDamagePolicy.context(event.getSource());
        if(context.isPresent()) {
            var resolution=WeaponCombatResolutionService.resolve(damage,context.get(),event.getEntity());
            damage=resolution.finalDamage();
            LastWeaponDamageResolution.record(context.get().owner(),event.getEntity().getUUID(),
                    dev.maplesadventure.progression.defense.EntityDefenseService.resolve(event.getEntity()).requestedId().toString(),resolution);
        }
        // Frost's initial burst is issued before the debuff is activated.
        damage*=StatusRuntimeService.damageTaken(event.getEntity());
        float result=(float)damage;
        if(result!=event.getNewDamage()) event.setNewDamage(result);
    }
    private CombatDamageFinalizationService() {}
}
