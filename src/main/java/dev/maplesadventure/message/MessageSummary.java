package dev.maplesadventure.message;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** Anonymous, render-safe projection of a server record. */
public record MessageSummary(
        UUID messageId,
        Vec3 position,
        BlockPos supportPos,
        float yaw,
        Direction surfaceNormal,
        List<MessagePhrase> phrases,
        List<String> connectors,
        long createdAt,
        int positiveRatings,
        int negativeRatings
) {
    public MessageSummary {
        supportPos = supportPos.immutable();
        phrases = List.copyOf(phrases);
        if (connectors.isEmpty() && phrases.size() > 1) {
            connectors = java.util.Collections.nCopies(phrases.size() - 1, "and");
        }
        if (connectors.size() != Math.max(0, phrases.size() - 1)) {
            throw new IllegalArgumentException("Connector count must be exactly one less than phrase count");
        }
        connectors = List.copyOf(connectors);
        positiveRatings = Math.max(0, positiveRatings);
        negativeRatings = Math.max(0, negativeRatings);
    }

    public MessageSummary(UUID messageId, Vec3 position, BlockPos supportPos, float yaw,
                          Direction surfaceNormal, List<MessagePhrase> phrases, long createdAt,
                          int positiveRatings, int negativeRatings) {
        this(messageId, position, supportPos, yaw, surfaceNormal, phrases, List.of(), createdAt,
                positiveRatings, negativeRatings);
    }
}
