package example;

import dev.maplesadventure.api.damage.MaplesDamageChannel;
import dev.maplesadventure.api.weapon.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Compile-checked public API example. The example mod registers its own weapon item. */
public final class WeaponIntegrationExample {
    public static double[] inspect(ServerPlayer player, ItemStack blade) {
        var profile = MaplesWeaponApi.query(blade);
        if (profile.isEmpty()) return new double[0];
        int requiredFaith = profile.get().requirements().get(MaplesWeaponAttribute.FAITH);
        double holyRatio = profile.get().damageComponents().stream()
                .filter(component -> component.channel() == MaplesDamageChannel.HOLY)
                .mapToDouble(WeaponDamageComponentView::baseRatio).sum();
        double nominalAttackRating = MaplesWeaponApi.evaluate(player, blade)
                .map(WeaponEvaluationView::totalAttackRating).orElse(0.0);
        return new double[] {requiredFaith, holyRatio, nominalAttackRating};
    }
    private WeaponIntegrationExample() {}
}
