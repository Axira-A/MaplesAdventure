package dev.maplesadventure.regression;

import dev.maplesadventure.progression.status.*;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.server.level.ServerPlayer;

/** Loaded only by the optional fixture branch when Iron's is installed. */
final class StatusManaRegression {
    static void run(ServerPlayer player) {
        var mana=MagicData.getPlayerMagicData(player);
        double max=player.getAttributeValue(AttributeRegistry.MAX_MANA);
        float hp=player.getHealth();
        mana.setMana((float)max);
        StatusBuildupService.proc(player,StatusEffectType.SLEEP,StatusSourceContext.admin(StatusEffectType.SLEEP));
        PlayerDefenseRegression.check(Math.abs(mana.getMana()-Math.max(0,max-30-max*.1))<.001,"Sleep drains real Iron's mana 30+10% MAX, once");
        PlayerDefenseRegression.check(player.getHealth()==hp&&StatusControlLockService.locked(player),"Player Sleep has control lock but no HP damage");
        StatusRuntimeService.clearAll(player,StatusRuntimeService.ClearReason.ADMIN);
        mana.setMana(5);
        StatusBuildupService.proc(player,StatusEffectType.MADNESS,StatusSourceContext.admin(StatusEffectType.MADNESS));
        PlayerDefenseRegression.check(mana.getMana()==0,"Madness mana clamps at zero");
        PlayerDefenseRegression.check(Math.abs(hp-player.getHealth()-player.getMaxHealth()*.15)<.01,"Madness player burst is 15% MAX HP");
        StatusRuntimeService.clearAll(player,StatusRuntimeService.ClearReason.SESSION_RETURN);
        PlayerDefenseRegression.check(!StatusControlLockService.locked(player),"Phantom return clears control lock");
    }
}
