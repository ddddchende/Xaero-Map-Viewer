package xaero.map.mods.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.class_1044;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_4587;
import net.minecraft.class_8251;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import xaero.map.exception.OpenGLException;
import xaero.map.graphics.ImprovedFramebuffer;
import xaero.map.icon.XaeroIcon;
import xaero.map.icon.XaeroIconAtlas;
import xaero.map.icon.XaeroIconAtlasManager;
import xaero.map.misc.Misc;

public class WaypointSymbolCreator {
   private static final int PREFERRED_ATLAS_WIDTH = 1024;
   private static final int ICON_WIDTH = 64;
   public static final class_2960 minimapTextures = class_2960.method_60655("xaerobetterpvp", "gui/guis.png");
   public static final int white = -1;
   private class_310 mc = class_310.method_1551();
   private XaeroIcon deathSymbolTexture;
   private final Map<String, XaeroIcon> charSymbols = new HashMap();
   private XaeroIconAtlasManager iconManager;
   private ImprovedFramebuffer atlasRenderFramebuffer;
   private XaeroIconAtlas lastAtlas;

   public XaeroIcon getDeathSymbolTexture(class_332 guiGraphics) {
      if (this.deathSymbolTexture == null) {
         this.createDeathSymbolTexture(guiGraphics);
      }

      return this.deathSymbolTexture;
   }

   private void createDeathSymbolTexture(class_332 guiGraphics) {
      this.deathSymbolTexture = this.createCharSymbol(guiGraphics, true, (String)null);
   }

   public XaeroIcon getSymbolTexture(class_332 guiGraphics, String c) {
      XaeroIcon icon;
      synchronized(this.charSymbols) {
         icon = (XaeroIcon)this.charSymbols.get(c);
      }

      if (icon == null) {
         icon = this.createCharSymbol(guiGraphics, false, c);
      }

      return icon;
   }

   private XaeroIcon createCharSymbol(class_332 guiGraphics, boolean death, String c) {
      if (this.iconManager == null) {
         OpenGLException.checkGLError();
         int maxTextureSize = GlStateManager._getInteger(3379);
         OpenGLException.checkGLError();
         int atlasTextureSize = Math.min(maxTextureSize, 1024) / 64 * 64;
         this.atlasRenderFramebuffer = new ImprovedFramebuffer(atlasTextureSize, atlasTextureSize, false);
         OpenGLException.checkGLError();
         GlStateManager._deleteTexture(this.atlasRenderFramebuffer.getFramebufferTexture());
         OpenGLException.checkGLError();
         this.atlasRenderFramebuffer.setFramebufferTexture(0);
         this.iconManager = new XaeroIconAtlasManager(64, atlasTextureSize, new ArrayList());
      }

      XaeroIconAtlas atlas = this.iconManager.getCurrentAtlas();
      XaeroIcon icon = atlas.createIcon();
      this.atlasRenderFramebuffer.bindAsMainTarget(false);
      GlStateManager._viewport(icon.getOffsetX(), icon.getOffsetY(), 64, 64);
      this.atlasRenderFramebuffer.setFramebufferTexture(atlas.getTextureId());
      this.atlasRenderFramebuffer.method_1239();
      if (this.lastAtlas != atlas) {
         RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
         RenderSystem.clear(16384, class_310.field_1703);
         this.lastAtlas = atlas;
      }

      Matrix4f ortho = (new Matrix4f()).setOrtho(0.0F, 64.0F, 64.0F, 0.0F, -1.0F, 1000.0F);
      RenderSystem.setProjectionMatrix(ortho, class_8251.field_43361);
      Matrix4fStack shaderMatrixStack = RenderSystem.getModelViewStack();
      shaderMatrixStack.pushMatrix();
      shaderMatrixStack.identity();
      RenderSystem.applyModelViewMatrix();
      class_4587 matrixStack = guiGraphics.method_51448();
      matrixStack.method_22903();
      matrixStack.method_34426();
      matrixStack.method_46416(2.0F, 2.0F, 0.0F);
      if (!death) {
         matrixStack.method_22905(3.0F, 3.0F, 1.0F);
         guiGraphics.method_25303(this.mc.field_1772, c, 0, 0, -1);
      } else {
         matrixStack.method_22905(3.0F, 3.0F, 1.0F);
         class_1044 texture = this.mc.method_1531().method_4619(minimapTextures);
         texture.method_4527(false, false);
         RenderSystem.setShaderColor(0.2431F, 0.2431F, 0.2431F, 1.0F);
         guiGraphics.method_25291(minimapTextures, 1, 1, 0, 0.0F, 78.0F, 9, 9, 256, 256);
         RenderSystem.setShaderColor(0.9882F, 0.9882F, 0.9882F, 1.0F);
         guiGraphics.method_25291(minimapTextures, 0, 0, 0, 0.0F, 78.0F, 9, 9, 256, 256);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }

      matrixStack.method_22909();
      Misc.minecraftOrtho(this.mc, false);
      shaderMatrixStack.popMatrix();
      RenderSystem.applyModelViewMatrix();
      this.atlasRenderFramebuffer.method_1240();
      this.atlasRenderFramebuffer.bindDefaultFramebuffer(this.mc);
      GlStateManager._viewport(0, 0, this.mc.method_22683().method_4489(), this.mc.method_22683().method_4506());
      if (death) {
         this.deathSymbolTexture = icon;
      } else {
         synchronized(this.charSymbols) {
            this.charSymbols.put(c, icon);
         }
      }

      return icon;
   }

   public void resetChars() {
      synchronized(this.charSymbols) {
         this.charSymbols.clear();
      }

      this.lastAtlas = null;
      this.deathSymbolTexture = null;
      if (this.iconManager != null) {
         this.iconManager.clearAtlases();
         this.atlasRenderFramebuffer.setFramebufferTexture(0);
      }

   }
}
