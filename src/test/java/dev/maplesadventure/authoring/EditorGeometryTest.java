package dev.maplesadventure.authoring;

import dev.maplesadventure.client.editor.EditorGeometry;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EditorGeometryTest {
    @Test void projectionAndUnprojectionRespectGuiScaleAndCameraOffset(){
        var matrix=new Matrix4f().perspective((float)Math.toRadians(70),16F/9,.05F,512);Vec3 camera=new Vec3(100,70,-50),point=new Vec3(1,2,-12);
        for(int w:new int[]{480,960,1920}){int h=w*9/16;var projected=EditorGeometry.project(matrix,point,w,h);var ray=EditorGeometry.ray(matrix,camera,projected.x(),projected.y(),w,h);var delta=point.add(camera).subtract(ray.origin()).normalize();assertEquals(1,delta.dot(ray.direction()),1e-6);}
    }
    @Test void axisAndYawDegeneracyDoNotJump(){
        assertTrue(Double.isNaN(EditorGeometry.axisParameter(new EditorGeometry.Ray(Vec3.ZERO,new Vec3(1,0,0)),new Vec3(2,0,0),new Vec3(1,0,0))));
        assertEquals(3,EditorGeometry.axisParameter(new EditorGeometry.Ray(new Vec3(3,4,2),new Vec3(0,-1,0)),Vec3.ZERO,new Vec3(1,0,0)),1e-9);
        assertTrue(Double.isNaN(EditorGeometry.yawAngle(new EditorGeometry.Ray(Vec3.ZERO,new Vec3(1,0,0)),Vec3.ZERO)));
        assertEquals(90,EditorGeometry.yawAngle(new EditorGeometry.Ray(new Vec3(0,2,3),new Vec3(0,-1,0)),Vec3.ZERO),1e-9);
        assertEquals(-170,EditorGeometry.yaw(190));
    }
}
