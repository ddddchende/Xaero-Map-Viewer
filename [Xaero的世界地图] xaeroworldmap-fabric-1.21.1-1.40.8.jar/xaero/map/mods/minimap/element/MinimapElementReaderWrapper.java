package xaero.map.mods.minimap.element;

import net.minecraft.class_310;
import xaero.common.minimap.element.render.MinimapElementReader;
import xaero.common.minimap.element.render.MinimapElementRenderLocation;
import xaero.map.element.MapElementReader;
import xaero.map.element.render.ElementRenderLocation;

public class MinimapElementReaderWrapper<E, C> extends MapElementReader<E, C, MinimapElementRendererWrapper<E, C>> {
   private final MinimapElementReader<E, C> reader;

   public MinimapElementReaderWrapper(MinimapElementReader<E, C> reader) {
      this.reader = reader;
   }

   public boolean isHidden(E element, C context) {
      return this.reader.isHidden(element, context);
   }

   /** @deprecated */
   @Deprecated
   public float getBoxScale(int location, E element, C context) {
      return this.getBoxScale(ElementRenderLocation.fromIndex(location), element, context);
   }

   public float getBoxScale(ElementRenderLocation location, E element, C context) {
      return this.reader.getBoxScale(MinimapElementRenderLocation.fromWorldMap(location.getIndex()), element, context);
   }

   public double getRenderX(E element, C context, float partialTicks) {
      return this.reader.getRenderX(element, context, partialTicks);
   }

   public double getRenderZ(E element, C context, float partialTicks) {
      return this.reader.getRenderZ(element, context, partialTicks);
   }

   public int getInteractionBoxLeft(E element, C context, float partialTicks) {
      return this.reader.getInteractionBoxLeft(element, context, partialTicks);
   }

   public int getInteractionBoxRight(E element, C context, float partialTicks) {
      return this.reader.getInteractionBoxRight(element, context, partialTicks);
   }

   public int getInteractionBoxTop(E element, C context, float partialTicks) {
      return this.reader.getInteractionBoxTop(element, context, partialTicks);
   }

   public int getInteractionBoxBottom(E element, C context, float partialTicks) {
      return this.reader.getInteractionBoxBottom(element, context, partialTicks);
   }

   public int getLeftSideLength(E element, class_310 mc) {
      return this.reader.getLeftSideLength(element, mc);
   }

   public String getMenuName(E element) {
      return this.reader.getMenuName(element);
   }

   public String getFilterName(E element) {
      return this.reader.getFilterName(element);
   }

   public int getMenuTextFillLeftPadding(E element) {
      return this.reader.getMenuTextFillLeftPadding(element);
   }

   public int getRightClickTitleBackgroundColor(E element) {
      return this.reader.getRightClickTitleBackgroundColor(element);
   }

   public int getRenderBoxLeft(E element, C context, float partialTicks) {
      return this.reader.getRenderBoxLeft(element, context, partialTicks);
   }

   public int getRenderBoxRight(E element, C context, float partialTicks) {
      return this.reader.getRenderBoxRight(element, context, partialTicks);
   }

   public int getRenderBoxTop(E element, C context, float partialTicks) {
      return this.reader.getRenderBoxTop(element, context, partialTicks);
   }

   public int getRenderBoxBottom(E element, C context, float partialTicks) {
      return this.reader.getRenderBoxBottom(element, context, partialTicks);
   }

   /** @deprecated */
   @Deprecated
   public boolean isInteractable(int location, E element) {
      return this.isInteractable(ElementRenderLocation.fromIndex(location), element);
   }

   public boolean isInteractable(ElementRenderLocation location, E element) {
      return this.reader.isInteractable(location.getIndex(), element);
   }

   public boolean shouldScaleBoxWithOptionalScale() {
      return this.reader.shouldScaleBoxWithOptionalScale();
   }
}
