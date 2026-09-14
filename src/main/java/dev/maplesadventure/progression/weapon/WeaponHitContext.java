package dev.maplesadventure.progression.weapon;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

/** Future resistance consumes this bundle in the existing single LivingDamage event. */
public record WeaponHitContext(ResourceLocation usedWeapon,WeaponDamageBundle bundle,WeaponRequirementResult requirements,
                               boolean projectileSnapshot,UUID owner) {
    public double effectiveMultiplier() { return bundle.effectiveMultiplier(); }
}
