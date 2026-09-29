package dev.maplesadventure.client.editor;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.api.editor.client.MaplesEditorClientApi;
import dev.maplesadventure.authoring.*;
import dev.maplesadventure.authoring.component.BuiltinComponents;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class EditorWorldRenderer {
    private static Matrix4f matrix;
    private static Vec3 camera=Vec3.ZERO;
    private static UUID hovered;
    private static final Set<net.minecraft.resources.ResourceLocation> reported=new HashSet<>();
    public static void clear(){matrix=null;hovered=null;}
    public static void hover(double x,double y){hovered=pick(x,y);}
    public static void clearHover(){hovered=null;}
    public static EditorGeometry.Point project(Vec3 world){var w=Minecraft.getInstance().getWindow();return matrix==null?null:EditorGeometry.project(matrix,world.subtract(camera),w.getGuiScaledWidth(),w.getGuiScaledHeight());}
    public static EditorGeometry.Ray ray(double x,double y){var w=Minecraft.getInstance().getWindow();return matrix==null?null:EditorGeometry.ray(matrix,camera,x,y,w.getGuiScaledWidth(),w.getGuiScaledHeight());}
    public static double handleSize(Vec3 pivot){var a=project(pivot);var mc=Minecraft.getInstance();Vector3f right=new Vector3f(mc.gameRenderer.getMainCamera().getLeftVector()).negate();var b=project(pivot.add(right.x,right.y,right.z));
        if(a==null||b==null)return 1;return Math.clamp(64/Math.max(1,Math.hypot(b.x()-a.x(),b.y()-a.y())),.3,32);}
    public static UUID pick(double x,double y){
        if(EditorClient.scene()==null)return null;var ray=ray(x,y);if(ray==null)return null;UUID best=null;double distance=Double.POSITIVE_INFINITY;
        for(var o:EditorClient.scene().objects().values()){
            if(o.transform().position().distanceToSqr(camera)>128*128)continue;
            var transform=o.transform();var rotation=new Quaternionf().rotateY((float)Math.toRadians(-transform.yaw())).rotateX((float)Math.toRadians(transform.pitch()));
            var inverse=new Quaternionf(rotation).conjugate();Vec3 from=rotate(ray.origin().subtract(transform.position()),inverse),to=from.add(rotate(ray.direction(),inverse).scale(256));
            var bounds=new ArrayList<AABB>();bounds.add(new AABB(-.25,-.25,-.25,.25,.25,.25));
            for(var c:o.components().keySet()){
                var fields=EditorClient.fields(o.id(),c);
                if(fields.isEmpty())continue;
                if(c.equals(BuiltinComponents.RADIUS)&&fields.containsKey("radius")){double r=fields.get("radius").number();bounds.add(new AABB(-r,-r,-r,r,r,r));}
                if(c.equals(BuiltinComponents.BOX)&&fields.containsKey("sizeX")){double a=fields.get("sizeX").number()/2,b=fields.get("sizeY").number()/2,z=fields.get("sizeZ").number()/2;bounds.add(new AABB(-a,-b,-z,a,b,z));}
                MaplesEditorClientApi.gizmo(c).ifPresent(provider->{try{bounds.add(provider.bounds(fields));}catch(RuntimeException|LinkageError failure){warn(c,failure);}});
            }
            for(var box:bounds){var hit=box.clip(from,to);if(hit.isPresent()){double d=hit.get().distanceToSqr(from);if(d<distance){distance=d;best=o.id();}}}
        }return best;
    }
    private static Vec3 rotate(Vec3 p,Quaternionf q){var v=q.transform(new Vector3f((float)p.x,(float)p.y,(float)p.z));return new Vec3(v.x,v.y,v.z);}
    public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||!EditorClient.active())return;
        var mc=Minecraft.getInstance();var scene=EditorClient.scene();if(mc.level==null||scene==null||!scene.dimension().equals(mc.level.dimension().location()))return;
        camera=e.getCamera().getPosition();matrix=new Matrix4f(e.getProjectionMatrix()).mul(e.getModelViewMatrix());
        var buffers=mc.renderBuffers().bufferSource();var lines=buffers.getBuffer(RenderType.lines());var pose=e.getPoseStack();
        for(var o:scene.objects().values()){
            var t=o.transform();boolean selected=o.id().equals(EditorClient.selected());
            if(selected&&mc.screen instanceof EditorScreen screen&&screen.preview()!=null)t=screen.preview();
            var position=t.position();double extent=extent(o);
            if(position.distanceToSqr(camera)>128*128||!e.getFrustum().isVisible(new AABB(position.subtract(extent,extent,extent),position.add(extent,extent,extent))))continue;
            pose.pushPose();pose.translate(position.x-camera.x,position.y-camera.y,position.z-camera.z);pose.mulPose(Axis.YP.rotationDegrees(-t.yaw()));pose.mulPose(Axis.XP.rotationDegrees(t.pitch()));
            boolean hover=o.id().equals(hovered);
            float r=selected?1:hover?.8F:.4F,g=selected?.85F:hover?1:.75F,b=selected?.2F:1;
            var marker=EditorClient.fields(o.id(),BuiltinComponents.MARKER);
            if(!marker.containsKey("visible")||Boolean.parseBoolean(marker.get("visible").text())){
                line(pose,lines,-.25,0,0,.25,0,0,r,g,b);line(pose,lines,0,-.25,0,0,.25,0,r,g,b);line(pose,lines,0,0,-.25,0,0,.6,r,g,b);
                line(pose,lines,0,0,.6,-.12,0,.42,r,g,b);line(pose,lines,0,0,.6,.12,0,.42,r,g,b);
            }
            for(var c:o.components().keySet()){
                var values=EditorClient.fields(o.id(),c);
                if(values.isEmpty())continue;
                if(c.equals(BuiltinComponents.BOX)&&values.containsKey("sizeX")){double x=values.get("sizeX").number()/2,y=values.get("sizeY").number()/2,z=values.get("sizeZ").number()/2;LevelRenderer.renderLineBox(pose,lines,-x,-y,-z,x,y,z,r,g,b,1);}
                if(c.equals(BuiltinComponents.RADIUS)&&values.containsKey("radius")){double radius=values.get("radius").number();for(int plane=0;plane<3;plane++)for(int i=0;i<48;i++){
                    double a=i*Math.PI/24,n=(i+1)*Math.PI/24,x=Math.cos(a)*radius,y=Math.sin(a)*radius,xx=Math.cos(n)*radius,yy=Math.sin(n)*radius;
                    line(pose,lines,plane==2?0:x,plane==0?0:(plane==1?y:x),plane==0?y:(plane==2?y:0),plane==2?0:xx,plane==0?0:(plane==1?yy:xx),plane==0?yy:(plane==2?yy:0),r,g,b);
                }}
                MaplesEditorClientApi.gizmo(c).ifPresent(provider->{pose.pushPose();try{provider.render(pose,lines,values,selected);}catch(RuntimeException|LinkageError failure){warn(c,failure);}finally{pose.popPose();}});
            }pose.popPose();
        }buffers.endBatch(RenderType.lines());
    }
    private static void warn(net.minecraft.resources.ResourceLocation id,Throwable error){if(reported.add(id))MaplesAdventure.LOGGER.warn("Editor gizmo failed {}",id,error);}
    private static double extent(MaplesObject object){
        double extent=1;
        for(var type:object.components().keySet()){
            var fields=EditorClient.fields(object.id(),type);if(fields.isEmpty())continue;
            if(type.equals(BuiltinComponents.RADIUS)&&fields.containsKey("radius"))extent=Math.max(extent,fields.get("radius").number());
            if(type.equals(BuiltinComponents.BOX)&&fields.containsKey("sizeX")){
                double x=fields.get("sizeX").number()/2,y=fields.get("sizeY").number()/2,z=fields.get("sizeZ").number()/2;
                extent=Math.max(extent,Math.sqrt(x*x+y*y+z*z));
            }
            var provider=MaplesEditorClientApi.gizmo(type);if(provider.isPresent())try{
                var b=provider.get().bounds(fields);double x=Math.max(Math.abs(b.minX),Math.abs(b.maxX)),y=Math.max(Math.abs(b.minY),Math.abs(b.maxY)),z=Math.max(Math.abs(b.minZ),Math.abs(b.maxZ));
                if(Double.isFinite(x+y+z))extent=Math.max(extent,Math.sqrt(x*x+y*y+z*z));
            }catch(RuntimeException|LinkageError error){warn(type,error);}
        }return extent;
    }
    public static void line(PoseStack p,VertexConsumer v,double x,double y,double z,double a,double b,double c,float red,float green,float blue){
        var normal=new Vector3f((float)(a-x),(float)(b-y),(float)(c-z));if(normal.lengthSquared()<1e-12)return;normal.normalize();
        v.addVertex(p.last(),(float)x,(float)y,(float)z).setColor(red,green,blue,1).setNormal(p.last(),normal.x,normal.y,normal.z);
        v.addVertex(p.last(),(float)a,(float)b,(float)c).setColor(red,green,blue,1).setNormal(p.last(),normal.x,normal.y,normal.z);
    }
    private EditorWorldRenderer(){}
}
