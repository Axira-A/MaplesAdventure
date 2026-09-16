package dev.maplesadventure.progression.defense;

import java.util.*;
import dev.maplesadventure.progression.weapon.WeaponDamageResolution;

/** Bounded, nonpersistent admin diagnostics. Lookup expiry avoids a tick scanner. */
public final class LastWeaponDamageResolution {
    private static final int MAX_ENTRIES = 256;
    private static final long TTL_NANOS = 60_000_000_000L;
    public record Entry(UUID target, String profile, WeaponDamageResolution resolution, long recordedAt,
                        String typedSource, double pressure, double frostMultiplier) {
        public double finalMaplesDamage() { return resolution.finalDamage()*frostMultiplier; }
    }
    private static final Map<UUID, Entry> hits = new LinkedHashMap<>();
    public static void record(UUID attacker, UUID target, String profile, WeaponDamageResolution resolution) {
        record(attacker,target,profile,resolution,"WEAPON",DefenseMitigationCurve.DEFAULT_PRESSURE,1);
    }
    public static void record(UUID attacker, UUID target, String profile, WeaponDamageResolution resolution,
                              String typedSource, double pressure, double frostMultiplier) {
        if (attacker == null) return;
        hits.remove(attacker);
        if (hits.size() >= MAX_ENTRIES) hits.remove(hits.keySet().iterator().next());
        hits.put(attacker, new Entry(target, profile, resolution, System.nanoTime(),typedSource,pressure,frostMultiplier));
    }
    public static Optional<Entry> get(UUID attacker) {
        var hit = hits.get(attacker);
        if (hit != null && System.nanoTime() - hit.recordedAt() > TTL_NANOS) { hits.remove(attacker); hit = null; }
        return Optional.ofNullable(hit);
    }
    public static void clear() { hits.clear(); }
    private LastWeaponDamageResolution() {}
}
