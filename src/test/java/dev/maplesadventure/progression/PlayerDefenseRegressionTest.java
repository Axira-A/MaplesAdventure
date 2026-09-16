package dev.maplesadventure.progression;

import java.util.*;
import dev.maplesadventure.progression.defense.*;
import dev.maplesadventure.progression.weapon.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerDefenseRegressionTest {
    @Test void enemyRound10ReferenceBitsPreserved() {
        var random=new Random(12);
        for(int i=0;i<1000;i++) {
            double physical=random.nextDouble()*20,fire=random.nextDouble()*10;
            var b=new WeaponDamageBundle(Map.of(WeaponDamageChannel.PHYSICAL,new WeaponDamageBundle.ChannelAttack(physical,physical*.35),
                    WeaponDamageChannel.FIRE,new WeaponDamageBundle.ChannelAttack(fire,fire*.15)),physical+fire,
                    (physical*1.35+fire*1.15)/(physical+fire),i%2==0?1:.35);
            var p=new EntityDefenseProfile(net.minecraft.resources.ResourceLocation.parse("test:enemy"),Map.of(
                    WeaponDamageChannel.PHYSICAL,new ChannelDefense(random.nextDouble()*100,0),
                    WeaponDamageChannel.FIRE,new ChannelDefense(random.nextDouble()*100,.2)),"TEST");
            double damage=random.nextDouble()*100;
            double nominal=damage*b.nominalMultiplier(),qualified=0;
            for(var e:b.channels().entrySet()) {
                double ar=e.getValue().attackRating(),share=ar/b.totalAttackRating();
                qualified+=nominal*share*DefenseMitigationCurve.finalChannelMultiplier(ar,p.channel(e.getKey()),.35);
            }
            double reference=qualified*b.requirementMultiplier();
            assertEquals(Double.doubleToLongBits(reference),Double.doubleToLongBits(WeaponCombatResolutionService.resolve(damage,b,p,.35).finalDamage()));
        }
    }
    @Test void buildNeverClampedAsPercentage() {
        var p=new PlayerDefenseSnapshot(Arrays.stream(WeaponDamageChannel.values()).collect(java.util.stream.Collectors.toMap(c->c,
                c->new dev.maplesadventure.progression.stats.StatBreakdown(150,0,0,0))));
        assertEquals(150,p.channel(WeaponDamageChannel.PHYSICAL).defense());
    }
}
