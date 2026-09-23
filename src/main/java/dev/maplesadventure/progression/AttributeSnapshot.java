package dev.maplesadventure.progression;

/** Server-authored local-player view used by future screens without trusting client-side config. */
public record AttributeSnapshot(PlayerAttributeState state, int level, double maxHealth, double mana,
                                double stamina, long nextLevelCost,
                                dev.maplesadventure.progression.runtime.RuntimeResourceSnapshot runtimeResources,
                                dev.maplesadventure.progression.encumbrance.EquipLoadRuntimeSnapshot equipLoad,
                                dev.maplesadventure.progression.spell.SpellSchoolScalingSnapshot spellSchools,
                                dev.maplesadventure.progression.weapon.WeaponLoadoutSnapshot weapons,
                                dev.maplesadventure.progression.armor.ArmorEquipmentSnapshot armor) {
    public AttributeSnapshot(PlayerAttributeState state, int level, double health, double mana, double stamina,
            long cost, dev.maplesadventure.progression.runtime.RuntimeResourceSnapshot runtime,
            dev.maplesadventure.progression.encumbrance.EquipLoadRuntimeSnapshot equipLoad,
            dev.maplesadventure.progression.spell.SpellSchoolScalingSnapshot schools,
            dev.maplesadventure.progression.weapon.WeaponLoadoutSnapshot weapons) {
        this(state, level, health, mana, stamina, cost, runtime, equipLoad, schools, weapons,
                dev.maplesadventure.progression.armor.ArmorEquipmentSnapshot.EMPTY);
    }
    public AttributeSnapshot(PlayerAttributeState state, int level, double health, double mana, double stamina,
            long cost, dev.maplesadventure.progression.runtime.RuntimeResourceSnapshot runtime,
            dev.maplesadventure.progression.encumbrance.EquipLoadRuntimeSnapshot equipLoad,
            dev.maplesadventure.progression.spell.SpellSchoolScalingSnapshot schools) {
        this(state,level,health,mana,stamina,cost,runtime,equipLoad,schools,
                dev.maplesadventure.progression.weapon.WeaponLoadoutSnapshot.empty());
    }
    public AttributeSnapshot(PlayerAttributeState state, int level, double health, double mana, double stamina,
            long cost, dev.maplesadventure.progression.runtime.RuntimeResourceSnapshot runtime,
            dev.maplesadventure.progression.encumbrance.EquipLoadRuntimeSnapshot equipLoad) {
        this(state, level, health, mana, stamina, cost, runtime, equipLoad,
                dev.maplesadventure.progression.spell.SpellSchoolScalingSnapshot.empty());
    }
    public static AttributeSnapshot of(PlayerAttributeState state) {
        var runtime = dev.maplesadventure.progression.runtime.RuntimeResourceSnapshot.progressionOnly(state);
        var stats = dev.maplesadventure.progression.stats.CharacterStatsService.preview(state, runtime);
        return new AttributeSnapshot(state.cleanCopy(), AttributeProgression.level(state),
                stats.value(dev.maplesadventure.progression.stats.CharacterStat.MAX_HEALTH).value(),
                stats.value(dev.maplesadventure.progression.stats.CharacterStat.MAX_MANA).value(),
                stats.value(dev.maplesadventure.progression.stats.CharacterStat.MAX_STAMINA).value(),
                AttributeProgression.costForNextLevel(AttributeProgression.level(state)), runtime,
                dev.maplesadventure.progression.encumbrance.EquipLoadRuntimeSnapshot.unavailable());
    }

    public static AttributeSnapshot of(net.minecraft.server.level.ServerPlayer player) {
        PlayerAttributeState state = PlayerAttributeService.state(player);
        var runtime = dev.maplesadventure.progression.runtime.DerivedStatRuntimeService.snapshot(player);
        var equipLoad = dev.maplesadventure.progression.encumbrance.EncumbranceRuntimeService.snapshot(player);
        var stats = dev.maplesadventure.progression.stats.CharacterStatsService.preview(state, runtime, equipLoad);
        return new AttributeSnapshot(state.cleanCopy(), AttributeProgression.level(state),
                stats.value(dev.maplesadventure.progression.stats.CharacterStat.MAX_HEALTH).value(),
                stats.value(dev.maplesadventure.progression.stats.CharacterStat.MAX_MANA).value(),
                stats.value(dev.maplesadventure.progression.stats.CharacterStat.MAX_STAMINA).value(),
                AttributeProgression.costForNextLevel(AttributeProgression.level(state)), runtime, equipLoad,
                dev.maplesadventure.progression.spell.SpellScalingRuntimeService.snapshot(player),
                dev.maplesadventure.progression.weapon.WeaponLoadoutSnapshot.of(player),
                dev.maplesadventure.progression.armor.ArmorEquipmentService.snapshot(player));
    }

    /** Complete reusable view; Screens never scan a player or duplicate derived formulas. */
    public dev.maplesadventure.progression.stats.CharacterStatsSnapshot characterStats() {
        return dev.maplesadventure.progression.stats.CharacterStatsService.preview(state, runtimeResources, equipLoad, spellSchools, armor)
                .withWeapons(weapons, state);
    }
}
