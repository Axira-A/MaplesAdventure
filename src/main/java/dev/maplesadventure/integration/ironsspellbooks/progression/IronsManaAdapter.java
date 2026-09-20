package dev.maplesadventure.integration.ironsspellbooks.progression;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.progression.PlayerAttributeState;
import dev.maplesadventure.progression.runtime.AttributeRuntimeSupport;
import dev.maplesadventure.progression.runtime.DerivedRuntimeResource;
import dev.maplesadventure.progression.runtime.DerivedStatRefreshReason;
import dev.maplesadventure.progression.runtime.DerivedStatRuntimeAdapter;
import dev.maplesadventure.progression.runtime.RuntimeResourceValue;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.OptionalDouble;

/** Loaded reflectively only when Iron's Spells 3.16.3 is present. */
public final class IronsManaAdapter implements DerivedStatRuntimeAdapter {
    private static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            MaplesAdventure.MOD_ID, "progression_mind_mana");
    private static final double IRONS_BASELINE = 100.0D;

    @Override public DerivedRuntimeResource resource() { return DerivedRuntimeResource.MANA; }
    @Override public boolean consumeExact(ServerPlayer player,double amount) {
        MagicData magicData=MagicData.getPlayerMagicData(player);
        if(magicData==null) return false;
        magicData.setMana((float)Math.max(0,magicData.getMana()-amount));
        PacketDistributor.sendToPlayer(player,new SyncManaPacket(magicData));
        return true;
    }

    @Override
    public RuntimeResourceValue refresh(ServerPlayer player, PlayerAttributeState state, DerivedStatRefreshReason reason) {
        var instance = player.getAttribute(AttributeRegistry.MAX_MANA);
        MagicData magicData = MagicData.getPlayerMagicData(player);
        if (instance == null || magicData == null) throw new IllegalStateException("Iron's player mana is not ready");
        double formula = resource().formula(state);
        RuntimeResourceValue value = AttributeRuntimeSupport.apply(instance, MODIFIER_ID, formula, IRONS_BASELINE,
                magicData::getMana, mana -> magicData.setMana((float) mana));
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
        return value;
    }

    @Override public RuntimeResourceValue inspect(ServerPlayer player, PlayerAttributeState state) {
        var instance = player.getAttribute(AttributeRegistry.MAX_MANA);
        if (instance == null) throw new IllegalStateException("Iron's MAX_MANA is absent");
        return AttributeRuntimeSupport.inspect(instance, resource().formula(state));
    }

    @Override
    public OptionalDouble captureCurrentRatio(ServerPlayer player) {
        var instance = player.getAttribute(AttributeRegistry.MAX_MANA);
        MagicData magicData = MagicData.getPlayerMagicData(player);
        if (instance == null || magicData == null) return OptionalDouble.empty();
        return AttributeRuntimeSupport.currentRatio(instance.getValue(), magicData.getMana());
    }

    @Override
    public void restoreCurrentRatio(ServerPlayer player, double ratio) {
        var instance = player.getAttribute(AttributeRegistry.MAX_MANA);
        MagicData magicData = MagicData.getPlayerMagicData(player);
        if (instance == null || magicData == null) return;
        AttributeRuntimeSupport.restoreRatio(instance.getValue(), ratio, value -> magicData.setMana((float) value));
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
    }
}
