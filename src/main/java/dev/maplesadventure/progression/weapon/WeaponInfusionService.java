package dev.maplesadventure.progression.weapon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;

public final class WeaponInfusionService {
    public enum SetResult { SUCCESS, EMPTY_HAND, NOT_WEAPON, UNKNOWN_INFUSION, NOT_ELIGIBLE, INVALID_TRANSFORM }

    public static Optional<WeaponInfusionState> state(ItemStack stack) {
        return stack.isEmpty()?Optional.empty():Optional.ofNullable(stack.get(ProgressionDataComponents.WEAPON_INFUSION));
    }
    public static ResourceLocation infusionId(ItemStack stack) {
        return state(stack).map(WeaponInfusionState::infusionId).orElse(WeaponInfusionRegistry.NORMAL_ID);
    }
    public static SetResult set(ServerPlayer player,ResourceLocation id) {
        ItemStack stack=player.getMainHandItem(); if(stack.isEmpty()) return SetResult.EMPTY_HAND;
        var base=WeaponCombatProfileResolver.resolveBase(stack); if(!base.weapon()) return SetResult.NOT_WEAPON;
        var definition=WeaponInfusionRegistry.find(id).orElse(null); if(definition==null) return SetResult.UNKNOWN_INFUSION;
        if(!WeaponInfusionEligibilityService.eligibility(stack).allows(id)) return SetResult.NOT_ELIGIBLE;
        try { WeaponCombatProfileResolver.apply(base,WeaponInfusionState.of(id),definition,WeaponInfusionEligibilityService.eligibility(stack)); }
        catch(RuntimeException invalid) { return SetResult.INVALID_TRANSFORM; }
        stack.set(ProgressionDataComponents.WEAPON_INFUSION,WeaponInfusionState.of(id)); changed(player); return SetResult.SUCCESS;
    }
    public static boolean clear(ServerPlayer player) {
        var stack=player.getMainHandItem(); if(stack.isEmpty()) return false;
        stack.remove(ProgressionDataComponents.WEAPON_INFUSION); changed(player); return true;
    }
    private static void changed(ServerPlayer player) {
        player.getInventory().setChanged(); player.inventoryMenu.broadcastChanges();
        dev.maplesadventure.progression.AttributeSyncService.sync(player);
    }
    private WeaponInfusionService() {}
}
