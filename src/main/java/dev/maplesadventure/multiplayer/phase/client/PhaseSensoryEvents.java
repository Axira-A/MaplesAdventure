package dev.maplesadventure.multiplayer.phase.client;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

/** Filters sounds whose source entity is still known to NeoForge. */
public final class PhaseSensoryEvents {
    public static void register() {
        NeoForge.EVENT_BUS.register(new PhaseSensoryEvents());
    }

    @SubscribeEvent
    public void onSoundAtEntity(PlayLevelSoundEvent.AtEntity event) {
        if (event.getEntity() != null && !PhaseSensoryPolicy.shouldExposeEntitySource(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private PhaseSensoryEvents() {
    }
}
