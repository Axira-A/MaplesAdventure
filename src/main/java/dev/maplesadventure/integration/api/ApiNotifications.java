package dev.maplesadventure.integration.api;

import java.util.function.Supplier;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/** Internal callback guard, not gameplay state. Restored even when an integration fails. */
public final class ApiNotifications {
    private static final ThreadLocal<Integer> DEPTH=ThreadLocal.withInitial(()->0);
    public static boolean busy() { return DEPTH.get()>0; }
    public static <T> T readOnly(Supplier<T> callback) {
        int depth=DEPTH.get(); DEPTH.set(depth+1);
        try { return callback.get(); }
        finally { if(depth==0) DEPTH.remove(); else DEPTH.set(depth); }
    }
    public static void post(Event event) {
        try { readOnly(()->NeoForge.EVENT_BUS.post(event)); }
        catch(RuntimeException failure) {
            dev.maplesadventure.MaplesAdventure.LOGGER.error("Public API notification listener failed; gameplay already committed",failure);
        }
    }
    private ApiNotifications() {}
}
