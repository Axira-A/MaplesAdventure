package dev.maplesadventure.progression.network;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.progression.*;
import dev.maplesadventure.progression.upgrade.*;
import io.netty.handler.codec.DecoderException;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Bounded access-neutral level-up protocol. Clients send intent, never price or final state. */
public final class UpgradePayloads {
    public enum Mode { OFFER, OPEN, DIRECT_OPEN, RESULT }
    public record Open(UUID nonce) implements CustomPacketPayload {
        public static final Type<Open> TYPE = UpgradePayloads.type("upgrade_open");
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = codec(
                b -> new Open(b.readUUID()), (b, p) -> b.writeUUID(p.nonce));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Close(UUID nonce) implements CustomPacketPayload {
        public static final Type<Close> TYPE = UpgradePayloads.type("upgrade_close");
        public static final StreamCodec<RegistryFriendlyByteBuf, Close> CODEC = codec(
                b -> new Close(b.readUUID()), (b, p) -> b.writeUUID(p.nonce));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Closed(UUID nonce, BatchUpgradeStatus reason) implements CustomPacketPayload {
        public static final Type<Closed> TYPE = UpgradePayloads.type("upgrade_closed");
        public static final StreamCodec<RegistryFriendlyByteBuf, Closed> CODEC = codec(
                b -> new Closed(b.readUUID(), b.readEnum(BatchUpgradeStatus.class)),
                (b, p) -> { b.writeUUID(p.nonce); b.writeEnum(p.reason); });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Submit(UUID nonce, long revision, Map<Attribute, Integer> deltas) implements CustomPacketPayload {
        public Submit { deltas = Map.copyOf(deltas); }
        public static final Type<Submit> TYPE = UpgradePayloads.type("upgrade_submit");
        public static final StreamCodec<RegistryFriendlyByteBuf, Submit> CODEC = codec(b -> {
            UUID nonce = b.readUUID();
            long revision = b.readVarLong();
            Map<Attribute, Integer> values = new EnumMap<>(Attribute.class);
            for (Attribute attribute : Attribute.values()) {
                int delta = b.readVarInt();
                if (delta < 0 || delta > 94) throw new DecoderException("Invalid attribute delta");
                values.put(attribute, delta);
            }
            return new Submit(nonce, revision, values);
        }, (b, p) -> {
            b.writeUUID(p.nonce); b.writeVarLong(p.revision);
            for (Attribute a : Attribute.values()) b.writeVarInt(p.deltas.getOrDefault(a, 0));
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record View(Mode mode, BatchUpgradeStatus status, UpgradeView view) implements CustomPacketPayload {
        public static final Type<View> TYPE = UpgradePayloads.type("upgrade_view");
        public static final StreamCodec<RegistryFriendlyByteBuf, View> CODEC = codec(b -> {
            Mode mode = b.readEnum(Mode.class);
            BatchUpgradeStatus status = b.readEnum(BatchUpgradeStatus.class);
            UpgradeAccessType accessType = b.readEnum(UpgradeAccessType.class);
            ResourceLocation dimension = b.readResourceLocation();
            BlockPos pos = b.readBlockPos();
            ResourceLocation sourceKey = b.readResourceLocation();
            UUID sourceId = b.readUUID(), nonce = b.readUUID();
            long openedAt = b.readVarLong(), revision = b.readVarLong();
            var snapshot = AttributePayloads.Snapshot.STREAM_CODEC.decode(b).snapshot();
            int xp = b.readVarInt(), cap = b.readVarInt();
            double multiplier = b.readDouble();
            if (openedAt < 0 || revision < 1 || xp < 0 || cap < 5 || cap > 99
                    || !Double.isFinite(multiplier) || multiplier < 0.01 || multiplier > 100)
                throw new DecoderException("Invalid upgrade view");
            var access = new UpgradeAccessContext(accessType, dimension, pos, sourceKey, sourceId, nonce, openedAt);
            return new View(mode, status, new UpgradeView(access, revision, snapshot, xp, cap, multiplier));
        }, (b, p) -> {
            b.writeEnum(p.mode); b.writeEnum(p.status);
            var v = p.view; var a = v.access();
            b.writeEnum(a.type()); b.writeResourceLocation(a.sourceDimension()); b.writeBlockPos(a.sourcePosition());
            b.writeResourceLocation(a.sourceKey()); b.writeUUID(a.sourceId()); b.writeUUID(a.nonce());
            b.writeVarLong(a.openedAt()); b.writeVarLong(v.revision());
            AttributePayloads.Snapshot.STREAM_CODEC.encode(b, new AttributePayloads.Snapshot(v.attributes()));
            b.writeVarInt(v.experience()); b.writeVarInt(v.hardCap()); b.writeDouble(v.costMultiplier());
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, path));
    }
    private static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> codec(
            java.util.function.Function<RegistryFriendlyByteBuf, T> decoder,
            java.util.function.BiConsumer<RegistryFriendlyByteBuf, T> encoder) {
        return new StreamCodec<>() {
            public T decode(RegistryFriendlyByteBuf b) { return decoder.apply(b); }
            public void encode(RegistryFriendlyByteBuf b, T p) { encoder.accept(b, p); }
        };
    }
    private UpgradePayloads() {}
}
