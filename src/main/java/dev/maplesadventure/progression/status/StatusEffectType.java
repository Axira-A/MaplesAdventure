package dev.maplesadventure.progression.status;

import java.util.Locale;
import net.minecraft.resources.ResourceLocation;

/** Canonical ailments; independent of weapon damage channels. */
public enum StatusEffectType {
    BLEED(StatusResistanceType.ROBUSTNESS, true, false),
    POISON(StatusResistanceType.IMMUNITY, true, true),
    SCARLET_ROT(StatusResistanceType.IMMUNITY, false, true),
    FROSTBITE(StatusResistanceType.ROBUSTNESS, false, true),
    SLEEP(StatusResistanceType.FOCUS, true, false),
    MADNESS(StatusResistanceType.FOCUS, true, false),
    DEATH_BLIGHT(StatusResistanceType.VITALITY, false, false);
    private final StatusResistanceType resistance;
    private final boolean arcane, duration;
    StatusEffectType(StatusResistanceType resistance, boolean arcane, boolean duration) {
        this.resistance = resistance; this.arcane = arcane; this.duration = duration;
    }
    public StatusResistanceType resistanceType() { return resistance; }
    public boolean allowsArcaneScaling() { return arcane; }
    public boolean durationBar() { return duration; }
    public String id() { return name().toLowerCase(Locale.ROOT); }
    public ResourceLocation definitionId() { return ResourceLocation.fromNamespaceAndPath("maplesadventure", id()); }
    public static StatusEffectType parse(String id) { return valueOf(id.toUpperCase(Locale.ROOT)); }
}
