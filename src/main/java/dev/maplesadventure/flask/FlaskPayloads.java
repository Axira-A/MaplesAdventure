package dev.maplesadventure.flask;

import dev.maplesadventure.api.flask.FlaskSnapshot;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class FlaskPayloads {
    public enum Page { ALLOCATION, UPGRADE }
    public enum Action { ALLOCATE, UPGRADE_CAPACITY, UPGRADE_POTENCY, CLOSE }
    public enum Result { OK, INVALID_SESSION, INSUFFICIENT_MATERIAL, AT_CAP, INVALID_ALLOCATION, FAILED }
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.parse("maplesadventure:flask_" + path));
    }
    public static void writeState(RegistryFriendlyByteBuf b, FlaskSnapshot s) {
        b.writeByte(s.totalCapacity()); b.writeByte(s.potencyLevel()); b.writeByte(s.crimsonAllocated());
        b.writeByte(s.crimsonRemaining()); b.writeByte(s.ashenAllocated()); b.writeByte(s.ashenRemaining());
    }
    public static FlaskSnapshot readState(RegistryFriendlyByteBuf b) {
        return new FlaskSnapshot(b.readUnsignedByte(), b.readUnsignedByte(), b.readUnsignedByte(),
                b.readUnsignedByte(), b.readUnsignedByte(), b.readUnsignedByte());
    }
    public record Snapshot(FlaskSnapshot state, boolean mana, int usingKind) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = FlaskPayloads.type("snapshot");
        public Snapshot { if (usingKind < 0 || usingKind > 2) throw new IllegalArgumentException("Flask action"); }
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC = StreamCodec.of(
                (b,p) -> { writeState(b,p.state); b.writeBoolean(p.mana); b.writeByte(p.usingKind); },
                b -> new Snapshot(readState(b),b.readBoolean(),b.readUnsignedByte()));
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    /** replyTo is null for opening a feature, otherwise the consumed request nonce. */
    public record Menu(UUID nonce, UUID replyTo, Page page, FlaskSnapshot state, boolean mana, int shards, int ash, Result result) implements CustomPacketPayload {
        public static final Type<Menu> TYPE = FlaskPayloads.type("menu");
        public static final StreamCodec<RegistryFriendlyByteBuf, Menu> CODEC = StreamCodec.of(
                (b,p) -> { b.writeUUID(p.nonce); b.writeNullable(p.replyTo, (buf,id)->buf.writeUUID(id)); b.writeEnum(p.page); writeState(b,p.state); b.writeBoolean(p.mana); b.writeVarInt(p.shards); b.writeVarInt(p.ash); b.writeEnum(p.result); },
                b -> new Menu(b.readUUID(), b.readNullable(buf->buf.readUUID()), b.readEnum(Page.class), readState(b), b.readBoolean(), bounded(b.readVarInt()), bounded(b.readVarInt()), b.readEnum(Result.class)));
        private static int bounded(int value) { if (value < 0 || value > 4096) throw new IllegalArgumentException("Material count"); return value; }
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    /** Client chooses an intent, never a resulting level or price. Server-held page restricts legal intents. */
    public record Request(UUID nonce, Action action, int crimson, int ashen) implements CustomPacketPayload {
        public static final Type<Request> TYPE = FlaskPayloads.type("request");
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (b,p) -> { b.writeUUID(p.nonce); b.writeEnum(p.action); b.writeVarInt(p.crimson); b.writeVarInt(p.ashen); },
                b -> new Request(b.readUUID(),b.readEnum(Action.class),b.readVarInt(),b.readVarInt()));
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    private FlaskPayloads() {}
}
