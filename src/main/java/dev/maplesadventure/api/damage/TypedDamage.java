package dev.maplesadventure.api.damage;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable description of pressure within ONE existing hit, never additional damage.
 *
     * @param channels absolute channel pressure; 1–9 entries, finite nonnegative values, positive
 * total at most 1000000. Pressure is not a requested HP subtraction.
 */
public record TypedDamage(Map<MaplesDamageChannel, Double> channels) {
    /** Copies and validates all inputs. */
    public TypedDamage {
        channels = Map.copyOf(channels);
        if (channels.isEmpty() || channels.size() > 9) throw new IllegalArgumentException("Channel count");
        double sum = 0;
        for (var e : channels.entrySet()) {
            Objects.requireNonNull(e.getKey());
            double value = e.getValue();
            if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Channel pressure");
            sum += value;
        }
        if (!Double.isFinite(sum) || sum <= 0 || sum > 1000000) throw new IllegalArgumentException("Total pressure");
    }
}
