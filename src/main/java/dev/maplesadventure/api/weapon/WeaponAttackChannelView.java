package dev.maplesadventure.api.weapon;

import dev.maplesadventure.api.damage.MaplesDamageChannel;
import java.util.Objects;

/** Nominal attack rating for one channel, before requirement efficiency and target defense. */
public record WeaponAttackChannelView(MaplesDamageChannel channel, double baseAttack,
                                      double scalingBonus, double total) {
    public WeaponAttackChannelView {
        Objects.requireNonNull(channel);
        if (!Double.isFinite(baseAttack) || baseAttack < 0 || !Double.isFinite(scalingBonus) || scalingBonus < 0 ||
                !Double.isFinite(total) || Math.abs(total - baseAttack - scalingBonus) > 1e-7)
            throw new IllegalArgumentException("Attack channel bounds");
    }
}
