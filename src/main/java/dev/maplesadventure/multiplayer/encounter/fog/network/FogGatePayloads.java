package dev.maplesadventure.multiplayer.encounter.fog.network;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.multiplayer.encounter.fog.FogGateClientView;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class FogGatePayloads {
    private static final int MAX_GATES = 64;
    private static final int MAX_BLOCKS = 8192;

    public record Snapshot(List<FogGateClientView> gates) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = FogGatePayloads.type("fog_gate_snapshot");
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> STREAM_CODEC = new StreamCodec<>() {
            @Override public Snapshot decode(RegistryFriendlyByteBuf buffer) {
                int count = buffer.readVarInt();
                if (count < 0 || count > MAX_GATES) throw new DecoderException("Too many fog gates");
                ArrayList<FogGateClientView> values = new ArrayList<>(count);
                for (int index = 0; index < count; index++) values.add(readView(buffer));
                return new Snapshot(values);
            }
            @Override public void encode(RegistryFriendlyByteBuf buffer, Snapshot payload) {
                if (payload.gates.size() > MAX_GATES) throw new IllegalArgumentException("Too many fog gates");
                buffer.writeVarInt(payload.gates.size());
                payload.gates.forEach(view -> writeView(buffer, view));
            }
        };
        public Snapshot { gates = List.copyOf(gates); }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record RequestEntry(UUID gateId) implements CustomPacketPayload {
        public static final Type<RequestEntry> TYPE = FogGatePayloads.type("fog_gate_entry");
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestEntry> STREAM_CODEC = new StreamCodec<>() {
            @Override public RequestEntry decode(RegistryFriendlyByteBuf buffer) { return new RequestEntry(buffer.readUUID()); }
            @Override public void encode(RegistryFriendlyByteBuf buffer, RequestEntry payload) { buffer.writeUUID(payload.gateId); }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record StartTraversal(Direction direction, int ticks) implements CustomPacketPayload {
        public static final Type<StartTraversal> TYPE = FogGatePayloads.type("fog_traversal_start");
        public static final StreamCodec<RegistryFriendlyByteBuf, StartTraversal> STREAM_CODEC = new StreamCodec<>() {
            @Override public StartTraversal decode(RegistryFriendlyByteBuf buffer) {
                int ordinal = buffer.readUnsignedByte();
                if (ordinal >= Direction.values().length) throw new DecoderException("Invalid traversal direction");
                return new StartTraversal(Direction.values()[ordinal], buffer.readVarInt());
            }
            @Override public void encode(RegistryFriendlyByteBuf buffer, StartTraversal value) {
                buffer.writeByte(value.direction.ordinal()); buffer.writeVarInt(value.ticks);
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record StopTraversal() implements CustomPacketPayload {
        public static final Type<StopTraversal> TYPE = FogGatePayloads.type("fog_traversal_stop");
        public static final StreamCodec<RegistryFriendlyByteBuf, StopTraversal> STREAM_CODEC = new StreamCodec<>() {
            @Override public StopTraversal decode(RegistryFriendlyByteBuf buffer) { return new StopTraversal(); }
            @Override public void encode(RegistryFriendlyByteBuf buffer, StopTraversal value) {}
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static FogGateClientView readView(RegistryFriendlyByteBuf buffer) {
        UUID gate = buffer.readUUID();
        ResourceLocation encounter = buffer.readResourceLocation();
        int count = buffer.readVarInt();
        if (count <= 0 || count > MAX_BLOCKS) throw new DecoderException("Invalid fog gate block count");
        long[] blocks = new long[count];
        for (int index = 0; index < count; index++) blocks[index] = buffer.readLong();
        int ordinal = buffer.readUnsignedByte();
        Direction[] directions = Direction.values();
        if (ordinal >= directions.length) throw new DecoderException("Invalid fog gate facing");
        return new FogGateClientView(gate, encounter, blocks, directions[ordinal],
                buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
    }
    private static void writeView(RegistryFriendlyByteBuf buffer, FogGateClientView view) {
        buffer.writeUUID(view.gateId()); buffer.writeResourceLocation(view.encounterId());
        long[] blocks = view.blocks();
        if (blocks.length == 0 || blocks.length > MAX_BLOCKS) throw new IllegalArgumentException("Invalid fog blocks");
        buffer.writeVarInt(blocks.length);
        for (long block : blocks) buffer.writeLong(block);
        buffer.writeByte(view.facing().ordinal());
        buffer.writeBoolean(view.render()); buffer.writeBoolean(view.passable()); buffer.writeBoolean(view.interactable());
    }
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, path));
    }
    private FogGatePayloads() {}
}
