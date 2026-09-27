package dev.maplesadventure.flask;

import dev.maplesadventure.MaplesAdventure;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/** No optional classes in the core constant pool. */
public final class FlaskManaBridge {
    public interface Adapter { void restore(ServerPlayer player, double amount); }
    private static Adapter adapter;
    private static boolean checked;
    public static boolean available() {
        if (!checked) {
            checked = true;
            if (ModList.get().isLoaded("irons_spellbooks")) {
                try {
                    adapter = (Adapter)Class.forName("dev.maplesadventure.integration.ironsspellbooks.flask.IronsFlaskAdapter").getConstructor().newInstance();
                } catch (ReflectiveOperationException | LinkageError error) {
                    MaplesAdventure.LOGGER.error("Flask mana integration unavailable", error);
                }
            }
        }
        return adapter != null;
    }
    public static void restore(ServerPlayer player, double amount) {
        if (!available()) throw new IllegalStateException("Mana integration unavailable");
        adapter.restore(player, amount);
    }
    private FlaskManaBridge() {}
}
