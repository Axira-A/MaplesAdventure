package dev.maplesadventure.progression.armor;

import dev.maplesadventure.progression.weapon.WeaponDamageChannel;
import dev.maplesadventure.progression.status.StatusResistanceType;
import java.util.*;

/** Fixed Maples equipment contributions. Vanilla armor attributes are separate. */
public record ArmorProfile(Map<WeaponDamageChannel, Double> channels,
                           Map<StatusResistanceType, Double> resistances) {
    public static final ArmorProfile EMPTY = new ArmorProfile(Map.of(), Map.of());
    public ArmorProfile {
        channels = copy(channels, WeaponDamageChannel.class);
        resistances = copy(resistances, StatusResistanceType.class);
    }
    private static <E extends Enum<E>> Map<E, Double> copy(Map<E, Double> input, Class<E> type) {
        Objects.requireNonNull(input);
        var result = new EnumMap<E, Double>(type);
        input.forEach((key, value) -> {
            Objects.requireNonNull(key);
            if (value == null || !Double.isFinite(value) || value < 0 || value > 1000)
                throw new IllegalArgumentException("Armor contribution outside 0..1000");
            result.put(key, value);
        });
        return Collections.unmodifiableMap(result);
    }
    public double channel(WeaponDamageChannel type) { return channels.getOrDefault(type, 0.0); }
    public double resistance(StatusResistanceType type) { return resistances.getOrDefault(type, 0.0); }
}
