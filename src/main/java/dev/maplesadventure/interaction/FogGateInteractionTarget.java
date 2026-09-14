package dev.maplesadventure.interaction;

import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public record FogGateInteractionTarget(UUID gateId, BlockPos blockPos) implements InteractionTarget {
    @Override public Kind kind() { return Kind.FOG_GATE; }
    @Override public Vec3 markerPosition(ClientLevel level, float partialTick) {
        return blockPos.getCenter();
    }
    @Override public long stableSortKey() { return gateId.getMostSignificantBits() ^ gateId.getLeastSignificantBits(); }
    @Override public String debugDescription(ClientLevel level) { return "fog_gate:" + gateId + "@" + blockPos.toShortString(); }
    @Override public boolean equals(Object other) {
        return this == other || other instanceof FogGateInteractionTarget target && gateId.equals(target.gateId);
    }
    @Override public int hashCode() { return gateId.hashCode(); }
}
