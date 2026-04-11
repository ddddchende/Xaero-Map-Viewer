package xaero.map.region;

import net.minecraft.class_1959;
import net.minecraft.class_2378;
import xaero.map.region.texture.ExportLeafRegionTexture;
import xaero.map.world.MapDimension;

public class ExportMapRegion extends MapRegion {
   public ExportMapRegion(MapDimension dim, int x, int z, int caveLayer, class_2378<class_1959> biomeRegistry) {
      super("png", "null", (String)null, dim, x, z, caveLayer, 0, false, biomeRegistry);
   }

   protected MapTileChunk createTileChunk(int x, int y) {
      return new ExportMapTileChunk(this, this.regionX * 8 + x, this.regionZ * 8 + y);
   }

   public ExportLeafRegionTexture getTexture(int x, int y) {
      return (ExportLeafRegionTexture)super.getTexture(x, y);
   }

   public ExportMapTileChunk getChunk(int x, int z) {
      return (ExportMapTileChunk)super.getChunk(x, z);
   }
}
