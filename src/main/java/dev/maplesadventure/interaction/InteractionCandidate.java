package dev.maplesadventure.interaction;

import dev.maplesadventure.interaction.provider.InteractionTargetProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public record InteractionCandidate(
        InteractionTarget target,
        InteractionTargetProvider provider,
        Component displayName,
        InteractionPriority priority,
        Vec3 markerPosition,
        double distance,
        double score,
        boolean lineOfSight
) {
    public boolean sameTarget(InteractionTarget other) {
        return target.equals(other);
    }
}
