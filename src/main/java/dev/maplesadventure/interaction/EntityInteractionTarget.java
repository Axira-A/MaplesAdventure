package dev.maplesadventure.interaction;

import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record EntityInteractionTarget(int entityId, UUID entityUuid) implements InteractionTarget {
    public static EntityInteractionTarget from(Entity entity) {
        return new EntityInteractionTarget(entity.getId(), entity.getUUID());
    }

    public @Nullable Entity resolve(ClientLevel level) {
        Entity entity = level.getEntity(entityId);
        return entity != null && entityUuid.equals(entity.getUUID()) ? entity : null;
    }

    @Override
    public Kind kind() {
        return Kind.ENTITY;
    }

    @Override
    public Vec3 markerPosition(ClientLevel level, float partialTick) {
        Entity entity = resolve(level);
        if (entity == null) {
            return Vec3.ZERO;
        }
        return new Vec3(
                Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getBbHeight() * 0.72D,
                Mth.lerp(partialTick, entity.zo, entity.getZ())
        );
    }

    @Override
    public long stableSortKey() {
        return Integer.toUnsignedLong(entityId);
    }

    @Override
    public String debugDescription(ClientLevel level) {
        Entity entity = resolve(level);
        return entity == null ? "missing_entity#" + entityId : BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()) + "#" + entityId;
    }
}
