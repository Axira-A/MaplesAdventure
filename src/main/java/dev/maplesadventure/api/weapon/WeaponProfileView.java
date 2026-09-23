package dev.maplesadventure.api.weapon;

import java.util.*;
import net.minecraft.resources.ResourceLocation;

/** Detached resolved stack profile after datapacks, integration and infusion, before player attributes. */
public record WeaponProfileView(ResourceLocation itemId, double baseAttack,
        WeaponRequirementView requirements, WeaponScalingView scaling,
        List<WeaponDamageComponentView> damageComponents, List<WeaponStatusComponentView> statuses,
        WeaponInfusionInfo infusion, Set<ResourceLocation> allowedInfusions,
        Optional<MaplesWeaponWeightClass> weightClass) {
    public WeaponProfileView {
        Objects.requireNonNull(itemId); Objects.requireNonNull(requirements); Objects.requireNonNull(scaling);
        Objects.requireNonNull(infusion); Objects.requireNonNull(weightClass);
        if (!Double.isFinite(baseAttack) || baseAttack < 0 || baseAttack > 10000) throw new IllegalArgumentException("Base attack bounds");
        damageComponents = List.copyOf(damageComponents); statuses = List.copyOf(statuses);
        allowedInfusions = Set.copyOf(allowedInfusions);
        if (damageComponents.isEmpty() || damageComponents.size() > 8 || statuses.size() > 8 || allowedInfusions.size() > 32)
            throw new IllegalArgumentException("Profile component bounds");
    }
}
