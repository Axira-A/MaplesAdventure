package dev.maplesadventure.progression.weapon;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

public record WeaponInfusionEligibility(Set<ResourceLocation> allowed) {
    public static final int MAX_ALLOWED = 32;
    public WeaponInfusionEligibility {
        allowed = Set.copyOf(allowed);
        if (allowed.isEmpty() || allowed.size() > MAX_ALLOWED || allowed.stream().anyMatch(id -> id.toString().length() > 256))
            throw new IllegalArgumentException("Infusion eligibility bounds");
    }
    public boolean allows(ResourceLocation id) { return id.equals(WeaponInfusionRegistry.NORMAL_ID) || allowed.contains(id); }
    public static boolean simplePhysical(WeaponDamageProfile damage) {
        return damage.components().size() == 1 && damage.components().getFirst().channel().physical();
    }
    public void write(RegistryFriendlyByteBuf buffer) {
        var sorted = allowed.stream().sorted().toList(); buffer.writeVarInt(sorted.size());
        for (var id : sorted) buffer.writeUtf(id.toString(), 256);
    }
    public static WeaponInfusionEligibility read(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt(); if (count < 1 || count > MAX_ALLOWED) throw new IllegalArgumentException("Infusion eligibility count");
        Set<ResourceLocation> result = new HashSet<>();
        for (int index = 0; index < count; index++) if (!result.add(ResourceLocation.parse(buffer.readUtf(256))))
            throw new IllegalArgumentException("Duplicate infusion eligibility");
        return new WeaponInfusionEligibility(result);
    }
}
