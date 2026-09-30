package dev.maplesadventure.authoring.logic;
import dev.maplesadventure.authoring.*;
import dev.maplesadventure.authoring.component.BuiltinComponents;
import dev.maplesadventure.editor.EditorFoundation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/** Same center/rotation convention as the existing volume gizmos. Box and sphere form a union. */
public record TriggerVolume(EditorTransform transform,Vec3 halfBox,double radius,double coverage) {
    public static TriggerVolume of(MaplesObject o){Vec3 half=null;double radius=-1;
        var box=o.components().get(BuiltinComponents.BOX);var sphere=o.components().get(BuiltinComponents.RADIUS);
        if(box!=null){var f=EditorFoundation.COMPONENTS.fields(box);half=new Vec3(f.get("sizeX").number()/2,f.get("sizeY").number()/2,f.get("sizeZ").number()/2);}
        if(sphere!=null)radius=EditorFoundation.COMPONENTS.fields(sphere).get("radius").number();
        return half==null&&radius<0?null:new TriggerVolume(o.transform(),half,radius,Math.max(radius,half==null?0:half.length()));
    }
    public boolean contains(Vec3 position){Vec3 p=position.subtract(transform.position());
        if(radius>=0&&p.lengthSqr()<=radius*radius)return true;if(halfBox==null)return false;
        var inverse=new Quaterniond().rotateY(Math.toRadians(-transform.yaw())).rotateX(Math.toRadians(transform.pitch())).conjugate();
        var local=inverse.transform(new Vector3d(p.x,p.y,p.z));
        return Math.abs(local.x)<=halfBox.x&&Math.abs(local.y)<=halfBox.y&&Math.abs(local.z)<=halfBox.z;
    }
}
