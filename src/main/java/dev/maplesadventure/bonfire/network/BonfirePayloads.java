package dev.maplesadventure.bonfire.network;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.bonfire.BonfireSessionState;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** The client sends only a nonce and an action; never a position, role or feature flag. */
public final class BonfirePayloads {
    public enum ActionType { REST, LEVEL_UP, LEAVE }
    public record Action(UUID nonce, ActionType action) implements CustomPacketPayload {
        public static final Type<Action> TYPE = BonfirePayloads.type("bonfire_action");
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = codec(
                b -> new Action(b.readUUID(), b.readEnum(ActionType.class)),
                (b, p) -> { b.writeUUID(p.nonce); b.writeEnum(p.action); });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record View(UUID nonce, BonfireSessionState state, String name, boolean canLevelUp) implements CustomPacketPayload {
        public static final Type<View> TYPE = BonfirePayloads.type("bonfire_view");
        public static final StreamCodec<RegistryFriendlyByteBuf, View> CODEC = codec(b -> {
            UUID nonce = b.readUUID();
            BonfireSessionState state = b.readEnum(BonfireSessionState.class);
            String name = b.readUtf(64);
            return new View(nonce, state, name, b.readBoolean());
        }, (b, p) -> { b.writeUUID(p.nonce); b.writeEnum(p.state); b.writeUtf(p.name, 64); b.writeBoolean(p.canLevelUp); });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Closed(UUID nonce) implements CustomPacketPayload {
        public static final Type<Closed> TYPE = BonfirePayloads.type("bonfire_closed");
        public static final StreamCodec<RegistryFriendlyByteBuf, Closed> CODEC = codec(
                b -> new Closed(b.readUUID()), (b, p) -> b.writeUUID(p.nonce));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Activated(String name) implements CustomPacketPayload {
        public static final Type<Activated> TYPE = BonfirePayloads.type("bonfire_activated");
        public static final StreamCodec<RegistryFriendlyByteBuf, Activated> CODEC = codec(
                b -> new Activated(b.readUtf(64)), (b, p) -> b.writeUtf(p.name, 64));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, path));
    }
    private static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> codec(
            java.util.function.Function<RegistryFriendlyByteBuf, T> decoder,
            java.util.function.BiConsumer<RegistryFriendlyByteBuf, T> encoder) {
        return new StreamCodec<>() {
            @Override public T decode(RegistryFriendlyByteBuf buffer) { return decoder.apply(buffer); }
            @Override public void encode(RegistryFriendlyByteBuf buffer, T value) { encoder.accept(buffer, value); }
        };
    }
    private BonfirePayloads() {}
}
