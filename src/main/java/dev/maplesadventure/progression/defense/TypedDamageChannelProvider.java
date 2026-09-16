package dev.maplesadventure.progression.defense;

import java.util.Map;
import java.util.Optional;
import net.minecraft.world.damagesource.DamageSource;
import dev.maplesadventure.progression.weapon.WeaponDamageChannel;

/** Server-only descriptive adapter. Return bounded channel pressure; never mutate health/events. */
public interface TypedDamageChannelProvider {
    Optional<Map<WeaponDamageChannel, Double>> channelAttackRatings(DamageSource source);
}
