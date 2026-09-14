package dev.maplesadventure.multiplayer.echo.network;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.multiplayer.echo.EchoAppearanceSnapshot;
import dev.maplesadventure.multiplayer.echo.EpicFightEchoFrameState;
import dev.maplesadventure.multiplayer.echo.EchoFrame;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class EchoPayloads {
    public static final int MAX_FRAMES = 256;
    private static final int MAX_PROFILE_NAME = 16;
    private static final int MAX_ANIMATION_ID = 128;

    public record AnimationSample(EpicFightEchoFrameState state) implements CustomPacketPayload {
        public static final Type<AnimationSample> TYPE = EchoPayloads.type("echo_epicfight_sample");
        public static final StreamCodec<RegistryFriendlyByteBuf, AnimationSample> STREAM_CODEC = codec(
                buffer -> new AnimationSample(readEpicFightState(buffer)),
                (buffer, payload) -> writeEpicFightState(buffer, payload.state));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Playback(UUID playbackId, ResourceLocation dimension, EchoAppearanceSnapshot appearance,
                           List<EchoFrame> frames, int fadeInTicks, int fadeOutTicks)
            implements CustomPacketPayload {
        public static final Type<Playback> TYPE = EchoPayloads.type("echo_playback");
        public static final StreamCodec<RegistryFriendlyByteBuf, Playback> STREAM_CODEC = codec(buffer -> {
            UUID playbackId = buffer.readUUID();
            ResourceLocation dimension = buffer.readResourceLocation();
            EchoAppearanceSnapshot appearance = readAppearance(buffer);
            int count = buffer.readVarInt();
            if (count < 2 || count > MAX_FRAMES) throw new DecoderException("Invalid echo frame count: " + count);
            ArrayList<EchoFrame> frames = new ArrayList<>(count);
            for (int index = 0; index < count; index++) frames.add(readFrame(buffer));
            int fadeIn = buffer.readUnsignedByte();
            int fadeOut = buffer.readUnsignedByte();
            if (fadeIn > 40 || fadeOut > 60) throw new DecoderException("Invalid echo fade duration");
            return new Playback(playbackId, dimension, appearance, frames, fadeIn, fadeOut);
        }, (buffer, payload) -> {
            if (payload.frames.size() < 2 || payload.frames.size() > MAX_FRAMES) {
                throw new IllegalArgumentException("Echo frame count is outside the protocol bound");
            }
            buffer.writeUUID(payload.playbackId);
            buffer.writeResourceLocation(payload.dimension);
            writeAppearance(buffer, payload.appearance);
            buffer.writeVarInt(payload.frames.size());
            for (EchoFrame frame : payload.frames) writeFrame(buffer, frame);
            buffer.writeByte(payload.fadeInTicks);
            buffer.writeByte(payload.fadeOutTicks);
        });
        public Playback { frames = List.copyOf(frames); }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static EchoAppearanceSnapshot readAppearance(RegistryFriendlyByteBuf buffer) {
        UUID id = buffer.readUUID();
        String name = buffer.readUtf(MAX_PROFILE_NAME);
        return new EchoAppearanceSnapshot(id, name, readStack(buffer), readStack(buffer), readStack(buffer),
                readStack(buffer), readStack(buffer), readStack(buffer));
    }

    private static void writeAppearance(RegistryFriendlyByteBuf buffer, EchoAppearanceSnapshot appearance) {
        buffer.writeUUID(appearance.playerId());
        buffer.writeUtf(appearance.profileName(), MAX_PROFILE_NAME);
        writeStack(buffer, appearance.head()); writeStack(buffer, appearance.chest());
        writeStack(buffer, appearance.legs()); writeStack(buffer, appearance.feet());
        writeStack(buffer, appearance.mainHand()); writeStack(buffer, appearance.offHand());
    }

    private static ItemStack readStack(RegistryFriendlyByteBuf buffer) {
        ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        return stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }
    private static void writeStack(RegistryFriendlyByteBuf buffer, ItemStack stack) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
    }

    private static EchoFrame readFrame(RegistryFriendlyByteBuf buffer) {
        EchoFrame frame = new EchoFrame(buffer.readVarInt(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readByte(), buffer.readBoolean(),
                readEpicFightState(buffer));
        if (!Double.isFinite(frame.x()) || !Double.isFinite(frame.y()) || !Double.isFinite(frame.z())) {
            throw new DecoderException("Non-finite echo position");
        }
        return frame;
    }
    private static void writeFrame(RegistryFriendlyByteBuf buffer, EchoFrame frame) {
        buffer.writeVarInt(frame.serverTick());
        buffer.writeDouble(frame.x()); buffer.writeDouble(frame.y()); buffer.writeDouble(frame.z());
        buffer.writeFloat(frame.yaw()); buffer.writeFloat(frame.pitch()); buffer.writeFloat(frame.bodyYaw());
        buffer.writeFloat(frame.headYaw()); buffer.writeFloat(frame.walkPosition()); buffer.writeFloat(frame.walkSpeed());
        buffer.writeFloat(frame.swingProgress()); buffer.writeByte(frame.pose()); buffer.writeBoolean(frame.onGround());
        writeEpicFightState(buffer, frame.epicFight());
    }

    private static EpicFightEchoFrameState readEpicFightState(RegistryFriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) return EpicFightEchoFrameState.INACTIVE;
        int count = buffer.readUnsignedByte();
        if (count < 1 || count > EpicFightEchoFrameState.MAX_LAYERS) {
            throw new DecoderException("Invalid Epic Fight echo layer count: " + count);
        }
        ArrayList<EpicFightEchoFrameState.LayerState> layers = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            ResourceLocation animation = readAnimationId(buffer);
            float elapsed = buffer.readFloat();
            float previousElapsed = buffer.readFloat();
            float speed = buffer.readFloat();
            float blend = buffer.readFloat();
            byte priority = buffer.readByte();
            ResourceLocation previous = buffer.readBoolean() ? readAnimationId(buffer) : null;
            EpicFightEchoFrameState.LayerState layer = new EpicFightEchoFrameState.LayerState(
                    animation, elapsed, previousElapsed, speed, blend, priority, previous);
            if (!Float.isFinite(elapsed) || !Float.isFinite(previousElapsed) || !Float.isFinite(speed)
                    || !Float.isFinite(blend) || elapsed < 0.0F || previousElapsed < 0.0F
                    || speed < 0.0F || speed > 16.0F) {
                throw new DecoderException("Invalid Epic Fight echo animation clock");
            }
            layers.add(layer);
        }
        return new EpicFightEchoFrameState(true, layers);
    }

    private static void writeEpicFightState(RegistryFriendlyByteBuf buffer, EpicFightEchoFrameState state) {
        buffer.writeBoolean(state != null && state.active());
        if (state == null || !state.active()) return;
        if (state.layers().isEmpty() || state.layers().size() > EpicFightEchoFrameState.MAX_LAYERS) {
            throw new IllegalArgumentException("Epic Fight echo layer count is outside the protocol bound");
        }
        buffer.writeByte(state.layers().size());
        for (EpicFightEchoFrameState.LayerState layer : state.layers()) {
            writeAnimationId(buffer, layer.animationId());
            buffer.writeFloat(layer.elapsedTime()); buffer.writeFloat(layer.previousElapsedTime());
            buffer.writeFloat(layer.playSpeed()); buffer.writeFloat(layer.blend()); buffer.writeByte(layer.priority());
            buffer.writeBoolean(layer.previousAnimationId() != null);
            if (layer.previousAnimationId() != null) writeAnimationId(buffer, layer.previousAnimationId());
        }
    }

    private static ResourceLocation readAnimationId(RegistryFriendlyByteBuf buffer) {
        ResourceLocation id = ResourceLocation.tryParse(buffer.readUtf(MAX_ANIMATION_ID));
        if (id == null) throw new DecoderException("Invalid Epic Fight animation id");
        return id;
    }

    private static void writeAnimationId(RegistryFriendlyByteBuf buffer, ResourceLocation id) {
        String value = id.toString();
        if (value.length() > MAX_ANIMATION_ID) throw new IllegalArgumentException("Epic Fight animation id too long");
        buffer.writeUtf(value, MAX_ANIMATION_ID);
    }

    private static <T> StreamCodec<RegistryFriendlyByteBuf, T> codec(
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
    private EchoPayloads() {}
}
