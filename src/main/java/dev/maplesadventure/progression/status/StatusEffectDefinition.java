package dev.maplesadventure.progression.status;

/** Minecraft balance defaults, not an assertion about a hidden FromSoftware formula. Times are ticks. */
public record StatusEffectDefinition(int decayDelay, double decayPerSecond, int duration, int tickInterval,
        double maxHealthFraction, double flatDamage, double damageTakenMultiplier, double staminaRegenMultiplier) {
    public StatusEffectDefinition {
        if (decayDelay < 0 || decayDelay > 72000 || duration < 0 || duration > 72000 || tickInterval < 1 || tickInterval > 1200)
            throw new IllegalArgumentException("Status time bounds");
        StatusResistance.bounded(decayPerSecond, 0, 10000);
        StatusResistance.bounded(maxHealthFraction, 0, 1);
        StatusResistance.bounded(flatDamage, 0, 10000);
        StatusResistance.bounded(damageTakenMultiplier, 1, 3);
        StatusResistance.bounded(staminaRegenMultiplier, .05, 1);
    }
    public double damage(double maxHealth, double resistanceMultiplier) {
        return (maxHealth * maxHealthFraction + flatDamage) * resistanceMultiplier;
    }
    public static StatusEffectDefinition defaults(StatusEffectType type) {
        return switch (type) {
            case BLEED -> new StatusEffectDefinition(60, 5, 0, 20, .15, 0, 1, 1);
            case POISON -> new StatusEffectDefinition(60, 5, 1200, 40, .0025, .1, 1, 1);
            case SCARLET_ROT -> new StatusEffectDefinition(60, 5, 1200, 40, .0075, .2, 1, 1);
            case FROSTBITE -> new StatusEffectDefinition(60, 5, 300, 20, .11, 0, 1.07, .75);
        };
    }
}
