package dev.maplesadventure.progression.weapon;

import java.util.ArrayList;
import dev.maplesadventure.progression.defense.*;
import dev.maplesadventure.config.EnemyDefenseConfig;
import net.minecraft.world.entity.LivingEntity;

public final class WeaponCombatResolutionService {
    public static WeaponDamageResolution resolve(double originalDamage, WeaponHitContext context, LivingEntity target) {
        return resolve(originalDamage, context.bundle(), EntityDefenseService.resolve(target).profile(), EnemyDefenseConfig.DEFENSE_PRESSURE.get());
    }
    /** Pure shared arithmetic; commands/test providers must not apply a second damage event. */
    public static WeaponDamageResolution resolve(double originalDamage, WeaponDamageBundle bundle, EntityDefenseProfile profile, double pressure) {
        if (!Double.isFinite(originalDamage) || originalDamage < 0) throw new IllegalArgumentException("Invalid incoming damage");
        double nominal = originalDamage * bundle.nominalMultiplier();
        double totalAR = bundle.totalAttackRating();
        double qualified = 0;
        boolean identity = true;
        var results = new ArrayList<WeaponDamageResolution.ChannelResult>();
        for (var entry : bundle.channels().entrySet()) {
            var channel = entry.getKey(); double ar = entry.getValue().attackRating();
            double share = totalAR == 0 ? 0 : ar / totalAR;
            var defense = profile.channel(channel);
            double multiplier = DefenseMitigationCurve.finalChannelMultiplier(ar, defense, pressure);
            double incoming = nominal * share, outgoing = incoming * multiplier;
            results.add(new WeaponDamageResolution.ChannelResult(channel, ar, share, incoming, defense.defense(), defense.absorption(), multiplier, outgoing));
            qualified += outgoing;
            if (share > 0 && multiplier != 1) identity = false;
        }
        // Preserve Round 9's multiplication association exactly on neutral targets (including players).
        // Splitting/recombining neutral channels would otherwise change the low floating-point bits.
        if (identity) qualified = nominal;
        double finalDamage = identity ? originalDamage * bundle.effectiveMultiplier() : qualified * bundle.requirementMultiplier();
        return new WeaponDamageResolution(originalDamage, nominal, results, qualified, bundle.requirementMultiplier(), finalDamage);
    }
    private WeaponCombatResolutionService() {}
}
