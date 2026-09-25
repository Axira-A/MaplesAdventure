package dev.maplesadventure.bonfire.network;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.bonfire.BonfireSessionState;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** The client sends only a nonce and an action; never a position, role or feature flag. */
public final class BonfirePayloads {
    /** Private progress snapshot for this player only; never broadcast to other players. */
    public record Progress(java.util.List<dev.maplesadventure.bonfire.BonfireRef> activated) implements CustomPacketPayload {
        public Progress {
            if (activated.size() > dev.maplesadventure.bonfire.PlayerBonfireState.MAX_ACTIVATED)
                throw new IllegalArgumentException("Too many bonfire refs");
            activated = java.util.List.copyOf(activated);
        }
        public static final Type<Progress> TYPE = BonfirePayloads.type("bonfire_progress");
        public static final StreamCodec<RegistryFriendlyByteBuf, Progress> CODEC = codec(b -> {
            int count = b.readVarInt();
            if (count < 0 || count > dev.maplesadventure.bonfire.PlayerBonfireState.MAX_ACTIVATED)
                throw new IllegalArgumentException("Invalid bonfire ref count");
            var refs = new java.util.ArrayList<dev.maplesadventure.bonfire.BonfireRef>(count);
            for (int i = 0; i < count; i++) refs.add(new dev.maplesadventure.bonfire.BonfireRef(
                    ResourceLocation.parse(b.readUtf(128)), b.readBlockPos(), b.readUUID()));
            return new Progress(refs);
        }, (b, p) -> {
            b.writeVarInt(p.activated.size());
            for (var ref : p.activated) {
                b.writeUtf(ref.dimension().toString(), 128); b.writeBlockPos(ref.pos()); b.writeUUID(ref.generation());
            }
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public enum ActionType { SELECT_FEATURE, LEAVE }
    public record Action(UUID nonce, ActionType action, ResourceLocation featureId) implements CustomPacketPayload {
        public Action {
            java.util.Objects.requireNonNull(nonce);
            java.util.Objects.requireNonNull(action);
            if (action == ActionType.SELECT_FEATURE && (featureId == null || featureId.toString().length() > 128))
                throw new IllegalArgumentException("Missing or oversized feature ID");
            if (action == ActionType.LEAVE && featureId != null)
                throw new IllegalArgumentException("Leave has no feature ID");
        }
        public static final Type<Action> TYPE = BonfirePayloads.type("bonfire_action");
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = codec(
                b -> {
                    UUID nonce = b.readUUID();
                    ActionType action = b.readEnum(ActionType.class);
                    return new Action(nonce, action, action == ActionType.SELECT_FEATURE ? ResourceLocation.parse(b.readUtf(128)) : null);
                },
                (b, p) -> { b.writeUUID(p.nonce); b.writeEnum(p.action);
                    if (p.action == ActionType.SELECT_FEATURE) b.writeUtf(p.featureId.toString(), 128); });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record MenuEntry(ResourceLocation featureId, String translationKey, int order) {
        public MenuEntry {
            java.util.Objects.requireNonNull(featureId);
            java.util.Objects.requireNonNull(translationKey);
            if (featureId.toString().length() > 128 || translationKey.isBlank() || translationKey.length() > 256)
                throw new IllegalArgumentException("Invalid bonfire menu entry");
        }
    }
    public record View(UUID nonce, BonfireSessionState state, String name, java.util.List<MenuEntry> actions,
                       int transitionTicks, int commitTick, int fadeInTick,
                       net.minecraft.core.BlockPos bonfirePos) implements CustomPacketPayload {
        public View {
            actions = java.util.List.copyOf(actions);
            if (actions.size() > 64 || actions.stream().map(MenuEntry::featureId).distinct().count() != actions.size())
                throw new IllegalArgumentException("Invalid bonfire menu action count");
        }
        public static final Type<View> TYPE = BonfirePayloads.type("bonfire_view");
        public static final StreamCodec<RegistryFriendlyByteBuf, View> CODEC = codec(b -> {
            UUID nonce = b.readUUID();
            BonfireSessionState state = b.readEnum(BonfireSessionState.class);
            String name = b.readUtf(64);
            int count = b.readVarInt();
            if (count < 0 || count > 64) throw new IllegalArgumentException("Invalid bonfire menu count");
            var actions = new java.util.ArrayList<MenuEntry>(count);
            for (int i = 0; i < count; i++) actions.add(new MenuEntry(ResourceLocation.parse(b.readUtf(128)), b.readUtf(256), b.readInt()));
            return new View(nonce, state, name, actions,
                    b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readBlockPos());
        }, (b, p) -> { b.writeUUID(p.nonce); b.writeEnum(p.state); b.writeUtf(p.name, 64);
            b.writeVarInt(p.actions.size());
            for (var entry : p.actions) {
                b.writeUtf(entry.featureId.toString(), 128); b.writeUtf(entry.translationKey, 256); b.writeInt(entry.order);
            }
            b.writeVarInt(p.transitionTicks);
            b.writeVarInt(p.commitTick); b.writeVarInt(p.fadeInTick); b.writeBlockPos(p.bonfirePos); });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Closed(UUID nonce) implements CustomPacketPayload {
        public static final Type<Closed> TYPE = BonfirePayloads.type("bonfire_closed");
        public static final StreamCodec<RegistryFriendlyByteBuf, Closed> CODEC = codec(
                b -> new Closed(b.readUUID()), (b, p) -> b.writeUUID(p.nonce));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Activated(String name) implements CustomPacketPayload {
        public static final Type<Activated> TYPE = BonfirePayloads.type("bonfire_activated");
        public static final StreamCodec<RegistryFriendlyByteBuf, Activated> CODEC = codec(
                b -> new Activated(b.readUtf(64)), (b, p) -> b.writeUtf(p.name, 64));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, path));
    }
    private static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> codec(
            java.util.function.Function<RegistryFriendlyByteBuf, T> decoder,
            java.util.function.BiConsumer<RegistryFriendlyByteBuf, T> encoder) {
        return new StreamCodec<>() {
            @Override public T decode(RegistryFriendlyByteBuf buffer) { return decoder.apply(buffer); }
            @Override public void encode(RegistryFriendlyByteBuf buffer, T value) { encoder.accept(buffer, value); }
        };
    }
    private BonfirePayloads() {}
}
