package dev.maplesadventure.integration.soulscombathud;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.api.flask.FlaskApi;
import dev.maplesadventure.api.flask.FlaskKind;
import dev.maplesadventure.client.flask.FlaskClient;
import dev.maplesadventure.flask.FlaskItem;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only optional boundary. No Souls HUD classes are linked by core/server bytecode. */
public final class SoulsFlaskClient {
    private static boolean available, warned;
    private static Method selected, selectedSlot, selectHeld, autoExcluded, matches;
    private static Method cancelPendingRestore, canSwitchHoldingItem;
    private static Field previousSlot, jumpedToSlot;
    private static Object blacklist;
    private static ModConfigSpec.BooleanValue useSelected;
    private static final FlaskQuickUseState USE = new FlaskQuickUseState();
    private static LocalPlayer usePlayer;
    private static ResourceLocation useDimension;

    public static void initialize() {
        if (!ModList.get().isLoaded("souls_combat_hud")) return;
        try {
            String version = ModList.get().getModContainerById("souls_combat_hud").orElseThrow().getModInfo().getVersion().toString();
            if (!SoulsHudCompatibility.supportedVersion(version))
                throw new IllegalStateException("Unverified Souls Combat HUD version " + version);
            Class<?> selection = Class.forName("net.tablesouls.souls_combat_hud.client.util.slots.ConsumableSlotManager");
            Class<?> rendering = Class.forName("net.tablesouls.souls_combat_hud.client.render.EquipmentHudOverlay");
            Class<?> input = Class.forName("net.tablesouls.souls_combat_hud.event.ClientForgeEvents");
            if (!SoulsFlaskHooks.Selection.class.isAssignableFrom(selection)
                    || !SoulsFlaskHooks.Rendering.class.isAssignableFrom(rendering)
                    || !SoulsFlaskHooks.Use.class.isAssignableFrom(input))
                throw new IllegalStateException("Optional HUD hooks missing: " + SoulsHudCompatibility.gateFailure());
            // Validate the critical injection contracts before hiding the standalone display.
            selection.getDeclaredMethod("isConsumable", ItemStack.class, Player.class);
            input.getDeclaredMethod("jumpToSelectedConsumable", Player.class);
            rendering.getDeclaredMethod("render", net.minecraft.client.gui.GuiGraphics.class, net.minecraft.client.DeltaTracker.class);
            rendering.getDeclaredMethod("renderItemSlot", net.minecraft.client.gui.GuiGraphics.class,
                    Minecraft.class, int.class, int.class, ItemStack.class, int.class);
            selected = selection.getMethod("getSelected", Player.class);
            selectedSlot = selection.getMethod("getSelectedHotbarSlot", Player.class);
            selectHeld = selection.getMethod("setSelectedToHeldItem", Player.class);
            autoExcluded = selection.getMethod("isAutoConsumeExcluded", ItemStack.class);
            previousSlot = input.getDeclaredField("previousSlot"); previousSlot.setAccessible(true);
            jumpedToSlot = input.getDeclaredField("jumpedToSlot"); jumpedToSlot.setAccessible(true);
            cancelPendingRestore = input.getDeclaredMethod("cancelPendingRestore"); cancelPendingRestore.setAccessible(true);
            canSwitchHoldingItem = Class.forName("net.tablesouls.souls_combat_hud.compat.epicfight.EpicFightCompat")
                    .getMethod("canSwitchHoldingItem", Player.class);
            var blacklistField = selection.getDeclaredField("EXCLUDE_CONSUMABLES"); blacklistField.setAccessible(true);
            blacklist = blacklistField.get(null); matches = blacklist.getClass().getMethod("matches", ItemStack.class);
            Object equipment = Class.forName("net.tablesouls.souls_combat_hud.config.SoulsCombatHUDConfig").getField("EQUIPMENT_HUD").get(null);
            Object slots = equipment.getClass().getField("slots").get(equipment);
            Object consumable = slots.getClass().getField("consumable").get(slots);
            useSelected = (ModConfigSpec.BooleanValue) consumable.getClass().getField("useConsumableOnSelected").get(consumable);
            available = true;
            NeoForge.EVENT_BUS.addListener(SoulsFlaskClient::tick);
            MaplesAdventure.LOGGER.info("Souls Combat HUD Flask adapter enabled ({})", version);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) { disable(error); }
    }
    public static boolean available() { return available; }
    public static FlaskKind kind(ItemStack stack) {
        if (!(stack.getItem() instanceof FlaskItem item)) return null;
        return FlaskHudPolicy.enabled(item.kind(), FlaskClient.mana(), false) ? item.kind() : null;
    }
    public static boolean recognized(ItemStack stack) {
        if (!available || kind(stack) == null) return false;
        try { return FlaskHudPolicy.enabled(kind(stack), FlaskClient.mana(), (boolean) matches.invoke(blacklist, stack)); }
        catch (ReflectiveOperationException | RuntimeException error) { disable(error); return false; }
    }
    public static ItemStack selected(Player player) {
        if (!available) return ItemStack.EMPTY;
        try { return (ItemStack) selected.invoke(null, player); }
        catch (ReflectiveOperationException | RuntimeException error) { disable(error); return ItemStack.EMPTY; }
    }
    /** -2 = suppress repeat input, -1 = let Souls HUD handle a non-Flask item. */
    public static int requestSlot(Player player) {
        if (!available) return -1;
        if (USE.busy() || FlaskApi.isUsingFlask(player)) return -2;
        try {
            boolean useHeld = useSelected.get() && recognized(player.getMainHandItem());
            int slot = useHeld
                    ? player.getInventory().selected : (int) selectedSlot.invoke(null, player);
            if (slot < 0 || slot > 8) return -1;
            ItemStack stack = player.getInventory().getItem(slot);
            if (!recognized(stack) || (boolean) autoExcluded.invoke(null, stack)) return -1;
            if (!(boolean) canSwitchHoldingItem.invoke(null, player)) return -2;
            if (useHeld) selectHeld.invoke(null, player);
            return player.isUsingItem() ? -2 : slot;
        } catch (ReflectiveOperationException | RuntimeException error) { disable(error); return -1; }
    }
    public static void use(Player player, int slot) {
        var mc = Minecraft.getInstance();
        if (player != mc.player || mc.gameMode == null || mc.getConnection() == null) return;
        var kind = kind(player.getInventory().getItem(slot));
        int previous = player.getInventory().selected;
        try {
            int saved = previousSlot.getInt(null);
            if (jumpedToSlot.getInt(null) == previous && saved >= 0 && saved < 9) previous = saved;
            cancelPendingRestore.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException error) { disable(error); return; }
        if (kind == null || !USE.begin(kind.ordinal() + 1, previous, slot)) return;
        usePlayer = mc.player; useDimension = player.level().dimension().location();
        player.getInventory().selected = slot;
        // Vanilla useItem synchronizes the carried slot before the single use request.
        if (!mc.gameMode.useItem(player, InteractionHand.MAIN_HAND).consumesAction()) USE.snapshot(0);
    }
    public static void snapshot(int usingKind) { USE.snapshot(usingKind); }
    public static void reset() { USE.clear(); usePlayer = null; useDimension = null; SoulsFlaskRendering.reset(); }
    private static void tick(ClientTickEvent.Post event) {
        if (!USE.busy()) return;
        var mc = Minecraft.getInstance(); var player = mc.player;
        if (player == null || player != usePlayer || !player.isAlive() || mc.getConnection() == null
                || !player.level().dimension().location().equals(useDimension)) { reset(); return; }
        int restore = USE.tick(player.getInventory().selected, mc.options.keyUse.isDown());
        if (restore >= 0) {
            player.getInventory().selected = restore;
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(restore));
        }
    }
    private static void disable(Throwable error) {
        available = false; reset();
        if (!warned) { warned = true; MaplesAdventure.LOGGER.warn("Souls Combat HUD Flask compatibility unavailable; retaining standalone Flask HUD", error); }
    }
    private SoulsFlaskClient() {}
}
