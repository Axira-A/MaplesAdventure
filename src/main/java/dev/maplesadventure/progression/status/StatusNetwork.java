package dev.maplesadventure.progression.status;

import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Owner-only, change-driven. Client extrapolates decay/duration, never writes gameplay. */
public final class StatusNetwork {
    public enum HUDMode { BUILDUP, ACTIVE_DURATION }
    public record Row(StatusEffectType type,HUDMode mode,double current,double maximum,long remaining,long total,
                      long decayIn,double decayPerSecond,long serial) {
        public Row {
            StatusResistance.bounded(current,0,100000); StatusResistance.bounded(maximum,.001,100000); StatusResistance.bounded(decayPerSecond,0,10000);
            if(remaining<0||total<0||total>72000||remaining>total||decayIn<0||decayIn>72000||serial<0) throw new IllegalArgumentException("HUD time bounds");
        }
    }
    public record Snapshot(long serverTime,long revision,List<Row> rows,long controlRemaining) implements CustomPacketPayload {
        public Snapshot(long time,long revision,List<Row> rows) { this(time,revision,rows,0); }
        public static final Type<Snapshot> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("maplesadventure","status_hud"));
        public Snapshot {
            rows=List.copyOf(rows);
            if(controlRemaining<0||controlRemaining>1200) throw new IllegalArgumentException("Control snapshot bounds");
            if(serverTime<0||revision<0||rows.size()!=StatusEffectType.values().length||rows.stream().map(Row::type).distinct().count()!=StatusEffectType.values().length) throw new IllegalArgumentException("HUD bounds");
        }
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Proc(StatusEffectType status,long revision,long serial) implements CustomPacketPayload {
        public static final Type<Proc> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("maplesadventure","status_proc"));
        public Proc { if(revision<0||serial<0) throw new IllegalArgumentException("Proc revision bounds"); }
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> SNAPSHOT_CODEC=new StreamCodec<>() {
        public Snapshot decode(RegistryFriendlyByteBuf b) {
            if(b.readableBytes()>1024) throw new IllegalArgumentException("Status HUD bytes");
            long time=b.readVarLong(),rev=b.readVarLong(); var rows=new ArrayList<Row>();
            for(int i=0;i<StatusEffectType.values().length;i++) rows.add(new Row(b.readEnum(StatusEffectType.class),b.readEnum(HUDMode.class),b.readDouble(),b.readDouble(),
                    b.readVarLong(),b.readVarLong(),b.readVarLong(),b.readDouble(),b.readVarLong()));
            return new Snapshot(time,rev,rows,b.readVarLong());
        }
        public void encode(RegistryFriendlyByteBuf b,Snapshot s) {
            b.writeVarLong(s.serverTime()); b.writeVarLong(s.revision());
            for(var row:s.rows()) {
                b.writeEnum(row.type()); b.writeEnum(row.mode()); b.writeDouble(row.current()); b.writeDouble(row.maximum());
                b.writeVarLong(row.remaining()); b.writeVarLong(row.total()); b.writeVarLong(row.decayIn()); b.writeDouble(row.decayPerSecond()); b.writeVarLong(row.serial());
            }
            b.writeVarLong(s.controlRemaining());
        }
    };
    private static final StreamCodec<RegistryFriendlyByteBuf,Proc> PROC_CODEC=new StreamCodec<>() {
        public Proc decode(RegistryFriendlyByteBuf b) { return new Proc(b.readEnum(StatusEffectType.class),b.readVarLong(),b.readVarLong()); }
        public void encode(RegistryFriendlyByteBuf b,Proc p) { b.writeEnum(p.status()); b.writeVarLong(p.revision()); b.writeVarLong(p.serial()); }
    };
    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Snapshot.TYPE,SNAPSHOT_CODEC,(p,c)->dev.maplesadventure.progression.status.client.ClientStatusState.accept(p));
        registrar.playToClient(Proc.TYPE,PROC_CODEC,(p,c)->dev.maplesadventure.progression.status.client.ClientStatusState.proc(p));
    }
    public static Snapshot snapshot(ServerPlayer player) {
        long now=StatusRuntimeService.now(player); var state=player.getExistingData(dev.maplesadventure.progression.ProgressionAttachments.STATUS_RUNTIME).orElse(null);
        var rows=new ArrayList<Row>();
        for(var type:StatusEffectType.values()) {
            var e=state==null?null:state.get(type); var d=StatusDefinitions.get(type); boolean active=e!=null&&e.active(now);
            rows.add(new Row(type,active?HUDMode.ACTIVE_DURATION:HUDMode.BUILDUP,e==null?0:e.current,StatusResistanceService.resolve(player,type).threshold(),
                    active?e.activeEnd-now:0,active?e.activeEnd-e.activeStart:0,e==null?0:Math.clamp(e.lastBuildup+d.decayDelay()-now,0,72000),
                    d.decayPerSecond(),e==null?0:e.procSerial));
        }
        return new Snapshot(now,state==null?0:state.revision(),rows,state!=null&&state.locked(now)?Math.min(1200,state.lockEnd-now):0);
    }
    public static void sync(ServerPlayer player) { if(player.connection!=null) PacketDistributor.sendToPlayer(player,snapshot(player)); }
    public static void proc(ServerPlayer player,StatusEffectType type,long revision,long serial) {
        if(player.connection!=null) PacketDistributor.sendToPlayer(player,new Proc(type,revision,serial));
    }
    private StatusNetwork() {}
}
