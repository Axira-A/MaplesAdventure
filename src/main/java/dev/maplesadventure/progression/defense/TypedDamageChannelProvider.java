package dev.maplesadventure.progression.defense;

import java.util.Map;
import java.util.Optional;
import net.minecraft.world.damagesource.DamageSource;
import dev.maplesadventure.progression.weapon.WeaponDamageChannel;

/** Future adapter seam only. No providers are registered or executed in Round 10. */
public interface TypedDamageChannelProvider {
    Optional<Map<WeaponDamageChannel, Double>> channelAttackRatings(DamageSource source);
}
