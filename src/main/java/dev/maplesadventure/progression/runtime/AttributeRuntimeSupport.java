package dev.maplesadventure.progression.runtime;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.OptionalDouble;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public final class AttributeRuntimeSupport {
    private static final double EPSILON = 0.000_001D;

    public static void replaceProgressionModifier(AttributeInstance instance, ResourceLocation id, double amount) {
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null && existing.operation() == AttributeModifier.Operation.ADD_VALUE
                && Math.abs(existing.amount() - amount) <= EPSILON) return;
        instance.removeModifier(id);
        instance.addOrReplacePermanentModifier(new AttributeModifier(id, amount,
                AttributeModifier.Operation.ADD_VALUE));
    }

    public static double addValueScale(AttributeInstance instance) {
        double scale = 1.0D;
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                scale *= 1.0D + modifier.amount();
            }
        }
        return Math.max(EPSILON, scale);
    }

    public static void preserveRatio(double oldMaximum, double oldCurrent, double newMaximum,
                                     DoubleConsumer currentSetter) {
        if (!(oldMaximum > 0.0D) || !Double.isFinite(oldCurrent) || !(newMaximum > 0.0D)) return;
        double ratio = Math.clamp(oldCurrent / oldMaximum, 0.0D, 1.0D);
        currentSetter.accept(Math.clamp(newMaximum * ratio, 0.0D, newMaximum));
    }

    public static OptionalDouble currentRatio(double maximum, double current) {
        if (!(maximum > 0.0D) || !Double.isFinite(current)) return OptionalDouble.empty();
        return OptionalDouble.of(Math.clamp(current / maximum, 0.0D, 1.0D));
    }

    public static void restoreRatio(double maximum, double ratio, DoubleConsumer currentSetter) {
        if (!(maximum > 0.0D) || !Double.isFinite(ratio)) return;
        currentSetter.accept(Math.clamp(maximum * Math.clamp(ratio, 0.0D, 1.0D), 0.0D, maximum));
    }

    public static RuntimeResourceValue apply(AttributeInstance instance, ResourceLocation modifierId,
                                             double formula, double progressionBaseline,
                                             DoubleSupplier currentGetter, DoubleConsumer currentSetter) {
        double oldMaximum = instance.getValue();
        double oldCurrent = currentGetter.getAsDouble();
        replaceProgressionModifier(instance, modifierId, formula - progressionBaseline);
        double newMaximum = instance.getValue();
        preserveRatio(oldMaximum, oldCurrent, newMaximum, currentSetter);
        return new RuntimeResourceValue(formula, newMaximum, addValueScale(instance),
                dev.maplesadventure.progression.stats.StatImplementationState.ACTIVE);
    }

    public static RuntimeResourceValue inspect(AttributeInstance instance, double formula) {
        return new RuntimeResourceValue(formula, instance.getValue(), addValueScale(instance),
                dev.maplesadventure.progression.stats.StatImplementationState.ACTIVE);
    }

    private AttributeRuntimeSupport() {}
}
