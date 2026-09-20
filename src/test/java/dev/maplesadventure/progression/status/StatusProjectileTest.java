package dev.maplesadventure.progression.status;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import dev.maplesadventure.progression.weapon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;

class StatusProjectileTest {
    @Test void launchFreezesArcAndReinfusionCannotMutateIt() {
        var p=new WeaponStatusProfile(List.of(new StatusBuildupComponent(StatusEffectType.POISON,30,.45)));
        var frozen=p.evaluate(40);
        var hit=new WeaponHitContext(ResourceLocation.parse("minecraft:bow"),WeaponDamageBundle.legacy(1,.35),
                new WeaponRequirementResult(false,Map.of(),.35,false),true,UUID.randomUUID(),frozen);
        var projectile=new ProjectileRequirementPenalty(hit); var loaded=new ProjectileRequirementPenalty();
        loaded.deserializeNBT(null,projectile.serializeNBT(null));
        assertEquals(frozen,loaded.context().statuses()); assertNotEquals(p.evaluate(99),frozen);
        assertEquals(5,projectile.serializeNBT(null).getInt("dataVersion"));
    }
    @Test void oldNbtHasEmptyStatusWithoutChangingOldDamage() {
        var n=new CompoundTag(); n.putInt("dataVersion",2); n.putDouble("multiplier",.35); n.putDouble("scalingMultiplier",1.5);
        var p=new ProjectileRequirementPenalty(); p.deserializeNBT(null,n);
        assertTrue(p.context().statuses().amounts().isEmpty()); assertEquals(1.5*.35,p.combatMultiplier());
    }
    @Test void malformedStatusDoesNotLoseExistingAttackBundle() {
        var p=new ProjectileRequirementPenalty(UUID.randomUUID(),new WeaponRequirementResult(true,Map.of(),1,true),1.5);
        var n=p.serializeNBT(null); n.getCompound("statuses").putDouble("bleed",Double.NaN);
        var q=new ProjectileRequirementPenalty(); q.deserializeNBT(null,n);
        assertTrue(q.context().statuses().amounts().isEmpty()); assertEquals(1.5,q.combatMultiplier());
    }
}
