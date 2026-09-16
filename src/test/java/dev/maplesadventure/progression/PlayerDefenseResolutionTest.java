package dev.maplesadventure.progression;

import java.util.*;
import dev.maplesadventure.progression.defense.*;
import dev.maplesadventure.progression.weapon.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerDefenseResolutionTest {
    static WeaponDamageBundle bundle(double penalty) {
        return new WeaponDamageBundle(Map.of(WeaponDamageChannel.PHYSICAL,new WeaponDamageBundle.ChannelAttack(7,3)),7,10.0/7,penalty);
    }
    @Test void pvpBuildMitigatesWeaponAr() {
        var p=PlayerDefenseService.preview(PlayerDefenseCalculatorTest.all(5));
        var r=WeaponCombatResolutionService.resolve(7,bundle(1),p,.1);
        assertEquals(10*(10.0/11),r.finalDamage(),1e-12);
    }
    @Test void requirementLastExactlyOnce() {
        var p=PlayerDefenseService.preview(PlayerDefenseCalculatorTest.all(40));
        double a=WeaponCombatResolutionService.resolve(7,bundle(1),p,.1).finalDamage();
        double b=WeaponCombatResolutionService.resolve(7,bundle(.35),p,.1).finalDamage();
        assertEquals(a*.35,b);
    }
    @Test void pressuresIndependentAndPlayerLowerBoundSupported() {
        var p=PlayerDefenseService.preview(PlayerDefenseCalculatorTest.all(40));
        assertTrue(WeaponCombatResolutionService.resolve(7,bundle(1),p,.1).finalDamage()>
                WeaponCombatResolutionService.resolve(7,bundle(1),p,.35).finalDamage());
        assertDoesNotThrow(()->DefenseMitigationCurve.penetration(7,10,.01));
    }
    @Test void splitUsesDifferentChannelDefenses() {
        var attrs=PlayerDefenseCalculatorTest.all(5).with(Attribute.FAITH,99,99);
        var b=new WeaponDamageBundle(Map.of(WeaponDamageChannel.SLASH,new WeaponDamageBundle.ChannelAttack(5,0),
                WeaponDamageChannel.HOLY,new WeaponDamageBundle.ChannelAttack(5,0)),10,1,1);
        var r=WeaponCombatResolutionService.resolve(10,b,PlayerDefenseService.preview(attrs),.1);
        assertTrue(r.channelResults().get(0).defenseMultiplier()>r.channelResults().get(1).defenseMultiplier());
    }
    @Test void genericPressureDoesNotBecomeWeaponScaling() {
        var context=TypedIncomingDamageContext.generic(Map.of(WeaponDamageChannel.PHYSICAL,6.0),"MOB","test");
        var r=WeaponCombatResolutionService.resolve(6,context,PlayerDefenseService.preview(PlayerDefenseCalculatorTest.all(5)),.1);
        assertEquals(6,r.nominalDamage()); assertEquals(1,r.requirementMultiplier()); assertEquals(36.0/7,r.finalDamage(),1e-12);
    }
}
