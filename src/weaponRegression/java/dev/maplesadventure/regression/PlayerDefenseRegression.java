package dev.maplesadventure.regression;

import java.util.*;
import com.mojang.authlib.GameProfile;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.progression.*;
import dev.maplesadventure.progression.defense.*;
import dev.maplesadventure.progression.status.*;
import dev.maplesadventure.progression.weapon.*;
import dev.maplesadventure.multiplayer.phase.*;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.damagesource.*;
import net.neoforged.neoforge.common.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.bus.api.EventPriority;

/** Real server hurt pipeline with synthetic participants, never a substitute for real-client PvP. */
public final class PlayerDefenseRegression {
    public static void register() {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e)->e.getDispatcher().register(
                Commands.literal("playerdefenseregression").requires(s->s.hasPermission(2)).executes(c->run(c.getSource()))));
    }
    static void check(boolean result,String label) {
        if(!result) throw new IllegalStateException("[Player defense] FAIL "+label);
        MaplesAdventure.LOGGER.info("[Player defense] PASS {}",label);
    }
    private static ServerPlayer player(ServerLevel level,String name) {
        var profile=new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)),name);
        // NeoForge FakePlayer is always invulnerable and canHarmPlayer=false. Use the real class
        // with only a no-op network sink; retain normal ServerPlayer.hurt/PvP logic.
        var p=new ServerPlayer(level.getServer(),level,profile,ClientInformation.createDefault());
        p.connection=FakePlayerFactory.get(level,profile).connection;
        p.setPos(level.getSharedSpawnPos().getX()+.5,120,level.getSharedSpawnPos().getZ()+.5);
        // FakePlayer.tick() is deliberately inert, including spawn protection. Test-only setup.
        try { var field=ServerPlayer.class.getDeclaredField("spawnInvulnerableTime"); field.setAccessible(true); field.setInt(p,0); }
        catch(ReflectiveOperationException failure) { throw new IllegalStateException("Fixture spawn protection setup",failure); }
        p.getAbilities().invulnerable=false; p.getAbilities().instabuild=false; p.setInvulnerable(false);
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); p.setHealth(1000); p.setAbsorptionAmount(0);
        p.setData(ProgressionAttachments.PLAYER_ATTRIBUTES,PlayerAttributeState.defaultsState());
        for(var slot:EquipmentSlot.values()) p.setItemSlot(slot,ItemStack.EMPTY);
        return p;
    }
    private static void hit(ServerPlayer target,DamageSource source,float raw,String label) {
        target.setHealth(1000); target.invulnerableTime=0; target.setAbsorptionAmount(0);
        int[] pre={0}; double[] input={0};
        java.util.function.Consumer<LivingDamageEvent.Pre> observer=e->{ if(e.getEntity()==target){pre[0]++; input[0]=e.getNewDamage();} };
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH,observer);
        LastWeaponDamageResolution.clear();
        try {
            target.hurt(source,raw);
            var entry=LastWeaponDamageResolution.get(source.getEntity().getUUID()).orElseThrow(()->new IllegalStateException(label+" missing resolution; HP="+target.getHealth()));
            check(pre[0]==1,label+" exactly one Pre");
            check(entry.profile().equals("PLAYER_BUILD"),label+" PLAYER_BUILD not enemy NONE");
            check(Math.abs(1000-target.getHealth()-entry.finalMaplesDamage())<.001,label+" actual HP equals final once");
            check(entry.resolution().originalDamage()==input[0],label+" real post-armor input");
            MaplesAdventure.LOGGER.info("[Player defense] {} RAW={} pre={} pressure={} typed={} qualified={} req={} frost={} final={} HPLOSS={}",
                    label,raw,input[0],entry.pressure(),entry.typedSource(),entry.resolution().qualifiedDamage(),entry.resolution().requirementMultiplier(),entry.frostMultiplier(),entry.finalMaplesDamage(),1000-target.getHealth());
        } finally { NeoForge.EVENT_BUS.unregister(observer); }
    }
    private static int run(CommandSourceStack command) {
        var level=command.getLevel(); var attacker=player(level,"DefenseAttacker"); var target=player(level,"DefenseTarget");
        PhaseManager.assignSolo(attacker); PhaseManager.join(target,attacker);
        var zombie=Objects.requireNonNull(EntityType.ZOMBIE.create(level)); zombie.setPos(target.position());
        var arrow=new Arrow(EntityType.ARROW,level);
        try {
            attacker.setItemSlot(EquipmentSlot.MAINHAND,Items.WOODEN_SWORD.getDefaultInstance());
            hit(target,attacker.damageSources().playerAttack(attacker),20,"unarmored PvP");
            check(PlayerDefenseService.snapshot(target).channel(WeaponDamageChannel.PHYSICAL).defense()==10,"starter defense10");
            for(var armor:List.of(List.of(Items.IRON_HELMET,Items.IRON_CHESTPLATE,Items.IRON_LEGGINGS,Items.IRON_BOOTS),
                    List.of(Items.DIAMOND_HELMET,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_LEGGINGS,Items.DIAMOND_BOOTS))) {
                int i=0; for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)) {
                    var stack=armor.get(i++).getDefaultInstance(); target.setItemSlot(slot,stack);
                    // Fake players do not tick equipment reconciliation; apply actual stack slot modifiers.
                    stack.forEachModifier(slot,(attribute,modifier)->target.getAttribute(attribute).addOrReplacePermanentModifier(modifier));
                }
                hit(target,attacker.damageSources().playerAttack(attacker),20,armor.get(0).toString());
                check(PlayerDefenseService.snapshot(target).channel(WeaponDamageChannel.PHYSICAL).defense()==10,"armor never becomes build defense");
                for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)) {
                    target.getItemBySlot(slot).forEachModifier(slot,(attribute,modifier)->target.getAttribute(attribute).removeModifier(modifier.id()));
                    target.setItemSlot(slot,ItemStack.EMPTY);
                }
            }
            hit(target,zombie.damageSources().mobAttack(zombie),6,"unarmed mob PHYSICAL");
            zombie.setItemSlot(EquipmentSlot.MAINHAND,Items.MACE.getDefaultInstance());
            hit(target,zombie.damageSources().mobAttack(zombie),6,"mace mob STRIKE");
            check(LastWeaponDamageResolution.get(zombie.getUUID()).orElseThrow().resolution().channelResults().getFirst().channel()==WeaponDamageChannel.STRIKE,"mob profile without player attributes");
            arrow.setOwner(zombie);
            hit(target,zombie.damageSources().arrow(arrow,zombie),6,"generic arrow PIERCE");
            var trident=new ThrownTrident(EntityType.TRIDENT,level); trident.setOwner(zombie);
            hit(target,zombie.damageSources().trident(trident,zombie),6,"generic trident PIERCE");
            var unknown=zombie.damageSources().indirectMagic(zombie,zombie);
            LastWeaponDamageResolution.clear(); target.invulnerableTime=0; target.hurt(unknown,1);
            check(LastWeaponDamageResolution.get(zombie.getUUID()).isEmpty(),"unknown spell MAGIC not guessed");
            // Nonzero FIRE launch snapshot; main hand deliberately physical at impact.
            var fireBundle=new WeaponDamageBundle(Map.of(WeaponDamageChannel.FIRE,new WeaponDamageBundle.ChannelAttack(7,0)),7,1,1);
            var fireHit=new WeaponHitContext(ResourceLocation.parse("test:fire_bow"),fireBundle,new WeaponRequirementResult(true,Map.of(),1,true),true,attacker.getUUID());
            // Requires bootstrapped Minecraft DamageTypes, so run in the dedicated fixture, not plain JUnit.
            var source=attacker.damageSources().playerAttack(attacker);
            var otherSource=attacker.damageSources().playerAttack(attacker);
            CombatHitLifecycle.prepared(target.getUUID(),source,Optional.of(fireHit));
            CombatHitLifecycle.prepared(target.getUUID(),source,Optional.empty());
            check(CombatHitLifecycle.consume(target.getUUID(),otherSource).isEmpty(),"handoff uses source identity");
            check(CombatHitLifecycle.consume(UUID.randomUUID(),source).isEmpty(),"handoff uses target identity");
            check(CombatHitLifecycle.consume(target.getUUID(),source).isEmpty(),"nested untyped hit cannot steal outer context");
            check(CombatHitLifecycle.consume(target.getUUID(),source).orElseThrow()==fireHit,"outer original context retained");
            check(CombatHitLifecycle.consume(target.getUUID(),source).isEmpty(),"handoff consumed once");
            CombatHitLifecycle.prepared(target.getUUID(),source,Optional.of(fireHit)); CombatHitLifecycle.clear();
            check(CombatHitLifecycle.consume(target.getUUID(),source).isEmpty(),"tick cleanup drops abandoned context");
            arrow.setOwner(attacker); arrow.setData(ProgressionAttachments.PROJECTILE_REQUIREMENT,new ProjectileRequirementPenalty(fireHit));
            StatusBuildupService.proc(target,StatusEffectType.FROSTBITE,StatusSourceContext.admin(StatusEffectType.FROSTBITE));
            hit(target,attacker.damageSources().playerAttack(attacker),2,"physical weapon while frozen");
            check(StatusRuntimeService.active(target,StatusEffectType.FROSTBITE),"physical does not clear active frost");
            hit(target,attacker.damageSources().arrow(arrow,attacker),2,"frozen FIRE arrow after hand swap");
            check(!StatusRuntimeService.active(target,StatusEffectType.FROSTBITE),"positive FIRE bundle clears active frost in Post");
            StatusBuildupService.proc(target,StatusEffectType.FROSTBITE,StatusSourceContext.admin(StatusEffectType.FROSTBITE));
            attacker.setItemSlot(EquipmentSlot.MAINHAND,Items.GOLDEN_SWORD.getDefaultInstance());
            java.util.function.Consumer<LivingDamageEvent.Post> swap=e->{if(e.getEntity()==target) attacker.setItemSlot(EquipmentSlot.MAINHAND,Items.WOODEN_SWORD.getDefaultInstance());};
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGH,swap);
            try { hit(target,attacker.damageSources().playerAttack(attacker),2,"direct FIRE weapon with hand changed before Status Post"); }
            finally { NeoForge.EVENT_BUS.unregister(swap); }
            check(!StatusRuntimeService.active(target,StatusEffectType.FROSTBITE),"Post consumes Pre FIRE context, never new hand");
            StatusBuildupService.apply(target,StatusEffectType.FROSTBITE,70,StatusSourceContext.admin(StatusEffectType.FROSTBITE));
            hit(target,attacker.damageSources().arrow(arrow,attacker),2,"FIRE against buildup only");
            check(StatusRuntimeService.state(target).get(StatusEffectType.FROSTBITE).current==70,"fire preserves unprocced buildup");
            StatusRuntimeService.clearAll(target,StatusRuntimeService.ClearReason.ADMIN);
            LastWeaponDamageResolution.clear(); float before=target.getHealth();
            StatusBuildupService.proc(target,StatusEffectType.BLEED,StatusSourceContext.admin(StatusEffectType.BLEED));
            check(Math.abs(before-target.getHealth()-150)<.001,"Bleed bypasses player channel defense");
            check(LastWeaponDamageResolution.get(attacker.getUUID()).isEmpty(),"Status proc creates no typed weapon LastHit");
            PhaseManager.assignSolo(target); LastWeaponDamageResolution.clear(); before=target.getHealth(); target.invulnerableTime=0;
            target.hurt(attacker.damageSources().playerAttack(attacker),20);
            check(before==target.getHealth()&&LastWeaponDamageResolution.get(attacker.getUUID()).isEmpty(),"illegal cross phase has no defense/status side effects");
            PhaseManager.join(target,attacker);
            if(net.neoforged.fml.ModList.get().isLoaded("epicfight")) hit(target,EpicRegression.defenseSource(attacker),10,"Epic Fight actual UsedItem PvP");
            command.sendSuccess(()->Component.literal("Player defense fixture PASS (synthetic players, not real-client PvP)"),false);
            MaplesAdventure.LOGGER.info("[Player defense] COMPLETE");
            return 1;
        } finally {
            StatusRuntimeService.clearAll(target,StatusRuntimeService.ClearReason.ADMIN);
            PhaseManager.forget(attacker.getUUID()); PhaseManager.forget(target.getUUID()); zombie.discard(); arrow.discard();
        }
    }
}
