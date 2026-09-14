package dev.maplesadventure.integration.epicfight.echo;

import dev.maplesadventure.multiplayer.echo.EpicFightEchoFrameState;
import dev.maplesadventure.multiplayer.echo.client.EpicFightEchoClientBridge;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.LinkAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/** Epic Fight 21.17.3.1 client API adapter. Loaded reflectively only when Epic Fight is present. */
public final class EpicFightEchoClientAdapter implements EpicFightEchoClientBridge.CaptureAdapter {
    private final Map<Byte, PreviousLayer> previousLayers = new HashMap<>();

    @Override
    public EpicFightEchoFrameState capture() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return EpicFightEchoFrameState.INACTIVE;
        PlayerPatch<?> patch = EpicFightCapabilities.getPlayerPatch(minecraft.player);
        if (patch == null || !patch.isEpicFightMode()) {
            previousLayers.clear();
            return EpicFightEchoFrameState.INACTIVE;
        }
        ClientAnimator animator = patch.getClientAnimator();
        ArrayList<EpicFightEchoFrameState.LayerState> states = new ArrayList<>();
        Map<Byte, PreviousLayer> nextPrevious = new HashMap<>();
        animator.iterVisibleLayers(layer -> captureLayer(patch, animator, layer, states, nextPrevious));
        previousLayers.clear();
        previousLayers.putAll(nextPrevious);
        return states.isEmpty() ? EpicFightEchoFrameState.INACTIVE : new EpicFightEchoFrameState(true, states);
    }

    private void captureLayer(PlayerPatch<?> patch, ClientAnimator animator, Layer layer,
                              ArrayList<EpicFightEchoFrameState.LayerState> states,
                              Map<Byte, PreviousLayer> nextPrevious) {
        if (layer.isOff() || states.size() >= EpicFightEchoFrameState.MAX_LAYERS) return;
        AnimationPlayer player = layer.animationPlayer;
        AssetAccessor<? extends DynamicAnimation> currentAccessor = player.getAnimation();
        if (currentAccessor == null || currentAccessor.isEmpty()) return;
        DynamicAnimation current = currentAccessor.get();
        AssetAccessor<?> realAccessor = player.getRealAnimation();
        if (realAccessor == null || realAccessor.isEmpty() || realAccessor.registryName() == null) return;
        Layer.Priority priorityValue = animator.getPriorityFor(currentAccessor);
        byte priority = (byte) (priorityValue == null ? 0 : priorityValue.ordinal());
        float elapsed = Math.max(0.0F, player.getElapsedTime());
        float previousElapsed = Math.max(0.0F, player.getPrevElapsedTime());
        float speed = Mth.clamp(current.getPlaySpeed(patch, current), 0.0F, 16.0F);
        float blend = 1.0F;
        ResourceLocation previousAnimation = null;

        if (current instanceof LinkAnimation link) {
            blend = Mth.clamp(elapsed / Math.max(0.001F, current.getTotalTime()), 0.0F, 1.0F);
            elapsed = Math.max(0.0F, link.getNextStartTime() + elapsed);
            PreviousLayer remembered = previousLayers.get(priority);
            AssetAccessor<? extends DynamicAnimation> from = link.getFromAnimation();
            AssetAccessor<?> fromReal = from == null || from.isEmpty() ? null : from.get().getRealAnimation();
            if (fromReal != null && fromReal.registryName() != null) {
                previousAnimation = fromReal.registryName();
            }
            if (remembered != null && (previousAnimation == null || previousAnimation.equals(remembered.animationId))) {
                previousAnimation = remembered.animationId;
                previousElapsed = remembered.elapsedTime;
            }
        }

        ResourceLocation animationId = realAccessor.registryName();
        states.add(new EpicFightEchoFrameState.LayerState(animationId, elapsed, previousElapsed,
                speed, blend, priority, previousAnimation));
        nextPrevious.put(priority, new PreviousLayer(animationId, elapsed));
    }

    private record PreviousLayer(ResourceLocation animationId, float elapsedTime) {}
}
