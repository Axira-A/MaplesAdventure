package dev.maplesadventure.progression.weapon;
import java.util.*;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import dev.maplesadventure.config.WeaponRequirementConfig;
public final class WeaponRequirementNetwork {
    // Two entries leave room for maximum metadata, per-item eligibility and the bounded definition snapshot.
    public static final int BATCH_SIZE=2, MAX_BYTES=131072, MAX_BATCHES=16384;
    public record Entry(ResourceLocation item,WeaponRequirementProfile profile,WeaponScalingProfile scaling,double baseAttack,boolean weapon,
                        WeaponDamageProfile damage,WeaponInfusionEligibility infusionEligibility) {
        public Entry(ResourceLocation item,WeaponRequirementProfile profile,WeaponScalingProfile scaling,double baseAttack,boolean weapon,WeaponDamageProfile damage) { this(item,profile,scaling,baseAttack,weapon,damage,new WeaponInfusionEligibility(Set.of(WeaponInfusionRegistry.NORMAL_ID))); }
        public Entry(ResourceLocation item,WeaponRequirementProfile profile,WeaponScalingProfile scaling,double baseAttack,boolean weapon) { this(item,profile,scaling,baseAttack,weapon,WeaponDamageProfile.STANDARD); }
        public Entry(ResourceLocation item,WeaponRequirementProfile profile) { this(item,profile,WeaponScalingProfile.NONE,0,false); }
        public Entry { if(!Double.isFinite(baseAttack)||baseAttack<0||baseAttack>10000) throw new IllegalArgumentException("Weapon base bounds"); }
        public WeaponLoadoutSnapshot.Held held() { return new WeaponLoadoutSnapshot.Held(item,profile,scaling,baseAttack,weapon,damage); }
    }
    public record Batch(UUID revision,int index,int batches,double penalty,List<Entry> entries,List<WeaponInfusionDefinition> infusions) implements CustomPacketPayload {
        public static final Type<Batch> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("maplesadventure","weapon_requirements"));
        public Batch {
            entries=List.copyOf(entries); infusions=List.copyOf(infusions);
            if(index<0||batches<1||batches>MAX_BATCHES||index>=batches||entries.size()>BATCH_SIZE||!Double.isFinite(penalty)||penalty<.1||penalty>1) throw new IllegalArgumentException("Bad weapon registry batch");
            if((index==0&&(infusions.isEmpty()||infusions.size()>WeaponInfusionDefinition.MAX_DEFINITIONS))||(index>0&&!infusions.isEmpty())) throw new IllegalArgumentException("Bad infusion registry batch");
        }
        public Batch(UUID revision,int index,int batches,double penalty,List<Entry> entries) { this(revision,index,batches,penalty,entries,index==0?WeaponInfusionRegistry.definitions().values().stream().sorted(Comparator.comparing(WeaponInfusionDefinition::id)).toList():List.of()); }
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    private static final StreamCodec<RegistryFriendlyByteBuf,Batch> CODEC=new StreamCodec<>() {
        public Batch decode(RegistryFriendlyByteBuf b) {
            if(b.readableBytes()>MAX_BYTES) throw new DecoderException("Weapon batch exceeds byte limit");
            try {
                UUID revision=b.readUUID(); int index=b.readVarInt(), batches=b.readVarInt(); double penalty=b.readDouble();
                int count=b.readVarInt(); if(count<0||count>BATCH_SIZE) throw new IllegalArgumentException("Weapon batch count");
                List<Entry> entries=new ArrayList<>();
                for(int i=0;i<count;i++) {
                    var held=WeaponLoadoutSnapshot.readHeld(b);
                    entries.add(new Entry(held.item(),held.profile(),held.scaling(),held.baseAttack(),held.weapon(),held.damage(),WeaponInfusionEligibility.read(b)));
                }
                int infusionCount=b.readVarInt(); if(infusionCount<0||infusionCount>WeaponInfusionDefinition.MAX_DEFINITIONS) throw new IllegalArgumentException("Infusion registry count");
                List<WeaponInfusionDefinition> infusions=new ArrayList<>(); for(int i=0;i<infusionCount;i++) infusions.add(WeaponInfusionDefinition.read(b));
                return new Batch(revision,index,batches,penalty,entries,infusions);
            } catch(IllegalArgumentException e) { throw new DecoderException("Invalid weapon registry",e); }
        }
        public void encode(RegistryFriendlyByteBuf b,Batch value) {
            int start=b.writerIndex();
            b.writeUUID(value.revision()); b.writeVarInt(value.index()); b.writeVarInt(value.batches()); b.writeDouble(value.penalty()); b.writeVarInt(value.entries().size());
            for(var e:value.entries()) { WeaponLoadoutSnapshot.writeHeld(b,e.held()); e.infusionEligibility().write(b); }
            b.writeVarInt(value.infusions().size()); for(var infusion:value.infusions()) infusion.write(b);
            if(b.writerIndex()-start>MAX_BYTES) throw new IllegalArgumentException("Weapon batch too large");
        }
    };
    public static WeaponRequirementProfile readProfile(RegistryFriendlyByteBuf b) {
        return new WeaponRequirementProfile(b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readUtf(32),b.readUtf(64),b.readUtf(512));
    }
    public static void writeProfile(RegistryFriendlyByteBuf b,WeaponRequirementProfile p) {
        for(var a:WeaponRequirementProfile.ATTRIBUTES) b.writeVarInt(p.get(a));
        b.writeUtf(p.source(),32); b.writeUtf(p.weaponClass(),64); b.writeUtf(p.debugReason(),512);
    }
    public static WeaponScalingProfile readScaling(RegistryFriendlyByteBuf b) {
        return new WeaponScalingProfile(b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),
                b.readUtf(32),b.readUtf(64),b.readUtf(512));
    }
    public static void writeScaling(RegistryFriendlyByteBuf b,WeaponScalingProfile p) {
        for(var a:WeaponRequirementProfile.ATTRIBUTES) b.writeDouble(p.get(a));
        b.writeDouble(p.maxBonus()); b.writeUtf(p.source(),32); b.writeUtf(p.weaponClass(),64); b.writeUtf(p.debugReason(),512);
    }
    public static void register(PayloadRegistrar r) {
        r.playToClient(Batch.TYPE,CODEC,(payload,context)->dev.maplesadventure.progression.weapon.client.ClientWeaponRequirements.accept(payload));
    }
    public static void sync(ServerPlayer player) {
        Set<net.minecraft.world.item.Item> items=Collections.newSetFromMap(new IdentityHashMap<>());
        items.addAll(WeaponRequirementService.compiled().keySet()); items.addAll(WeaponScalingService.compiled().keySet());
        items.addAll(WeaponDamageProfileService.compiled().keySet());
        if(items.size()>WeaponRequirementService.MAX_ITEMS) throw new IllegalArgumentException("Weapon balance registry bounds");
        List<Entry> list=items.stream().map(item->{
                var stack=item.getDefaultInstance(); var resolved=WeaponCombatProfileResolver.resolve(stack); boolean weapon=resolved.weapon();
                return new Entry(BuiltInRegistries.ITEM.getKey(item),WeaponRequirementService.profile(stack),WeaponScalingService.profile(stack),
                        weapon?WeaponClassifier.baseAttack(stack):0,weapon,WeaponDamageProfileService.profile(stack),WeaponInfusionEligibilityService.eligibility(stack));
            })
            .sorted(Comparator.comparing(Entry::item)).toList();
        int count=Math.max(1,(list.size()+BATCH_SIZE-1)/BATCH_SIZE);
        for(int index=0;index<count;index++) PacketDistributor.sendToPlayer(player,new Batch(WeaponRequirementService.revision,index,count,
                WeaponRequirementConfig.UNMET_MULTIPLIER.get(),list.subList(index*BATCH_SIZE,Math.min(list.size(),(index+1)*BATCH_SIZE)),
                index==0?WeaponInfusionRegistry.definitions().values().stream().sorted(Comparator.comparing(WeaponInfusionDefinition::id)).toList():List.of()));
    }
    private WeaponRequirementNetwork() {}
}
