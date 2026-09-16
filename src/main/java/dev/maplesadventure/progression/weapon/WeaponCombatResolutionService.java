package dev.maplesadventure.progression.weapon;

import java.util.ArrayList;
import dev.maplesadventure.progression.defense.*;
import dev.maplesadventure.config.EnemyDefenseConfig;
import net.minecraft.world.entity.LivingEntity;

public final class WeaponCombatResolutionService {
    public static WeaponDamageResolution resolve(double originalDamage, WeaponHitContext context, LivingEntity target) {
        var defense = TargetDefenseResolver.resolve(target);
        return resolve(originalDamage, context.bundle(), defense.view(), defense.pressure());
    }
    /** Pure shared arithmetic; commands/test providers must not apply a second damage event. */
    public static WeaponDamageResolution resolve(double originalDamage, WeaponDamageBundle bundle, ChannelDefenseView profile, double pressure) {
        double totalAR = bundle.totalAttackRating();
        var slices = bundle.channels().entrySet().stream().map(e -> new TypedIncomingDamageContext.Slice(e.getKey(),
                totalAR == 0 ? 0 : e.getValue().attackRating()/totalAR, e.getValue().attackRating())).toList();
        return resolveSlices(originalDamage, originalDamage * bundle.nominalMultiplier(), bundle.requirementMultiplier(),
                bundle.effectiveMultiplier(), slices, profile, pressure);
    }
    public static WeaponDamageResolution resolve(double originalDamage, TypedIncomingDamageContext typed, ChannelDefenseView profile, double pressure) {
        if (typed.weapon().isPresent()) return resolve(originalDamage, typed.weapon().get().bundle(), profile, pressure);
        return resolveSlices(originalDamage, originalDamage, 1, 1, typed.slices(), profile, pressure);
    }
    private static WeaponDamageResolution resolveSlices(double originalDamage, double nominal, double requirement, double effective,
            java.util.List<TypedIncomingDamageContext.Slice> slices, ChannelDefenseView profile, double pressure) {
        if (!Double.isFinite(originalDamage) || originalDamage < 0) throw new IllegalArgumentException("Invalid incoming damage");
        double qualified = 0;
        boolean identity = true;
        var results = new ArrayList<WeaponDamageResolution.ChannelResult>();
        for (var slice : slices) {
            var channel = slice.channel(); double ar = slice.attackPower();
            double share = slice.share();
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
        double finalDamage = identity ? originalDamage * effective : qualified * requirement;
        return new WeaponDamageResolution(originalDamage, nominal, results, qualified, requirement, finalDamage);
    }
    private WeaponCombatResolutionService() {}
}
