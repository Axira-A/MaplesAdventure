package dev.maplesadventure.api.weapon;

import java.util.*;

/** Detached numeric scaling coefficients; letter grades are UI only. */
public record WeaponScalingView(Map<MaplesWeaponAttribute, Double> coefficients, double maxBonus) {
    public WeaponScalingView {
        Objects.requireNonNull(coefficients);
        if (!Double.isFinite(maxBonus) || maxBonus < 0 || maxBonus > 2) throw new IllegalArgumentException("maxBonus outside 0..2");
        var copy = new EnumMap<MaplesWeaponAttribute, Double>(MaplesWeaponAttribute.class);
        coefficients.forEach((key, value) -> {
            Objects.requireNonNull(key);
            if (value == null || !Double.isFinite(value) || value < 0 || value > 1.5)
                throw new IllegalArgumentException("Scaling coefficient outside 0..1.5");
            copy.put(key, value);
        });
        coefficients = Collections.unmodifiableMap(copy);
    }
    public double get(MaplesWeaponAttribute attribute) { return coefficients.getOrDefault(attribute, 0.0); }
}
