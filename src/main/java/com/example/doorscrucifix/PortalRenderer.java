package com.example.doorscrucifix;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;

/** Draws the green swirling Rick-style portal procedurally (no texture needed). */
public class PortalRenderer extends EntityRenderer<PortalEntity> {
    private static final int SEG = 40;
    private static final float RX = 0.9F;
    private static final float RY = 1.4F;

    public PortalRenderer(EntityRendererManager manager) {
        super(manager);
    }

    @Override
    public ResourceLocation getEntityTexture(PortalEntity entity) {
        return new ResourceLocation("minecraft", "textures/misc/white.png");
    }

    @Override
    public void render(PortalEntity entity, float yaw, float partialTicks, MatrixStack ms,
                       IRenderTypeBuffer buffer, int light) {
        float age = entity.ticksExisted + partialTicks;
        float fade = MathHelper.clamp(Math.min(age / 8.0F, (PortalEntity.LIFETIME - age) / 8.0F), 0.0F, 1.0F);
        if (fade <= 0.0F) return;
        float pulse = 0.85F + 0.15F * MathHelper.sin(age * 0.3F);

        ms.push();
        ms.rotate(Vector3f.YP.rotationDegrees(-yaw));
        ms.translate(0.0D, 1.5D, 0.0D);
        ms.scale(fade, fade, 1.0F);
        Matrix4f m = ms.getLast().getMatrix();
        IVertexBuilder vb = buffer.getBuffer(RenderType.getLightning());

        // 1) filled disc
        float[] centre = col(0.05F, 0.55F, 0.12F, 0.35F * fade);
        float[] edge = col(0.2F, 0.95F, 0.25F, 0.6F * fade);
        for (int i = 0; i < SEG; i++) {
            float a0 = (float) (Math.PI * 2.0 * i / SEG);
            float a1 = (float) (Math.PI * 2.0 * (i + 1) / SEG);
            float x0 = RX * MathHelper.cos(a0), y0 = RY * MathHelper.sin(a0);
            float x1 = RX * MathHelper.cos(a1), y1 = RY * MathHelper.sin(a1);
            quad(vb, m, 0.0F, 0, 0, centre, x0, y0, edge, x1, y1, edge, x1, y1, edge);
        }

        // 2) swirl arms
        for (int k = 0; k < 3; k++) {
            for (int s = 0; s < 16; s++) {
                float t0 = s / 16.0F, t1 = (s + 1) / 16.0F;
                float th0 = k * 2.0944F + t0 * 3.4F - age * 0.15F;
                float th1 = k * 2.0944F + t1 * 3.4F - age * 0.15F;
                float r0 = 0.1F + 0.8F * t0, r1 = 0.1F + 0.8F * t1;
                float w = 0.06F;
                float[] c0 = col(0.75F, 1.0F, 0.6F, (0.6F * (1.0F - t0) + 0.1F) * fade);
                float[] c1 = col(0.75F, 1.0F, 0.6F, (0.6F * (1.0F - t1) + 0.1F) * fade);
                quad(vb, m, 0.002F,
                        ex(r0 - w, th0), ey(r0 - w, th0), c0,
                        ex(r0 + w, th0), ey(r0 + w, th0), c0,
                        ex(r1 + w, th1), ey(r1 + w, th1), c1,
                        ex(r1 - w, th1), ey(r1 - w, th1), c1);
            }
        }

        // 3) bright rim and outer glow
        ring(vb, m, 0.004F, 0.80F, 1.0F, col(0.5F, 1.0F, 0.4F, 0.0F), col(0.85F, 1.0F, 0.65F, 0.95F * pulse * fade));
        ring(vb, m, 0.006F, 1.0F, 1.2F, col(0.5F, 1.0F, 0.4F, 0.8F * pulse * fade), col(0.2F, 1.0F, 0.2F, 0.0F));

        ms.pop();
    }

    private static float ex(float r, float th) {
        return RX * Math.max(r, 0.0F) * MathHelper.cos(th);
    }

    private static float ey(float r, float th) {
        return RY * Math.max(r, 0.0F) * MathHelper.sin(th);
    }

    private static float[] col(float r, float g, float b, float a) {
        return new float[]{r, g, b, a};
    }

    private static void ring(IVertexBuilder vb, Matrix4f m, float z, float rIn, float rOut, float[] cIn, float[] cOut) {
        for (int i = 0; i < SEG; i++) {
            float a0 = (float) (Math.PI * 2.0 * i / SEG);
            float a1 = (float) (Math.PI * 2.0 * (i + 1) / SEG);
            quad(vb, m, z,
                    ex(rIn, a0), ey(rIn, a0), cIn,
                    ex(rOut, a0), ey(rOut, a0), cOut,
                    ex(rOut, a1), ey(rOut, a1), cOut,
                    ex(rIn, a1), ey(rIn, a1), cIn);
        }
    }

    private static void v(IVertexBuilder vb, Matrix4f m, float x, float y, float z, float[] c) {
        vb.pos(m, x, y, z).color(c[0], c[1], c[2], c[3]).endVertex();
    }

    /** Emits a quad twice (front + back winding) so it is visible from both sides. */
    private static void quad(IVertexBuilder vb, Matrix4f m, float z,
                             float x1, float y1, float[] c1, float x2, float y2, float[] c2,
                             float x3, float y3, float[] c3, float x4, float y4, float[] c4) {
        v(vb, m, x1, y1, z, c1);
        v(vb, m, x2, y2, z, c2);
        v(vb, m, x3, y3, z, c3);
        v(vb, m, x4, y4, z, c4);
        v(vb, m, x4, y4, z, c4);
        v(vb, m, x3, y3, z, c3);
        v(vb, m, x2, y2, z, c2);
        v(vb, m, x1, y1, z, c1);
    }
}
