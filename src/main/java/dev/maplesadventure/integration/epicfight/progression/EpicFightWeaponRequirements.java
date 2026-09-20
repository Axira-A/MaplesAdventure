package dev.maplesadventure.integration.epicfight.progression;
import dev.maplesadventure.progression.weapon.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import java.util.*;
public final class EpicFightWeaponRequirements implements WeaponIntegration {
    public String category(ItemStack stack) {
        var cap=EpicFightCapabilities.getItemStackCapability(stack);
        // CapabilityItem defaults an unspecified category to FIST, including armor/ranged capabilities.
        if(!(cap instanceof yesman.epicfight.world.capabilities.item.WeaponCapability) || cap.isEmpty()) return "";
        if(cap.getWeaponCategory()==yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories.FIST
                && (stack.getItem() instanceof net.minecraft.world.item.DiggerItem
                || stack.getItem() instanceof net.minecraft.world.item.SwordItem
                || stack.getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem
                || stack.getItem() instanceof net.minecraft.world.item.MaceItem
                || stack.getItem() instanceof net.minecraft.world.item.TridentItem)) return "";
        return cap.getWeaponCategory().toString().toUpperCase(Locale.ROOT);
    }
    public Optional<ItemStack> usedWeapon(DamageSource source) {
        return source instanceof EpicFightDamageSource epic?Optional.ofNullable(epic.getUsedItem()).or(()->Optional.of(ItemStack.EMPTY)):Optional.empty();
    }
    public static boolean reject(SkillContainer container) {
        if(dev.maplesadventure.progression.status.StatusControlLockService.locked(container.getExecutor().getOriginal())) return true;
        if(container.getSlot()!=SkillSlots.WEAPON_INNATE || !(container.getExecutor().getOriginal() instanceof ServerPlayer player)) return false;
        // Weapon innate ownership in this version is installed by main-hand CapabilityItem.changeWeaponInnateSkill.
        if(WeaponRequirementService.evaluate(player,player.getMainHandItem()).weaponSkillAllowed()) return false;
        player.displayClientMessage(Component.translatable("message.maplesadventure.weapon.skill_denied"),true);
        return true;
    }
}
