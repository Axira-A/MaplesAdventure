package dev.maplesadventure.integration.epicfight.progression;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.progression.encumbrance.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.player.SkillCastEvent;
import yesman.epicfight.api.event.types.player.SkillConsumeEvent;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPChangeSkill;
import yesman.epicfight.network.server.SPAddLearnedSkill;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/** Epic Fight 21.17.3.1 adapter. Nightfall is discovered only through registered skill IDs. */
public final class EpicFightEncumbranceAdapter implements EncumbranceCombatAdapter {
    public static final ResourceLocation VANILLA_ROLL = ResourceLocation.fromNamespaceAndPath("epicfight", "roll");
    public static final ResourceLocation VANILLA_STEP = ResourceLocation.fromNamespaceAndPath("epicfight", "step");
    public static final ResourceLocation NIGHTFALL_ROLL = NightfallDodgeAdapter.ROLL;
    public static final ResourceLocation NIGHTFALL_STEP = NightfallDodgeAdapter.STEP;
    private static final ResourceLocation REGEN_MODIFIER = ResourceLocation.fromNamespaceAndPath(
            MaplesAdventure.MOD_ID, "encumbrance_stamina_regen");
    private static final int VANILLA_INIT_VERSION = 1;
    private static final int NIGHTFALL_INIT_VERSION = 1;
    private static final ThreadLocal<Boolean> APPLYING_DODGE = ThreadLocal.withInitial(() -> false);

    public EpicFightEncumbranceAdapter() {}

    @Override public void registerHooks() {
        // One subscription per process, shared by client affordability prediction and server consumption.
        EpicFightEventHooks.Player.CONSUME_SKILL.registerEvent(this::onConsumeSkill, "maplesadventure:encumbrance");
        EpicFightEventHooks.Player.CAST_SKILL.registerEvent(this::onCastSkill, "maplesadventure:encumbrance");
    }

    @Override public double currentEquipmentLoad(ServerPlayer player) {
        var instance = player.getAttribute(EpicFightAttributes.WEIGHT);
        if (instance == null) return Double.NaN;
        // Epic Fight's entity-body base (40 for a normal player) is not equipment weight.
        return Math.max(0.0D, instance.getValue() - instance.getBaseValue());
    }

    @Override public DodgeMode currentDodgeMode(ServerPlayer player) {
        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null) return DodgeMode.NONE;
        Skill skill = patch.getSkill(SkillSlots.DODGE).getSkill();
        if (skill == null) return DodgeMode.NONE;
        ResourceLocation id = skill.getRegistryName();
        if (VANILLA_STEP.equals(id) || NIGHTFALL_STEP.equals(id)) return DodgeMode.STEP;
        if (VANILLA_ROLL.equals(id) || NIGHTFALL_ROLL.equals(id)) return DodgeMode.ROLL;
        return DodgeMode.NONE;
    }

    @Override public boolean isDodgeAnimationActive(ServerPlayer player) {
        return dodgeAnimationActive(player);
    }

    private static boolean dodgeAnimationActive(ServerPlayer player) {
        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null) return false;
        // ServerAnimator has one player. getRealAnimation also resolves the link/transition animation.
        // Nightfall's skill extends Skill directly, but both its roll/step animations extend DodgeAnimation.
        var animationPlayer = patch.getAnimator().getPlayerFor(null);
        return animationPlayer != null && !animationPlayer.isEnd()
                && animationPlayer.getRealAnimation().get() instanceof yesman.epicfight.api.animation.types.DodgeAnimation;
    }

    @Override public void initializeAndRegister(ServerPlayer player, CombatSkillInitializationState migration) {
        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null) return;
        List<Holder<Skill>> newlyLearned = new ArrayList<>(4);
        if (migration.vanillaDodgeVersion() < VANILLA_INIT_VERSION) {
            var roll = EpicFightRegistries.SKILL.getHolder(VANILLA_ROLL).orElse(null);
            var step = EpicFightRegistries.SKILL.getHolder(VANILLA_STEP).orElse(null);
            if (roll != null && step != null) {
                learn(patch.getPlayerSkills(), roll, newlyLearned);
                learn(patch.getPlayerSkills(), step, newlyLearned);
                migration.markVanilla(VANILLA_INIT_VERSION);
            } else MaplesAdventure.LOGGER.warn("Epic Fight registered dodge skills are incomplete for {}",
                    player.getGameProfile().getName());
        }
        if (nightfallAvailable() && migration.nightfallDodgeVersion() < NIGHTFALL_INIT_VERSION) {
            var roll = EpicFightRegistries.SKILL.getHolder(NIGHTFALL_ROLL).orElse(null);
            var step = EpicFightRegistries.SKILL.getHolder(NIGHTFALL_STEP).orElse(null);
            if (roll != null && step != null) {
                learn(patch.getPlayerSkills(), roll, newlyLearned);
                learn(patch.getPlayerSkills(), step, newlyLearned);
                migration.markNightfall(NIGHTFALL_INIT_VERSION);
            }
        }
        if (!newlyLearned.isEmpty()) EpicFightNetworkManager.sendToPlayer(
                new SPAddLearnedSkill(List.copyOf(newlyLearned)), player);
    }

    @Override public void apply(ServerPlayer player, EquipLoadRuntimeSnapshot snapshot,
                                EncumbranceProfile profile, boolean allowDodgeSwitch) {
        var regen = player.getAttribute(EpicFightAttributes.STAMINA_REGEN);
        if (regen != null) {
            regen.removeModifier(REGEN_MODIFIER);
            double amount = profile.staminaRegenMultiplier() - 1.0D;
            if (Math.abs(amount) > 0.000_000_1D) regen.addOrReplacePermanentModifier(
                    new AttributeModifier(REGEN_MODIFIER, amount,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        if (allowDodgeSwitch) enforceDodge(player, profile.dodgeMode());
    }

    private void onConsumeSkill(SkillConsumeEvent event) {
        if (event.getResourceType() != Skill.Resource.STAMINA) return;
        if (!(event.getEntityPatch().getOriginal() instanceof net.minecraft.world.entity.player.Player player)) return;
        double multiplier;
        if (player instanceof ServerPlayer serverPlayer) {
            multiplier = EncumbranceRuntimeService.profile(serverPlayer).staminaCostMultiplier();
        } else if (player.isLocalPlayer()) {
            var snapshot = dev.maplesadventure.progression.client.ClientAttributeState.snapshot().equipLoad();
            multiplier = snapshot.policy().profile(snapshot.tier()).staminaCostMultiplier();
        } else return;
        float original = event.getAmount();
        event.setAmount((float) (original * multiplier));
        MaplesAdventure.LOGGER.debug("[Encumbrance] stamina original={} multiplier={} final={} player={}",
                original, multiplier, event.getAmount(), player.getGameProfile().getName());
    }

    private void onCastSkill(SkillCastEvent event) {
        if (event.getSkillContainer().getSlot() != SkillSlots.DODGE) return;
        if (!(event.getPlayerPatch().getOriginal() instanceof ServerPlayer player)) return;
        EncumbranceProfile profile = EncumbranceRuntimeService.profile(player);
        if (!profile.canDodge()) {
            event.cancel();
            player.displayClientMessage(Component.translatable("message.maplesadventure.encumbrance.cannot_dodge"), true);
            return;
        }
    }

    public static void enforceDodge(ServerPlayer player, DodgeMode mode) {
        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null) return;
        ResourceLocation wanted = switch (mode) {
            case STEP -> nightfallAvailable() ? NIGHTFALL_STEP : VANILLA_STEP;
            case ROLL -> nightfallAvailable() ? NIGHTFALL_ROLL : VANILLA_ROLL;
            case NONE -> null;
        };
        Holder.Reference<Skill> holder = wanted == null ? null : EpicFightRegistries.SKILL.getHolder(wanted).orElse(null);
        if (wanted != null && holder == null) {
            ResourceLocation fallback = mode == DodgeMode.STEP ? VANILLA_STEP : VANILLA_ROLL;
            holder = EpicFightRegistries.SKILL.getHolder(fallback).orElse(null);
        }
        Skill current = patch.getSkill(SkillSlots.DODGE).getSkill();
        Skill desired = holder == null ? null : holder.value();
        if (current == desired) return;
        APPLYING_DODGE.set(true);
        try { patch.getSkill(SkillSlots.DODGE).setSkill(desired); }
        finally { APPLYING_DODGE.remove(); }
        if (desired != null && !patch.getPlayerSkills().hasLearned(desired)) {
            patch.getPlayerSkills().addLearnedSkill(desired);
            EpicFightNetworkManager.sendToPlayer(new SPAddLearnedSkill(List.of(holder)), player);
        }
        EpicFightNetworkManager.sendToAllPlayerTrackingThisEntityWithSelf(
                new SPChangeSkill(SkillSlots.DODGE, player.getId(), holder), player);
    }

    public static void rejectManualDodgeChange(ServerPlayer player) {
        EncumbranceProfile profile = EncumbranceRuntimeService.profile(player);
        if (!dodgeAnimationActive(player))
            enforceDodge(player, profile.dodgeMode());
        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch != null) EpicFightNetworkManager.sendToPlayer(
                patch.getSkill(SkillSlots.DODGE).createSyncPacketToLocalPlayer(), player);
        player.displayClientMessage(Component.translatable("message.maplesadventure.encumbrance.dodge_locked"), true);
    }

    /** Also covers skill books/add-on GUIs that bypass CPChangeSkill. Never intercepts initial NBT loading. */
    public static boolean rejectSlotWrite(yesman.epicfight.skill.SkillContainer container, Skill requested) {
        if (APPLYING_DODGE.get() || container.getSlot() != SkillSlots.DODGE
                || !(container.getExecutor().getOriginal() instanceof ServerPlayer player)
                || !EncumbranceRuntimeService.hasSnapshot(player)) return false;
        // After initialization only the runtime owns this slot. Re-initializing its current skill is harmless.
        return requested != container.getSkill();
    }

    private static void learn(yesman.epicfight.world.capabilities.skill.PlayerSkills skills,
                              Holder<Skill> holder, List<Holder<Skill>> newlyLearned) {
        if (skills.hasLearned(holder.value())) return;
        skills.addLearnedSkill(holder.value());
        newlyLearned.add(holder);
    }

    private static boolean nightfallAvailable() {
        return NightfallDodgeAdapter.available();
    }
}
