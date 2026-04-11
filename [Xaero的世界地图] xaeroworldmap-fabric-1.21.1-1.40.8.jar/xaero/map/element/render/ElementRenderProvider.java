package xaero.map.element.render;

public abstract class ElementRenderProvider<E, C> {
   public abstract void begin(ElementRenderLocation var1, C var2);

   public abstract boolean hasNext(ElementRenderLocation var1, C var2);

   public abstract E getNext(ElementRenderLocation var1, C var2);

   public E setupContextAndGetNext(ElementRenderLocation location, C context) {
      return this.getNext(location, context);
   }

   public abstract void end(ElementRenderLocation var1, C var2);
}
