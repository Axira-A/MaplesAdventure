package dev.maplesadventure.message;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MessageConnectorRegistry {
    private static final Map<String, MessageConnector> CONNECTORS = new LinkedHashMap<>();

    static {
        register("and");
        register("but");
        register("therefore");
        register("however");
        register("because");
        register("then");
    }

    public static Optional<MessageConnector> find(String id) {
        return Optional.ofNullable(CONNECTORS.get(id));
    }

    public static List<MessageConnector> all() {
        return List.copyOf(CONNECTORS.values());
    }

    private static void register(String id) {
        CONNECTORS.put(id, new MessageConnector(
                id,
                "message.maplesadventure.connector." + id,
                "message.maplesadventure.connector.join." + id
        ));
    }

    private MessageConnectorRegistry() {
    }
}
