package dev.maplesadventure.progression.spell;

import io.netty.handler.codec.DecoderException;
import java.util.LinkedHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

/** Bounded server-to-local-player preview context, not a client write protocol. */
public final class SpellSchoolSnapshotCodec {
    public static SpellSchoolScalingSnapshot read(RegistryFriendlyByteBuf b) {
        int size = b.readVarInt();
        if (size < 0 || size > SpellSchoolScalingRegistry.MAX_SCHOOLS) throw new DecoderException("School count out of bounds");
        var values = new LinkedHashMap<ResourceLocation, SpellSchoolStat>();
        try {
            for (int i = 0; i < size; i++) {
                var id = b.readResourceLocation();
                var profile = new SpellSchoolScalingProfile(id, b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble());
                var name = ComponentSerialization.STREAM_CODEC.decode(b);
                double bonus = b.readDouble();
                var ctx = new SpellPowerContext(b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(),
                        b.readDouble(), b.readDouble(), b.readBoolean());
                if (values.put(id, new SpellSchoolStat(profile, name, bonus, ctx, b.readBoolean())) != null)
                    throw new IllegalArgumentException("Duplicate school ID");
            }
            return new SpellSchoolScalingSnapshot(values);
        } catch (IllegalArgumentException invalid) { throw new DecoderException("Invalid school snapshot", invalid); }
    }
    public static void write(RegistryFriendlyByteBuf b, SpellSchoolScalingSnapshot snapshot) {
        b.writeVarInt(snapshot.schools().size());
        for (var s : snapshot.schools().values()) {
            var p = s.profile(); var c = s.context();
            b.writeResourceLocation(s.schoolId());
            b.writeDouble(p.intelligenceWeight()); b.writeDouble(p.faithWeight()); b.writeDouble(p.arcaneWeight());
            b.writeDouble(p.maxProgressionBonus());
            ComponentSerialization.STREAM_CODEC.encode(b, s.displayName());
            b.writeDouble(s.progressionBonus());
            b.writeDouble(c.additiveBase()); b.writeDouble(c.multiplication()); b.writeDouble(c.minimum()); b.writeDouble(c.maximum());
            b.writeDouble(c.globalPower()); b.writeDouble(c.observedSchoolPower()); b.writeBoolean(c.available());
            b.writeBoolean(s.modifierPresent());
        }
    }
    private SpellSchoolSnapshotCodec() {}
}
