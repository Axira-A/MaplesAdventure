package dev.maplesadventure.interaction;

import dev.maplesadventure.client.message.MessageRenderCache;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

public record MessageInteractionTarget(UUID messageId) implements InteractionTarget {
    @Override public Kind kind() { return Kind.MESSAGE; }

    @Override
    public Vec3 markerPosition(ClientLevel level, float partialTick) {
        return MessageRenderCache.get(messageId).map(summary -> summary.position().add(0.0D, 0.08D, 0.0D)).orElse(Vec3.ZERO);
    }

    @Override public long stableSortKey() { return messageId.getMostSignificantBits() ^ messageId.getLeastSignificantBits(); }

    @Override
    public String debugDescription(ClientLevel level) {
        return "message#" + messageId;
    }
}
