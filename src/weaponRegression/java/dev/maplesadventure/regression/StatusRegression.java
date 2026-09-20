package dev.maplesadventure.regression;

import com.mojang.authlib.GameProfile;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.multiplayer.phase.*;
import dev.maplesadventure.multiplayer.phase.mob.*;
import dev.maplesadventure.progression.*;
import dev.maplesadventure.progression.status.*;
import dev.maplesadventure.progression.weapon.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

/** Explicit isolated-world integration fixture. Never included in the production JAR. */
public final class StatusRegression {
    private record Pending(int tick, Runnable job) {}
    private static final List<Pending> pending=new ArrayList<>();
    private static final UUID PERSIST_ID=UUID.fromString("11000000-0000-0000-0000-000000000099");
    public static void register() {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e)->e.getDispatcher().register(
            Commands.literal("statusregression").requires(s->s.hasPermission(2))
                .then(Commands.literal("run").executes(c->run(c.getSource())))
                .then(Commands.literal("extended").executes(c->extended(c.getSource())))
                .then(Commands.literal("persist").executes(c->persist(c.getSource(),false)))
                .then(Commands.literal("checkpersist").executes(c->persist(c.getSource(),true)))
                .then(Commands.literal("hud").then(Commands.argument("player",EntityArgument.player())
                    .then(Commands.argument("percent",com.mojang.brigadier.arguments.IntegerArgumentType.integer(0,100))
                        .executes(c->hud(EntityArgument.getPlayer(c,"player"),com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c,"percent"))))))));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e)->{
            var due=pending.stream().filter(p->p.tick()<=e.getServer().getTickCount()).toList();
            pending.removeAll(due); due.forEach(p->p.job().run());
        });
    }
    private static void check(boolean result,String label) {
        if(!result) throw new IllegalStateException("[Status regression] FAIL "+label);
        MaplesAdventure.LOGGER.info("[Status regression] PASS {}",label);
    }
    private static Zombie mob(ServerLevel level,ServerPlayer player) {
        var mob=Objects.requireNonNull(EntityType.ZOMBIE.create(level));
        mob.setNoAi(true); mob.setNoGravity(true); mob.setPersistenceRequired(); mob.setPos(player.position().add(0,0,4));
        mob.setItemSlot(EquipmentSlot.HEAD,Items.NETHERITE_HELMET.getDefaultInstance());
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); mob.getAttribute(Attributes.ARMOR).setBaseValue(20);
        mob.setHealth(1000); level.addFreshEntity(mob); return mob;
    }
    private static double buildup(LivingEntity mob,StatusEffectType type) {
        var entry=StatusRuntimeService.state(mob).get(type); return entry==null?0:entry.current;
    }
    private static int run(CommandSourceStack source) {
        var level=source.getLevel();
        var player=FakePlayerFactory.get(level,new GameProfile(UUID.fromString("11000000-0000-0000-0000-000000000011"),"StatusFixture"));
        player.setPos(level.getSharedSpawnPos().getX()+.5,100,level.getSharedSpawnPos().getZ()+.5);
        level.getChunk(player.blockPosition()); // Test arena only; production status never loads chunks.
        var attributes=PlayerAttributeState.defaultsState();
        for(var a:Attribute.values()) attributes=attributes.with(a,40,99);
        player.setData(ProgressionAttachments.PLAYER_ATTRIBUTES,attributes);
        var sword=Items.DIAMOND_SWORD.getDefaultInstance();
        sword.set(ProgressionDataComponents.WEAPON_INFUSION,WeaponInfusionState.of(ResourceLocation.parse("maplesadventure:blood")));
        player.setItemSlot(EquipmentSlot.MAINHAND,sword);
        var target=mob(level,player);
        try {
            var direct=player.damageSources().playerAttack(player);
            double amount=WeaponDamagePolicy.context(direct).orElseThrow().statuses().amounts().get(StatusEffectType.BLEED);
            check(amount>30,"Blood infusion ARC produces actual buildup");
            target.hurt(direct,1);
            check(Math.abs(buildup(target,StatusEffectType.BLEED)-amount)<1e-8,"Vanilla successful hit adds exactly once independent of damage/armor");
            if(ModList.get().isLoaded("epicfight")) {
                EpicRegression.statusMotion(player,target);
                StatusRuntimeService.clearAll(target,StatusRuntimeService.ClearReason.ADMIN); target.invulnerableTime=0;
                target.hurt(EpicRegression.defenseSource(player),1);
                check(Math.abs(buildup(target,StatusEffectType.BLEED)-amount)<1e-8,"Epic Fight hit adds exactly once");
            }
            StatusRuntimeService.clearAll(target,StatusRuntimeService.ClearReason.ADMIN);
            target.hurt(player.damageSources().indirectMagic(player,player),1);
            check(StatusRuntimeService.state(target).empty(),"Magic does not inherit held weapon status");
            var arrow=new net.minecraft.world.entity.projectile.Arrow(level,player,Items.ARROW.getDefaultInstance(),sword);
            WeaponDamagePolicy.recordLaunch(arrow,player,sword);
            player.setData(ProgressionAttachments.PLAYER_ATTRIBUTES,PlayerAttributeState.defaultsState());
            var poisonSword=Items.DIAMOND_SWORD.getDefaultInstance();
            poisonSword.set(ProgressionDataComponents.WEAPON_INFUSION,WeaponInfusionState.of(ResourceLocation.parse("maplesadventure:poison")));
            player.setItemSlot(EquipmentSlot.MAINHAND,poisonSword);
            target.invulnerableTime=0; target.hurt(player.damageSources().arrow(arrow,player),1);
            check(Math.abs(buildup(target,StatusEffectType.BLEED)-amount)<1e-8&&buildup(target,StatusEffectType.POISON)==0,
                    "Real Arrow damage uses frozen Blood/ARC40 after weapon and ARC change");
            StatusRuntimeService.clearAll(target,StatusRuntimeService.ClearReason.ADMIN);
            target.invulnerableTime=0; target.hurt(direct,1);
            double poisonAmount=WeaponDamagePolicy.context(direct).orElseThrow().statuses().amounts().get(StatusEffectType.POISON);
            check(Math.abs(buildup(target,StatusEffectType.POISON)-poisonAmount)<1e-8,"Poison weapon direct hit independent of unmet requirement multiplier");
            StatusRuntimeService.clearAll(target,StatusRuntimeService.ClearReason.ADMIN);
            player.setData(ProgressionAttachments.PLAYER_ATTRIBUTES,attributes);
            player.setItemSlot(EquipmentSlot.MAINHAND,sword);
            float hp=target.getHealth();
            StatusBuildupService.proc(target,StatusEffectType.BLEED,StatusSourceContext.admin(StatusEffectType.BLEED));
            check(Math.abs(hp-target.getHealth()-150)<.002,"Bleed 15% bypasses armor and invulnerability frames");
            check(buildup(target,StatusEffectType.BLEED)==0&&!StatusRuntimeService.active(target,StatusEffectType.BLEED),"Bleed discards overflow, no duration/no recursion");
            hp=target.getHealth();
            StatusBuildupService.proc(target,StatusEffectType.FROSTBITE,StatusSourceContext.admin(StatusEffectType.FROSTBITE));
            check(Math.abs(hp-target.getHealth()-100)<.002,"Frost burst precedes vulnerability");
            check(StatusRuntimeService.active(target,StatusEffectType.FROSTBITE)&&StatusRuntimeService.damageTaken(target)==1.20,"Frost active vulnerability");
            check(StatusRuntimeService.frostRegen(target)==.80,"Frost regen profile");
            long until=StatusRuntimeService.state(target).get(StatusEffectType.FROSTBITE).activeEnd;
            StatusBuildupService.proc(target,StatusEffectType.FROSTBITE,StatusSourceContext.admin(StatusEffectType.FROSTBITE));
            check(StatusRuntimeService.state(target).get(StatusEffectType.FROSTBITE).activeEnd==until,"Active Frost cannot refresh/reproc");
            target.invulnerableTime=0; target.hurt(target.damageSources().onFire(),1);
            check(!StatusRuntimeService.active(target,StatusEffectType.FROSTBITE),"Successful fire clears active Frost");
            StatusBuildupService.apply(target,StatusEffectType.FROSTBITE,50,StatusSourceContext.admin(StatusEffectType.FROSTBITE));
            target.invulnerableTime=0; target.hurt(target.damageSources().onFire(),1);
            check(buildup(target,StatusEffectType.FROSTBITE)==50,"Fire does not erase unprocced Frost buildup");
            target.invulnerableTime=0; target.hurt(target.damageSources().genericKill(),Float.MAX_VALUE);
            check(StatusRuntimeService.state(target).empty(),"Normal death clears runtime");
        } finally { target.discard(); }
        var attacker=mob(level,player); var poisoned=mob(level,player);
        var phase=PhaseId.solo(UUID.randomUUID());
        attacker.setData(ModPhaseAttachments.MOB_PHASE,MobPhaseState.prototype(phase));
        poisoned.setData(ModPhaseAttachments.MOB_PHASE,MobPhaseState.prototype(PhaseId.solo(UUID.randomUUID())));
        var poisonSource=new StatusSourceContext(attacker.getUUID(),StatusSourceContext.SourceKind.ADAPTER,
                ResourceLocation.withDefaultNamespace("air"),false,StatusEffectType.POISON);
        StatusBuildupService.proc(poisoned,StatusEffectType.POISON,poisonSource);
        check(StatusRuntimeService.state(poisoned).empty(),"Cross-Phase live source cannot apply buildup");
        poisoned.setData(ModPhaseAttachments.MOB_PHASE,MobPhaseState.prototype(phase));
        StatusBuildupService.proc(poisoned,StatusEffectType.POISON,poisonSource);
        StatusBuildupService.proc(poisoned,StatusEffectType.SCARLET_ROT,StatusSourceContext.admin(StatusEffectType.SCARLET_ROT));
        check(StatusRuntimeService.active(poisoned,StatusEffectType.POISON)&&StatusRuntimeService.active(poisoned,StatusEffectType.SCARLET_ROT),"Poison and Rot coexist independently");
        attacker.setData(ModPhaseAttachments.MOB_PHASE,MobPhaseState.prototype(PhaseId.solo(UUID.randomUUID())));
        float initial=poisoned.getHealth();
        pending.add(new Pending(source.getServer().getTickCount()+45,()->{
            try {
                double expected=2*(StatusDefinitions.get(StatusEffectType.POISON).damage(1000,1)+StatusDefinitions.get(StatusEffectType.SCARLET_ROT).damage(1000,1));
                check(Math.abs(initial-poisoned.getHealth()-expected)<.01,"Real poison+rot DOT continues after source Phase changes; bypasses armor actual="+(initial-poisoned.getHealth())+" expected="+expected+" removed="+poisoned.isRemoved());
                check(buildup(poisoned,StatusEffectType.BLEED)==0,"DOT does not trigger weapon status");
                source.sendSuccess(()->Component.literal("Status regression complete: immediate + delayed checks PASS"),false);
                MaplesAdventure.LOGGER.info("[Status regression] COMPLETE epicfight={} irons={}",ModList.get().isLoaded("epicfight"),ModList.get().isLoaded("irons_spellbooks"));
            } finally { attacker.discard(); poisoned.discard(); }
        }));
        source.sendSuccess(()->Component.literal("Status immediate tests PASS; delayed DOT check in 45 ticks"),false); return 1;
    }
    private static int hud(ServerPlayer player,int percent) {
        // Explicit rendering fixture, not authoritative gameplay or part of the shipped mod.
        var rows=new ArrayList<StatusNetwork.Row>();
        for(var type:StatusEffectType.values()) rows.add(new StatusNetwork.Row(type,StatusNetwork.HUDMode.BUILDUP,percent,100,0,0,1200,0,0));
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,new StatusNetwork.Snapshot(StatusRuntimeService.now(player),100000,rows));
        return 1;
    }
    private static int extended(CommandSourceStack source) {
        var level=source.getLevel();
        var manaPlayer=PlayerDefenseRegression.player(level,"StatusManaFixture");
        try {
            if(ModList.get().isLoaded("irons_spellbooks")) StatusManaRegression.run(manaPlayer);
            else check(!dev.maplesadventure.progression.runtime.PlayerManaService.consumeExact(manaPlayer,40),"No Iron's: no invented mana and no class loading crash");
        } finally { StatusRuntimeService.forget(manaPlayer); PhaseManager.forget(manaPlayer.getUUID()); }
        var player=FakePlayerFactory.get(level,new GameProfile(UUID.fromString("13000000-0000-0000-0000-000000000013"),"Status13Fixture"));
        player.setPos(level.getSharedSpawnPos().getX()+.5,100,level.getSharedSpawnPos().getZ()+.5);
        level.getChunk(player.blockPosition());
        var target=mob(level,player); var victim=mob(level,player);
        try {
            check(StatusResistanceService.resolve(target,StatusEffectType.MADNESS).immune(),"Ordinary mob is Madness immune");
            check(StatusResistanceService.resolve(target,StatusEffectType.DEATH_BLIGHT).immune(),"Ordinary mob is Death Blight immune");
            float hp=target.getHealth();
            check(!StatusBuildupService.proc(target,StatusEffectType.MADNESS,StatusSourceContext.admin(StatusEffectType.MADNESS))&&target.getHealth()==hp,"Immune mob cannot proc Madness");
            dev.maplesadventure.progression.defense.EntityDefenseService.assign(target,ResourceLocation.parse("weaponregression:status13"));
            StatusBuildupService.proc(target,StatusEffectType.BLEED,StatusSourceContext.admin(StatusEffectType.BLEED));
            check(StatusResistanceService.resolve(target,StatusEffectType.BLEED).threshold()==241,"First proc uses +21 correction, not percentage");
            StatusRuntimeService.clear(target,StatusEffectType.BLEED,StatusRuntimeService.ClearReason.CURE);
            check(StatusResistanceService.resolve(target,StatusEffectType.BLEED).threshold()==241,"Cure/empty bar preserves correction history");
            StatusRuntimeService.clear(target,StatusEffectType.BLEED,StatusRuntimeService.ClearReason.ADMIN);
            check(StatusResistanceService.resolve(target,StatusEffectType.BLEED).threshold()==220,"Explicit single-status admin reset works after bar removed");
            target.setHealth(1000);
            StatusBuildupService.proc(target,StatusEffectType.FROSTBITE,StatusSourceContext.admin(StatusEffectType.FROSTBITE));
            hp=target.getHealth();
            StatusBuildupService.proc(target,StatusEffectType.BLEED,StatusSourceContext.admin(StatusEffectType.BLEED));
            check(Math.abs(hp-target.getHealth()-150)<.002,"Frost does not amplify status burst damage");
            StatusRuntimeService.clearAll(target,StatusRuntimeService.ClearReason.ADMIN);
            hp=target.getHealth();
            StatusBuildupService.proc(target,StatusEffectType.SLEEP,StatusSourceContext.admin(StatusEffectType.SLEEP));
            check(target.getHealth()==hp&&StatusControlLockService.locked(target),"Deep Sleep locks without HP damage");
            float victimHp=victim.getHealth(); victim.invulnerableTime=0;
            victim.hurt(victim.damageSources().mobAttack(target),10);
            check(victim.getHealth()==victimHp,"Locked mob cannot deal direct attacks");
            target.invulnerableTime=0; target.hurt(target.damageSources().generic(),1);
            check(!StatusControlLockService.locked(target),"Positive external hurt wakes deep sleep");
            StatusBuildupService.proc(target,StatusEffectType.MADNESS,StatusSourceContext.admin(StatusEffectType.MADNESS));
            check(StatusControlLockService.locked(target),"Explicit TarnishedLike mob permits Madness control lock");
            StatusRuntimeService.clearAll(target,StatusRuntimeService.ClearReason.ADMIN);
            target.setNoAi(false); hp=target.getHealth();
            StatusBuildupService.proc(target,StatusEffectType.SLEEP,StatusSourceContext.admin(StatusEffectType.SLEEP));
            check(!target.isNoAi()&&target.getHealth()==hp,"Sleep does not permanently set NoAI");
            StatusRuntimeService.clearAll(target,StatusRuntimeService.ClearReason.ADMIN);
            StatusBuildupService.proc(target,StatusEffectType.DEATH_BLIGHT,StatusSourceContext.admin(StatusEffectType.DEATH_BLIGHT));
            check(!target.isAlive()&&StatusRuntimeService.state(target).empty(),"Eligible Death Blight uses actual death pipeline and clears runtime");
        } finally { target.discard(); victim.discard(); }
        var sleepy=mob(level,player);
        sleepy.setNoAi(false);
        StatusBuildupService.proc(sleepy,StatusEffectType.SLEEP,StatusSourceContext.admin(StatusEffectType.SLEEP));
        check(StatusControlLockService.locked(sleepy),"Default mob stagger starts");
        pending.add(new Pending(source.getServer().getTickCount()+10,()->check(StatusControlLockService.locked(sleepy),"Removing empty buildup bar does not cancel 30-tick stagger")));
        pending.add(new Pending(source.getServer().getTickCount()+35,()->{
            try { check(!StatusControlLockService.locked(sleepy)&&!sleepy.isNoAi(),"Stagger expires and AI flag unchanged"); }
            finally { sleepy.discard(); }
        }));
        var poison=mob(level,player);
        StatusBuildupService.proc(poison,StatusEffectType.POISON,StatusSourceContext.admin(StatusEffectType.POISON));
        var entry=StatusRuntimeService.state(poison).get(StatusEffectType.POISON);
        // Shortened test clock: two genuine pulses including the duration-end boundary.
        entry.activeEnd=entry.activeStart+40;
        float initial=poison.getHealth();
        pending.add(new Pending(source.getServer().getTickCount()+45,()->{
            try {
                check(Math.abs(initial-poison.getHealth()-9)<.01,"Poison includes final duration pulse (2 x 4.5 HP)");
                check(!StatusRuntimeService.active(poison,StatusEffectType.POISON),"DOT expires after final pulse");
                MaplesAdventure.LOGGER.info("[Status regression] EXTENDED COMPLETE epicfight={} irons={}",ModList.get().isLoaded("epicfight"),ModList.get().isLoaded("irons_spellbooks"));
            } finally { poison.discard(); }
        }));
        source.sendSuccess(()->Component.literal("Extended immediate PASS; control and final-pulse checks pending 45 ticks."),false);
        return 1;
    }
    private static int persist(CommandSourceStack source,boolean verify) {
        var level=source.getLevel(); var pos=level.getSharedSpawnPos(); level.getChunk(pos);
        if(verify) {
            pending.add(new Pending(source.getServer().getTickCount()+20,()->{
                var entity=level.getEntity(PERSIST_ID);
                check(entity instanceof LivingEntity,"Persisted probe loaded from real chunk");
                var living=(LivingEntity)entity; var state=StatusRuntimeService.state(living);
                check(StatusRuntimeService.active(living,StatusEffectType.POISON),"Poison duration attachment survives server restart");
                check(state.get(StatusEffectType.POISON).source.sourceKind()==StatusSourceContext.SourceKind.ADMIN,"Persisted source context");
                check(living.getExistingData(ModPhaseAttachments.MOB_PHASE).isPresent(),"Status persistence preserves Phase identity");
                MaplesAdventure.LOGGER.info("[Status regression] PERSISTENCE COMPLETE uuid={} state={}",PERSIST_ID,state.serializeNBT(null));
                entity.discard();
            }));
        } else {
            if(level.getEntity(PERSIST_ID)!=null) throw new IllegalStateException("Probe already exists; verify it first");
            var mob=Objects.requireNonNull(EntityType.ZOMBIE.create(level)); mob.setUUID(PERSIST_ID);
            mob.setPersistenceRequired(); mob.setNoAi(true); mob.setNoGravity(true); mob.setPos(pos.getX()+.5,120,pos.getZ()+.5);
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000); mob.setHealth(10000);
            mob.setData(ModPhaseAttachments.MOB_PHASE,MobPhaseState.prototype(PhaseId.solo(PERSIST_ID)));
            level.addFreshEntity(mob);
            StatusBuildupService.proc(mob,StatusEffectType.POISON,StatusSourceContext.admin(StatusEffectType.POISON));
            source.sendSuccess(()->Component.literal("Persistent status probe created; save-all flush then stop/restart and checkpersist."),false);
        }
        return 1;
    }
    private StatusRegression() {}
}
