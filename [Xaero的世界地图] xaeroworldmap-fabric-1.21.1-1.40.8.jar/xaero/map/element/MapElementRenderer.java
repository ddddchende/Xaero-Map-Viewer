package xaero.map.element;

import net.minecraft.class_1060;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_4597.class_4598;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;

public abstract class MapElementRenderer<E, C, R extends MapElementRenderer<E, C, R>> extends ElementRenderer<E, C, R> {
   protected MapElementRenderer(C context, MapElementRenderProvider<E, C> provider, MapElementReader<E, C, R> reader) {
      super(context, provider, reader);
   }

   public void preRender(ElementRenderInfo renderInfo, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, boolean shadow) {
      this.beforeRender(renderInfo.location.getIndex(), class_310.method_1551(), (class_332)null, renderInfo.renderPos.field_1352, renderInfo.renderPos.field_1350, renderInfo.mouseX, renderInfo.mouseZ, renderInfo.brightness, renderInfo.scale, renderInfo.screenSizeBasedScale, class_310.method_1551().method_1531(), class_310.method_1551().field_1772, vanillaBufferSource, rendererProvider, shadow);
   }

   public void postRender(ElementRenderInfo renderInfo, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, boolean shadow) {
      this.afterRender(renderInfo.location.getIndex(), class_310.method_1551(), (class_332)null, renderInfo.renderPos.field_1352, renderInfo.renderPos.field_1350, renderInfo.mouseX, renderInfo.mouseZ, renderInfo.brightness, renderInfo.scale, renderInfo.screenSizeBasedScale, class_310.method_1551().method_1531(), class_310.method_1551().field_1772, vanillaBufferSource, rendererProvider, shadow);
   }

   public void renderElementShadow(E element, boolean hovered, float optionalScale, double partialX, double partialY, ElementRenderInfo renderInfo, class_332 guiGraphics, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider) {
      this.renderElementPre(renderInfo.location.getIndex(), element, hovered, class_310.method_1551(), guiGraphics, renderInfo.renderPos.field_1352, renderInfo.renderPos.field_1350, renderInfo.mouseX, renderInfo.mouseZ, renderInfo.brightness, renderInfo.scale, renderInfo.screenSizeBasedScale, class_310.method_1551().method_1531(), class_310.method_1551().field_1772, vanillaBufferSource, rendererProvider, optionalScale, partialX, partialY, renderInfo.cave, renderInfo.partialTicks);
   }

   public boolean renderElement(E element, boolean hovered, double optionalDepth, float optionalScale, double partialX, double partialY, ElementRenderInfo renderInfo, class_332 guiGraphics, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider) {
      return this.renderElement(renderInfo.location.getIndex(), element, hovered, class_310.method_1551(), guiGraphics, renderInfo.renderPos.field_1352, renderInfo.renderPos.field_1350, renderInfo.mouseX, renderInfo.mouseZ, renderInfo.brightness, renderInfo.scale, renderInfo.screenSizeBasedScale, class_310.method_1551().method_1531(), class_310.method_1551().field_1772, vanillaBufferSource, rendererProvider, 0, optionalDepth, optionalScale, partialX, partialY, renderInfo.cave, renderInfo.partialTicks);
   }

   public boolean shouldRender(ElementRenderLocation location, boolean shadow) {
      return this.shouldRender(location.getIndex(), shadow);
   }

   /** @deprecated */
   @Deprecated
   public abstract void beforeRender(int var1, class_310 var2, class_332 var3, double var4, double var6, double var8, double var10, float var12, double var13, double var15, class_1060 var17, class_327 var18, class_4598 var19, MultiTextureRenderTypeRendererProvider var20, boolean var21);

   /** @deprecated */
   @Deprecated
   public abstract void afterRender(int var1, class_310 var2, class_332 var3, double var4, double var6, double var8, double var10, float var12, double var13, double var15, class_1060 var17, class_327 var18, class_4598 var19, MultiTextureRenderTypeRendererProvider var20, boolean var21);

   /** @deprecated */
   @Deprecated
   public abstract void renderElementPre(int var1, E var2, boolean var3, class_310 var4, class_332 var5, double var6, double var8, double var10, double var12, float var14, double var15, double var17, class_1060 var19, class_327 var20, class_4598 var21, MultiTextureRenderTypeRendererProvider var22, float var23, double var24, double var26, boolean var28, float var29);

   /** @deprecated */
   @Deprecated
   public abstract boolean renderElement(int var1, E var2, boolean var3, class_310 var4, class_332 var5, double var6, double var8, double var10, double var12, float var14, double var15, double var17, class_1060 var19, class_327 var20, class_4598 var21, MultiTextureRenderTypeRendererProvider var22, int var23, double var24, float var26, double var27, double var29, boolean var31, float var32);

   /** @deprecated */
   @Deprecated
   public abstract boolean shouldRender(int var1, boolean var2);

   /** @deprecated */
   @Deprecated
   public MapElementReader<E, C, R> getReader() {
      return (MapElementReader)super.getReader();
   }

   /** @deprecated */
   @Deprecated
   public MapElementRenderProvider<E, C> getProvider() {
      return (MapElementRenderProvider)super.getProvider();
   }
}
