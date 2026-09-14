package dev.maplesadventure.progression.weapon;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
/** Frozen combat decision. Keeps the Round 6 attachment ID and legacy multiplier field readable. */
public final class ProjectileRequirementPenalty implements INBTSerializable<CompoundTag> {
    public UUID owner=new UUID(0,0);
    public boolean qualified=true;
    public double multiplier=1;
    public double scalingMultiplier=1;
    private WeaponDamageBundle frozenBundle;
    private net.minecraft.resources.ResourceLocation weaponId=net.minecraft.resources.ResourceLocation.withDefaultNamespace("air");
    public ProjectileRequirementPenalty() {}
    public ProjectileRequirementPenalty(UUID owner,WeaponRequirementResult result) { this.owner=owner; qualified=result.satisfied(); multiplier=result.damageMultiplier(); }
    public ProjectileRequirementPenalty(UUID owner,WeaponRequirementResult result,double scaling) {
        this(owner,result);
        if (!Double.isFinite(scaling)||scaling<1||scaling>3) throw new IllegalArgumentException("Projectile scaling bounds");
        scalingMultiplier=scaling;
    }
    public ProjectileRequirementPenalty(WeaponHitContext context) {
        owner=context.owner(); qualified=context.requirements().satisfied(); multiplier=context.bundle().requirementMultiplier();
        scalingMultiplier=context.bundle().nominalMultiplier(); frozenBundle=context.bundle(); weaponId=context.usedWeapon();
    }
    public WeaponDamageBundle bundle() { return frozenBundle!=null?frozenBundle:WeaponDamageBundle.legacy(scalingMultiplier,multiplier); }
    public WeaponHitContext context() {
        return new WeaponHitContext(weaponId,bundle(),new WeaponRequirementResult(qualified,java.util.Map.of(),multiplier,qualified),true,owner);
    }
    public double combatMultiplier() { return bundle().effectiveMultiplier(); }
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var n=new CompoundTag(); n.putInt("dataVersion",3); n.putUUID("owner",owner); n.putBoolean("qualified",qualified);
        n.putDouble("multiplier",multiplier); n.putDouble("scalingMultiplier",scalingMultiplier);
        var bundle=bundle(); n.putString("weapon",weaponId.toString()); n.putDouble("weaponBase",bundle.weaponBaseAttack());
        var channels=new net.minecraft.nbt.ListTag();
        bundle.channels().forEach((channel,ar)->{
            var c=new CompoundTag(); c.putString("channel",channel.id()); c.putDouble("base",ar.base()); c.putDouble("bonus",ar.scalingBonus());
            channels.add(c);
        });
        n.put("channels",channels); return n;
    }
    public void deserializeNBT(HolderLookup.Provider provider,CompoundTag n) {
        owner=n.hasUUID("owner")?n.getUUID("owner"):new UUID(0,0); qualified=n.getBoolean("qualified");
        multiplier=n.getDouble("multiplier"); if(!Double.isFinite(multiplier)||multiplier<.1||multiplier>1) multiplier=1;
        scalingMultiplier=n.contains("scalingMultiplier")?n.getDouble("scalingMultiplier"):1;
        frozenBundle=null; weaponId=net.minecraft.resources.ResourceLocation.withDefaultNamespace("air");
        boolean modern=n.getInt("dataVersion")>=3;
        if(!Double.isFinite(scalingMultiplier)||scalingMultiplier<(modern?0:1)||scalingMultiplier>(modern?6:3)) scalingMultiplier=1;
        if(modern) {
            try {
                var channels=n.getList("channels",net.minecraft.nbt.Tag.TAG_COMPOUND);
                if(channels.isEmpty()||channels.size()>8) throw new IllegalArgumentException("Snapshot channels");
                var values=new java.util.EnumMap<WeaponDamageChannel,WeaponDamageBundle.ChannelAttack>(WeaponDamageChannel.class);
                for(int i=0;i<channels.size();i++) {
                    var c=channels.getCompound(i);
                    if(values.put(WeaponDamageChannel.parse(c.getString("channel")),new WeaponDamageBundle.ChannelAttack(c.getDouble("base"),c.getDouble("bonus")))!=null)
                        throw new IllegalArgumentException("Duplicate snapshot channel");
                }
                frozenBundle=new WeaponDamageBundle(values,n.getDouble("weaponBase"),scalingMultiplier,multiplier);
                if(n.getString("weapon").length()>256) throw new IllegalArgumentException("Weapon ID bounds");
                weaponId=net.minecraft.resources.ResourceLocation.parse(n.getString("weapon"));
            } catch(RuntimeException invalid) {
                // A corrupt new bundle must not take the server down or reconstruct live owner state.
                frozenBundle=null; scalingMultiplier=1;
                dev.maplesadventure.MaplesAdventure.LOGGER.warn("Invalid projectile weapon bundle; using safe physical fallback: {}",invalid.getMessage());
            }
        }
    }
}
