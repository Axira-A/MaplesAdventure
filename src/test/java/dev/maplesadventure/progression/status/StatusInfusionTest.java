package dev.maplesadventure.progression.status;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import dev.maplesadventure.progression.weapon.*;
import java.util.*;
class StatusInfusionTest {
    @Test void maximumDatapackBuildupRemainsValidAfterWeightTransform() {
        var profile=new WeaponStatusProfile(List.of(new StatusBuildupComponent(StatusEffectType.BLEED,1000,.3)));
        var weighted=profile.forWeight(StatusWeaponWeightClass.COLOSSAL);
        assertEquals(1000,weighted.components().getFirst().baseBuildup());
        assertEquals(1300,weighted.evaluate(99).amounts().get(StatusEffectType.BLEED),1e-9);
    }
    @Test void newInfusionsHaveCanonicalSourcesAndNoInventedDamageChannels() {
        var registry=WeaponInfusionRegistry.definitions(); assertEquals(14,registry.size()); assertEquals(32,WeaponInfusionDefinition.MAX_DEFINITIONS);
        var expected=Map.of(WeaponInfusionRegistry.COLD_ID,StatusEffectType.FROSTBITE,WeaponInfusionRegistry.SLUMBER_ID,StatusEffectType.SLEEP,
                WeaponInfusionRegistry.FRENZIED_ID,StatusEffectType.MADNESS,WeaponInfusionRegistry.ROT_ID,StatusEffectType.SCARLET_ROT,WeaponInfusionRegistry.BLIGHT_ID,StatusEffectType.DEATH_BLIGHT);
        expected.forEach((id,type)->assertEquals(type,registry.get(id).statuses().components().getFirst().type()));
        assertTrue(registry.get(WeaponInfusionRegistry.OCCULT_ID).statuses().components().isEmpty());
        assertEquals(9,WeaponDamageChannel.values().length);
        var cold=registry.get(WeaponInfusionRegistry.COLD_ID); assertEquals(.95,cold.baseMultiplier()); assertEquals(.7,cold.elementSplit().physicalRatio());
        assertEquals(WeaponDamageChannel.MAGIC,cold.elementSplit().channel());assertEquals(StatusArcaneScalingPolicy.NONE,cold.statuses().components().getFirst().policy());
    }
    @Test void occultOnlyScalesInheritedPermittedStatuses() {
        assertTrue(WeaponStatusProfile.EMPTY.followWeaponArcane().components().isEmpty());
        var profile=new WeaponStatusProfile(List.of(new StatusBuildupComponent(StatusEffectType.BLEED,20,0),new StatusBuildupComponent(StatusEffectType.FROSTBITE,25,0))).followWeaponArcane();
        assertEquals(35,profile.evaluate(99,.75,1).amounts().get(StatusEffectType.BLEED));
        assertEquals(25,profile.evaluate(99,.75,1).amounts().get(StatusEffectType.FROSTBITE));
    }
    @Test void weightTablesAndRestrictedEligibility() {
        assertEquals(12,StatusWeaponWeightClass.THROWING.buildup(StatusEffectType.BLEED));
        assertEquals(28,StatusWeaponWeightClass.NORMAL.buildup(StatusEffectType.BLEED));
        assertEquals(34,StatusWeaponWeightClass.GREAT.buildup(StatusEffectType.BLEED));
        assertEquals(41,StatusWeaponWeightClass.COLOSSAL.buildup(StatusEffectType.BLEED));
        assertTrue(WeaponInfusionRegistry.requiresExplicitEligibility(WeaponInfusionRegistry.BLIGHT_ID));
        assertTrue(WeaponInfusionRegistry.requiresExplicitEligibility(WeaponInfusionRegistry.FRENZIED_ID));
        assertTrue(WeaponInfusionRegistry.requiresExplicitEligibility(WeaponInfusionRegistry.ROT_ID));
        assertFalse(WeaponInfusionRegistry.requiresExplicitEligibility(WeaponInfusionRegistry.COLD_ID));
    }
    @Test void legacyBridgeReadableButBuiltinsUseCanonicalProfiles() {
        assertFalse(WeaponStatusProfile.legacy(WeaponInfusionBuildup.BLEED).components().isEmpty());
        for(var definition:WeaponInfusionRegistry.definitions().values()) assertEquals(WeaponInfusionBuildup.NONE,definition.futureBuildup());
        assertThrows(IllegalArgumentException.class,()->WeaponStatusProfile.parse(com.google.gson.JsonParser.parseString("{\"frostbite\":{\"base_buildup\":30,\"arcane_scaling\":1}}").getAsJsonObject()));
    }
}
