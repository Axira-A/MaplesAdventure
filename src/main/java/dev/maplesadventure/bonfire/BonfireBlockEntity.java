package dev.maplesadventure.bonfire;

import dev.maplesadventure.registry.ModBlocks;
import java.util.LinkedHashSet;
import net.minecraft.resources.ResourceLocation;
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
    private final Set<ResourceLocation> features = new LinkedHashSet<>();

    public BonfireBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BONFIRE_ENTITY.get(), pos, state);
    }
    public UUID generation() { return generation; }
    public String displayName() { return displayName; }
    public Set<ResourceLocation> features() { return Set.copyOf(features); }
    public boolean hasFeature(BonfireFeature feature) { return hasFeature(BonfireFeatureIds.parse(feature.id())); }
    public boolean hasFeature(ResourceLocation feature) { return features.contains(feature); }
    public void setDisplayName(String name) {
        displayName = name == null ? "" : name.strip().substring(0, Math.min(64, name.strip().length()));
        setChanged();
    }
    public void setFeature(BonfireFeature feature, boolean enabled) {
        setFeature(BonfireFeatureIds.parse(feature.id()), enabled);
    }
    public void setFeature(ResourceLocation feature, boolean enabled) {
        if (feature == null || BonfireFeatureIds.parse(feature.toString()) == null)
            throw new IllegalArgumentException("Invalid feature ID");
        if (enabled && !features.contains(feature) && features.size() >= BonfireFeatureIds.MAX_FEATURES)
            throw new IllegalArgumentException("Too many bonfire features");
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
        tag.putInt("FeatureDataVersion", 2);
        tag.put("Features", BonfireFeatureIds.save(features));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        generation = tag.hasUUID("Generation") ? tag.getUUID("Generation") : UUID.randomUUID();
        String name = tag.getString("DisplayName");
        displayName = name.substring(0, Math.min(64, name.length()));
        features.clear();
        features.addAll(BonfireFeatureIds.load(tag.getList("Features", Tag.TAG_STRING)));
    }
}
