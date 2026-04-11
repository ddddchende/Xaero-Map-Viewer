package xaero.map.biome;

import net.minecraft.class_1920;
import net.minecraft.class_1937;
import net.minecraft.class_1959;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2586;
import net.minecraft.class_2680;
import net.minecraft.class_3568;
import net.minecraft.class_3610;
import net.minecraft.class_6539;
import net.minecraft.class_6880;
import net.minecraft.class_2338.class_2339;

public class BiomeBlendCalculator implements class_1920 {
   private class_1937 original;

   public void setWorld(class_1937 original) {
      this.original = original;
   }

   public class_2586 method_8321(class_2338 blockPos) {
      return this.original.method_8321(blockPos);
   }

   public class_2680 method_8320(class_2338 blockPos) {
      return this.original.method_8320(blockPos);
   }

   public class_3610 method_8316(class_2338 blockPos) {
      return this.original.method_8316(blockPos);
   }

   public float method_24852(class_2350 direction, boolean bl) {
      return this.original.method_24852(direction, bl);
   }

   public class_3568 method_22336() {
      return this.original.method_22336();
   }

   public int method_31605() {
      return this.original.method_31605();
   }

   public int method_31607() {
      return this.original.method_31607();
   }

   public int method_23752(class_2338 blockPos, class_6539 colorResolver) {
      class_2339 mutableBlockPos = new class_2339();
      int x = blockPos.method_10263();
      int y = blockPos.method_10264();
      int z = blockPos.method_10260();
      int redAccumulator = 0;
      int greenAccumulator = 0;
      int blueAccumulator = 0;
      int count = 0;

      int i;
      int j;
      for(i = -1; i < 2; ++i) {
         for(j = -1; j < 2; ++j) {
            mutableBlockPos.method_10103(x + i, y, z + j);
            class_6880<class_1959> biomeHolder = this.original.method_23753(mutableBlockPos);
            class_1959 biome = biomeHolder == null ? null : (class_1959)biomeHolder.comp_349();
            if (biome != null) {
               int colorSample = colorResolver.getColor(biome, (double)mutableBlockPos.method_10263(), (double)mutableBlockPos.method_10260());
               redAccumulator += colorSample >> 16 & 255;
               greenAccumulator += colorSample >> 8 & 255;
               blueAccumulator += colorSample & 255;
               ++count;
            }
         }
      }

      i = redAccumulator / count;
      j = greenAccumulator / count;
      int blue = blueAccumulator / count;
      return -16777216 | i << 16 | j << 8 | blue;
   }
}
