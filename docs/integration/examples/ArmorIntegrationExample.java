package example;

import dev.maplesadventure.api.armor.MaplesArmorApi;
import dev.maplesadventure.api.damage.MaplesDamageChannel;
import dev.maplesadventure.api.status.MaplesResistanceType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Compile-checked public API example; only equipped armor contributes to totals. */
public final class ArmorIntegrationExample {
    public static double[] inspect(ServerPlayer player, ItemStack chestplate) {
        double pieceHoly = MaplesArmorApi.query(chestplate)
                .map(profile -> profile.channels().getOrDefault(MaplesDamageChannel.HOLY, 0.0)).orElse(0.0);
        double totalHoly = MaplesArmorApi.equipped(player)
                .map(equipment -> equipment.totalChannels().getOrDefault(MaplesDamageChannel.HOLY, 0.0)).orElse(0.0);
        double robustness = MaplesArmorApi.equipped(player)
                .map(equipment -> equipment.totalResistances().getOrDefault(MaplesResistanceType.ROBUSTNESS, 0.0)).orElse(0.0);
        return new double[] {pieceHoly, totalHoly, robustness};
    }
    private ArmorIntegrationExample() {}
}
