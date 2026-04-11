package xaero.map.mods.minimap.element;

import xaero.common.minimap.element.render.MinimapElementRenderLocation;
import xaero.common.minimap.element.render.MinimapElementRenderProvider;
import xaero.map.element.MapElementRenderProvider;
import xaero.map.element.render.ElementRenderLocation;

public class MinimapElementRenderProviderWrapper<E, C> extends MapElementRenderProvider<E, C> {
   private final MinimapElementRenderProvider<E, C> provider;

   public MinimapElementRenderProviderWrapper(MinimapElementRenderProvider<E, C> provider) {
      this.provider = provider;
   }

   public void begin(ElementRenderLocation location, C context) {
      this.provider.begin(MinimapElementRenderLocation.fromWorldMap(location.getIndex()), context);
   }

   public boolean hasNext(ElementRenderLocation location, C context) {
      return this.provider.hasNext(MinimapElementRenderLocation.fromWorldMap(location.getIndex()), context);
   }

   public E setupContextAndGetNext(ElementRenderLocation location, C context) {
      return this.provider.setupContextAndGetNext(MinimapElementRenderLocation.fromWorldMap(location.getIndex()), context);
   }

   public E getNext(ElementRenderLocation location, C context) {
      return this.provider.getNext(MinimapElementRenderLocation.fromWorldMap(location.getIndex()), context);
   }

   public void end(ElementRenderLocation location, C context) {
      this.provider.end(MinimapElementRenderLocation.fromWorldMap(location.getIndex()), context);
   }

   /** @deprecated */
   @Deprecated
   public void begin(int location, C context) {
      this.begin(ElementRenderLocation.fromIndex(location), context);
   }

   /** @deprecated */
   @Deprecated
   public boolean hasNext(int location, C context) {
      return this.hasNext(ElementRenderLocation.fromIndex(location), context);
   }

   /** @deprecated */
   @Deprecated
   public E getNext(int location, C context) {
      return this.getNext(ElementRenderLocation.fromIndex(location), context);
   }

   /** @deprecated */
   @Deprecated
   public E setupContextAndGetNext(int location, C context) {
      return this.setupContextAndGetNext(ElementRenderLocation.fromIndex(location), context);
   }

   /** @deprecated */
   @Deprecated
   public void end(int location, C context) {
      this.end(ElementRenderLocation.fromIndex(location), context);
   }
}
