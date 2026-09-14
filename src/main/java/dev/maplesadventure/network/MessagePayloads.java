package dev.maplesadventure.network;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.message.MessagePhrase;
import dev.maplesadventure.message.MessageRating;
import dev.maplesadventure.message.MessageSummary;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public final class MessagePayloads {
    public static final int MAX_SLOT_COUNT = 4;
    public static final int MAX_MODIFIERS_PER_SLOT = 3;
    public static final int MAX_PHRASE_COUNT = 2;
    public static final int MAX_SYNC_MESSAGES = 256;
    private static final int MAX_ID_LENGTH = 32;

    public record Create(List<MessagePhrase> phrases, List<String> connectors) implements CustomPacketPayload {
        public static final Type<Create> TYPE = MessagePayloads.type("create_message");
        public static final StreamCodec<RegistryFriendlyByteBuf, Create> STREAM_CODEC = codec(
                buffer -> new Create(readPhrases(buffer), readConnectors(buffer)),
                (buffer, payload) -> {
                    writePhrases(buffer, payload.phrases);
                    writeConnectors(buffer, payload.connectors, payload.phrases.size());
                });
        public Create { phrases = List.copyOf(phrases); connectors = List.copyOf(connectors); }
        public Create(MessagePhrase phrase) { this(List.of(phrase), List.of()); }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Read(UUID messageId) implements CustomPacketPayload {
        public static final Type<Read> TYPE = MessagePayloads.type("read_message");
        public static final StreamCodec<RegistryFriendlyByteBuf, Read> STREAM_CODEC = uuidCodec(Read::new, Read::messageId);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Rate(UUID messageId, MessageRating rating) implements CustomPacketPayload {
        public static final Type<Rate> TYPE = MessagePayloads.type("rate_message");
        public static final StreamCodec<RegistryFriendlyByteBuf, Rate> STREAM_CODEC = codec(buffer -> {
            UUID id = buffer.readUUID();
            int ordinal = buffer.readUnsignedByte();
            if (ordinal <= 0 || ordinal >= MessageRating.values().length) throw new DecoderException("Invalid message rating");
            return new Rate(id, MessageRating.values()[ordinal]);
        }, (buffer, payload) -> { buffer.writeUUID(payload.messageId); buffer.writeByte(payload.rating.ordinal()); });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Delete(UUID messageId) implements CustomPacketPayload {
        public static final Type<Delete> TYPE = MessagePayloads.type("delete_message");
        public static final StreamCodec<RegistryFriendlyByteBuf, Delete> STREAM_CODEC = uuidCodec(Delete::new, Delete::messageId);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record NearbySnapshot(List<MessageSummary> messages) implements CustomPacketPayload {
        public static final Type<NearbySnapshot> TYPE = MessagePayloads.type("nearby_messages");
        public static final StreamCodec<RegistryFriendlyByteBuf, NearbySnapshot> STREAM_CODEC = codec(buffer -> {
            int count = readBoundedCount(buffer, MAX_SYNC_MESSAGES, "nearby message");
            ArrayList<MessageSummary> result = new ArrayList<>(count);
            for (int index = 0; index < count; index++) result.add(readSummary(buffer));
            return new NearbySnapshot(result);
        }, (buffer, payload) -> {
            if (payload.messages.size() > MAX_SYNC_MESSAGES) throw new IllegalArgumentException("Too many nearby messages");
            buffer.writeVarInt(payload.messages.size());
            for (MessageSummary summary : payload.messages) writeSummary(buffer, summary);
        });
        public NearbySnapshot { messages = List.copyOf(messages); }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Upsert(MessageSummary message) implements CustomPacketPayload {
        public static final Type<Upsert> TYPE = MessagePayloads.type("upsert_message");
        public static final StreamCodec<RegistryFriendlyByteBuf, Upsert> STREAM_CODEC = codec(
                buffer -> new Upsert(readSummary(buffer)), (buffer, payload) -> writeSummary(buffer, payload.message));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Remove(UUID messageId) implements CustomPacketPayload {
        public static final Type<Remove> TYPE = MessagePayloads.type("remove_message");
        public static final StreamCodec<RegistryFriendlyByteBuf, Remove> STREAM_CODEC = uuidCodec(Remove::new, Remove::messageId);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record OpenReader(MessageSummary message, MessageRating viewerRating, boolean ownMessage)
            implements CustomPacketPayload {
        public static final Type<OpenReader> TYPE = MessagePayloads.type("open_message_reader");
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenReader> STREAM_CODEC = codec(buffer -> {
            MessageSummary summary = readSummary(buffer);
            MessageRating rating = MessageRating.fromNetwork(buffer.readUnsignedByte());
            return new OpenReader(summary, rating, buffer.readBoolean());
        }, (buffer, payload) -> {
            writeSummary(buffer, payload.message); buffer.writeByte(payload.viewerRating.ordinal()); buffer.writeBoolean(payload.ownMessage);
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static MessagePhrase readPhrase(RegistryFriendlyByteBuf buffer) {
        String template = buffer.readUtf(MAX_ID_LENGTH);
        int slotCount = readBoundedCount(buffer, MAX_SLOT_COUNT, "message slot");
        Map<String, String> slots = new LinkedHashMap<>();
        for (int index = 0; index < slotCount; index++) {
            String slot = buffer.readUtf(MAX_ID_LENGTH);
            String token = buffer.readUtf(MAX_ID_LENGTH);
            if (slots.putIfAbsent(slot, token) != null) throw new DecoderException("Duplicate message slot");
        }
        int modifierSlotCount = readBoundedCount(buffer, MAX_SLOT_COUNT, "modifier slot");
        Map<String, List<String>> modifiers = new LinkedHashMap<>();
        for (int index = 0; index < modifierSlotCount; index++) {
            String slot = buffer.readUtf(MAX_ID_LENGTH);
            int count = readBoundedCount(buffer, MAX_MODIFIERS_PER_SLOT, "message modifier");
            ArrayList<String> values = new ArrayList<>(count);
            for (int modifier = 0; modifier < count; modifier++) values.add(buffer.readUtf(MAX_ID_LENGTH));
            if (modifiers.putIfAbsent(slot, List.copyOf(values)) != null) throw new DecoderException("Duplicate modifier slot");
        }
        return new MessagePhrase(template, slots, modifiers);
    }

    private static void writePhrase(RegistryFriendlyByteBuf buffer, MessagePhrase phrase) {
        if (phrase.templateId().length() > MAX_ID_LENGTH || phrase.slots().size() > MAX_SLOT_COUNT
                || phrase.modifiers().size() > MAX_SLOT_COUNT) throw new IllegalArgumentException("Unbounded message phrase");
        buffer.writeUtf(phrase.templateId(), MAX_ID_LENGTH);
        buffer.writeVarInt(phrase.slots().size());
        for (Map.Entry<String, String> entry : phrase.slots().entrySet()) {
            buffer.writeUtf(entry.getKey(), MAX_ID_LENGTH); buffer.writeUtf(entry.getValue(), MAX_ID_LENGTH);
        }
        buffer.writeVarInt(phrase.modifiers().size());
        for (Map.Entry<String, List<String>> entry : phrase.modifiers().entrySet()) {
            if (entry.getValue().size() > MAX_MODIFIERS_PER_SLOT) throw new IllegalArgumentException("Too many modifiers");
            buffer.writeUtf(entry.getKey(), MAX_ID_LENGTH);
            buffer.writeVarInt(entry.getValue().size());
            for (String modifier : entry.getValue()) buffer.writeUtf(modifier, MAX_ID_LENGTH);
        }
    }

    private static List<MessagePhrase> readPhrases(RegistryFriendlyByteBuf buffer) {
        int count = readBoundedCount(buffer, MAX_PHRASE_COUNT, "message phrase");
        if (count == 0) throw new DecoderException("Message contains no phrase");
        ArrayList<MessagePhrase> phrases = new ArrayList<>(count);
        for (int index = 0; index < count; index++) phrases.add(readPhrase(buffer));
        return List.copyOf(phrases);
    }

    private static void writePhrases(RegistryFriendlyByteBuf buffer, List<MessagePhrase> phrases) {
        if (phrases.isEmpty() || phrases.size() > MAX_PHRASE_COUNT) throw new IllegalArgumentException("Invalid phrase count");
        buffer.writeVarInt(phrases.size());
        for (MessagePhrase phrase : phrases) writePhrase(buffer, phrase);
    }

    private static List<String> readConnectors(RegistryFriendlyByteBuf buffer) {
        int count = readBoundedCount(buffer, MAX_PHRASE_COUNT - 1, "message connector");
        ArrayList<String> connectors = new ArrayList<>(count);
        for (int index = 0; index < count; index++) connectors.add(buffer.readUtf(MAX_ID_LENGTH));
        return List.copyOf(connectors);
    }

    private static void writeConnectors(RegistryFriendlyByteBuf buffer, List<String> connectors, int phraseCount) {
        if (connectors.size() != phraseCount - 1) throw new IllegalArgumentException("Connector count does not match phrases");
        buffer.writeVarInt(connectors.size());
        for (String connector : connectors) buffer.writeUtf(connector, MAX_ID_LENGTH);
    }

    private static MessageSummary readSummary(RegistryFriendlyByteBuf buffer) {
        UUID id = buffer.readUUID();
        Vec3 position = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        BlockPos support = buffer.readBlockPos();
        float yaw = buffer.readFloat();
        int direction = buffer.readUnsignedByte();
        if (direction >= Direction.values().length) throw new DecoderException("Invalid message surface normal");
        List<MessagePhrase> phrases = readPhrases(buffer);
        List<String> connectors = readConnectors(buffer);
        return new MessageSummary(id, position, support, yaw, Direction.values()[direction], phrases, connectors,
                buffer.readLong(), buffer.readVarInt(), buffer.readVarInt());
    }

    private static void writeSummary(RegistryFriendlyByteBuf buffer, MessageSummary summary) {
        buffer.writeUUID(summary.messageId());
        buffer.writeDouble(summary.position().x); buffer.writeDouble(summary.position().y); buffer.writeDouble(summary.position().z);
        buffer.writeBlockPos(summary.supportPos()); buffer.writeFloat(summary.yaw()); buffer.writeByte(summary.surfaceNormal().ordinal());
        writePhrases(buffer, summary.phrases()); writeConnectors(buffer, summary.connectors(), summary.phrases().size());
        buffer.writeLong(summary.createdAt()); buffer.writeVarInt(summary.positiveRatings()); buffer.writeVarInt(summary.negativeRatings());
    }

    private static int readBoundedCount(RegistryFriendlyByteBuf buffer, int maximum, String label) {
        int count = buffer.readVarInt();
        if (count < 0 || count > maximum) throw new DecoderException(label + " count exceeds bound");
        return count;
    }
    private static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> codec(
            java.util.function.Function<RegistryFriendlyByteBuf, T> decoder,
            java.util.function.BiConsumer<RegistryFriendlyByteBuf, T> encoder) {
        return new StreamCodec<>() {
            @Override public T decode(RegistryFriendlyByteBuf buffer) { return decoder.apply(buffer); }
            @Override public void encode(RegistryFriendlyByteBuf buffer, T value) { encoder.accept(buffer, value); }
        };
    }
    private static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> uuidCodec(
            java.util.function.Function<UUID, T> constructor, java.util.function.Function<T, UUID> getter) {
        return codec(buffer -> constructor.apply(buffer.readUUID()), (buffer, value) -> buffer.writeUUID(getter.apply(value)));
    }
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, path));
    }
    private MessagePayloads() {}
}
