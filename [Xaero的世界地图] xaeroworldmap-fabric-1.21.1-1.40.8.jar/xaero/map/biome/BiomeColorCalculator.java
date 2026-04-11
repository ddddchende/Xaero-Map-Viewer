package xaero.map.biome;

import net.minecraft.class_1959;
import net.minecraft.class_1972;
import net.minecraft.class_2338;
import net.minecraft.class_2378;
import net.minecraft.class_2960;
import net.minecraft.class_5321;
import net.minecraft.class_6539;
import net.minecraft.class_7924;
import net.minecraft.class_2338.class_2339;
import xaero.map.MapProcessor;
import xaero.map.region.MapTile;

public class BiomeColorCalculator {
   public final class_5321<class_1959> UNREACHABLE_BIOME;
   public final class_5321<class_1959> RIVER_BIOME;
   private int startO;
   private int endO;
   private int startP;
   private int endP;

   public BiomeColorCalculator() {
      this.UNREACHABLE_BIOME = class_5321.method_29179(class_7924.field_41236, class_2960.method_60654("xaeroworldmap:unreachable_biome"));
      this.RIVER_BIOME = class_1972.field_9438;
   }

   public void prepare(boolean biomeBlending) {
      this.startO = this.endO = this.startP = this.endP = 0;
      if (biomeBlending) {
         this.startO = -1;
         this.endO = 1;
         this.startP = -1;
         this.endP = 1;
      }

   }

   public int getBiomeColor(class_6539 stateColorResolver, boolean overlay, class_2339 pos, MapTile tile, int caveLayer, class_2378<class_1959> biomeRegistry, MapProcessor mapProcessor) {
      if (stateColorResolver == null) {
         return -1;
      } else {
         int i = 0;
         int j = 0;
         int k = 0;
         int total = 0;
         int initX = pos.method_10263();
         int initZ = pos.method_10260();

         for(int o = this.startO; o <= this.endO; ++o) {
            for(int p = this.startP; p <= this.endP; ++p) {
               if (o == 0 || p == 0) {
                  pos.method_10103(initX + o, pos.method_10264(), initZ + p);
                  class_5321<class_1959> b = this.getBiomeAtPos(pos, tile, caveLayer, mapProcessor);
                  if (b != this.UNREACHABLE_BIOME) {
                     if (b == null && overlay) {
                        b = this.RIVER_BIOME;
                     }

                     if (b != null) {
                        int l = false;
                        class_1959 gen = (class_1959)biomeRegistry.method_29107(b);
                        if (gen != null) {
                           int l = stateColorResolver.getColor(gen, (double)pos.method_10263(), (double)pos.method_10260());
                           i += l & 16711680;
                           j += l & '\uff00';
                           k += l & 255;
                           ++total;
                        }
                     }
                  }
               }
            }
         }

         pos.method_10103(initX, pos.method_10264(), initZ);
         if (total == 0) {
            class_1959 defaultBiome = (class_1959)biomeRegistry.method_29107(class_1972.field_9438);
            if (defaultBiome == null) {
               return -1;
            } else {
               return stateColorResolver.getColor(defaultBiome, (double)pos.method_10263(), (double)pos.method_10260());
            }
         } else {
            return i / total & 16711680 | j / total & '\uff00' | k / total;
         }
      }
   }

   public class_5321<class_1959> getBiomeAtPos(class_2338 pos, MapTile centerTile, int caveLayer, MapProcessor mapProcessor) {
      int tileX = pos.method_10263() >> 4;
      int tileZ = pos.method_10260() >> 4;
      MapTile tile = tileX == centerTile.getChunkX() && tileZ == centerTile.getChunkZ() ? centerTile : mapProcessor.getMapTile(caveLayer, tileX, tileZ);
      return tile != null && tile.isLoaded() ? tile.getBlock(pos.method_10263() & 15, pos.method_10260() & 15).getBiome() : this.UNREACHABLE_BIOME;
   }
}
