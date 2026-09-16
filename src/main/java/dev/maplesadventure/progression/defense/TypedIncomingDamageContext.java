package dev.maplesadventure.progression.defense;

import java.util.*;
import dev.maplesadventure.progression.weapon.*;

/** Describes pressure, not extra damage. Weapon hits retain their original immutable bundle. */
public record TypedIncomingDamageContext(List<Slice> slices, double attackPower, String sourceKind,
                                         String debugReason, Optional<WeaponHitContext> weapon) {
    public record Slice(WeaponDamageChannel channel, double share, double attackPower) {
        public Slice {
            Objects.requireNonNull(channel);
            if (!Double.isFinite(share) || share < 0 || share > 1 || !Double.isFinite(attackPower)
                    || attackPower < 0 || attackPower > 1_000_000) throw new IllegalArgumentException("Typed slice bounds");
        }
    }
    public TypedIncomingDamageContext {
        slices = List.copyOf(slices); Objects.requireNonNull(weapon);
        if (slices.isEmpty() || slices.size() > 9 || !Double.isFinite(attackPower) || attackPower < 0
                || attackPower > 1_000_000 || sourceKind == null || sourceKind.length() > 128
                || debugReason == null || debugReason.length() > 512) throw new IllegalArgumentException("Typed context bounds");
        var seen = EnumSet.noneOf(WeaponDamageChannel.class);
        double sum = 0;
        for (var slice : slices) {
            if (!seen.add(slice.channel())) throw new IllegalArgumentException("Duplicate typed channel");
            sum += slice.share();
            if (Math.abs(slice.attackPower()-attackPower*slice.share()) > 1e-6) throw new IllegalArgumentException("Inconsistent attack pressure");
        }
        if (Math.abs(sum-(attackPower == 0 ? 0 : 1)) > 1e-8) throw new IllegalArgumentException("Typed shares must sum to one");
    }
    public static TypedIncomingDamageContext weapon(WeaponHitContext hit) {
        var b = hit.bundle();
        var slices = b.channels().entrySet().stream().map(e -> new Slice(e.getKey(), b.fraction(e.getKey()), e.getValue().attackRating())).toList();
        return new TypedIncomingDamageContext(slices, b.totalAttackRating(), hit.projectileSnapshot()?"FROZEN_WEAPON":"DIRECT_WEAPON", "WeaponHitContext", Optional.of(hit));
    }
    public static TypedIncomingDamageContext generic(Map<WeaponDamageChannel,Double> powers, String kind, String reason) {
        var sorted = new EnumMap<WeaponDamageChannel,Double>(WeaponDamageChannel.class); sorted.putAll(powers);
        double total = sorted.values().stream().mapToDouble(Double::doubleValue).sum();
        var slices = sorted.entrySet().stream().map(e -> new Slice(e.getKey(), total==0?0:e.getValue()/total, e.getValue())).toList();
        return new TypedIncomingDamageContext(slices, total, kind, reason, Optional.empty());
    }
}
