package xaero.map.region;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import net.minecraft.class_1959;
import net.minecraft.class_2378;
import net.minecraft.class_2960;
import net.minecraft.class_5321;
import net.minecraft.class_7924;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.file.MapSaveLoad;
import xaero.map.file.OldFormatSupport;
import xaero.map.palette.FastPalette;
import xaero.map.palette.Paletted2DFastBitArrayIntStorage;
import xaero.map.region.texture.RegionTexture;
import xaero.map.world.MapDimension;

public abstract class LeveledRegion<T extends RegionTexture<T>> implements Comparable<LeveledRegion<T>> {
   public static final int SIDE_LENGTH = 8;
   private static int comparisonX = 0;
   private static int comparisonZ = 0;
   protected static int comparisonLevel;
   private static int comparisonLeafX = 0;
   private static int comparisonLeafZ = 0;
   protected BranchLeveledRegion parent;
   protected int caveLayer;
   protected int regionX;
   protected int regionZ;
   protected int level;
   private boolean allCachePrepared;
   protected boolean shouldCache;
   protected boolean recacheHasBeenRequested;
   protected boolean reloadHasBeenRequested;
   protected File cacheFile = null;
   protected String worldId;
   protected String dimId;
   protected String mwId;
   protected MapDimension dim;
   public int activeBranchUpdateReferences;
   public int[][] leafTextureVersionSum = new int[8][8];
   protected int[][] cachedTextureVersions = new int[8][8];
   protected boolean metaLoaded;
   private int distanceFromPlayerCache;
   private int leafDistanceFromPlayerCache;
   protected long lastSaveTime;
   private FastPalette<class_5321<class_1959>> biomePalette;

   public LeveledRegion(String worldId, String dimId, String mwId, MapDimension dim, int level, int leveledX, int leveledZ, int caveLayer, BranchLeveledRegion parent) {
      this.worldId = worldId;
      this.dimId = dimId;
      this.mwId = mwId;
      this.dim = dim;
      this.level = level;
      this.regionX = leveledX;
      this.regionZ = leveledZ;
      this.caveLayer = caveLayer;
      this.parent = parent;
   }

   public void onDimensionClear(MapProcessor mapProcessor) {
      this.deleteTexturesAndBuffers();
   }

   public void deleteTexturesAndBuffers() {
      synchronized(this) {
         this.setAllCachePrepared(false);
      }

      if (this.hasTextures()) {
         for(int i = 0; i < 8; ++i) {
            for(int j = 0; j < 8; ++j) {
               T texture = this.getTexture(i, j);
               if (texture != null) {
                  synchronized(this) {
                     this.setAllCachePrepared(false);
                     texture.setCachePrepared(false);
                  }

                  texture.deleteTexturesAndBuffers();
                  if (this.level > 0) {
                     this.putTexture(i, j, (RegionTexture)null);
                  }
               }
            }
         }
      }

   }

   public boolean hasTextures() {
      return true;
   }

   public void deleteBuffers() {
      synchronized(this) {
         this.setAllCachePrepared(false);
      }

      if (this.hasTextures()) {
         for(int i = 0; i < 8; ++i) {
            for(int j = 0; j < 8; ++j) {
               T texture = this.getTexture(i, j);
               if (texture != null && texture.getColorBuffer() != null) {
                  synchronized(this) {
                     this.setAllCachePrepared(false);
                     texture.setCachePrepared(false);
                  }

                  texture.setToUpload(false);
                  texture.deleteColorBuffer();
               }
            }
         }
      }

   }

   public void deleteGLBuffers() {
      if (this.hasTextures()) {
         for(int i = 0; i < 8; ++i) {
            for(int j = 0; j < 8; ++j) {
               T texture = this.getTexture(i, j);
               if (texture != null) {
                  texture.deletePBOs();
               }
            }
         }
      }

   }

   public boolean isAllCachePrepared() {
      return this.allCachePrepared;
   }

   public void setAllCachePrepared(boolean allCachePrepared) {
      if (this.allCachePrepared && !allCachePrepared && WorldMap.detailed_debug) {
         WorldMap.LOGGER.info("Cancelling cache: " + String.valueOf(this));
      }

      this.allCachePrepared = allCachePrepared;
   }

   public int getRegionX() {
      return this.regionX;
   }

   public int getRegionZ() {
      return this.regionZ;
   }

   public boolean shouldCache() {
      return this.shouldCache;
   }

   public int getLevel() {
      return this.level;
   }

   public void setShouldCache(boolean shouldCache, String by) {
      this.shouldCache = shouldCache;
      if (WorldMap.detailed_debug) {
         WorldMap.LOGGER.info("shouldCache set to " + shouldCache + " by " + by + " for " + String.valueOf(this));
      }

   }

   public boolean recacheHasBeenRequested() {
      return this.recacheHasBeenRequested;
   }

   public void setRecacheHasBeenRequested(boolean recacheHasBeenRequested, String by) {
      if (WorldMap.detailed_debug && recacheHasBeenRequested != this.recacheHasBeenRequested) {
         WorldMap.LOGGER.info("Recache set to " + recacheHasBeenRequested + " by " + by + " for " + String.valueOf(this));
      }

      this.recacheHasBeenRequested = recacheHasBeenRequested;
   }

   public File getCacheFile() {
      return this.cacheFile;
   }

   public void setCacheFile(File cacheFile) {
      this.cacheFile = cacheFile;
   }

   public MapDimension getDim() {
      return this.dim;
   }

   public String toString() {
      int var10000 = this.caveLayer;
      return "(" + var10000 + ") " + this.regionX + "_" + this.regionZ + " L" + this.level + " " + super.toString();
   }

   public boolean reloadHasBeenRequested() {
      return this.reloadHasBeenRequested;
   }

   public void setReloadHasBeenRequested(boolean reloadHasBeenRequested, String by) {
      if (WorldMap.detailed_debug && reloadHasBeenRequested != this.reloadHasBeenRequested) {
         WorldMap.LOGGER.info("Reload set to " + reloadHasBeenRequested + " by " + by + " for " + String.valueOf(this));
      }

      this.reloadHasBeenRequested = reloadHasBeenRequested;
   }

   public static void setComparison(int x, int z, int level, int leafX, int leafZ) {
      comparisonX = x;
      comparisonZ = z;
      comparisonLevel = level;
      comparisonLeafX = leafX;
      comparisonLeafZ = leafZ;
   }

   protected int distanceFromPlayer() {
      int toRegionX = (this.regionX << this.level >> comparisonLevel) - comparisonX;
      int toRegionZ = (this.regionZ << this.level >> comparisonLevel) - comparisonZ;
      return (int)Math.sqrt((double)(toRegionX * toRegionX + toRegionZ * toRegionZ));
   }

   protected int leafDistanceFromPlayer() {
      int toRegionX = (this.regionX << this.level) - comparisonLeafX;
      int toRegionZ = (this.regionZ << this.level) - comparisonLeafZ;
      return (int)Math.sqrt((double)(toRegionX * toRegionX + toRegionZ * toRegionZ));
   }

   public void calculateSortingDistance() {
      this.distanceFromPlayerCache = this.distanceFromPlayer();
      this.leafDistanceFromPlayerCache = this.leafDistanceFromPlayer();
   }

   protected int chunkDistanceFromPlayer() {
      int toRegionX = (this.regionX << this.level << 5) - comparisonX;
      int toRegionZ = (this.regionZ << this.level << 5) - comparisonZ;
      return (int)Math.sqrt((double)(toRegionX * toRegionX + toRegionZ * toRegionZ));
   }

   public void calculateSortingChunkDistance() {
      this.distanceFromPlayerCache = this.chunkDistanceFromPlayer();
      this.leafDistanceFromPlayerCache = this.distanceFromPlayerCache;
   }

   public int compareTo(LeveledRegion<T> arg0) {
      if (this.level == 3 && arg0.level != 3) {
         return -1;
      } else if (arg0.level == 3 && this.level != 3) {
         return 1;
      } else if (this.level == comparisonLevel && arg0.level != comparisonLevel) {
         return -1;
      } else if (arg0.level == comparisonLevel && this.level != comparisonLevel) {
         return 1;
      } else {
         int toRegion = this.distanceFromPlayerCache;
         int toRegion2 = arg0.distanceFromPlayerCache;
         if (toRegion > toRegion2) {
            return 1;
         } else if (toRegion == toRegion2) {
            toRegion = this.leafDistanceFromPlayerCache;
            toRegion2 = arg0.leafDistanceFromPlayerCache;
            if (toRegion > toRegion2) {
               return 1;
            } else {
               return toRegion == toRegion2 ? 0 : -1;
            }
         } else {
            return -1;
         }
      }
   }

   public void onProcessingEnd() {
   }

   public void addDebugLines(List<String> debugLines, MapProcessor mapProcessor, int textureX, int textureY) {
      boolean var10001 = mapProcessor.isProcessed(this);
      debugLines.add("processed: " + var10001);
      debugLines.add(String.format("recache: %s reload: %s metaLoaded: %s", this.recacheHasBeenRequested(), this.reloadHasBeenRequested(), this.metaLoaded));
      var10001 = this.shouldCache();
      debugLines.add("shouldCache: " + var10001 + " allCachePrepared: " + this.allCachePrepared);
      debugLines.add("activeBranchUpdateReferences: " + this.activeBranchUpdateReferences);
      int var10 = this.leafTextureVersionSum[textureX][textureY];
      debugLines.add("leafTextureVersionSum: " + var10 + " cachedTextureVersions: " + this.cachedTextureVersions[textureX][textureY] + " [" + textureX + "," + textureY + "]");
      if (this.biomePalette != null) {
         String biomePaletteLine = "";

         for(int i = 0; i < this.getBiomePaletteSize(); ++i) {
            if (i > 0) {
               biomePaletteLine = biomePaletteLine + ", ";
            }

            class_5321<class_1959> biomeKey = this.getBiomeKey(i);
            int count = biomeKey == null ? 0 : this.biomePalette.getCount(i);
            String biomeString = biomeKey == null ? "-" : biomeKey.method_29177().toString() + ":" + count;
            biomePaletteLine = biomePaletteLine + (biomeKey == null ? biomeString : biomeString.toString().substring(biomeString.indexOf(58) + 1));
         }

         debugLines.add(biomePaletteLine);
      }

   }

   protected void writeCacheMetaData(DataOutputStream output, byte[] usableBuffer, byte[] integerByteBuffer) throws IOException {
      for(int i = 0; i < 8; ++i) {
         for(int j = 0; j < 8; ++j) {
            T texture = this.getTexture(i, j);
            if (texture != null && texture.shouldIncludeInCache()) {
               if (!texture.isCachePrepared()) {
                  throw new RuntimeException("Trying to save cache but " + i + " " + j + " in " + String.valueOf(this) + " is not prepared.");
               }

               output.write(i << 4 | j);
               int bufferedTextureVersion = texture.getBufferedTextureVersion();
               output.writeInt(bufferedTextureVersion);
            }
         }
      }

      output.write(255);
   }

   public boolean saveCacheTextures(File tempFile, boolean debugConfig, int extraAttempts) {
      if (debugConfig) {
         WorldMap.LOGGER.info("Saving cache: " + String.valueOf(this));
      }

      boolean success = false;

      try {
         ZipOutputStream zipOutput = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile)));

         try {
            DataOutputStream output = new DataOutputStream(zipOutput);

            try {
               ZipEntry e = new ZipEntry("cache.xaero");
               zipOutput.putNextEntry(e);
               byte[] usableBuffer = new byte[16384];
               byte[] integerByteBuffer = new byte[4];
               int currentFullVersion = 65560;
               output.writeInt(currentFullVersion);
               this.writeCacheMetaData(output, usableBuffer, integerByteBuffer);
               this.saveBiomePalette(output);

               for(int i = 0; i < 8; ++i) {
                  for(int j = 0; j < 8; ++j) {
                     T texture = this.getTexture(i, j);
                     if (texture != null && texture.shouldIncludeInCache()) {
                        if (!texture.isCachePrepared()) {
                           throw new RuntimeException("Trying to save cache but " + i + " " + j + " in " + String.valueOf(this) + " is not prepared.");
                        }

                        output.write(i << 4 | j);
                        texture.writeCacheMapData(output, usableBuffer, integerByteBuffer, this);
                     }
                  }
               }

               output.write(255);
               zipOutput.closeEntry();
               success = true;
            } catch (Throwable var21) {
               try {
                  output.close();
               } catch (Throwable var20) {
                  var21.addSuppressed(var20);
               }

               throw var21;
            }

            output.close();
         } catch (Throwable var22) {
            try {
               zipOutput.close();
            } catch (Throwable var19) {
               var22.addSuppressed(var19);
            }

            throw var22;
         }

         zipOutput.close();
      } catch (IOException var23) {
         WorldMap.LOGGER.info("IO exception while trying to save cache textures for " + String.valueOf(this), var23);
         if (extraAttempts > 0) {
            WorldMap.LOGGER.info("Retrying...");

            try {
               Thread.sleep(20L);
            } catch (InterruptedException var16) {
            }

            return this.saveCacheTextures(tempFile, debugConfig, extraAttempts - 1);
         }
      }

      synchronized(this) {
         this.setAllCachePrepared(false);
      }

      for(int i = 0; i < 8; ++i) {
         for(int j = 0; j < 8; ++j) {
            T texture = this.getTexture(i, j);
            if (texture != null && texture.shouldIncludeInCache()) {
               texture.deleteColorBuffer();
               synchronized(this) {
                  texture.setCachePrepared(false);
                  this.setAllCachePrepared(false);
               }
            }
         }
      }

      return success;
   }

   protected void readCacheMetaData(DataInputStream input, int minorSaveVersion, int majorSaveVersion, byte[] usableBuffer, byte[] integerByteBuffer, boolean[][] textureLoaded, MapProcessor mapProcessor) throws IOException {
      if (minorSaveVersion == 8 || minorSaveVersion >= 12) {
         this.readCacheInput(true, input, minorSaveVersion, majorSaveVersion, usableBuffer, integerByteBuffer, textureLoaded, false, mapProcessor);
      }

   }

   public boolean loadCacheTextures(MapProcessor mapProcessor, class_2378<class_1959> biomeRegistry, boolean justMetaData, boolean[][] textureLoaded, int targetHighlightsHash, boolean[] leafShouldAffectBranchesDest, boolean[] metaLoadedDest, int extraAttempts, OldFormatSupport oldFormatSupport) {
      if (this.cacheFile == null) {
         return false;
      } else {
         if (this.cacheFile.exists()) {
            try {
               boolean var36 = false;

               label569: {
                  boolean var20;
                  int j;
                  label570: {
                     boolean leafShouldAffectBranches;
                     int j;
                     label571: {
                        boolean var48;
                        try {
                           ZipInputStream zipInput;
                           label524: {
                              label523: {
                                 label542: {
                                    var36 = true;
                                    zipInput = new ZipInputStream(new BufferedInputStream(new FileInputStream(this.cacheFile)));

                                    try {
                                       label543: {
                                          DataInputStream input = new DataInputStream(zipInput);

                                          label517: {
                                             label516: {
                                                label515: {
                                                   try {
                                                      ZipEntry entry = zipInput.getNextEntry();
                                                      if (entry == null) {
                                                         break label516;
                                                      }

                                                      byte[] integerByteBuffer = new byte[4];
                                                      int cacheFullSaveVersion = input.readInt();
                                                      int minorSaveVersion = cacheFullSaveVersion & '\uffff';
                                                      int majorSaveVersion = cacheFullSaveVersion >> 16 & '\uffff';
                                                      int currentFullVersion = 65560;
                                                      if (cacheFullSaveVersion > currentFullVersion || cacheFullSaveVersion == 7 || minorSaveVersion == 21) {
                                                         input.close();
                                                         WorldMap.LOGGER.info("Trying to load newer region cache " + String.valueOf(this) + " using an older version of Xaero's World Map!");
                                                         mapProcessor.getMapSaveLoad().backupFile(this.cacheFile, cacheFullSaveVersion);
                                                         this.cacheFile = null;
                                                         this.shouldCache = true;
                                                         var48 = false;
                                                         break label517;
                                                      }

                                                      if (cacheFullSaveVersion < currentFullVersion) {
                                                         this.shouldCache = true;
                                                      }

                                                      this.biomePalette = null;
                                                      byte[] usableBuffer = new byte[16384];
                                                      if (minorSaveVersion >= 8) {
                                                         this.readCacheMetaData(input, minorSaveVersion, majorSaveVersion, usableBuffer, integerByteBuffer, textureLoaded, mapProcessor);
                                                         metaLoadedDest[0] = true;
                                                         if (justMetaData && (minorSaveVersion == 8 || minorSaveVersion >= 12)) {
                                                            leafShouldAffectBranches = true;
                                                            break label515;
                                                         }
                                                      }

                                                      this.preCacheLoad();
                                                      this.loadBiomePalette(input, minorSaveVersion, majorSaveVersion, mapProcessor, biomeRegistry, oldFormatSupport);
                                                      leafShouldAffectBranches = this.shouldLeafAffectCache(targetHighlightsHash);
                                                      if (leafShouldAffectBranchesDest != null) {
                                                         leafShouldAffectBranchesDest[0] = leafShouldAffectBranches;
                                                      }

                                                      this.readCacheInput(false, input, minorSaveVersion, majorSaveVersion, usableBuffer, integerByteBuffer, textureLoaded, leafShouldAffectBranches, mapProcessor);
                                                      metaLoadedDest[0] = true;
                                                      zipInput.closeEntry();
                                                      var20 = false;
                                                   } catch (Throwable var40) {
                                                      try {
                                                         input.close();
                                                      } catch (Throwable var39) {
                                                         var40.addSuppressed(var39);
                                                      }

                                                      throw var40;
                                                   }

                                                   input.close();
                                                   break label543;
                                                }

                                                input.close();
                                                break label523;
                                             }

                                             input.close();
                                             break label542;
                                          }

                                          input.close();
                                          break label524;
                                       }
                                    } catch (Throwable var41) {
                                       try {
                                          zipInput.close();
                                       } catch (Throwable var38) {
                                          var41.addSuppressed(var38);
                                       }

                                       throw var41;
                                    }

                                    zipInput.close();
                                    var36 = false;
                                    break label570;
                                 }

                                 zipInput.close();
                                 var36 = false;
                                 break label569;
                              }

                              zipInput.close();
                              var36 = false;
                              break label571;
                           }

                           zipInput.close();
                           var36 = false;
                        } finally {
                           if (var36) {
                              int i = 0;

                              while(true) {
                                 if (i >= 8) {
                                    ;
                                 } else {
                                    for(int j = 0; j < 8; ++j) {
                                       RegionTexture<?> texture = this.getTexture(i, j);
                                       if (texture != null && texture.getBiomes() != null && texture.getBiomes().getRegionBiomePalette() != this.biomePalette) {
                                          texture.resetBiomes();
                                       }
                                    }

                                    ++i;
                                 }
                              }
                           }
                        }

                        for(int i = 0; i < 8; ++i) {
                           for(j = 0; j < 8; ++j) {
                              RegionTexture<?> texture = this.getTexture(i, j);
                              if (texture != null && texture.getBiomes() != null && texture.getBiomes().getRegionBiomePalette() != this.biomePalette) {
                                 texture.resetBiomes();
                              }
                           }
                        }

                        return var48;
                     }

                     for(j = 0; j < 8; ++j) {
                        for(j = 0; j < 8; ++j) {
                           RegionTexture<?> texture = this.getTexture(j, j);
                           if (texture != null && texture.getBiomes() != null && texture.getBiomes().getRegionBiomePalette() != this.biomePalette) {
                              texture.resetBiomes();
                           }
                        }
                     }

                     return leafShouldAffectBranches;
                  }

                  for(j = 0; j < 8; ++j) {
                     for(int j = 0; j < 8; ++j) {
                        RegionTexture<?> texture = this.getTexture(j, j);
                        if (texture != null && texture.getBiomes() != null && texture.getBiomes().getRegionBiomePalette() != this.biomePalette) {
                           texture.resetBiomes();
                        }
                     }
                  }

                  return var20;
               }

               for(int i = 0; i < 8; ++i) {
                  for(int j = 0; j < 8; ++j) {
                     RegionTexture<?> texture = this.getTexture(i, j);
                     if (texture != null && texture.getBiomes() != null && texture.getBiomes().getRegionBiomePalette() != this.biomePalette) {
                        texture.resetBiomes();
                     }
                  }
               }
            } catch (IOException var43) {
               WorldMap.LOGGER.error("IO exception while trying to load cache for region " + String.valueOf(this) + "! " + String.valueOf(this.cacheFile), var43);
               if (extraAttempts > 0) {
                  WorldMap.LOGGER.info("Retrying...");

                  try {
                     Thread.sleep(20L);
                  } catch (InterruptedException var37) {
                  }

                  metaLoadedDest[0] = false;
                  return this.loadCacheTextures(mapProcessor, biomeRegistry, justMetaData, textureLoaded, targetHighlightsHash, leafShouldAffectBranchesDest, metaLoadedDest, extraAttempts - 1, oldFormatSupport);
               }

               this.cacheFile = null;
               this.shouldCache = true;
               this.onCacheLoadFailed(textureLoaded);
            } catch (Throwable var44) {
               this.cacheFile = null;
               this.shouldCache = true;
               WorldMap.LOGGER.error("Failed to load cache for region " + String.valueOf(this) + "! " + String.valueOf(this.cacheFile), var44);
               this.onCacheLoadFailed(textureLoaded);
            }
         } else {
            this.cacheFile = null;
            this.shouldCache = true;
         }

         return false;
      }
   }

   protected abstract void onCacheLoadFailed(boolean[][] var1);

   public void saveBiomePalette(DataOutputStream output) throws IOException {
      int paletteSize = 0;
      if (this.biomePalette != null) {
         paletteSize = this.biomePalette.getSize();
      }

      output.writeInt(paletteSize);
      if (this.biomePalette != null) {
         for(int i = 0; i < paletteSize; ++i) {
            class_5321<class_1959> paletteKey = (class_5321)this.biomePalette.get(i);
            if (paletteKey == null) {
               output.write(255);
            } else {
               output.write(0);
               output.writeUTF(paletteKey.method_29177().toString());
            }
         }
      }

   }

   private void loadBiomePalette(DataInputStream input, int minorSaveVersion, int majorSaveVersion, MapProcessor mapProcessor, class_2378<class_1959> biomeRegistry, OldFormatSupport oldFormatSupport) throws IOException {
      if (minorSaveVersion >= 19) {
         int paletteSize = input.readInt();
         if (paletteSize > 0) {
            this.ensureBiomePalette();

            for(int i = 0; i < paletteSize; ++i) {
               int paletteElementType = input.read();
               if (paletteElementType == 255) {
                  this.biomePalette.addNull();
               } else {
                  class_2960 biomeResourceLocation;
                  if (paletteElementType == 0) {
                     biomeResourceLocation = class_2960.method_60654(input.readUTF());
                  } else {
                     int biomeInt = input.readInt();
                     String biomeString = oldFormatSupport.fixBiome(biomeInt, 5, (String)null);
                     if (biomeString == null) {
                        biomeString = "xaeroworldmap:unknown_biome_" + biomeInt;
                     }

                     biomeResourceLocation = class_2960.method_60654(biomeString + "_old_xaero");
                  }

                  class_5321<class_1959> biomeKey = class_5321.method_29179(class_7924.field_41236, biomeResourceLocation);
                  if (minorSaveVersion <= 20) {
                     input.readShort();
                  }

                  this.biomePalette.append(biomeKey, 0);
               }
            }
         } else if (paletteSize == -1) {
            this.shouldCache = true;
         }
      }

   }

   protected boolean shouldLeafAffectCache(int targetHighlightsHash) {
      return false;
   }

   private void readCacheInput(boolean isMeta, DataInputStream input, int minorSaveVersion, int majorSaveVersion, byte[] usableBuffer, byte[] integerByteBuffer, boolean[][] textureLoaded, boolean leafShouldAffectBranches, MapProcessor mapProcessor) throws IOException {
      for(int textureCoords = input.read(); textureCoords != -1 && textureCoords != 255; textureCoords = input.read()) {
         int x = textureCoords >> 4;
         int y = textureCoords & 15;
         if (isMeta) {
            int cachedTextureVersion = input.readInt();
            this.cachedTextureVersions[x][y] = cachedTextureVersion;
            this.updateLeafTextureVersion(x, y, cachedTextureVersion);
         } else {
            RegionTexture<T> texture = this.hasTextures() ? this.getTexture(x, y) : null;
            if (texture == null) {
               texture = this.createTexture(x, y);
               if (this.level == 0) {
                  synchronized(this) {
                     this.setAllCachePrepared(false);
                  }
               }
            }

            texture.readCacheData(minorSaveVersion, majorSaveVersion, input, usableBuffer, integerByteBuffer, this, mapProcessor, x, y, leafShouldAffectBranches);
         }

         if (textureLoaded != null) {
            textureLoaded[x][y] = true;
         }
      }

   }

   public int getAndResetCachedTextureVersion(int x, int y) {
      int result = this.cachedTextureVersions[x][y];
      this.cachedTextureVersions[x][y] = -1;
      return result;
   }

   public BranchLeveledRegion getParent() {
      return this.parent;
   }

   public boolean shouldAffectLoadingRequestFrequency() {
      return this.shouldBeProcessed();
   }

   protected void preCacheLoad() {
   }

   public void processWhenLoadedChunksExist(int globalRegionCacheHashCode) {
   }

   public boolean isMetaLoaded() {
      return this.metaLoaded;
   }

   public void confirmMetaLoaded() {
      this.metaLoaded = true;
   }

   public LeveledRegion<?> getRootRegion() {
      LeveledRegion<?> result = this;
      if (this.parent != null) {
         result = this.parent.getRootRegion();
      }

      return result;
   }

   public void checkForUpdates(MapProcessor mapProcessor, boolean prevWaitingForBranchCache, boolean[] waitingForBranchCache, ArrayList<BranchLeveledRegion> branchRegionBuffer, int viewedLevel, int minViewedLeafX, int minViewedLeafZ, int maxViewedLeafX, int maxViewedLeafZ) {
   }

   public void ensureBiomePalette() {
      if (this.biomePalette == null) {
         this.biomePalette = FastPalette.Builder.begin().setMaxCountPerElement(64).build();
      }

   }

   public class_5321<class_1959> getBiomeKey(int paletteIndex) {
      return this.biomePalette == null ? null : (class_5321)this.biomePalette.get(paletteIndex);
   }

   public int getBiomePaletteIndex(class_5321<class_1959> biome) {
      return this.biomePalette == null ? -1 : this.biomePalette.getIndex(biome);
   }

   public int onBiomeAddedToTexture(class_5321<class_1959> biome) {
      this.ensureBiomePalette();
      int paletteIndex = this.biomePalette.add(biome);
      this.biomePalette.count(paletteIndex, true);
      return paletteIndex;
   }

   public void onBiomeRemovedFromTexture(int paletteIndex) {
      if (paletteIndex < this.biomePalette.getSize() && this.biomePalette.get(paletteIndex) != null) {
         int count = this.biomePalette.count(paletteIndex, false);
         if (count == 0) {
            this.biomePalette.remove(paletteIndex);
         }

      }
   }

   public void uncountTextureBiomes(RegionTexture<?> texture) {
      if (texture != null && texture.getBiomes() != null) {
         Paletted2DFastBitArrayIntStorage biomeStorage = texture.getBiomes().getBiomeIndexStorage();
         int chunkPaletteSize = biomeStorage.getPaletteSize();

         for(int i = 0; i < chunkPaletteSize; ++i) {
            int biomeIndex = biomeStorage.getPaletteElement(i);
            if (biomeIndex != -1) {
               this.onBiomeRemovedFromTexture(biomeIndex);
            }
         }
      }

   }

   public int getBiomePaletteSize() {
      return this.biomePalette == null ? 0 : this.biomePalette.getSize();
   }

   public FastPalette<class_5321<class_1959>> getBiomePalette() {
      return this.biomePalette;
   }

   public void resetBiomePalette() {
      this.biomePalette = null;
   }

   public boolean isRefreshing() {
      return false;
   }

   public boolean shouldAllowAnotherRegionToLoad() {
      synchronized(this) {
         return !this.reloadHasBeenRequested() && !this.hasRemovableSourceData() && !this.isRefreshing();
      }
   }

   public abstract boolean shouldEndProcessingAfterUpload();

   public abstract T createTexture(int var1, int var2);

   public abstract void putTexture(int var1, int var2, T var3);

   public abstract T getTexture(int var1, int var2);

   protected abstract void putLeaf(int var1, int var2, MapRegion var3);

   protected abstract boolean remove(int var1, int var2, int var3);

   protected abstract LeveledRegion<?> get(int var1, int var2, int var3);

   public abstract boolean loadingAnimation();

   public abstract boolean cleanAndCacheRequestsBlocked();

   public abstract boolean shouldBeProcessed();

   public abstract boolean isLoaded();

   public abstract void preCache();

   public abstract void postCache(File var1, MapSaveLoad var2, boolean var3) throws IOException;

   public abstract boolean skipCaching(int var1);

   public abstract File findCacheFile(MapSaveLoad var1) throws IOException;

   public abstract void onCurrentDimFinish(MapSaveLoad var1, MapProcessor var2);

   public abstract void onLimiterRemoval(MapProcessor var1);

   public abstract void afterLimiterRemoval(MapProcessor var1);

   public String getExtraInfo() {
      return "";
   }

   public void updateLeafTextureVersion(int localTextureX, int localTextureZ, int newVersion) {
   }

   public boolean hasRemovableSourceData() {
      return false;
   }

   public int getCaveLayer() {
      return this.caveLayer;
   }
}
