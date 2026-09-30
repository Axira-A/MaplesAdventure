package dev.maplesadventure.config;
import net.neoforged.neoforge.common.ModConfigSpec;
/** Local preferences only, never synchronized to the server or stored in a scene. */
public final class EditorClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue LEFT,RIGHT;
    static{var b=new ModConfigSpec.Builder();LEFT=b.defineInRange("leftPanelRatio",.19,.1,.4);RIGHT=b.defineInRange("rightPanelRatio",.25,.1,.45);SPEC=b.build();}
    private EditorClientConfig(){}
}
