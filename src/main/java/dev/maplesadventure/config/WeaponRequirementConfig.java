package dev.maplesadventure.config;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class WeaponRequirementConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue AUTO_CAP;
    public static final ModConfigSpec.DoubleValue UNMET_MULTIPLIER;
    static {
        var b = new ModConfigSpec.Builder();
        AUTO_CAP = b.defineInRange("autoRequirementCap",40,5,40);
        UNMET_MULTIPLIER = b.defineInRange("unmetRequirementDamageMultiplier",.35,.10,1.0);
        SPEC = b.build();
    }
    private WeaponRequirementConfig() {}
}
