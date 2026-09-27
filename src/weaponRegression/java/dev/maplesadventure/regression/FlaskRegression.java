package dev.maplesadventure.regression;

import com.mojang.authlib.GameProfile;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.api.flask.*;
import dev.maplesadventure.flask.*;
import dev.maplesadventure.progression.ProgressionAttachments;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Synthetic server fixture, opt-in isolated worlds only; not a client animation test. */
public final class FlaskRegression {
    public static void register() {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e)->e.getDispatcher().register(
                Commands.literal("flaskregression").requires(s->s.hasPermission(2)).executes(c->run(c.getSource()))));
    }
    private static void check(boolean ok,String label) { if(!ok)throw new IllegalStateException("Flask regression: "+label);MaplesAdventure.LOGGER.info("[Flask regression] PASS {}",label); }
    private static ServerPlayer player(CommandSourceStack source,String name) {
        var profile=new GameProfile(UUID.randomUUID(),name);
        var p=new ServerPlayer(source.getServer(),source.getLevel(),profile,ClientInformation.createDefault());
        p.connection=FakePlayerFactory.get(source.getLevel(),profile).connection;
        p.setPos(source.getLevel().getSharedSpawnPos().getCenter());
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(FlaskItems.CRIMSON.get()));
        return p;
    }
    private static void tick(ServerPlayer p,int ticks) { for(int i=0;i<ticks;i++)FlaskUseController.tick(p.server); }
    private static int run(CommandSourceStack source) {
        var a=player(source,"FlaskFixtureA");var b=player(source,"FlaskFixtureB");
        try {
            var initial=FlaskService.state(a);var beforeB=FlaskService.state(b);
            check(initial.equals(FlaskState.initial(FlaskManaBridge.available())),"New player distribution");
            a.setHealth(3);
            check(FlaskUseController.start(a,FlaskKind.CRIMSON,InteractionHand.MAIN_HAND),"Start independent action");
            a.releaseUsingItem();check(a.isUsingItem(),"Mouse release does not end action");
            tick(a,13);check(a.getHealth()==3&&FlaskService.state(a).equals(initial),"Before effect no heal/charge");
            FlaskApi.cancelUse(a,FlaskCancelReason.STAGGER);check(FlaskService.state(a).equals(initial),"Pre-effect cancel no charge");
            check(FlaskUseController.start(a,FlaskKind.CRIMSON,InteractionHand.MAIN_HAND),"Restart action");
            tick(a,14);check(a.getHealth()==3&&FlaskService.state(a).crimsonRemaining()==initial.crimsonRemaining()-1,"Tick14 commits one charge, not instantaneous healing");
            FlaskApi.cancelUse(a,FlaskCancelReason.STAGGER);check(FlaskService.state(a).crimsonRemaining()==initial.crimsonRemaining()-1,"Post-effect cancel no refund");
            for(int step=1;step<=6;step++) {
                tick(a,1);check(Math.abs(a.getHealth()-(3+2.5*step))<0.0001,"Gradual HP step "+step+" survives stagger");
            }
            tick(a,2);check(a.getHealth()==18,"No repeated restoration after sixth step");
            for(var reason:new FlaskCancelReason[]{FlaskCancelReason.DEATH,FlaskCancelReason.LOGOUT,
                    FlaskCancelReason.DIMENSION_CHANGE,FlaskCancelReason.BONFIRE}) {
                FlaskApi.refill(a,FlaskRechargeReason.BONFIRE);a.setHealth(3);
                check(FlaskUseController.start(a,FlaskKind.CRIMSON,InteractionHand.MAIN_HAND),"Start cleanup case "+reason);
                tick(a,15);check(a.getHealth()==5.5F,"First increment before "+reason);
                FlaskApi.cancelUse(a,reason);tick(a,6);check(a.getHealth()==5.5F,"Pending recovery removed by "+reason);
            }
            FlaskApi.refill(a,FlaskRechargeReason.BONFIRE);a.setHealth(3);
            FlaskUseController.start(a,FlaskKind.CRIMSON,InteractionHand.MAIN_HAND);tick(a,16);
            a.setHealth(4);FlaskApi.cancelUse(a,FlaskCancelReason.DAMAGE);tick(a,4);
            check(a.getHealth()==14,"Intervening damage is preserved while committed recovery continues");
            FlaskApi.refill(a,FlaskRechargeReason.BONFIRE);a.setHealth(97);
            FlaskUseController.start(a,FlaskKind.CRIMSON,InteractionHand.MAIN_HAND);tick(a,15);
            check(a.getHealth()==99.5F,"HP increment below cap");tick(a,15);check(a.getHealth()==100,"HP restoration capped");
            a.setHealth(100);FlaskApi.refill(a,FlaskRechargeReason.SCRIPTED);
            check(FlaskUseController.start(a,FlaskKind.CRIMSON,InteractionHand.MAIN_HAND),"Full HP still permits drink");
            tick(a,30);check(a.getHealth()==100&&FlaskService.state(a).crimsonRemaining()==initial.crimsonRemaining()-1&&!FlaskApi.isUsingFlask(a),"Full HP consumes; finish30");
            check(beforeB.equals(FlaskService.state(b)),"Second player independent");
            a.setData(ProgressionAttachments.FLASK,new FlaskState(new FlaskSnapshot(4,0,4,0,0,0)));
            check(!FlaskUseController.start(a,FlaskKind.CRIMSON,InteractionHand.MAIN_HAND),"Empty rejects start");
            FlaskApi.refill(a,FlaskRechargeReason.BONFIRE);check(FlaskService.state(a).crimsonRemaining()==4,"Bonfire reason refills");
            var tag=a.getData(ProgressionAttachments.FLASK).serializeNBT(a.registryAccess());
            var copy=new FlaskState();copy.deserializeNBT(a.registryAccess(),tag);check(copy.value().equals(FlaskService.state(a)),"Persistent NBT roundtrip");
            var before=FlaskService.state(a);
            FlaskMenuService.request(a,new FlaskPayloads.Request(UUID.randomUUID(),FlaskPayloads.Action.ALLOCATE,0,4));
            check(before.equals(FlaskService.state(a)),"Forged menu nonce has no effect");
            var upgrade=FlaskMenuService.class.getDeclaredMethod("upgrade",ServerPlayer.class,FlaskPayloads.Action.class);upgrade.setAccessible(true);
            a.getInventory().add(new ItemStack(FlaskItems.SHARD.get(),30));a.getInventory().add(new ItemStack(FlaskItems.ASH.get(),12));
            for(int capacity=4;capacity<14;capacity++) {
                int held=a.getInventory().countItem(FlaskItems.SHARD.get());
                check(upgrade.invoke(null,a,FlaskPayloads.Action.UPGRADE_CAPACITY)==FlaskPayloads.Result.OK,"Capacity purchase "+capacity);
                check(a.getInventory().countItem(FlaskItems.SHARD.get())==held-FlaskRules.capacityCost(capacity),"Exact shard cost "+capacity);
            }
            check(FlaskService.state(a).totalCapacity()==14&&a.getInventory().countItem(FlaskItems.SHARD.get())==0,"Capacity14 costs30");
            check(upgrade.invoke(null,a,FlaskPayloads.Action.UPGRADE_CAPACITY)==FlaskPayloads.Result.AT_CAP,"Capacity cap rejects");
            for(int i=0;i<12;i++)check(upgrade.invoke(null,a,FlaskPayloads.Action.UPGRADE_POTENCY)==FlaskPayloads.Result.OK,"Potency purchase "+i);
            check(a.getInventory().countItem(FlaskItems.ASH.get())==0&&FlaskService.state(a).potencyLevel()==12,"Potency12 costs12");
            check(upgrade.invoke(null,a,FlaskPayloads.Action.UPGRADE_POTENCY)==FlaskPayloads.Result.AT_CAP,"Potency cap rejects");
            if(FlaskManaBridge.available()) {
                FlaskService.debugReset(a);check(FlaskService.allocate(a,2,2),"Valid allocation");
                check(!FlaskService.allocate(a,3,3)&&!FlaskService.allocate(a,-1,5),"Invalid allocations reject");
                var adapter=(dev.maplesadventure.progression.runtime.DerivedStatRuntimeAdapter)Class.forName(
                        "dev.maplesadventure.integration.ironsspellbooks.progression.IronsManaAdapter").getConstructor().newInstance();
                adapter.restoreCurrentRatio(a,0);a.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(FlaskItems.ASHEN.get()));
                double max=adapter.inspect(a,dev.maplesadventure.progression.PlayerAttributeService.state(a)).runtimeValue();
                check(FlaskUseController.start(a,FlaskKind.ASHEN,InteractionHand.MAIN_HAND),"Ashen start");tick(a,14);
                check(Math.abs(adapter.currentValue(a).orElseThrow())<0.001,"Mana effect commit is not instantaneous");
                for(int step=1;step<=6;step++) {
                    tick(a,1);check(Math.abs(adapter.currentValue(a).orElseThrow()-Math.min(max,129.0*step/6))<0.001,"Gradual mana step "+step);
                }
                tick(a,10);
                check(Math.abs(adapter.currentValue(a).orElseThrow()-Math.min(max,129))<0.001&&FlaskService.state(a).ashenRemaining()==1,"Ashen restores real mana");
                adapter.restoreCurrentRatio(a,1);check(FlaskUseController.start(a,FlaskKind.ASHEN,InteractionHand.MAIN_HAND),"Full mana start");tick(a,30);
                check(FlaskService.state(a).ashenRemaining()==0&&Math.abs(adapter.currentValue(a).orElseThrow()-max)<0.001,"Full mana consumes without overflow");
            } else check(!FlaskService.allocate(a,13,1),"Absent Iron's rejects blue allocation");
            source.sendSuccess(()->Component.literal("Flask regression PASS (synthetic server participants)"),false);return 1;
        } catch(ReflectiveOperationException error) {throw new IllegalStateException(error);}
        finally {FlaskUseController.cancel(a,FlaskCancelReason.SCRIPTED);FlaskUseController.cancel(b,FlaskCancelReason.SCRIPTED);}
    }
    private FlaskRegression() {}
}
