package com.deltasf.createpropulsion.utility;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.optical_sensors.rendering.OpticalSensorBeamRenderType;
import com.deltasf.createpropulsion.optical_sensors.rendering.OpticalSensorRenderer.BeamRenderData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreatePropulsion.ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TranslucentGeometryRenderer {
    private static final Queue<BeamRenderData> RENDER_QUEUE = new ConcurrentLinkedQueue<>();

    public static void scheduleBeamRender(BeamRenderData data) {
        RENDER_QUEUE.offer(data);
    }
    

    @SubscribeEvent
    public static void onRenderLevelStageEnd(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            if (RENDER_QUEUE.isEmpty()) {
                return; // Nothing to render
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null || mc.gameRenderer == null) { // Basic safety checks
                return;
            }
            PoseStack eventPoseStack = event.getPoseStack();
            MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

            VertexConsumer buffer = bufferSource.getBuffer(OpticalSensorBeamRenderType.SOLID_TRANSLUCENT_BEAM);

            //Render all invoked
            for (BeamRenderData data : RENDER_QUEUE) {
                eventPoseStack.pushPose();
                drawBeam(data.poseSnapshot, buffer, data);
                eventPoseStack.popPose();
            }

            RENDER_QUEUE.clear();
        }
    }

    private static void drawBeam(PoseStack.Pose poseStack, VertexConsumer buffer, BeamRenderData data){
        Matrix4f pose = poseStack.pose();

        Vector3f normalBottom = new Vector3f(data.upVector);
        Vector3f normalRight = new Vector3f(data.sideVector).negate();
        Vector3f normalTop = new Vector3f(data.upVector).negate();
        Vector3f normalLeft = new Vector3f(data.sideVector);

        //Rendering
        drawQuad(buffer, pose, data.sBottomLeft, data.sBottomRight, data.eBottomRight, data.eBottomLeft, data.startColor, data.endColor, normalBottom);
        drawQuad(buffer, pose, data.sBottomRight, data.sTopRight, data.eTopRight, data.eBottomRight, data.startColor, data.endColor, normalRight);
        drawQuad(buffer, pose, data.sTopRight, data.sTopLeft, data.eTopLeft, data.eTopRight, data.startColor, data.endColor, normalTop);
        drawQuad(buffer, pose, data.sTopLeft, data.sBottomLeft, data.eBottomLeft, data.eTopLeft, data.startColor, data.endColor, normalLeft);
    }

    private static void drawQuad(VertexConsumer buffer, Matrix4f pose,
                          Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4,
                          Vector4f startColor, Vector4f endColor, Vector3f normal) {

        // Vertex 1 (Start)
        buffer.vertex(pose, v1.x(), v1.y(), v1.z())
              .color(startColor.x(), startColor.y(), startColor.z(), startColor.w())
              .normal(normal.x(), normal.y(), normal.z())
              .endVertex();

        // Vertex 2 (Start)
        buffer.vertex(pose, v2.x(), v2.y(), v2.z())
              .color(startColor.x(), startColor.y(), startColor.z(), startColor.w())
              .normal(normal.x(), normal.y(), normal.z())
              .endVertex();

        // Vertex 3 (End)
        buffer.vertex(pose, v3.x(), v3.y(), v3.z())
              .color(endColor.x(), endColor.y(), endColor.z(), endColor.w())
              .normal(normal.x(), normal.y(), normal.z())
              .endVertex();

        // Vertex 4 (End)
        buffer.vertex(pose, v4.x(), v4.y(), v4.z())
              .color(endColor.x(), endColor.y(), endColor.z(), endColor.w())
              .normal(normal.x(), normal.y(), normal.z())
              .endVertex();
    }
}
