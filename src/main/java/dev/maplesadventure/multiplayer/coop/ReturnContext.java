package dev.maplesadventure.multiplayer.coop;

import dev.maplesadventure.multiplayer.phase.PhaseId;
import dev.maplesadventure.multiplayer.phase.PhaseRole;
import dev.maplesadventure.multiplayer.phase.PlayerPhaseState;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record ReturnContext(
        PhaseId originalPhase,
        PhaseRole originalRole,
        ResourceKey<Level> dimension,
        Vec3 position,
        float yaw,
        float pitch
) {
    public static ReturnContext capture(ServerPlayer player, PlayerPhaseState state) {
        return new ReturnContext(state.phaseId(), state.role(), player.level().dimension(),
                player.position(), player.getYRot(), player.getXRot());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("OriginalPhase", originalPhase.value());
        tag.putString("OriginalRole", originalRole.name());
        tag.putString("Dimension", dimension.location().toString());
        tag.putDouble("X", position.x);
        tag.putDouble("Y", position.y);
        tag.putDouble("Z", position.z);
        tag.putFloat("Yaw", yaw);
        tag.putFloat("Pitch", pitch);
        return tag;
    }

    public static Optional<ReturnContext> load(CompoundTag tag) {
        try {
            PhaseId phase = new PhaseId(tag.getUUID("OriginalPhase"));
            PhaseRole role = PhaseRole.valueOf(tag.getString("OriginalRole"));
            ResourceLocation dimensionId = ResourceLocation.parse(tag.getString("Dimension"));
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
            Vec3 position = new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
            if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)) {
                return Optional.empty();
            }
            return Optional.of(new ReturnContext(phase, role, dimension, position,
                    tag.getFloat("Yaw"), tag.getFloat("Pitch")));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
