package xaero.map.mods.pac.gui.claim.element;

import java.util.Iterator;
import net.minecraft.class_1060;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_4587;
import net.minecraft.class_4597.class_4598;
import xaero.map.WorldMap;
import xaero.map.element.MapElementRenderer;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.graphics.CustomRenderTypes;
import xaero.map.graphics.MapRenderHelper;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.map.mods.pac.gui.claim.ClaimResultElement;
import xaero.map.mods.pac.gui.claim.ClaimResultElementManager;

public class ClaimResultElementRenderer extends MapElementRenderer<ClaimResultElement, ClaimResultElementRenderContext, ClaimResultElementRenderer> {
   private final ClaimResultElementManager manager;

   private ClaimResultElementRenderer(ClaimResultElementManager manager, ClaimResultElementRenderContext context, ClaimResultElementRenderProvider provider, ClaimResultElementRenderReader reader) {
      super(context, provider, reader);
      this.manager = manager;
   }

   public void preRender(ElementRenderInfo renderInfo, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, boolean shadow) {
      class_4598 renderTypeBuffers = WorldMap.worldMapClientOnly.customVertexConsumers.getRenderTypeBuffers();
      ((ClaimResultElementRenderContext)this.context).guiIconBuffer = renderTypeBuffers.getBuffer(CustomRenderTypes.GUI_BILINEAR);
      ((ClaimResultElementRenderContext)this.context).toDelete.clear();
   }

   public void postRender(ElementRenderInfo renderInfo, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, boolean shadow) {
      class_4598 renderTypeBuffers = WorldMap.worldMapClientOnly.customVertexConsumers.getRenderTypeBuffers();
      renderTypeBuffers.method_22993();
      Iterator var6 = ((ClaimResultElementRenderContext)this.context).toDelete.iterator();

      while(var6.hasNext()) {
         ClaimResultElement element = (ClaimResultElement)var6.next();
         this.manager.remove(element);
      }

   }

   public void renderElementShadow(ClaimResultElement element, boolean hovered, float optionalScale, double partialX, double partialY, ElementRenderInfo renderInfo, class_332 guiGraphics, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider) {
   }

   public boolean renderElement(ClaimResultElement element, boolean hovered, double optionalDepth, float optionalScale, double partialX, double partialY, ElementRenderInfo renderInfo, class_332 guiGraphics, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider) {
      class_4587 matrixStack = guiGraphics.method_51448();
      long time = System.currentTimeMillis();
      int iconScale = (int)Math.ceil((double)optionalScale);
      matrixStack.method_22904(partialX, partialY, 0.0D);
      matrixStack.method_22905((float)iconScale, (float)iconScale, 1.0F);
      int iconU = element.hasPositive() ? 0 : 32;
      int iconV = 78;
      float r;
      float g;
      float b;
      if (element.hasPositive() == element.hasNegative()) {
         r = 1.0F;
         g = 0.6666667F;
         b = 0.0F;
      } else if (element.hasPositive()) {
         r = 0.0F;
         g = 0.6666667F;
         b = 0.0F;
      } else {
         r = 0.8F;
         g = 0.1F;
         b = 0.1F;
      }

      MapRenderHelper.blitIntoExistingBuffer(matrixStack.method_23760().method_23761(), ((ClaimResultElementRenderContext)this.context).guiIconBuffer, -16.0F, -16.0F, iconU, iconV, 32, 32, 32, 32, r, g, b, 1.0F, 256, 256);
      if (hovered) {
         element.setFadeOutStartTime(time);
      }

      if (time - element.getFadeOutStartTime() > 3000L) {
         ((ClaimResultElementRenderContext)this.context).toDelete.add(element);
      }

      return false;
   }

   /** @deprecated */
   @Deprecated
   public void beforeRender(int location, class_310 mc, class_332 guiGraphics, double cameraX, double cameraZ, double mouseX, double mouseZ, float brightness, double scale, double screenSizeBasedScale, class_1060 textureManager, class_327 fontRenderer, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, boolean pre) {
      this.preRender((ElementRenderInfo)null, vanillaBufferSource, rendererProvider, pre);
   }

   /** @deprecated */
   @Deprecated
   public void afterRender(int location, class_310 mc, class_332 guiGraphics, double cameraX, double cameraZ, double mouseX, double mouseZ, float brightness, double scale, double screenSizeBasedScale, class_1060 textureManager, class_327 fontRenderer, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, boolean pre) {
      this.postRender((ElementRenderInfo)null, vanillaBufferSource, rendererProvider, pre);
   }

   /** @deprecated */
   @Deprecated
   public void renderElementPre(int location, ClaimResultElement element, boolean hovered, class_310 mc, class_332 guiGraphics, double cameraX, double cameraZ, double mouseX, double mouseZ, float brightness, double scale, double screenSizeBasedScale, class_1060 textureManager, class_327 fontRenderer, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, float optionalScale, double partialX, double partialY, boolean cave, float partialTicks) {
      this.renderElementShadow((ClaimResultElement)element, hovered, optionalScale, partialX, partialY, (ElementRenderInfo)null, guiGraphics, vanillaBufferSource, rendererProvider);
   }

   /** @deprecated */
   @Deprecated
   public boolean renderElement(int location, ClaimResultElement element, boolean hovered, class_310 mc, class_332 guiGraphics, double cameraX, double cameraZ, double mouseX, double mouseZ, float brightness, double scale, double screenSizeBasedScale, class_1060 textureManager, class_327 fontRenderer, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, int elementIndex, double optionalDepth, float optionalScale, double partialX, double partialY, boolean cave, float partialTicks) {
      return this.renderElement((ClaimResultElement)element, hovered, optionalDepth, optionalScale, partialX, partialY, (ElementRenderInfo)null, guiGraphics, vanillaBufferSource, rendererProvider);
   }

   public boolean shouldRender(int location, boolean shadow) {
      return true;
   }

   public int getOrder() {
      return 150;
   }

   public static final class Builder {
      private ClaimResultElementManager manager;

      private Builder() {
      }

      private ClaimResultElementRenderer.Builder setDefault() {
         this.setManager((ClaimResultElementManager)null);
         return this;
      }

      public ClaimResultElementRenderer.Builder setManager(ClaimResultElementManager manager) {
         this.manager = manager;
         return this;
      }

      public ClaimResultElementRenderer build() {
         if (this.manager == null) {
            throw new IllegalStateException();
         } else {
            return new ClaimResultElementRenderer(this.manager, new ClaimResultElementRenderContext(), new ClaimResultElementRenderProvider(this.manager), new ClaimResultElementRenderReader());
         }
      }

      public static ClaimResultElementRenderer.Builder begin() {
         return (new ClaimResultElementRenderer.Builder()).setDefault();
      }
   }
}
