package dev.maplesadventure.flask;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.api.bonfire.*;
import dev.maplesadventure.api.flask.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Nonces are transient authorizations, not persistent player progression. Each submission consumes its nonce. */
public final class FlaskMenuService {
    private record Session(UUID nonce, MaplesBonfireRef ref, FlaskPayloads.Page page, long opened, FlaskSnapshot baseline) {}
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    public static void register() {
        for (FlaskPayloads.Page page : FlaskPayloads.Page.values()) MaplesBonfireApi.registerFeature(new MaplesBonfireFeatureHandler() {
            public ResourceLocation id() { return feature(page); }
            public String translationKey() { return "screen.maplesadventure.flask.menu." + page.name().toLowerCase(Locale.ROOT); }
            public int order() { return 200 + page.ordinal() * 10; }
            public boolean isAvailable(MaplesBonfireContext context) { return page != FlaskPayloads.Page.ALLOCATION || FlaskManaBridge.available(); }
            public void execute(MaplesBonfireContext context) { open(context.player(),context.bonfire().ref(),page,FlaskPayloads.Result.OK,null); }
        });
    }
    public static ResourceLocation feature(FlaskPayloads.Page page) {
        return switch (page) { case ALLOCATION -> MaplesBonfireFeatures.FLASK_ALLOCATION; case UPGRADE -> MaplesBonfireFeatures.FLASK_UPGRADE; };
    }
    private static void open(ServerPlayer p, MaplesBonfireRef ref, FlaskPayloads.Page page, FlaskPayloads.Result result, UUID replyTo) {
        Session session = new Session(UUID.randomUUID(),ref,page,p.server.getTickCount(),FlaskService.state(p));
        SESSIONS.put(p.getUUID(),session); send(p,session,result,replyTo);
    }
    private static boolean valid(ServerPlayer p, Session s) {
        return p.server.getTickCount() - s.opened <= 2400 && MaplesBonfireApi.isResting(p)
                && p.level().dimension().location().equals(s.ref.dimension())
                && MaplesBonfireApi.lastRested(p).filter(s.ref::equals).isPresent()
                && MaplesBonfireApi.availableFeatures(p).contains(feature(s.page))
                && MaplesBonfireApi.query(p.serverLevel(),s.ref.position()).filter(v -> v.ref().equals(s.ref)).isPresent();
    }
    public static int count(ServerPlayer p, Item item) { return Math.min(4096,p.getInventory().countItem(item)); }
    public static void request(ServerPlayer p, FlaskPayloads.Request request) {
        FlaskService.checkThread(p);
        Session s = SESSIONS.get(p.getUUID());
        if (s == null || !s.nonce.equals(request.nonce())) return;
        SESSIONS.remove(p.getUUID());
        if (request.action() == FlaskPayloads.Action.CLOSE) return;
        if (!valid(p,s)) { send(p,s,FlaskPayloads.Result.INVALID_SESSION,s.nonce); return; }
        FlaskPayloads.Result result;
        if (!permits(s.page,request.action()) || !s.baseline.equals(FlaskService.state(p))) result = FlaskPayloads.Result.FAILED;
        else if (s.page == FlaskPayloads.Page.ALLOCATION) result = FlaskManaBridge.available()
                && FlaskService.allocate(p,request.crimson(),request.ashen()) ? FlaskPayloads.Result.OK : FlaskPayloads.Result.INVALID_ALLOCATION;
        else result = upgrade(p,request.action());
        open(p,s.ref,s.page,result,s.nonce); // rotates even on failure; stale double-clicks cannot buy twice
    }
    static boolean permits(FlaskPayloads.Page page, FlaskPayloads.Action action) {
        return action == FlaskPayloads.Action.CLOSE || (page == FlaskPayloads.Page.ALLOCATION
                ? action == FlaskPayloads.Action.ALLOCATE
                : action == FlaskPayloads.Action.UPGRADE_CAPACITY || action == FlaskPayloads.Action.UPGRADE_POTENCY);
    }
    private static FlaskPayloads.Result upgrade(ServerPlayer p, FlaskPayloads.Action action) {
        if (action != FlaskPayloads.Action.UPGRADE_CAPACITY && action != FlaskPayloads.Action.UPGRADE_POTENCY)
            return FlaskPayloads.Result.FAILED;
        var old = FlaskService.state(p);
        boolean capacity = action == FlaskPayloads.Action.UPGRADE_CAPACITY;
        if (capacity ? old.totalCapacity() >= 14 : old.potencyLevel() >= 12) return FlaskPayloads.Result.AT_CAP;
        Item item = capacity ? FlaskItems.SHARD.get() : FlaskItems.ASH.get();
        int cost = capacity ? FlaskRules.capacityCost(old.totalCapacity()) : 1;
        if (count(p,item) < cost) return FlaskPayloads.Result.INSUFFICIENT_MATERIAL;
        // No callbacks during commit. Snapshot only the slots we will actually mutate.
        Map<Integer,ItemStack> slots = new LinkedHashMap<>();
        int needed = cost;
        for (int slot=0; slot<p.getInventory().getContainerSize() && needed>0; slot++) {
            ItemStack stack = p.getInventory().getItem(slot);
            if (stack.is(item)) { slots.put(slot,stack.copy()); needed -= Math.min(needed,stack.getCount()); }
        }
        if (needed != 0) return FlaskPayloads.Result.INSUFFICIENT_MATERIAL;
        int extra = capacity ? 1 : 0;
        var next = new FlaskSnapshot(old.totalCapacity()+extra, old.potencyLevel()+(capacity?0:1),
                old.crimsonAllocated()+extra,old.crimsonAllocated()+extra,old.ashenAllocated(),old.ashenAllocated());
        try {
            needed=cost;
            for (int slot:slots.keySet()) { ItemStack stack=p.getInventory().getItem(slot); int take=Math.min(needed,stack.getCount()); stack.shrink(take); needed-=take; }
            FlaskService.write(p,next);
        } catch (RuntimeException error) {
            slots.forEach((slot,stack) -> p.getInventory().setItem(slot,stack)); FlaskService.write(p,old);
            MaplesAdventure.LOGGER.error("Flask upgrade rolled back for {}",p.getUUID(),error);
            return FlaskPayloads.Result.FAILED;
        }
        p.getInventory().setChanged(); p.containerMenu.broadcastChanges(); FlaskService.sync(p);
        FlaskService.post(new FlaskEvents.Upgrade(p,old,next));
        return FlaskPayloads.Result.OK;
    }
    private static void send(ServerPlayer p, Session s, FlaskPayloads.Result result, UUID replyTo) {
        PacketDistributor.sendToPlayer(p,new FlaskPayloads.Menu(s.nonce,replyTo,s.page,FlaskService.state(p),FlaskManaBridge.available(),
                count(p,FlaskItems.SHARD.get()),count(p,FlaskItems.ASH.get()),result));
    }
    public static void close(ServerPlayer p) { SESSIONS.remove(p.getUUID()); }
    public static void clear() { SESSIONS.clear(); }
    private FlaskMenuService() {}
}
