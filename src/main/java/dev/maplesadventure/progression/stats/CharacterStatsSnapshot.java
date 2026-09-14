package dev.maplesadventure.progression.stats;

import java.util.*;

/** Immutable, UI-neutral complete character-stat view for one attribute/equipment snapshot. */
public final class CharacterStatsSnapshot {
    private final int level;
    private final Map<CharacterStat, CharacterStatValue> values;
    private final EquipLoadSnapshot equipLoad;
    private final dev.maplesadventure.progression.spell.SpellSchoolScalingSnapshot spellSchools;
    private final List<dev.maplesadventure.progression.weapon.WeaponLoadoutSnapshot.View> weapons;

    public CharacterStatsSnapshot(int level, Map<CharacterStat, CharacterStatValue> source) {
        this(level, source, EquipLoadSnapshot.unavailable(source.getOrDefault(CharacterStat.MAX_EQUIP_LOAD,
                CharacterStatValue.unavailable())));
    }

    public CharacterStatsSnapshot(int level, Map<CharacterStat, CharacterStatValue> source,
                                  EquipLoadSnapshot equipLoad) {
        this(level, source, equipLoad, dev.maplesadventure.progression.spell.SpellSchoolScalingSnapshot.empty());
    }

    public CharacterStatsSnapshot(int level, Map<CharacterStat, CharacterStatValue> source, EquipLoadSnapshot equipLoad,
            dev.maplesadventure.progression.spell.SpellSchoolScalingSnapshot spellSchools) {
        this(level,source,equipLoad,spellSchools,List.of());
    }
    private CharacterStatsSnapshot(int level, Map<CharacterStat,CharacterStatValue> source, EquipLoadSnapshot equipLoad,
            dev.maplesadventure.progression.spell.SpellSchoolScalingSnapshot spellSchools,
            List<dev.maplesadventure.progression.weapon.WeaponLoadoutSnapshot.View> weapons) {
        if (level < 0 || source == null) throw new IllegalArgumentException("Invalid character stats snapshot");
        EnumMap<CharacterStat, CharacterStatValue> copy = new EnumMap<>(CharacterStat.class);
        for (CharacterStat stat : CharacterStat.values())
            copy.put(stat, source.getOrDefault(stat, CharacterStatValue.unavailable()));
        this.level = level;
        this.values = Collections.unmodifiableMap(copy);
        this.equipLoad = Objects.requireNonNull(equipLoad);
        this.spellSchools = Objects.requireNonNull(spellSchools);
        this.weapons = List.copyOf(weapons);
    }

    public int level() { return level; }
    public List<dev.maplesadventure.progression.weapon.WeaponLoadoutSnapshot.View> weapons() { return weapons; }
    public CharacterStatsSnapshot withWeapons(List<dev.maplesadventure.progression.weapon.WeaponLoadoutSnapshot.View> weapons) {
        var updated=new EnumMap<CharacterStat,CharacterStatValue>(CharacterStat.class); updated.putAll(values);
        updated.put(CharacterStat.MAIN_HAND_ATTACK,CharacterStatValue.unavailable());
        updated.put(CharacterStat.OFF_HAND_ATTACK,CharacterStatValue.unavailable());
        for(var view:weapons) if(view.held().weapon()) {
            var attack=view.attack();
            updated.put(view.offhand()?CharacterStat.OFF_HAND_ATTACK:CharacterStat.MAIN_HAND_ATTACK,
                    CharacterStatValue.active(new StatBreakdown(attack.baseAttack(),attack.scalingBonus(),0,0)));
        }
        return new CharacterStatsSnapshot(level,updated,equipLoad,spellSchools,weapons);
    }
    public dev.maplesadventure.progression.spell.SpellSchoolScalingSnapshot spellSchools() { return spellSchools; }
    public CharacterStatValue value(CharacterStat stat) { return values.get(stat); }
    public Map<CharacterStat, CharacterStatValue> values() { return values; }
    public List<CharacterStat> section(CharacterStatSection section) {
        return Arrays.stream(CharacterStat.values()).filter(stat -> stat.section() == section && stat != CharacterStat.SPELL_POWER).toList();
    }
    public EquipLoadSnapshot equipLoad() { return equipLoad; }
    public AttackProfileSnapshot attackProfile() {
        return new AttackProfileSnapshot(value(CharacterStat.MAIN_HAND_ATTACK), value(CharacterStat.OFF_HAND_ATTACK));
    }
    public SpellScalingSnapshot spellScaling() {
        return new SpellScalingSnapshot(value(CharacterStat.SPELL_POWER), value(CharacterStat.INTELLIGENCE_SCALING),
                value(CharacterStat.FAITH_SCALING), value(CharacterStat.ARCANE_SCALING));
    }
}
