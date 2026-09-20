package dev.maplesadventure.progression.runtime;

import net.minecraft.server.level.ServerPlayer;
import dev.maplesadventure.progression.PlayerAttributeService;
import dev.maplesadventure.progression.status.StatusResistance;

/** Does not manufacture a mana pool when the optional runtime is absent. */
public final class PlayerManaService {
    public static boolean consumeExact(ServerPlayer player,double amount) {
        StatusResistance.bounded(amount,0,1000000);
        return DerivedStatIntegrationRegistry.consumeExact(DerivedRuntimeResource.MANA,player,amount);
    }
    public static boolean consumeFraction(ServerPlayer player,double flat,double fraction) {
        StatusResistance.bounded(flat,0,10000); StatusResistance.bounded(fraction,0,1);
        var snapshot=DerivedStatIntegrationRegistry.inspect(DerivedRuntimeResource.MANA,player,PlayerAttributeService.state(player));
        return consumeExact(player,flat+snapshot.runtimeValue()*fraction);
    }
    private PlayerManaService() {}
}
