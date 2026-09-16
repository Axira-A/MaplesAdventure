package dev.maplesadventure.progression.status;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import dev.maplesadventure.progression.status.client.StatusHudMath;

class StatusHudMathTest {
    @Test void resistanceChangesFillNotTextureWidth() {
        var row=new StatusNetwork.Row(StatusEffectType.BLEED,StatusNetwork.HUDMode.BUILDUP,30,200,0,0,60,5,0);
        assertEquals(.15,StatusHudMath.fill(row,0)); assertEquals(.15,StatusHudMath.fill(row,59));
        assertEquals(.125,StatusHudMath.fill(row,80)); assertEquals(0,StatusHudMath.fill(row,10000));
    }
    @Test void durationStartsFullAndCountsDownNotCurrentBuildup() {
        var row=new StatusNetwork.Row(StatusEffectType.POISON,StatusNetwork.HUDMode.ACTIVE_DURATION,0,100,1200,1200,0,5,1);
        assertEquals(1,StatusHudMath.fill(row,0)); assertEquals(.5,StatusHudMath.fill(row,600)); assertEquals(0,StatusHudMath.fill(row,1200));
    }
    @Test void pixelCropIsIntegerAndDoesNotStretch() {
        assertEquals(0,StatusHudMath.fillPixels(0,107)); assertEquals(26,StatusHudMath.fillPixels(.25,107));
        assertEquals(53,StatusHudMath.fillPixels(.5,107)); assertEquals(107,StatusHudMath.fillPixels(1,107));
    }
    @Test void palettePreservesAlphaAndRedCanBecomeGreen() {
        int p=StatusHudMath.remap(0xAB1010FF,0x789B36);
        assertEquals(0xAB,p>>>24); assertTrue((p>>>8&255)>(p&255));
        int dark=StatusHudMath.remap(0xAB080880,0x789B36);
        assertTrue((dark>>>8&255)<(p>>>8&255));
    }
}
