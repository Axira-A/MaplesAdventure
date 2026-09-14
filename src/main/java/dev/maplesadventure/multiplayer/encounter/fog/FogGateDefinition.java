package dev.maplesadventure.multiplayer.encounter.fog;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import dev.maplesadventure.registry.ModBlocks;

public record FogGateDefinition(UUID gateId, ResourceLocation bossEncounterId, ResourceKey<Level> dimension,
                                long[] fogBlocks, Direction facing, Direction insideDirection,
                                BossRoomVolume room) {
    public FogGateDefinition {
        fogBlocks = fogBlocks.clone();
        room = room == null ? BossRoomVolume.empty() : room;
        if (!facing.getAxis().isHorizontal() || !insideDirection.getAxis().isHorizontal())
            throw new IllegalArgumentException("Fog gate directions must be horizontal");
    }
    @Override public long[] fogBlocks() { return fogBlocks.clone(); }
    public boolean contains(BlockPos pos) {
        long packed = pos.asLong();
        for (long block : fogBlocks) if (block == packed) return true;
        return false;
    }
    public BlockPos representative() { return BlockPos.of(fogBlocks[0]); }
    public boolean isIntact(ServerLevel level) {
        if (!level.dimension().equals(dimension)) return false;
        for (long packed : fogBlocks) {
            BlockPos pos = BlockPos.of(packed);
            if (!level.isLoaded(pos) || !level.getBlockState(pos).is(ModBlocks.BOSS_FOG_GATE.get())
                    || level.getBlockState(pos).getValue(BossFogGateBlock.FACING) != facing) return false;
        }
        return true;
    }
    /** Cheap F-time integrity check; room geometry is a map-author responsibility. */
    public boolean isRuntimeValid(ServerLevel level) {
        return isIntact(level);
    }

    public double planeCoordinate() {
        BlockPos position = representative();
        return facing.getAxis() == Direction.Axis.X ? position.getX() + 0.5D : position.getZ() + 0.5D;
    }

    /** Positive is INSIDE, negative is OUTSIDE, zero is on the gate plane. */
    public double signedDistanceToInside(Vec3 position) {
        double coordinate = facing.getAxis() == Direction.Axis.X ? position.x : position.z;
        double raw = coordinate - planeCoordinate();
        int sign = insideDirection.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;
        return raw * sign;
    }

    public GateSide side(Vec3 position) {
        double distance = signedDistanceToInside(position);
        if (distance > 1.0E-4D) return GateSide.INSIDE;
        if (distance < -1.0E-4D) return GateSide.OUTSIDE;
        return GateSide.ON_PLANE;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("GateId", gateId);
        tag.putString("Encounter", bossEncounterId.toString());
        tag.putString("Dimension", dimension.location().toString());
        tag.putLongArray("FogBlocks", fogBlocks);
        tag.putString("Facing", facing.getName());
        tag.putString("InsideDirection", insideDirection.getName());
        if (!room.isEmpty()) tag.put("Room", room.save());
        return tag;
    }

    public static Optional<FogGateDefinition> load(CompoundTag tag) {
        ResourceLocation encounter = ResourceLocation.tryParse(tag.getString("Encounter"));
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
        Direction facing = Direction.byName(tag.getString("Facing"));
        Direction inside = Direction.byName(tag.getString("InsideDirection"));
        long[] blocks = tag.getLongArray("FogBlocks");
        if (!tag.hasUUID("GateId") || encounter == null || dimension == null || facing == null || inside == null
                || blocks.length == 0) return Optional.empty();
        return Optional.of(new FogGateDefinition(tag.getUUID("GateId"), encounter,
                ResourceKey.create(Registries.DIMENSION, dimension), blocks, facing, inside,
                tag.contains("Room") ? BossRoomVolume.load(tag.getCompound("Room")) : BossRoomVolume.empty()));
    }

    public enum GateSide { OUTSIDE, ON_PLANE, INSIDE }
}
