package dev.maplesadventure.progression.weapon;

import net.minecraft.resources.ResourceLocation;

/** Nominal attack rating is deliberately NOT multiplied by requirement efficiency. */
public record WeaponAttackSnapshot(ResourceLocation itemId, double baseAttack, double scalingBonus,
        double attackRating, double scalingFactor, double effectiveScaling, WeaponScalingProfile profile,
        WeaponRequirementResult requirements,WeaponDamageBundle bundle,WeaponDamageProfile damageProfile) {
    public WeaponAttackSnapshot {
        for (double value : new double[]{baseAttack,scalingBonus,attackRating,scalingFactor,effectiveScaling})
            if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Invalid attack snapshot");
    }
    public double effectiveRequirementMultiplier() { return requirements.damageMultiplier(); }
    public static WeaponAttackSnapshot of(ResourceLocation item, WeaponAttackRatingCalculator.Rating r,
            WeaponScalingProfile profile, WeaponRequirementResult requirements) {
        var bundle=new WeaponDamageBundle(java.util.Map.of(WeaponDamageChannel.PHYSICAL,new WeaponDamageBundle.ChannelAttack(r.baseAttack(),r.bonusAttack())),r.baseAttack(),r.damageMultiplier(),requirements.damageMultiplier());
        return of(item,bundle,profile,WeaponDamageProfile.STANDARD,requirements);
    }
    public static WeaponAttackSnapshot of(ResourceLocation item,WeaponDamageBundle bundle,WeaponScalingProfile scaling,
                                           WeaponDamageProfile damage,WeaponRequirementResult requirements) {
        double bonusRatio=bundle.totalBase()==0?0:bundle.totalScalingBonus()/bundle.totalBase();
        return new WeaponAttackSnapshot(item,bundle.totalBase(),bundle.totalScalingBonus(),bundle.totalAttackRating(),bonusRatio,bonusRatio,scaling,requirements,bundle,damage);
    }
}
