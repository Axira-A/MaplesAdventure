package dev.maplesadventure.client.soul;

import dev.maplesadventure.config.InteractionConfig;
import dev.maplesadventure.soul.LostSoulEntity;
import net.minecraft.core.particles.ParticleTypes;

/** Low-frequency client-only particles; no blocks, collision, fire ticks, or damage are involved. */
public final class LostSoulVisualEffects {
    public static void tick(LostSoulEntity soul) {
        if (!InteractionConfig.LOST_SOUL_PARTICLES.get() || soul.isRemoved()) {
            return;
        }
        int phase = Math.floorMod(soul.getId(), 20);
        double time = soul.tickCount * 0.19D + phase;
        if ((soul.tickCount + phase) % 7 == 0) {
            double x = soul.getX() + Math.sin(time) * 0.18D;
            double z = soul.getZ() + Math.cos(time * 1.13D) * 0.18D;
            soul.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, soul.getY() + 0.12D, z, 0.0D, 0.012D, 0.0D);
        }
        if ((soul.tickCount + phase) % 20 == 0) {
            double x = soul.getX() + Math.cos(time * 0.71D) * 0.24D;
            double z = soul.getZ() + Math.sin(time * 0.83D) * 0.24D;
            soul.level().addParticle(ParticleTypes.SOUL, x, soul.getY() + 0.28D, z, 0.0D, 0.018D, 0.0D);
        }
    }

    private LostSoulVisualEffects() {
    }
}
