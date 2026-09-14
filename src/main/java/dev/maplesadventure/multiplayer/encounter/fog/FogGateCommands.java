package dev.maplesadventure.multiplayer.encounter.fog;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.multiplayer.encounter.EncounterDefinition;
import dev.maplesadventure.multiplayer.encounter.EncounterSavedData;
import dev.maplesadventure.multiplayer.encounter.EncounterSpawnRole;
import dev.maplesadventure.multiplayer.encounter.EncounterType;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class FogGateCommands {
    public static void register() { NeoForge.EVENT_BUS.register(new FogGateCommands()); }

    @SubscribeEvent public void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("maplesadventure").then(encounterRoot()).then(fogRoot()));
        event.getDispatcher().register(Commands.literal("ma").then(encounterRoot()).then(fogRoot()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> encounterRoot() {
        return Commands.literal("encounter").requires(source -> source.hasPermission(2))
                .then(Commands.literal("bindgate").then(Commands.argument("id", StringArgumentType.word())
                        .executes(context -> bind(context.getSource(), id(StringArgumentType.getString(context, "id"))))));
    }
    private static LiteralArgumentBuilder<CommandSourceStack> fogRoot() {
        var root = Commands.literal("foggate").requires(source -> source.hasPermission(2));
        root.then(Commands.literal("info").executes(context -> info(context.getSource())));
        root.then(Commands.literal("validate").then(Commands.argument("id", StringArgumentType.word())
                .executes(context -> validate(context.getSource(), id(StringArgumentType.getString(context, "id"))))));
        root.then(Commands.literal("unbind").then(Commands.argument("id", StringArgumentType.word())
                .executes(context -> unbind(context.getSource(), id(StringArgumentType.getString(context, "id"))))));
        root.then(Commands.literal("debug").then(Commands.argument("id", StringArgumentType.word())
                .executes(context -> debug(context.getSource(), id(StringArgumentType.getString(context, "id"))))));
        return root;
    }

    private static int bind(CommandSourceStack source, ResourceLocation encounterId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EncounterSavedData data = EncounterSavedData.get(source.getServer());
        EncounterDefinition encounter = data.definition(encounterId).orElse(null);
        if (encounter == null || encounter.type() != EncounterType.BOSS) return fail(source, "Unknown BOSS encounter: " + encounterId);
        if (encounter.primaryCount() != 1L) return fail(source, "Boss must have exactly one BOSS_PRIMARY before gate binding");
        BlockPos looked = lookedBlock(player);
        if (looked == null) return fail(source, "Look at a boss_fog_gate block");
        FogGateComponentScanner.Result result = FogGateComponentScanner.scan(player.serverLevel(), encounter, looked);
        if (!result.valid()) return fail(source, "Fog gate validation failed: " + result.reason());
        data.putFogGate(result.definition());
        FogGateSyncService.syncPhase(source.getServer(), dev.maplesadventure.multiplayer.phase.PhaseManager.state(player).phaseId());
        source.sendSuccess(() -> Component.literal("Bound fog gate " + result.definition().gateId() + " to " + encounterId
                + " blocks=" + result.definition().fogBlocks().length + " facing=" + result.definition().facing()
                + " inside=" + result.definition().insideDirection()), true);
        return 1;
    }

    private static int validate(CommandSourceStack source, ResourceLocation encounterId) {
        EncounterSavedData data = EncounterSavedData.get(source.getServer());
        EncounterDefinition encounter = data.definition(encounterId).orElse(null);
        FogGateDefinition old = data.fogGate(encounterId).orElse(null);
        if (encounter == null || old == null) return fail(source, "Encounter has no bound fog gate: " + encounterId);
        var level = source.getServer().getLevel(old.dimension());
        if (level == null || !level.isLoaded(old.representative())) return fail(source, "Fog gate chunk is not loaded; validation does not force-load chunks");
        FogGateComponentScanner.Result result = FogGateComponentScanner.scan(level, encounter, old.representative());
        if (!result.valid()) return fail(source, "Fog gate validation failed: " + result.reason());
        FogGateDefinition refreshed = new FogGateDefinition(old.gateId(), encounterId, old.dimension(),
                result.definition().fogBlocks(), result.definition().facing(), result.definition().insideDirection(), result.definition().room());
        data.putFogGate(refreshed);
        var primary = encounter.spawnPoints().stream()
                .filter(point -> point.role() == EncounterSpawnRole.BOSS_PRIMARY).findFirst().orElseThrow();
        source.sendSuccess(() -> Component.literal("Fog gate valid\nBlocks: " + refreshed.fogBlocks().length
                + "\nFacing: " + refreshed.facing() + "\nInside: " + refreshed.insideDirection()
                + "\nBoss: " + primary.entityType() + "\nEncounter: " + encounterId), true);
        return 1;
    }

    private static int info(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BlockPos looked = lookedBlock(player);
        FogGateDefinition gate = looked == null ? null : EncounterSavedData.get(source.getServer())
                .fogGate(player.level().dimension(), looked).orElse(null);
        if (gate == null) return fail(source, "Look at a bound fog gate");
        source.sendSuccess(() -> Component.literal("gate=" + gate.gateId() + " encounter=" + gate.bossEncounterId()
                + " facing=" + gate.facing() + " inside=" + gate.insideDirection() + " blocks=" + gate.fogBlocks().length
                + " player=" + gate.side(player.getBoundingBox().getCenter())
                + " traversal=" + FogTraversalManager.debugState(player.getUUID(), gate.gateId())), false);
        return 1;
    }

    private static int unbind(CommandSourceStack source, ResourceLocation encounterId) {
        if (EncounterSavedData.get(source.getServer()).removeFogGate(encounterId).isEmpty())
            return fail(source, "Encounter has no fog gate: " + encounterId);
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) FogGateSyncService.syncNow(player);
        source.sendSuccess(() -> Component.literal("Unbound fog gate from " + encounterId), true);
        return 1;
    }

    private static int debug(CommandSourceStack source, ResourceLocation encounterId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        FogGateDefinition gate = EncounterSavedData.get(source.getServer()).fogGate(encounterId).orElse(null);
        if (gate == null || !player.level().dimension().equals(gate.dimension())) return fail(source, "No fog gate in this dimension");
        for (long packed : gate.fogBlocks()) {
            BlockPos pos = BlockPos.of(packed);
            player.serverLevel().sendParticles(player, ParticleTypes.END_ROD, true, pos.getX()+.5, pos.getY()+.5, pos.getZ()+.5, 1, 0,0,0,0);
        }
        EncounterDefinition encounter = EncounterSavedData.get(source.getServer()).definition(encounterId).orElse(null);
        var primary = encounter == null ? null : encounter.spawnPoints().stream()
                .filter(point -> point.role() == EncounterSpawnRole.BOSS_PRIMARY).findFirst().orElse(null);
        if (primary != null) player.serverLevel().sendParticles(player, ParticleTypes.SOUL_FIRE_FLAME, true,
                primary.position().x, primary.position().y + 0.25D, primary.position().z, 8, 0.15D, 0.15D, 0.15D, 0.0D);
        source.sendSuccess(() -> Component.literal("Fog debug: gate=END_ROD blocks=" + gate.fogBlocks().length
                + " plane=" + gate.facing().getAxis() + "@" + gate.planeCoordinate()
                + " facing=" + gate.facing() + " inside=" + gate.insideDirection()
                + " outside=" + gate.insideDirection().getOpposite() + " primary="
                + (primary == null ? "MISSING" : primary.position())
                + " playerSide=" + gate.side(player.getBoundingBox().getCenter())
                + " traversal=" + FogTraversalManager.debugState(player.getUUID(), gate.gateId())), false);
        return 1;
    }

    private static BlockPos lookedBlock(ServerPlayer player) {
        BlockHitResult hit = player.level().clip(new ClipContext(player.getEyePosition(),
                player.getEyePosition().add(player.getLookAngle().scale(8.0D)), ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
    }
    private static ResourceLocation id(String text) {
        ResourceLocation id = ResourceLocation.tryParse(text.contains(":") ? text : MaplesAdventure.MOD_ID + ":" + text);
        if (id == null) throw new IllegalArgumentException("Invalid id: " + text);
        return id;
    }
    private static int fail(CommandSourceStack source, String message) { source.sendFailure(Component.literal(message)); return 0; }
    private FogGateCommands() {}
}
