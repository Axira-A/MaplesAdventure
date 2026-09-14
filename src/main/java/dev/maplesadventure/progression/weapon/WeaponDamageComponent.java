package dev.maplesadventure.progression.weapon;

import java.util.Objects;

public record WeaponDamageComponent(WeaponDamageChannel channel, double baseRatio, ScalingMode scalingMode,
                                    WeaponScalingProfile optionalScalingProfile) {
    public enum ScalingMode { INHERIT_WEAPON, OVERRIDE }
    public WeaponDamageComponent {
        Objects.requireNonNull(channel); Objects.requireNonNull(scalingMode);
        if (!Double.isFinite(baseRatio) || baseRatio<0 || baseRatio>2) throw new IllegalArgumentException("base_ratio outside 0..2");
        if ((scalingMode==ScalingMode.OVERRIDE)!=(optionalScalingProfile!=null)) throw new IllegalArgumentException("Scaling mode/profile mismatch");
    }
    public static WeaponDamageComponent inherit(WeaponDamageChannel channel) { return new WeaponDamageComponent(channel,1,ScalingMode.INHERIT_WEAPON,null); }
    public WeaponScalingProfile scaling(WeaponScalingProfile weapon) { return scalingMode==ScalingMode.INHERIT_WEAPON?weapon:optionalScalingProfile; }
}
