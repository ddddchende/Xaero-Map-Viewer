package xaero.map.element;

import java.util.ArrayList;
import xaero.map.element.render.ElementRenderer;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

public class HoveredMapElementHolder<E, C> implements IRightClickableElement {
   private final E element;
   private final ElementRenderer<E, C, ?> renderer;

   public HoveredMapElementHolder(E element, ElementRenderer<E, C, ?> renderer) {
      this.element = element;
      this.renderer = renderer;
   }

   public ArrayList<RightClickOption> getRightClickOptions() {
      return this.renderer.getReader().getRightClickOptions(this.element, this);
   }

   public boolean isRightClickValid() {
      return this.renderer.getReader().isRightClickValid(this.element);
   }

   public int getRightClickTitleBackgroundColor() {
      return this.renderer.getReader().getRightClickTitleBackgroundColor(this.element);
   }

   public E getElement() {
      return this.element;
   }

   public ElementRenderer<E, C, ?> getRenderer() {
      return this.renderer;
   }

   public boolean is(Object o) {
      return this.element == o;
   }
}
