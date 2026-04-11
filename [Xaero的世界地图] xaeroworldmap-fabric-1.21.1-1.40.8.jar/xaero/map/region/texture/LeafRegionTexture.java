package xaero.map.region.texture;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import net.minecraft.class_310;
import org.lwjgl.opengl.GL11;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.biome.BlockTintProvider;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.config.util.WorldMapClientConfigUtils;
import xaero.map.exception.OpenGLException;
import xaero.map.graphics.TextureUploader;
import xaero.map.highlight.DimensionHighlighterHandler;
import xaero.map.misc.Misc;
import xaero.map.pool.buffer.PoolTextureDirectBufferUnit;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapRegion;
import xaero.map.region.MapTileChunk;
import xaero.map.region.MapUpdateFastConfig;
import xaero.map.region.OverlayManager;

public class LeafRegionTexture extends RegionTexture<LeafRegionTexture> {
   private MapTileChunk tileChunk;
   protected PoolTextureDirectBufferUnit highlitColorBuffer;

   public LeafRegionTexture(MapTileChunk tileChunk) {
      super(tileChunk.getInRegion());
      this.tileChunk = tileChunk;
   }

   public void postBufferUpdate(boolean hasLight) {
      this.colorBufferFormat = -1;
      this.colorBufferCompressed = false;
      this.bufferHasLight = hasLight;
   }

   public void preUpload(MapProcessor mapProcessor, BlockTintProvider blockTintProvider, OverlayManager overlayManager, LeveledRegion<LeafRegionTexture> leveledRegion, boolean detailedDebug, BlockStateShortShapeCache blockStateShortShapeCache, MapUpdateFastConfig updateConfig) {
      MapRegion region = (MapRegion)leveledRegion;
      if (this.tileChunk.getToUpdateBuffers() && !mapProcessor.isWritingPaused()) {
         synchronized(region.writerThreadPauseSync) {
            if (!region.isWritingPaused()) {
               this.tileChunk.updateBuffers(mapProcessor, blockTintProvider, overlayManager, detailedDebug, blockStateShortShapeCache, updateConfig);
            }
         }
      }

   }

   public void postUpload(MapProcessor mapProcessor, LeveledRegion<LeafRegionTexture> leveledRegion, boolean cleanAndCacheRequestsBlocked) {
      MapRegion region = (MapRegion)leveledRegion;
      if (region.getLoadState() >= 2 && (region.getLoadState() == 3 || !region.isBeingWritten() && (region.getLastVisited() == 0L || region.getTimeSinceVisit() > 1000L)) && !cleanAndCacheRequestsBlocked && !this.tileChunk.getToUpdateBuffers() && this.tileChunk.getLoadState() != 3) {
         region.setLoadState((byte)3);
         this.tileChunk.setLoadState((byte)3);
         this.tileChunk.clean(mapProcessor);
      }

   }

   public boolean canUpload() {
      return this.tileChunk.getLoadState() >= 2;
   }

   public boolean isUploaded() {
      return super.isUploaded() && !this.tileChunk.getToUpdateBuffers();
   }

   public boolean hasSourceData() {
      return this.tileChunk.getLoadState() != 3;
   }

   protected long uploadNonCache(DimensionHighlighterHandler highlighterHandler, TextureUploader textureUploader, BranchTextureRenderer unused) {
      PoolTextureDirectBufferUnit colorBufferToUpload = this.applyHighlights(highlighterHandler, this.colorBuffer, true);
      if (this.textureVersion == -1) {
         this.updateTextureVersion(this.bufferedTextureVersion != -1 ? this.bufferedTextureVersion + 1 : 1 + (int)(Math.random() * 1000000.0D));
      } else {
         this.updateTextureVersion(this.textureVersion + 1);
      }

      if (colorBufferToUpload == null) {
         return 0L;
      } else {
         this.writeToUnpackPBO(0, colorBufferToUpload);
         this.textureHasLight = this.bufferHasLight;
         this.colorBuffer.getDirectBuffer().position(0);
         this.colorBufferFormat = 32856;
         this.bufferedTextureVersion = this.textureVersion;
         boolean subsequent = this.glColorTexture != -1;
         this.bindColorTexture(true);
         OpenGLException.checkGLError();
         if (this.unpackPbo[0] == 0) {
            return 0L;
         } else {
            long totalEstimatedTime;
            if (subsequent) {
               totalEstimatedTime = textureUploader.requestSubsequentNormal(this.glColorTexture, this.unpackPbo[0], 3553, 0, 64, 64, 0, 0L, 32993, 32821, 0, 0);
            } else {
               totalEstimatedTime = textureUploader.requestNormal(this.glColorTexture, this.unpackPbo[0], 3553, 0, 32856, 64, 64, 0, 0L, 32993, 32821);
            }

            boolean toUploadImmediately = this.tileChunk.getInRegion().isBeingWritten();
            if (toUploadImmediately) {
               textureUploader.finishNewestRequestImmediately();
            }

            return totalEstimatedTime;
         }
      }
   }

   protected PoolTextureDirectBufferUnit applyHighlights(DimensionHighlighterHandler highlighterHandler, PoolTextureDirectBufferUnit colorBuffer, boolean separateBuffer) {
      if (!this.tileChunk.hasHighlights()) {
         return colorBuffer;
      } else {
         colorBuffer = super.applyHighlights(highlighterHandler, colorBuffer, separateBuffer);
         int startChunkX = this.tileChunk.getX() << 2;
         int startChunkZ = this.tileChunk.getZ() << 2;
         boolean prepared = false;

         for(int i = 0; i < 4; ++i) {
            for(int j = 0; j < 4; ++j) {
               boolean discovered = this.getHeight(i << 4, j << 4) != 32767;
               int chunkX = startChunkX + i;
               int chunkZ = startChunkZ + j;
               PoolTextureDirectBufferUnit result = highlighterHandler.applyChunkHighlightColors(chunkX, chunkZ, i, j, colorBuffer, this.highlitColorBuffer, prepared, discovered, separateBuffer);
               if (result != null && separateBuffer) {
                  this.highlitColorBuffer = result;
                  prepared = true;
               }
            }
         }

         if (prepared) {
            return this.highlitColorBuffer;
         } else {
            return colorBuffer;
         }
      }
   }

   public void postBufferWrite(PoolTextureDirectBufferUnit buffer) {
      super.postBufferWrite(buffer);
      if (buffer == this.highlitColorBuffer) {
         this.highlitColorBuffer = null;
         if (!WorldMap.textureDirectBufferPool.addToPool(buffer)) {
            WorldMap.bufferDeallocator.deallocate(buffer.getDirectBuffer(), WorldMapClientConfigUtils.getDebug());
         }
      }

   }

   protected void updateTextureVersion(int newVersion) {
      super.updateTextureVersion(newVersion);
      this.region.updateLeafTextureVersion(this.tileChunk.getX() & 7, this.tileChunk.getZ() & 7, newVersion);
   }

   public void addDebugLines(List<String> lines) {
      super.addDebugLines(lines);
      int var10001 = this.tileChunk.getX();
      lines.add(var10001 + " " + this.tileChunk.getZ());
      lines.add("loadState: " + this.tileChunk.getLoadState());
      lines.add(String.format("changed: %s include: %s terrain: %s highlights: %s toUpdateBuffers: %s", this.tileChunk.wasChanged(), this.tileChunk.includeInSave(), this.tileChunk.hasHadTerrain(), this.tileChunk.hasHighlights(), this.tileChunk.getToUpdateBuffers()));
   }

   protected void onDownloadedBuffer(ByteBuffer mappedPBO, int isCompressed) {
      int length;
      if (isCompressed == 1) {
         length = GL11.glGetTexLevelParameteri(3553, 0, 34464);
      } else {
         length = 16384;
      }

      ByteBuffer directBuffer = this.colorBuffer.getDirectBuffer();
      directBuffer.clear();
      if (mappedPBO != null) {
         mappedPBO.limit(length);
         directBuffer.put(mappedPBO);
         directBuffer.flip();
      } else {
         directBuffer.limit(length);
      }

   }

   public void writeCacheMapData(DataOutputStream output, byte[] usableBuffer, byte[] integerByteBuffer, LeveledRegion<LeafRegionTexture> inRegion) throws IOException {
      super.writeCacheMapData(output, usableBuffer, integerByteBuffer, inRegion);
      this.tileChunk.writeCacheData(output, usableBuffer, integerByteBuffer, inRegion);
   }

   public void readCacheData(int minorSaveVersion, int majorSaveVersion, DataInputStream input, byte[] usableBuffer, byte[] integerByteBuffer, LeveledRegion<LeafRegionTexture> inRegion, MapProcessor mapProcessor, int x, int y, boolean leafShouldAffectBranches) throws IOException {
      super.readCacheData(minorSaveVersion, majorSaveVersion, input, usableBuffer, integerByteBuffer, inRegion, mapProcessor, x, y, leafShouldAffectBranches);
      this.tileChunk.readCacheData(minorSaveVersion, majorSaveVersion, input, usableBuffer, integerByteBuffer, mapProcessor, x, y);
      if (leafShouldAffectBranches) {
         this.colorBufferFormat = -1;
      }

      if (this.colorBuffer != null) {
         this.tileChunk.setHasHadTerrain();
      }

   }

   public void resetHeights() {
      Misc.clearHeightsData1024(this.heightValues.getData());
      Misc.clearHeightsData1024(this.topHeightValues.getData());
   }

   public boolean shouldBeUsedForBranchUpdate(int usedVersion) {
      return this.tileChunk.getLoadState() != 1 && super.shouldBeUsedForBranchUpdate(usedVersion);
   }

   public boolean shouldHaveContentForBranchUpdate() {
      return this.tileChunk.getLoadState() > 0 && super.shouldHaveContentForBranchUpdate();
   }

   public void deleteTexturesAndBuffers() {
      if (!class_310.method_1551().method_18854()) {
         synchronized(this.region.getLevel() == 3 ? this.region : this.region.getParent()) {
            synchronized(this.region) {
               this.tileChunk.setLoadState((byte)0);
            }
         }
      }

      super.deleteTexturesAndBuffers();
   }

   public void prepareBuffer() {
      super.prepareBuffer();
      this.tileChunk.setHasHadTerrain();
   }

   public MapTileChunk getTileChunk() {
      return this.tileChunk;
   }

   public boolean shouldIncludeInCache() {
      return this.tileChunk.hasHadTerrain();
   }

   public void requestHighlightOnlyUpload() {
      this.resetBiomes();
      this.colorBufferCompressed = false;
      this.colorBufferFormat = 32856;
      this.bufferedTextureVersion = this.tileChunk.getInRegion().getTargetHighlightsHash();
      this.setToUpload(true);
      if (this.tileChunk.getLoadState() < 2) {
         this.tileChunk.setLoadState((byte)2);
      }

   }
}
