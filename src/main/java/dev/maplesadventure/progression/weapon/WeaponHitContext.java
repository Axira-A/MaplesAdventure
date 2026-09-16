package dev.maplesadventure.progression.weapon;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

/** Future resistance consumes this bundle in the existing single LivingDamage event. */
public record WeaponHitContext(ResourceLocation usedWeapon,WeaponDamageBundle bundle,WeaponRequirementResult requirements,
                               boolean projectileSnapshot,UUID owner,dev.maplesadventure.progression.status.StatusBuildupSnapshot statuses) {
    public WeaponHitContext(ResourceLocation w,WeaponDamageBundle b,WeaponRequirementResult r,boolean p,UUID owner) {
        this(w,b,r,p,owner,dev.maplesadventure.progression.status.StatusBuildupSnapshot.EMPTY);
    }
    public double effectiveMultiplier() { return bundle.effectiveMultiplier(); }
}
