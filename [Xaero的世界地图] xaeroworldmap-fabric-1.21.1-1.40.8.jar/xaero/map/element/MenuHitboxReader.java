package xaero.map.element;

import net.minecraft.class_310;

public class MenuHitboxReader extends MenuOnlyElementReader<MapElementMenuHitbox> {
   public int getLeftSideLength(MapElementMenuHitbox element, class_310 mc) {
      return 0;
   }

   public boolean isMouseOverMenuElement(MapElementMenuHitbox element, int menuX, int menuY, int mouseX, int mouseY, class_310 mc) {
      int hitboxMinX = menuX + element.getX();
      int hitboxMinY = menuY + element.getY();
      int hitboxMaxX = hitboxMinX + element.getW();
      int hitboxMaxY = hitboxMinY + element.getH();
      return mouseX >= hitboxMinX && mouseX < hitboxMaxX && mouseY >= hitboxMinY && mouseY < hitboxMaxY;
   }

   public String getMenuName(MapElementMenuHitbox element) {
      return "";
   }

   public int getMenuTextFillLeftPadding(MapElementMenuHitbox element) {
      return 0;
   }

   public String getFilterName(MapElementMenuHitbox element) {
      return this.getMenuName(element);
   }

   public int getRightClickTitleBackgroundColor(MapElementMenuHitbox element) {
      return 0;
   }

   public boolean shouldScaleBoxWithOptionalScale() {
      return false;
   }
}
