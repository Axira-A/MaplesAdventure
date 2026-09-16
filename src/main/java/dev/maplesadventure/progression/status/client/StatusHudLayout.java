package dev.maplesadventure.progression.status.client;
import dev.maplesadventure.config.StatusClientConfig;
public record StatusHudLayout(int x,int bottom,int stride,int scale) {
    public static StatusHudLayout forScreen(int width,int height) {
        int scale=StatusClientConfig.SCALE.get();
        return new StatusHudLayout((width-128*scale)/2+StatusClientConfig.OFFSET_X.get(),
                height-110+StatusClientConfig.OFFSET_Y.get(),(32+StatusClientConfig.SPACING.get())*scale,scale);
    }
}
