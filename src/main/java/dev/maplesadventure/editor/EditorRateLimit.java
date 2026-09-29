package dev.maplesadventure.editor;

/** Tick-based server token bucket, used only for connected editor requests. */
public final class EditorRateLimit {
    private long last; private double tokens=40;
    public boolean take(long tick){tokens=Math.min(40,tokens+Math.max(0,tick-last));last=tick;if(tokens<1)return false;tokens--;return true;}
}
