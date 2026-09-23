package dev.maplesadventure.api.weapon;

import dev.maplesadventure.api.status.MaplesStatusType;
import java.util.Objects;

/** Base buildup and Arcane policy before a particular attack's motion multiplier. */
public record WeaponStatusComponentView(MaplesStatusType type, double baseBuildup,
                                        double arcaneScaling, MaplesWeaponStatusArcanePolicy policy) {
    public WeaponStatusComponentView {
        Objects.requireNonNull(type); Objects.requireNonNull(policy);
        if (!Double.isFinite(baseBuildup) || baseBuildup < 0 || baseBuildup > 1000 ||
                !Double.isFinite(arcaneScaling) || arcaneScaling < 0 || arcaneScaling > 2)
            throw new IllegalArgumentException("Status buildup bounds");
    }
}
