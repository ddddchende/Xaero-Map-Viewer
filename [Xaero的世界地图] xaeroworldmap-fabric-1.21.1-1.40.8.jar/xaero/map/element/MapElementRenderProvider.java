package xaero.map.element;

import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderProvider;

public abstract class MapElementRenderProvider<E, C> extends ElementRenderProvider<E, C> {
   /** @deprecated */
   @Deprecated
   public abstract void begin(int var1, C var2);

   /** @deprecated */
   @Deprecated
   public abstract boolean hasNext(int var1, C var2);

   /** @deprecated */
   @Deprecated
   public abstract E getNext(int var1, C var2);

   /** @deprecated */
   @Deprecated
   public E setupContextAndGetNext(int location, C context) {
      return this.getNext(location, context);
   }

   /** @deprecated */
   @Deprecated
   public abstract void end(int var1, C var2);

   public void begin(ElementRenderLocation location, C context) {
      this.begin(location.getIndex(), context);
   }

   public boolean hasNext(ElementRenderLocation location, C context) {
      return this.hasNext(location.getIndex(), context);
   }

   public E getNext(ElementRenderLocation location, C context) {
      return this.getNext(location.getIndex(), context);
   }

   public E setupContextAndGetNext(ElementRenderLocation location, C context) {
      return this.setupContextAndGetNext(location.getIndex(), context);
   }

   public void end(ElementRenderLocation location, C context) {
      this.end(location.getIndex(), context);
   }
}
