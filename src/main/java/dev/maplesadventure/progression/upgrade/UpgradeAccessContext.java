package dev.maplesadventure.progression.upgrade;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Serializable-neutral identity of the access point; never exposes a third-party class. */
public record UpgradeAccessContext(
        UpgradeAccessType type,
        ResourceLocation sourceDimension,
        BlockPos sourcePosition,
        ResourceLocation sourceKey,
        UUID sourceId,
        UUID nonce,
        long openedAt
) {}
