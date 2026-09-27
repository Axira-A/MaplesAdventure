package dev.maplesadventure.integration.ironsspellbooks.flask;

import dev.maplesadventure.flask.FlaskManaBridge;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class IronsFlaskAdapter implements FlaskManaBridge.Adapter {
    public IronsFlaskAdapter() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (io.redspace.ironsspellbooks.api.events.SpellPreCastEvent event) -> {
                    if (dev.maplesadventure.api.flask.FlaskApi.isUsingFlask(event.getEntity())) event.setCanceled(true);
                });
    }
    @Override public void restore(ServerPlayer player, double amount) {
        MagicData data = MagicData.getPlayerMagicData(player);
        double maximum = player.getAttributeValue(AttributeRegistry.MAX_MANA);
        if (data == null || !Double.isFinite(maximum) || maximum < 0 || !Float.isFinite(data.getMana()))
            throw new IllegalStateException("Iron's mana not ready");
        data.setMana((float)Math.min(maximum, data.getMana() + amount));
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(data));
    }
}
