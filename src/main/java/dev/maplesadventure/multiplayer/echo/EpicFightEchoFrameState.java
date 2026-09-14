package dev.maplesadventure.multiplayer.echo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Bounded semantic Epic Fight animation snapshot. It intentionally stores animation registry keys and clocks,
 * never armature matrices or a reference to the live source player.
 */
public record EpicFightEchoFrameState(boolean active, List<LayerState> layers) {
    public static final int MAX_LAYERS = 6;
    public static final EpicFightEchoFrameState INACTIVE = new EpicFightEchoFrameState(false, List.of());

    public EpicFightEchoFrameState {
        if (!active || layers == null || layers.isEmpty()) {
            active = false;
            layers = List.of();
        } else {
            ArrayList<LayerState> bounded = new ArrayList<>(Math.min(MAX_LAYERS, layers.size()));
            for (LayerState layer : layers) {
                if (layer != null && layer.valid() && bounded.size() < MAX_LAYERS) bounded.add(layer);
            }
            bounded.sort(Comparator.comparingInt(LayerState::priority));
            layers = bounded.isEmpty() ? List.of() : List.copyOf(bounded);
            active = !layers.isEmpty();
        }
    }

    public static EpicFightEchoFrameState interpolate(EpicFightEchoFrameState from,
                                                       EpicFightEchoFrameState to, float amount) {
        float t = Mth.clamp(amount, 0.0F, 1.0F);
        if (!from.active || !to.active || from.layers.size() != to.layers.size()) return t < 0.5F ? from : to;
        ArrayList<LayerState> result = new ArrayList<>(from.layers.size());
        for (int index = 0; index < from.layers.size(); index++) {
            LayerState a = from.layers.get(index);
            LayerState b = to.layers.get(index);
            if (a.priority != b.priority || !a.animationId.equals(b.animationId)
                    || !java.util.Objects.equals(a.previousAnimationId, b.previousAnimationId)) {
                return t < 0.5F ? from : to;
            }
            result.add(new LayerState(a.animationId,
                    Mth.lerp(t, a.elapsedTime, b.elapsedTime),
                    Mth.lerp(t, a.previousElapsedTime, b.previousElapsedTime),
                    Mth.lerp(t, a.playSpeed, b.playSpeed),
                    Mth.lerp(t, a.blend, b.blend), a.priority, a.previousAnimationId));
        }
        return new EpicFightEchoFrameState(true, result);
    }

    public record LayerState(ResourceLocation animationId, float elapsedTime, float previousElapsedTime,
                             float playSpeed, float blend, byte priority,
                             ResourceLocation previousAnimationId) {
        public LayerState {
            blend = Mth.clamp(blend, 0.0F, 1.0F);
            priority = (byte) Mth.clamp(priority, 0, 4);
        }

        boolean valid() {
            return animationId != null && Float.isFinite(elapsedTime) && elapsedTime >= 0.0F
                    && Float.isFinite(previousElapsedTime) && previousElapsedTime >= 0.0F
                    && Float.isFinite(playSpeed) && playSpeed >= 0.0F && playSpeed <= 16.0F
                    && Float.isFinite(blend);
        }
    }
}
