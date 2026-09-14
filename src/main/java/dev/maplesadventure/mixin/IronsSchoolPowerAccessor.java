package dev.maplesadventure.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

/** SchoolType 3.16.3 exposes getPowerFor, but no public power-attribute holder getter. */
@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.api.spells.SchoolType", remap = false)
public interface IronsSchoolPowerAccessor {
    @Accessor("powerAttribute") Holder<Attribute> maplesadventure$powerAttribute();
}
