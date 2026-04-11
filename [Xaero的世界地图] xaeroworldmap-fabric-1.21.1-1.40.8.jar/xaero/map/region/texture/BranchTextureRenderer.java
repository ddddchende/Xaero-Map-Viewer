package xaero.map.region.texture;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.class_276;
import net.minecraft.class_285;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_8251;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import xaero.map.exception.OpenGLException;
import xaero.map.graphics.ImprovedFramebuffer;
import xaero.map.graphics.MapRenderHelper;
import xaero.map.misc.Misc;

public class BranchTextureRenderer {
   private ImprovedFramebuffer renderFBO;
   private int glEmptyTexture;
   private class_4587 matrixStack = new class_4587();
   private Matrix4f projectionMatrix = (new Matrix4f()).setOrtho(0.0F, 64.0F, 64.0F, 0.0F, -1.0F, 1.0F);

   public void render(int destTexture, Integer srcTextureTopLeft, Integer srcTextureTopRight, Integer srcTextureBottomLeft, Integer srcTextureBottomRight, class_276 defaultFramebuffer, boolean justAllocated) {
      if (this.renderFBO == null) {
         this.renderFBO = new ImprovedFramebuffer(64, 64, false);
         this.glEmptyTexture = this.renderFBO.getFramebufferTexture();
         this.renderFBO.bindAsMainTarget(true);
         GlStateManager._disableBlend();
         GlStateManager._clearColor(0.0F, 0.0F, 0.0F, 0.0F);
         GlStateManager._clear(16384, class_310.field_1703);
         GlStateManager._enableBlend();
      }

      GlStateManager._bindTexture(0);
      this.renderFBO.bindAsMainTarget(true);
      this.renderFBO.setFramebufferTexture(destTexture);
      OpenGLException.checkGLError();
      Matrix4fStack shaderMatrixStack = RenderSystem.getModelViewStack();
      shaderMatrixStack.pushMatrix();
      shaderMatrixStack.identity();
      RenderSystem.applyModelViewMatrix();
      RenderSystem.setProjectionMatrix(this.projectionMatrix, class_8251.field_43361);
      GlStateManager._disableBlend();
      if (justAllocated) {
         GlStateManager._clearColor(0.0F, 0.0F, 0.0F, 1.0F);
         GlStateManager._clear(16384, class_310.field_1703);
      }

      boolean first = true;
      if (srcTextureTopLeft != null) {
         first = this.renderCorner(srcTextureTopLeft, 0, 0, first);
      }

      if (srcTextureTopRight != null) {
         first = this.renderCorner(srcTextureTopRight, 1, 0, first);
      }

      if (srcTextureBottomLeft != null) {
         first = this.renderCorner(srcTextureBottomLeft, 0, 1, first);
      }

      if (srcTextureBottomRight != null) {
         this.renderCorner(srcTextureBottomRight, 1, 1, first);
      }

      OpenGLException.checkGLError(false, "updating a map branch texture");
      GlStateManager._enableBlend();
      class_285.method_22094(0);
      GlStateManager._bindTexture(0);
      shaderMatrixStack.popMatrix();
      RenderSystem.applyModelViewMatrix();
      class_310 mc = class_310.method_1551();
      Misc.minecraftOrtho(mc, false);
      this.renderFBO.method_1240();
      this.renderFBO.bindDefaultFramebuffer(mc);
      GlStateManager._viewport(0, 0, mc.method_22683().method_4489(), mc.method_22683().method_4506());
      OpenGLException.checkGLError();
   }

   private boolean renderCorner(Integer srcTexture, int cornerX, int cornerY, boolean first) {
      int xOffset = cornerX * 32;
      int yOffset = (1 - cornerY) * 32;
      int texture = srcTexture != -1 ? srcTexture : this.glEmptyTexture;
      if (first) {
         RenderSystem.setShaderTexture(0, texture);
      } else {
         GlStateManager._activeTexture(33984);
         GlStateManager._bindTexture(texture);
      }

      MapRenderHelper.renderBranchUpdate((float)xOffset, (float)yOffset, 32.0F, 32.0F, 0, 64, 64.0F, -64.0F, 64.0F, 64.0F, first);
      return false;
   }
}
