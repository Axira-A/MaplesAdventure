package dev.maplesadventure.progression.client;

import dev.maplesadventure.progression.AttributeSnapshot;
import dev.maplesadventure.progression.PlayerAttributeState;

public final class ClientAttributeState {
    private static volatile AttributeSnapshot snapshot;

    public static void update(AttributeSnapshot next) { snapshot = next; }
    public static AttributeSnapshot snapshot() {
        AttributeSnapshot current = snapshot;
        if (current != null) return current;
        return AttributeSnapshot.of(PlayerAttributeState.defaultsState());
    }
    public static boolean synchronizedFromServer() { return snapshot != null; }
    public static void clear() { snapshot = null; }
    private ClientAttributeState() {}
}
