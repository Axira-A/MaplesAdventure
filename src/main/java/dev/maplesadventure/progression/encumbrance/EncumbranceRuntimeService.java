package dev.maplesadventure.progression.encumbrance;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.progression.Attribute;
import dev.maplesadventure.progression.AttributeSyncService;
import dev.maplesadventure.progression.PlayerAttributeService;
import dev.maplesadventure.progression.ProgressionAttachments;
import dev.maplesadventure.progression.stats.EquipLoadCalculator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.fml.ModList;

/** Event-driven owner of tier, movement modifier and optional combat integration. */
public final class EncumbranceRuntimeService {
    public static final ResourceLocation MOVEMENT_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            MaplesAdventure.MOD_ID, "encumbrance_movement");
    private static final String EPIC_ADAPTER =
            "dev.maplesadventure.integration.epicfight.progression.EpicFightEncumbranceAdapter";
    private static final Map<UUID, EquipLoadRuntimeSnapshot> SNAPSHOTS = new ConcurrentHashMap<>();
    private static final Set<UUID> PENDING_DODGE = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> PENDING_EQUIPMENT = ConcurrentHashMap.newKeySet();
    private static volatile EncumbranceCombatAdapter combatAdapter;
    private static volatile boolean adapterResolved;

    public static void initialize() { adapter(); }
    public static void equipmentChanged(ServerPlayer player) { PENDING_EQUIPMENT.add(player.getUUID()); }

    public static void refresh(ServerPlayer player) {
        EncumbranceCombatAdapter adapter = adapter();
        CombatSkillInitializationState migration = player.getData(ProgressionAttachments.COMBAT_SKILL_INITIALIZATION);
        if (adapter != null) adapter.initializeAndRegister(player, migration);
        double current = adapter == null ? 0.0D : Math.max(0.0D, adapter.currentEquipmentLoad(player));
        boolean available = adapter != null && Double.isFinite(current);
        if (!available) current = 0.0D;
        double maximum = EquipLoadCalculator.maxEquipLoad(PlayerAttributeService.get(player, Attribute.ENDURANCE));
        EncumbrancePolicySnapshot policy = EncumbrancePolicySnapshot.fromServerConfig();
        EquipLoadTier tier = available ? policy.tier(maximum <= 0.0D ? 0.0D : current / maximum)
                : EquipLoadTier.NORMAL;
        EncumbranceProfile profile = policy.profile(tier);
        applyMovement(player, profile);
        boolean deferDodge = adapter != null && adapter.isDodgeAnimationActive(player);
        if (adapter != null) adapter.apply(player,
                new EquipLoadRuntimeSnapshot(current, true, tier, adapter.currentDodgeMode(player), policy),
                profile, !deferDodge);
        if (deferDodge) PENDING_DODGE.add(player.getUUID()); else PENDING_DODGE.remove(player.getUUID());
        DodgeMode currentDodge = adapter == null ? DodgeMode.NONE : adapter.currentDodgeMode(player);
        SNAPSHOTS.put(player.getUUID(), new EquipLoadRuntimeSnapshot(current, available, tier, currentDodge, policy));
    }

    public static void refreshAndSync(ServerPlayer player) {
        refresh(player);
        AttributeSyncService.sync(player);
    }

    public static EquipLoadRuntimeSnapshot snapshot(ServerPlayer player) {
        EquipLoadRuntimeSnapshot cached = SNAPSHOTS.get(player.getUUID());
        if (cached != null) return cached;
        refresh(player);
        return SNAPSHOTS.getOrDefault(player.getUUID(), EquipLoadRuntimeSnapshot.unavailable());
    }

    public static boolean hasSnapshot(ServerPlayer player) { return SNAPSHOTS.containsKey(player.getUUID()); }

    public static boolean canSprint(net.minecraft.world.entity.LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            var cached = SNAPSHOTS.get(player.getUUID());
            return cached == null || cached.policy().profile(cached.tier()).canSprint();
        }
        if (entity instanceof net.minecraft.world.entity.player.Player player && player.isLocalPlayer()) {
            var cached = dev.maplesadventure.progression.client.ClientAttributeState.snapshot().equipLoad();
            return cached.policy().profile(cached.tier()).canSprint();
        }
        return true;
    }

    public static EncumbranceProfile profile(ServerPlayer player) {
        EquipLoadRuntimeSnapshot snapshot = snapshot(player);
        return snapshot.policy().profile(snapshot.tier());
    }

    public static void tick(MinecraftServer server) {
        for (UUID id : Set.copyOf(PENDING_EQUIPMENT)) {
            PENDING_EQUIPMENT.remove(id);
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null && !player.isRemoved()) refreshAndSync(player);
        }
        for (UUID id : Set.copyOf(PENDING_DODGE)) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) { PENDING_DODGE.remove(id); continue; }
            EncumbranceCombatAdapter adapter = adapter();
            if (adapter == null || !adapter.isDodgeAnimationActive(player)) refreshAndSync(player);
        }
        for (var entry : SNAPSHOTS.entrySet()) {
            if (entry.getValue().tier() != EquipLoadTier.OVERLOADED) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null && player.isSprinting()) player.setSprinting(false);
        }
    }

    public static void forget(UUID playerId) {
        SNAPSHOTS.remove(playerId); PENDING_DODGE.remove(playerId); PENDING_EQUIPMENT.remove(playerId);
    }
    public static void clear() { SNAPSHOTS.clear(); PENDING_DODGE.clear(); PENDING_EQUIPMENT.clear(); }

    private static void applyMovement(ServerPlayer player, EncumbranceProfile profile) {
        var instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (instance == null) return;
        instance.removeModifier(MOVEMENT_MODIFIER_ID);
        double amount = profile.movementMultiplier() - 1.0D;
        if (Math.abs(amount) > 0.000_000_1D) instance.addOrReplacePermanentModifier(
                new AttributeModifier(MOVEMENT_MODIFIER_ID, amount,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static EncumbranceCombatAdapter adapter() {
        if (adapterResolved) return combatAdapter;
        synchronized (EncumbranceRuntimeService.class) {
            if (adapterResolved) return combatAdapter;
            adapterResolved = true;
            if (!ModList.get().isLoaded("epicfight")) return null;
            try {
                combatAdapter = (EncumbranceCombatAdapter) Class.forName(EPIC_ADAPTER).getConstructor().newInstance();
                combatAdapter.registerHooks();
                MaplesAdventure.LOGGER.info("Enabled Epic Fight equipment-load runtime");
            } catch (ReflectiveOperationException | LinkageError failure) {
                MaplesAdventure.LOGGER.error("Epic Fight equipment-load runtime is unavailable", failure);
            }
            return combatAdapter;
        }
    }

    private EncumbranceRuntimeService() {}
}
