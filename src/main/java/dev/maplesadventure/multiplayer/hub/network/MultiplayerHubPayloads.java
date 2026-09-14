package dev.maplesadventure.multiplayer.hub.network;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.multiplayer.hub.MultiplayerAction;
import dev.maplesadventure.multiplayer.hub.MultiplayerHubState;
import dev.maplesadventure.multiplayer.phase.PhaseRole;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class MultiplayerHubPayloads {
    private static final int MAX_NAME = 64;

    public record RequestState() implements CustomPacketPayload {
        public static final Type<RequestState> TYPE = MultiplayerHubPayloads.type("multiplayer_hub_request_state");
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestState> STREAM_CODEC = codec(
                buffer -> new RequestState(), (buffer, payload) -> {});
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Action(MultiplayerAction action) implements CustomPacketPayload {
        public static final Type<Action> TYPE = MultiplayerHubPayloads.type("multiplayer_hub_action");
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> STREAM_CODEC = codec(buffer -> {
            int ordinal = buffer.readVarInt();
            if (ordinal < 0 || ordinal >= MultiplayerAction.values().length) throw new DecoderException("Invalid multiplayer action");
            return new Action(MultiplayerAction.values()[ordinal]);
        }, (buffer, payload) -> buffer.writeVarInt(payload.action.ordinal()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record State(MultiplayerHubState state) implements CustomPacketPayload {
        public static final Type<State> TYPE = MultiplayerHubPayloads.type("multiplayer_hub_state");
        public static final StreamCodec<RegistryFriendlyByteBuf, State> STREAM_CODEC = codec(buffer -> {
            int roleOrdinal = buffer.readVarInt();
            if (roleOrdinal < 0 || roleOrdinal >= PhaseRole.values().length) throw new DecoderException("Invalid phase role");
            return new State(new MultiplayerHubState(PhaseRole.values()[roleOrdinal], buffer.readBoolean(), buffer.readBoolean(),
                    buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                    buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                    buffer.readUtf(MAX_NAME), buffer.readUtf(MAX_NAME)));
        }, (buffer, payload) -> {
            MultiplayerHubState state = payload.state;
            buffer.writeVarInt(state.role().ordinal());
            buffer.writeBoolean(state.coopSession()); buffer.writeBoolean(state.hostileSession());
            buffer.writeBoolean(state.invasionQueued()); buffer.writeBoolean(state.coopSign());
            buffer.writeBoolean(state.duelSign()); buffer.writeBoolean(state.bossActive());
            buffer.writeBoolean(state.messageAvailable()); buffer.writeBoolean(state.signAvailable());
            buffer.writeBoolean(state.invasionAvailable());
            buffer.writeUtf(state.cooperatorName(), MAX_NAME); buffer.writeUtf(state.invaderName(), MAX_NAME);
        });
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
    private MultiplayerHubPayloads() {}
}
