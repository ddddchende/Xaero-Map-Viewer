package xaero.map.highlight;

import java.util.List;
import net.minecraft.class_1937;
import net.minecraft.class_2561;
import net.minecraft.class_5321;

public class TestHighlighter extends ChunkHighlighter {
   public TestHighlighter() {
      super(true);
   }

   public boolean regionHasHighlights(class_5321<class_1937> dimension, int regionX, int regionZ) {
      return true;
   }

   protected int[] getColors(class_5321<class_1937> dimension, int chunkX, int chunkZ) {
      if (!this.chunkIsHighlit(dimension, chunkX, chunkZ)) {
         return null;
      } else {
         int centerColor = 1442796919;
         int sideColor = 1442797004;
         this.resultStore[0] = centerColor;
         this.resultStore[1] = (chunkZ & 3) == 0 ? sideColor : centerColor;
         this.resultStore[2] = (chunkX & 3) == 3 ? sideColor : centerColor;
         this.resultStore[3] = (chunkZ & 3) == 3 ? sideColor : centerColor;
         this.resultStore[4] = (chunkX & 3) == 0 ? sideColor : centerColor;
         return this.resultStore;
      }
   }

   public int calculateRegionHash(class_5321<class_1937> dimension, int regionX, int regionZ) {
      return 51;
   }

   public boolean chunkIsHighlit(class_5321<class_1937> dimension, int chunkX, int chunkZ) {
      return (chunkX >> 2 & 1) == (chunkZ >> 2 & 1);
   }

   public class_2561 getChunkHighlightSubtleTooltip(class_5321<class_1937> dimension, int chunkX, int chunkZ) {
      return class_2561.method_43470("subtle!");
   }

   public class_2561 getChunkHighlightBluntTooltip(class_5321<class_1937> dimension, int chunkX, int chunkZ) {
      return class_2561.method_43470("blunt!");
   }

   public void addMinimapBlockHighlightTooltips(List<class_2561> list, class_5321<class_1937> dimension, int blockX, int blockZ, int width) {
      list.add(class_2561.method_43470("minimap tooltip!"));
   }
}
