package dev.maplesadventure.api.bonfire;

import java.util.Objects;
import net.neoforged.bus.api.Event;

/**
 * Non-cancellable logical-server notification after core rest, resource restore and phase reset commit.
 * Optional restore failures do not roll back rest. Listen on NeoForge.EVENT_BUS; do not initiate another rest.
 */
public final class MaplesBonfireRestCompletedEvent extends Event {
    private final MaplesBonfireContext context;
    public MaplesBonfireRestCompletedEvent(MaplesBonfireContext context) {
        this.context = Objects.requireNonNull(context);
    }
    public MaplesBonfireContext context() { return context; }
}
