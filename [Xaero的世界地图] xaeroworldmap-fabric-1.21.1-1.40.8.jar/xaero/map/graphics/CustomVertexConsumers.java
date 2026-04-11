package xaero.map.graphics;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.util.SortedMap;
import net.minecraft.class_156;
import net.minecraft.class_1921;
import net.minecraft.class_4597;
import net.minecraft.class_9799;
import net.minecraft.class_4597.class_4598;

public class CustomVertexConsumers {
   private final SortedMap<class_1921, class_9799> builders = (SortedMap)class_156.method_654(new Object2ObjectLinkedOpenHashMap(), (map) -> {
      checkedAddToMap(map, CustomRenderTypes.MAP_COLOR_FILLER, new class_9799(256));
      checkedAddToMap(map, CustomRenderTypes.MAP_FRAME_TEXTURE_OVER_TRANSPARENT, new class_9799(256));
      checkedAddToMap(map, CustomRenderTypes.GUI_NEAREST, new class_9799(256));
      checkedAddToMap(map, CustomRenderTypes.GUI_BILINEAR, new class_9799(256));
      checkedAddToMap(map, CustomRenderTypes.GUI_BILINEAR_PREMULTIPLIED, new class_9799(256));
      checkedAddToMap(map, CustomRenderTypes.MAP_COLOR_OVERLAY, new class_9799(256));
      checkedAddToMap(map, CustomRenderTypes.MAP, new class_9799(256));
      checkedAddToMap(map, CustomRenderTypes.MAP_ELEMENT_TEXT_BG, new class_9799(42));
   });
   private class_4598 renderTypeBuffers;

   public CustomVertexConsumers() {
      this.renderTypeBuffers = class_4597.method_22992(this.builders, new class_9799(256));
   }

   public class_4598 getRenderTypeBuffers() {
      return this.renderTypeBuffers;
   }

   private static void checkedAddToMap(Object2ObjectLinkedOpenHashMap<class_1921, class_9799> map, class_1921 layer, class_9799 bb) {
      if (map.containsKey(layer)) {
         throw new RuntimeException("Duplicate render layers!");
      } else {
         map.put(layer, bb);
      }
   }
}
