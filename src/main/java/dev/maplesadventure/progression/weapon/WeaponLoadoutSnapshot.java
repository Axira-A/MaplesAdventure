package dev.maplesadventure.progression.weapon;
import dev.maplesadventure.progression.PlayerAttributeState;
import dev.maplesadventure.config.WeaponRequirementConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import java.util.*;
/** Only canonical item IDs/profiles, no inventory or ItemStack NBT. */
public record WeaponLoadoutSnapshot(Held mainHand,Held offHand,double penalty) {
    /** Read-only equipment context, derived from the existing wire fields, not implementation state. */
    public enum HandState { EMPTY, NON_WEAPON, WEAPON }
    public record Held(ResourceLocation item,WeaponRequirementProfile profile,WeaponScalingProfile scaling,double baseAttack,boolean weapon,
                       WeaponDamageProfile damage,WeaponInfusionView infusion,dev.maplesadventure.progression.status.WeaponStatusProfile statuses) {
        public Held(ResourceLocation item,WeaponRequirementProfile profile,WeaponScalingProfile scaling,double baseAttack,boolean weapon,WeaponDamageProfile damage,WeaponInfusionView infusion) { this(item,profile,scaling,baseAttack,weapon,damage,infusion,dev.maplesadventure.progression.status.WeaponStatusProfile.EMPTY); }
        public Held(ResourceLocation item,WeaponRequirementProfile profile,WeaponScalingProfile scaling,double baseAttack,boolean weapon,WeaponDamageProfile damage) { this(item,profile,scaling,baseAttack,weapon,damage,WeaponInfusionView.normal()); }
        public Held(ResourceLocation item,WeaponRequirementProfile profile,WeaponScalingProfile scaling,double baseAttack,boolean weapon) { this(item,profile,scaling,baseAttack,weapon,WeaponDamageProfile.STANDARD); }
        public Held(ResourceLocation item,WeaponRequirementProfile profile) { this(item,profile,WeaponScalingProfile.NONE,0,false); }
        public Held {
            if(!Double.isFinite(baseAttack)||baseAttack<0||baseAttack>10000) throw new IllegalArgumentException("Weapon base bounds");
        }
        public Component name() { var type=BuiltInRegistries.ITEM.get(item); return type==null?Component.empty():type.getDescription(); }
        public HandState handState() {
            if (ResourceLocation.withDefaultNamespace("air").equals(item)) return HandState.EMPTY;
            return weapon ? HandState.WEAPON : HandState.NON_WEAPON;
        }
    }
    public record View(boolean offhand,Held held,PlayerAttributeState attributes,WeaponRequirementResult result) {
        public dev.maplesadventure.progression.status.StatusBuildupSnapshot statuses() { return held.statuses().evaluate(attributes.get(dev.maplesadventure.progression.Attribute.ARCANE),held.scaling().arcane(),1); }
        public WeaponAttackSnapshot attack() {
            return WeaponAttackSnapshot.of(held.item(),WeaponAttackRatingCalculator.calculate(held.baseAttack(),held.scaling(),held.damage(),attributes,result.damageMultiplier()),held.scaling(),held.damage(),result);
        }
    }
    public WeaponLoadoutSnapshot { if(!Double.isFinite(penalty)||penalty<.1||penalty>1) throw new IllegalArgumentException("Weapon penalty range"); }
    public static WeaponLoadoutSnapshot empty() {
        var none=new Held(ResourceLocation.withDefaultNamespace("air"),WeaponRequirementProfile.NONE); return new WeaponLoadoutSnapshot(none,none,1);
    }
    public static WeaponLoadoutSnapshot of(ServerPlayer player) { return new WeaponLoadoutSnapshot(held(player.getMainHandItem()),held(player.getOffhandItem()),WeaponRequirementConfig.UNMET_MULTIPLIER.get()); }
    private static Held held(ItemStack stack) {
        var resolved=WeaponCombatProfileResolver.resolve(stack); boolean weapon=resolved.weapon();
        return new Held(BuiltInRegistries.ITEM.getKey(stack.getItem()),resolved.requirements(),resolved.scaling(),
                weapon?WeaponClassifier.baseAttack(stack):0,weapon,resolved.damage(),resolved.infusion(),resolved.statuses());
    }
    public List<View> evaluate(PlayerAttributeState attributes) {
        List<View> views=new ArrayList<>();
        if(mainHand.weapon()||mainHand.profile().enabled()||!mainHand.statuses().components().isEmpty()) views.add(new View(false,mainHand,attributes.cleanCopy(),WeaponRequirementService.evaluate(attributes,mainHand.profile(),penalty)));
        if(offHand.weapon()||offHand.profile().enabled()||!offHand.statuses().components().isEmpty()) views.add(new View(true,offHand,attributes.cleanCopy(),WeaponRequirementService.evaluate(attributes,offHand.profile(),penalty)));
        return List.copyOf(views);
    }
    public static WeaponLoadoutSnapshot read(RegistryFriendlyByteBuf b) {
        var main=readHeld(b); var off=readHeld(b);
        return new WeaponLoadoutSnapshot(main,off,b.readDouble());
    }
    public void write(RegistryFriendlyByteBuf b) {
        for(var held:List.of(mainHand,offHand)) writeHeld(b,held);
        b.writeDouble(penalty);
    }
    public static Held readHeld(RegistryFriendlyByteBuf b) {
        return new Held(ResourceLocation.parse(b.readUtf(256)),WeaponRequirementNetwork.readProfile(b),
                WeaponRequirementNetwork.readScaling(b),b.readDouble(),b.readBoolean(),WeaponDamageProfile.read(b),WeaponInfusionView.read(b),dev.maplesadventure.progression.status.WeaponStatusProfile.read(b));
    }
    public static void writeHeld(RegistryFriendlyByteBuf b,Held held) {
        b.writeUtf(held.item().toString(),256); WeaponRequirementNetwork.writeProfile(b,held.profile());
        WeaponRequirementNetwork.writeScaling(b,held.scaling()); b.writeDouble(held.baseAttack()); b.writeBoolean(held.weapon());
        held.damage().write(b); held.infusion().write(b); held.statuses().write(b);
    }
}
