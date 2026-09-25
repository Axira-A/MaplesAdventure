package dev.maplesadventure.multiplayer.encounter;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.multiplayer.phase.PhaseId;
import dev.maplesadventure.multiplayer.encounter.fog.FogGateDefinition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

/** Definitions and per-phase runtime are global server SavedData; the chunk index is rebuilt, not duplicated on disk. */
public final class EncounterSavedData extends SavedData {
    private static final int DATA_VERSION = 4;
    private static final String DATA_NAME = "maplesadventure_encounters";
    private static final Factory<EncounterSavedData> FACTORY = new Factory<>(EncounterSavedData::new, EncounterSavedData::load);

    private final Map<ResourceLocation, EncounterDefinition> definitions = new HashMap<>();
    private final Map<PhaseEncounterKey, PhaseEncounterState> runtime = new HashMap<>();
    private final Map<PhaseId, Long> phaseGenerations = new HashMap<>();
    private final Map<ResourceLocation, FogGateDefinition> fogGates = new HashMap<>();
    private final Map<ResourceKey<Level>, Map<Long, ResourceLocation>> fogGateBlockIndex = new HashMap<>();
    private final Map<ResourceKey<Level>, Map<Long, Set<ResourceLocation>>> spatialIndex = new HashMap<>();

    public static EncounterSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public Collection<EncounterDefinition> definitions() { return List.copyOf(definitions.values()); }
    public Optional<EncounterDefinition> definition(ResourceLocation id) { return Optional.ofNullable(definitions.get(id)); }
    public void putDefinition(EncounterDefinition definition) {
        EncounterDefinition old = definitions.put(definition.encounterId(), definition);
        if (old != null) deindex(old);
        index(definition);
        setDirty();
    }
    public Optional<EncounterDefinition> removeDefinition(ResourceLocation id) {
        EncounterDefinition old = definitions.remove(id);
        if (old == null) return Optional.empty();
        deindex(old);
        removeFogGate(id);
        runtime.keySet().removeIf(key -> key.encounterId().equals(id));
        setDirty();
        return Optional.of(old);
    }

    public PhaseEncounterState state(PhaseId phaseId, EncounterDefinition definition) {
        PhaseEncounterKey key = new PhaseEncounterKey(phaseId, definition.encounterId());
        PhaseEncounterState state = runtime.get(key);
        if (state == null) {
            state = new PhaseEncounterState(EncounterStatus.READY, generation(phaseId), 1);
            runtime.put(key, state);
            setDirty();
        }
        return state;
    }
    public Optional<PhaseEncounterState> existingState(PhaseId phaseId, ResourceLocation encounterId) {
        return Optional.ofNullable(runtime.get(new PhaseEncounterKey(phaseId, encounterId)));
    }
    public Map<PhaseEncounterKey, PhaseEncounterState> runtimeSnapshot() { return Map.copyOf(runtime); }
    public long generation(PhaseId phaseId) { return phaseGenerations.getOrDefault(phaseId, 0L); }
    public long incrementGeneration(PhaseId phaseId) {
        // Per-encounter admin resets can advance beyond the phase counter. Never reuse that
        // generation on a later bonfire reset: unloaded old entities must remain invalid.
        long latest = generation(phaseId);
        for (var entry : runtime.entrySet()) {
            if (entry.getKey().phaseId().equals(phaseId)) latest = Math.max(latest, entry.getValue().generation());
        }
        long next = latest == Long.MAX_VALUE ? 1L : latest + 1L;
        phaseGenerations.put(phaseId, next);
        setDirty();
        return next;
    }
    public void changed() { setDirty(); }

    public Optional<FogGateDefinition> fogGate(ResourceLocation encounterId) {
        return Optional.ofNullable(fogGates.get(encounterId));
    }
    public Optional<FogGateDefinition> fogGate(ResourceKey<Level> dimension, net.minecraft.core.BlockPos pos) {
        Map<Long, ResourceLocation> index = fogGateBlockIndex.get(dimension);
        ResourceLocation encounter = index == null ? null : index.get(pos.asLong());
        return encounter == null ? Optional.empty() : fogGate(encounter);
    }
    public Collection<FogGateDefinition> fogGates() { return List.copyOf(fogGates.values()); }
    public void putFogGate(FogGateDefinition definition) {
        FogGateDefinition old = fogGates.put(definition.bossEncounterId(), definition);
        if (old != null) deindexFogGate(old);
        indexFogGate(definition);
        setDirty();
    }
    public Optional<FogGateDefinition> removeFogGate(ResourceLocation encounterId) {
        FogGateDefinition old = fogGates.remove(encounterId);
        if (old == null) return Optional.empty();
        deindexFogGate(old);
        setDirty();
        return Optional.of(old);
    }

    public List<EncounterDefinition> nearby(ResourceKey<Level> dimension, Vec3 position) {
        Map<Long, Set<ResourceLocation>> chunks = spatialIndex.get(dimension);
        if (chunks == null) return List.of();
        Set<ResourceLocation> ids = chunks.get(ChunkPos.asLong(((int) Math.floor(position.x)) >> 4,
                ((int) Math.floor(position.z)) >> 4));
        if (ids == null) return List.of();
        ArrayList<EncounterDefinition> result = new ArrayList<>(ids.size());
        for (ResourceLocation id : ids) {
            EncounterDefinition definition = definitions.get(id);
            if (definition != null) result.add(definition);
        }
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("DataVersion", DATA_VERSION);
        ListTag definitionTags = new ListTag();
        definitions.values().forEach(definition -> definitionTags.add(definition.save()));
        tag.put("Definitions", definitionTags);
        ListTag runtimeTags = new ListTag();
        runtime.forEach((key, state) -> {
            CompoundTag entry = state.save();
            entry.putUUID("Phase", key.phaseId().value());
            entry.putString("Encounter", key.encounterId().toString());
            runtimeTags.add(entry);
        });
        tag.put("Runtime", runtimeTags);
        ListTag generations = new ListTag();
        phaseGenerations.forEach((phase, generation) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Phase", phase.value());
            entry.putLong("Generation", generation);
            generations.add(entry);
        });
        tag.put("PhaseGenerations", generations);
        ListTag gates = new ListTag();
        fogGates.values().forEach(gate -> gates.add(gate.save()));
        tag.put("FogGates", gates);
        return tag;
    }

    private static EncounterSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        EncounterSavedData data = new EncounterSavedData();
        ListTag definitionTags = tag.getList("Definitions", Tag.TAG_COMPOUND);
        for (int i = 0; i < definitionTags.size(); i++) {
            EncounterDefinition.load(definitionTags.getCompound(i)).ifPresent(definition -> {
                data.definitions.put(definition.encounterId(), definition);
                data.index(definition);
            });
        }
        ListTag runtimeTags = tag.getList("Runtime", Tag.TAG_COMPOUND);
        int skipped = 0;
        for (int i = 0; i < runtimeTags.size(); i++) {
            CompoundTag entry = runtimeTags.getCompound(i);
            ResourceLocation encounterId = ResourceLocation.tryParse(entry.getString("Encounter"));
            EncounterDefinition definition = encounterId == null ? null : data.definitions.get(encounterId);
            if (!entry.hasUUID("Phase") || definition == null) { skipped++; continue; }
            data.runtime.put(new PhaseEncounterKey(new PhaseId(entry.getUUID("Phase")), encounterId),
                    PhaseEncounterState.load(entry, definition.type()));
        }
        ListTag generations = tag.getList("PhaseGenerations", Tag.TAG_COMPOUND);
        for (int i = 0; i < generations.size(); i++) {
            CompoundTag entry = generations.getCompound(i);
            if (entry.hasUUID("Phase")) data.phaseGenerations.put(new PhaseId(entry.getUUID("Phase")),
                    Math.max(0L, entry.getLong("Generation")));
        }
        ListTag gates = tag.getList("FogGates", Tag.TAG_COMPOUND);
        for (int i = 0; i < gates.size(); i++) FogGateDefinition.load(gates.getCompound(i)).ifPresent(gate -> {
            if (data.definitions.containsKey(gate.bossEncounterId())) {
                data.fogGates.put(gate.bossEncounterId(), gate);
                data.indexFogGate(gate);
            }
        });
        if (skipped > 0) MaplesAdventure.LOGGER.warn("Skipped {} invalid encounter runtime record(s)", skipped);
        return data;
    }

    private void index(EncounterDefinition definition) {
        int minX = ((int) Math.floor(definition.anchor().x - definition.activationRadius())) >> 4;
        int maxX = ((int) Math.floor(definition.anchor().x + definition.activationRadius())) >> 4;
        int minZ = ((int) Math.floor(definition.anchor().z - definition.activationRadius())) >> 4;
        int maxZ = ((int) Math.floor(definition.anchor().z + definition.activationRadius())) >> 4;
        Map<Long, Set<ResourceLocation>> chunks = spatialIndex.computeIfAbsent(definition.dimension(), ignored -> new HashMap<>());
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++)
            chunks.computeIfAbsent(ChunkPos.asLong(x, z), ignored -> new HashSet<>()).add(definition.encounterId());
    }

    private void deindex(EncounterDefinition definition) {
        Map<Long, Set<ResourceLocation>> chunks = spatialIndex.get(definition.dimension());
        if (chunks == null) return;
        chunks.values().removeIf(ids -> { ids.remove(definition.encounterId()); return ids.isEmpty(); });
        if (chunks.isEmpty()) spatialIndex.remove(definition.dimension());
    }

    private void indexFogGate(FogGateDefinition gate) {
        Map<Long, ResourceLocation> index = fogGateBlockIndex.computeIfAbsent(gate.dimension(), ignored -> new HashMap<>());
        for (long block : gate.fogBlocks()) index.put(block, gate.bossEncounterId());
    }

    private void deindexFogGate(FogGateDefinition gate) {
        Map<Long, ResourceLocation> index = fogGateBlockIndex.get(gate.dimension());
        if (index == null) return;
        for (long block : gate.fogBlocks()) index.remove(block, gate.bossEncounterId());
        if (index.isEmpty()) fogGateBlockIndex.remove(gate.dimension());
    }
}
