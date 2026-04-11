package xaero.map.element;

import net.minecraft.class_1060;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_4597.class_4598;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;

public final class MenuOnlyElementRenderer<E> extends MapElementRenderer<E, Object, MenuOnlyElementRenderer<E>> {
   protected MenuOnlyElementRenderer(MenuOnlyElementReader<E> reader) {
      super((Object)null, (MapElementRenderProvider)null, reader);
   }

   public boolean shouldRender(int location, boolean shadow) {
      return false;
   }

   /** @deprecated */
   @Deprecated
   public void beforeRender(int location, class_310 mc, class_332 guiGraphics, double cameraX, double cameraZ, double mouseX, double mouseZ, float brightness, double scale, double screenSizeBasedScale, class_1060 textureManager, class_327 fontRenderer, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, boolean pre) {
   }

   /** @deprecated */
   @Deprecated
   public void afterRender(int location, class_310 mc, class_332 guiGraphics, double cameraX, double cameraZ, double mouseX, double mouseZ, float brightness, double scale, double screenSizeBasedScale, class_1060 textureManager, class_327 fontRenderer, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, boolean pre) {
   }

   /** @deprecated */
   @Deprecated
   public void renderElementPre(int location, E element, boolean hovered, class_310 mc, class_332 guiGraphics, double cameraX, double cameraZ, double mouseX, double mouseZ, float brightness, double scale, double screenSizeBasedScale, class_1060 textureManager, class_327 fontRenderer, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, float optionalScale, double partialX, double partialY, boolean cave, float partialTicks) {
   }

   /** @deprecated */
   @Deprecated
   public boolean renderElement(int location, E element, boolean hovered, class_310 mc, class_332 guiGraphics, double cameraX, double cameraZ, double mouseX, double mouseZ, float brightness, double scale, double screenSizeBasedScale, class_1060 textureManager, class_327 fontRenderer, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, int elementIndex, double optionalDepth, float optionalScale, double partialX, double partialY, boolean cave, float partialTicks) {
      return false;
   }

   public void preRender(ElementRenderInfo renderInfo, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, boolean shadow) {
   }

   public void postRender(ElementRenderInfo renderInfo, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, boolean shadow) {
   }

   public void renderElementShadow(E element, boolean hovered, float optionalScale, double partialX, double partialY, ElementRenderInfo renderInfo, class_332 guiGraphics, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider) {
   }

   public boolean renderElement(E element, boolean hovered, double optionalDepth, float optionalScale, double partialX, double partialY, ElementRenderInfo renderInfo, class_332 guiGraphics, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider) {
      return false;
   }
}
