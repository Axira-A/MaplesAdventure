package dev.maplesadventure.progression.encumbrance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
/** Same Epic Fight WEIGHT attribute as EncumbranceRuntimeService. No second load formula. */
public final class EquipmentWeightService {
    public static double weaponWeight(ItemStack stack) {
        var weight=BuiltInRegistries.ATTRIBUTE.get(ResourceLocation.fromNamespaceAndPath("epicfight","weight"));
        if(weight==null) return 0;
        double base=weight.getDefaultValue(); double[] operands={base,1,1};
        stack.forEachModifier(EquipmentSlot.MAINHAND,(attribute,modifier)-> {
            if(attribute.value()!=weight) return;
            switch(modifier.operation()) {
                case ADD_VALUE -> operands[0]+=modifier.amount();
                case ADD_MULTIPLIED_BASE -> operands[1]+=modifier.amount();
                case ADD_MULTIPLIED_TOTAL -> operands[2]*=1+modifier.amount();
            }
        });
        return Math.max(0,operands[0]*operands[1]*operands[2]-base);
    }
    private EquipmentWeightService() {}
}
