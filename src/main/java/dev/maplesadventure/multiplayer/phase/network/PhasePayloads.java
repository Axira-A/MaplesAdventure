package dev.maplesadventure.multiplayer.phase.network;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.multiplayer.phase.PhaseId;
import dev.maplesadventure.multiplayer.phase.PhaseRole;
import dev.maplesadventure.multiplayer.phase.PlayerPhaseState;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class PhasePayloads {
    public static final int MAX_TRACKED_PLAYERS = 4096;

    public record Entry(UUID playerId, PlayerPhaseState state) {
        static Entry read(RegistryFriendlyByteBuf buffer) {
            UUID playerId = buffer.readUUID();
            PhaseId phaseId = new PhaseId(buffer.readUUID());
            int roleOrdinal = buffer.readUnsignedByte();
            if (roleOrdinal >= PhaseRole.values().length) throw new DecoderException("Invalid phase role");
            return new Entry(playerId, new PlayerPhaseState(phaseId, PhaseRole.values()[roleOrdinal]));
        }
        void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeUUID(playerId);
            buffer.writeUUID(state.phaseId().value());
            buffer.writeByte(state.role().ordinal());
        }
    }

    public record Snapshot(List<Entry> entries) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = PhasePayloads.type("phase_snapshot");
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> STREAM_CODEC = codec(buffer -> {
            int count = buffer.readVarInt();
            if (count < 0 || count > MAX_TRACKED_PLAYERS) throw new DecoderException("Phase snapshot exceeds bound");
            ArrayList<Entry> entries = new ArrayList<>(count);
            for (int index = 0; index < count; index++) entries.add(Entry.read(buffer));
            return new Snapshot(entries);
        }, (buffer, payload) -> {
            if (payload.entries.size() > MAX_TRACKED_PLAYERS) throw new IllegalArgumentException("Phase snapshot exceeds bound");
            buffer.writeVarInt(payload.entries.size());
            payload.entries.forEach(entry -> entry.write(buffer));
        });
        public Snapshot { entries = List.copyOf(entries); }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Update(Entry entry) implements CustomPacketPayload {
        public static final Type<Update> TYPE = PhasePayloads.type("phase_update");
        public static final StreamCodec<RegistryFriendlyByteBuf, Update> STREAM_CODEC = codec(
                buffer -> new Update(Entry.read(buffer)), (buffer, payload) -> payload.entry.write(buffer));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Remove(UUID playerId) implements CustomPacketPayload {
        public static final Type<Remove> TYPE = PhasePayloads.type("phase_remove");
        public static final StreamCodec<RegistryFriendlyByteBuf, Remove> STREAM_CODEC = codec(
                buffer -> new Remove(buffer.readUUID()), (buffer, payload) -> buffer.writeUUID(payload.playerId));
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

    private PhasePayloads() {}
}
