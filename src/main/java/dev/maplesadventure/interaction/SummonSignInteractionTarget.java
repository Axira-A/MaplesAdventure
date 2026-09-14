package dev.maplesadventure.interaction;

import dev.maplesadventure.multiplayer.coop.client.SummonSignRenderCache;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

public record SummonSignInteractionTarget(UUID signId) implements InteractionTarget {
    @Override public Kind kind() { return Kind.SUMMON_SIGN; }
    @Override public Vec3 markerPosition(ClientLevel level, float partialTick) {
        return SummonSignRenderCache.get(signId).map(sign -> sign.position().add(0.0D, 0.08D, 0.0D)).orElse(Vec3.ZERO);
    }
    @Override public long stableSortKey() { return signId.getMostSignificantBits() ^ signId.getLeastSignificantBits(); }
    @Override public String debugDescription(ClientLevel level) { return "summon_sign#" + signId; }
}
