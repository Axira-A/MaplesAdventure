package dev.maplesadventure.api.weapon;

import java.util.*;

/** Detached required attribute points, each 0..99. */
public record WeaponRequirementView(Map<MaplesWeaponAttribute, Integer> requirements) {
    public WeaponRequirementView {
        Objects.requireNonNull(requirements);
        var copy = new EnumMap<MaplesWeaponAttribute, Integer>(MaplesWeaponAttribute.class);
        requirements.forEach((key, value) -> {
            Objects.requireNonNull(key);
            if (value == null || value < 0 || value > 99) throw new IllegalArgumentException("Requirement outside 0..99");
            copy.put(key, value);
        });
        requirements = Collections.unmodifiableMap(copy);
    }
    public int get(MaplesWeaponAttribute attribute) { return requirements.getOrDefault(attribute, 0); }
}
