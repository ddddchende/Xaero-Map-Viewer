package xaero.map.region.texture;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import net.minecraft.class_1959;
import net.minecraft.class_5321;
import xaero.map.MapProcessor;
import xaero.map.biome.BlockTintProvider;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.exception.OpenGLException;
import xaero.map.graphics.TextureUploader;
import xaero.map.highlight.DimensionHighlighterHandler;
import xaero.map.region.BranchLeveledRegion;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapUpdateFastConfig;
import xaero.map.region.OverlayManager;

public class BranchRegionTexture extends RegionTexture<BranchRegionTexture> {
   private boolean updating;
   private boolean colorAllocationRequested;
   private BranchRegionTexture.ChildTextureInfo topLeftInfo;
   private BranchRegionTexture.ChildTextureInfo topRightInfo;
   private BranchRegionTexture.ChildTextureInfo bottomLeftInfo;
   private BranchRegionTexture.ChildTextureInfo bottomRightInfo;
   private LeveledRegion<?> branchUpdateChildRegion;
   private boolean checkForUpdatesAfterDownload;

   public BranchRegionTexture(LeveledRegion<BranchRegionTexture> region) {
      super(region);
      this.reset();
   }

   private void reset() {
      this.updating = false;
      this.colorAllocationRequested = false;
      this.topLeftInfo = new BranchRegionTexture.ChildTextureInfo(this);
      this.topRightInfo = new BranchRegionTexture.ChildTextureInfo(this);
      this.bottomLeftInfo = new BranchRegionTexture.ChildTextureInfo(this);
      this.bottomRightInfo = new BranchRegionTexture.ChildTextureInfo(this);
      this.checkForUpdatesAfterDownload = false;
   }

   public boolean checkForUpdates(RegionTexture<?> topLeft, RegionTexture<?> topRight, RegionTexture<?> bottomLeft, RegionTexture<?> bottomRight, LeveledRegion<?> childRegion) {
      boolean needsUpdating = false;
      if (topLeft != null && topLeft.glColorTexture == -1 || topRight != null && topRight.glColorTexture == -1 || bottomLeft != null && bottomLeft.glColorTexture == -1 || bottomRight != null && bottomRight.glColorTexture == -1) {
         return false;
      } else {
         needsUpdating = needsUpdating || this.isChildUpdated(this.topLeftInfo, topLeft, childRegion);
         needsUpdating = needsUpdating || this.isChildUpdated(this.topRightInfo, topRight, childRegion);
         needsUpdating = needsUpdating || this.isChildUpdated(this.bottomLeftInfo, bottomLeft, childRegion);
         needsUpdating = needsUpdating || this.isChildUpdated(this.bottomRightInfo, bottomRight, childRegion);
         if (needsUpdating) {
            if (this.toUpload) {
               if (this.shouldDownloadFromPBO) {
                  this.checkForUpdatesAfterDownload = true;
                  return false;
               }

               if (this.topLeftInfo.temporaryReference == topLeft && this.topRightInfo.temporaryReference == topRight && this.bottomLeftInfo.temporaryReference == bottomLeft && this.bottomRightInfo.temporaryReference == bottomRight) {
                  return false;
               }
            } else {
               ++childRegion.activeBranchUpdateReferences;
            }

            this.setCachePrepared(false);
            this.region.setAllCachePrepared(false);
            this.colorBufferFormat = -1;
            this.toUpload = true;
            this.updating = true;
            this.topLeftInfo.temporaryReference = topLeft;
            this.topRightInfo.temporaryReference = topRight;
            this.bottomLeftInfo.temporaryReference = bottomLeft;
            this.bottomRightInfo.temporaryReference = bottomRight;
            this.branchUpdateChildRegion = childRegion;
         }

         return needsUpdating;
      }
   }

   private boolean isChildUpdated(BranchRegionTexture.ChildTextureInfo info, RegionTexture<?> texture, LeveledRegion<?> region) {
      if (region.isLoaded()) {
         if (texture == null && info.usedTextureVersion != 0) {
            return true;
         }

         if (texture != null && texture.glColorTexture != -1 && texture.shouldBeUsedForBranchUpdate(info.usedTextureVersion)) {
            return true;
         }
      }

      return false;
   }

   public void preUpload(MapProcessor mapProcessor, BlockTintProvider blockTintProvider, OverlayManager overlayManager, LeveledRegion<BranchRegionTexture> region, boolean detailedDebug, BlockStateShortShapeCache blockStateShortShapeCache, MapUpdateFastConfig updateConfig) {
   }

   public void postUpload(MapProcessor mapProcessor, LeveledRegion<BranchRegionTexture> leveledRegion, boolean cleanAndCacheRequestsBlocked) {
   }

   public long uploadBuffer(DimensionHighlighterHandler highlighterHandler, TextureUploader textureUploader, LeveledRegion<BranchRegionTexture> inRegion, BranchTextureRenderer branchTextureRenderer, int x, int y) throws OpenGLException, IllegalArgumentException, IllegalAccessException {
      return super.uploadBuffer(highlighterHandler, textureUploader, inRegion, branchTextureRenderer, x, y);
   }

   private void copyNonColorData(RegionTexture<?> childTexture, int offX, int offZ) {
      boolean resetting = childTexture == null;

      for(int i = 0; i < 32; ++i) {
         for(int j = 0; j < 32; ++j) {
            int childHeight = resetting ? 32767 : childTexture.getHeight(i << 1, j << 1);
            int childTopHeight = resetting ? 32767 : childTexture.getTopHeight(i << 1, j << 1);
            class_5321<class_1959> childBiome = resetting ? null : childTexture.getBiome(i << 1, j << 1);
            int destX = offX | i;
            int destZ = offZ | j;
            if (childHeight != 32767) {
               this.putHeight(destX, destZ, childHeight);
            } else {
               this.removeHeight(destX, destZ);
            }

            if (childTopHeight != 32767) {
               this.putTopHeight(destX, destZ, childTopHeight);
            } else {
               this.removeTopHeight(destX, destZ);
            }

            this.setBiome(destX, destZ, childBiome);
         }
      }

   }

   protected long uploadNonCache(DimensionHighlighterHandler highlighterHandler, TextureUploader textureUploader, BranchTextureRenderer renderer) {
      this.timer = 5;
      this.prepareBuffer();
      this.shouldDownloadFromPBO = true;
      if (this.updating) {
         this.bindPackPBO();
         this.unbindPackPBO();
         this.bindColorTexture(true);
         OpenGLException.checkGLError();
         BranchRegionTexture.ChildTextureInfo topLeftInfo = this.topLeftInfo;
         BranchRegionTexture.ChildTextureInfo topRightInfo = this.topRightInfo;
         BranchRegionTexture.ChildTextureInfo bottomLeftInfo = this.bottomLeftInfo;
         BranchRegionTexture.ChildTextureInfo bottomRightInfo = this.bottomRightInfo;
         Integer topLeftColor = topLeftInfo.getColorTextureForUpdate();
         Integer topRightColor = topRightInfo.getColorTextureForUpdate();
         Integer bottomLeftColor = bottomLeftInfo.getColorTextureForUpdate();
         Integer bottomRightColor = bottomRightInfo.getColorTextureForUpdate();
         long estimatedTime = textureUploader.requestBranchUpdate(!this.colorAllocationRequested, this.glColorTexture, this.unpackPbo[0], 3553, 0, 32856, 64, 64, 0, 0L, 32993, 32821, topLeftColor, topRightColor, bottomLeftColor, bottomRightColor, renderer, this.packPbo, 0);
         if (topLeftColor != null) {
            this.copyNonColorData(topLeftInfo.temporaryReference, 0, 0);
         }

         if (topRightColor != null) {
            this.copyNonColorData(topRightInfo.temporaryReference, 32, 0);
         }

         if (bottomLeftColor != null) {
            this.copyNonColorData(bottomLeftInfo.temporaryReference, 0, 32);
         }

         if (bottomRightColor != null) {
            this.copyNonColorData(bottomRightInfo.temporaryReference, 32, 32);
         }

         int textureVersionSum = 0;
         int topLeftVersion;
         int textureVersionSum = textureVersionSum + (topLeftVersion = topLeftInfo.getTextureVersion());
         int topRightVersion;
         textureVersionSum += topRightVersion = topRightInfo.getTextureVersion();
         int bottomLeftVersion;
         textureVersionSum += bottomLeftVersion = bottomLeftInfo.getTextureVersion();
         int bottomRightVersion;
         textureVersionSum += bottomRightVersion = bottomRightInfo.getTextureVersion();
         this.updateTextureVersion(textureVersionSum);
         this.colorAllocationRequested = true;
         this.textureHasLight = topLeftInfo.hasLight() || topRightInfo.hasLight() || bottomLeftInfo.hasLight() || bottomRightInfo.hasLight();
         --this.branchUpdateChildRegion.activeBranchUpdateReferences;
         this.branchUpdateChildRegion = null;
         topLeftInfo.onUpdate(topLeftVersion);
         topRightInfo.onUpdate(topRightVersion);
         bottomLeftInfo.onUpdate(bottomLeftVersion);
         bottomRightInfo.onUpdate(bottomRightVersion);
         BranchLeveledRegion branchRegion = (BranchLeveledRegion)this.region;
         branchRegion.postTextureUpdate();
         return estimatedTime;
      } else {
         this.bindPackPBO();
         this.unbindPackPBO();
         return textureUploader.requestBranchDownload(this.glColorTexture, 3553, this.packPbo, 0);
      }
   }

   protected void onCacheUploadRequested() {
      super.onCacheUploadRequested();
      this.colorAllocationRequested = true;
   }

   protected void onDownloadedBuffer(ByteBuffer mappedPBO, int isCompressed) {
      ByteBuffer directBuffer = this.colorBuffer.getDirectBuffer();
      directBuffer.clear();
      if (mappedPBO != null) {
         directBuffer.put(mappedPBO);
         directBuffer.flip();
      } else {
         directBuffer.limit(16384);
      }

      if (this.checkForUpdatesAfterDownload) {
         ((BranchLeveledRegion)this.region).setShouldCheckForUpdatesRecursive(true);
         this.checkForUpdatesAfterDownload = false;
      }

   }

   protected void endPBODownload(int format, boolean compressed, boolean success) {
      if (!success) {
         --this.topLeftInfo.usedTextureVersion;
         --this.topRightInfo.usedTextureVersion;
         --this.bottomLeftInfo.usedTextureVersion;
         --this.bottomRightInfo.usedTextureVersion;
         this.updateTextureVersion(this.topLeftInfo.usedTextureVersion + this.topRightInfo.usedTextureVersion + this.bottomLeftInfo.usedTextureVersion + this.bottomRightInfo.usedTextureVersion);
      }

      super.endPBODownload(format, compressed, success);
   }

   public boolean hasSourceData() {
      return false;
   }

   public void addDebugLines(List<String> lines) {
      super.addDebugLines(lines);
      lines.add("updating: " + this.updating);
      lines.add("colorAllocationRequested: " + this.colorAllocationRequested);
      lines.add("topLeftInfo: " + String.valueOf(this.topLeftInfo));
      lines.add("topRightInfo: " + String.valueOf(this.topRightInfo));
      lines.add("bottomLeftInfo: " + String.valueOf(this.bottomLeftInfo));
      lines.add("bottomRightInfo: " + String.valueOf(this.bottomRightInfo));
   }

   public void onTextureDeletion() {
      super.onTextureDeletion();
      if (this.branchUpdateChildRegion != null) {
         --this.branchUpdateChildRegion.activeBranchUpdateReferences;
      }

      this.topLeftInfo.onParentDeletion();
      this.topRightInfo.onParentDeletion();
      this.bottomLeftInfo.onParentDeletion();
      this.bottomRightInfo.onParentDeletion();
      this.reset();
   }

   public void requestDownload() {
      this.toUpload = true;
      this.updating = false;
   }

   public void writeCacheMapData(DataOutputStream output, byte[] usableBuffer, byte[] integerByteBuffer, LeveledRegion<BranchRegionTexture> inRegion) throws IOException {
      super.writeCacheMapData(output, usableBuffer, integerByteBuffer, inRegion);
      output.writeInt(this.topLeftInfo.usedTextureVersion);
      output.writeInt(this.topRightInfo.usedTextureVersion);
      output.writeInt(this.bottomLeftInfo.usedTextureVersion);
      output.writeInt(this.bottomRightInfo.usedTextureVersion);
   }

   public void readCacheData(int minorSaveVersion, int majorSaveVersion, DataInputStream input, byte[] usableBuffer, byte[] integerByteBuffer, LeveledRegion<BranchRegionTexture> inRegion, MapProcessor mapProcessor, int x, int y, boolean leafShouldAffectBranches) throws IOException {
      super.readCacheData(minorSaveVersion, majorSaveVersion, input, usableBuffer, integerByteBuffer, inRegion, mapProcessor, x, y, leafShouldAffectBranches);
      if (minorSaveVersion >= 15) {
         this.topLeftInfo.usedTextureVersion = input.readInt();
         this.topRightInfo.usedTextureVersion = input.readInt();
         this.bottomLeftInfo.usedTextureVersion = input.readInt();
         this.bottomRightInfo.usedTextureVersion = input.readInt();
      }

   }

   public class ChildTextureInfo {
      private int usedTextureVersion;
      private RegionTexture<?> temporaryReference;

      public ChildTextureInfo(final BranchRegionTexture this$0) {
      }

      private Integer getColorTextureForUpdate() {
         if ((this.temporaryReference != null || this.usedTextureVersion != 0) && (this.temporaryReference == null || this.temporaryReference.shouldBeUsedForBranchUpdate(this.usedTextureVersion))) {
            return this.temporaryReference != null && this.temporaryReference.shouldHaveContentForBranchUpdate() ? this.temporaryReference.glColorTexture : -1;
         } else {
            return null;
         }
      }

      private int getTextureVersion() {
         return this.temporaryReference != null && this.temporaryReference.shouldHaveContentForBranchUpdate() ? this.temporaryReference.textureVersion : 0;
      }

      private boolean hasLight() {
         return this.temporaryReference != null && this.temporaryReference.textureHasLight && this.temporaryReference.shouldHaveContentForBranchUpdate();
      }

      public void onUpdate(int newVersion) {
         this.usedTextureVersion = newVersion;
         if (this.temporaryReference != null) {
            this.temporaryReference = null;
         }

      }

      public void onParentDeletion() {
         if (this.temporaryReference != null) {
            this.temporaryReference = null;
         }

      }

      public Integer getReferenceColorTexture() {
         return this.temporaryReference == null ? null : this.temporaryReference.glColorTexture;
      }

      public String toString() {
         int var10000 = this.usedTextureVersion;
         return "tv " + var10000 + ", ct " + this.getReferenceColorTexture();
      }
   }
}
