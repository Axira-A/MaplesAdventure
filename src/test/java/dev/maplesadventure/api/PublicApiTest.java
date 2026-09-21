package dev.maplesadventure.api;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import dev.maplesadventure.api.status.*;
import dev.maplesadventure.api.damage.*;
import dev.maplesadventure.api.defense.*;
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
                "integration.TypedDamageProvider","event.StatusBuildupEvent","event.StatusProcEvent","event.StatusClearEvent")) {
            var type=Class.forName("dev.maplesadventure.api."+name);
            for(var method:type.getDeclaredMethods()) if(java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                var signature=method.toGenericString();
                assertFalse(signature.contains("dev.maplesadventure.progression."),signature);
                assertFalse(signature.contains("dev.maplesadventure.multiplayer."),signature);
                assertFalse(signature.contains("net.minecraft.client."),signature);
                assertFalse(signature.contains("yesman.epicfight."),signature);
                assertFalse(signature.contains("io.redspace."),signature);
            }
        }
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
