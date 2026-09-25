package dev.maplesadventure.integration.epicfight.bonfire;

import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.MainFrameAnimation;
import yesman.epicfight.gameasset.Armatures;

/** Pose-only main action: prevents locomotion replacing rest clips, without ActionAnimation root motion. */
final class BonfirePoseAnimation extends MainFrameAnimation {
    private final boolean looping;

    BonfirePoseAnimation(boolean looping, AnimationManager.AnimationAccessor<BonfirePoseAnimation> accessor) {
        super(0.12F, accessor, Armatures.BIPED);
        this.looping = looping;
        newTimePair(0, Float.MAX_VALUE);
        addState(EntityState.INACTION, true);
        addState(EntityState.UPDATE_LIVING_MOTION, false);
        addState(EntityState.MOVEMENT_LOCKED, true);
        addState(EntityState.TURNING_LOCKED, true);
        addState(EntityState.CAN_USE_ITEM, false);
        addState(EntityState.CAN_SWITCH_HAND_ITEM, false);
        addState(EntityState.SKILL_EXECUTABLE, false);
        addState(EntityState.COMBO_ATTACKS_DOABLE, false);
    }

    @Override public boolean isRepeat() { return looping; }
}
