package dev.maplesadventure.integration.bonfires;

import dev.maplesadventure.progression.upgrade.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/** Optional server adapter. It knows Bonfires identity/reachability; progression core does not. */
public final class BonfiresUpgradeAdapter {
    private static final ResourceLocation SOURCE = ResourceLocation.fromNamespaceAndPath("bonfires", "rested_bonfire");
    private static boolean registered;

    public static synchronized void register() {
        if (registered || !ModList.get().isLoaded("bonfires")) return;
        UpgradeAccessRegistry.register(UpgradeAccessType.BONFIRE, BonfiresUpgradeAdapter::validate);
        registered = true;
    }

    public static void onSuccessfulRest(ServerPlayer player, BlockPos position) {
        register();
        if (!registered || !BonfiresAccess.reachable(player, position)) return;
        UUID identity = BonfiresAccess.identity(player.level().getBlockEntity(position));
        if (identity != null) UpgradeAccessService.authorize(player, UpgradeAccessType.BONFIRE,
                player.level().dimension().location(), position, SOURCE, identity);
    }

    private static boolean validate(ServerPlayer player, UpgradeAccessContext context) {
        if (context.type() != UpgradeAccessType.BONFIRE
                || !SOURCE.equals(context.sourceKey())
                || !player.level().dimension().location().equals(context.sourceDimension())
                || !BonfiresAccess.reachable(player, context.sourcePosition())) return false;
        UUID current = BonfiresAccess.identity(player.level().getBlockEntity(context.sourcePosition()));
        return context.sourceId().equals(current);
    }
    private BonfiresUpgradeAdapter() {}
}
