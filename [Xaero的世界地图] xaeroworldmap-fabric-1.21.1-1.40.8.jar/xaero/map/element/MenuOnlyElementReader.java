package xaero.map.element;

public abstract class MenuOnlyElementReader<E> extends MapElementReader<E, Object, MenuOnlyElementRenderer<E>> {
   public boolean isHidden(E element, Object context) {
      return false;
   }

   public double getRenderX(E element, Object context, float partialTicks) {
      return 0.0D;
   }

   public double getRenderZ(E element, Object context, float partialTicks) {
      return 0.0D;
   }

   public int getInteractionBoxLeft(E element, Object context, float partialTicks) {
      return 0;
   }

   public int getInteractionBoxRight(E element, Object context, float partialTicks) {
      return 0;
   }

   public int getInteractionBoxTop(E element, Object context, float partialTicks) {
      return 0;
   }

   public int getInteractionBoxBottom(E element, Object context, float partialTicks) {
      return 0;
   }

   public int getRenderBoxLeft(E element, Object context, float partialTicks) {
      return 0;
   }

   public int getRenderBoxRight(E element, Object context, float partialTicks) {
      return 0;
   }

   public int getRenderBoxTop(E element, Object context, float partialTicks) {
      return 0;
   }

   public int getRenderBoxBottom(E element, Object context, float partialTicks) {
      return 0;
   }
}
