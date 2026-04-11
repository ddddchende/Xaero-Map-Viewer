package xaero.map;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import net.minecraft.class_1047;
import net.minecraft.class_1058;
import net.minecraft.class_1059;
import net.minecraft.class_1087;
import net.minecraft.class_1657;
import net.minecraft.class_1921;
import net.minecraft.class_1937;
import net.minecraft.class_1944;
import net.minecraft.class_1959;
import net.minecraft.class_2189;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2320;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2356;
import net.minecraft.class_2378;
import net.minecraft.class_2404;
import net.minecraft.class_2464;
import net.minecraft.class_2521;
import net.minecraft.class_2586;
import net.minecraft.class_2680;
import net.minecraft.class_2688;
import net.minecraft.class_2806;
import net.minecraft.class_2812;
import net.minecraft.class_2818;
import net.minecraft.class_2826;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3298;
import net.minecraft.class_3481;
import net.minecraft.class_3610;
import net.minecraft.class_3619;
import net.minecraft.class_3620;
import net.minecraft.class_4696;
import net.minecraft.class_5321;
import net.minecraft.class_5819;
import net.minecraft.class_638;
import net.minecraft.class_773;
import net.minecraft.class_777;
import net.minecraft.class_8237;
import net.minecraft.class_8923;
import net.minecraft.class_2338.class_2339;
import net.minecraft.class_2902.class_2903;
import org.apache.logging.log4j.Logger;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.common.reflection.util.ReflectionUtils;
import xaero.lib.common.util.ImageIOUtils;
import xaero.map.biome.BiomeColorCalculator;
import xaero.map.biome.BiomeGetter;
import xaero.map.biome.BlockTintProvider;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.config.util.WorldMapClientConfigUtils;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.exception.SilentException;
import xaero.map.gui.GuiMap;
import xaero.map.misc.CachedFunction;
import xaero.map.misc.Misc;
import xaero.map.mods.SupportMods;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapBlock;
import xaero.map.region.MapRegion;
import xaero.map.region.MapTile;
import xaero.map.region.MapTileChunk;
import xaero.map.region.MapUpdateFastConfig;
import xaero.map.region.Overlay;
import xaero.map.region.OverlayBuilder;
import xaero.map.region.OverlayManager;

public abstract class MapWriter {
   public static final int NO_Y_VALUE = 32767;
   public static final int MAX_TRANSPARENCY_BLEND_DEPTH = 5;
   public static final String[] DEFAULT_RESOURCE = new String[]{"minecraft", ""};
   private int X;
   private int Z;
   private int playerChunkX;
   private int playerChunkZ;
   private int loadDistance;
   private int startTileChunkX;
   private int startTileChunkZ;
   private int endTileChunkX;
   private int endTileChunkZ;
   private int insideX;
   private int insideZ;
   private long updateCounter;
   private int caveStart;
   private int writingLayer = Integer.MAX_VALUE;
   private int writtenCaveStart = Integer.MAX_VALUE;
   private boolean clearCachedColours;
   private MapBlock loadingObject = new MapBlock();
   private OverlayBuilder overlayBuilder;
   private final class_2339 mutableLocalPos;
   private final class_2339 mutableGlobalPos;
   protected final class_5819 usedRandom = class_5819.method_43049(0L);
   private long lastWrite = -1L;
   private long lastWriteTry = -1L;
   private int workingFrameCount;
   private long framesFreedTime = -1L;
   public long writeFreeSinceLastWrite = -1L;
   private int writeFreeSizeTiles;
   private int writeFreeFullUpdateTargetTime;
   private MapProcessor mapProcessor;
   private ArrayList<class_2680> buggedStates;
   private BlockStateShortShapeCache blockStateShortShapeCache;
   private int topH;
   private final CachedFunction<class_2688<?, ?>, Boolean> transparentCache;
   private int firstTransparentStateY;
   private final class_2339 mutableBlockPos3;
   private CachedFunction<class_3610, class_2680> fluidToBlock;
   private BiomeGetter biomeGetter;
   private ArrayList<MapRegion> regionBuffer = new ArrayList();
   private MapTileChunk rightChunk = null;
   private MapTileChunk bottomRightChunk = null;
   private HashMap<String, Integer> textureColours = new HashMap();
   private HashMap<class_2680, Integer> blockColours = new HashMap();
   private final Object2IntMap<class_2680> blockTintIndices;
   private long lastLayerSwitch;
   private class_2680 lastBlockStateForTextureColor = null;
   private int lastBlockStateForTextureColorResult = -1;

   public MapWriter(OverlayManager overlayManager, BlockStateShortShapeCache blockStateShortShapeCache, BiomeGetter biomeGetter) {
      this.overlayBuilder = new OverlayBuilder(overlayManager);
      this.mutableLocalPos = new class_2339();
      this.mutableGlobalPos = new class_2339();
      this.buggedStates = new ArrayList();
      this.blockStateShortShapeCache = blockStateShortShapeCache;
      this.transparentCache = new CachedFunction((state) -> {
         return this.shouldOverlay(state);
      });
      this.mutableBlockPos3 = new class_2339();
      this.fluidToBlock = new CachedFunction(class_3610::method_15759);
      this.biomeGetter = biomeGetter;
      this.blockTintIndices = new Object2IntOpenHashMap();
   }

   protected abstract boolean blockStateHasTranslucentRenderType(class_2680 var1);

   public void onRender(BiomeColorCalculator biomeColorCalculator, OverlayManager overlayManager) {
      long var3 = System.nanoTime();

      try {
         if (WorldMap.crashHandler.getCrashedBy() == null) {
            synchronized(this.mapProcessor.renderThreadPauseSync) {
               if (!this.mapProcessor.isWritingPaused() && !this.mapProcessor.isWaitingForWorldUpdate() && this.mapProcessor.getMapSaveLoad().isRegionDetectionComplete() && this.mapProcessor.isCurrentMultiworldWritable()) {
                  ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
                  boolean loadChunksConfig = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.LOAD_NEW_CHUNKS);
                  boolean updateChunksConfig = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.UPDATE_CHUNKS);
                  if (this.mapProcessor.getWorld() == null || this.mapProcessor.isCurrentMapLocked() || this.mapProcessor.getMapWorld().isCacheOnlyMode()) {
                     return;
                  }

                  if (this.mapProcessor.getCurrentWorldId() != null && !this.mapProcessor.ignoreWorld(this.mapProcessor.getWorld()) && (updateChunksConfig || loadChunksConfig || this.mapProcessor.getMapWorld().getCurrentDimension().isUsingWorldSave())) {
                     double playerX;
                     double playerY;
                     double playerZ;
                     synchronized(this.mapProcessor.mainStuffSync) {
                        if (this.mapProcessor.mainWorld != this.mapProcessor.getWorld()) {
                           return;
                        }

                        if (this.mapProcessor.getWorld().method_27983() != this.mapProcessor.getMapWorld().getCurrentDimensionId()) {
                           return;
                        }

                        playerX = this.mapProcessor.mainPlayerX;
                        playerY = this.mapProcessor.mainPlayerY;
                        playerZ = this.mapProcessor.mainPlayerZ;
                     }

                     XaeroWorldMapCore.ensureField();
                     int lengthX = this.endTileChunkX - this.startTileChunkX + 1;
                     int lengthZ = this.endTileChunkZ - this.startTileChunkZ + 1;
                     if (this.lastWriteTry == -1L) {
                        lengthX = 3;
                        lengthZ = 3;
                     }

                     int sizeTileChunks = lengthX * lengthZ;
                     int sizeTiles = sizeTileChunks * 4 * 4;
                     int sizeBasedTargetTime = sizeTiles * 1000 / 1500;
                     int fullUpdateTargetTime = Math.max(100, sizeBasedTargetTime);
                     long time = System.currentTimeMillis();
                     long passed = this.lastWrite == -1L ? 0L : time - this.lastWrite;
                     if (this.lastWriteTry == -1L || this.writeFreeSizeTiles != sizeTiles || this.writeFreeFullUpdateTargetTime != fullUpdateTargetTime || this.workingFrameCount > 30) {
                        this.framesFreedTime = time;
                        this.writeFreeSizeTiles = sizeTiles;
                        this.writeFreeFullUpdateTargetTime = fullUpdateTargetTime;
                        this.workingFrameCount = 0;
                     }

                     long sinceLastWrite = Math.min(passed, this.writeFreeSinceLastWrite);
                     if (this.framesFreedTime != -1L) {
                        sinceLastWrite = time - this.framesFreedTime;
                     }

                     long tilesToUpdate = Math.min(sinceLastWrite * (long)sizeTiles / (long)fullUpdateTargetTime, 100L);
                     if (this.lastWrite == -1L || tilesToUpdate != 0L) {
                        this.lastWrite = time;
                     }

                     int timeLimit;
                     boolean shouldRequestLoading;
                     int counter;
                     if (tilesToUpdate != 0L) {
                        if (this.framesFreedTime != -1L) {
                           this.writeFreeSinceLastWrite = sinceLastWrite;
                           this.framesFreedTime = -1L;
                        } else {
                           timeLimit = (int)(Math.min(sinceLastWrite, 50L) * 86960L);
                           long writeStartNano = System.nanoTime();
                           class_2378<class_1959> biomeRegistry = this.mapProcessor.worldBiomeRegistry;
                           shouldRequestLoading = loadChunksConfig || this.mapProcessor.getMapWorld().getCurrentDimension().isUsingWorldSave();
                           boolean updateChunks = updateChunksConfig || this.mapProcessor.getMapWorld().getCurrentDimension().isUsingWorldSave();
                           boolean ignoreHeightmaps = this.mapProcessor.getMapWorld().isIgnoreHeightmaps();
                           boolean flowers = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.FLOWERS);
                           boolean detailedDebug = WorldMap.detailed_debug;
                           counter = (Integer)configManager.getEffective(WorldMapProfiledConfigOptions.CAVE_MODE_DEPTH);
                           class_2339 mutableBlockPos3 = this.mutableBlockPos3;
                           BlockTintProvider blockTintProvider = this.mapProcessor.getWorldBlockTintProvider();
                           MapUpdateFastConfig config = new MapUpdateFastConfig();
                           class_638 world = this.mapProcessor.getWorld();
                           class_2378<class_2248> blockRegistry = this.mapProcessor.getWorldBlockRegistry();

                           for(int i = 0; (long)i < tilesToUpdate; ++i) {
                              if (this.writeMap(world, blockRegistry, playerX, playerY, playerZ, biomeRegistry, biomeColorCalculator, overlayManager, shouldRequestLoading, updateChunks, ignoreHeightmaps, flowers, detailedDebug, mutableBlockPos3, blockTintProvider, counter, config)) {
                                 --i;
                              }

                              if (System.nanoTime() - writeStartNano >= (long)timeLimit) {
                                 break;
                              }
                           }

                           ++this.workingFrameCount;
                        }
                     }

                     this.lastWriteTry = time;
                     timeLimit = this.startTileChunkX >> 3;
                     int startRegionZ = this.startTileChunkZ >> 3;
                     int endRegionX = this.endTileChunkX >> 3;
                     int endRegionZ = this.endTileChunkZ >> 3;
                     shouldRequestLoading = false;
                     LeveledRegion<?> nextToLoad = this.mapProcessor.getMapSaveLoad().getNextToLoadByViewing();
                     if (nextToLoad != null) {
                        shouldRequestLoading = nextToLoad.shouldAllowAnotherRegionToLoad();
                     } else {
                        shouldRequestLoading = true;
                     }

                     this.regionBuffer.clear();
                     int comparisonChunkX = this.playerChunkX - 16;
                     int comparisonChunkZ = this.playerChunkZ - 16;
                     LeveledRegion.setComparison(comparisonChunkX, comparisonChunkZ, 0, comparisonChunkX, comparisonChunkZ);

                     for(int visitRegionX = timeLimit; visitRegionX <= endRegionX; ++visitRegionX) {
                        for(counter = startRegionZ; counter <= endRegionZ; ++counter) {
                           MapRegion visitRegion = this.mapProcessor.getLeafMapRegion(this.writingLayer, visitRegionX, counter, true);
                           if (visitRegion != null && visitRegion.getLoadState() == 2) {
                              visitRegion.registerVisit();
                           }

                           synchronized(visitRegion) {
                              if (visitRegion.isResting() && shouldRequestLoading && visitRegion.canRequestReload_unsynced() && visitRegion.getLoadState() != 2) {
                                 visitRegion.calculateSortingChunkDistance();
                                 Misc.addToListOfSmallest(10, this.regionBuffer, visitRegion);
                              }
                           }
                        }
                     }

                     int toRequest = 1;
                     counter = 0;

                     for(int i = 0; i < this.regionBuffer.size() && counter < toRequest; ++i) {
                        MapRegion region = (MapRegion)this.regionBuffer.get(i);
                        if (region != nextToLoad || this.regionBuffer.size() <= 1) {
                           synchronized(region) {
                              if (region.canRequestReload_unsynced() && region.getLoadState() != 2) {
                                 region.setBeingWritten(true);
                                 this.mapProcessor.getMapSaveLoad().requestLoad(region, "writing");
                                 if (counter == 0) {
                                    this.mapProcessor.getMapSaveLoad().setNextToLoadByViewing(region);
                                 }

                                 ++counter;
                                 if (region.getLoadState() == 4) {
                                    return;
                                 }
                              }
                           }
                        }
                     }
                  }
               }

               return;
            }
         }
      } catch (Throwable var52) {
         WorldMap.crashHandler.setCrashedBy(var52);
      }

   }

   private int getWriteDistance() {
      int limit = this.mapProcessor.getMapWorld().getCurrentDimension().isUsingWorldSave() ? Integer.MAX_VALUE : (Integer)WorldMap.INSTANCE.getConfigs().getClientConfigManager().getEffective(WorldMapProfiledConfigOptions.WRITING_DISTANCE);
      if (limit < 0) {
         limit = Integer.MAX_VALUE;
      }

      return Math.min(limit, Math.min(32, (Integer)class_310.method_1551().field_1690.method_42503().method_41753()));
   }

   public boolean writeMap(class_1937 world, class_2378<class_2248> blockRegistry, double playerX, double playerY, double playerZ, class_2378<class_1959> biomeRegistry, BiomeColorCalculator biomeColorCalculator, OverlayManager overlayManager, boolean loadChunks, boolean updateChunks, boolean ignoreHeightmaps, boolean flowers, boolean detailedDebug, class_2339 mutableBlockPos3, BlockTintProvider blockTintProvider, int caveDepth, MapUpdateFastConfig config) {
      boolean onlyLoad = loadChunks && (!updateChunks || this.updateCounter % 5L != 0L);
      synchronized(world) {
         int newWritingLayer;
         if (this.insideX == 0 && this.insideZ == 0) {
            if (this.X == 0 && this.Z == 0) {
               this.writtenCaveStart = this.caveStart;
            }

            this.mapProcessor.updateCaveStart();
            newWritingLayer = this.mapProcessor.getCurrentCaveLayer();
            if (this.writingLayer != newWritingLayer && System.currentTimeMillis() - this.lastLayerSwitch > 300L) {
               this.writingLayer = newWritingLayer;
               this.lastLayerSwitch = System.currentTimeMillis();
            }

            this.loadDistance = this.getWriteDistance();
            if (this.writingLayer != Integer.MAX_VALUE && !(class_310.method_1551().field_1755 instanceof GuiMap)) {
               this.loadDistance = Math.min(16, this.loadDistance);
            }

            this.caveStart = this.mapProcessor.getMapWorld().getCurrentDimension().getLayeredMapRegions().getLayer(this.writingLayer).getCaveStart();
            if (this.caveStart != this.writtenCaveStart) {
               this.loadDistance = Math.min(4, this.loadDistance);
            }

            this.playerChunkX = (int)Math.floor(playerX) >> 4;
            this.playerChunkZ = (int)Math.floor(playerZ) >> 4;
            this.startTileChunkX = this.playerChunkX - this.loadDistance >> 2;
            this.startTileChunkZ = this.playerChunkZ - this.loadDistance >> 2;
            this.endTileChunkX = this.playerChunkX + this.loadDistance >> 2;
            this.endTileChunkZ = this.playerChunkZ + this.loadDistance >> 2;
         }

         newWritingLayer = this.startTileChunkX + this.X;
         int tileChunkZ = this.startTileChunkZ + this.Z;
         int tileChunkLocalX = newWritingLayer & 7;
         int tileChunkLocalZ = tileChunkZ & 7;
         int chunkX = newWritingLayer * 4 + this.insideX;
         int chunkZ = tileChunkZ * 4 + this.insideZ;
         boolean wasSkipped = this.writeChunk(world, blockRegistry, this.loadDistance, onlyLoad, biomeRegistry, overlayManager, loadChunks, updateChunks, ignoreHeightmaps, flowers, detailedDebug, mutableBlockPos3, blockTintProvider, caveDepth, this.caveStart, this.writingLayer, newWritingLayer, tileChunkZ, tileChunkLocalX, tileChunkLocalZ, chunkX, chunkZ, config);
         return wasSkipped && (Math.abs(chunkX - this.playerChunkX) > 8 || Math.abs(chunkZ - this.playerChunkZ) > 8);
      }
   }

   public boolean writeChunk(class_1937 world, class_2378<class_2248> blockRegistry, int distance, boolean onlyLoad, class_2378<class_1959> biomeRegistry, OverlayManager overlayManager, boolean loadChunks, boolean updateChunks, boolean ignoreHeightmaps, boolean flowers, boolean detailedDebug, class_2339 mutableBlockPos3, BlockTintProvider blockTintProvider, int caveDepth, int caveStart, int layerToWrite, int tileChunkX, int tileChunkZ, int tileChunkLocalX, int tileChunkLocalZ, int chunkX, int chunkZ, MapUpdateFastConfig updateConfig) {
      int regionX = tileChunkX >> 3;
      int regionZ = tileChunkZ >> 3;
      MapTileChunk tileChunk = null;
      this.rightChunk = null;
      MapTileChunk bottomChunk = null;
      this.bottomRightChunk = null;
      int worldBottomY = world.method_31607();
      int worldTopY = world.method_31600();
      MapRegion region = this.mapProcessor.getLeafMapRegion(layerToWrite, regionX, regionZ, true);
      boolean wasSkipped = true;
      synchronized(region.writerThreadPauseSync) {
         if (!region.isWritingPaused()) {
            boolean createdTileChunk = false;
            boolean regionIsResting;
            boolean isProperLoadState;
            synchronized(region) {
               isProperLoadState = region.getLoadState() == 2;
               if (isProperLoadState) {
                  region.registerVisit();
               }

               regionIsResting = region.isResting();
               if (regionIsResting) {
                  region.setBeingWritten(true);
                  tileChunk = region.getChunk(tileChunkLocalX, tileChunkLocalZ);
                  if (isProperLoadState && tileChunk == null) {
                     region.setChunk(tileChunkLocalX, tileChunkLocalZ, tileChunk = new MapTileChunk(region, tileChunkX, tileChunkZ));
                     tileChunk.setLoadState((byte)2);
                     region.setAllCachePrepared(false);
                     createdTileChunk = true;
                  }

                  if (!region.isNormalMapData()) {
                     region.getDim().getLayeredMapRegions().applyToEachLoadedLayer((ix, layer) -> {
                        if (ix != region.getCaveLayer()) {
                           MapRegion sameRegionAnotherLayer = this.mapProcessor.getLeafMapRegion(ix, regionX, regionZ, true);
                           sameRegionAnotherLayer.setOutdatedWithOtherLayers(true);
                           sameRegionAnotherLayer.setHasHadTerrain();
                        }

                     });
                  }
               }
            }

            if (regionIsResting && isProperLoadState) {
               if (tileChunk != null && tileChunk.getLoadState() == 2) {
                  if (!tileChunk.getLeafTexture().shouldUpload()) {
                     boolean cave = caveStart != Integer.MAX_VALUE;
                     boolean fullCave = caveStart == Integer.MIN_VALUE;
                     int lowH = worldBottomY;
                     if (cave && !fullCave) {
                        lowH = caveStart + 1 - caveDepth;
                        if (lowH < worldBottomY) {
                           lowH = worldBottomY;
                        }
                     }

                     if (chunkX >= this.playerChunkX - distance && chunkX <= this.playerChunkX + distance && chunkZ >= this.playerChunkZ - distance && chunkZ <= this.playerChunkZ + distance) {
                        class_2818 chunk = (class_2818)world.method_8402(chunkX, chunkZ, class_2806.field_12803, false);
                        MapTile mapTile = tileChunk.getTile(this.insideX, this.insideZ);
                        boolean chunkUpdated = false;

                        try {
                           chunkUpdated = chunk != null && (mapTile == null || mapTile.getWrittenCaveStart() != caveStart || mapTile.getWrittenCaveDepth() != caveDepth || !(Boolean)XaeroWorldMapCore.chunkCleanField.get(chunk));
                        } catch (IllegalAccessException | IllegalArgumentException var64) {
                           throw new RuntimeException(var64);
                        }

                        if (chunkUpdated && !(chunk instanceof class_2812)) {
                           boolean edgeChunk = false;

                           label373:
                           for(int i = -1; i < 2; ++i) {
                              for(int j = -1; j < 2; ++j) {
                                 if (i != 0 || j != 0) {
                                    class_2818 neighbor = world.method_8497(chunkX + i, chunkZ + j);
                                    if (neighbor == null || neighbor instanceof class_2812) {
                                       edgeChunk = true;
                                       break label373;
                                    }
                                 }
                              }
                           }

                           if (!edgeChunk && (mapTile == null && loadChunks || mapTile != null && updateChunks && (!onlyLoad || mapTile.getWrittenCaveStart() != caveStart || mapTile.getWrittenCaveDepth() != caveDepth))) {
                              wasSkipped = false;
                              if (mapTile == null) {
                                 mapTile = this.mapProcessor.getTilePool().get(this.mapProcessor.getCurrentDimension(), chunkX, chunkZ);
                                 tileChunk.setChanged(true);
                              }

                              MapTileChunk prevTileChunk = tileChunk.getNeighbourTileChunk(0, -1, this.mapProcessor, false);
                              MapTileChunk prevTileChunkDiagonal = tileChunk.getNeighbourTileChunk(-1, -1, this.mapProcessor, false);
                              MapTileChunk prevTileChunkHorisontal = tileChunk.getNeighbourTileChunk(-1, 0, this.mapProcessor, false);
                              int sectionBasedHeight = this.getSectionBasedHeight(chunk, 64);
                              class_2903 typeWorldSurface = class_2903.field_13202;
                              MapTile bottomTile = this.insideZ < 3 ? tileChunk.getTile(this.insideX, this.insideZ + 1) : null;
                              MapTile rightTile = this.insideX < 3 ? tileChunk.getTile(this.insideX + 1, this.insideZ) : null;
                              boolean triedFetchingBottomChunk = false;
                              boolean triedFetchingRightChunk = false;
                              int x = 0;

                              while(true) {
                                 if (x >= 16) {
                                    mapTile.setWorldInterpretationVersion(1);
                                    if (mapTile.getWrittenCaveStart() != caveStart) {
                                       tileChunk.setChanged(true);
                                    }

                                    mapTile.setWrittenCave(caveStart, caveDepth);
                                    tileChunk.setTile(this.insideX, this.insideZ, mapTile, this.blockStateShortShapeCache);
                                    mapTile.setWrittenOnce(true);
                                    mapTile.setLoaded(true);
                                    ReflectionUtils.setReflectFieldValue(chunk, XaeroWorldMapCore.chunkCleanField, true);
                                    break;
                                 }

                                 for(int z = 0; z < 16; ++z) {
                                    int mappedHeight = chunk.method_12005(typeWorldSurface, x, z);
                                    int startHeight;
                                    if (cave && !fullCave) {
                                       startHeight = caveStart;
                                    } else if (!ignoreHeightmaps && mappedHeight >= worldBottomY) {
                                       startHeight = mappedHeight;
                                    } else {
                                       startHeight = sectionBasedHeight;
                                    }

                                    if (startHeight >= worldTopY) {
                                       startHeight = worldTopY - 1;
                                    }

                                    MapBlock currentPixel = mapTile.isLoaded() ? mapTile.getBlock(x, z) : null;
                                    this.loadPixel(world, blockRegistry, this.loadingObject, currentPixel, chunk, x, z, startHeight, lowH, cave, fullCave, mappedHeight, mapTile.wasWrittenOnce(), ignoreHeightmaps, biomeRegistry, flowers, worldBottomY, mutableBlockPos3);
                                    this.loadingObject.fixHeightType(x, z, mapTile, tileChunk, prevTileChunk, prevTileChunkDiagonal, prevTileChunkHorisontal, this.loadingObject.getEffectiveHeight(this.blockStateShortShapeCache, updateConfig), true, this.blockStateShortShapeCache, updateConfig);
                                    boolean equalsSlopesExcluded = this.loadingObject.equalsSlopesExcluded(currentPixel);
                                    boolean fullyEqual = this.loadingObject.equals(currentPixel, equalsSlopesExcluded);
                                    if (!fullyEqual) {
                                       MapBlock loadedBlock = this.loadingObject;
                                       mapTile.setBlock(x, z, loadedBlock);
                                       if (currentPixel != null) {
                                          this.loadingObject = currentPixel;
                                       } else {
                                          this.loadingObject = new MapBlock();
                                       }

                                       if (!equalsSlopesExcluded) {
                                          tileChunk.setChanged(true);
                                          boolean zEdge = z == 15;
                                          boolean xEdge = x == 15;
                                          if ((zEdge || xEdge) && (currentPixel == null || currentPixel.getEffectiveHeight(this.blockStateShortShapeCache, updateConfig) != loadedBlock.getEffectiveHeight(this.blockStateShortShapeCache, updateConfig))) {
                                             if (zEdge) {
                                                if (!triedFetchingBottomChunk && bottomTile == null && this.insideZ == 3 && tileChunkLocalZ < 7) {
                                                   bottomChunk = region.getChunk(tileChunkLocalX, tileChunkLocalZ + 1);
                                                   triedFetchingBottomChunk = true;
                                                   bottomTile = bottomChunk != null ? bottomChunk.getTile(this.insideX, 0) : null;
                                                   if (bottomTile != null) {
                                                      bottomChunk.setChanged(true);
                                                   }
                                                }

                                                if (bottomTile != null && bottomTile.isLoaded()) {
                                                   bottomTile.getBlock(x, 0).setSlopeUnknown(true);
                                                   if (!xEdge) {
                                                      bottomTile.getBlock(x + 1, 0).setSlopeUnknown(true);
                                                   }
                                                }

                                                if (xEdge) {
                                                   this.updateBottomRightTile(region, tileChunk, bottomChunk, tileChunkLocalX, tileChunkLocalZ);
                                                }
                                             } else if (xEdge) {
                                                if (!triedFetchingRightChunk && rightTile == null && this.insideX == 3 && tileChunkLocalX < 7) {
                                                   this.rightChunk = region.getChunk(tileChunkLocalX + 1, tileChunkLocalZ);
                                                   triedFetchingRightChunk = true;
                                                   rightTile = this.rightChunk != null ? this.rightChunk.getTile(0, this.insideZ) : null;
                                                   if (rightTile != null) {
                                                      this.rightChunk.setChanged(true);
                                                   }
                                                }

                                                if (rightTile != null && rightTile.isLoaded()) {
                                                   rightTile.getBlock(0, z + 1).setSlopeUnknown(true);
                                                }
                                             }
                                          }
                                       }
                                    }
                                 }

                                 ++x;
                              }
                           }
                        }
                     }
                  }

                  if (createdTileChunk) {
                     if (tileChunk.includeInSave()) {
                        tileChunk.setHasHadTerrain();
                     }

                     this.mapProcessor.getMapRegionHighlightsPreparer().prepare(region, tileChunkLocalX, tileChunkLocalZ, false);
                     if (!tileChunk.includeInSave() && !tileChunk.hasHighlightsIfUndiscovered()) {
                        region.setChunk(tileChunkLocalX, tileChunkLocalZ, (MapTileChunk)null);
                        tileChunk = null;
                     }
                  }
               }

               if (tileChunk != null && this.insideX == 3 && this.insideZ == 3 && tileChunk.wasChanged()) {
                  tileChunk.updateBuffers(this.mapProcessor, blockTintProvider, overlayManager, detailedDebug, this.blockStateShortShapeCache, updateConfig);
                  if (bottomChunk == null && tileChunkLocalZ < 7) {
                     bottomChunk = region.getChunk(tileChunkLocalX, tileChunkLocalZ + 1);
                  }

                  if (this.rightChunk == null && tileChunkLocalX < 7) {
                     this.rightChunk = region.getChunk(tileChunkLocalX + 1, tileChunkLocalZ);
                  }

                  if (this.bottomRightChunk == null && tileChunkLocalX < 7 && tileChunkLocalZ < 7) {
                     this.bottomRightChunk = region.getChunk(tileChunkLocalX + 1, tileChunkLocalZ + 1);
                  }

                  if (bottomChunk != null && bottomChunk.wasChanged()) {
                     bottomChunk.updateBuffers(this.mapProcessor, blockTintProvider, overlayManager, detailedDebug, this.blockStateShortShapeCache, updateConfig);
                     bottomChunk.setChanged(false);
                  }

                  if (this.rightChunk != null && this.rightChunk.wasChanged()) {
                     this.rightChunk.setToUpdateBuffers(true);
                     this.rightChunk.setChanged(false);
                  }

                  if (this.bottomRightChunk != null && this.bottomRightChunk.wasChanged()) {
                     this.bottomRightChunk.setToUpdateBuffers(true);
                     this.bottomRightChunk.setChanged(false);
                  }

                  tileChunk.setChanged(false);
               }
            }
         } else {
            this.insideX = 3;
            this.insideZ = 3;
         }
      }

      ++this.insideZ;
      if (this.insideZ > 3) {
         this.insideZ = 0;
         ++this.insideX;
         if (this.insideX > 3) {
            this.insideX = 0;
            ++this.Z;
            if (this.Z > this.endTileChunkZ - this.startTileChunkZ) {
               this.Z = 0;
               ++this.X;
               if (this.X > this.endTileChunkX - this.startTileChunkX) {
                  this.X = 0;
                  ++this.updateCounter;
               }
            }
         }
      }

      return wasSkipped;
   }

   public void updateBottomRightTile(MapRegion region, MapTileChunk tileChunk, MapTileChunk bottomChunk, int tileChunkLocalX, int tileChunkLocalZ) {
      MapTile bottomRightTile = this.insideX < 3 && this.insideZ < 3 ? tileChunk.getTile(this.insideX + 1, this.insideZ + 1) : null;
      if (bottomRightTile == null) {
         if (this.insideX == 3 && tileChunkLocalX < 7) {
            if (this.insideZ == 3) {
               if (tileChunkLocalZ < 7) {
                  this.bottomRightChunk = region.getChunk(tileChunkLocalX + 1, tileChunkLocalZ + 1);
               }

               bottomRightTile = this.bottomRightChunk != null ? this.bottomRightChunk.getTile(0, 0) : null;
               if (bottomRightTile != null) {
                  this.bottomRightChunk.setChanged(true);
               }
            } else {
               if (this.rightChunk == null) {
                  this.rightChunk = region.getChunk(tileChunkLocalX + 1, tileChunkLocalZ);
               }

               bottomRightTile = this.rightChunk != null ? this.rightChunk.getTile(0, this.insideZ + 1) : null;
               if (bottomRightTile != null) {
                  this.rightChunk.setChanged(true);
               }
            }
         } else if (this.insideX != 3 && this.insideZ == 3 && tileChunkLocalZ < 7) {
            bottomRightTile = bottomChunk != null ? bottomChunk.getTile(this.insideX + 1, 0) : null;
            if (bottomRightTile != null) {
               bottomChunk.setChanged(true);
            }
         }
      }

      if (bottomRightTile != null && bottomRightTile.isLoaded()) {
         bottomRightTile.getBlock(0, 0).setSlopeUnknown(true);
      }

   }

   public int getSectionBasedHeight(class_2818 bchunk, int startY) {
      class_2826[] sections = bchunk.method_12006();
      if (sections.length == 0) {
         return 0;
      } else {
         int chunkBottomY = bchunk.method_31607();
         int playerSection = Math.min(startY - chunkBottomY >> 4, sections.length - 1);
         if (playerSection < 0) {
            playerSection = 0;
         }

         int result = 0;

         int i;
         class_2826 searchedSection;
         for(i = playerSection; i < sections.length; ++i) {
            searchedSection = sections[i];
            if (!searchedSection.method_38292()) {
               result = chunkBottomY + (i << 4) + 15;
            }
         }

         if (playerSection > 0 && result == 0) {
            for(i = playerSection - 1; i >= 0; --i) {
               searchedSection = sections[i];
               if (!searchedSection.method_38292()) {
                  result = chunkBottomY + (i << 4) + 15;
                  break;
               }
            }
         }

         return result;
      }
   }

   public boolean isGlowing(class_2680 state) {
      return (double)state.method_26213() >= 0.5D;
   }

   private boolean shouldOverlayCached(class_2688<?, ?> state) {
      return (Boolean)this.transparentCache.apply(state);
   }

   public boolean shouldOverlay(class_2688<?, ?> state) {
      if (state instanceof class_2680) {
         class_2680 blockState = (class_2680)state;
         return !(blockState.method_26204() instanceof class_2189) && !(blockState.method_26204() instanceof class_8923) ? this.blockStateHasTranslucentRenderType(blockState) : true;
      } else {
         class_3610 fluidState = (class_3610)state;
         return class_4696.method_23680(fluidState) == class_1921.method_23583();
      }
   }

   public boolean isInvisible(class_2680 state, class_2248 b, boolean flowers) {
      if (!(b instanceof class_2404) && state.method_26217() == class_2464.field_11455) {
         return true;
      } else if (b == class_2246.field_10336) {
         return true;
      } else if (b == class_2246.field_10479) {
         return true;
      } else if (b != class_2246.field_10033 && b != class_2246.field_10285) {
         boolean isFlower = b instanceof class_8237 || b instanceof class_2521 || b instanceof class_2356 || state.method_26164(class_3481.field_20339);
         if (b instanceof class_2320 && !isFlower) {
            return true;
         } else if (isFlower && !flowers) {
            return true;
         } else {
            synchronized(this.buggedStates) {
               return this.buggedStates.contains(state);
            }
         }
      } else {
         return true;
      }
   }

   public boolean hasVanillaColor(class_2680 state, class_1937 world, class_2378<class_2248> blockRegistry, class_2338 pos) {
      class_3620 materialColor = null;

      try {
         materialColor = state.method_26205(world, pos);
      } catch (Throwable var10) {
         synchronized(this.buggedStates) {
            this.buggedStates.add(state);
         }

         Logger var10000 = WorldMap.LOGGER;
         class_2960 var10001 = blockRegistry.method_10221(state.method_26204());
         var10000.info("Broken vanilla map color definition found: " + String.valueOf(var10001));
      }

      return materialColor != null && materialColor.field_16011 != 0;
   }

   private class_2680 unpackFramedBlocks(class_2680 original, class_1937 world, class_2338 globalPos) {
      if (original.method_26204() instanceof class_2189) {
         return original;
      } else {
         class_2680 result = original;
         if (SupportMods.framedBlocks() && SupportMods.supportFramedBlocks.isFrameBlock(world, (class_2378)null, original)) {
            class_2586 tileEntity = world.method_8321(globalPos);
            if (tileEntity != null) {
               result = SupportMods.supportFramedBlocks.unpackFramedBlock(world, (class_2378)null, original, tileEntity);
               if (result == null || result.method_26204() instanceof class_2189) {
                  result = original;
               }
            }
         }

         return result;
      }
   }

   public void loadPixel(class_1937 world, class_2378<class_2248> blockRegistry, MapBlock pixel, MapBlock currentPixel, class_2818 bchunk, int insideX, int insideZ, int highY, int lowY, boolean cave, boolean fullCave, int mappedHeight, boolean canReuseBiomeColours, boolean ignoreHeightmaps, class_2378<class_1959> biomeRegistry, boolean flowers, int worldBottomY, class_2339 mutableBlockPos3) {
      pixel.prepareForWriting(worldBottomY);
      this.overlayBuilder.startBuilding();
      boolean underair = !cave || fullCave;
      boolean shouldEnterGround = fullCave;
      class_2680 opaqueState = null;
      byte workingLight = -1;
      boolean worldHasSkyLight = world.method_8597().comp_642();
      byte workingSkyLight = worldHasSkyLight ? 15 : 0;
      this.topH = lowY;
      this.mutableGlobalPos.method_10103((bchunk.method_12004().field_9181 << 4) + insideX, lowY - 1, (bchunk.method_12004().field_9180 << 4) + insideZ);
      boolean shouldExtendTillTheBottom = false;
      int transparentSkipY = 0;

      int h;
      class_2680 state;
      for(h = highY; h >= lowY; h = shouldExtendTillTheBottom ? transparentSkipY : h - 1) {
         this.mutableLocalPos.method_10103(insideX, h, insideZ);
         state = bchunk.method_8320(this.mutableLocalPos);
         if (state == null) {
            state = class_2246.field_10124.method_9564();
         }

         this.mutableGlobalPos.method_33098(h);
         state = this.unpackFramedBlocks(state, world, this.mutableGlobalPos);
         class_3610 fluidFluidState = state.method_26227();
         shouldExtendTillTheBottom = !shouldExtendTillTheBottom && !this.overlayBuilder.isEmpty() && this.firstTransparentStateY - h >= 5;
         class_2680 traceState;
         if (shouldExtendTillTheBottom) {
            for(transparentSkipY = h - 1; transparentSkipY >= lowY; --transparentSkipY) {
               traceState = bchunk.method_8320(mutableBlockPos3.method_10103(insideX, transparentSkipY, insideZ));
               if (traceState == null) {
                  traceState = class_2246.field_10124.method_9564();
               }

               class_3610 traceFluidState = traceState.method_26227();
               if (!traceFluidState.method_15769()) {
                  if (!this.shouldOverlayCached(traceFluidState)) {
                     break;
                  }

                  if (!(traceState.method_26204() instanceof class_2189) && traceState.method_26204() == ((class_2680)this.fluidToBlock.apply(traceFluidState)).method_26204()) {
                     continue;
                  }
               }

               if (!this.shouldOverlayCached(traceState)) {
                  break;
               }
            }
         }

         this.mutableGlobalPos.method_33098(h + 1);
         workingLight = (byte)world.method_8314(class_1944.field_9282, this.mutableGlobalPos);
         if (cave && workingLight < 15 && worldHasSkyLight) {
            if (!ignoreHeightmaps && !fullCave && highY >= mappedHeight) {
               workingSkyLight = 15;
            } else {
               workingSkyLight = (byte)world.method_8314(class_1944.field_9284, this.mutableGlobalPos);
            }
         }

         this.mutableGlobalPos.method_33098(h);
         if (!fluidFluidState.method_15769() && (!cave || !shouldEnterGround)) {
            underair = true;
            traceState = (class_2680)this.fluidToBlock.apply(fluidFluidState);
            if (this.loadPixelHelp(pixel, currentPixel, world, blockRegistry, traceState, workingLight, (byte)workingSkyLight, bchunk, insideX, insideZ, h, canReuseBiomeColours, cave, fluidFluidState, biomeRegistry, transparentSkipY, shouldExtendTillTheBottom, flowers, underair)) {
               opaqueState = state;
               break;
            }
         }

         class_2248 b = state.method_26204();
         if (b instanceof class_2189) {
            underair = true;
         } else if (underair && state.method_26204() != ((class_2680)this.fluidToBlock.apply(fluidFluidState)).method_26204()) {
            if (cave && shouldEnterGround) {
               if (!state.method_50011() && !state.method_45474() && state.method_26223() != class_3619.field_15971 && !this.shouldOverlayCached(state)) {
                  underair = false;
                  shouldEnterGround = false;
               }
            } else if (this.loadPixelHelp(pixel, currentPixel, world, blockRegistry, state, workingLight, (byte)workingSkyLight, bchunk, insideX, insideZ, h, canReuseBiomeColours, cave, (class_3610)null, biomeRegistry, transparentSkipY, shouldExtendTillTheBottom, flowers, underair)) {
               opaqueState = state;
               break;
            }
         }
      }

      if (h < lowY) {
         h = lowY;
      }

      state = null;
      class_2680 state = opaqueState == null ? class_2246.field_10124.method_9564() : opaqueState;
      this.overlayBuilder.finishBuilding(pixel);
      byte light = 0;
      if (opaqueState != null) {
         light = workingLight;
         if (cave && workingLight < 15 && pixel.getNumberOfOverlays() == 0 && workingSkyLight > workingLight) {
            light = workingSkyLight;
         }
      } else {
         h = worldBottomY;
      }

      class_5321 blockBiome;
      if (canReuseBiomeColours && currentPixel != null && currentPixel.getState() == state && currentPixel.getTopHeight() == this.topH) {
         blockBiome = currentPixel.getBiome();
      } else {
         this.mutableGlobalPos.method_33098(this.topH);
         blockBiome = this.biomeGetter.getBiome(world, this.mutableGlobalPos, biomeRegistry);
         this.mutableGlobalPos.method_33098(h);
      }

      if (this.overlayBuilder.getOverlayBiome() != null) {
         blockBiome = this.overlayBuilder.getOverlayBiome();
      }

      boolean glowing = this.isGlowing(state);
      pixel.write(state, h, this.topH, blockBiome, (byte)light, glowing, cave);
   }

   private boolean loadPixelHelp(MapBlock pixel, MapBlock currentPixel, class_1937 world, class_2378<class_2248> blockRegistry, class_2680 state, byte light, byte skyLight, class_2818 bchunk, int insideX, int insideZ, int h, boolean canReuseBiomeColours, boolean cave, class_3610 fluidFluidState, class_2378<class_1959> biomeRegistry, int transparentSkipY, boolean shouldExtendTillTheBottom, boolean flowers, boolean underair) {
      class_2248 b = state.method_26204();
      if (this.isInvisible(state, b, flowers)) {
         return false;
      } else if (this.shouldOverlayCached((class_2688)(fluidFluidState == null ? state : fluidFluidState))) {
         if (cave && !underair) {
            return false;
         } else {
            if (h > this.topH) {
               this.topH = h;
            }

            byte overlayLight = light;
            if (this.overlayBuilder.isEmpty()) {
               this.firstTransparentStateY = h;
               if (cave && skyLight > light) {
                  overlayLight = skyLight;
               }
            }

            if (shouldExtendTillTheBottom) {
               this.overlayBuilder.getCurrentOverlay().increaseOpacity(this.overlayBuilder.getCurrentOverlay().getState().method_26193(world, this.mutableGlobalPos) * (h - transparentSkipY));
            } else {
               class_5321<class_1959> overlayBiome = this.overlayBuilder.getOverlayBiome();
               if (overlayBiome == null) {
                  if (canReuseBiomeColours && currentPixel != null && currentPixel.getNumberOfOverlays() > 0 && ((Overlay)currentPixel.getOverlays().get(0)).getState() == state) {
                     overlayBiome = currentPixel.getBiome();
                  } else {
                     overlayBiome = this.biomeGetter.getBiome(world, this.mutableGlobalPos, biomeRegistry);
                  }
               }

               this.overlayBuilder.build(state, state.method_26193(world, this.mutableGlobalPos), overlayLight, this.mapProcessor, overlayBiome);
            }

            return false;
         }
      } else if (!this.hasVanillaColor(state, world, blockRegistry, this.mutableGlobalPos)) {
         return false;
      } else if (cave && !underair) {
         return true;
      } else {
         if (h > this.topH) {
            this.topH = h;
         }

         return true;
      }
   }

   protected abstract List<class_777> getQuads(class_1087 var1, class_2680 var2, class_2350 var3);

   protected abstract class_1058 getParticleIcon(class_773 var1, class_1087 var2, class_2680 var3);

   public int loadBlockColourFromTexture(class_2680 state, boolean convert, class_1937 world, class_2378<class_2248> blockRegistry, class_2338 globalPos) {
      if (this.clearCachedColours) {
         this.textureColours.clear();
         this.blockColours.clear();
         this.blockTintIndices.clear();
         this.lastBlockStateForTextureColor = null;
         this.lastBlockStateForTextureColorResult = -1;
         this.clearCachedColours = false;
         if (WorldMapClientConfigUtils.getDebug()) {
            WorldMap.LOGGER.info("Xaero's World Map cache cleared!");
         }
      }

      if (state == this.lastBlockStateForTextureColor) {
         return this.lastBlockStateForTextureColorResult;
      } else {
         Integer c = (Integer)this.blockColours.get(state);
         int red = false;
         int green = false;
         int blue = false;
         int alpha = 0;
         class_2248 b = state.method_26204();
         if (c == null) {
            String name = null;
            int tintIndex = -1;

            Logger var10000;
            class_2960 var10001;
            try {
               List<class_777> upQuads = null;
               class_773 bms = class_310.method_1551().method_1541().method_3351();
               class_1087 model = bms.method_3335(state);
               if (convert) {
                  upQuads = this.getQuads(model, state, class_2350.field_11036);
               }

               class_1058 missingTexture = class_310.method_1551().method_1554().method_24153(class_1059.field_5275).method_4608(class_1047.method_4539());
               class_1058 texture;
               if (upQuads != null && !upQuads.isEmpty() && ((class_777)upQuads.get(0)).method_35788() != missingTexture) {
                  texture = ((class_777)upQuads.get(0)).method_35788();
                  tintIndex = ((class_777)upQuads.get(0)).method_3359();
               } else {
                  texture = this.getParticleIcon(bms, model, state);
                  tintIndex = 0;
               }

               if (texture == null) {
                  throw new SilentException("No texture for " + String.valueOf(state));
               }

               c = -1;
               name = String.valueOf(texture.method_45851().method_45816()) + ".png";
               String[] args = name.split(":");
               if (args.length < 2) {
                  DEFAULT_RESOURCE[1] = args[0];
                  args = DEFAULT_RESOURCE;
               }

               Integer cachedColour = (Integer)this.textureColours.get(name);
               if (cachedColour != null) {
                  c = cachedColour;
               } else {
                  class_2960 location = class_2960.method_60655(args[0], "textures/" + args[1]);
                  class_3298 resource = (class_3298)class_310.method_1551().method_1478().method_14486(location).orElse((Object)null);
                  if (resource == null) {
                     throw new SilentException("No texture " + String.valueOf(location));
                  }

                  InputStream input = resource.method_14482();
                  ImageInputStream imageInputStream = ImageIO.createImageInputStream(input);
                  BufferedImage img = ImageIOUtils.getImageThroughZipError(imageInputStream, location.toString());
                  imageInputStream.close();
                  if (img == null) {
                     throw new SilentException("No image loaded " + String.valueOf(location));
                  }

                  int red = 0;
                  int green = 0;
                  int blue = 0;
                  int total = 0;
                  int ts = Math.min(img.getWidth(), img.getHeight());
                  if (ts > 0) {
                     int diff = Math.max(1, Math.min(4, ts / 8));
                     int parts = ts / diff;
                     Raster raster = img.getData();
                     int[] colorHolder = null;

                     for(int i = 0; i < parts; ++i) {
                        for(int j = 0; j < parts; ++j) {
                           int rgb;
                           int a;
                           if (img.getColorModel().getNumComponents() < 3) {
                              colorHolder = raster.getPixel(i * diff, j * diff, colorHolder);
                              a = colorHolder[0] & 255;
                              int a = 255;
                              if (colorHolder.length > 1) {
                                 a = colorHolder[1];
                              }

                              rgb = a << 24 | a << 16 | a << 8 | a;
                           } else {
                              rgb = img.getRGB(i * diff, j * diff);
                           }

                           a = rgb >> 24 & 255;
                           if (rgb != 0 && a != 0) {
                              red += rgb >> 16 & 255;
                              green += rgb >> 8 & 255;
                              blue += rgb & 255;
                              alpha += a;
                              ++total;
                           }
                        }
                     }
                  }

                  if (total == 0) {
                     total = 1;
                  }

                  red /= total;
                  green /= total;
                  blue /= total;
                  alpha /= total;
                  if (convert && red == 0 && green == 0 && blue == 0) {
                     throw new SilentException("Black texture " + ts);
                  }

                  c = alpha << 24 | red << 16 | green << 8 | blue;
                  this.textureColours.put(name, c);
               }
            } catch (FileNotFoundException var37) {
               if (convert) {
                  return this.loadBlockColourFromTexture(state, false, world, blockRegistry, globalPos);
               }

               var10000 = WorldMap.LOGGER;
               var10001 = blockRegistry.method_10221(b);
               var10000.info("Block file not found: " + String.valueOf(var10001));
               c = 0;
               if (state != null && state.method_26205(world, globalPos) != null) {
                  c = state.method_26205(world, globalPos).field_16011;
               }

               if (name != null) {
                  this.textureColours.put(name, c);
               }
            } catch (Exception var38) {
               var10000 = WorldMap.LOGGER;
               var10001 = blockRegistry.method_10221(b);
               var10000.info("Exception when loading " + String.valueOf(var10001) + " texture, using material colour.");
               c = 0;
               if (state.method_26205(world, globalPos) != null) {
                  c = state.method_26205(world, globalPos).field_16011;
               }

               if (name != null) {
                  this.textureColours.put(name, c);
               }

               if (var38 instanceof SilentException) {
                  WorldMap.LOGGER.info(var38.getMessage());
               } else {
                  WorldMap.LOGGER.error("suppressed exception", var38);
               }
            }

            if (c != null) {
               this.blockColours.put(state, c);
               this.blockTintIndices.put(state, tintIndex);
            }
         }

         this.lastBlockStateForTextureColor = state;
         this.lastBlockStateForTextureColorResult = c;
         return c;
      }
   }

   public long getUpdateCounter() {
      return this.updateCounter;
   }

   public void resetPosition() {
      this.X = 0;
      this.Z = 0;
      this.insideX = 0;
      this.insideZ = 0;
   }

   public void requestCachedColoursClear() {
      this.clearCachedColours = true;
   }

   public void setMapProcessor(MapProcessor mapProcessor) {
      this.mapProcessor = mapProcessor;
   }

   public MapProcessor getMapProcessor() {
      return this.mapProcessor;
   }

   public void setDirtyInWriteDistance(class_1657 player, class_1937 level) {
      int writeDistance = this.getWriteDistance();
      int playerChunkX = player.method_24515().method_10263() >> 4;
      int playerChunkZ = player.method_24515().method_10260() >> 4;
      int startChunkX = playerChunkX - writeDistance;
      int startChunkZ = playerChunkZ - writeDistance;
      int endChunkX = playerChunkX + writeDistance;
      int endChunkZ = playerChunkZ + writeDistance;

      for(int x = startChunkX; x < endChunkX; ++x) {
         for(int z = startChunkZ; z < endChunkZ; ++z) {
            class_2818 chunk = level.method_8497(x, z);
            if (chunk != null) {
               try {
                  XaeroWorldMapCore.chunkCleanField.set(chunk, false);
               } catch (IllegalAccessException | IllegalArgumentException var14) {
                  throw new RuntimeException(var14);
               }
            }
         }
      }

   }

   public int getBlockTintIndex(class_2680 state) {
      return this.blockTintIndices.getInt(state);
   }
}
