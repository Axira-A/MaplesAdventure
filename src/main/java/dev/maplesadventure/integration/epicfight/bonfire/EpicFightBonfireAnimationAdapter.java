package dev.maplesadventure.integration.epicfight.bonfire;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.bonfire.BonfireAnimationIntegration;
import dev.maplesadventure.bonfire.BonfireSessionState;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/** Epic Fight 21.17.3.1 biped assets; loaded only when the optional mod is installed. */
public final class EpicFightBonfireAnimationAdapter implements BonfireAnimationIntegration.Adapter {
    private static AnimationManager.AnimationAccessor<BonfirePoseAnimation> activate;
    private static AnimationManager.AnimationAccessor<BonfirePoseAnimation> sitDown;
    private static AnimationManager.AnimationAccessor<BonfirePoseAnimation> sitIdle;
    private static AnimationManager.AnimationAccessor<BonfirePoseAnimation> standUp;

    @Override public void register(IEventBus modBus) {
        modBus.addListener(this::registerAnimations);
    }

    private void registerAnimations(AnimationManager.AnimationRegistryEvent event) {
        event.newBuilder("maplesadventure", builder -> {
            activate = builder.nextAccessor("bonfire_activate",
                    accessor -> new BonfirePoseAnimation(false, accessor));
            sitDown = builder.nextAccessor("bonfire_sit_down",
                    accessor -> new BonfirePoseAnimation(false, accessor));
            sitIdle = builder.nextAccessor("bonfire_sit_idle",
                    accessor -> new BonfirePoseAnimation(true, accessor));
            standUp = builder.nextAccessor("bonfire_stand_up",
                    accessor -> new BonfirePoseAnimation(false, accessor));
        });
        MaplesAdventure.LOGGER.info("Registered four optional Epic Fight bonfire animations");
    }

    @Override public void play(ServerPlayer player, BonfireSessionState state) {
        var patch = EpicFightCapabilities.getPlayerPatch(player);
        if (patch == null) return;
        var animation = switch (state) {
            case ACTIVATING -> activate;
            case SITTING_DOWN -> sitDown;
            case RESTING -> sitIdle;
            case STANDING_UP -> standUp;
            default -> null;
        };
        if (animation != null) patch.playAnimationSynchronized(animation, 0.0F);
    }

    @Override public void stop(ServerPlayer player) {
        var patch = EpicFightCapabilities.getPlayerPatch(player);
        if (patch != null && Animations.BIPED_IDLE != null)
            patch.playAnimationSynchronized(Animations.BIPED_IDLE, 0.12F);
    }
}
