package dev.maplesadventure.editor;

import dev.maplesadventure.MaplesAdventure;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.*;

public final class EditorAttachments {
    private static final DeferredRegister<AttachmentType<?>> TYPES=DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES,MaplesAdventure.MOD_ID);
    public static final DeferredHolder<AttachmentType<?>,AttachmentType<EditorRecoveryState>> RECOVERY=TYPES.register("editor_recovery",()->
            AttachmentType.serializable(EditorRecoveryState::new).copyOnDeath().build());
    public static final DeferredHolder<AttachmentType<?>,AttachmentType<dev.maplesadventure.authoring.logic.GameFlagState>> FLAGS=TYPES.register("game_flags",()->
            AttachmentType.serializable(dev.maplesadventure.authoring.logic.GameFlagState::new).copyOnDeath().build());
    public static void register(IEventBus bus){TYPES.register(bus);}
    private EditorAttachments(){}
}
