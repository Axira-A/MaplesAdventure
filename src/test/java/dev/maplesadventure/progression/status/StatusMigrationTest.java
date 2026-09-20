package dev.maplesadventure.progression.status;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
class StatusMigrationTest {
    @Test void versionOnePreservesAllFourOriginalStates() {
        var s=new StatusRuntimeState();
        for(var type:new StatusEffectType[]{StatusEffectType.BLEED,StatusEffectType.POISON,StatusEffectType.SCARLET_ROT,StatusEffectType.FROSTBITE}) {
            s.accumulate(type,40,StatusResistance.DEFAULT,StatusSourceContext.admin(type),100);
            if(type.durationBar()) {var e=s.get(type); e.activeStart=100;e.activeEnd=500;e.nextDot=120;}
        }
        var n=s.serializeNBT(null);n.putInt("dataVersion",1);n.remove("procCounts");
        var next=new StatusRuntimeState();next.deserializeNBT(null,n);assertEquals(4,next.entries().size());
        assertEquals(40,next.get(StatusEffectType.BLEED).current);assertEquals(500,next.get(StatusEffectType.POISON).activeEnd);
        assertNull(next.get(StatusEffectType.SLEEP));assertEquals(2,next.serializeNBT(null).getInt("dataVersion"));
    }
    @Test void sevenFrozenAmountsRoundTrip() {
        var map=new java.util.EnumMap<StatusEffectType,Double>(StatusEffectType.class);for(var t:StatusEffectType.values())map.put(t,12.3);
        var s=new StatusBuildupSnapshot(map);assertEquals(s,StatusBuildupSnapshot.load(s.save()));
    }
}
