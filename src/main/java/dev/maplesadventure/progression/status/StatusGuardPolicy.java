package dev.maplesadventure.progression.status;
/** Future shield negation adapters may extend this policy. No guard penetration is invented here. */
public final class StatusGuardPolicy {
    public static boolean permitsBuildup(double healthDamage) { return Double.isFinite(healthDamage)&&healthDamage>0; }
    private StatusGuardPolicy() {}
}
