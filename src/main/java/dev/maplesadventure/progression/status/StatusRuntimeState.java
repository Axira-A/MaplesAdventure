package dev.maplesadventure.progression.status;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Life-state only. No copyOnDeath, no entity/ItemStack references or visual state. */
public final class StatusRuntimeState implements INBTSerializable<CompoundTag> {
    public static final int VERSION=1;
    private final EnumMap<StatusEffectType, Entry> entries = new EnumMap<>(StatusEffectType.class);
    private long revision;
    public static final class Entry {
        public double current;
        public long lastBuildup, lastUpdate, activeStart, activeEnd, nextDot, procSerial;
        public StatusSourceContext source;
        public boolean active(long now) { return activeEnd > now; }
        public boolean hasDuration() { return activeEnd > activeStart; }
        public void decay(long now, StatusEffectDefinition d) {
            long start=Math.max(lastUpdate,lastBuildup+d.decayDelay());
            if (!hasDuration() && now>start) current=Math.max(0,current-d.decayPerSecond()*(now-start)/20.0);
            lastUpdate=now;
        }
    }
    public Map<StatusEffectType,Entry> entries() { return Collections.unmodifiableMap(entries); }
    public Entry get(StatusEffectType type) { return entries.get(type); }
    Entry ensure(StatusEffectType type) { return entries.computeIfAbsent(type, t->new Entry()); }
    public boolean empty() { return entries.isEmpty(); }
    public long revision() { return revision; }
    void changed() { revision=revision==Long.MAX_VALUE?1:revision+1; }
    void remove(StatusEffectType type) { entries.remove(type); changed(); }
    void clear() { entries.clear(); changed(); }
    /** Returns true once; caller executes the proc. Overflow is deliberately discarded. */
    public boolean accumulate(StatusEffectType type, double amount, StatusResistance resistance, StatusSourceContext source, long now) {
        StatusResistance.bounded(amount,0,100000);
        if (resistance.immune() || amount==0) return false;
        var e=ensure(type);
        if (e.active(now)) return false;
        if (e.hasDuration()) { e.activeStart=0; e.activeEnd=0; }
        e.decay(now,StatusDefinitions.get(type)); e.source=source; e.lastBuildup=now; e.lastUpdate=now;
        e.current+=amount; changed();
        if (e.current>=resistance.threshold()) { e.current=0; e.procSerial++; return true; }
        return false;
    }
    @Override public CompoundTag serializeNBT(HolderLookup.Provider lookup) {
        var n=new CompoundTag(); n.putInt("dataVersion",VERSION); n.putLong("revision",revision);
        var list=new ListTag();
        entries.forEach((type,e)->{
            var x=new CompoundTag(); x.putString("type",type.id()); x.putDouble("current",e.current);
            x.putLong("lastBuildup",e.lastBuildup); x.putLong("lastUpdate",e.lastUpdate);
            x.putLong("activeStart",e.activeStart); x.putLong("activeEnd",e.activeEnd); x.putLong("nextDot",e.nextDot); x.putLong("procSerial",e.procSerial);
            if(e.source!=null) {
                if(e.source.attackerUUID()!=null) x.putUUID("source",e.source.attackerUUID());
                x.putString("sourceKind",e.source.sourceKind().name()); x.putString("weapon",e.source.weaponItemId().toString()); x.putBoolean("projectile",e.source.projectile());
            }
            list.add(x);
        });
        n.put("entries",list); return n;
    }
    @Override public void deserializeNBT(HolderLookup.Provider lookup, CompoundTag n) {
        entries.clear(); revision=Math.max(0,n.getLong("revision"));
        if(n.getInt("dataVersion")!=VERSION) return;
        var list=n.getList("entries",Tag.TAG_COMPOUND);
        if(list.size()>StatusEffectType.values().length) return;
        for(int i=0;i<list.size();i++) try {
            var x=list.getCompound(i); var type=StatusEffectType.parse(x.getString("type")); var e=new Entry();
            e.current=StatusResistance.bounded(x.getDouble("current"),0,100000);
            e.lastBuildup=time(x,"lastBuildup"); e.lastUpdate=time(x,"lastUpdate");
            e.activeStart=time(x,"activeStart"); e.activeEnd=time(x,"activeEnd"); e.nextDot=time(x,"nextDot"); e.procSerial=time(x,"procSerial");
            if(e.activeEnd<e.activeStart||e.activeEnd-e.activeStart>72000) throw new IllegalArgumentException("Status duration");
            var kind=x.contains("sourceKind")?StatusSourceContext.SourceKind.valueOf(x.getString("sourceKind")):StatusSourceContext.SourceKind.ENVIRONMENT;
            String weapon=x.contains("weapon")?x.getString("weapon"):"minecraft:air";
            if(weapon.length()>256) throw new IllegalArgumentException("Weapon ID bounds");
            e.source=new StatusSourceContext(x.hasUUID("source")?x.getUUID("source"):null,kind,ResourceLocation.parse(weapon),x.getBoolean("projectile"),type);
            entries.put(type,e);
        } catch(RuntimeException error) { dev.maplesadventure.MaplesAdventure.LOGGER.warn("Discarded invalid status entry: {}",error.getMessage()); }
    }
    private static long time(CompoundTag n,String key) { long value=n.getLong(key); if(value<0) throw new IllegalArgumentException("Status time"); return value; }
}
