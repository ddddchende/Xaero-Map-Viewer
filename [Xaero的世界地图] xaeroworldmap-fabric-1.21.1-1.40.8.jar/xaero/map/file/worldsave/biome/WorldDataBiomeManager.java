package xaero.map.file.worldsave.biome;

import net.minecraft.class_1959;
import net.minecraft.class_1972;
import net.minecraft.class_2378;
import net.minecraft.class_2960;
import net.minecraft.class_4543;
import net.minecraft.class_6880;
import net.minecraft.class_2338.class_2339;
import net.minecraft.class_4543.class_4544;
import xaero.map.misc.CachedFunction;

public class WorldDataBiomeManager implements class_4544 {
   private final WorldDataReaderChunkBiomeData[] chunkBiomeData = new WorldDataReaderChunkBiomeData[1156];
   private int regionX;
   private int regionZ;
   private class_1959 theVoid;
   private class_1959 defaultBiome;
   private class_2378<class_1959> biomeRegistry;
   private class_2339 mutableBlockPos;
   private CachedFunction<String, class_2960> resourceLocationCache;

   public WorldDataBiomeManager() {
      for(int i = 0; i < this.chunkBiomeData.length; ++i) {
         this.chunkBiomeData[i] = new WorldDataReaderChunkBiomeData();
      }

      this.mutableBlockPos = new class_2339();
      this.resourceLocationCache = new CachedFunction(class_2960::method_60654);
   }

   public void resetChunkBiomeData(int regionX, int regionZ, class_1959 defaultBiome, class_2378<class_1959> biomeRegistry) {
      this.regionX = regionX;
      this.regionZ = regionZ;
      this.biomeRegistry = biomeRegistry;
      this.theVoid = (class_1959)biomeRegistry.method_29107(class_1972.field_9473);
   }

   public void clear() {
      for(int i = 0; i < this.chunkBiomeData.length; ++i) {
         this.chunkBiomeData[i].clear();
      }

   }

   private WorldDataReaderChunkBiomeData getChunkBiomeData(int chunkX, int chunkZ) {
      return chunkX >= -1 && chunkZ >= -1 && chunkX <= 32 && chunkZ <= 32 ? this.chunkBiomeData[(chunkZ + 1) * 34 + chunkX + 1] : null;
   }

   public void addBiomeSectionForRegionChunk(int chunkX, int chunkZ, int sectionIndex, WorldDataReaderSectionBiomeData section) {
      this.getChunkBiomeData(chunkX, chunkZ).addSection(sectionIndex, section);
   }

   public class_1959 getBiome(class_4543 biomeZoomer, int x, int y, int z) {
      this.defaultBiome = null;
      this.defaultBiome = (class_1959)this.method_16359(x >> 2, y >> 2, z >> 2).comp_349();
      if (this.defaultBiome == null) {
         this.defaultBiome = this.theVoid;
      }

      this.mutableBlockPos.method_10103(x, y, z);
      return (class_1959)biomeZoomer.method_22393(this.mutableBlockPos).comp_349();
   }

   public class_6880<class_1959> method_16359(int x, int y, int z) {
      int relativeX = x - this.regionX * 128;
      int relativeZ = z - this.regionZ * 128;
      int chunkX = relativeX >> 2;
      int chunkZ = relativeZ >> 2;
      int quadX = relativeX & 3;
      int quadZ = relativeZ & 3;
      WorldDataReaderChunkBiomeData chunkBiomeData = this.getChunkBiomeData(chunkX, chunkZ);
      if (chunkBiomeData == null) {
         return class_6880.method_40223(this.defaultBiome);
      } else {
         class_1959 biome = chunkBiomeData.getNoiseBiome(quadX, y, quadZ, this.biomeRegistry, this.resourceLocationCache);
         return biome == null ? class_6880.method_40223(this.defaultBiome) : class_6880.method_40223(biome);
      }
   }
}
