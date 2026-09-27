package dev.maplesadventure.integration.soulscombathud;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/** Gates only the three Souls HUD mixins, before incompatible signatures can fail application. */
public final class SoulsHudMixinPlugin implements IMixinConfigPlugin {
    private Boolean supported;
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (supported == null) supported = inspect();
        return supported && SoulsHudCompatibility.CONTRACTS.containsKey(targetClassName);
    }
    private boolean inspect() {
        var loading = LoadingModList.get();
        if (loading == null || loading.getMods().stream().noneMatch(mod -> mod.getModId().equals("souls_combat_hud")
                && SoulsHudCompatibility.supportedVersion(mod.getVersion().toString())))
            return SoulsHudCompatibility.reject("Souls Combat HUD absent or unsupported in early mod list");
        try {
            var provider = MixinService.getService().getBytecodeProvider();
            for (String target : SoulsHudCompatibility.CONTRACTS.keySet())
                if (!SoulsHudCompatibility.matches(target, provider.getClassNode(target)))
                    return SoulsHudCompatibility.reject("Injection signature mismatch: " + target);
            return true;
        } catch (IOException | ClassNotFoundException | RuntimeException | LinkageError error) {
            // The client adapter reports missing markers once and retains its standalone HUD.
            return SoulsHudCompatibility.reject("Cannot inspect HUD bytecode: " + error);
        }
    }
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
