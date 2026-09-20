package dev.maplesadventure.progression.status.client;
import java.util.*;
import dev.maplesadventure.progression.status.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

public final class ClientStatusState {
    public static final class Display {
        public StatusNetwork.Row row;
        public double received;
        public long zeroSince;
        Display(StatusNetwork.Row row,double received) { this.row=row; this.received=received; }
        public double fill() { return StatusHudMath.fill(row,Math.max(0,gameTime()-received)); }
        public float alpha() {
            if(fill()>0) { zeroSince=0; return 1; }
            if(zeroSince==0) zeroSince=System.nanoTime();
            return (float)Math.clamp(1-(System.nanoTime()-zeroSince)/300000000.0,0,1);
        }
    }
    private static final EnumMap<StatusEffectType,Display> rows=new EnumMap<>(StatusEffectType.class);
    private static final ArrayDeque<StatusNetwork.Proc> procs=new ArrayDeque<>();
    private static final EnumMap<StatusEffectType,Long> thresholdsRevision=new EnumMap<>(StatusEffectType.class);
    private static long procStart;
    private static double controlEnd;
    public static boolean controlLocked() { return gameTime()<controlEnd; }
    public static void register() { NeoForge.EVENT_BUS.register(new ClientStatusState()); }
    public static void accept(StatusNetwork.Snapshot snapshot) {
        double now=gameTime();
        controlEnd=now+snapshot.controlRemaining();
        for(var row:snapshot.rows()) {
            var old=rows.get(row.type());
            if(old==null) { if(row.current()>0||row.remaining()>0) rows.put(row.type(),new Display(row,now)); }
            else { old.row=row; old.received=now; if(row.current()>0||row.remaining()>0) old.zeroSince=0; }
        }
        latest=snapshot;
    }
    private static StatusNetwork.Snapshot latest;
    private static double gameTime() {
        var mc=net.minecraft.client.Minecraft.getInstance();
        return mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
    }
    public static Map<StatusEffectType,Double> thresholds() {
        if(latest==null) return Map.of();
        var out=new EnumMap<StatusEffectType,Double>(StatusEffectType.class);
        latest.rows().forEach(r->out.put(r.type(),r.maximum())); return Map.copyOf(out);
    }
    public static Map<StatusEffectType,Display> displays() { return Collections.unmodifiableMap(rows); }
    public static void proc(StatusNetwork.Proc packet) {
        long previous=thresholdsRevision.getOrDefault(packet.status(),-1L);
        if(packet.revision()<=previous) return;
        thresholdsRevision.put(packet.status(),packet.revision());
        if(procs.size()<4) { if(procs.isEmpty()) procStart=System.nanoTime(); procs.addLast(packet); }
    }
    public static StatusNetwork.Proc currentProc() {
        if(!procs.isEmpty()&&System.nanoTime()-procStart>=1800000000L) { procs.removeFirst(); procStart=System.nanoTime(); }
        return procs.peekFirst();
    }
    public static float procAlpha() {
        double elapsed=(System.nanoTime()-procStart)/1000000.0;
        return (float)Math.clamp(elapsed<100?elapsed/100:elapsed<1400?1:(1800-elapsed)/400,0,1);
    }
    @SubscribeEvent public void input(net.neoforged.neoforge.client.event.MovementInputUpdateEvent e) {
        if(!controlLocked()) return;
        var input=e.getInput(); input.forwardImpulse=0; input.leftImpulse=0; input.jumping=false; input.shiftKeyDown=false;
        input.up=false; input.down=false; input.left=false; input.right=false;
    }
    @SubscribeEvent public void click(net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered e) {
        if(controlLocked()) { e.setCanceled(true); e.setSwingHand(false); }
    }
    @SubscribeEvent public void logout(ClientPlayerNetworkEvent.LoggingOut e) { rows.clear(); procs.clear(); thresholdsRevision.clear(); latest=null; controlEnd=0; }
    @SubscribeEvent public void clone(ClientPlayerNetworkEvent.Clone e) { rows.clear(); procs.clear(); thresholdsRevision.clear(); controlEnd=0; }
    private ClientStatusState() {}
}
