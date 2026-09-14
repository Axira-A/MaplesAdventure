package dev.maplesadventure.progression.weapon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/** The only infusion data persisted on an ItemStack. Balance remains datapack-owned. */
public record WeaponInfusionState(ResourceLocation infusionId, int dataVersion) {
    public static final int CURRENT_VERSION = 1;
    public static final Codec<WeaponInfusionState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("infusion").forGetter(WeaponInfusionState::infusionId),
            Codec.intRange(1, 16).fieldOf("data_version").forGetter(WeaponInfusionState::dataVersion)
    ).apply(instance, WeaponInfusionState::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, WeaponInfusionState> STREAM_CODEC = new StreamCodec<>() {
        @Override public WeaponInfusionState decode(RegistryFriendlyByteBuf buffer) {
            return new WeaponInfusionState(ResourceLocation.parse(buffer.readUtf(256)), buffer.readVarInt());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, WeaponInfusionState value) {
            buffer.writeUtf(value.infusionId().toString(), 256); buffer.writeVarInt(value.dataVersion());
        }
    };
    public WeaponInfusionState {
        if (infusionId == null || infusionId.toString().length() > 256) throw new IllegalArgumentException("Infusion id bounds");
        if (dataVersion < 1 || dataVersion > 16) throw new IllegalArgumentException("Infusion data version bounds");
    }
    public static WeaponInfusionState of(ResourceLocation id) { return new WeaponInfusionState(id, CURRENT_VERSION); }
}
