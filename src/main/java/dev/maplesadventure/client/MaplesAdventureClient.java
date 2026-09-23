package dev.maplesadventure.client;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.client.hud.InteractionHud;
import dev.maplesadventure.client.hud.InteractionTargetMarkerRenderer;
import dev.maplesadventure.client.input.AdventureKeyMappings;
import dev.maplesadventure.client.message.MessageWorldRenderer;
import dev.maplesadventure.interaction.InteractionExecutor;
import dev.maplesadventure.interaction.InteractionRegistry;
import dev.maplesadventure.interaction.InteractionTargetManager;
import dev.maplesadventure.interaction.provider.LostSoulInteractionProvider;
import dev.maplesadventure.interaction.provider.MessageInteractionProvider;
import dev.maplesadventure.interaction.provider.SummonSignInteractionProvider;
import dev.maplesadventure.interaction.provider.FogGateInteractionProvider;
import dev.maplesadventure.multiplayer.coop.client.SummonClientEvents;
import dev.maplesadventure.multiplayer.coop.client.SummonSignWorldRenderer;
import dev.maplesadventure.registry.ModEntityTypes;
import dev.maplesadventure.registry.ModBlocks;
import dev.maplesadventure.multiplayer.encounter.fog.client.BossFogGateRenderer;
import dev.maplesadventure.multiplayer.phase.client.PhaseClientEvents;
import dev.maplesadventure.multiplayer.phase.client.PhaseSensoryEvents;
import dev.maplesadventure.multiplayer.echo.client.EchoClientEvents;
import dev.maplesadventure.multiplayer.echo.client.EchoWorldRenderer;
import dev.maplesadventure.multiplayer.hub.client.MultiplayerScreen;
import dev.maplesadventure.multiplayer.invasion.client.InvasionMaterializationCache;
import dev.maplesadventure.multiplayer.invasion.client.InvasionRuneWorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

public final class MaplesAdventureClient {
    private static final InteractionExecutor INTERACTION_EXECUTOR = new InteractionExecutor(
            InteractionRegistry.getInstance(),
            InteractionTargetManager.getInstance()
    );

    public static void initialize(IEventBus modBus, ModContainer modContainer) {
        InteractionRegistry.getInstance().register(new LostSoulInteractionProvider());
        InteractionRegistry.getInstance().register(new MessageInteractionProvider());
        InteractionRegistry.getInstance().register(new SummonSignInteractionProvider());
        InteractionRegistry.getInstance().register(new FogGateInteractionProvider());
        InteractionRegistry.getInstance().register(new dev.maplesadventure.client.bonfire.BonfireInteractionProvider());
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(MaplesAdventureClient::registerKeyMappings);
        modBus.addListener(MaplesAdventureClient::registerGuiLayers);
        modBus.addListener(MaplesAdventureClient::registerEntityRenderers);
        NeoForge.EVENT_BUS.addListener(MaplesAdventureClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(MessageWorldRenderer::render);
        NeoForge.EVENT_BUS.addListener(EchoWorldRenderer::render);
        NeoForge.EVENT_BUS.addListener(SummonSignWorldRenderer::render);
        NeoForge.EVENT_BUS.addListener(InvasionRuneWorldRenderer::render);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) ->
                dev.maplesadventure.client.bonfire.BonfireClient.clear());
        PhaseClientEvents.register();
        PhaseSensoryEvents.register();
        EchoClientEvents.register();
        SummonClientEvents.register();
        dev.maplesadventure.progression.client.UpgradeClient.register();
        dev.maplesadventure.progression.weapon.client.ClientWeaponRequirements.register();
        dev.maplesadventure.progression.status.client.ClientStatusState.register();
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT,dev.maplesadventure.config.StatusClientConfig.SPEC,"maplesadventure-status-client.toml");
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent e)->e.registerReloadListener(new dev.maplesadventure.progression.status.client.StatusHudTextureCache()));
    }

    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                ModEntityTypes.LOST_SOUL.get(),
                dev.maplesadventure.client.soul.LostSoulRenderer::new
        );
        event.registerBlockEntityRenderer(ModBlocks.BOSS_FOG_GATE_ENTITY.get(), BossFogGateRenderer::new);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(AdventureKeyMappings.INTERACT);
        event.register(AdventureKeyMappings.SWITCH_TARGET);
        event.register(AdventureKeyMappings.OPEN_MULTIPLAYER_MENU);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR,ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID,"status_hud"),dev.maplesadventure.progression.status.client.StatusHud::render);
        event.registerAbove(
                VanillaGuiLayers.CROSSHAIR,
                ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, "interaction_marker"),
                new InteractionTargetMarkerRenderer()
        );
        event.registerAbove(
                ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, "interaction_marker"),
                ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, "interaction_prompt"),
                new InteractionHud()
        );
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        InteractionTargetManager manager = InteractionTargetManager.getInstance();
        manager.tick();
        dev.maplesadventure.multiplayer.encounter.fog.client.FogTraversalClientState.tick();
        InvasionMaterializationCache.tick();

        // Vanilla also binds Advancements to L by default. Its key handler may open that
        // screen before ClientTickEvent.Post runs, so the hub intent must be consumed
        // independently of the screen-null guard used by the in-world F/Y actions.
        // KeyConflictContext.IN_GAME still prevents this mapping from firing while chat
        // or an unrelated screen is already open.
        while (AdventureKeyMappings.OPEN_MULTIPLAYER_MENU.consumeClick()) {
            minecraft.setScreen(new MultiplayerScreen());
        }
        if (minecraft.screen == null) {
            while (AdventureKeyMappings.INTERACT.consumeClick()) {
                INTERACTION_EXECUTOR.interactCurrentTarget();
            }
            while (AdventureKeyMappings.SWITCH_TARGET.consumeClick()) {
                manager.cycleTarget();
            }
        }
    }

    public static boolean interactCurrentTarget() {
        return INTERACTION_EXECUTOR.interactCurrentTarget();
    }

    private MaplesAdventureClient() {
    }
}
