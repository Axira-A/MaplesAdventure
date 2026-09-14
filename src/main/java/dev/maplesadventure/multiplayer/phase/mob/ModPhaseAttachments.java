package dev.maplesadventure.multiplayer.phase.mob;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.multiplayer.encounter.EncounterMobState;
import dev.maplesadventure.multiplayer.encounter.boss.BossEntityLinkState;
import dev.maplesadventure.multiplayer.phase.loot.PhaseObjectState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** NeoForge-owned persistence and tracking-scoped client synchronization. */
public final class ModPhaseAttachments {
    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MaplesAdventure.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<MobPhaseState>> MOB_PHASE = TYPES.register(
            "mob_phase",
            () -> AttachmentType.serializable(MobPhaseState::new)
                    .sync(MobPhaseState.STREAM_CODEC)
                    .build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<EncounterMobState>> ENCOUNTER_MOB = TYPES.register(
            "encounter_mob",
            () -> AttachmentType.serializable(EncounterMobState::new)
                    .sync(EncounterMobState.STREAM_CODEC)
                    .build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PhaseObjectState>> PHASE_OBJECT = TYPES.register(
            "phase_object",
            () -> AttachmentType.serializable(PhaseObjectState::new)
                    .sync(PhaseObjectState.STREAM_CODEC)
                    .build()
    );

    /** Persistent lineage for every entity owned by a managed boss attempt, including non-Mob effects. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<BossEntityLinkState>> BOSS_ENTITY_LINK = TYPES.register(
            "boss_entity_link",
            () -> AttachmentType.serializable(BossEntityLinkState::new)
                    .sync(BossEntityLinkState.STREAM_CODEC)
                    .build()
    );

    public static void register(IEventBus modBus) { TYPES.register(modBus); }

    private ModPhaseAttachments() {}
}
