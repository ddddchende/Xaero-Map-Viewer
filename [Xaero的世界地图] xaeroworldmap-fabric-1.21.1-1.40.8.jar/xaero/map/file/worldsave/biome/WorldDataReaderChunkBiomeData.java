package xaero.map.file.worldsave.biome;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.class_1959;
import net.minecraft.class_2378;
import net.minecraft.class_2960;
import xaero.map.misc.CachedFunction;

public class WorldDataReaderChunkBiomeData {
   private Int2ObjectMap<WorldDataReaderSectionBiomeData> sections;

   public void addSection(int sectionIndex, WorldDataReaderSectionBiomeData section) {
      if (this.sections == null) {
         this.sections = new Int2ObjectOpenHashMap();
      }

      this.sections.put(sectionIndex, section);
   }

   public class_1959 getNoiseBiome(int quadX, int quadY, int quadZ, class_2378<class_1959> biomeRegistry, CachedFunction<String, class_2960> resourceLocationCache) {
      if (this.sections == null) {
         return null;
      } else {
         int sectionIndex = quadY >> 2;
         WorldDataReaderSectionBiomeData section = (WorldDataReaderSectionBiomeData)this.sections.get(sectionIndex);
         if (section == null) {
            return null;
         } else {
            int sectionQuadY = quadY & 3;
            class_2960 biomeLocation = (class_2960)resourceLocationCache.apply(section.get(quadX, sectionQuadY, quadZ));
            return biomeLocation == null ? null : (class_1959)biomeRegistry.method_10223(biomeLocation);
         }
      }
   }

   public void clear() {
      this.sections = null;
   }
}
