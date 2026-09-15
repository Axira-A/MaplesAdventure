package dev.maplesadventure.progression.defense;

import dev.maplesadventure.progression.stats.DamageDefenseType;
import dev.maplesadventure.progression.weapon.WeaponDamageChannel;

/** Bijective mapping; PHYSICAL never aggregates the three specialized physical channels. */
public final class DamageChannelMapping {
    public static WeaponDamageChannel channel(DamageDefenseType type) { return WeaponDamageChannel.valueOf(type.name()); }
    public static DamageDefenseType preview(WeaponDamageChannel channel) { return DamageDefenseType.valueOf(channel.name()); }
    private DamageChannelMapping() {}
}
