package xaero.map.highlight;

import net.minecraft.class_1937;
import net.minecraft.class_2561;
import net.minecraft.class_5321;

public abstract class ChunkHighlighter extends AbstractHighlighter {
   protected ChunkHighlighter(boolean coveringOutsideDiscovered) {
      super(coveringOutsideDiscovered);
   }

   protected abstract int[] getColors(class_5321<class_1937> var1, int var2, int var3);

   public int[] getChunkHighlitColor(class_5321<class_1937> dimension, int chunkX, int chunkZ) {
      int[] colors = this.getColors(dimension, chunkX, chunkZ);
      if (colors == null) {
         return null;
      } else {
         int centerColor = colors[0];
         int topColor = colors[1];
         int rightColor = colors[2];
         int bottomColor = colors[3];
         int leftColor = colors[4];
         int topLeftColor = this.getSideBlend(topColor, leftColor, centerColor);
         int topRightColor = this.getSideBlend(topColor, rightColor, centerColor);
         int bottomRightColor = this.getSideBlend(bottomColor, rightColor, centerColor);
         int bottomLeftColor = this.getSideBlend(bottomColor, leftColor, centerColor);
         this.setResult(0, 0, topLeftColor);
         this.setResult(15, 0, topRightColor);
         this.setResult(15, 15, bottomRightColor);
         this.setResult(0, 15, bottomLeftColor);

         for(int i = 1; i < 15; ++i) {
            this.setResult(i, 0, topColor);
            this.setResult(15, i, rightColor);
            this.setResult(i, 15, bottomColor);
            this.setResult(0, i, leftColor);

            for(int j = 1; j < 15; ++j) {
               this.setResult(i, j, centerColor);
            }
         }

         return this.resultStore;
      }
   }

   private int getSideBlend(int color1, int color2, int centerColor) {
      return this.getBlend(color1 == centerColor ? color2 : color1, color2 == centerColor ? color1 : color2);
   }

   public class_2561 getBlockHighlightBluntTooltip(class_5321<class_1937> dimension, int blockX, int blockZ) {
      return !this.chunkIsHighlit(dimension, blockX >> 4, blockZ >> 4) ? null : this.getChunkHighlightBluntTooltip(dimension, blockX >> 4, blockZ >> 4);
   }

   public class_2561 getBlockHighlightSubtleTooltip(class_5321<class_1937> dimension, int blockX, int blockZ) {
      return !this.chunkIsHighlit(dimension, blockX >> 4, blockZ >> 4) ? null : this.getChunkHighlightSubtleTooltip(dimension, blockX >> 4, blockZ >> 4);
   }

   public abstract class_2561 getChunkHighlightSubtleTooltip(class_5321<class_1937> var1, int var2, int var3);

   public abstract class_2561 getChunkHighlightBluntTooltip(class_5321<class_1937> var1, int var2, int var3);
}
