package xaero.map.element;

import net.minecraft.class_1074;
import net.minecraft.class_310;

public class MenuScrollReader extends MenuOnlyElementReader<MapElementMenuScroll> {
   public int getLeftSideLength(MapElementMenuScroll element, class_310 mc) {
      return 9 + mc.field_1772.method_1727(class_1074.method_4662(element.getName(), new Object[0]));
   }

   public String getMenuName(MapElementMenuScroll element) {
      return class_1074.method_4662(element.getName(), new Object[0]);
   }

   public String getFilterName(MapElementMenuScroll element) {
      return this.getMenuName(element);
   }

   public int getMenuTextFillLeftPadding(MapElementMenuScroll element) {
      return 0;
   }

   public int getRightClickTitleBackgroundColor(MapElementMenuScroll element) {
      return 0;
   }

   public boolean shouldScaleBoxWithOptionalScale() {
      return false;
   }
}
