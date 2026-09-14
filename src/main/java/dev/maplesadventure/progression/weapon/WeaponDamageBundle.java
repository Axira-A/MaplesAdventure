package dev.maplesadventure.progression.weapon;

import java.util.*;

/** Immutable composition of ONE hit. No resistance, hurt calls or requirement penalty inside AR. */
public record WeaponDamageBundle(Map<WeaponDamageChannel, ChannelAttack> channels,double weaponBaseAttack,
                                 double nominalMultiplier,double requirementMultiplier) {
    public record ChannelAttack(double base,double scalingBonus) {
        public ChannelAttack {
            if(!Double.isFinite(base)||base<0||base>20000||!Double.isFinite(scalingBonus)||scalingBonus<0||scalingBonus>base*2+1e-9)
                throw new IllegalArgumentException("Channel AR bounds");
        }
        public double attackRating() { return base+scalingBonus; }
    }
    public WeaponDamageBundle {
        var copy=new EnumMap<WeaponDamageChannel,ChannelAttack>(WeaponDamageChannel.class); copy.putAll(channels);
        channels=Collections.unmodifiableMap(copy);
        if(channels.isEmpty()||channels.size()>8 || !Double.isFinite(weaponBaseAttack)||weaponBaseAttack<0||weaponBaseAttack>10000
                ||!Double.isFinite(nominalMultiplier)||nominalMultiplier<0||nominalMultiplier>6
                ||!Double.isFinite(requirementMultiplier)||requirementMultiplier<.1||requirementMultiplier>1) throw new IllegalArgumentException("Bundle bounds");
        double base=channels.values().stream().mapToDouble(ChannelAttack::base).sum();
        double total=channels.values().stream().mapToDouble(ChannelAttack::attackRating).sum();
        if(base>weaponBaseAttack*2+1e-8 || Math.abs(total-weaponBaseAttack*nominalMultiplier)>1e-7) throw new IllegalArgumentException("Inconsistent bundle");
    }
    public double totalBase() { return channels.values().stream().mapToDouble(ChannelAttack::base).sum(); }
    public double totalScalingBonus() { return channels.values().stream().mapToDouble(ChannelAttack::scalingBonus).sum(); }
    public double totalAttackRating() { return totalBase()+totalScalingBonus(); }
    public double effectiveMultiplier() { return nominalMultiplier*requirementMultiplier; }
    public double fraction(WeaponDamageChannel channel) {
        var c=channels.get(channel); return c==null||totalAttackRating()==0?0:c.attackRating()/totalAttackRating();
    }
    public static WeaponDamageBundle legacy(double scaling,double requirement) {
        return new WeaponDamageBundle(Map.of(WeaponDamageChannel.PHYSICAL,new ChannelAttack(1,scaling-1)),1,scaling,requirement);
    }
}
