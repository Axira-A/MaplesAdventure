package dev.maplesadventure.multiplayer.encounter;

import dev.maplesadventure.multiplayer.encounter.boss.BossAttemptState;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Persistent state for one definition in one phase. Mutated only on the server thread. */
public final class PhaseEncounterState {
    private EncounterStatus status;
    private long generation;
    private int bossStage;
    private BossAttemptState bossAttempt;
    private final Set<UUID> livingEntities = new LinkedHashSet<>();

    public PhaseEncounterState() { this(EncounterStatus.READY, 0L, 1); }
    public PhaseEncounterState(EncounterStatus status, long generation, int bossStage) {
        this.status = status;
        this.generation = generation;
        this.bossStage = Math.max(1, bossStage);
    }

    public EncounterStatus status() { return status; }
    public long generation() { return generation; }
    public int bossStage() { return bossStage; }
    public Set<UUID> livingEntities() { return Set.copyOf(livingEntities); }
    public BossAttemptState bossAttempt() { return bossAttempt; }
    public void setStatus(EncounterStatus status) { this.status = status; }
    public void setGeneration(long generation) { this.generation = generation; }
    public void setBossStage(int stage) {
        this.bossStage = Math.max(1, stage);
        if (bossAttempt != null) bossAttempt.setBossStage(this.bossStage);
    }
    public void setBossAttempt(BossAttemptState attempt) {
        bossAttempt = attempt;
        if (attempt != null) bossStage = attempt.bossStage();
    }
    public void clearBossAttempt() { bossAttempt = null; }
    public void clearLiving() { livingEntities.clear(); }
    public void addLiving(UUID uuid) { livingEntities.add(uuid); }
    public boolean removeLiving(UUID uuid) { return livingEntities.remove(uuid); }
    public boolean hasLiving() { return !livingEntities.isEmpty(); }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Status", status.name());
        tag.putLong("Generation", generation);
        tag.putInt("BossStage", bossStage);
        if (bossAttempt != null) tag.put("BossAttempt", bossAttempt.save());
        ListTag entities = new ListTag();
        for (UUID uuid : livingEntities) {
            CompoundTag entity = new CompoundTag();
            entity.putUUID("UUID", uuid);
            entities.add(entity);
        }
        tag.put("Living", entities);
        return tag;
    }

    public static PhaseEncounterState load(CompoundTag tag, EncounterType type) {
        EncounterStatus status;
        try { status = EncounterStatus.valueOf(tag.getString("Status")); }
        catch (IllegalArgumentException exception) { status = EncounterStatus.READY; }
        if (!status.validFor(type)) status = EncounterStatus.READY;
        PhaseEncounterState state = new PhaseEncounterState(status, Math.max(0L, tag.getLong("Generation")),
                Math.max(1, tag.getInt("BossStage")));
        ListTag entities = tag.getList("Living", Tag.TAG_COMPOUND);
        for (int i = 0; i < entities.size(); i++) {
            CompoundTag entity = entities.getCompound(i);
            if (entity.hasUUID("UUID")) state.livingEntities.add(entity.getUUID("UUID"));
        }
        if (type == EncounterType.BOSS && status == EncounterStatus.ACTIVE) {
            state.bossAttempt = tag.contains("BossAttempt", Tag.TAG_COMPOUND)
                    ? BossAttemptState.load(tag.getCompound("BossAttempt"), state.generation, state.bossStage)
                    : BossAttemptState.legacy(state.generation, state.bossStage);
            state.bossStage = state.bossAttempt.bossStage();
        }
        return state;
    }
}
