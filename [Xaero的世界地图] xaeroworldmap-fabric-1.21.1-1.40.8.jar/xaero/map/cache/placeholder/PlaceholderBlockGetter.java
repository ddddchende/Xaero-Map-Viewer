package xaero.map.cache.placeholder;

import net.minecraft.class_1922;
import net.minecraft.class_2338;
import net.minecraft.class_2586;
import net.minecraft.class_2680;
import net.minecraft.class_3610;

public class PlaceholderBlockGetter implements class_1922 {
   private class_2680 placeholderState;

   public void setPlaceholderState(class_2680 placeholderState) {
      this.placeholderState = placeholderState;
   }

   public class_2586 method_8321(class_2338 blockPos) {
      return null;
   }

   public class_2680 method_8320(class_2338 blockPos) {
      return this.placeholderState;
   }

   public class_3610 method_8316(class_2338 blockPos) {
      return this.placeholderState == null ? null : this.placeholderState.method_26227();
   }

   public int method_31605() {
      return 16;
   }

   public int method_31607() {
      return 0;
   }
}
