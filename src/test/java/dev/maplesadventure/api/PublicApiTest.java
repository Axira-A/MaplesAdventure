package dev.maplesadventure.api;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import dev.maplesadventure.api.status.*;
import dev.maplesadventure.api.damage.*;
import dev.maplesadventure.api.defense.*;
import dev.maplesadventure.api.weapon.*;
import dev.maplesadventure.api.armor.*;
import dev.maplesadventure.integration.api.ApiNotifications;
import net.minecraft.resources.ResourceLocation;

class PublicApiTest {
    @Test void stableStatusIdsAndUnknownIds() {
        assertEquals(7,MaplesStatusType.values().length);
        for(var t:MaplesStatusType.values()) assertEquals(Optional.of(t),MaplesStatusType.find(t.id()));
        assertTrue(MaplesStatusType.find(ResourceLocation.parse("test:unknown")).isEmpty());
        assertTrue(MaplesStatusType.find(null).isEmpty());
    }
    @Test void nullTargetsAreSafeAndNeverCreateState() {
        assertEquals(StatusApplyResult.Outcome.INVALID_TARGET,MaplesStatusApi.apply(null,MaplesStatusType.BLEED,1,StatusSource.environment()).outcome());
        assertTrue(MaplesStatusApi.query(null,MaplesStatusType.BLEED).isEmpty());
        assertTrue(MaplesDefenseApi.query(null).isEmpty());
        assertFalse(MaplesDefenseApi.assignProfile(null,ResourceLocation.parse("test:boss")));
    }
    @Test void typedDescriptionCopiesAndValidates() {
        var input=new EnumMap<MaplesDamageChannel,Double>(MaplesDamageChannel.class);
        input.put(MaplesDamageChannel.MAGIC,20.);
        var description=new TypedDamage(input);
        input.put(MaplesDamageChannel.MAGIC,99.);
        assertEquals(20.,description.channels().get(MaplesDamageChannel.MAGIC));
        assertThrows(UnsupportedOperationException.class,()->description.channels().clear());
        for(double bad:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY,1000001})
            assertThrows(IllegalArgumentException.class,()->new TypedDamage(Map.of(MaplesDamageChannel.FIRE,bad)));
        assertThrows(IllegalArgumentException.class,()->new TypedDamage(Map.of()));
        assertThrows(IllegalArgumentException.class,()->new TypedDamage(Map.of(MaplesDamageChannel.FIRE,0.)));
    }
    @Test void defenseMapsAreDetached() {
        var input=new EnumMap<MaplesDamageChannel,DefenseView.Channel>(MaplesDamageChannel.class);
        input.put(MaplesDamageChannel.PHYSICAL,new DefenseView.Channel(20,0));
        var view=new DefenseView(input,Map.of(),Optional.empty(),false,.35);
        input.clear();
        assertEquals(1,view.channels().size());
        assertThrows(UnsupportedOperationException.class,()->view.channels().clear());
        assertThrows(UnsupportedOperationException.class,()->view.statuses().clear());
    }
    @Test void readOnlyGuardRestoresAfterFailureAndNesting() {
        assertFalse(ApiNotifications.busy());
        assertThrows(IllegalStateException.class,()->ApiNotifications.readOnly(()->{
            assertTrue(ApiNotifications.busy());
            ApiNotifications.readOnly(()->{assertTrue(ApiNotifications.busy());return null;});
            throw new IllegalStateException("fixture");
        }));
        assertFalse(ApiNotifications.busy());
    }
    @Test void publicSignaturesNeverExposeInternals() throws Exception {
        for(String name:List.of("status.MaplesStatusApi","status.StatusSource","status.StatusView","status.StatusApplyResult",
                "defense.MaplesDefenseApi","defense.DefenseView","damage.MaplesTypedDamageApi","damage.TypedDamage",
                "integration.TypedDamageProvider","event.StatusBuildupEvent","event.StatusProcEvent","event.StatusClearEvent",
                "status.MaplesResistanceType", "weapon.MaplesWeaponAttribute", "weapon.WeaponRequirementView",
                "weapon.WeaponScalingView", "weapon.WeaponDamageComponentView", "weapon.WeaponStatusComponentView",
                "weapon.WeaponInfusionInfo", "weapon.WeaponProfileView", "weapon.WeaponAttackChannelView",
                "weapon.WeaponEvaluationView", "weapon.MaplesWeaponApi", "armor.ArmorProfileView",
                "armor.ArmorEquipmentView", "armor.MaplesArmorApi")) {
            var type=Class.forName("dev.maplesadventure.api."+name);
            var signatures = new java.util.ArrayList<String>();
            for(var method:type.getDeclaredMethods()) if(java.lang.reflect.Modifier.isPublic(method.getModifiers())) signatures.add(method.toGenericString());
            for(var constructor:type.getDeclaredConstructors()) if(java.lang.reflect.Modifier.isPublic(constructor.getModifiers())) signatures.add(constructor.toGenericString());
            for(var field:type.getDeclaredFields()) if(java.lang.reflect.Modifier.isPublic(field.getModifiers())) signatures.add(field.toGenericString());
            if(type.isRecord()) for(var component:type.getRecordComponents()) signatures.add(component.getGenericType().getTypeName());
            for(var signature:signatures) {
                assertFalse(signature.contains("dev.maplesadventure.progression."),signature);
                assertFalse(signature.contains("dev.maplesadventure.multiplayer."),signature);
                assertFalse(signature.contains("net.minecraft.client."),signature);
                assertFalse(signature.contains("yesman.epicfight."),signature);
                assertFalse(signature.contains("io.redspace."),signature);
            }
        }
    }
    @Test void equipmentPublicIdsAndViewsAreStableAndDetached() {
        assertEquals(5, MaplesWeaponAttribute.values().length);
        assertEquals(9, MaplesDamageChannel.values().length);
        assertEquals(7, MaplesStatusType.values().length);
        assertEquals(4, MaplesResistanceType.values().length);
        for(var type:MaplesWeaponAttribute.values()) assertEquals(Optional.of(type),MaplesWeaponAttribute.find(type.id()));
        for(var type:MaplesResistanceType.values()) assertEquals(Optional.of(type),MaplesResistanceType.find(type.id()));
        var req = new EnumMap<MaplesWeaponAttribute,Integer>(MaplesWeaponAttribute.class);
        req.put(MaplesWeaponAttribute.FAITH,24);
        var view = new WeaponRequirementView(req); req.put(MaplesWeaponAttribute.FAITH,99);
        assertEquals(24,view.get(MaplesWeaponAttribute.FAITH));
        assertThrows(UnsupportedOperationException.class,()->view.requirements().clear());
        var channels = new EnumMap<MaplesDamageChannel,Double>(MaplesDamageChannel.class);
        channels.put(MaplesDamageChannel.HOLY,18.);
        var profile = new ArmorProfileView(ResourceLocation.parse("example:chest"),channels,Map.of(MaplesResistanceType.ROBUSTNESS,12.));
        channels.clear();
        assertEquals(18.,profile.channels().get(MaplesDamageChannel.HOLY));
        assertThrows(UnsupportedOperationException.class,()->profile.channels().clear());
        var equipped = new ArmorEquipmentView(profile.channels(),profile.resistances(),Map.of(net.minecraft.world.entity.EquipmentSlot.CHEST,profile));
        assertThrows(UnsupportedOperationException.class,()->equipped.slots().clear());
        var scaling = new WeaponScalingView(Map.of(MaplesWeaponAttribute.FAITH,.8),1.15);
        var damage = new WeaponDamageComponentView(MaplesDamageChannel.HOLY,.35,scaling);
        var weapon = new WeaponProfileView(ResourceLocation.parse("example:blade"),20,new WeaponRequirementView(Map.of()),scaling,
                new java.util.ArrayList<>(List.of(damage)),List.of(),new WeaponInfusionInfo(ResourceLocation.parse("maplesadventure:normal"),
                WeaponInfusionInfo.State.NORMAL,Optional.empty()),Set.of(ResourceLocation.parse("maplesadventure:normal")),Optional.empty());
        assertThrows(UnsupportedOperationException.class,()->weapon.damageComponents().clear());
        assertThrows(IllegalArgumentException.class,()->new WeaponScalingView(Map.of(MaplesWeaponAttribute.FAITH,Double.NaN),1));
    }
    @Test void documentedDatapackExamplesUseRealParsers() throws Exception {
        var root=java.nio.file.Path.of("docs/integration/examples/datapack/data/example/maplesadventure");
        var profile=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(root.resolve("entity_defense_profiles/boss.json"))).getAsJsonObject();
        var rule=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(root.resolve("entity_defense_rules/boss.json"))).getAsJsonObject();
        var parsed=dev.maplesadventure.progression.defense.EntityDefenseRegistry.parseProfile(ResourceLocation.parse("example:boss"),profile);
        assertEquals(9,parsed.channels().size());
        assertEquals(7,parsed.statusResistances().size());
        assertEquals(parsed.profileId(),dev.maplesadventure.progression.defense.EntityDefenseRegistry.parseRule(ResourceLocation.parse("example:boss"),rule).profile());
    }
}
