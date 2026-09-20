package dev.maplesadventure.progression.status;

public record StatusBuildupComponent(StatusEffectType type, double baseBuildup, double arcaneScaling, StatusArcaneScalingPolicy policy) {
    public StatusBuildupComponent(StatusEffectType type, double baseBuildup, double arcaneScaling) {
        this(type, baseBuildup, arcaneScaling, arcaneScaling == 0 ? StatusArcaneScalingPolicy.NONE : StatusArcaneScalingPolicy.EXPLICIT);
    }
    public StatusBuildupComponent {
        java.util.Objects.requireNonNull(type);
        java.util.Objects.requireNonNull(policy);
        StatusResistance.bounded(baseBuildup,0,1000); StatusResistance.bounded(arcaneScaling,0,2);
        if (!type.allowsArcaneScaling() && (policy != StatusArcaneScalingPolicy.NONE || arcaneScaling != 0))
            throw new IllegalArgumentException(type + " cannot have ARC status scaling");
        if (policy == StatusArcaneScalingPolicy.NONE && arcaneScaling != 0)
            throw new IllegalArgumentException("NONE cannot carry ARC scaling");
    }
    public double amount(int arcane) {
        return amount(arcane, 0, 1);
    }
    public double amount(int arcane, double weaponArcane, double motion) {
        StatusResistance.bounded(weaponArcane, 0, 2); StatusResistance.bounded(motion, 0, 4);
        double coefficient = switch (policy) {
            case NONE -> 0; case EXPLICIT -> arcaneScaling; case FOLLOW_WEAPON_ARCANE -> weaponArcane;
        };
        return baseBuildup * motion * (1 + StatusArcaneScalingCurve.evaluate(arcane) * coefficient);
    }
}
