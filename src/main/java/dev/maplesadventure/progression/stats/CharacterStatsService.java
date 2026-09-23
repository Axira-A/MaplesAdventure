package dev.maplesadventure.progression.stats;

import dev.maplesadventure.progression.PlayerAttributeService;
import dev.maplesadventure.progression.PlayerAttributeState;
import net.minecraft.server.level.ServerPlayer;
import dev.maplesadventure.progression.runtime.DerivedStatRuntimeService;
import dev.maplesadventure.progression.runtime.RuntimeResourceSnapshot;

/** Shared entry point for Bonfire, future NPC, character and equipment UIs. */
public final class CharacterStatsService {
    public static CharacterStatsSnapshot snapshot(ServerPlayer player) {
        return CharacterStatCalculator.calculate(PlayerAttributeService.state(player),
                DerivedStatRuntimeService.snapshot(player),
                dev.maplesadventure.progression.encumbrance.EncumbranceRuntimeService.snapshot(player),
                dev.maplesadventure.progression.spell.SpellScalingRuntimeService.snapshot(player),
                dev.maplesadventure.progression.armor.ArmorEquipmentService.snapshot(player));
    }

    public static CharacterStatsSnapshot preview(ServerPlayer player, PlayerAttributeState previewAttributes) {
        return CharacterStatCalculator.calculate(previewAttributes, DerivedStatRuntimeService.snapshot(player),
                dev.maplesadventure.progression.encumbrance.EncumbranceRuntimeService.snapshot(player),
                dev.maplesadventure.progression.spell.SpellScalingRuntimeService.snapshot(player),
                dev.maplesadventure.progression.armor.ArmorEquipmentService.snapshot(player));
    }

    /** Pure shared preview used after a server-authored attribute snapshot reaches the client. */
    public static CharacterStatsSnapshot preview(PlayerAttributeState previewAttributes) {
        return CharacterStatCalculator.calculate(previewAttributes);
    }

    /** Client-side preview using the runtime context included in the last server snapshot. */
    public static CharacterStatsSnapshot preview(PlayerAttributeState previewAttributes,
                                                 RuntimeResourceSnapshot runtimeResources) {
        return CharacterStatCalculator.calculate(previewAttributes, runtimeResources);
    }

    public static CharacterStatsSnapshot preview(PlayerAttributeState previewAttributes,
                                                 RuntimeResourceSnapshot runtimeResources,
                                                 dev.maplesadventure.progression.encumbrance.EquipLoadRuntimeSnapshot equipLoad) {
        return CharacterStatCalculator.calculate(previewAttributes, runtimeResources, equipLoad);
    }

    private CharacterStatsService() {}
    private static java.util.Map<dev.maplesadventure.progression.status.StatusEffectType,Double> thresholds(ServerPlayer player) {
        var out=new java.util.EnumMap<dev.maplesadventure.progression.status.StatusEffectType,Double>(dev.maplesadventure.progression.status.StatusEffectType.class);
        for(var type:dev.maplesadventure.progression.status.StatusEffectType.values()) out.put(type,dev.maplesadventure.progression.status.StatusResistanceService.resolve(player,type).threshold());
        return out;
    }
    public static CharacterStatsSnapshot preview(PlayerAttributeState attributes, RuntimeResourceSnapshot runtime,
            dev.maplesadventure.progression.encumbrance.EquipLoadRuntimeSnapshot equipLoad,
            dev.maplesadventure.progression.spell.SpellSchoolScalingSnapshot spellSchools) {
        return CharacterStatCalculator.calculate(attributes, runtime, equipLoad, spellSchools);
    }
    public static CharacterStatsSnapshot preview(PlayerAttributeState attributes, RuntimeResourceSnapshot runtime,
            dev.maplesadventure.progression.encumbrance.EquipLoadRuntimeSnapshot equipLoad,
            dev.maplesadventure.progression.spell.SpellSchoolScalingSnapshot spellSchools,
            dev.maplesadventure.progression.armor.ArmorEquipmentSnapshot armor) {
        return CharacterStatCalculator.calculate(attributes, runtime, equipLoad, spellSchools, armor);
    }
}
