package dev.maplesadventure.progression.stamina;

/**
 * Souls-scaled stamina constants and the normalized Elden Ring END -> stamina curve.
 * Maples END 5 represents the Wretch/Deprived white-box baseline; Maples END 99 maps to ER END 99.
 */
public final class StaminaPolicy {
    public static final double SOULS_SCALE = 4.8D;
    public static final double BASE_MAX_STAMINA = 20.0D;
    public static final double BASE_REGEN_PER_SECOND = 45.0D / SOULS_SCALE; // 9.375
    public static final double BASE_REGEN_PER_TICK = BASE_REGEN_PER_SECOND / 20.0D; // 0.46875
    public static final double MAX_STAMINA_DEBT = 60.0D / SOULS_SCALE; // DS3-style debt cap: 12.5

    private static final int MAPLES_MIN_ENDURANCE = 5;
    private static final int MAPLES_MAX_ENDURANCE = 99;
    private static final int ER_BASE_ENDURANCE = 10;

    /** Exact ER stamina totals for END 10..99 (current Eldenpedia table). */
    private static final int[] ER_STAMINA_10_TO_99 = {
            96, 97, 99, 101, 103, 105, 106, 108, 110, 111,
            113, 115, 116, 118, 120, 121, 123, 125, 126, 128,
            130, 131, 132, 133, 135, 136, 137, 138, 140, 141,
            142, 143, 145, 146, 147, 148, 150, 151, 152, 153,
            155, 155, 155, 155, 156, 156, 156, 157, 157, 157,
            158, 158, 158, 158, 159, 159, 159, 160, 160, 160,
            161, 161, 161, 162, 162, 162, 162, 163, 163, 163,
            164, 164, 164, 165, 165, 165, 166, 166, 166, 166,
            167, 167, 167, 168, 168, 168, 169, 169, 169, 170
    };

    public static double maxStamina(int maplesEndurance) {
        int clamped = Math.clamp(maplesEndurance, MAPLES_MIN_ENDURANCE, MAPLES_MAX_ENDURANCE);
        double erEndurance = ER_BASE_ENDURANCE
                + (clamped - MAPLES_MIN_ENDURANCE)
                * (89.0D / (MAPLES_MAX_ENDURANCE - MAPLES_MIN_ENDURANCE));
        return erStamina(erEndurance) / SOULS_SCALE;
    }

    static double erStamina(double endurance) {
        double clamped = Math.clamp(endurance, 10.0D, 99.0D);
        int lower = (int) Math.floor(clamped);
        int upper = (int) Math.ceil(clamped);
        double lowValue = ER_STAMINA_10_TO_99[lower - 10];
        if (lower == upper) return lowValue;
        double highValue = ER_STAMINA_10_TO_99[upper - 10];
        return lowValue + (highValue - lowValue) * (clamped - lower);
    }

    private StaminaPolicy() {}
}
