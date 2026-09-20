package dev.maplesadventure.config;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class StatusClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue OFFSET_X,OFFSET_Y,SPACING,SCALE,PROC_OFFSET_X,PROC_OFFSET_Y;
    static {
        var b=new ModConfigSpec.Builder();
        OFFSET_X=b.defineInRange("offsetX",0,-2000,2000);
        OFFSET_Y=b.defineInRange("offsetY",0,-2000,2000);
        PROC_OFFSET_X=b.defineInRange("procOffsetX",0,-2000,2000);
        PROC_OFFSET_Y=b.defineInRange("procOffsetY",0,-2000,2000);
        SPACING=b.defineInRange("spacing",0,0,32);
        SCALE=b.comment("Integer scaling keeps the native pixels sharp. Minecraft GUI scale also applies.").defineInRange("scale",1,1,3);
        SPEC=b.build();
    }
    private StatusClientConfig() {}
}
