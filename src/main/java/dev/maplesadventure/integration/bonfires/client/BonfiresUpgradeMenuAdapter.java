package dev.maplesadventure.integration.bonfires.client;

import dev.maplesadventure.mixin.client.BonfiresScreenAccessor;
import dev.maplesadventure.progression.client.UpgradeClient;
import dev.maplesadventure.progression.upgrade.UpgradeAccessType;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Inserts one native-looking action using Bonfires' actual root widgets and page state. */
public final class BonfiresUpgradeMenuAdapter {
    private static final String ROOT_SCREEN = "wehavecookies56.bonfires.client.gui.BonfireScreen";
    private static final Map<Screen, Entry> ENTRIES = new WeakHashMap<>();

    public static boolean isRootScreen(Screen screen) {
        return screen != null && screen.getClass().getName().equals(ROOT_SCREEN)
                && screen instanceof BonfiresScreenAccessor access && !access.maplesadventure$isTravelOpen();
    }

    @SubscribeEvent public void onInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!screen.getClass().getName().equals(ROOT_SCREEN) || !(screen instanceof BonfiresScreenAccessor access)) return;
        Button reinforce = access.maplesadventure$reinforceButton();
        Button leave = access.maplesadventure$leaveButton();
        if (reinforce == null || leave == null) return;

        int step = Math.max(21, Math.abs(leave.getY() - reinforce.getY()));
        int upgradeY = reinforce.visible ? reinforce.getY() + step : reinforce.getY();
        leave.setY(upgradeY + step);
        Button upgrade = Button.builder(Component.translatable("screen.maplesadventure.level_up"),
                ignored -> UpgradeClient.requestOpen(UpgradeAccessType.BONFIRE))
                .bounds(reinforce.getX(), upgradeY, reinforce.getWidth(), reinforce.getHeight()).build();
        upgrade.setTooltip(Tooltip.create(Component.translatable("screen.maplesadventure.level_up.entry_hint")));
        event.addListener(upgrade);
        ENTRIES.put(screen, new Entry(access, upgrade, step));
        refresh(ENTRIES.get(screen));
    }

    @SubscribeEvent public void onRender(ScreenEvent.Render.Pre event) {
        Entry entry = ENTRIES.get(event.getScreen());
        if (entry != null) refresh(entry);
    }

    @SubscribeEvent public void onOpening(ScreenEvent.Opening event) {
        Screen current = net.minecraft.client.Minecraft.getInstance().screen;
        if (current != null && current.getClass().getName().equals(ROOT_SCREEN)
                && event.getNewScreen() != current
                && !(event.getNewScreen() instanceof dev.maplesadventure.progression.client.LevelUpScreen)
                && (event.getNewScreen() == null || !event.getNewScreen().getClass().getName().equals(ROOT_SCREEN))) {
            UpgradeClient.clearOffer(true);
        }
    }

    private static void refresh(Entry entry) {
        boolean root = !entry.access.maplesadventure$isTravelOpen();
        Button reinforce = entry.access.maplesadventure$reinforceButton();
        Button leave = entry.access.maplesadventure$leaveButton();
        // Bonfires rewrites the native coordinates after its dimensions packet and
        // when changing pages. Reapply the root layout from the stable Reinforce
        // anchor so the operation is idempotent and Leave never overlaps Upgrade.
        if (root && reinforce != null && leave != null) {
            int upgradeY = reinforce.visible ? reinforce.getY() + entry.step : reinforce.getY();
            entry.button.setX(reinforce.getX());
            entry.button.setY(upgradeY);
            entry.button.setWidth(reinforce.getWidth());
            leave.setY(upgradeY + entry.step);
        }
        entry.button.visible = root;
        entry.button.active = root && UpgradeClient.hasOffer(UpgradeAccessType.BONFIRE)
                && !UpgradeClient.isOpening();
        if (entry.lastActive == null || entry.lastActive != entry.button.active) {
            entry.lastActive = entry.button.active;
            entry.button.setTooltip(Tooltip.create(Component.translatable(entry.button.active
                    ? "screen.maplesadventure.level_up.entry_hint" : "screen.maplesadventure.level_up.no_authorization")));
        }
    }

    private static final class Entry {
        private final BonfiresScreenAccessor access;
        private final Button button;
        private final int step;
        private Boolean lastActive;

        private Entry(BonfiresScreenAccessor access, Button button, int step) {
            this.access = access;
            this.button = button;
            this.step = step;
        }
    }
}
