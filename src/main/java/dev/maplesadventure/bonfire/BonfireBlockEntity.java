package dev.maplesadventure.bonfire;

import dev.maplesadventure.registry.ModBlocks;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Shared map-author configuration only. Player activation never belongs here. */
public final class BonfireBlockEntity extends BlockEntity {
    private UUID generation = UUID.randomUUID();
    private String displayName = "";
    private final EnumSet<BonfireFeature> features = EnumSet.noneOf(BonfireFeature.class);

    public BonfireBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BONFIRE_ENTITY.get(), pos, state);
    }
    public UUID generation() { return generation; }
    public String displayName() { return displayName; }
    public Set<BonfireFeature> features() { return Set.copyOf(features); }
    public boolean hasFeature(BonfireFeature feature) { return features.contains(feature); }
    public void setDisplayName(String name) {
        displayName = name == null ? "" : name.strip().substring(0, Math.min(64, name.strip().length()));
        setChanged();
    }
    public void setFeature(BonfireFeature feature, boolean enabled) {
        if (enabled) features.add(feature); else features.remove(feature);
        setChanged();
    }
    public BonfireRef ref() {
        if (level == null) throw new IllegalStateException("Bonfire is not placed");
        return new BonfireRef(level.dimension().location(), worldPosition, generation);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putUUID("Generation", generation);
        tag.putString("DisplayName", displayName);
        ListTag list = new ListTag();
        for (BonfireFeature feature : features) list.add(StringTag.valueOf(feature.id()));
        tag.put("Features", list);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        generation = tag.hasUUID("Generation") ? tag.getUUID("Generation") : UUID.randomUUID();
        String name = tag.getString("DisplayName");
        displayName = name.substring(0, Math.min(64, name.length()));
        features.clear();
        for (Tag value : tag.getList("Features", Tag.TAG_STRING)) BonfireFeature.byId(value.getAsString()).ifPresent(features::add);
    }
}
