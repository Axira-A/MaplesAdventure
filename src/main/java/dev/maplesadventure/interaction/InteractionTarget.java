package dev.maplesadventure.interaction;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

public sealed interface InteractionTarget permits BlockInteractionTarget, EntityInteractionTarget, MessageInteractionTarget,
        SummonSignInteractionTarget, FogGateInteractionTarget {
    Kind kind();

    Vec3 markerPosition(ClientLevel level, float partialTick);

    long stableSortKey();

    String debugDescription(ClientLevel level);

    enum Kind {
        BLOCK,
        ENTITY,
        MESSAGE,
        SUMMON_SIGN,
        FOG_GATE
    }
}
