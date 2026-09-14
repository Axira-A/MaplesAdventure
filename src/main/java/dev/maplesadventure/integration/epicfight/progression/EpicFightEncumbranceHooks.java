package dev.maplesadventure.integration.epicfight.progression;

import dev.maplesadventure.progression.client.ClientAttributeState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.DodgeAnimation;
import yesman.epicfight.network.client.CPChangeSkill;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Tiny source-aware entry points used by optional Epic Fight mixins. */
public final class EpicFightEncumbranceHooks {
    public static boolean rejectManualDodge(CPChangeSkill request, Entity player) {
        if (request.skillSlot() != SkillSlots.DODGE || !(player instanceof ServerPlayer serverPlayer)) return false;
        EpicFightEncumbranceAdapter.rejectManualDodgeChange(serverPlayer);
        return true;
    }

    public static Vec3 scaleDodgeMovement(Vec3 movement, LivingEntityPatch<?> patch,
                                           ActionAnimation animation) {
        if (movement == null || patch == null || !(animation instanceof DodgeAnimation)
                || !(patch instanceof yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch<?>))
            return movement;
        Entity entity = patch.getOriginal();
        // Remote players are moved by authoritative entity tracking, not this client's equipment profile.
        if (!(entity instanceof ServerPlayer)
                && (!(entity instanceof net.minecraft.world.entity.player.Player player) || !player.isLocalPlayer()))
            return movement;
        double multiplier = entity instanceof ServerPlayer serverPlayer
                ? dev.maplesadventure.progression.encumbrance.EncumbranceRuntimeService.profile(serverPlayer)
                .dodgeDistanceMultiplier()
                : ClientAttributeState.snapshot().equipLoad().policy()
                .profile(ClientAttributeState.snapshot().equipLoad().tier()).dodgeDistanceMultiplier();
        return multiplier == 1.0D ? movement : movement.scale(multiplier);
    }

    private EpicFightEncumbranceHooks() {}
}
