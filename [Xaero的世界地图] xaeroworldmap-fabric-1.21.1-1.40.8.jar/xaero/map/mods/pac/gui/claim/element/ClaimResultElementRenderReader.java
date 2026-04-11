package xaero.map.mods.pac.gui.claim.element;

import net.minecraft.class_310;
import xaero.lib.client.gui.widget.Tooltip;
import xaero.map.element.MapElementReader;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.mods.pac.gui.claim.ClaimResultElement;

public class ClaimResultElementRenderReader extends MapElementReader<ClaimResultElement, ClaimResultElementRenderContext, ClaimResultElementRenderer> {
   public boolean isHidden(ClaimResultElement element, ClaimResultElementRenderContext context) {
      return false;
   }

   public double getRenderX(ClaimResultElement element, ClaimResultElementRenderContext context, float partialTicks) {
      return (double)((element.getLeft() + element.getRight() << 3) + 8);
   }

   public double getRenderZ(ClaimResultElement element, ClaimResultElementRenderContext context, float partialTicks) {
      return (double)((element.getTop() + element.getBottom() << 3) + 8);
   }

   public int getInteractionBoxLeft(ClaimResultElement element, ClaimResultElementRenderContext context, float partialTicks) {
      return -14;
   }

   public int getInteractionBoxRight(ClaimResultElement element, ClaimResultElementRenderContext context, float partialTicks) {
      return 14;
   }

   public int getInteractionBoxTop(ClaimResultElement element, ClaimResultElementRenderContext context, float partialTicks) {
      return -14;
   }

   public int getInteractionBoxBottom(ClaimResultElement element, ClaimResultElementRenderContext context, float partialTicks) {
      return 14;
   }

   public int getRenderBoxLeft(ClaimResultElement element, ClaimResultElementRenderContext context, float partialTicks) {
      return -16;
   }

   public int getRenderBoxRight(ClaimResultElement element, ClaimResultElementRenderContext context, float partialTicks) {
      return 16;
   }

   public int getRenderBoxTop(ClaimResultElement element, ClaimResultElementRenderContext context, float partialTicks) {
      return -16;
   }

   public int getRenderBoxBottom(ClaimResultElement element, ClaimResultElementRenderContext context, float partialTicks) {
      return 16;
   }

   public int getLeftSideLength(ClaimResultElement element, class_310 mc) {
      return 0;
   }

   public String getMenuName(ClaimResultElement element) {
      return "n/a";
   }

   public String getFilterName(ClaimResultElement element) {
      return this.getMenuName(element);
   }

   public int getMenuTextFillLeftPadding(ClaimResultElement element) {
      return 0;
   }

   public int getRightClickTitleBackgroundColor(ClaimResultElement element) {
      return 0;
   }

   public boolean shouldScaleBoxWithOptionalScale() {
      return true;
   }

   public boolean isInteractable(ElementRenderLocation location, ClaimResultElement element) {
      return true;
   }

   /** @deprecated */
   @Deprecated
   public boolean isInteractable(int location, ClaimResultElement element) {
      return this.isInteractable(ElementRenderLocation.fromIndex(location), element);
   }

   public Tooltip getTooltip(ClaimResultElement element, ClaimResultElementRenderContext context, boolean overMenu) {
      return element.getTooltip();
   }
}
