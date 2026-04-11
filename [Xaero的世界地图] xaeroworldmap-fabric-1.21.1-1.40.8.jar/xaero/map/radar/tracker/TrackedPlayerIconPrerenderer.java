package xaero.map.radar.tracker;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.class_1657;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_4587;
import net.minecraft.class_8251;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import xaero.map.graphics.ImprovedFramebuffer;
import xaero.map.icon.XaeroIcon;
import xaero.map.icon.XaeroIconAtlas;
import xaero.map.misc.Misc;

public class TrackedPlayerIconPrerenderer {
   private ImprovedFramebuffer renderFramebuffer;
   private XaeroIconAtlas lastAtlas;
   private final PlayerTrackerIconRenderer renderer = new PlayerTrackerIconRenderer();

   public void prerender(class_332 guiGraphics, XaeroIcon icon, class_1657 player, int iconWidth, class_2960 skinTextureLocation, PlayerTrackerMapElement<?> mapElement) {
      if (this.renderFramebuffer == null) {
         this.renderFramebuffer = new ImprovedFramebuffer(icon.getTextureAtlas().getWidth(), icon.getTextureAtlas().getWidth(), false);
         GlStateManager._deleteTexture(this.renderFramebuffer.method_30277());
         this.renderFramebuffer.setFramebufferTexture(0);
      }

      this.renderFramebuffer.bindAsMainTarget(false);
      GlStateManager._viewport(icon.getOffsetX(), icon.getOffsetY(), iconWidth, iconWidth);
      this.renderFramebuffer.setFramebufferTexture(icon.getTextureAtlas().getTextureId());
      this.renderFramebuffer.method_1239();
      if (this.lastAtlas != icon.getTextureAtlas()) {
         RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
         RenderSystem.clear(16384, class_310.field_1703);
         this.lastAtlas = icon.getTextureAtlas();
      }

      Matrix4f ortho = (new Matrix4f()).setOrtho(0.0F, (float)iconWidth, (float)iconWidth, 0.0F, -1.0F, 1000.0F);
      RenderSystem.setProjectionMatrix(ortho, class_8251.field_43361);
      Matrix4fStack shaderMatrixStack = RenderSystem.getModelViewStack();
      shaderMatrixStack.pushMatrix();
      shaderMatrixStack.identity();
      RenderSystem.applyModelViewMatrix();
      class_4587 matrixStack = guiGraphics.method_51448();
      matrixStack.method_22903();
      matrixStack.method_34426();
      matrixStack.method_46416((float)(iconWidth / 2), (float)(iconWidth / 2), 0.0F);
      matrixStack.method_22905(3.0F, 3.0F, 1.0F);
      guiGraphics.method_25294(-5, -5, 5, 5, -1);
      this.renderer.renderIcon(guiGraphics, player, skinTextureLocation);
      matrixStack.method_22909();
      class_310 mc = class_310.method_1551();
      Misc.minecraftOrtho(mc, false);
      shaderMatrixStack.popMatrix();
      RenderSystem.applyModelViewMatrix();
      this.renderFramebuffer.method_1240();
      this.renderFramebuffer.bindDefaultFramebuffer(mc);
      GlStateManager._viewport(0, 0, mc.method_22683().method_4489(), mc.method_22683().method_4506());
   }
}
