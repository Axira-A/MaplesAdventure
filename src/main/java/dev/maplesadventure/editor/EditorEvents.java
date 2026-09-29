package dev.maplesadventure.editor;

import dev.maplesadventure.authoring.persistence.AuthoringSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class EditorEvents {
    public static void register(){NeoForge.EVENT_BUS.register(new EditorEvents());}
    @SubscribeEvent public void freeze(ServerAboutToStartEvent e){EditorFoundation.COMPONENTS.freeze();EditorPermissions.freeze();}
    @SubscribeEvent public void load(ServerStartedEvent e){AuthoringSavedData.get(e.getServer());}
    @SubscribeEvent public void stop(ServerStoppedEvent e){EditorSessionService.clear();}
    @SubscribeEvent public void tick(ServerTickEvent.Post e){EditorSessionService.tick(e.getServer());}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)EditorSessionService.forget(p);}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)EditorSessionService.close(p);}
    @SubscribeEvent public void death(LivingDeathEvent e){if(!e.isCanceled()&&e.getEntity() instanceof ServerPlayer p)EditorSessionService.close(p);}
    private void interaction(PlayerInteractEvent e){if(e.getEntity() instanceof ServerPlayer p&&EditorSessionService.active(p)&&e instanceof ICancellableEvent c)c.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void attack(AttackEntityEvent e){if(e.getEntity() instanceof ServerPlayer p&&EditorSessionService.active(p))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void block(PlayerInteractEvent.RightClickBlock e){interaction(e);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void item(PlayerInteractEvent.RightClickItem e){interaction(e);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void entity(PlayerInteractEvent.EntityInteract e){interaction(e);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void specific(PlayerInteractEvent.EntityInteractSpecific e){interaction(e);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void left(PlayerInteractEvent.LeftClickBlock e){interaction(e);}
}
