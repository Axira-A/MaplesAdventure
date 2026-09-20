package dev.maplesadventure.progression.status;

/** Minecraft balance defaults, not an assertion about a hidden FromSoftware formula. Times are ticks. */
public record StatusEffectDefinition(int decayDelay, double decayPerSecond, int duration, int tickInterval,
        double maxHealthFraction, double flatDamage, double damageTakenMultiplier, double staminaRegenMultiplier,
        int controlTicks,int deepSleepTicks,double manaFlat,double manaFraction) {
    public StatusEffectDefinition(int delay,double decay,int duration,int interval,double fraction,double flat,double taken,double regen) {
        this(delay,decay,duration,interval,fraction,flat,taken,regen,0,0,0,0);
    }
    public StatusEffectDefinition {
        if (decayDelay < 0 || decayDelay > 72000 || duration < 0 || duration > 72000 || tickInterval < 1 || tickInterval > 1200)
            throw new IllegalArgumentException("Status time bounds");
        StatusResistance.bounded(decayPerSecond, 0, 10000);
        StatusResistance.bounded(maxHealthFraction, 0, 1);
        StatusResistance.bounded(flatDamage, 0, 10000);
        StatusResistance.bounded(damageTakenMultiplier, 1, 3);
        StatusResistance.bounded(staminaRegenMultiplier, .05, 1);
        if(controlTicks<0||controlTicks>100||deepSleepTicks<0||deepSleepTicks>1200) throw new IllegalArgumentException("Control duration bounds");
        StatusResistance.bounded(manaFlat,0,10000); StatusResistance.bounded(manaFraction,0,1);
    }
    public double damage(double maxHealth, double resistanceMultiplier) {
        return (maxHealth * maxHealthFraction + flatDamage) * resistanceMultiplier;
    }
    public static StatusEffectDefinition defaults(StatusEffectType type) {
        return switch (type) {
            case BLEED -> new StatusEffectDefinition(60, 5, 0, 20, .15, 0, 1, 1);
            case POISON -> new StatusEffectDefinition(60, 5, 1800, 20, .0045, 0, 1, 1);
            case SCARLET_ROT -> new StatusEffectDefinition(60, 5, 1800, 20, .009, 0, 1, 1);
            case FROSTBITE -> new StatusEffectDefinition(60, 5, 600, 20, .10, 0, 1.20, .80);
            case SLEEP -> new StatusEffectDefinition(60, 5, 0, 20, 0, 0, 1, 1,30,1200,30,.10);
            case DEATH_BLIGHT -> new StatusEffectDefinition(60, 5, 0, 20, 0, 0, 1, 1);
            case MADNESS -> new StatusEffectDefinition(60, 5, 0, 20, .15, 0, 1, 1,40,0,30,.10);
        };
    }
}
