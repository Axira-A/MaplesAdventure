package dev.maplesadventure.client.editor;
/** GUI-unit layout; responsive minima allow high GUI scales without losing the viewport. */
public record EditorDockLayout(int width,int left,int right,int minimumViewport) {
    public static EditorDockLayout resolve(int width,double leftRatio,double rightRatio){
        if(width<1)throw new IllegalArgumentException("Window width");
        if(!Double.isFinite(leftRatio))leftRatio=.19;if(!Double.isFinite(rightRatio))rightRatio=.25;
        int minView=Math.min(240,Math.max(80,width/3));minView=Math.min(minView,width/2);
        int minLeft=Math.min(180,width/5),minRight=Math.min(220,width/4);
        int left=Math.clamp((int)Math.round(width*leftRatio),minLeft,(int)(width*.4));
        int sizeRight=Math.clamp((int)Math.round(width*rightRatio),minRight,(int)(width*.45));
        int excess=Math.max(0,left+sizeRight+minView-width);
        int take=Math.min(excess,Math.max(0,left-minLeft));left-=take;excess-=take;sizeRight-=Math.min(excess,Math.max(0,sizeRight-minRight));
        return new EditorDockLayout(width,left,width-sizeRight,minView);
    }
    public EditorDockLayout dragLeft(double x){double ratio=Math.min(x,width-(width-right)-minimumViewport)/width;return resolve(width,ratio,rightRatio());}
    public EditorDockLayout dragRight(double x){double ratio=Math.min(width-x,width-left-minimumViewport)/width;return resolve(width,leftRatio(),ratio);}
    public double leftRatio(){return (double)left/width;}
    public double rightRatio(){return (double)(width-right)/width;}
}
