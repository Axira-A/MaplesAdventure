package dev.maplesadventure.multiplayer.coop.network;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.multiplayer.coop.SummonSignSummary;
import dev.maplesadventure.multiplayer.coop.SummonSignType;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public final class CoopPayloads {
    public static final int MAX_SYNC_SIGNS = 256;

    public record ToggleSign() implements CustomPacketPayload {
        public static final Type<ToggleSign> TYPE = CoopPayloads.type("toggle_summon_sign");
        public static final StreamCodec<RegistryFriendlyByteBuf, ToggleSign> STREAM_CODEC = unit(ToggleSign::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record RequestSummon(UUID signId) implements CustomPacketPayload {
        public static final Type<RequestSummon> TYPE = CoopPayloads.type("request_summon");
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestSummon> STREAM_CODEC = uuidCodec(RequestSummon::new, RequestSummon::signId);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Leave() implements CustomPacketPayload {
        public static final Type<Leave> TYPE = CoopPayloads.type("leave_coop");
        public static final StreamCodec<RegistryFriendlyByteBuf, Leave> STREAM_CODEC = unit(Leave::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Dismiss() implements CustomPacketPayload {
        public static final Type<Dismiss> TYPE = CoopPayloads.type("dismiss_coop");
        public static final StreamCodec<RegistryFriendlyByteBuf, Dismiss> STREAM_CODEC = unit(Dismiss::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SignSnapshot(List<SummonSignSummary> signs) implements CustomPacketPayload {
        public static final Type<SignSnapshot> TYPE = CoopPayloads.type("summon_sign_snapshot");
        public static final StreamCodec<RegistryFriendlyByteBuf, SignSnapshot> STREAM_CODEC = codec(buffer -> {
            int count = buffer.readVarInt();
            if (count < 0 || count > MAX_SYNC_SIGNS) throw new DecoderException("Summon sign snapshot exceeds bound");
            ArrayList<SummonSignSummary> signs = new ArrayList<>(count);
            for (int index = 0; index < count; index++) signs.add(readSummary(buffer));
            return new SignSnapshot(signs);
        }, (buffer, payload) -> {
            if (payload.signs.size() > MAX_SYNC_SIGNS) throw new IllegalArgumentException("Too many summon signs");
            buffer.writeVarInt(payload.signs.size());
            payload.signs.forEach(sign -> writeSummary(buffer, sign));
        });
        public SignSnapshot { signs = List.copyOf(signs); }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SignUpsert(SummonSignSummary sign) implements CustomPacketPayload {
        public static final Type<SignUpsert> TYPE = CoopPayloads.type("summon_sign_upsert");
        public static final StreamCodec<RegistryFriendlyByteBuf, SignUpsert> STREAM_CODEC = codec(
                buffer -> new SignUpsert(readSummary(buffer)), (buffer, payload) -> writeSummary(buffer, payload.sign));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SignRemove(UUID signId) implements CustomPacketPayload {
        public static final Type<SignRemove> TYPE = CoopPayloads.type("summon_sign_remove");
        public static final StreamCodec<RegistryFriendlyByteBuf, SignRemove> STREAM_CODEC = uuidCodec(SignRemove::new, SignRemove::signId);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static SummonSignSummary readSummary(RegistryFriendlyByteBuf buffer) {
        UUID signId = buffer.readUUID();
        UUID ownerUuid = buffer.readUUID();
        Vec3 position = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)) {
            throw new DecoderException("Invalid summon sign position");
        }
        net.minecraft.core.BlockPos support = buffer.readBlockPos();
        float yaw = buffer.readFloat();
        int type = buffer.readVarInt();
        if (type < 0 || type >= SummonSignType.values().length) throw new DecoderException("Invalid summon sign type");
        return new SummonSignSummary(signId, ownerUuid, position, support, yaw, SummonSignType.values()[type]);
    }

    private static void writeSummary(RegistryFriendlyByteBuf buffer, SummonSignSummary sign) {
        buffer.writeUUID(sign.signId());
        buffer.writeUUID(sign.ownerUuid());
        buffer.writeDouble(sign.position().x);
        buffer.writeDouble(sign.position().y);
        buffer.writeDouble(sign.position().z);
        buffer.writeBlockPos(sign.supportPos());
        buffer.writeFloat(sign.yaw());
        buffer.writeVarInt(sign.type().ordinal());
    }

    private static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> unit(java.util.function.Supplier<T> supplier) {
        return codec(buffer -> supplier.get(), (buffer, payload) -> {});
    }

    private static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> uuidCodec(
            java.util.function.Function<UUID, T> constructor, java.util.function.Function<T, UUID> getter) {
        return codec(buffer -> constructor.apply(buffer.readUUID()), (buffer, value) -> buffer.writeUUID(getter.apply(value)));
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

    private CoopPayloads() {}
}
