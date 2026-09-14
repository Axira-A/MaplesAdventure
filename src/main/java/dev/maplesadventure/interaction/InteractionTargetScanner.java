package dev.maplesadventure.interaction;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.client.message.MessageRenderCache;
import dev.maplesadventure.config.InteractionConfig;
import dev.maplesadventure.interaction.provider.InteractionTargetProvider;
import dev.maplesadventure.multiplayer.coop.client.SummonSignRenderCache;
import dev.maplesadventure.multiplayer.phase.PhaseRelations;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class InteractionTargetScanner {
    private static final int MAX_CANDIDATES = 96;
    private static final Comparator<InteractionCandidate> STABLE_ORDER = Comparator
            .comparingDouble(InteractionCandidate::score).reversed()
            .thenComparingDouble(InteractionCandidate::distance)
            .thenComparingInt(candidate -> candidate.target().kind().ordinal())
            .thenComparingLong(candidate -> candidate.target().stableSortKey());

    private final InteractionRegistry registry;
    private final InteractionTargetValidator validator;
    private final ArrayList<InteractionCandidate> candidates = new ArrayList<>(32);
    private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
    private int nextFogDebugTick;

    public InteractionTargetScanner(InteractionRegistry registry, InteractionTargetValidator validator) {
        this.registry = registry;
        this.validator = validator;
    }

    public List<InteractionCandidate> scan(ClientLevel level, LocalPlayer player) {
        candidates.clear();
        Vec3 eye = player.getEyePosition();
        Vec3 playerFacing = horizontalFacing(player);
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraFacing = new Vec3(camera.getLookVector());

        scanBlocks(level, player, eye, playerFacing, cameraFacing);
        scanEntities(level, player, eye, playerFacing, cameraFacing);
        scanMessages(level, player, eye, playerFacing, cameraFacing);
        scanSummonSigns(level, player, eye, playerFacing, cameraFacing);
        scanFogGates(level, player, eye, playerFacing, cameraFacing);
        candidates.sort(STABLE_ORDER);
        if (candidates.size() > MAX_CANDIDATES) {
            candidates.subList(MAX_CANDIDATES, candidates.size()).clear();
        }
        return candidates;
    }

    private void scanFogGates(ClientLevel level, LocalPlayer player, Vec3 eye, Vec3 playerFacing, Vec3 cameraFacing) {
        double range = InteractionDistanceCalculator.blockBroadPhaseRange(player);
        AABB bounds = player.getBoundingBox().inflate(range);
        var nearbyGates = dev.maplesadventure.multiplayer.encounter.fog.client.FogGateClientCache.inBounds(bounds);
        if (nearbyGates.isEmpty()) debugFog(player, "no cached fog gate intersects broad phase");
        for (var gate : nearbyGates) {
            BlockPos nearest = null;
            double best = Double.MAX_VALUE;
            for (long packed : gate.blocks()) {
                BlockPos pos = BlockPos.of(packed);
                if (!bounds.contains(pos.getCenter())) continue;
                double distance = player.distanceToSqr(pos.getCenter());
                if (distance < best) { best = distance; nearest = pos; }
            }
            if (nearest == null) {
                debugFog(player, "cached gate " + gate.gateId() + " had no block center in broad phase");
                continue;
            }
            FogGateInteractionTarget target = new FogGateInteractionTarget(gate.gateId(), nearest);
            InteractionTargetProvider provider = registry.findProvider(level, player, target);
            if (provider != null) addCandidate(level, player, target, provider, eye, playerFacing, cameraFacing);
            else debugFog(player, "no provider for " + target.debugDescription(level));
        }
    }

    private void scanSummonSigns(ClientLevel level, LocalPlayer player, Vec3 eye, Vec3 playerFacing, Vec3 cameraFacing) {
        double range = InteractionDistanceCalculator.blockBroadPhaseRange(player);
        AABB bounds = player.getBoundingBox().inflate(range);
        var signs = SummonSignRenderCache.inBounds(bounds);
        for (int index = 0; index < signs.size(); index++) {
            SummonSignInteractionTarget target = new SummonSignInteractionTarget(signs.get(index).signId());
            InteractionTargetProvider provider = registry.findProvider(level, player, target);
            if (provider != null) addCandidate(level, player, target, provider, eye, playerFacing, cameraFacing);
        }
    }

    private void scanMessages(
            ClientLevel level,
            LocalPlayer player,
            Vec3 eye,
            Vec3 playerFacing,
            Vec3 cameraFacing
    ) {
        double range = InteractionDistanceCalculator.blockBroadPhaseRange(player);
        AABB bounds = player.getBoundingBox().inflate(range);
        var messages = MessageRenderCache.inBounds(bounds);
        for (int index = 0; index < messages.size(); index++) {
            MessageInteractionTarget target = new MessageInteractionTarget(messages.get(index).messageId());
            InteractionTargetProvider provider = registry.findProvider(level, player, target);
            if (provider != null) addCandidate(level, player, target, provider, eye, playerFacing, cameraFacing);
        }
    }

    public void clear() {
        candidates.clear();
    }

    private void scanBlocks(
            ClientLevel level,
            LocalPlayer player,
            Vec3 eye,
            Vec3 playerFacing,
            Vec3 cameraFacing
    ) {
        double broadPhaseRange = InteractionDistanceCalculator.blockBroadPhaseRange(player);
        AABB searchBounds = player.getBoundingBox().inflate(broadPhaseRange);
        int minX = (int) Math.floor(searchBounds.minX);
        int maxX = (int) Math.floor(searchBounds.maxX);
        int minY = (int) Math.floor(searchBounds.minY);
        int maxY = (int) Math.floor(searchBounds.maxY);
        int minZ = (int) Math.floor(searchBounds.minZ);
        int maxZ = (int) Math.floor(searchBounds.maxZ);

        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    mutablePos.set(x, y, z);
                    if (!level.isLoaded(mutablePos)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(mutablePos);
                    if (state.isAir()) {
                        continue;
                    }
                    BlockInteractionTarget target = new BlockInteractionTarget(mutablePos, state.getBlock());
                    InteractionTargetProvider provider = registry.findProvider(level, player, target);
                    if (provider != null) {
                        addCandidate(level, player, target, provider, eye, playerFacing, cameraFacing);
                    }
                }
            }
        }
    }

    private void scanEntities(
            ClientLevel level,
            LocalPlayer player,
            Vec3 eye,
            Vec3 playerFacing,
            Vec3 cameraFacing
    ) {
        double broadPhaseRange = InteractionDistanceCalculator.entityBroadPhaseRange(player);
        AABB searchBox = player.getBoundingBox().inflate(broadPhaseRange);
        List<Entity> entities = level.getEntities(player, searchBox, entity -> entity.isAlive()
                && !entity.isRemoved() && PhaseRelations.canInteract(player, entity));
        for (int index = 0; index < entities.size(); index++) {
            EntityInteractionTarget target = EntityInteractionTarget.from(entities.get(index));
            InteractionTargetProvider provider = registry.findProvider(level, player, target);
            if (provider != null) {
                addCandidate(level, player, target, provider, eye, playerFacing, cameraFacing);
            }
        }
    }

    private void addCandidate(
            ClientLevel level,
            LocalPlayer player,
            InteractionTarget target,
            InteractionTargetProvider provider,
            Vec3 eye,
            Vec3 playerFacing,
            Vec3 cameraFacing
    ) {
        try {
            InteractionTargetValidator.ValidationResult validation = validator.validate(
                    level,
                    player,
                    target,
                    provider,
                    InteractionTargetValidator.ValidationMode.ACQUIRE
            );
            if (!validation.valid()) {
                if (target instanceof FogGateInteractionTarget) {
                    debugFog(player, target.debugDescription(level) + " rejected: " + validation.failureReason()
                            + " distance=" + validation.preciseDistance());
                }
                return;
            }
            Vec3 marker = provider.getMarkerPosition(level, target, 1.0F);
            Vec3 direction = marker.subtract(eye);
            if (direction.lengthSqr() < 1.0E-8D) {
                return;
            }
            direction = direction.normalize();
            InteractionPriority priority = provider.getPriority(level, player, target);
            double score = InteractionScorer.calculate(
                    validation.preciseDistance(),
                    Math.max(validation.effectiveMaximum(), 1.0E-6D),
                    playerFacing.dot(direction),
                    cameraFacing.dot(direction),
                    priority
            );
            candidates.add(new InteractionCandidate(
                    target,
                    provider,
                    provider.getDisplayName(level, player, target),
                    priority,
                    marker,
                    validation.preciseDistance(),
                    score,
                    true
            ));
            if (target instanceof FogGateInteractionTarget) {
                debugFog(player, target.debugDescription(level) + " accepted distance="
                        + validation.preciseDistance());
            }
        } catch (RuntimeException exception) {
            registry.reportProviderError(provider, target, level, exception);
        } catch (LinkageError error) {
            registry.reportProviderError(provider, target, level, error);
        }
    }

    private void debugFog(LocalPlayer player, String message) {
        if (!InteractionConfig.DEBUG.get() || player.tickCount < nextFogDebugTick) return;
        nextFogDebugTick = player.tickCount + 20;
        MaplesAdventure.LOGGER.info("[Interaction/FogGate] {}", message);
    }

    private static Vec3 horizontalFacing(LocalPlayer player) {
        double radians = Math.toRadians(player.yBodyRot);
        return new Vec3(-Math.sin(radians), 0.0D, Math.cos(radians));
    }
}
