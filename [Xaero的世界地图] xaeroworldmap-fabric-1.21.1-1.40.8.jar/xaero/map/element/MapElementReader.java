package xaero.map.element;

import xaero.map.element.render.ElementReader;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderer;

public abstract class MapElementReader<E, C, R extends ElementRenderer<E, ?, R>> extends ElementReader<E, C, R> {
   /** @deprecated */
   @Deprecated
   public boolean isInteractable(int location, E element) {
      return super.isInteractable(ElementRenderLocation.fromIndex(location), element);
   }

   /** @deprecated */
   @Deprecated
   public float getBoxScale(int location, E element, C context) {
      return super.getBoxScale(ElementRenderLocation.fromIndex(location), element, context);
   }

   /** @deprecated */
   @Deprecated
   public boolean isHoveredOnMap(int location, E element, double mouseX, double mouseZ, double scale, double screenSizeBasedScale, double rendererDimDiv, C context, float partialTicks) {
      return super.isHoveredOnMap(ElementRenderLocation.fromIndex(location), element, mouseX, mouseZ, scale, screenSizeBasedScale, rendererDimDiv, context, partialTicks);
   }

   public boolean isInteractable(ElementRenderLocation location, E element) {
      return this.isInteractable(location.getIndex(), element);
   }

   public float getBoxScale(ElementRenderLocation location, E element, C context) {
      return this.getBoxScale(location.getIndex(), element, context);
   }

   public boolean isHoveredOnMap(ElementRenderLocation location, E element, double mouseX, double mouseZ, double scale, double screenSizeBasedScale, double rendererDimDiv, C context, float partialTicks) {
      return this.isHoveredOnMap(location.getIndex(), element, mouseX, mouseZ, scale, screenSizeBasedScale, rendererDimDiv, context, partialTicks);
   }
}
