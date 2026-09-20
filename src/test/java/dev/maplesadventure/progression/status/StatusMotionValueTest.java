package dev.maplesadventure.progression.status;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
class StatusMotionValueTest {
    @Test void datapackSelectorRequiresString() {
        var invalid=com.google.gson.JsonParser.parseString("{\"animation\":123,\"motion\":0.4}").getAsJsonObject();
        assertThrows(IllegalArgumentException.class,()->StatusMotionRules.parse(invalid));
    }
    @Test void explicitFourHitSourceTotalsOnePointSixNotFour() {
        var base=new StatusBuildupSnapshot(Map.of(StatusEffectType.BLEED,28.));
        var attack=new StatusMotionValueResolver.Attack(null,null,null,.4);
        double mv=StatusMotionValueResolver.resolve(attack,"SWORD",null).value();
        assertEquals(28*1.6,4*StatusMotionValueResolver.apply(base,mv).amounts().get(StatusEffectType.BLEED),1e-12);
        assertEquals(1,StatusMotionValueResolver.resolve((StatusMotionValueResolver.Attack)null,"SWORD",null).value());
    }
    @Test void motionIsValidatedAndIndependentOfDamage() {
        assertThrows(IllegalArgumentException.class,()->new StatusMotionValueResolver.Decision(Double.NaN,"TEST"));
        assertThrows(IllegalArgumentException.class,()->new StatusMotionValueResolver.Decision(5,"TEST"));
        var c=new StatusBuildupComponent(StatusEffectType.BLEED,28,.3);
        assertEquals(c.amount(99)*.4,c.amount(99,0,.4),1e-12);
    }
}
