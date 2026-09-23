package dev.maplesadventure.regression;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.maplesadventure.progression.ProgressionAttachments;
import dev.maplesadventure.progression.weapon.WeaponDamagePolicy;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import java.util.*;

/** Invoked only on isolated test worlds with an explicitly selected real test player. */
@Mod("weaponregression")
public final class WeaponRegressionMod {
    private final List<Runnable> pending=new ArrayList<>();
    public WeaponRegressionMod() {
        DefenseRegression.register();
        StatusRegression.register();
        PlayerDefenseRegression.register();
        PublicApiRegression.register();
        EquipmentRegression.register();
        NeoForge.EVENT_BUS.addListener(this::commands);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e)->{
            var jobs=List.copyOf(pending); pending.clear(); jobs.forEach(Runnable::run);
        });
    }
    private void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("weaponregression").requires(s->s.hasPermission(2))
            .then(Commands.argument("player",EntityArgument.player())
                .then(Commands.literal("damage").executes(c->damage(c.getSource(),EntityArgument.getPlayer(c,"player"))))
                .then(Commands.literal("useditem").executes(c->EpicRegression.usedItem(c.getSource(),EntityArgument.getPlayer(c,"player"))))
                .then(Commands.literal("probes").executes(c->probes(c.getSource(),EntityArgument.getPlayer(c,"player"))))
                .then(Commands.literal("launch").then(Commands.argument("weapon",StringArgumentType.word())
                    .executes(c->launch(c.getSource(),EntityArgument.getPlayer(c,"player"),StringArgumentType.getString(c,"weapon")))))
                .then(Commands.literal("innate").executes(c->EpicRegression.innate(c.getSource(),EntityArgument.getPlayer(c,"player"))))));
    }
    private int launch(CommandSourceStack source,ServerPlayer player,String name) {
        Item item=switch(name) { case "bow"->Items.BOW;case "crossbow"->Items.CROSSBOW;case "trident"->Items.TRIDENT;default->throw new IllegalArgumentException("bow/crossbow/trident"); };
        var level=player.serverLevel(); Set<UUID> before=new HashSet<>(); level.getAllEntities().forEach(e->before.add(e.getUUID()));
        ItemStack weapon=item.getDefaultInstance(); player.setItemInHand(InteractionHand.MAIN_HAND,weapon);
        player.getInventory().add(new ItemStack(Items.ARROW,3));
        // Exact Vanilla release path after a >=30-tick charge; no direct projectile construction.
        player.startUsingItem(InteractionHand.MAIN_HAND);
        item.releaseUsing(weapon,level,player,item.getUseDuration(weapon,player)-30);
        if(item instanceof CrossbowItem crossbow) crossbow.performShooting(level,player,InteractionHand.MAIN_HAND,weapon,3.15f,0,null);
        player.stopUsingItem();
        pending.add(()->inspect(source,player,name,before));
        return 1;
    }
    private void inspect(CommandSourceStack source,ServerPlayer player,String name,Set<UUID> before) {
        var level=player.serverLevel(); int count=0;
        for(Entity e:level.getAllEntities()) if(!before.contains(e.getUUID()) && e instanceof AbstractArrow arrow) {
            var snapshot=e.getExistingData(ProgressionAttachments.PROJECTILE_REQUIREMENT).orElseThrow(()->new IllegalStateException("Missing launch attachment"));
            arrow.pickup=AbstractArrow.Pickup.DISALLOWED; arrow.setNoGravity(true);
            arrow.setPos(player.getX()+3+count,player.getY()+3,player.getZ()); arrow.setDeltaMovement(0,0,0); arrow.addTag("round7_probe");
            String result=name+" uuid="+e.getUUID()+" snapshot="+snapshot.serializeNBT(level.registryAccess())
                +" actualMultiplier="+WeaponDamagePolicy.multiplier(level.damageSources().arrow(arrow,player));
            source.sendSuccess(()->Component.literal(result),false);
            dev.maplesadventure.MaplesAdventure.LOGGER.info("[Weapon regression] {}",result); count++;
        }
        if(count==0) source.sendFailure(Component.literal("Launch inspection found no projectile"));
    }
    private int probes(CommandSourceStack source,ServerPlayer player) {
        int count=0;
        for(Entity e:player.serverLevel().getAllEntities()) if(e instanceof AbstractArrow arrow && e.getTags().contains("round7_probe")) {
            var n=e.getExistingData(ProgressionAttachments.PROJECTILE_REQUIREMENT).orElseThrow();
            source.sendSuccess(()->Component.literal(e.getUUID()+" "+n.serializeNBT(player.registryAccess())+" currentPolicy="+
                    WeaponDamagePolicy.multiplier(player.damageSources().arrow(arrow,player))+" independentMagic="+
                    WeaponDamagePolicy.multiplier(player.damageSources().indirectMagic(arrow,player))),false); count++;
        }
        return count;
    }
    private int damage(CommandSourceStack source,ServerPlayer player) {
        for(String mode:List.of("weapon","magic","fire","fall")) {
            var target=net.minecraft.world.entity.EntityType.ZOMBIE.create(player.serverLevel());
            if(target==null) throw new IllegalStateException("No test zombie");
            target.setNoAi(true); target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
            target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0); target.setHealth(1000);
            target.setPos(player.position().add(3,0,0)); player.serverLevel().addFreshEntity(target);
            var ds=switch(mode) {
                case "weapon"->player.damageSources().playerAttack(player);
                case "magic"->player.damageSources().indirectMagic(player,player);
                case "fire"->player.damageSources().onFire(); default->player.damageSources().fall();
            };
            try {
                target.hurt(ds,100); float loss=1000-target.getHealth(); double expected=100*WeaponDamagePolicy.multiplier(ds);
                source.sendSuccess(()->Component.literal(mode+" actual="+loss+" expected="+expected+" pass="+(Math.abs(loss-expected)<.01)),false);
            } finally { target.discard(); }
        }
        return 1;
    }
}
