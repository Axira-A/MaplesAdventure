package dev.maplesadventure.progression.weapon;

import dev.maplesadventure.progression.*;

/** Pure shared math for combat, snapshots and complete level-up drafts. */
public final class WeaponAttackRatingCalculator {
    public record Rating(double baseAttack, double bonusAttack, double attackRating, double scalingFactor,
                         double effectiveScaling) {
        public double damageMultiplier() { return 1 + effectiveScaling; }
    }
    public static double contribution(PlayerAttributeState state, WeaponScalingProfile profile, Attribute attribute) {
        return profile.enabled() ? OffensiveScalingCurve.evaluate(state.get(attribute)) * profile.get(attribute) : 0;
    }
    public static double effectiveScaling(PlayerAttributeState state, WeaponScalingProfile profile) {
        return Math.clamp(factor(state, profile), 0, profile.maxBonus());
    }
    private static double factor(PlayerAttributeState state, WeaponScalingProfile profile) {
        double sum = 0;
        for (var attribute : WeaponRequirementProfile.ATTRIBUTES) sum += contribution(state,profile,attribute);
        if (!Double.isFinite(sum) || sum < 0) throw new IllegalArgumentException("Non-finite scaling");
        return sum;
    }
    public static Rating calculate(WeaponFacts facts, WeaponScalingProfile profile, PlayerAttributeState state) {
        return calculate(facts.damage(),profile,state);
    }
    public static WeaponDamageBundle calculate(double base,WeaponScalingProfile weapon,WeaponDamageProfile damage,
                                               PlayerAttributeState state,double requirement) {
        var channels=new java.util.EnumMap<WeaponDamageChannel,WeaponDamageBundle.ChannelAttack>(WeaponDamageChannel.class);
        double nominal=0;
        for(var component:damage.components()) {
            var scaling=component.scaling(weapon);
            // Reuse the Round 7 exact curve/cap calculation, including base==0 safely.
            var rating=calculate(base,scaling,state);
            channels.put(component.channel(),new WeaponDamageBundle.ChannelAttack(base*component.baseRatio(),rating.bonusAttack()*component.baseRatio()));
            nominal+=component.baseRatio()*rating.damageMultiplier();
        }
        return new WeaponDamageBundle(channels,base,Math.min(6,nominal),requirement);
    }
    public static Rating calculate(double base, WeaponScalingProfile profile, PlayerAttributeState state) {
        if (!Double.isFinite(base) || base < 0 || base > 10000) throw new IllegalArgumentException("Base attack bounds");
        double factor = factor(state,profile), effective = Math.clamp(factor,0,profile.maxBonus());
        double bonus = base * effective;
        return new Rating(base,bonus,base+bonus,factor,effective);
    }
    private WeaponAttackRatingCalculator() {}
}
