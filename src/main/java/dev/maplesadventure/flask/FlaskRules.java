package dev.maplesadventure.flask;

/** Frozen gameplay tables. No max-resource percentage scaling. */
public final class FlaskRules {
    public static final int MIN_CAPACITY = 4, MAX_CAPACITY = 14, MAX_POTENCY = 12;
    public static final int USE_TICKS = 30, EFFECT_TICK = 14;
    public static final int RECOVERY_TICKS = 6;
    private static final int[] CAPACITY_COSTS = {1, 1, 2, 2, 3, 3, 4, 4, 5, 5};
    private static final double[] HEALTH = {15, 20.5, 25.5, 30, 34, 37.5, 40, 42, 43.5, 45, 46.5, 47.5, 48.5};
    private static final double[] MANA = {129, 153, 177, 202, 226, 242, 258, 274, 290, 306, 323, 339, 355};
    public static int capacityCost(int capacity) {
        return capacity >= MIN_CAPACITY && capacity < MAX_CAPACITY ? CAPACITY_COSTS[capacity - MIN_CAPACITY] : 0;
    }
    public static double health(int potency) { return HEALTH[Math.clamp(potency, 0, MAX_POTENCY)]; }
    public static double mana(int potency) { return MANA[Math.clamp(potency, 0, MAX_POTENCY)]; }
    /** Supplied artwork: 1 is full, 7 is empty. A nonempty pool always retains visible liquid. */
    public static int icon(int remaining, int allocated) {
        if (remaining <= 0 || allocated <= 0) return 7;
        return 7 - Math.clamp((int)Math.ceil(6.0 * remaining / allocated), 1, 6);
    }
    private FlaskRules() {}
}
