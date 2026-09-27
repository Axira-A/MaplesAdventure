package dev.maplesadventure.flask;

import dev.maplesadventure.api.flask.*;
import io.netty.buffer.Unpooled;
import java.util.*;
import java.nio.file.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlaskCoreTest {
    @Test void legacyUpgradeFeaturesCollapseToOneEntry() {
        var tags = new net.minecraft.nbt.ListTag();
        tags.add(net.minecraft.nbt.StringTag.valueOf("flask_capacity"));
        tags.add(net.minecraft.nbt.StringTag.valueOf("maplesadventure:flask_potency"));
        tags.add(net.minecraft.nbt.StringTag.valueOf("maplesadventure:flask_upgrade"));
        tags.add(net.minecraft.nbt.StringTag.valueOf("addon:craft"));
        var features = dev.maplesadventure.bonfire.BonfireFeatureIds.load(tags);
        assertEquals(Set.of(dev.maplesadventure.api.bonfire.MaplesBonfireFeatures.FLASK_UPGRADE,
                net.minecraft.resources.ResourceLocation.parse("addon:craft")), features);
        assertEquals(features, dev.maplesadventure.bonfire.BonfireFeatureIds.load(
                dev.maplesadventure.bonfire.BonfireFeatureIds.save(features)));
        assertEquals(2, FlaskPayloads.Page.values().length); // allocation + one reinforcement entry
    }
    @Test void upgradeAuthorizationDoesNotPermitAllocationAndViceVersa() {
        for (var page : FlaskPayloads.Page.values()) for (var action : FlaskPayloads.Action.values()) {
            boolean expected = action == FlaskPayloads.Action.CLOSE || (page == FlaskPayloads.Page.ALLOCATION
                    ? action == FlaskPayloads.Action.ALLOCATE
                    : action == FlaskPayloads.Action.UPGRADE_CAPACITY || action == FlaskPayloads.Action.UPGRADE_POTENCY);
            assertEquals(expected, FlaskMenuService.permits(page, action));
        }
    }
    @Test void combinedUpgradeMenuAndIntentRoundtrip() {
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            UUID nonce = UUID.randomUUID();
            for (var action : FlaskPayloads.Action.values()) {
                var request = new FlaskPayloads.Request(nonce, action, 3, 1);
                FlaskPayloads.Request.CODEC.encode(buf, request);
                assertEquals(request, FlaskPayloads.Request.CODEC.decode(buf));
            }
            for (var page : FlaskPayloads.Page.values()) for (boolean reply : List.of(false, true)) {
                var packet = new FlaskPayloads.Menu(nonce, reply ? UUID.randomUUID() : null, page,
                        FlaskState.initial(true), true, 30, 12, FlaskPayloads.Result.OK);
                FlaskPayloads.Menu.CODEC.encode(buf, packet);
                assertEquals(packet, FlaskPayloads.Menu.CODEC.decode(buf));
            }
            buf.clear(); buf.writeUUID(nonce); buf.writeVarInt(99); buf.writeVarInt(0); buf.writeVarInt(0);
            assertThrows(RuntimeException.class, () -> FlaskPayloads.Request.CODEC.decode(buf));
        } finally { buf.release(); }
    }
    @Test void bonfirePagesHaveDistinctIdsAndLocalizedMenuLabels() throws Exception {
        Set<net.minecraft.resources.ResourceLocation> ids = new HashSet<>();
        for (var page : FlaskPayloads.Page.values()) assertTrue(ids.add(FlaskMenuService.feature(page)));
        for (String language : List.of("en_us", "zh_cn")) {
            var json = com.google.gson.JsonParser.parseString(Files.readString(Path.of(
                    "src/main/resources/assets/maplesadventure/lang/" + language + ".json"))).getAsJsonObject();
            Set<String> labels = new HashSet<>();
            for (var page : FlaskPayloads.Page.values()) {
                String key = "screen.maplesadventure.flask.menu." + page.name().toLowerCase(Locale.ROOT);
                assertTrue(json.has(key), key);
                assertTrue(labels.add(json.get(key).getAsString()), "Ambiguous Flask menu label in " + language);
            }
        }
    }
    @Test void frozenTables() {
        double[] hp={15,20.5,25.5,30,34,37.5,40,42,43.5,45,46.5,47.5,48.5};
        double[] mana={129,153,177,202,226,242,258,274,290,306,323,339,355};
        for(int i=0;i<=12;i++) {assertEquals(hp[i],FlaskRules.health(i));assertEquals(mana[i],FlaskRules.mana(i));}
    }
    @Test void capacityCostsAndCaps() {
        int[] costs={1,1,2,2,3,3,4,4,5,5};int sum=0;
        for(int capacity=4;capacity<14;capacity++) {assertEquals(costs[capacity-4],FlaskRules.capacityCost(capacity));sum+=FlaskRules.capacityCost(capacity);}
        assertEquals(30,sum); assertEquals(0,FlaskRules.capacityCost(14));
        assertThrows(IllegalArgumentException.class,()->new FlaskSnapshot(15,0,15,15,0,0));
        assertThrows(IllegalArgumentException.class,()->new FlaskSnapshot(4,13,4,4,0,0));
    }
    @Test void allAllocationsAndRefills() {
        for(int cap=4;cap<=14;cap++)for(int red=0;red<=cap;red++) {
            var s=new FlaskSnapshot(cap,12,red,0,cap-red,0).refill();
            assertEquals(red,s.crimsonRemaining());assertEquals(cap-red,s.ashenRemaining());
        }
        assertThrows(IllegalArgumentException.class,()->new FlaskSnapshot(8,0,5,5,4,4));
        assertThrows(IllegalArgumentException.class,()->new FlaskSnapshot(8,0,-1,0,9,0));
    }
    @Test void newPlayerVsRemovedManaMod() {
        assertEquals(new FlaskSnapshot(4,0,4,4,0,0),FlaskState.initial(false));
        assertEquals(new FlaskSnapshot(4,0,3,3,1,1),FlaskState.initial(true));
        var migrated=FlaskState.normalize(10,8,4,2,6,3,false);
        assertEquals(new FlaskSnapshot(10,8,10,5,0,0),migrated); // merge remaining, never refill by uninstalling
        var existing=FlaskState.normalize(4,0,4,2,0,0,true);
        assertEquals(4,existing.crimsonAllocated());assertEquals(0,existing.ashenAllocated());
    }
    @Test void malformedNbtAndExtremeIntegersNormalize() {
        int[] values={Integer.MIN_VALUE,-1,0,4,10,14,99,Integer.MAX_VALUE};
        for(int cap:values)for(int red:values)for(boolean mana:new boolean[]{false,true}) {
            var s=FlaskState.normalize(cap,-5,red,Integer.MAX_VALUE,99,Integer.MAX_VALUE,mana);
            assertTrue(s.totalCapacity()>=4&&s.totalCapacity()<=14);
            assertEquals(s.totalCapacity(),s.crimsonAllocated()+s.ashenAllocated());
            assertEquals(0,s.potencyLevel());
            if(!mana)assertEquals(0,s.ashenRemaining());
        }
        FlaskState state=new FlaskState();CompoundTag bad=new CompoundTag();
        bad.putInt("Capacity",100);bad.putInt("Potency",100);bad.putInt("CrimsonAllocated",9);bad.putInt("AshenAllocated",9);
        state.deserializeNBT(null,bad);assertEquals(14,state.value().totalCapacity());assertEquals(12,state.value().potencyLevel());
        assertEquals(5,state.value().ashenAllocated());
    }
    @Test void saveCloneRestartPreservesAllSixFields() {
        var original=new FlaskState(new FlaskSnapshot(11,7,3,1,8,4));
        var copy=new FlaskState();copy.deserializeNBT(null,original.serializeNBT(null));
        assertEquals(original.value(),copy.value());assertTrue(copy.initialized());
    }
    @Test void zeroChargeCannotConsume() {
        var empty=new FlaskSnapshot(4,0,4,0,0,0);
        assertThrows(IllegalStateException.class,()->empty.consume(FlaskKind.CRIMSON));
        assertThrows(IllegalStateException.class,()->empty.consume(FlaskKind.ASHEN));
    }
    @Test void effectOnceAt14AndFinish30() {
        var clock=new FlaskUseTimeline();var state=FlaskState.initial(false);
        for(int i=0;i<13;i++){clock.tick();assertFalse(clock.claimEffect());}
        assertEquals(4,state.crimsonRemaining()); // abandoning here leaves state untouched
        clock.tick();assertTrue(clock.claimEffect());state=state.consume(FlaskKind.CRIMSON);
        assertEquals(3,state.crimsonRemaining());assertFalse(clock.claimEffect());assertFalse(clock.finished());
        // Abandoning after effect retains the committed 3 charges; no refund operation exists.
        for(int i=14;i<30;i++)clock.tick();assertTrue(clock.finished());assertFalse(clock.claimEffect());
    }
    @Test void iconsUseAllocatedPool() {
        assertEquals(1,FlaskRules.icon(1,1));assertEquals(7,FlaskRules.icon(0,0));
        assertEquals(6,FlaskRules.icon(1,14));assertEquals(4,FlaskRules.icon(2,4));
        for (int allocated = 1; allocated <= 14; allocated++) {
            assertEquals(1, FlaskRules.icon(allocated, allocated));
            assertEquals(7, FlaskRules.icon(0, allocated));
            for (int left = 1; left <= allocated; left++) {
                assertTrue(FlaskRules.icon(left, allocated) < 7);
                assertTrue(FlaskRules.icon(left, allocated) <= FlaskRules.icon(left - 1, allocated));
            }
        }
    }
    @Test void wireRoundtripBoundedSnapshot() {
        var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try {
            var packet=new FlaskPayloads.Snapshot(new FlaskSnapshot(14,12,6,3,8,2),true,2);
            FlaskPayloads.Snapshot.CODEC.encode(buf,packet);assertEquals(packet,FlaskPayloads.Snapshot.CODEC.decode(buf));
            buf.clear();buf.writeByte(100);for(int i=0;i<5;i++)buf.writeByte(0);
            assertThrows(IllegalArgumentException.class,()->FlaskPayloads.readState(buf));
        } finally {buf.release();}
    }
    @Test void apiDoesNotExposeMutableInternalOrOptionalTypes() {
        for(var method:FlaskApi.class.getMethods()) {
            assertFalse(method.getName().startsWith("set"));
            assertFalse(method.getReturnType().getName().contains("FlaskState"));
            for(var parameter:method.getParameterTypes()) {
                assertFalse(parameter.getName().startsWith("io.redspace"));assertFalse(parameter.getName().startsWith("yesman"));
            }
        }
    }
    @Test void coreHasNoOptionalImportsAndNoHurtOrItemDurability() throws Exception {
        try(var paths=Files.walk(Path.of("src/main/java/dev/maplesadventure/flask"))) {
            for(var path:paths.filter(p->p.toString().endsWith(".java")).toList()) {
                String source=Files.readString(path);assertFalse(source.contains("import io.redspace"));
                assertFalse(source.contains("import yesman"));assertFalse(source.contains("hurtAndBreak("));
            }
        }
    }
}
