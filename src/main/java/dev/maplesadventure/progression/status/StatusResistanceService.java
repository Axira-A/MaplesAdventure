package dev.maplesadventure.progression.status;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
public final class StatusResistanceService {
    public static StatusResistance resolve(LivingEntity target,StatusEffectType type) {
        if(target instanceof net.minecraft.server.level.ServerPlayer player) return new StatusResistance(PlayerStatusResistanceCalculator.calculate(
                dev.maplesadventure.progression.PlayerAttributeService.state(player)).value(type.resistanceType()),false,1);
        var profile=dev.maplesadventure.progression.defense.EntityDefenseService.resolve(target).profile();
        var base=profile.statusResistances().getOrDefault(type,StatusResistance.DEFAULT);
        boolean primaryBoss=target.getExistingData(dev.maplesadventure.multiplayer.phase.mob.ModPhaseAttachments.ENCOUNTER_MOB)
                .map(dev.maplesadventure.multiplayer.encounter.EncounterMobState::isPrimary).orElse(false);
        boolean immune=base.immune() || type==StatusEffectType.SLEEP&&base.sleepResponse()==SleepResponse.IMMUNE
                || type==StatusEffectType.MADNESS&&(!profile.statusTraits().tarnishedLike()||profile.statusTraits().madnessImmune())
                || type==StatusEffectType.DEATH_BLIGHT&&(!profile.statusTraits().tarnishedLike()&&!profile.statusTraits().allowDeathBlight()
                    ||primaryBoss&&!profile.statusTraits().allowDeathBlight()||profile.statusTraits().deathBlightImmune());
        return new StatusResistance(Math.min(100000,base.threshold()+StatusResistanceCorrectionService.offset(target,type,base)),immune,
                base.procDamageMultiplier(),base.correction(),base.sleepResponse());
    }
    private StatusResistanceService() {}
}
