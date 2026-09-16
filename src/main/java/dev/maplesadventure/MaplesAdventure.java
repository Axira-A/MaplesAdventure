package dev.maplesadventure;

import com.mojang.logging.LogUtils;
import dev.maplesadventure.client.MaplesAdventureClient;
import dev.maplesadventure.config.InteractionConfig;
import dev.maplesadventure.config.LostSoulConfig;
import dev.maplesadventure.config.MessageConfig;
import dev.maplesadventure.config.PhaseConfig;
import dev.maplesadventure.config.EchoClientConfig;
import dev.maplesadventure.config.EchoServerConfig;
import dev.maplesadventure.config.EncounterConfig;
import dev.maplesadventure.config.InvasionConfig;
import dev.maplesadventure.config.ProgressionConfig;
import dev.maplesadventure.config.EquipLoadConfig;
import dev.maplesadventure.message.MessageEvents;
import dev.maplesadventure.multiplayer.phase.PhaseCommands;
import dev.maplesadventure.multiplayer.phase.PhaseEvents;
import dev.maplesadventure.multiplayer.phase.mob.ModPhaseAttachments;
import dev.maplesadventure.multiplayer.phase.mob.PhaseMobEvents;
import dev.maplesadventure.multiplayer.phase.loot.PhaseLootEvents;
import dev.maplesadventure.multiplayer.coop.CoopCommands;
import dev.maplesadventure.multiplayer.coop.CoopEvents;
import dev.maplesadventure.multiplayer.echo.EchoCommands;
import dev.maplesadventure.multiplayer.echo.EchoEvents;
import dev.maplesadventure.multiplayer.encounter.EncounterCommands;
import dev.maplesadventure.multiplayer.encounter.EncounterEvents;
import dev.maplesadventure.multiplayer.encounter.boss.BossEntityEvents;
import dev.maplesadventure.multiplayer.encounter.fog.FogGateCommands;
import dev.maplesadventure.multiplayer.invasion.InvasionCommands;
import dev.maplesadventure.multiplayer.invasion.InvasionEvents;
import dev.maplesadventure.network.MessageNetwork;
import dev.maplesadventure.registry.ModEntityTypes;
import dev.maplesadventure.registry.ModBlocks;
import dev.maplesadventure.soul.LostSoulEvents;
import dev.maplesadventure.progression.ProgressionAttachments;
import dev.maplesadventure.progression.ProgressionCommands;
import dev.maplesadventure.progression.ProgressionEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(MaplesAdventure.MOD_ID)
public final class MaplesAdventure {
    public static final String MOD_ID = "maplesadventure";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MaplesAdventure(IEventBus modEventBus, ModContainer modContainer) {
        ModEntityTypes.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModPhaseAttachments.register(modEventBus);
        ProgressionAttachments.register(modEventBus);
        dev.maplesadventure.progression.weapon.ProgressionDataComponents.register(modEventBus);
        CoopEvents.register();
        CoopCommands.register();
        LostSoulEvents.register();
        MessageEvents.register();
        PhaseEvents.register();
        PhaseMobEvents.register();
        PhaseLootEvents.register();
        PhaseCommands.register();
        EchoEvents.register();
        EchoCommands.register();
        BossEntityEvents.register();
        EncounterEvents.register();
        EncounterCommands.register();
        FogGateCommands.register();
        InvasionEvents.register();
        InvasionCommands.register();
        ProgressionEvents.register();
        ProgressionCommands.register();
        dev.maplesadventure.progression.weapon.WeaponRequirementEvents.register();
        dev.maplesadventure.progression.weapon.WeaponRequirementCommands.register();
        dev.maplesadventure.progression.defense.EntityDefenseCommands.register();
        dev.maplesadventure.progression.status.StatusEvents.register();
        dev.maplesadventure.progression.status.StatusCommands.register();
        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                event.enqueueWork(dev.maplesadventure.progression.weapon.WeaponIntegrations::initialize));
        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                event.enqueueWork(dev.maplesadventure.progression.encumbrance.EncumbranceRuntimeService::initialize));
        dev.maplesadventure.integration.bonfires.BonfiresUpgradeAdapter.register();
        modEventBus.addListener(MessageNetwork::register);
        modContainer.registerConfig(ModConfig.Type.SERVER, LostSoulConfig.SPEC, "maplesadventure-server.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, MessageConfig.SPEC, "maplesadventure-messages-server.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, PhaseConfig.SPEC, "maplesadventure-phase-server.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, EchoServerConfig.SPEC, "maplesadventure-echo-server.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, EncounterConfig.SPEC, "maplesadventure-encounters-server.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, InvasionConfig.SPEC, "maplesadventure-invasion-server.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, ProgressionConfig.SPEC, "maplesadventure-progression-server.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, EquipLoadConfig.SPEC, "maplesadventure-equipment-load-server.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, dev.maplesadventure.config.WeaponRequirementConfig.SPEC, "maplesadventure-weapon-requirements-server.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, dev.maplesadventure.config.EnemyDefenseConfig.SPEC, "maplesadventure-enemy-defense-server.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, dev.maplesadventure.config.PlayerDefenseConfig.SPEC, "maplesadventure-player-defense-server.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, dev.maplesadventure.config.StatusConfig.SPEC, "maplesadventure-status-server.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, EchoClientConfig.SPEC, "maplesadventure-echo-client.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, InteractionConfig.SPEC, "maplesadventure-client.toml");
        if (FMLEnvironment.dist == Dist.CLIENT) {
            MaplesAdventureClient.initialize(modEventBus, modContainer);
        }
    }
}
