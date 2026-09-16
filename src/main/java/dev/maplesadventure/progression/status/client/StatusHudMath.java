package dev.maplesadventure.progression.status.client;
import dev.maplesadventure.progression.status.StatusNetwork;
public final class StatusHudMath {
    public static double fill(StatusNetwork.Row row,double elapsedTicks) {
        double elapsed=Math.max(0,elapsedTicks);
        double current=row.mode()==StatusNetwork.HUDMode.ACTIVE_DURATION?Math.max(0,row.remaining()-elapsed):
                Math.max(0,row.current()-Math.max(0,elapsed-row.decayIn())*row.decayPerSecond()/20);
        double max=row.mode()==StatusNetwork.HUDMode.ACTIVE_DURATION?row.total():row.maximum();
        return max<=0?0:Math.clamp(current/max,0,1);
    }
    public static int fillPixels(double ratio,int width) { return (int)Math.floor(Math.clamp(ratio,0,1)*width+1e-8); }
    /** ABGR input/output; luminance is derived from the red artwork's intensity, never red*green multiply. */
    public static int remap(int abgr,int rgb) {
        int a=abgr>>>24,r=abgr&255,g=abgr>>>8&255,b=abgr>>>16&255;
        double intensity=Math.max(r,Math.max(g,b))/255.0;
        int rr=(int)Math.round((rgb>>16&255)*intensity),gg=(int)Math.round((rgb>>8&255)*intensity),bb=(int)Math.round((rgb&255)*intensity);
        return a<<24|bb<<16|gg<<8|rr;
    }
    private StatusHudMath() {}
}
