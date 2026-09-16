package dev.maplesadventure.progression.status;

public record StatusBuildupComponent(StatusEffectType type, double baseBuildup, double arcaneScaling) {
    public StatusBuildupComponent {
        java.util.Objects.requireNonNull(type);
        StatusResistance.bounded(baseBuildup,0,1000); StatusResistance.bounded(arcaneScaling,0,2);
    }
    public double amount(int arcane) {
        return baseBuildup*(1+dev.maplesadventure.progression.OffensiveScalingCurve.evaluate(arcane)*arcaneScaling);
    }
}
