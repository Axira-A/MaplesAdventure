package dev.maplesadventure.progression.network;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.progression.Attribute;
import dev.maplesadventure.progression.AttributeSnapshot;
import dev.maplesadventure.progression.PlayerAttributeMigration;
import dev.maplesadventure.progression.PlayerAttributeState;
import io.netty.handler.codec.DecoderException;
import java.util.EnumMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import dev.maplesadventure.progression.runtime.RuntimeResourceSnapshot;
import dev.maplesadventure.progression.runtime.RuntimeResourceValue;
import dev.maplesadventure.progression.stats.StatImplementationState;
import dev.maplesadventure.progression.encumbrance.DodgeMode;
import dev.maplesadventure.progression.encumbrance.EquipLoadRuntimeSnapshot;
import dev.maplesadventure.progression.encumbrance.EquipLoadTier;
import dev.maplesadventure.progression.encumbrance.EncumbrancePolicySnapshot;
import dev.maplesadventure.progression.encumbrance.EncumbranceProfile;

public final class AttributePayloads {
    public record Snapshot(AttributeSnapshot snapshot) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = AttributePayloads.type("attribute_snapshot");
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> STREAM_CODEC = codec(buffer -> {
            int version = buffer.readVarInt();
            EnumMap<Attribute, Integer> values = new EnumMap<>(Attribute.class);
            for (Attribute attribute : Attribute.values()) {
                int value = buffer.readVarInt();
                if (value < PlayerAttributeMigration.MINIMUM_VALUE
                        || value > PlayerAttributeMigration.ABSOLUTE_HARD_CAP)
                    throw new DecoderException("Attribute snapshot value outside schema bounds");
                values.put(attribute, value);
            }
            PlayerAttributeState state = PlayerAttributeState.fromValues(version, values);
            int level = buffer.readVarInt();
            double health = buffer.readDouble();
            double mana = buffer.readDouble();
            double stamina = buffer.readDouble();
            long cost = buffer.readVarLong();
            RuntimeResourceSnapshot runtime = new RuntimeResourceSnapshot(
                    readRuntimeResource(buffer), readRuntimeResource(buffer), readRuntimeResource(buffer));
            double currentLoad = buffer.readDouble();
            boolean loadAvailable = buffer.readBoolean();
            EquipLoadTier tier = readEnum(buffer, EquipLoadTier.values(), "equipment-load tier");
            DodgeMode dodge = readEnum(buffer, DodgeMode.values(), "dodge mode");
            EncumbrancePolicySnapshot policy = readEncumbrancePolicy(buffer);
            EquipLoadRuntimeSnapshot equipLoad;
            try { equipLoad = new EquipLoadRuntimeSnapshot(currentLoad, loadAvailable, tier, dodge, policy); }
            catch (IllegalArgumentException invalid) { throw new DecoderException("Invalid equipment load", invalid); }
            if (level < 5 || cost < 0L || !Double.isFinite(health) || !Double.isFinite(mana)
                    || !Double.isFinite(stamina)) throw new DecoderException("Invalid attribute snapshot");
            return new Snapshot(new AttributeSnapshot(state, level, health, mana, stamina, cost, runtime, equipLoad,
                    dev.maplesadventure.progression.spell.SpellSchoolSnapshotCodec.read(buffer),
                    dev.maplesadventure.progression.weapon.WeaponLoadoutSnapshot.read(buffer)));
        }, (buffer, payload) -> {
            AttributeSnapshot snapshot = payload.snapshot;
            buffer.writeVarInt(snapshot.state().dataVersion());
            for (Attribute attribute : Attribute.values()) buffer.writeVarInt(snapshot.state().get(attribute));
            buffer.writeVarInt(snapshot.level());
            buffer.writeDouble(snapshot.maxHealth());
            buffer.writeDouble(snapshot.mana());
            buffer.writeDouble(snapshot.stamina());
            buffer.writeVarLong(snapshot.nextLevelCost());
            writeRuntimeResource(buffer, snapshot.runtimeResources().health());
            writeRuntimeResource(buffer, snapshot.runtimeResources().mana());
            writeRuntimeResource(buffer, snapshot.runtimeResources().stamina());
            buffer.writeDouble(snapshot.equipLoad().currentLoad());
            buffer.writeBoolean(snapshot.equipLoad().available());
            buffer.writeVarInt(snapshot.equipLoad().tier().ordinal());
            buffer.writeVarInt(snapshot.equipLoad().currentDodgeMode().ordinal());
            writeEncumbrancePolicy(buffer, snapshot.equipLoad().policy());
            dev.maplesadventure.progression.spell.SpellSchoolSnapshotCodec.write(buffer, snapshot.spellSchools());
            snapshot.weapons().write(buffer);
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record RequestSnapshot() implements CustomPacketPayload {
        public static final Type<RequestSnapshot> TYPE = AttributePayloads.type("attribute_snapshot_request");
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestSnapshot> STREAM_CODEC = codec(
                buffer -> new RequestSnapshot(), (buffer, payload) -> {});
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> codec(
            java.util.function.Function<RegistryFriendlyByteBuf, T> decoder,
            java.util.function.BiConsumer<RegistryFriendlyByteBuf, T> encoder) {
        return new StreamCodec<>() {
            @Override public T decode(RegistryFriendlyByteBuf buffer) { return decoder.apply(buffer); }
            @Override public void encode(RegistryFriendlyByteBuf buffer, T value) { encoder.accept(buffer, value); }
        };
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, path));
    }

    private static RuntimeResourceValue readRuntimeResource(RegistryFriendlyByteBuf buffer) {
        double formula = buffer.readDouble();
        double runtime = buffer.readDouble();
        double scale = buffer.readDouble();
        int ordinal = buffer.readVarInt();
        StatImplementationState[] states = StatImplementationState.values();
        if (ordinal < 0 || ordinal >= states.length) throw new DecoderException("Unknown resource state");
        try {
            return new RuntimeResourceValue(formula, runtime, scale, states[ordinal]);
        } catch (IllegalArgumentException invalid) {
            throw new DecoderException("Invalid runtime resource", invalid);
        }
    }

    private static void writeRuntimeResource(RegistryFriendlyByteBuf buffer, RuntimeResourceValue value) {
        buffer.writeDouble(value.formulaValue());
        buffer.writeDouble(value.runtimeValue());
        buffer.writeDouble(value.addValueScale());
        buffer.writeVarInt(value.implementation().ordinal());
    }
    private static <E> E readEnum(RegistryFriendlyByteBuf buffer, E[] values, String description) {
        int ordinal = buffer.readVarInt();
        if (ordinal < 0 || ordinal >= values.length) throw new DecoderException("Unknown " + description);
        return values[ordinal];
    }

    private static EncumbrancePolicySnapshot readEncumbrancePolicy(RegistryFriendlyByteBuf buffer) {
        try {
            return new EncumbrancePolicySnapshot(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                    readProfile(buffer), readProfile(buffer), readProfile(buffer), readProfile(buffer));
        } catch (IllegalArgumentException invalid) {
            throw new DecoderException("Invalid encumbrance policy", invalid);
        }
    }

    private static EncumbranceProfile readProfile(RegistryFriendlyByteBuf buffer) {
        return new EncumbranceProfile(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                readEnum(buffer, DodgeMode.values(), "profile dodge mode"), buffer.readDouble(),
                buffer.readBoolean(), buffer.readBoolean());
    }

    private static void writeEncumbrancePolicy(RegistryFriendlyByteBuf buffer, EncumbrancePolicySnapshot policy) {
        buffer.writeDouble(policy.lightThreshold());
        buffer.writeDouble(policy.heavyThreshold());
        buffer.writeDouble(policy.overloadedThreshold());
        writeProfile(buffer, policy.light());
        writeProfile(buffer, policy.normal());
        writeProfile(buffer, policy.heavy());
        writeProfile(buffer, policy.overloaded());
    }

    private static void writeProfile(RegistryFriendlyByteBuf buffer, EncumbranceProfile profile) {
        buffer.writeDouble(profile.movementMultiplier());
        buffer.writeDouble(profile.staminaRegenMultiplier());
        buffer.writeDouble(profile.staminaCostMultiplier());
        buffer.writeVarInt(profile.dodgeMode().ordinal());
        buffer.writeDouble(profile.dodgeDistanceMultiplier());
        buffer.writeBoolean(profile.canSprint());
        buffer.writeBoolean(profile.canDodge());
    }
    private AttributePayloads() {}
}
