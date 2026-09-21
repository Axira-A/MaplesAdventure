package example.integration;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import dev.maplesadventure.api.damage.*;
import dev.maplesadventure.api.event.StatusProcEvent;
import dev.maplesadventure.api.status.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Small compilable adapter; call install once from common setup's enqueueWork. */
public final class BossIntegrationExample {
    public static final ResourceKey<DamageType> BOSS_BLAST = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.parse("example:boss_blast"));

    /** The callback may enqueue a visual response, but must not replay damage or reapply Madness. */
    public static void install(Consumer<StatusProcEvent> onMadnessVisual) {
        MaplesTypedDamageApi.register(ResourceLocation.parse("example:boss_blast"),100,source ->
                source.is(BOSS_BLAST)
                    ? Optional.of(new TypedDamage(Map.of(MaplesDamageChannel.MAGIC,20.,MaplesDamageChannel.FIRE,10.)))
                    : Optional.empty());
        NeoForge.EVENT_BUS.addListener((LivingDamageEvent.Post event) -> {
            if (event.getEntity().level().isClientSide() || event.getNewDamage() <= 0
                    || !event.getSource().is(BOSS_BLAST)
                    || !(event.getSource().getEntity() instanceof LivingEntity boss)
                    || !(event.getEntity() instanceof Player)) return;
            // This is the already-completed hit, not a second call to hurt.
            MaplesStatusApi.apply(event.getEntity(),MaplesStatusType.MADNESS,18,
                    StatusSource.integration(ResourceLocation.parse("example:boss"),boss));
        });
        NeoForge.EVENT_BUS.addListener((StatusProcEvent event) -> {
            if (event.type() == MaplesStatusType.MADNESS) onMadnessVisual.accept(event);
        });
    }

    /** Empty means invalid server context; threshold already includes any correction. */
    public static Optional<StatusView> madnessResistance(LivingEntity player) {
        return MaplesStatusApi.query(player,MaplesStatusType.MADNESS);
    }
    private BossIntegrationExample() {}
}
