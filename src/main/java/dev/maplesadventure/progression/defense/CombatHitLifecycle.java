package dev.maplesadventure.progression.defense;

import java.util.*;
import net.minecraft.world.damagesource.DamageSource;
import dev.maplesadventure.progression.weapon.WeaponHitContext;

/** Pre -> Post handoff. Post exposes source, not DamageContainer. Identity/LIFO supports nested hurt.
 * Tick-end cleanup handles third-party pipelines that omit Post; no persistent entity references. */
public final class CombatHitLifecycle {
    private record Entry(UUID target, DamageSource source, Optional<WeaponHitContext> weapon) {}
    private static final Deque<Entry> pending = new ArrayDeque<>();
    public static void prepared(UUID target, DamageSource source, Optional<WeaponHitContext> weapon) {
        if (pending.size() >= 1024) pending.removeLast();
        pending.addFirst(new Entry(target, source, weapon));
    }
    public static Optional<WeaponHitContext> consume(UUID target, DamageSource source) {
        var it = pending.iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (entry.source() == source && entry.target().equals(target)) { it.remove(); return entry.weapon(); }
        }
        return Optional.empty(); // Never resolve a possibly changed hand in Post.
    }
    public static boolean hasFire(WeaponHitContext hit) {
        var fire = hit.bundle().channels().get(dev.maplesadventure.progression.weapon.WeaponDamageChannel.FIRE);
        return fire != null && fire.attackRating() > 0 && hit.bundle().fraction(dev.maplesadventure.progression.weapon.WeaponDamageChannel.FIRE) > 0;
    }
    public static void clear() { pending.clear(); }
    private CombatHitLifecycle() {}
}
