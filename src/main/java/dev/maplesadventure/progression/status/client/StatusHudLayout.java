package dev.maplesadventure.progression.status.client;
import dev.maplesadventure.config.StatusClientConfig;
public record StatusHudLayout(int x,int bottom,int stride,int scale) {
    public record Position(int x,int y) {}
    /** Keep native integer pixels; overflow wraps into columns instead of leaving the screen. */
    public Position bar(int index,int count,int width,int height) {
        int safeBottom=Math.clamp(bottom,4,Math.max(4,height-32*scale-4));
        int rows=Math.max(1,(safeBottom-4)/stride+1);
        int columns=Math.max(1,(count+rows-1)/rows);
        int column=index/rows,row=index%rows,pitch=136*scale;
        int left=Math.clamp(x-(columns-1)*pitch/2,0,Math.max(0,width-(columns-1)*pitch-128*scale));
        return new Position(left+column*pitch,safeBottom-row*stride);
    }
    public static int procY(int height,int offset) { return Math.clamp((int)Math.round(height*.35)+offset,0,Math.max(0,height-24)); }
    public static StatusHudLayout forScreen(int width,int height) {
        int scale=StatusClientConfig.SCALE.get();
        return new StatusHudLayout((width-128*scale)/2+StatusClientConfig.OFFSET_X.get(),
                height-110+StatusClientConfig.OFFSET_Y.get(),(32+StatusClientConfig.SPACING.get())*scale,scale);
    }
}
