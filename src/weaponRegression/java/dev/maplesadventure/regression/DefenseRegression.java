package dev.maplesadventure.regression;

import com.mojang.authlib.GameProfile;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.multiplayer.phase.*;
import dev.maplesadventure.multiplayer.phase.mob.*;
import dev.maplesadventure.progression.*;
import dev.maplesadventure.progression.defense.*;
import dev.maplesadventure.progression.weapon.*;
import net.minecraft.commands.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import java.util.*;

/** Opt-in dedicated-server fixture; not a production mod or shipped balance data. */
public final class DefenseRegression {
    private static final ResourceLocation STONE = ResourceLocation.parse("weaponregression:stone");
    private static final UUID CHUNK_PROBE = UUID.fromString("10000000-0000-0000-0000-000000000110");
    private static final net.minecraft.core.BlockPos CHUNK_POS = new net.minecraft.core.BlockPos(512,100,512);
    public static void register() {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> e.getDispatcher().register(
                Commands.literal("defenseregression").requires(s -> s.hasPermission(2))
                    .then(Commands.literal("run").executes(c -> run(c.getSource())))
                    .then(Commands.literal("persist").executes(c -> persist(c.getSource())))
                    .then(Commands.literal("chunkseed").executes(c -> chunk(c.getSource(),"seed")))
                    .then(Commands.literal("chunkload").executes(c -> chunk(c.getSource(),"load")))
                    .then(Commands.literal("chunkcheck").executes(c -> chunk(c.getSource(),"check")))
                    .then(Commands.literal("checkpersist").executes(c -> checkPersist(c.getSource())))));
    }
    private static void check(boolean ok, String message) {
        if (!ok) throw new IllegalStateException("[Defense regression] FAIL " + message);
        MaplesAdventure.LOGGER.info("[Defense regression] PASS {}",message);
    }
    private static Zombie zombie(ServerLevel level, ServerPlayer player, double armor) {
        var mob=EntityType.ZOMBIE.create(level); Objects.requireNonNull(mob);
        mob.setNoAi(true); mob.setPersistenceRequired(); mob.setPos(player.position().add(0,0,4));
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); mob.setHealth(1000);
        mob.getAttribute(Attributes.ARMOR).setBaseValue(armor); return mob;
    }
    private static ServerPlayer player(ServerLevel level) {
        var player=FakePlayerFactory.get(level,new GameProfile(UUID.fromString("10000000-0000-0000-0000-000000000010"),"DefenseTester"));
        player.setPos(level.getSharedSpawnPos().getX()+.5,100,level.getSharedSpawnPos().getZ()+.5);
        var state=PlayerAttributeState.defaultsState();
        for(var attribute:Attribute.values()) state=state.with(attribute,40,99);
        player.setData(ProgressionAttachments.PLAYER_ATTRIBUTES,state);
        player.setItemSlot(EquipmentSlot.MAINHAND,Items.DIAMOND_SWORD.getDefaultInstance());
        return player;
    }
    private static WeaponDamageResolution hit(ServerPlayer player,Zombie mob,DamageSource source,float incoming,String label) {
        var context=WeaponDamagePolicy.context(source).orElseThrow();
        final int[] events={0}; final float[] pre={0};
        java.util.function.Consumer<LivingDamageEvent.Pre> observer=e->{if(e.getEntity()==mob){events[0]++; pre[0]=e.getNewDamage();}};
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST,observer);
        float before=mob.getHealth();
        try {
            mob.invulnerableTime=0; mob.hurt(source,incoming);
            var entry=LastWeaponDamageResolution.get(player.getUUID()).orElseThrow();
            var result=entry.resolution();
            check(events[0]==1,label+" one Pre event");
            check(entry.target().equals(mob.getUUID()),label+" target recorded");
            check(Math.abs(before-mob.getHealth()-result.finalDamage())<.002,label+" actual HP matches single resolution");
            check(Math.abs(result.finalDamage()-WeaponCombatResolutionService.resolve(result.originalDamage(),context,mob).finalDamage())<1e-10,
                    label+" no duplicate mitigation");
            MaplesAdventure.LOGGER.info("[Defense regression] {} raw={} nominal={} qualified={} final={} hpLoss={} channels={}",label,
                    result.originalDamage(),result.nominalDamage(),result.qualifiedDamage(),result.finalDamage(),before-mob.getHealth(),result.channelResults());
            return result;
        } finally { NeoForge.EVENT_BUS.unregister(observer); }
    }
    private static int run(CommandSourceStack source) {
        var level=source.getLevel(); var player=player(level); player.setPos(player.position().add(16,0,0));
        level.getChunk(player.blockPosition()); // Only the isolated test location; production defense never loads chunks.
        check(EntityDefenseRegistry.profiles().size()>=2,"fixture profiles loaded");
        var skeleton=EntityType.SKELETON.create(level); var husk=EntityType.HUSK.create(level);
        check(EntityDefenseService.resolve(skeleton).requestedId().equals(STONE),"actual EntityType exact wins over high-priority tag");
        check(EntityDefenseService.resolve(husk).requestedId().equals(ResourceLocation.parse("weaponregression:undead")),"actual EntityType tag priority and file order");
        var commandMob=zombie(level,player,0); level.addFreshEntity(commandMob); player.setYRot(0); player.setXRot(0);
        try {
            var commands=source.getServer().getCommands().getDispatcher();
            var commandSource=source.withEntity(player).withPosition(player.position()).withPermission(2);
            check(commands.execute("ma defense setprofile weaponregression:stone",commandSource)==1,"admin ray-picked setprofile");
            check(commandMob.getExistingData(ProgressionAttachments.ENTITY_DEFENSE_PROFILE).isPresent(),"admin command selected intended probe");
            float health=commandMob.getHealth();
            check(commands.execute("ma defense info",commandSource)==1,"admin info");
            check(commands.execute("ma defense simulate strike 40 20",commandSource)==1 && commandMob.getHealth()==health,"admin simulate does not damage");
            check(commands.execute("ma defense clearprofile",commandSource)==1 && EntityDefenseService.resolve(commandMob).profile()==EntityDefenseProfile.NONE,"admin clearprofile");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException error) { throw new IllegalStateException(error); }
        finally { commandMob.discard(); }
        for(var infusion:WeaponInfusionRegistry.definitions().keySet()) {
            var stack=Items.DIAMOND_SWORD.getDefaultInstance(); stack.set(ProgressionDataComponents.WEAPON_INFUSION,WeaponInfusionState.of(infusion));
            player.setItemSlot(EquipmentSlot.MAINHAND,stack);
            var mob=zombie(level,player,0); level.addFreshEntity(mob);
            try {
                check(EntityDefenseService.resolve(mob).profile()==EntityDefenseProfile.NONE,"zombie unbound default NONE");
                var damage=player.damageSources().playerAttack(player);
                var result=hit(player,mob,damage,20,"neutral "+infusion);
                check(Double.doubleToLongBits(result.finalDamage())==Double.doubleToLongBits(result.originalDamage()*WeaponDamagePolicy.multiplier(damage)),"Round9 bit identity "+infusion);
            } finally { mob.discard(); }
        }
        for(double armor:new double[]{0,12}) {
            var mob=zombie(level,player,armor); level.addFreshEntity(mob);
            EntityDefenseService.assign(mob,STONE);
            try {
                var result=hit(player,mob,player.damageSources().playerAttack(player),20,"stone armor="+armor);
                check(armor==0||result.originalDamage()<20,"Vanilla armor already applied before profile");
                check(mob.getAttributeValue(Attributes.ARMOR)==armor,"profile did not alter armor");
                var saved=mob.saveWithoutId(new CompoundTag());
                var restored=zombie(level,player,armor); restored.load(saved);
                check(EntityDefenseService.resolve(restored).requestedId().equals(STONE),"full entity NBT retains explicit reference");
                restored.discard();
            } finally { mob.discard(); }
        }
        // ServerPlayer exclusion even with a deliberately attached profile (the public assign API rejects it).
        player.setData(ProgressionAttachments.ENTITY_DEFENSE_PROFILE,new EntityDefenseProfileRef(STONE));
        check(EntityDefenseService.resolve(player).profile()==EntityDefenseProfile.NONE,"player target defense excluded");
        player.removeData(ProgressionAttachments.ENTITY_DEFENSE_PROFILE);
        var originalState=PlayerAttributeService.state(player);
        player.setData(ProgressionAttachments.PLAYER_ATTRIBUTES,PlayerAttributeState.defaultsState());
        var unmet=zombie(level,player,0); level.addFreshEntity(unmet); EntityDefenseService.assign(unmet,STONE);
        try {
            var result=hit(player,unmet,player.damageSources().playerAttack(player),20,"unmet requirement");
            check(result.requirementMultiplier()==.35 && result.finalDamage()==result.qualifiedDamage()*.35,"requirement applied last exactly once");
        } finally { unmet.discard(); player.setData(ProgressionAttachments.PLAYER_ATTRIBUTES,originalState); }
        // Frozen launch bundle, live defense, through the real projectile damage source and event.
        var bow=Items.BOW.getDefaultInstance(); bow.set(ProgressionDataComponents.WEAPON_INFUSION,WeaponInfusionState.of(WeaponInfusionRegistry.MAGIC_ID));
        var arrow=new Arrow(level,player,Items.ARROW.getDefaultInstance(),bow);
        WeaponDamagePolicy.recordLaunch(arrow,player,bow);
        var frozen=arrow.getData(ProgressionAttachments.PROJECTILE_REQUIREMENT).bundle();
        bow.set(ProgressionDataComponents.WEAPON_INFUSION,WeaponInfusionState.of(WeaponInfusionRegistry.HEAVY_ID));
        var mob=zombie(level,player,0); level.addFreshEntity(mob);
        try {
            var before=hit(player,mob,player.damageSources().arrow(arrow,player),20,"projectile neutral");
            EntityDefenseService.assign(mob,STONE);
            var after=hit(player,mob,player.damageSources().arrow(arrow,player),20,"projectile live defense");
            check(arrow.getData(ProgressionAttachments.PROJECTILE_REQUIREMENT).bundle().equals(frozen),"arrow frozen after bow reinfusion");
            check(before.finalDamage()!=after.finalDamage(),"target defense change affects already launched attack");
        } finally { mob.discard(); arrow.discard(); }
        // Early phase rejection must prevent this layer being reached at all.
        var hidden=zombie(level,player,0);
        hidden.setData(ModPhaseAttachments.MOB_PHASE,MobPhaseState.prototype(PhaseId.solo(UUID.randomUUID())));
        EntityDefenseService.assign(hidden,STONE); level.addFreshEntity(hidden);
        try {
            LastWeaponDamageResolution.clear(); hidden.hurt(player.damageSources().playerAttack(player),20);
            check(hidden.getHealth()==1000 && LastWeaponDamageResolution.get(player.getUUID()).isEmpty(),"cross-phase hurt rejected before defense");
        } finally { hidden.discard(); }
        for(var damage:List.of(player.damageSources().indirectMagic(player,player),player.damageSources().onFire(),player.damageSources().fall())) {
            check(WeaponDamagePolicy.context(damage).isEmpty(),"spell/fire/fall excluded "+damage);
            var target=zombie(level,player,0); EntityDefenseService.assign(target,STONE); level.addFreshEntity(target);
            try { LastWeaponDamageResolution.clear(); target.hurt(damage,10); check(LastWeaponDamageResolution.get(player.getUUID()).isEmpty(),"non-weapon hurt did not enter defense"); }
            finally { target.discard(); }
        }
        if(ModList.get().isLoaded("epicfight")) {
            var target=zombie(level,player,0); EntityDefenseService.assign(target,STONE); level.addFreshEntity(target);
            try { hit(player,target,EpicRegression.defenseSource(player),20,"EpicFight weapon source"); }
            finally { target.discard(); }
        }
        source.sendSuccess(()->Component.literal("Defense regression PASS; see [Defense regression] server log."),false);
        return 1;
    }
    private static int persist(CommandSourceStack source) {
        var level=source.getLevel(); var player=player(level); level.getChunk(player.blockPosition());
        var mob=zombie(level,player,0); mob.addTag("round10_persistent");
        EntityDefenseService.assign(mob,STONE); level.addFreshEntity(mob);
        source.sendSuccess(()->Component.literal("Persistent defense probe "+mob.getUUID()+" pos="+mob.blockPosition()),false); return 1;
    }
    private static int checkPersist(CommandSourceStack source) {
        var level=source.getLevel(); level.getChunk(level.getSharedSpawnPos());
        int count=0;
        for(var entity:level.getAllEntities()) if(entity instanceof LivingEntity living && living.isAlive() && entity.getTags().contains("round10_persistent")) {
            check(living.getExistingData(ProgressionAttachments.ENTITY_DEFENSE_PROFILE).orElseThrow().profileId().equals(STONE),"restart reference ID retained");
            var resolved=EntityDefenseService.resolve(living);
            source.sendSuccess(()->Component.literal("Persistent probe "+living.getUUID()+" profile="+resolved.requestedId()+" source="+resolved.source()+" channels="+resolved.profile().channels()),false); count++;
        }
        check(count>0,"restart persistent probe found"); return count;
    }
    private static int chunk(CommandSourceStack source,String operation) {
        var level=source.getLevel(); var chunkPos=new net.minecraft.world.level.ChunkPos(CHUNK_POS);
        if(operation.equals("seed")||operation.equals("load")) {
            // Explicit temporary test ticket, removed below; never used by the production defense service.
            level.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.PORTAL,chunkPos,2,CHUNK_POS);
            level.getChunk(CHUNK_POS);
        }
        if(operation.equals("seed")) {
            var mob=zombie(level,player(level),0); mob.setUUID(CHUNK_PROBE); mob.setPos(CHUNK_POS.getCenter());
            mob.setNoGravity(true); EntityDefenseService.assign(mob,STONE);
            check(level.addFreshEntity(mob),"chunk probe created");
            source.getServer().saveEverything(false,true,true);
        }
        var entity=level.getEntity(CHUNK_PROBE);
        if(entity instanceof LivingEntity living) check(EntityDefenseService.resolve(living).requestedId().equals(STONE),"chunk probe retains defense ref");
        source.sendSuccess(()->Component.literal("chunk probe loaded="+(entity!=null)+" operation="+operation),false);
        // Entity IO completes asynchronously after getChunk; keep the load ticket until chunkcheck.
        if(operation.equals("seed")||operation.equals("check"))
            level.getChunkSource().removeRegionTicket(net.minecraft.server.level.TicketType.PORTAL,chunkPos,2,CHUNK_POS);
        return entity==null?0:1;
    }
    private DefenseRegression() {}
}
