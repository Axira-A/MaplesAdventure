package dev.maplesadventure.progression.status;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class StatusControlLockTest {
    @Test void controlOutlivesVisualBarAndExpires() {
        var s=new StatusRuntimeState(); s.lockType=StatusEffectType.SLEEP; s.lockStart=100; s.lockEnd=130;
        s.remove(StatusEffectType.SLEEP); assertTrue(s.locked(129)); assertFalse(s.locked(130)); assertFalse(s.empty());
        s.unlock(); assertTrue(s.empty());
    }
    @Test void deepSleepSurvivesSaveWithoutSettingNoAi() {
        var s=new StatusRuntimeState(); s.lockType=StatusEffectType.SLEEP;s.lockStart=100;s.lockEnd=1300;s.deepSleep=true;
        var next=new StatusRuntimeState();next.deserializeNBT(null,s.serializeNBT(null)); assertTrue(next.deepSleep);assertTrue(next.locked(500));
        next.unlock();assertFalse(next.deepSleep);assertFalse(next.locked(500));
    }
}
