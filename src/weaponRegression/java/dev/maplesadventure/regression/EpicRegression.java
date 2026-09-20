package dev.maplesadventure.regression;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.skill.SkillSlots;
final class EpicRegression {
    static void statusMotion(ServerPlayer player,net.minecraft.world.entity.LivingEntity target) {
        var patch=EpicFightCapabilities.getEntityPatch(target,yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch.class);
        if(patch==null) throw new IllegalStateException("Fixture requires a real joined Epic Fight entity patch");
        var animations=java.util.List.of(yesman.epicfight.gameasset.Animations.RUSHING_TEMPO1,
                yesman.epicfight.gameasset.Animations.BLADE_RUSH_COMBO1,yesman.epicfight.gameasset.Animations.EVISCERATE_FIRST,
                yesman.epicfight.gameasset.Animations.RELENTLESS_COMBO);
        for(var accessor:animations) {
            var animation=accessor.get(); double total=0;
            for(var phase:animation.phases) {
                // Public source factory executes the production optional Mixin, not a manual attach.
                var damage=animation.getEpicFightDamageSource(player.damageSources().playerAttack(player),patch,target,phase)
                        .setUsedItem(player.getMainHandItem());
                var decision=dev.maplesadventure.progression.status.StatusMotionValueResolver.resolve(damage,"sword");
                if(!decision.source().equals("ANIMATION")||decision.value()>=1) throw new IllegalStateException("Motion Hook did not capture "+accessor.registryName()+": "+decision);
                total+=decision.value();
            }
            dev.maplesadventure.MaplesAdventure.LOGGER.info("[Status regression] PASS real EF source factory animation={} phases={} totalMotion={}",accessor.registryName(),animation.phases.length,total);
        }
    }
    static net.minecraft.world.damagesource.DamageSource defenseSource(ServerPlayer player) {
        return new yesman.epicfight.world.damagesource.EpicFightDamageSource(player.damageSources().playerAttack(player))
                .setUsedItem(player.getMainHandItem()).setAnimation(yesman.epicfight.gameasset.Animations.SWORD_AUTO1)
                .setStunType(yesman.epicfight.world.damagesource.StunType.NONE);
    }
    static int usedItem(CommandSourceStack source,ServerPlayer player) {
        for(var stack:java.util.List.of(player.getMainHandItem(),player.getOffhandItem(),net.minecraft.world.item.ItemStack.EMPTY)) {
            var damage=new yesman.epicfight.world.damagesource.EpicFightDamageSource(player.damageSources().playerAttack(player)).setUsedItem(stack)
                    .setAnimation(yesman.epicfight.gameasset.Animations.SWORD_AUTO1).setStunType(yesman.epicfight.world.damagesource.StunType.NONE);
            double actual=dev.maplesadventure.progression.weapon.WeaponDamagePolicy.multiplier(damage);
            double expected=dev.maplesadventure.progression.weapon.WeaponDamagePolicy.weaponMultiplier(player,stack);
            source.sendSuccess(()->Component.literal("UsedItem="+stack+" actual="+actual+" expected="+expected+" pass="+(actual==expected)),false);
            var target=net.minecraft.world.entity.EntityType.ZOMBIE.create(player.serverLevel());
            target.setNoAi(true); target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
            target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0); target.setHealth(1000);
            target.setPos(player.position().add(3,0,0)); player.serverLevel().addFreshEntity(target);
            int[] events={0}; java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre> observer=e->{if(e.getEntity()==target)events[0]++;};
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(observer);
            try {
                target.hurt(damage,100); float loss=1000-target.getHealth();
                source.sendSuccess(()->Component.literal("EF actual hurt="+loss+" expected="+(100*expected)+" damageEvents="+events[0]+" pass="+(Math.abs(loss-100*expected)<.01&&events[0]==1)),false);
            } finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(observer); target.discard(); }
        }
        return 1;
    }
    static int innate(CommandSourceStack source,ServerPlayer player) {
        var patch=EpicFightCapabilities.getEntityPatch(player,ServerPlayerPatch.class);
        var cap=EpicFightCapabilities.getItemStackCapability(player.getMainHandItem());
        cap.changeWeaponInnateSkill(patch,player.getMainHandItem());
        var container=patch.getSkill(SkillSlots.WEAPON_INNATE);
        if(container.isEmpty()) throw new IllegalStateException("No actual weapon innate");
        container.setResource(container.getMaxResource()); container.setStack(1); container.setReplaceCooldown(0);
        patch.setStamina(patch.getMaxStamina());
        float stamina=patch.getStamina(), resource=container.getResource();
        int stack=container.getStack(), duration=container.getRemainDuration(), cooldown=container.getReplaceCooldown();
        boolean result=container.requestCasting(patch,new CompoundTag());
        boolean unchanged=stamina==patch.getStamina() && resource==container.getResource() && stack==container.getStack()
                && duration==container.getRemainDuration() && cooldown==container.getReplaceCooldown();
        source.sendSuccess(()->Component.literal("innate="+container.getSkill()+" accepted="+result+" unchanged="+unchanged
                +" stamina="+stamina+"/"+patch.getStamina()+" resource="+resource+"/"+container.getResource()),false);
        return !result&&unchanged?1:0;
    }
}
