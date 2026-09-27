package dev.maplesadventure.integration.soulscombathud;

/** Marker contracts used to verify optional mixins before suppressing the fallback HUD. */
public final class SoulsFlaskHooks {
    public interface Selection {}
    public interface Rendering {}
    public interface Use {}
    private SoulsFlaskHooks() {}
}
