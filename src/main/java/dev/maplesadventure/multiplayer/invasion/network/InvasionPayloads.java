package dev.maplesadventure.multiplayer.invasion.network;

import dev.maplesadventure.MaplesAdventure;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public final class InvasionPayloads {
    public record ToggleSeek() implements CustomPacketPayload {
        public static final Type<ToggleSeek> TYPE = InvasionPayloads.type("invasion_toggle_seek");
        public static final StreamCodec<RegistryFriendlyByteBuf, ToggleSeek> STREAM_CODEC = codec(
                buffer -> new ToggleSeek(), (buffer, value) -> {});
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Materialize(UUID source, Vec3 position, float yaw, int ticks) implements CustomPacketPayload {
        public static final Type<Materialize> TYPE = InvasionPayloads.type("invasion_materialize");
        public static final StreamCodec<RegistryFriendlyByteBuf, Materialize> STREAM_CODEC = codec(
                buffer -> new Materialize(buffer.readUUID(), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                        buffer.readFloat(), Math.min(60, Math.max(1, buffer.readVarInt()))),
                (buffer, value) -> { buffer.writeUUID(value.source); buffer.writeDouble(value.position.x);
                    buffer.writeDouble(value.position.y); buffer.writeDouble(value.position.z);
                    buffer.writeFloat(value.yaw); buffer.writeVarInt(value.ticks); });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Clear(UUID source) implements CustomPacketPayload {
        public static final Type<Clear> TYPE = InvasionPayloads.type("invasion_clear_visuals");
        public static final StreamCodec<RegistryFriendlyByteBuf, Clear> STREAM_CODEC = codec(
                buffer -> new Clear(buffer.readUUID()), (buffer, value) -> buffer.writeUUID(value.source));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    private static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> codec(
            java.util.function.Function<RegistryFriendlyByteBuf, T> decoder,
            java.util.function.BiConsumer<RegistryFriendlyByteBuf, T> encoder) {
        return new StreamCodec<>() {
            @Override public T decode(RegistryFriendlyByteBuf buffer) { return decoder.apply(buffer); }
            @Override public void encode(RegistryFriendlyByteBuf buffer, T value) { encoder.accept(buffer, value); }
        };
    }
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, path));
    }
    private InvasionPayloads() {}
}
