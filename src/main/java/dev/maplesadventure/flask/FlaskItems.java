package dev.maplesadventure.flask;

import dev.maplesadventure.api.flask.FlaskKind;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FlaskItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("maplesadventure");
    public static final DeferredItem<FlaskItem> CRIMSON = ITEMS.register("crimson_flask", () -> new FlaskItem(FlaskKind.CRIMSON));
    public static final DeferredItem<FlaskItem> ASHEN = ITEMS.register("ashen_flask", () -> new FlaskItem(FlaskKind.ASHEN));
    public static final DeferredItem<Item> SHARD = ITEMS.register("estus_shard", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ASH = ITEMS.register("noble_ash", () -> new Item(new Item.Properties()));
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
                event.accept(CRIMSON); event.accept(SHARD); event.accept(ASH);
                if (FlaskManaBridge.available()) event.accept(ASHEN);
            }
        });
    }
    private FlaskItems() {}
}
