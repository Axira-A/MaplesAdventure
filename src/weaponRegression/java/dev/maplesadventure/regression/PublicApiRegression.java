package dev.maplesadventure.regression;

import java.util.*;
import java.util.function.Consumer;
import dev.maplesadventure.api.status.*;
import dev.maplesadventure.api.defense.*;
import dev.maplesadventure.api.damage.*;
import dev.maplesadventure.api.event.*;
import dev.maplesadventure.multiplayer.phase.*;
import dev.maplesadventure.multiplayer.phase.mob.*;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Arrow;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import static dev.maplesadventure.regression.PlayerDefenseRegression.check;

/** Opt-in actual dedicated-server API contract checks. No fixture code ships in the mod. */
public final class PublicApiRegression {
    public static void register() {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event)->event.getDispatcher().register(
            Commands.literal("apiregression").requires(s->s.hasPermission(2)).executes(c->run(c.getSource()))));
    }
    private static int run(net.minecraft.commands.CommandSourceStack command) {
        var level=command.getLevel();
        var mob=Objects.requireNonNull(EntityType.ZOMBIE.create(level));
        mob.setNoAi(true); mob.setNoGravity(true); mob.setPos(level.getSharedSpawnPos().getCenter().add(0,30,0));
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); mob.setHealth(1000);
        level.getChunk(mob.blockPosition()); level.addFreshEntity(mob);
        var attacker=PlayerDefenseRegression.player(level,"PublicApiSource");
        var player=PlayerDefenseRegression.player(level,"PublicApiTarget");
        PhaseManager.assignSolo(attacker); PhaseManager.assignSolo(player);
        int[] events={0,0,0,0};
        Consumer<StatusBuildupEvent> buildup=e->{if(e.target()==mob) events[0]++;};
        Consumer<StatusProcEvent> proc=e->{if(e.target()==mob) {
            events[1]++;
            check(MaplesStatusApi.requestProc(mob,e.type(),StatusSource.environment()).outcome()==StatusApplyResult.Outcome.REENTRANT,"API event recursion rejected");
            check(!MaplesDefenseApi.assignProfile(mob,ResourceLocation.parse("weaponregression:status13")),"event profile mutation rejected");
        }};
        Consumer<StatusClearEvent> clear=e->{if(e.target()==mob) events[2]++;};
        Consumer<LivingDamageEvent.Pre> hurt=e->{if(e.getEntity()==mob||e.getEntity()==player) events[3]++;};
        NeoForge.EVENT_BUS.addListener(buildup); NeoForge.EVENT_BUS.addListener(proc);
        NeoForge.EVENT_BUS.addListener(clear); NeoForge.EVENT_BUS.addListener(hurt);
        try {
            check(MaplesStatusApi.query(mob,MaplesStatusType.BLEED).orElseThrow().threshold()==160,"API query existing default");
            check(MaplesStatusApi.apply(mob,MaplesStatusType.BLEED,Double.NaN,StatusSource.environment()).outcome()==StatusApplyResult.Outcome.INVALID_AMOUNT,"API invalid amount");
            check(MaplesStatusApi.apply(mob,null,1,StatusSource.environment()).outcome()==StatusApplyResult.Outcome.UNSUPPORTED_STATUS,"API unsupported status");
            var added=MaplesStatusApi.apply(mob,MaplesStatusType.BLEED,10,StatusSource.fromEntity(attacker));
            check(added.outcome()==StatusApplyResult.Outcome.APPLIED&&added.before().orElseThrow().buildup()==0&&added.after().orElseThrow().buildup()==10,"API apply snapshots");
            check(events[0]==1&&events[1]==0&&events[3]==0,"apply one event, no premature hurt");
            MaplesStatusApi.clearBuildup(mob,MaplesStatusType.BLEED);
            check(events[2]==1&&MaplesStatusApi.query(mob,MaplesStatusType.BLEED).orElseThrow().buildup()==0,"clear buildup");
            MaplesStatusApi.clear(mob,MaplesStatusType.BLEED);
            check(events[2]==1,"empty entry cleanup does not duplicate Clear event");
            float hp=mob.getHealth(); int prior=events[3];
            check(MaplesStatusApi.requestProc(mob,MaplesStatusType.BLEED,StatusSource.environment()).procced(),"API proc");
            check(events[1]==1&&events[3]==prior+1&&Math.abs(hp-mob.getHealth()-150)<.01,"one proc notification, one hurt, unchanged balance");
            check(MaplesStatusApi.requestProc(mob,MaplesStatusType.MADNESS,StatusSource.environment()).outcome()==StatusApplyResult.Outcome.IMMUNE,"ordinary mob immune");
            MaplesStatusApi.requestProc(mob,MaplesStatusType.POISON,StatusSource.environment());
            check(MaplesStatusApi.requestProc(mob,MaplesStatusType.POISON,StatusSource.environment()).outcome()==StatusApplyResult.Outcome.ALREADY_ACTIVE,"duplicate active proc rejected");
            MaplesStatusApi.clear(mob,MaplesStatusType.POISON);
            check(!MaplesStatusApi.query(mob,MaplesStatusType.POISON).orElseThrow().active(),"API cure");
            mob.setData(ModPhaseAttachments.MOB_PHASE,MobPhaseState.prototype(PhaseId.solo(UUID.randomUUID())));
            check(MaplesStatusApi.apply(mob,MaplesStatusType.BLEED,10,StatusSource.fromEntity(attacker)).outcome()==StatusApplyResult.Outcome.PHASE_DENIED,"API Phase denied before mutation");
            var arrow=new Arrow(EntityType.ARROW,level); arrow.setOwner(attacker);
            check(MaplesStatusApi.apply(mob,MaplesStatusType.BLEED,10,StatusSource.fromProjectile(arrow)).outcome()==StatusApplyResult.Outcome.PHASE_DENIED,"API projectile owner Phase denied");
            mob.removeData(ModPhaseAttachments.MOB_PHASE);
            check(!MaplesDefenseApi.assignProfile(player,ResourceLocation.parse("weaponregression:status13")),"player override forbidden");
            check(!MaplesDefenseApi.assignProfile(mob,ResourceLocation.parse("fixture:missing")),"unknown profile rejected");
            check(MaplesDefenseApi.assignProfile(mob,ResourceLocation.parse("weaponregression:status13")),"existing profile assigned");
            var view=MaplesDefenseApi.query(mob).orElseThrow();
            check(view.channels().size()==9&&view.statuses().size()==7&&view.profile().orElseThrow().equals(ResourceLocation.parse("weaponregression:status13")),"full defense view");
            check(MaplesDefenseApi.clearProfileOverride(mob),"clear profile override");
            var offThread=java.util.concurrent.CompletableFuture.supplyAsync(()->MaplesStatusApi.apply(mob,MaplesStatusType.BLEED,1,StatusSource.environment())).join();
            check(offThread.outcome()==StatusApplyResult.Outcome.WRONG_THREAD,"off-thread authority rejected");
            var magic=mob.damageSources().indirectMagic(mob,mob);
            MaplesTypedDamageApi.register(ResourceLocation.parse("weaponregression:public_api"),1000,source->
                source==magic?Optional.of(new TypedDamage(Map.of(MaplesDamageChannel.MAGIC,20.,MaplesDamageChannel.FIRE,10.))):Optional.empty());
            int hits=events[3]; player.hurt(magic,10);
            check(events[3]==hits+1,"typed provider describes single actual hurt");
            var resolved=dev.maplesadventure.progression.defense.LastWeaponDamageResolution.get(mob.getUUID()).orElseThrow();
            check(resolved.typedSource().equals("INTEGRATION")&&resolved.resolution().channelResults().size()==2,"MAGIC + FIRE provider used");
            MaplesTypedDamageApi.register(ResourceLocation.parse("weaponregression:public_api"),1000,source->Optional.empty());
            mob.discard();
            check(MaplesStatusApi.apply(mob,MaplesStatusType.BLEED,1,StatusSource.environment()).outcome()==StatusApplyResult.Outcome.INVALID_TARGET,"removed target rejected");
            command.sendSuccess(()->Component.literal("Public API regression PASS"),true);
            return 1;
        } finally {
            NeoForge.EVENT_BUS.unregister(buildup); NeoForge.EVENT_BUS.unregister(proc);
            NeoForge.EVENT_BUS.unregister(clear); NeoForge.EVENT_BUS.unregister(hurt);
            dev.maplesadventure.progression.status.StatusRuntimeService.forget(mob);
            mob.discard();
        }
    }
}
