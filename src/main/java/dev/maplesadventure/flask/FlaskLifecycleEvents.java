package dev.maplesadventure.flask;

import dev.maplesadventure.api.bonfire.*;
import dev.maplesadventure.api.flask.*;
import dev.maplesadventure.progression.ProgressionAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class FlaskLifecycleEvents {
    public static void register() { NeoForge.EVENT_BUS.register(new FlaskLifecycleEvents()); FlaskMenuService.register(); FlaskCommands.register(); }
    @SubscribeEvent public void rest(MaplesBonfireRestCompletedEvent e) { FlaskService.refill(e.context().player(),FlaskRechargeReason.BONFIRE); }
    @SubscribeEvent public void respawnAtBonfire(MaplesBonfireRespawnEvent e) { FlaskService.refill(e.player(),FlaskRechargeReason.BONFIRE); }
    @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent e) { if(e.getEntity() instanceof ServerPlayer p) { tools(p); FlaskService.sync(p); } }
    @SubscribeEvent public void respawn(PlayerEvent.PlayerRespawnEvent e) { if(e.getEntity() instanceof ServerPlayer p) { tools(p); FlaskService.sync(p); } }
    @SubscribeEvent public void clone(PlayerEvent.Clone e) {
        if(e.getEntity() instanceof ServerPlayer p) {
            e.getOriginal().getExistingData(ProgressionAttachments.FLASK).ifPresent(old -> {
                // An explicit copy, never refill: independent of keepInventory and death/non-death clone.
                FlaskState copy=new FlaskState(); copy.deserializeNBT(p.registryAccess(),old.serializeNBT(p.registryAccess()));
                p.setData(ProgressionAttachments.FLASK,copy);
            });
            p.setData(ProgressionAttachments.FLASK_USING,false);
        }
    }
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e) { if(e.getEntity() instanceof ServerPlayer p) { FlaskUseController.cancel(p,FlaskCancelReason.LOGOUT); FlaskMenuService.close(p); } }
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e) { if(e.getEntity() instanceof ServerPlayer p) { FlaskUseController.cancel(p,FlaskCancelReason.DIMENSION_CHANGE); FlaskMenuService.close(p); FlaskService.sync(p); } }
    @SubscribeEvent(priority=EventPriority.HIGHEST,receiveCanceled=true) public void death(LivingDeathEvent e) { if(e.getEntity() instanceof ServerPlayer p) { FlaskUseController.cancel(p,FlaskCancelReason.DEATH); FlaskMenuService.close(p); } }
    @SubscribeEvent public void deathHandles(LivingDropsEvent e) {
        if(e.getEntity() instanceof ServerPlayer) {
            // Respawn already regrants missing reusable handles. Charges live on the player,
            // so leaving obsolete handles as death loot only duplicates the entry items.
            // This does not intercept Q drops, materials, or keepInventory behavior.
            e.getDrops().removeIf(drop -> drop.getItem().is(FlaskItems.CRIMSON.get()) || drop.getItem().is(FlaskItems.ASHEN.get()));
        }
    }
    @SubscribeEvent public void damage(LivingDamageEvent.Post e) { if(e.getNewDamage()>0 && e.getEntity() instanceof ServerPlayer p) FlaskUseController.cancel(p,FlaskCancelReason.DAMAGE); }
    @SubscribeEvent public void tick(ServerTickEvent.Post e) { FlaskUseController.tick(e.getServer()); }
    @SubscribeEvent public void stop(ServerStoppedEvent e) { FlaskUseController.clear(); FlaskMenuService.clear(); }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void attack(AttackEntityEvent e) { if(FlaskApi.isUsingFlask(e.getEntity())) e.setCanceled(true); }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void damageSource(LivingIncomingDamageEvent e) {
        if(e.getSource().getEntity() instanceof ServerPlayer p && e.getSource().getDirectEntity()==p && FlaskApi.isUsingFlask(p)) e.setCanceled(true);
    }
    private void interaction(PlayerInteractEvent e) { if(e instanceof ICancellableEvent c && FlaskApi.isUsingFlask(e.getEntity())) c.setCanceled(true); }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void rightBlock(PlayerInteractEvent.RightClickBlock e) { interaction(e); }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void rightItem(PlayerInteractEvent.RightClickItem e) { interaction(e); }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void leftBlock(PlayerInteractEvent.LeftClickBlock e) { interaction(e); }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void entity(PlayerInteractEvent.EntityInteract e) { interaction(e); }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void specific(PlayerInteractEvent.EntityInteractSpecific e) { interaction(e); }
    private static void tools(ServerPlayer p) {
        FlaskService.state(p);
        // Reusable handles carry no economic value or charge. Recover a missing handle after death/login.
        if(!p.getInventory().contains(new ItemStack(FlaskItems.CRIMSON.get()))) p.getInventory().add(new ItemStack(FlaskItems.CRIMSON.get()));
        if(FlaskManaBridge.available() && !p.getInventory().contains(new ItemStack(FlaskItems.ASHEN.get()))) p.getInventory().add(new ItemStack(FlaskItems.ASHEN.get()));
    }
}
