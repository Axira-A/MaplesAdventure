package dev.maplesadventure.progression.status;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

public record StatusSourceContext(UUID attackerUUID, SourceKind sourceKind, ResourceLocation weaponItemId,
                                  boolean projectile, StatusEffectType statusType) {
    public enum SourceKind { WEAPON, ADMIN, ENVIRONMENT, ADAPTER }
    public static StatusSourceContext admin(StatusEffectType type) {
        return new StatusSourceContext(null, SourceKind.ADMIN, ResourceLocation.withDefaultNamespace("air"), false, type);
    }
}
