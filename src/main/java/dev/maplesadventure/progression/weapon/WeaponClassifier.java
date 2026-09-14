package dev.maplesadventure.progression.weapon;
import dev.maplesadventure.progression.encumbrance.EquipmentWeightService;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.component.DataComponents;
import java.util.*;
public final class WeaponClassifier {
    /** Stack-specific baseline for infrequent UI/snapshot work; never called in the damage hot path. */
    public static double baseAttack(ItemStack stack) {
        double[] value={1,1,1};
        stack.forEachModifier(EquipmentSlot.MAINHAND,(attribute,modifier)->{
            if (!attribute.equals(Attributes.ATTACK_DAMAGE)) return;
            switch(modifier.operation()) {
                case ADD_VALUE -> value[0]+=modifier.amount();
                case ADD_MULTIPLIED_BASE -> value[1]+=modifier.amount();
                case ADD_MULTIPLIED_TOTAL -> value[2]*=1+modifier.amount();
            }
        });
        double result=value[0]*value[1]*value[2];
        return Double.isFinite(result)?Math.clamp(result,0,10000):0;
    }
    public static WeaponFacts inspect(ItemStack stack) {
        var item=stack.getItem(); double[] damage={1,1,1},speed={4,1,1}; boolean[] found={false,false};
        stack.forEachModifier(EquipmentSlot.MAINHAND,(holder,m)-> {
            double[] values;
            if(holder.equals(Attributes.ATTACK_DAMAGE)) { values=damage; found[0]=true; }
            else if(holder.equals(Attributes.ATTACK_SPEED)) { values=speed; found[1]=true; } else return;
            switch(m.operation()) { case ADD_VALUE -> values[0]+=m.amount(); case ADD_MULTIPLIED_BASE -> values[1]+=m.amount(); case ADD_MULTIPLIED_TOTAL -> values[2]*=1+m.amount(); }
        });
        double dmg=Math.clamp(damage[0]*damage[1]*damage[2],0,10000),spd=Math.clamp(speed[0]*speed[1]*speed[2],0,100);
        var kind=WeaponRequirementArchetype.NONE; String source="NONE",category="NONE";
        if(!(item instanceof ShieldItem) && !(item instanceof ArmorItem) && !(item instanceof BlockItem) && !stack.has(DataComponents.FOOD)) {
            if(WeaponIntegrations.epic!=null) {
                String value=WeaponIntegrations.epic.category(stack);
                if(!value.isEmpty() && !value.equals("NOT_WEAPON") && !value.equals("SHIELD") && !value.equals("RANGED")) {
                    category=value; kind=WeaponRequirementArchetype.category(value); source="EPIC_FIGHT";
                }
            }
            if(kind==WeaponRequirementArchetype.NONE) {
                if(item instanceof BowItem) kind=WeaponRequirementArchetype.BOW;
                else if(item instanceof CrossbowItem) kind=WeaponRequirementArchetype.CROSSBOW;
                else if(item instanceof SwordItem) kind=WeaponRequirementArchetype.SWORD;
                else if(item instanceof AxeItem) kind=WeaponRequirementArchetype.AXE;
                else if(item instanceof TridentItem) kind=WeaponRequirementArchetype.TRIDENT;
                else if(item instanceof MaceItem) kind=WeaponRequirementArchetype.MACE;
                else if(item instanceof DiggerItem) kind=WeaponRequirementArchetype.TOOL;
                else if(item instanceof ProjectileWeaponItem) kind=WeaponRequirementArchetype.BOW;
                if(kind!=WeaponRequirementArchetype.NONE) source="VANILLA";
                else if(found[0] && found[1] && dmg>=4 && spd>0 && spd<=6 && stack.isDamageableItem()) { kind=WeaponRequirementArchetype.GENERIC; source="GENERIC"; }
                category=kind.name();
            }
        }
        return new WeaponFacts(kind,source,category,EquipmentWeightService.weaponWeight(stack),dmg,spd,
                kind!=WeaponRequirementArchetype.NONE && WeaponIntegrations.irons!=null?WeaponIntegrations.irons.schools(stack):Map.of());
    }
    private WeaponClassifier() {}
}
