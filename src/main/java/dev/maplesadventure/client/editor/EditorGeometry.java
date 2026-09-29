package dev.maplesadventure.client.editor;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Pure camera-relative math shared by picking, dragging and tests. */
public final class EditorGeometry {
    public record Ray(Vec3 origin,Vec3 direction){}
    public record Point(double x,double y){}
    public static Point project(Matrix4f matrix,Vec3 relative,int width,int height){
        var v=matrix.transform(new Vector4f((float)relative.x,(float)relative.y,(float)relative.z,1));
        if(v.w<=.001||!Float.isFinite(v.w))return null;
        return new Point((v.x/v.w+1)*width*.5,(1-v.y/v.w)*height*.5);
    }
    public static Ray ray(Matrix4f matrix,Vec3 camera,double x,double y,int width,int height){
        Matrix4f inverse=new Matrix4f(matrix).invert();float nx=(float)(2*x/width-1),ny=(float)(1-2*y/height);
        var near=inverse.transform(new Vector4f(nx,ny,-1,1));var far=inverse.transform(new Vector4f(nx,ny,1,1));near.div(near.w);far.div(far.w);
        Vec3 from=new Vec3(near.x,near.y,near.z).add(camera),to=new Vec3(far.x,far.y,far.z).add(camera);
        return new Ray(from,to.subtract(from).normalize());
    }
    public static double axisParameter(Ray ray,Vec3 origin,Vec3 axis){
        double b=axis.dot(ray.direction()),denominator=1-b*b;if(denominator<.0025)return Double.NaN;
        var offset=ray.origin().subtract(origin);return (axis.dot(offset)-b*ray.direction().dot(offset))/denominator;
    }
    public static double yawAngle(Ray ray,Vec3 pivot){
        if(Math.abs(ray.direction().y)<.02)return Double.NaN;
        double t=(pivot.y-ray.origin().y)/ray.direction().y;if(t<0)return Double.NaN;
        Vec3 point=ray.origin().add(ray.direction().scale(t)).subtract(pivot);
        return Math.toDegrees(Math.atan2(point.z,point.x));
    }
    public static double segmentDistance(double x,double y,Point a,Point b){
        double dx=b.x-a.x,dy=b.y-a.y,den=dx*dx+dy*dy;if(den<4)return Double.POSITIVE_INFINITY;
        double t=Math.clamp(((x-a.x)*dx+(y-a.y)*dy)/den,0,1);return Math.hypot(x-a.x-t*dx,y-a.y-t*dy);
    }
    public static float yaw(float value){return (float)(((value+180)%360+360)%360-180);}
    private EditorGeometry(){}
}
