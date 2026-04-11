package xaero.map.mods.pac.gui.claim.element;

import java.util.Iterator;
import xaero.map.element.MapElementRenderProvider;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.mods.pac.gui.claim.ClaimResultElement;
import xaero.map.mods.pac.gui.claim.ClaimResultElementManager;

public class ClaimResultElementRenderProvider extends MapElementRenderProvider<ClaimResultElement, ClaimResultElementRenderContext> {
   private final ClaimResultElementManager manager;
   private Iterator<ClaimResultElement> iterator;

   public ClaimResultElementRenderProvider(ClaimResultElementManager manager) {
      this.manager = manager;
   }

   public void begin(ElementRenderLocation location, ClaimResultElementRenderContext context) {
      this.iterator = this.manager.getIterator();
   }

   public boolean hasNext(ElementRenderLocation location, ClaimResultElementRenderContext context) {
      return this.iterator != null && this.iterator.hasNext();
   }

   public ClaimResultElement getNext(ElementRenderLocation location, ClaimResultElementRenderContext context) {
      return (ClaimResultElement)this.iterator.next();
   }

   public void end(ElementRenderLocation location, ClaimResultElementRenderContext context) {
      this.iterator = null;
   }

   /** @deprecated */
   @Deprecated
   public void begin(int location, ClaimResultElementRenderContext context) {
      this.begin(ElementRenderLocation.fromIndex(location), context);
   }

   /** @deprecated */
   @Deprecated
   public boolean hasNext(int location, ClaimResultElementRenderContext context) {
      return this.hasNext(ElementRenderLocation.fromIndex(location), context);
   }

   /** @deprecated */
   @Deprecated
   public ClaimResultElement getNext(int location, ClaimResultElementRenderContext context) {
      return this.getNext(ElementRenderLocation.fromIndex(location), context);
   }

   /** @deprecated */
   @Deprecated
   public ClaimResultElement setupContextAndGetNext(ElementRenderLocation location, ClaimResultElementRenderContext context) {
      return this.getNext(location, context);
   }

   /** @deprecated */
   @Deprecated
   public ClaimResultElement setupContextAndGetNext(int location, ClaimResultElementRenderContext context) {
      return this.setupContextAndGetNext(ElementRenderLocation.fromIndex(location), context);
   }

   /** @deprecated */
   @Deprecated
   public void end(int location, ClaimResultElementRenderContext context) {
      this.end(ElementRenderLocation.fromIndex(location), context);
   }
}
