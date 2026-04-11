package xaero.map.region;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import xaero.map.MapProcessor;
import xaero.map.file.MapSaveLoad;
import xaero.map.misc.Misc;
import xaero.map.region.texture.BranchRegionTexture;
import xaero.map.region.texture.RegionTexture;
import xaero.map.world.MapDimension;

public class BranchLeveledRegion extends LeveledRegion<BranchRegionTexture> {
   public static final int CHILD_LENGTH_IN_TEXTURES = 4;
   public static final int MAX_COORD_WITHIN_CHILD = 3;
   private boolean loaded;
   private boolean freed;
   private boolean readyForUpdates;
   private BranchRegionTexture[][] textures;
   private LeveledRegion<?>[][] children = new LeveledRegion[2][2];
   private boolean shouldCheckForUpdates;
   private boolean downloading;
   private long lastUpdateTime;
   private int updateCountSinceSave;

   public BranchLeveledRegion(String worldId, String dimId, String mwId, MapDimension dim, int level, int leveledX, int leveledZ, int caveLayer, BranchLeveledRegion parent) {
      super(worldId, dimId, mwId, dim, level, leveledX, leveledZ, caveLayer, parent);
      this.reset();
   }

   private void reset() {
      this.shouldCache = false;
      this.recacheHasBeenRequested = false;
      this.reloadHasBeenRequested = false;
      this.metaLoaded = false;
      this.loaded = false;
      this.freed = false;
      this.textures = null;
      this.downloading = false;
      this.updateCountSinceSave = 0;
      this.lastUpdateTime = 0L;
      this.readyForUpdates = false;
      this.resetBiomePalette();
   }

   private boolean checkAndTrackRegionExistence(MapProcessor mapProcessor, int x, int z) {
      MapDimension mapDimension = mapProcessor.getMapWorld().getCurrentDimension();
      MapLayer mapLayer = mapDimension.getLayeredMapRegions().getLayer(this.caveLayer);
      if (mapLayer.regionDetectionExists(x, z)) {
         return true;
      } else if (mapProcessor.getMapSaveLoad().isRegionDetectionComplete() && mapDimension.getHighlightHandler().shouldApplyRegionHighlights(x, z, false)) {
         mapLayer.getRegionHighlightExistenceTracker().track(x, z);
         return true;
      } else {
         return false;
      }
   }

   public void checkForUpdates(MapProcessor mapProcessor, boolean prevWaitingForBranchCache, boolean[] waitingForBranchCache, ArrayList<BranchLeveledRegion> branchRegionBuffer, int viewedLevel, int minViewedLeafX, int minViewedLeafZ, int maxViewedLeafX, int maxViewedLeafZ) {
      super.checkForUpdates(mapProcessor, prevWaitingForBranchCache, waitingForBranchCache, branchRegionBuffer, viewedLevel, minViewedLeafX, minViewedLeafZ, maxViewedLeafX, maxViewedLeafZ);
      if (!this.isLoaded()) {
         if (this.parent != null) {
            this.parent.setShouldCheckForUpdatesRecursive(true);
         }

         if (this.level == viewedLevel) {
            waitingForBranchCache[0] = true;
         }

         if (!this.recacheHasBeenRequested() && !this.reloadHasBeenRequested()) {
            this.calculateSortingDistance();
            Misc.addToListOfSmallest(10, branchRegionBuffer, this);
         }

      } else if (this.readyForUpdates && !prevWaitingForBranchCache) {
         synchronized(this) {
            if (!this.downloading && !this.recacheHasBeenRequested) {
               if (this.shouldCheckForUpdates) {
                  this.shouldCheckForUpdates = false;
                  boolean shouldRevisitParent = false;
                  boolean outdated = false;
                  int level = this.level;
                  int regionX = this.regionX;
                  int regionZ = this.regionZ;

                  for(int i = 0; i < 2; ++i) {
                     for(int j = 0; j < 2; ++j) {
                        LeveledRegion<?> childRegion = this.children[i][j];
                        int globalChildRegionX = regionX << 1 | i;
                        int globalChildRegionZ = regionZ << 1 | j;
                        int textureOffsetX = i * 4;
                        int textureOffsetY = j * 4;
                        boolean outdatedWithChild = false;
                        boolean outdatedWithLeaves = false;
                        boolean childRegionIsLoaded = childRegion != null && childRegion.isLoaded() || childRegion == null && level == 1 && !this.checkAndTrackRegionExistence(mapProcessor, globalChildRegionX, globalChildRegionZ);

                        for(int o = 0; o < 4; ++o) {
                           for(int p = 0; p < 4; ++p) {
                              int textureX = textureOffsetX + o;
                              int textureY = textureOffsetY + p;
                              BranchRegionTexture texture = this.getTexture(textureX, textureY);
                              int textureVersion = 0;
                              if (texture != null) {
                                 textureVersion = texture.getTextureVersion();
                                 if (textureVersion == -1) {
                                    textureVersion = texture.getBufferedTextureVersion();
                                 }
                              }

                              boolean leavesLoaded = true;
                              int leafTextureSum = -1;
                              int minLeafTextureX = (regionX << 3) + textureX << level;
                              int minLeafTextureZ = (regionZ << 3) + textureY << level;
                              int maxLeafTextureX = minLeafTextureX + (1 << level) - 1;
                              int maxLeafTextureZ = minLeafTextureZ + (1 << level) - 1;
                              int minLeafRegX = minLeafTextureX >> 3;
                              int minLeafRegZ = minLeafTextureZ >> 3;
                              int maxLeafRegX = maxLeafTextureX >> 3;
                              int maxLeafRegZ = maxLeafTextureZ >> 3;

                              int childTextureOffsetX;
                              int childTextureOffsetY;
                              label207:
                              for(childTextureOffsetX = minLeafRegX; childTextureOffsetX <= maxLeafRegX; ++childTextureOffsetX) {
                                 for(childTextureOffsetY = minLeafRegZ; childTextureOffsetY <= maxLeafRegZ; ++childTextureOffsetY) {
                                    MapRegion leafRegion = mapProcessor.getLeafMapRegion(this.caveLayer, childTextureOffsetX, childTextureOffsetY, false);
                                    if (leafRegion == null && this.checkAndTrackRegionExistence(mapProcessor, childTextureOffsetX, childTextureOffsetY)) {
                                       leavesLoaded = false;
                                       break label207;
                                    }

                                    if (leafRegion != null) {
                                       synchronized(leafRegion) {
                                          if (!leafRegion.isMetaLoaded() && !leafRegion.isLoaded()) {
                                             leavesLoaded = false;
                                             break label207;
                                          }

                                          if (leafTextureSum == -1) {
                                             leafTextureSum = this.leafTextureVersionSum[textureX][textureY];
                                          }
                                       }
                                    }
                                 }
                              }

                              if (leavesLoaded && leafTextureSum == -1) {
                                 leafTextureSum = 0;
                                 if (textureVersion != 0 && level > 1) {
                                    if (childRegion == null) {
                                       childRegion = this.children[i][j] = new BranchLeveledRegion(this.worldId, this.dimId, this.mwId, this.dim, level - 1, globalChildRegionX, globalChildRegionZ, this.caveLayer, this);
                                       this.dim.getLayeredMapRegions().addListRegion(childRegion);
                                       childRegionIsLoaded = false;
                                    }

                                    ((BranchLeveledRegion)childRegion).setShouldCheckForUpdatesRecursive(true);
                                 }
                              }

                              if (leavesLoaded && textureVersion != leafTextureSum) {
                                 outdatedWithLeaves = true;
                              }

                              if (childRegionIsLoaded) {
                                 childTextureOffsetX = o << 1;
                                 childTextureOffsetY = p << 1;
                                 RegionTexture<?> childTopLeft = null;
                                 RegionTexture<?> childTopRight = null;
                                 RegionTexture<?> childBottomLeft = null;
                                 RegionTexture<?> childBottomRight = null;
                                 if (childRegion != null) {
                                    childTopLeft = childRegion.getTexture(childTextureOffsetX, childTextureOffsetY);
                                    childTopRight = childRegion.getTexture(childTextureOffsetX + 1, childTextureOffsetY);
                                    childBottomLeft = childRegion.getTexture(childTextureOffsetX, childTextureOffsetY + 1);
                                    childBottomRight = childRegion.getTexture(childTextureOffsetX + 1, childTextureOffsetY + 1);
                                 }

                                 if (childTopLeft == null && childTopRight == null && childBottomLeft == null && childBottomRight == null) {
                                    if (texture != null) {
                                       this.putTexture(textureX, textureY, (BranchRegionTexture)null);
                                       texture.deleteTexturesAndBuffers();
                                       this.countTextureUpdate();
                                       outdatedWithChild = true;
                                       shouldRevisitParent = true;
                                    }
                                 } else {
                                    boolean newTexture = texture == null;
                                    if (newTexture) {
                                       texture = new BranchRegionTexture(this);
                                    }

                                    if (texture.checkForUpdates(childTopLeft, childTopRight, childBottomLeft, childBottomRight, childRegion)) {
                                       outdatedWithChild = true;
                                       if (newTexture) {
                                          this.putTexture(textureX, textureY, texture);
                                       }
                                    }
                                 }
                              }
                           }
                        }

                        if ((outdatedWithLeaves || outdatedWithChild) && childRegion != null) {
                           childRegion.checkForUpdates(mapProcessor, prevWaitingForBranchCache, waitingForBranchCache, branchRegionBuffer, viewedLevel, minViewedLeafX, minViewedLeafZ, maxViewedLeafX, maxViewedLeafZ);
                        }

                        if (outdatedWithChild) {
                           outdated = true;
                        }
                     }
                  }

                  if (outdated && this.freed) {
                     this.freed = false;
                     mapProcessor.addToProcess(this);
                  }

                  if (shouldRevisitParent && this.parent != null) {
                     this.parent.setShouldCheckForUpdatesRecursive(true);
                  }

               }
            } else {
               if (this.parent != null) {
                  this.parent.setShouldCheckForUpdatesRecursive(true);
               }

            }
         }
      } else {
         if (this.parent != null) {
            this.parent.setShouldCheckForUpdatesRecursive(true);
         }

      }
   }

   public void putTexture(int x, int y, BranchRegionTexture texture) {
      this.textures[x][y] = texture;
   }

   public BranchRegionTexture getTexture(int x, int y) {
      return this.textures[x][y];
   }

   public boolean hasTextures() {
      return this.textures != null;
   }

   public boolean isEmpty() {
      for(int i = 0; i < this.children.length; ++i) {
         for(int j = 0; j < this.children.length; ++j) {
            if (this.children[i][j] != null) {
               return false;
            }
         }
      }

      return true;
   }

   public void preCacheLoad() {
      this.textures = new BranchRegionTexture[8][8];
      this.freed = false;
   }

   protected void putLeaf(int X, int Z, MapRegion leaf) {
      int childLevel = this.level - 1;
      int childLevelX = X >> childLevel;
      int childLevelZ = Z >> childLevel;
      int localChildLevelX = childLevelX & 1;
      int localChildLevelZ = childLevelZ & 1;
      if (this.level == 1) {
         if (this.children[localChildLevelX][localChildLevelZ] == null) {
            leaf.setParent(this);
            this.children[localChildLevelX][localChildLevelZ] = leaf;
         }

      } else {
         LeveledRegion<?> childBranch = this.children[localChildLevelX][localChildLevelZ];
         if (childBranch == null) {
            childBranch = this.children[localChildLevelX][localChildLevelZ] = new BranchLeveledRegion(leaf.getWorldId(), leaf.getDimId(), leaf.getMwId(), this.dim, childLevel, childLevelX, childLevelZ, this.caveLayer, this);
            this.dim.getLayeredMapRegions().addListRegion(childBranch);
         }

         childBranch.putLeaf(X, Z, leaf);
      }
   }

   protected LeveledRegion<?> get(int leveledX, int leveledZ, int level) {
      if (this.level == level) {
         return this;
      } else {
         int childLevel = this.level - 1;
         if (level > childLevel) {
            throw new RuntimeException(new IllegalArgumentException());
         } else {
            int childLevelX = leveledX >> childLevel - level;
            int childLevelZ = leveledZ >> childLevel - level;
            int localChildLevelX = childLevelX & 1;
            int localChildLevelZ = childLevelZ & 1;
            LeveledRegion<?> childBranch = this.children[localChildLevelX][localChildLevelZ];
            return childBranch == null ? null : childBranch.get(leveledX, leveledZ, level);
         }
      }
   }

   protected boolean remove(int leveledX, int leveledZ, int level) {
      int childLevel = this.level - 1;
      if (level > childLevel) {
         throw new RuntimeException(new IllegalArgumentException());
      } else {
         int childLevelX = leveledX >> childLevel - level;
         int childLevelZ = leveledZ >> childLevel - level;
         int localChildLevelX = childLevelX & 1;
         int localChildLevelZ = childLevelZ & 1;
         LeveledRegion<?> childRegion = this.children[localChildLevelX][localChildLevelZ];
         if (level == childLevel) {
            if (childRegion != null) {
               this.children[localChildLevelX][localChildLevelZ] = null;
               return true;
            } else {
               return false;
            }
         } else {
            return childRegion == null ? false : childRegion.remove(leveledX, leveledZ, level);
         }
      }
   }

   public boolean loadingAnimation() {
      return !this.loaded;
   }

   public void addDebugLines(List<String> debugLines, MapProcessor mapProcessor, int textureX, int textureY) {
      super.addDebugLines(debugLines, mapProcessor, textureX, textureY);
      boolean var10001 = this.loaded;
      debugLines.add("loaded: " + var10001);
      var10001 = this.children[0][0] != null;
      debugLines.add("children: tl " + var10001 + " tr " + (this.children[1][0] != null) + " bl " + (this.children[0][1] != null) + " br " + (this.children[1][1] != null));
      var10001 = this.freed;
      debugLines.add("freed: " + var10001 + " shouldCheckForUpdates: " + this.shouldCheckForUpdates + " hasTextures: " + this.hasTextures());
      debugLines.add("updateCountSinceSave: " + this.updateCountSinceSave);
   }

   public boolean shouldEndProcessingAfterUpload() {
      return this.loaded;
   }

   public void setLoaded(boolean loaded) {
      this.loaded = loaded;
   }

   public boolean isLoaded() {
      return this.loaded;
   }

   public boolean cleanAndCacheRequestsBlocked() {
      return this.downloading || this.updateCountSinceSave > 0 && !this.recacheHasBeenRequested;
   }

   public void onProcessingEnd() {
      super.onProcessingEnd();
      this.freed = true;
      this.readyForUpdates = true;
   }

   public boolean shouldBeProcessed() {
      return this.loaded && !this.freed;
   }

   public void preCache() {
   }

   public void postCache(File permFile, MapSaveLoad mapSaveLoad, boolean successfullySaved) throws IOException {
      this.lastSaveTime = System.currentTimeMillis();
      this.updateCountSinceSave = 0;
   }

   public boolean skipCaching(int globalVersion) {
      return false;
   }

   public File findCacheFile(MapSaveLoad mapSaveLoad) throws IOException {
      Path subFolder = mapSaveLoad.getMWSubFolder(this.worldId, this.dimId, this.mwId);
      Path layerFolder = mapSaveLoad.getCaveLayerFolder(this.caveLayer, subFolder);
      Path rootCacheFolder = layerFolder.resolve("cache");
      Path levelCacheFolder = rootCacheFolder.resolve(this.level.makeConcatWithConstants<invokedynamic>(this.level));
      Files.createDirectories(levelCacheFolder);
      return levelCacheFolder.resolve(this.regionX + "_" + this.regionZ + ".xwmc").toFile();
   }

   public void onCurrentDimFinish(MapSaveLoad mapSaveLoad, MapProcessor mapProcessor) {
   }

   public void onLimiterRemoval(MapProcessor mapProcessor) {
      mapProcessor.removeMapRegion(this);
   }

   public void afterLimiterRemoval(MapProcessor mapProcessor) {
      this.reset();
   }

   public BranchRegionTexture createTexture(int x, int y) {
      return this.textures[x][y] = new BranchRegionTexture(this);
   }

   public void setShouldCheckForUpdatesRecursive(boolean shouldCheckForUpdates) {
      this.shouldCheckForUpdates = shouldCheckForUpdates;
      if (this.parent != null) {
         this.parent.setShouldCheckForUpdatesRecursive(shouldCheckForUpdates);
      }

   }

   public void setShouldCheckForUpdatesSingle(boolean shouldCheckForUpdates) {
      this.shouldCheckForUpdates = shouldCheckForUpdates;
   }

   public void startDownloadingTexturesForCache(MapProcessor mapProcessor) {
      synchronized(this) {
         this.recacheHasBeenRequested = true;
         this.shouldCache = true;
         this.downloading = true;
      }

      boolean hasSomething = false;

      for(int i = 0; i < 8; ++i) {
         for(int j = 0; j < 8; ++j) {
            BranchRegionTexture regionTexture = this.textures[i][j];
            if (regionTexture != null) {
               hasSomething = true;
               if (!regionTexture.shouldUpload() && !regionTexture.isCachePrepared()) {
                  regionTexture.requestDownload();
               }
            }
         }
      }

      if (this.freed) {
         this.freed = false;
         mapProcessor.addToProcess(this);
      }

      synchronized(this) {
         if (!hasSomething) {
            this.setAllCachePrepared(true);
         }

         this.downloading = false;
         this.updateCountSinceSave = 0;
      }
   }

   public void postTextureUpdate() {
      if (this.parent != null) {
         this.parent.setShouldCheckForUpdatesRecursive(true);
      }

      this.countTextureUpdate();
   }

   private void countTextureUpdate() {
      this.lastUpdateTime = System.currentTimeMillis();
      ++this.updateCountSinceSave;
   }

   public boolean eligibleForSaving(long currentTime) {
      return this.updateCountSinceSave > 0 && (this.updateCountSinceSave >= 64 || currentTime - this.lastUpdateTime > 1000L);
   }

   protected void onCacheLoadFailed(boolean[][] textureLoaded) {
      for(int i = 0; i < 8; ++i) {
         for(int j = 0; j < 8; ++j) {
            RegionTexture<?> texture = this.getTexture(i, j);
            if (texture != null && !textureLoaded[i][j]) {
               this.textures[i][j] = null;
               texture.deleteTexturesAndBuffers();
            }
         }
      }

   }
}
