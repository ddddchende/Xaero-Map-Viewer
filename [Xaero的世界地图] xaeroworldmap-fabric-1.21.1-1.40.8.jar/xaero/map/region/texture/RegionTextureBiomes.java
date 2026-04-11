package xaero.map.region.texture;

import net.minecraft.class_1959;
import net.minecraft.class_5321;
import xaero.map.palette.FastPalette;
import xaero.map.palette.Paletted2DFastBitArrayIntStorage;

public class RegionTextureBiomes {
   protected final Paletted2DFastBitArrayIntStorage biomeIndexStorage;
   protected final FastPalette<class_5321<class_1959>> regionBiomePalette;

   public RegionTextureBiomes(Paletted2DFastBitArrayIntStorage biomeIndexStorage, FastPalette<class_5321<class_1959>> regionBiomePalette) {
      this.biomeIndexStorage = biomeIndexStorage;
      this.regionBiomePalette = regionBiomePalette;
   }

   public Paletted2DFastBitArrayIntStorage getBiomeIndexStorage() {
      return this.biomeIndexStorage;
   }

   public FastPalette<class_5321<class_1959>> getRegionBiomePalette() {
      return this.regionBiomePalette;
   }
}
