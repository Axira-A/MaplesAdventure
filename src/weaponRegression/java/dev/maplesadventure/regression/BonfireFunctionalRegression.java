package dev.maplesadventure.regression;

import dev.maplesadventure.bonfire.*;

import com.mojang.authlib.GameProfile;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.api.bonfire.*;
import dev.maplesadventure.multiplayer.encounter.*;
import dev.maplesadventure.multiplayer.phase.*;
import dev.maplesadventure.multiplayer.phase.mob.*;
import dev.maplesadventure.progression.runtime.*;
import dev.maplesadventure.registry.ModBlocks;
import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Opt-in isolated-world fixture, never distributed in the production JAR. Synthetic players are NOT client tests. */
public final class BonfireFunctionalRegression {
    public static void register() {
        NeoForge.EVENT_BUS.addListener((MaplesBonfireRestCompletedEvent event) -> {
            var player = event.context().player();
            MaplesAdventure.LOGGER.info("[Bonfire real-player rest] completed player={} phase={} generation={} lastRested={}",
                    player.getGameProfile().getName(), event.context().phaseId(),
                    EncounterSavedData.get(player.server).generation(PhaseManager.state(player).phaseId()),
                    MaplesBonfireApi.lastRested(player));
            resources(player.createCommandSourceStack(), player, true);
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> e.getDispatcher().register(
                Commands.literal("bonfirefunctional").requires(s -> s.hasPermission(2))
                    .then(Commands.literal("core").executes(c -> core(c.getSource())))
                    .then(Commands.literal("resources").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> resources(c.getSource(), EntityArgument.getPlayer(c, "player"), false))))
                    .then(Commands.literal("check").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> resources(c.getSource(), EntityArgument.getPlayer(c, "player"), true))))));
    }
    private static void check(boolean condition, String label) {
        if (!condition) throw new IllegalStateException("Bonfire regression: " + label);
        MaplesAdventure.LOGGER.info("[Bonfire regression] PASS {}", label);
    }
    private static int core(CommandSourceStack source) {
        var level = source.getLevel();
        var server = source.getServer();
        var data = EncounterSavedData.get(server);
        var position = level.getSharedSpawnPos().atY(Math.min(150, level.getMaxBuildHeight() - 10));
        var phaseA = PhaseId.solo(UUID.randomUUID());
        var phaseB = PhaseId.solo(UUID.randomUUID());
        var commonId = ResourceLocation.parse("weaponregression:bonfire_" + UUID.randomUUID());
        var bossId = ResourceLocation.parse("weaponregression:bonfire_boss_" + UUID.randomUUID());
        var profile = new GameProfile(phaseA.value(), "BonfireCoreTest");
        var player = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
        player.connection = FakePlayerFactory.get(level, profile).connection;
        player.setPos(Vec3.atBottomCenterOf(position.offset(1, 0, 0)));
        PhaseManager.assignSolo(player);
        Map<BlockPos, BlockState> originals = new LinkedHashMap<>();
        List<net.minecraft.world.entity.Entity> entities = new ArrayList<>();
        try {
            // Deliberate fixture construction in its own isolated test world, never part of Reset.
            for (int x = -4; x <= 8; x++) for (int z = -4; z <= 4; z++) for (int y = -1; y <= 3; y++) {
                var p = position.offset(x, y, z);
                level.getChunkAt(p);
                originals.put(p, level.getBlockState(p));
                level.setBlock(p, y == -1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
            }
            level.setBlock(position, ModBlocks.BONFIRE.get().defaultBlockState(), 3);
            var bonfire = Objects.requireNonNull(BonfireStateService.resolve(level, position));
            var progress = BonfireStateService.state(player);
            progress.activate(bonfire.ref()); progress.rest(bonfire.ref(), 73);
            var common = new EncounterDefinition(commonId, level.dimension(), EncounterType.COMMON,
                    position.getCenter(), 16, List.of(
                        point(position.offset(4, 0, 0), EncounterSpawnRole.NORMAL),
                        point(position.offset(6, 0, 0), EncounterSpawnRole.NORMAL)));
            data.putDefinition(common);
            var stateA = data.state(phaseA, common);
            var stateB = data.state(phaseB, common);
            check(EncounterManager.activate(server, data, common, phaseA, stateA), "Phase A common spawn");
            check(EncounterManager.activate(server, data, common, phaseB, stateB), "Phase B common spawn");
            var oldA = new ArrayList<>(stateA.livingEntities()).stream().map(level::getEntity).toList();
            var oldB = new ArrayList<>(stateB.livingEntities()).stream().map(level::getEntity).toList();
            var survivor = (Mob) oldA.get(1);
            survivor.setHealth(3);
            ((Mob) oldA.getFirst()).kill();
            check(stateA.status() == EncounterStatus.ACTIVE && stateA.livingEntities().size() == 1, "Partial death remains ACTIVE");
            var shared = Objects.requireNonNull(EntityType.ZOMBIE.create(level));
            shared.setPos(position.getCenter().add(2, 0, 2)); shared.setHealth(7); level.addFreshEntity(shared); entities.add(shared);
            var prototype = Objects.requireNonNull(EntityType.ZOMBIE.create(level));
            prototype.setData(ModPhaseAttachments.MOB_PHASE, MobPhaseState.prototype(phaseA));
            prototype.setPos(position.getCenter().add(2, 0, -2)); level.addFreshEntity(prototype); entities.add(prototype);
            var boss = new EncounterDefinition(bossId, level.dimension(), EncounterType.BOSS, position.getCenter(), 16,
                    List.of(point(position.offset(8, 0, 0), EncounterSpawnRole.BOSS_PRIMARY)));
            data.putDefinition(boss);
            var bossState = data.state(phaseA, boss);
            bossState.setStatus(EncounterStatus.DEFEATED);
            long beforeA = stateA.generation(), beforeB = stateB.generation();
            var stateBTag = stateB.save().copy();
            var context = new MaplesBonfireContext(player, BonfireApiBridge.detached(bonfire), phaseA.value());
            check(BonfirePhaseResetService.reset(context), "Core phase reset");
            check(stateA.status() == EncounterStatus.READY && stateA.generation() > beforeA, "Phase A READY/new generation");
            check(oldA.stream().allMatch(net.minecraft.world.entity.Entity::isRemoved), "All loaded old A entities discarded");
            check(stateB.generation() == beforeB && stateBTag.equals(stateB.save())
                    && oldB.stream().noneMatch(net.minecraft.world.entity.Entity::isRemoved), "Phase B state and entities unchanged");
            check(!shared.isRemoved() && shared.getHealth() == 7 && !shared.hasData(ModPhaseAttachments.MOB_PHASE),
                    "Shared mob health/identity unchanged");
            check(!prototype.isRemoved(), "Debug prototype outside encounter reset ownership");
            check(bossState.status() == (dev.maplesadventure.config.EncounterConfig.RESPAWN_DEFEATED_BOSSES.get()
                    ? EncounterStatus.READY : EncounterStatus.DEFEATED), "Defeated boss configuration respected");
            check(EncounterManager.activate(server, data, common, phaseA, stateA) && stateA.livingEntities().size() == 2,
                    "Full common group regenerates");
            EncounterResetService.resetEncounter(server, phaseA, commonId, false);
            long individuallyReset = stateA.generation();
            check(BonfirePhaseResetService.reset(context) && stateA.generation() > individuallyReset,
                    "Bonfire generation exceeds individual encounter reset generation");
            player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(35.5);
            player.setHealth(3);
            var restored = DerivedStatRuntimeService.restoreToMaximum(player);
            check(player.getHealth() == player.getMaxHealth() && player.getHealth() == 35.5f, "HP 3 -> runtime max 35.5");
            MaplesAdventure.LOGGER.info("[Bonfire regression] Resource result {}", restored);
            var copy = new PlayerBonfireState();
            copy.deserializeNBT(level.registryAccess(), progress.serializeNBT(level.registryAccess()));
            check(copy.lastRested().equals(progress.lastRested()), "lastRested NBT ref/dimension/yaw roundtrip");
            var rest = progress.lastRested().orElseThrow();
            check(BonfireRespawnResolver.resolve(server, player, rest).isPresent(), "Safe respawn resolved");
            // Block the complete search area temporarily; this must not forget a still-valid bonfire.
            for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) for (int y = -2; y <= 3; y++) {
                if (x == 0 && y == 0 && z == 0) continue;
                var p = position.offset(x, y, z);
                originals.putIfAbsent(p, level.getBlockState(p));
                level.setBlock(p, Blocks.STONE.defaultBlockState(), 3);
            }
            check(BonfireRespawnResolver.resolve(server, player, rest).isEmpty() && progress.lastRested().isPresent(),
                    "No safe position falls back without forgetting");
            level.setBlock(position, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(position, ModBlocks.BONFIRE.get().defaultBlockState(), 3);
            check(BonfireRespawnResolver.resolve(server, player, rest).isEmpty() && progress.lastRested().isEmpty(),
                    "Replaced generation falls back and forgets stale lastRested");
            source.sendSuccess(() -> Component.literal("Bonfire functional core regression PASS (synthetic server participants)"), true);
            return 1;
        } finally {
            EncounterManager.discardLoaded(server, phaseA, commonId);
            EncounterManager.discardLoaded(server, phaseB, commonId);
            data.removeDefinition(commonId); data.removeDefinition(bossId);
            entities.forEach(net.minecraft.world.entity.Entity::discard);
            originals.forEach((p, state) -> level.setBlock(p, state, 3));
            PhaseManager.forget(player.getUUID());
        }
    }
    private static EncounterSpawnPoint point(BlockPos pos, EncounterSpawnRole role) {
        return new EncounterSpawnPoint(UUID.randomUUID(), Vec3.atBottomCenterOf(pos), 0,
                ResourceLocation.parse("minecraft:zombie"), role);
    }
    private static int resources(CommandSourceStack source, ServerPlayer player, boolean verify) {
        if (!verify) player.setHealth(Math.min(3, player.getMaxHealth()));
        check(!verify || Math.abs(player.getHealth() - player.getMaxHealth()) < 0.0001, "HP full");
        source.sendSuccess(() -> Component.literal("HP=" + player.getHealth() + "/" + player.getMaxHealth()), false);
        for (var entry : Map.of("epicfight", "dev.maplesadventure.integration.epicfight.progression.EpicFightStaminaAdapter",
                "irons_spellbooks", "dev.maplesadventure.integration.ironsspellbooks.progression.IronsManaAdapter").entrySet()) {
            if (!ModList.get().isLoaded(entry.getKey())) continue;
            try {
                var adapter = (DerivedStatRuntimeAdapter) Class.forName(entry.getValue()).getConstructor().newInstance();
                if (!verify) adapter.restoreCurrentRatio(player, 0.1);
                var current = adapter.currentValue(player).orElseThrow();
                double maximum = adapter.inspect(player, dev.maplesadventure.progression.PlayerAttributeService.state(player)).runtimeValue();
                String result = adapter.resource() + "=" + current + "/" + maximum;
                source.sendSuccess(() -> Component.literal(result), false);
                MaplesAdventure.LOGGER.info("[Bonfire real-player resources] {} {}", player.getGameProfile().getName(), result);
                check(!verify || Math.abs(current - maximum) < 0.0001, adapter.resource() + " full");
            } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        }
        return 1;
    }
    private BonfireFunctionalRegression() {}
}
