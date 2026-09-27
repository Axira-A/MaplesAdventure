package dev.maplesadventure.integration.soulscombathud;

import java.util.List;
import java.util.Map;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

/** Early bytecode contracts: inspect optional targets without loading Minecraft or HUD classes. */
public final class SoulsHudCompatibility {
    private static String gateFailure = "Early compatibility gate did not run";
    public static String gateFailure() { return gateFailure; }
    public static boolean reject(String reason) { gateFailure = reason; return false; }
    private static final String ROOT = "net.tablesouls.souls_combat_hud.";
    private static final String GUI = "Lnet/minecraft/client/gui/GuiGraphics;";
    private static final String MC = "Lnet/minecraft/client/Minecraft;";
    private static final String STACK = "Lnet/minecraft/world/item/ItemStack;";
    private static final String PLAYER = "Lnet/minecraft/world/entity/player/Player;";
    private static final String ID = "Lnet/minecraft/resources/ResourceLocation;";
    public record MethodContract(String name, String descriptor, boolean isStatic) {}
    public static final Map<String, List<MethodContract>> CONTRACTS = Map.of(
            ROOT + "client.util.slots.ConsumableSlotManager", List.of(
                    new MethodContract("isConsumable", "(" + STACK + PLAYER + ")Z", true)),
            ROOT + "event.ClientForgeEvents", List.of(
                    new MethodContract("jumpToSelectedConsumable", "(" + PLAYER + ")V", true)),
            ROOT + "client.render.EquipmentHudOverlay", List.of(
                    new MethodContract("render", "(" + GUI + "Lnet/minecraft/client/DeltaTracker;)V", false),
                    new MethodContract("renderItemSlot", "(" + GUI + MC + "II" + STACK + "I)V", false),
                    new MethodContract("renderWeaponSlot", "(" + GUI + MC + "II" + STACK + "Z)V", false),
                    new MethodContract("renderIconSlot", "(" + GUI + "II" + ID + "F)V", false),
                    new MethodContract("renderPreviewItemSlot", "(" + GUI + MC + "II" + STACK + ")V", false),
                    new MethodContract("renderPreviewIconSlot", "(" + GUI + "II" + ID + "F)V", false),
                    new MethodContract("drawName", "(" + GUI + MC + "ILjava/lang/String;IZF)V", false)));

    public static boolean supportedVersion(String version) {
        return version != null && (version.equals("1.3.1") || version.startsWith("1.3.1+") || version.startsWith("1.3.1-"));
    }
    public static boolean matches(String target, ClassNode node) {
        var contracts = CONTRACTS.get(target);
        return contracts != null && node != null && contracts.stream().allMatch(contract -> node.methods.stream().anyMatch(method ->
                method.name.equals(contract.name()) && method.desc.equals(contract.descriptor())
                        && ((method.access & Opcodes.ACC_STATIC) != 0) == contract.isStatic()));
    }
    private SoulsHudCompatibility() {}
}
