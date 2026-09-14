package dev.maplesadventure.client.message;

import dev.maplesadventure.message.MessageSummary;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.phys.AABB;

/** Bounded client-side cache containing only server-synchronized nearby records. */
public final class MessageRenderCache {
    private static final Map<UUID, MessageSummary> MESSAGES = new HashMap<>();

    public static void replace(List<MessageSummary> messages) {
        MESSAGES.clear();
        for (MessageSummary message : messages) MESSAGES.put(message.messageId(), message);
    }

    public static void upsert(MessageSummary message) {
        MESSAGES.put(message.messageId(), message);
    }

    public static void remove(UUID messageId) {
        MESSAGES.remove(messageId);
    }

    public static Optional<MessageSummary> get(UUID messageId) {
        return Optional.ofNullable(MESSAGES.get(messageId));
    }

    public static Collection<MessageSummary> all() {
        return List.copyOf(MESSAGES.values());
    }

    public static List<MessageSummary> inBounds(AABB bounds) {
        ArrayList<MessageSummary> result = new ArrayList<>();
        for (MessageSummary message : MESSAGES.values()) {
            if (bounds.contains(message.position())) result.add(message);
        }
        return result;
    }

    public static void clear() {
        MESSAGES.clear();
    }

    private MessageRenderCache() {
    }
}
