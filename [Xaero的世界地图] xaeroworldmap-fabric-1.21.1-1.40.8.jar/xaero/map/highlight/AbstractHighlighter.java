package xaero.map.highlight;

import java.util.List;
import net.minecraft.class_1937;
import net.minecraft.class_2561;
import net.minecraft.class_5321;

public abstract class AbstractHighlighter {
   protected final boolean coveringOutsideDiscovered;
   protected final int[] resultStore = new int[256];

   protected AbstractHighlighter(boolean coveringOutsideDiscovered) {
      this.coveringOutsideDiscovered = coveringOutsideDiscovered;
   }

   public abstract int calculateRegionHash(class_5321<class_1937> var1, int var2, int var3);

   public abstract boolean regionHasHighlights(class_5321<class_1937> var1, int var2, int var3);

   public abstract boolean chunkIsHighlit(class_5321<class_1937> var1, int var2, int var3);

   public abstract int[] getChunkHighlitColor(class_5321<class_1937> var1, int var2, int var3);

   public abstract class_2561 getBlockHighlightSubtleTooltip(class_5321<class_1937> var1, int var2, int var3);

   public abstract class_2561 getBlockHighlightBluntTooltip(class_5321<class_1937> var1, int var2, int var3);

   public abstract void addMinimapBlockHighlightTooltips(List<class_2561> var1, class_5321<class_1937> var2, int var3, int var4, int var5);

   protected void setResult(int x, int z, int color) {
      this.resultStore[z << 4 | x] = color;
   }

   protected int getBlend(int color1, int color2) {
      if (color1 == color2) {
         return color1;
      } else {
         int red1 = color1 >> 8 & 255;
         int green1 = color1 >> 16 & 255;
         int blue1 = color1 >> 24 & 255;
         int alpha1 = color1 & 255;
         int red2 = color2 >> 8 & 255;
         int green2 = color2 >> 16 & 255;
         int blue2 = color2 >> 24 & 255;
         int alpha2 = color2 & 255;
         int red = red1 + red2 >> 1;
         int green = green1 + green2 >> 1;
         int blue = blue1 + blue2 >> 1;
         int alpha = alpha1 + alpha2 >> 1;
         return blue << 24 | green << 16 | red << 8 | alpha;
      }
   }

   public boolean isCoveringOutsideDiscovered() {
      return this.coveringOutsideDiscovered;
   }
}
