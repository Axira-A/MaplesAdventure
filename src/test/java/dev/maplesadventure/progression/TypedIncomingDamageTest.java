package dev.maplesadventure.progression;

import java.util.*;
import dev.maplesadventure.progression.defense.*;
import dev.maplesadventure.progression.weapon.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TypedIncomingDamageTest {
    @Test void providerBoundsRejectInvalidPressure() {
        assertThrows(IllegalArgumentException.class,()->TypedIncomingDamageContext.generic(Map.of(WeaponDamageChannel.FIRE,Double.NaN),"BAD","test"));
        assertThrows(IllegalArgumentException.class,()->TypedIncomingDamageContext.generic(Map.of(WeaponDamageChannel.FIRE,-1.0),"BAD","test"));
        assertThrows(IllegalArgumentException.class,()->TypedIncomingDamageContext.generic(Map.of(WeaponDamageChannel.FIRE,1000001.0),"BAD","test"));
    }
    @Test void normalizesExplicitProviderSlices() {
        var t=TypedIncomingDamageContext.generic(Map.of(WeaponDamageChannel.SLASH,3.0,WeaponDamageChannel.FIRE,2.0),"ADAPTER","test");
        assertEquals(5,t.attackPower()); assertEquals(.6,t.slices().get(0).share());
        assertTrue(t.weapon().isEmpty());
    }
    @Test void weaponContextRetainsOriginalBundleAndZeroFireCannotReset() {
        var bundle=new WeaponDamageBundle(Map.of(WeaponDamageChannel.PHYSICAL,new WeaponDamageBundle.ChannelAttack(7,0),
                WeaponDamageChannel.FIRE,new WeaponDamageBundle.ChannelAttack(0,0)),7,1,1);
        var hit=new WeaponHitContext(net.minecraft.resources.ResourceLocation.parse("test:weapon"),bundle,
                new WeaponRequirementResult(true,Map.of(),1,true),false,UUID.randomUUID());
        var typed=TypedIncomingDamageContext.weapon(hit);
        assertSame(bundle,typed.weapon().orElseThrow().bundle()); assertFalse(CombatHitLifecycle.hasFire(hit));
    }
    @Test void fireFrozenBundleRemainsFire() {
        var b=new WeaponDamageBundle(Map.of(WeaponDamageChannel.FIRE,new WeaponDamageBundle.ChannelAttack(7,0)),7,1,1);
        var hit=new WeaponHitContext(net.minecraft.resources.ResourceLocation.parse("test:bow"),b,
                new WeaponRequirementResult(true,Map.of(),1,true),true,UUID.randomUUID());
        assertTrue(CombatHitLifecycle.hasFire(hit)); assertEquals("FROZEN_WEAPON",TypedIncomingDamageContext.weapon(hit).sourceKind());
    }
}
