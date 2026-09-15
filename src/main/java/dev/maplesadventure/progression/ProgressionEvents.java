package dev.maplesadventure.progression;

import net.minecraft.server.level.ServerPlayer;
import dev.maplesadventure.progression.upgrade.UpgradeAccessService;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import dev.maplesadventure.progression.runtime.DerivedStatRefreshReason;
import dev.maplesadventure.progression.runtime.DerivedStatRuntimeService;
import dev.maplesadventure.progression.encumbrance.EncumbranceRuntimeService;

/** Event-driven persistence verification and local-player synchronization. */
public final class ProgressionEvents {
    public static void register() { NeoForge.EVENT_BUS.register(new ProgressionEvents()); }

    @SubscribeEvent
    public void onReloadListeners(net.neoforged.neoforge.event.AddReloadListenerEvent event) {
        event.addListener(new dev.maplesadventure.progression.spell.SpellSchoolScalingRegistry());
    }

    @SubscribeEvent
    public void onDatapackSync(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) dev.maplesadventure.progression.weapon.WeaponRequirementService.compile();
        if (event.getPlayer() == null) dev.maplesadventure.progression.defense.EntityDefenseService.compile();
        var players = event.getPlayer() == null ? event.getPlayerList().getPlayers() : java.util.List.of(event.getPlayer());
        for (ServerPlayer player : players) {
            dev.maplesadventure.progression.weapon.WeaponRequirementNetwork.sync(player);
            dev.maplesadventure.progression.spell.SpellScalingRuntimeService.refresh(player);
            AttributeSyncService.sync(player);
            if (event.getPlayer() == null) UpgradeAccessService.refreshPolicyView(player);
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerAttributeService.state(player);
            deferredRefresh(player, DerivedStatRefreshReason.LOGIN);
        }
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original) UpgradeAccessService.close(original);
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // copyOnDeath is authoritative. This only covers unusual clone paths where another mod removed the copy.
        if (player.getExistingData(ProgressionAttachments.PLAYER_ATTRIBUTES).isEmpty()) {
            event.getOriginal().getExistingData(ProgressionAttachments.PLAYER_ATTRIBUTES)
                    .ifPresent(state -> player.setData(ProgressionAttachments.PLAYER_ATTRIBUTES, state.cleanCopy()));
        }
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) UpgradeAccessService.close(player);
        if (event.getEntity() instanceof ServerPlayer player)
            deferredRefresh(player, DerivedStatRefreshReason.RESPAWN);
    }

    @SubscribeEvent
    public void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) UpgradeAccessService.close(player);
        if (event.getEntity() instanceof ServerPlayer player)
            deferredRefresh(player, DerivedStatRefreshReason.DIMENSION_CHANGE);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UpgradeAccessService.close(player);
            DerivedStatRuntimeService.captureLogoutCheckpoint(player);
        }
        PlayerAttributeService.forget(event.getEntity().getUUID());
        EncumbranceRuntimeService.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        PlayerAttributeService.clearTransientState();
        DerivedStatRuntimeService.clearTransientState();
        UpgradeAccessService.clear();
        EncumbranceRuntimeService.clear();
    }

    @SubscribeEvent
    public void onTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        EncumbranceRuntimeService.tick(event.getServer());
        if (event.getServer().getTickCount() % 20 == 0) UpgradeAccessService.tick();
    }

    @SubscribeEvent
    public void onEquipmentChanged(net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Run after all equipment attribute modifiers (including Epic Fight WEIGHT) have settled.
        EncumbranceRuntimeService.equipmentChanged(player);
    }

    @SubscribeEvent
    public void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) UpgradeAccessService.close(player);
    }

    private static void deferredRefresh(ServerPlayer player, DerivedStatRefreshReason reason) {
        // Optional mods attach their player runtime during the same lifecycle event. Running on the
        // server queue avoids treating a not-yet-created patch/capability as a permanent adapter failure.
        player.getServer().execute(() -> {
            if (player.connection == null || player.isRemoved()) return;
            DerivedStatRuntimeService.refresh(player, reason);
            EncumbranceRuntimeService.refresh(player);
            AttributeSyncService.sync(player);
        });
    }

    private ProgressionEvents() {}
}
