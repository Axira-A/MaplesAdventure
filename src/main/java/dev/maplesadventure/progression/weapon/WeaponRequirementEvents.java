package dev.maplesadventure.progression.weapon;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.server.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.AbstractArrow;
import dev.maplesadventure.progression.defense.*;
public final class WeaponRequirementEvents {
    public static void register() { NeoForge.EVENT_BUS.register(new WeaponRequirementEvents()); }
    @SubscribeEvent public void listeners(AddReloadListenerEvent e) {
        e.addListener(new WeaponRequirementRules()); e.addListener(new WeaponScalingRules()); e.addListener(new WeaponDamageProfileRules());
        e.addListener(new WeaponInfusionRegistry()); e.addListener(new WeaponInfusionEligibilityRules());
        e.addListener(new EntityDefenseRegistry.Profiles()); e.addListener(new EntityDefenseRegistry.Rules());
    }
    @SubscribeEvent public void start(ServerStartedEvent e) { WeaponRequirementService.compile(); EntityDefenseService.compile(); }
    @SubscribeEvent public void stop(ServerStoppedEvent e) {
        WeaponRequirementService.clear(); WeaponInfusionRegistry.reset();
        EntityDefenseService.clear(); EntityDefenseRegistry.clear(); LastWeaponDamageResolution.clear();
    }
    @SubscribeEvent public void join(EntityJoinLevelEvent e) {
        if(e.getLevel().isClientSide() || e.loadedFromDisk()) return;
        // Vanilla AbstractArrow exposes its actual fired-from weapon; trident overrides it with its thrown stack.
        if(e.getEntity() instanceof AbstractArrow arrow && arrow.getOwner() instanceof ServerPlayer player && arrow.getWeaponItem()!=null)
            WeaponDamagePolicy.recordLaunch(arrow,player,arrow.getWeaponItem());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void damage(LivingDamageEvent.Pre e) {
        if(e.getEntity().level().isClientSide()) return;
        // After armor/effects/enchantments, before absorption hearts. The sole weapon-math modification site.
        WeaponDamagePolicy.context(e.getSource()).ifPresent(context -> {
            var resolution = WeaponCombatResolutionService.resolve(e.getNewDamage(), context, e.getEntity());
            float damage = (float)resolution.finalDamage();
            if (damage != e.getNewDamage()) e.setNewDamage(damage);
            LastWeaponDamageResolution.record(context.owner(), e.getEntity().getUUID(),
                    EntityDefenseService.resolve(e.getEntity()).requestedId().toString(), resolution);
        });
    }
}
