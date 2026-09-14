package dev.maplesadventure.interaction;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.interaction.provider.InteractionTargetProvider;
import dev.maplesadventure.interaction.provider.VanillaBlockInteractionProvider;
import dev.maplesadventure.interaction.provider.VanillaEntityInteractionProvider;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Nullable;

public final class InteractionRegistry {
    public static final TagKey<net.minecraft.world.level.block.Block> INTERACTION_BLOCKS = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, "interaction_blocks")
    );
    public static final TagKey<net.minecraft.world.level.block.Block> BLOCK_BLACKLIST = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, "interaction_block_blacklist")
    );
    public static final TagKey<net.minecraft.world.entity.EntityType<?>> INTERACTION_ENTITY_TYPES = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, "interaction_entity_types")
    );
    public static final TagKey<net.minecraft.world.entity.EntityType<?>> ENTITY_TYPE_BLACKLIST = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, "interaction_entity_type_blacklist")
    );

    private static final InteractionRegistry INSTANCE = new InteractionRegistry();

    private final List<InteractionTargetProvider> explicitProviders = new ArrayList<>();
    private final List<InteractionTargetProvider> builtInProviders = List.of(
            new VanillaBlockInteractionProvider(),
            new VanillaEntityInteractionProvider()
    );
    private final InteractionTargetProvider taggedProvider = new TaggedInteractionProvider();
    private final Set<String> loggedProviderErrors = new HashSet<>();

    public static InteractionRegistry getInstance() {
        return INSTANCE;
    }

    public synchronized void register(InteractionTargetProvider provider) {
        explicitProviders.add(provider);
    }

    public boolean isBlacklisted(ClientLevel level, InteractionTarget target) {
        if (target instanceof BlockInteractionTarget blockTarget) {
            return !level.isLoaded(blockTarget.pos()) || level.getBlockState(blockTarget.pos()).is(BLOCK_BLACKLIST);
        }
        if (target instanceof EntityInteractionTarget entityTarget) {
            var entity = entityTarget.resolve(level);
            return entity == null || entity.getType().is(ENTITY_TYPE_BLACKLIST);
        }
        return !(target instanceof MessageInteractionTarget || target instanceof SummonSignInteractionTarget
                || target instanceof FogGateInteractionTarget);
    }

    public @Nullable InteractionTargetProvider findProvider(
            ClientLevel level,
            LocalPlayer player,
            InteractionTarget target
    ) {
        if (isBlacklisted(level, target)) {
            return null;
        }
        InteractionTargetProvider provider = findSupporting(explicitProviders, level, player, target);
        if (provider != null) {
            return provider;
        }
        if (isTagged(level, target) && supportsSafely(taggedProvider, level, player, target)) {
            return taggedProvider;
        }
        return findSupporting(builtInProviders, level, player, target);
    }

    public void reportProviderError(InteractionTargetProvider provider, InteractionTarget target, ClientLevel level, LinkageError error) {
        reportProviderErrorInternal(provider, target, level, error);
    }

    public void reportProviderError(InteractionTargetProvider provider, InteractionTarget target, ClientLevel level, RuntimeException error) {
        reportProviderErrorInternal(provider, target, level, error);
    }

    private @Nullable InteractionTargetProvider findSupporting(
            List<InteractionTargetProvider> providers,
            ClientLevel level,
            LocalPlayer player,
            InteractionTarget target
    ) {
        for (int index = 0; index < providers.size(); index++) {
            InteractionTargetProvider provider = providers.get(index);
            if (supportsSafely(provider, level, player, target)) {
                return provider;
            }
        }
        return null;
    }

    private boolean supportsSafely(
            InteractionTargetProvider provider,
            ClientLevel level,
            LocalPlayer player,
            InteractionTarget target
    ) {
        try {
            return provider.supports(level, player, target);
        } catch (RuntimeException exception) {
            reportProviderError(provider, target, level, exception);
        } catch (LinkageError error) {
            reportProviderError(provider, target, level, error);
        }
        return false;
    }

    private boolean isTagged(ClientLevel level, InteractionTarget target) {
        if (target instanceof BlockInteractionTarget blockTarget) {
            return level.getBlockState(blockTarget.pos()).is(INTERACTION_BLOCKS);
        }
        if (target instanceof EntityInteractionTarget entityTarget) {
            var entity = entityTarget.resolve(level);
            return entity != null && entity.getType().is(INTERACTION_ENTITY_TYPES);
        }
        return false;
    }

    private void reportProviderErrorInternal(
            InteractionTargetProvider provider,
            InteractionTarget target,
            ClientLevel level,
            Throwable error
    ) {
        String key = provider.id() + "|" + target.kind() + "|" + error.getClass().getName();
        synchronized (loggedProviderErrors) {
            if (!loggedProviderErrors.add(key)) {
                return;
            }
        }
        MaplesAdventure.LOGGER.error(
                "Interaction provider {} failed for {}; this provider/target kind will be skipped (logged once)",
                provider.id(),
                target.debugDescription(level),
                error
        );
    }

    private static final class TaggedInteractionProvider implements InteractionTargetProvider {
        private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, "datapack_tag");
        private final VanillaBlockInteractionProvider blockNames = new VanillaBlockInteractionProvider();
        private final VanillaEntityInteractionProvider entityNames = new VanillaEntityInteractionProvider();

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public boolean supports(ClientLevel level, LocalPlayer player, InteractionTarget target) {
            return target instanceof BlockInteractionTarget || target instanceof EntityInteractionTarget;
        }

        @Override
        public boolean isValidTarget(ClientLevel level, LocalPlayer player, InteractionTarget target) {
            if (target instanceof BlockInteractionTarget blockTarget) {
                return level.isLoaded(blockTarget.pos())
                        && level.getBlockState(blockTarget.pos()).getBlock() == blockTarget.expectedBlock()
                        && !level.getBlockState(blockTarget.pos()).isAir();
            }
            if (target instanceof EntityInteractionTarget entityTarget) {
                var entity = entityTarget.resolve(level);
                return entity != null && entity != player && !entity.isRemoved() && entity.isAlive();
            }
            return false;
        }

        @Override
        public Component getDisplayName(ClientLevel level, LocalPlayer player, InteractionTarget target) {
            return target instanceof BlockInteractionTarget
                    ? blockNames.getDisplayName(level, player, target)
                    : entityNames.getDisplayName(level, player, target);
        }
    }

    private InteractionRegistry() {
    }
}
