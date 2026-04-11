package xaero.map.region.texture;

import xaero.map.highlight.DimensionHighlighterHandler;
import xaero.map.pool.buffer.PoolTextureDirectBufferUnit;
import xaero.map.region.MapTileChunk;

public class ExportLeafRegionTexture extends LeafRegionTexture {
   public ExportLeafRegionTexture(MapTileChunk tileChunk) {
      super(tileChunk);
   }

   public void applyHighlights(DimensionHighlighterHandler highlighterHandler, PoolTextureDirectBufferUnit colorBuffer) {
      super.applyHighlights(highlighterHandler, colorBuffer, false);
   }
}
