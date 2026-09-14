package dev.maplesadventure.interaction;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.config.InteractionConfig;
import dev.maplesadventure.interaction.provider.EntityInteractionMode;
import dev.maplesadventure.interaction.provider.InteractionTargetProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.ClientHooks;

public final class InteractionExecutor {
    private final InteractionRegistry registry;
    private final InteractionTargetManager manager;

    public InteractionExecutor(InteractionRegistry registry, InteractionTargetManager manager) {
        this.registry = registry;
        this.manager = manager;
    }

    public boolean interactCurrentTarget() {
        InteractionTargetManager.PreparedInteraction prepared = manager.prepareInteraction();
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        MultiPlayerGameMode gameMode = minecraft.gameMode;
        debugValidation(level, prepared);

        InteractionCandidate candidate = prepared.candidate();
        InteractionTargetProvider provider = prepared.provider();
        InteractionTargetValidator.ValidationResult validation = prepared.validation();
        if (!validation.valid() || candidate == null || provider == null
                || player == null || level == null || gameMode == null || !player.isAlive()) {
            manager.setLastValidation(validation.failureReason().name());
            return false;
        }
        if (player.isHandsBusy() || gameMode.isDestroying()) {
            manager.setLastValidation("vanilla_hands_busy");
            return false;
        }

        try {
            boolean consumed;
            if (candidate.target() instanceof BlockInteractionTarget blockTarget) {
                consumed = interactBlock(minecraft, level, player, gameMode, validation, blockTarget);
            } else if (candidate.target() instanceof EntityInteractionTarget entityTarget) {
                consumed = interactEntity(minecraft, level, player, gameMode, provider, validation, entityTarget);
            } else {
                consumed = provider.interactVirtual(level, player, candidate.target());
            }
            manager.setLastValidation(consumed ? "interaction_sent" : "interaction_passed");
            return consumed;
        } catch (RuntimeException exception) {
            registry.reportProviderError(provider, candidate.target(), level, exception);
            manager.setLastValidation("provider_error_at_execution");
        } catch (LinkageError error) {
            registry.reportProviderError(provider, candidate.target(), level, error);
            manager.setLastValidation("provider_linkage_error_at_execution");
        }
        return false;
    }

    private boolean interactBlock(
            Minecraft minecraft,
            ClientLevel level,
            LocalPlayer player,
            MultiPlayerGameMode gameMode,
            InteractionTargetValidator.ValidationResult validation,
            BlockInteractionTarget target
    ) {
        BlockHitResult hit = validation.blockHitResult();
        if (hit == null || !hit.getBlockPos().equals(target.pos())) {
            manager.setLastValidation(InteractionTargetValidator.FailureReason.NO_VALID_HIT_POINT.name());
            return false;
        }

        for (InteractionHand hand : InteractionHand.values()) {
            var inputEvent = ClientHooks.onClickInput(1, minecraft.options.keyUse, hand);
            if (inputEvent.isCanceled()) {
                if (inputEvent.shouldSwingHand()) {
                    player.swing(hand);
                }
                manager.setLastValidation("interaction_input_event_canceled");
                return false;
            }
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isItemEnabled(level.enabledFeatures())) {
                return false;
            }
            int oldCount = stack.getCount();
            InteractionResult result = gameMode.useItemOn(player, hand, hit);
            if (result.consumesAction()) {
                if (result.shouldSwing() && inputEvent.shouldSwingHand()) {
                    player.swing(hand);
                    if (!stack.isEmpty() && (stack.getCount() != oldCount || gameMode.hasInfiniteItems())) {
                        minecraft.gameRenderer.itemInHandRenderer.itemUsed(hand);
                    }
                }
                return true;
            }
            if (result == InteractionResult.FAIL) {
                return false;
            }
            if (useHeldItemIfApplicable(minecraft, level, player, gameMode, hand, stack)) {
                return true;
            }
        }
        return false;
    }

    private boolean interactEntity(
            Minecraft minecraft,
            ClientLevel level,
            LocalPlayer player,
            MultiPlayerGameMode gameMode,
            InteractionTargetProvider provider,
            InteractionTargetValidator.ValidationResult validation,
            EntityInteractionTarget target
    ) {
        Entity entity = target.resolve(level);
        EntityHitResult hit = validation.entityHitResult();
        if (entity == null || hit == null) {
            manager.setLastValidation(InteractionTargetValidator.FailureReason.TARGET_REMOVED.name());
            return false;
        }
        EntityInteractionMode mode = provider.getEntityInteractionMode(level, player, target);

        for (InteractionHand hand : InteractionHand.values()) {
            var inputEvent = ClientHooks.onClickInput(1, minecraft.options.keyUse, hand);
            if (inputEvent.isCanceled()) {
                if (inputEvent.shouldSwingHand()) {
                    player.swing(hand);
                }
                manager.setLastValidation("interaction_input_event_canceled");
                return false;
            }
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isItemEnabled(level.enabledFeatures())) {
                return false;
            }

            InteractionResult result = InteractionResult.PASS;
            if (mode != EntityInteractionMode.GENERAL_ONLY) {
                result = gameMode.interactAt(player, entity, hit, hand);
            }
            if (!result.consumesAction() && mode != EntityInteractionMode.SPECIFIC_ONLY) {
                result = gameMode.interact(player, entity, hand);
            }
            if (result.consumesAction()) {
                if (result.shouldSwing() && inputEvent.shouldSwingHand()) {
                    player.swing(hand);
                }
                return true;
            }
            if (useHeldItemIfApplicable(minecraft, level, player, gameMode, hand, stack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean useHeldItemIfApplicable(
            Minecraft minecraft,
            ClientLevel level,
            LocalPlayer player,
            MultiPlayerGameMode gameMode,
            InteractionHand hand,
            ItemStack stack
    ) {
        if (stack.isEmpty()) {
            return false;
        }
        InteractionResult itemResult = gameMode.useItem(player, hand);
        if (!itemResult.consumesAction()) {
            return false;
        }
        if (itemResult.shouldSwing()) {
            player.swing(hand);
        }
        minecraft.gameRenderer.itemInHandRenderer.itemUsed(hand);
        return true;
    }

    private static void debugValidation(
            ClientLevel level,
            InteractionTargetManager.PreparedInteraction prepared
    ) {
        if (!InteractionConfig.DEBUG.get()) {
            return;
        }
        InteractionCandidate candidate = prepared.candidate();
        InteractionTargetValidator.ValidationResult result = prepared.validation();
        InteractionTarget target = candidate == null ? null : candidate.target();
        BlockHitResult blockHit = result.blockHitResult();
        EntityHitResult entityHit = result.entityHitResult();
        String targetDescription = target == null || level == null ? "none" : target.debugDescription(level);
        String providerDescription = prepared.provider() == null ? "none" : prepared.provider().id().toString();
        String hitLocation = blockHit != null
                ? blockHit.getLocation().toString()
                : entityHit == null ? "none" : entityHit.getLocation().toString();
        String hitFace = blockHit == null ? "none" : blockHit.getDirection().getName();
        String firstHit = describeFirstHit(level, result.raycastFirstHit());
        MaplesAdventure.LOGGER.info(
                "Interaction F validation: target={} type={} provider={} preciseDistance={} maxDistance={} vanillaReach={} "
                        + "shape={} hitLocation={} hitFace={} LOS={} raycastFirstHit={} valid={} failureReason={}",
                targetDescription,
                target == null ? "none" : target.kind(),
                providerDescription,
                result.preciseDistance(),
                result.configuredMaximum(),
                result.vanillaReach(),
                result.shapeSource(),
                hitLocation,
                hitFace,
                result.valid(),
                firstHit,
                result.valid(),
                result.failureReason()
        );
    }

    private static String describeFirstHit(ClientLevel level, BlockHitResult hit) {
        if (hit == null) {
            return "none";
        }
        if (hit.getType() == HitResult.Type.MISS) {
            return "MISS";
        }
        if (level == null || !level.isLoaded(hit.getBlockPos())) {
            return "UNLOADED@" + hit.getBlockPos().toShortString();
        }
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(hit.getBlockPos()).getBlock())
                + "@" + hit.getBlockPos().toShortString();
    }
}
