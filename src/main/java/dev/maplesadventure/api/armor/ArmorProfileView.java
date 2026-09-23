package dev.maplesadventure.api.armor;

import dev.maplesadventure.api.damage.MaplesDamageChannel;
import dev.maplesadventure.api.status.MaplesResistanceType;
import java.util.*;
import net.minecraft.resources.ResourceLocation;

/** Immutable fixed per-item Maples contribution, independent of Vanilla armor points. */
public record ArmorProfileView(ResourceLocation itemId, Map<MaplesDamageChannel, Double> channels,
                               Map<MaplesResistanceType, Double> resistances) {
    public ArmorProfileView {
        Objects.requireNonNull(itemId);
        channels = checked(channels, MaplesDamageChannel.class, 1000);
        resistances = checked(resistances, MaplesResistanceType.class, 1000);
    }
    static <E extends Enum<E>> Map<E, Double> checked(Map<E, Double> input, Class<E> type, double maximum) {
        Objects.requireNonNull(input);
        var copy = new EnumMap<E, Double>(type);
        input.forEach((key, value) -> {
            Objects.requireNonNull(key);
            if (value == null || !Double.isFinite(value) || value < 0 || value > maximum)
                throw new IllegalArgumentException("Armor contribution bounds");
            copy.put(key, value);
        });
        return Collections.unmodifiableMap(copy);
    }
}
