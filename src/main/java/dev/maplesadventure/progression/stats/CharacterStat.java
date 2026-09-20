package dev.maplesadventure.progression.stats;

import java.util.Locale;

/** Stable semantic IDs shared by upgrade, character and future equipment screens. */
public enum CharacterStat {
    MAX_HEALTH(CharacterStatSection.CORE, 10),
    MAX_MANA(CharacterStatSection.CORE, 20),
    MAX_STAMINA(CharacterStatSection.CORE, 30),

    CURRENT_EQUIP_LOAD(CharacterStatSection.EQUIPMENT, 80),
    MAX_EQUIP_LOAD(CharacterStatSection.EQUIPMENT, 40),
    EQUIP_LOAD_RATIO(CharacterStatSection.EQUIPMENT, 70, true),

    MAIN_HAND_ATTACK(CharacterStatSection.OFFENSE, 50),
    OFF_HAND_ATTACK(CharacterStatSection.OFFENSE, 90),
    SPELL_POWER(CharacterStatSection.OFFENSE, 55),

    PHYSICAL_DEFENSE(CharacterStatSection.DEFENSE, 60),
    STRIKE_DEFENSE(CharacterStatSection.DEFENSE, 81),
    SLASH_DEFENSE(CharacterStatSection.DEFENSE, 82),
    PIERCE_DEFENSE(CharacterStatSection.DEFENSE, 83),

    MAGIC_DEFENSE(CharacterStatSection.ELEMENTAL, 61),
    FIRE_DEFENSE(CharacterStatSection.ELEMENTAL, 62),
    LIGHTNING_DEFENSE(CharacterStatSection.ELEMENTAL, 63),
    ICE_DEFENSE(CharacterStatSection.ELEMENTAL, 64),
    HOLY_DEFENSE(CharacterStatSection.ELEMENTAL, 65),

    IMMUNITY(CharacterStatSection.RESISTANCE, 100),
    ROBUSTNESS(CharacterStatSection.RESISTANCE, 101),
    FOCUS(CharacterStatSection.RESISTANCE, 102),
    VITALITY(CharacterStatSection.RESISTANCE, 103),

    INTELLIGENCE_SCALING(CharacterStatSection.MAGIC, 110),
    FAITH_SCALING(CharacterStatSection.MAGIC, 111),
    ARCANE_SCALING(CharacterStatSection.MAGIC, 112);

    private final CharacterStatSection section;
    private final int compactPriority;
    private final boolean percentage;

    CharacterStat(CharacterStatSection section, int compactPriority) {
        this(section, compactPriority, false);
    }

    CharacterStat(CharacterStatSection section, int compactPriority, boolean percentage) {
        this.section = section;
        this.compactPriority = compactPriority;
        this.percentage = percentage;
    }

    public CharacterStatSection section() { return section; }
    public int compactPriority() { return compactPriority; }
    public boolean percentage() { return percentage; }
    public String translationKey() {
        return "screen.maplesadventure.character_stats.stat." + name().toLowerCase(Locale.ROOT);
    }
}
