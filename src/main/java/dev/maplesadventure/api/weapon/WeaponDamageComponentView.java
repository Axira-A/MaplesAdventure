package dev.maplesadventure.api.weapon;

import dev.maplesadventure.api.damage.MaplesDamageChannel;
import java.util.Objects;

/** One resolved component. PHYSICAL means standard physical, independent of slash/strike/pierce. */
public record WeaponDamageComponentView(MaplesDamageChannel channel, double baseRatio, WeaponScalingView scaling) {
    public WeaponDamageComponentView {
        Objects.requireNonNull(channel); Objects.requireNonNull(scaling);
        if (!Double.isFinite(baseRatio) || baseRatio < 0 || baseRatio > 2) throw new IllegalArgumentException("baseRatio outside 0..2");
    }
}
