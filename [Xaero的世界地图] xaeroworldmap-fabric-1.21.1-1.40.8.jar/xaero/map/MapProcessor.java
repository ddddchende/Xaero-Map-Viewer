package xaero.map;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import net.minecraft.class_1074;
import net.minecraft.class_1297;
import net.minecraft.class_151;
import net.minecraft.class_1792;
import net.minecraft.class_1802;
import net.minecraft.class_1937;
import net.minecraft.class_1959;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2378;
import net.minecraft.class_2874;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_3611;
import net.minecraft.class_5218;
import net.minecraft.class_5321;
import net.minecraft.class_634;
import net.minecraft.class_638;
import net.minecraft.class_642;
import net.minecraft.class_7134;
import net.minecraft.class_7225;
import net.minecraft.class_7923;
import net.minecraft.class_7924;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.client.gui.IScreenBase;
import xaero.lib.common.config.Config;
import xaero.lib.common.config.single.SingleConfigManager;
import xaero.lib.common.reflection.util.ReflectionUtils;
import xaero.lib.common.util.IOUtils;
import xaero.map.biome.BiomeColorCalculator;
import xaero.map.biome.BiomeGetter;
import xaero.map.biome.BlockTintProvider;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.cache.BrokenBlockTintCache;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.config.primary.option.WorldMapPrimaryClientConfigOptions;
import xaero.map.config.util.WorldMapClientConfigUtils;
import xaero.map.controls.ControlsRegister;
import xaero.map.deallocator.ByteBufferDeallocator;
import xaero.map.exception.OpenGLException;
import xaero.map.file.MapSaveLoad;
import xaero.map.file.RegionDetection;
import xaero.map.file.worldsave.WorldDataHandler;
import xaero.map.graphics.CustomVertexConsumers;
import xaero.map.graphics.MapRenderHelper;
import xaero.map.graphics.TextureUploader;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.map.gui.GuiMap;
import xaero.map.gui.message.MessageBox;
import xaero.map.gui.message.render.MessageBoxRenderer;
import xaero.map.highlight.DimensionHighlighterHandler;
import xaero.map.highlight.HighlighterRegistry;
import xaero.map.highlight.MapRegionHighlightsPreparer;
import xaero.map.mcworld.WorldMapClientWorldData;
import xaero.map.mcworld.WorldMapClientWorldDataHelper;
import xaero.map.minimap.MinimapRenderListener;
import xaero.map.misc.CaveStartCalculator;
import xaero.map.mods.SupportMods;
import xaero.map.pool.MapTilePool;
import xaero.map.radar.tracker.synced.ClientSyncedTrackedPlayerManager;
import xaero.map.region.LayeredRegionManager;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapBlock;
import xaero.map.region.MapLayer;
import xaero.map.region.MapRegion;
import xaero.map.region.MapTile;
import xaero.map.region.MapTileChunk;
import xaero.map.region.MapUpdateFastConfig;
import xaero.map.region.OverlayManager;
import xaero.map.region.texture.BranchTextureRenderer;
import xaero.map.region.texture.RegionTexture;
import xaero.map.task.MapRunnerTask;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;

public class MapProcessor {
   public static final int ROOT_FOLDER_FORMAT = 5;
   public static final int DEFAULT_LIGHT_LEVELS = 4;
   private MapSaveLoad mapSaveLoad;
   private MapWriter mapWriter;
   private MapLimiter mapLimiter;
   private WorldDataHandler worldDataHandler;
   private ByteBufferDeallocator bufferDeallocator;
   private TextureUploader textureUploader;
   private BranchTextureRenderer branchTextureRenderer;
   private BiomeColorCalculator biomeColorCalculator;
   private final BlockStateShortShapeCache blockStateShortShapeCache;
   private final BiomeGetter biomeGetter;
   private final BrokenBlockTintCache brokenBlockTintCache;
   private final MapRegionHighlightsPreparer mapRegionHighlightsPreparer;
   private final CaveStartCalculator caveStartCalculator;
   private final ClientSyncedTrackedPlayerManager clientSyncedTrackedPlayerManager;
   private class_638 world;
   private class_7225<class_2248> worldBlockLookup;
   private class_2378<class_2248> worldBlockRegistry;
   private class_2378<class_3611> worldFluidRegistry;
   public class_2378<class_1959> worldBiomeRegistry;
   private class_2378<class_2874> worldDimensionTypeRegistry;
   private BlockTintProvider worldBlockTintProvider;
   private class_638 newWorld;
   private class_7225<class_2248> newWorldBlockLookup;
   public class_2378<class_2248> newWorldBlockRegistry;
   private class_2378<class_3611> newWorldFluidRegistry;
   public class_2378<class_1959> newWorldBiomeRegistry;
   public class_2378<class_2874> newWorldDimensionTypeRegistry;
   public final Object mainStuffSync;
   public class_638 mainWorld;
   private class_7225<class_2248> mainWorldBlockLookup;
   public class_2378<class_2248> mainWorldBlockRegistry;
   private class_2378<class_3611> mainWorldFluidRegistry;
   public class_2378<class_1959> mainWorldBiomeRegistry;
   public class_2378<class_2874> mainWorldDimensionTypeRegistry;
   public double mainPlayerX;
   public double mainPlayerY;
   public double mainPlayerZ;
   private boolean mainWorldUnloaded;
   private ArrayList<Double[]> footprints = new ArrayList();
   private int footprintsTimer;
   private boolean mapWorldUsable;
   private MapWorld mapWorld;
   private String currentWorldId;
   private String currentDimId;
   private String currentMWId;
   private FileLock mapLockToRelease;
   private FileChannel mapLockChannelToClose;
   private FileChannel currentMapLockChannel;
   private FileLock currentMapLock;
   private boolean mapWorldUsableRequest;
   public final Object renderThreadPauseSync = new Object();
   private int pauseUploading;
   private int pauseRendering;
   private int pauseWriting;
   public final Object processorThreadPauseSync = new Object();
   private int pauseProcessing;
   private final Object loadingSync = new Object();
   private boolean isLoading;
   public final Object uiSync = new Object();
   private boolean waitingForWorldUpdate;
   public final Object uiPauseSync = new Object();
   private boolean isUIPaused;
   private ArrayList<LeveledRegion<?>>[] toProcessLevels;
   private ArrayList<MapRegion> toRefresh = new ArrayList();
   private static final int SPAWNPOINT_TIMEOUT = 3000;
   private class_2338 spawnToRestore;
   private long mainWorldChangedTime = -1L;
   private MapTilePool tilePool;
   private int firstBranchLevel;
   private long lastRenderProcessTime = -1L;
   private int workingFramesCount;
   public long freeFramePeriod = -1L;
   private int testingFreeFrame = 1;
   private MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers;
   private CustomVertexConsumers cvc;
   private final MessageBox messageBox;
   private final MessageBoxRenderer messageBoxRenderer;
   private MinimapRenderListener minimapRenderListener;
   private boolean currentMapNeedsDeletion;
   private OverlayManager overlayManager;
   private long renderStartTime;
   private Field scheduledTasksField;
   private Runnable renderStartTimeUpdaterRunnable;
   private boolean finalizing;
   private int state;
   private HashSet<class_2960> hardcodedNetherlike;
   private final HighlighterRegistry highlighterRegistry;
   private int currentCaveLayer = Integer.MAX_VALUE;
   private long lastLocalCaveModeToggle;
   private int nextLocalCaveMode = Integer.MAX_VALUE;
   private int localCaveMode = Integer.MAX_VALUE;
   private class_1792 mapItem;
   private boolean consideringNetherFairPlayMessage;
   private boolean fairplayMessageReceived;
   private String[] dimensionsToIgnore = new String[]{"FZHammer"};
   public Field selectedField = null;

   public MapProcessor(MapSaveLoad mapSaveLoad, MapWriter mapWriter, MapLimiter mapLimiter, ByteBufferDeallocator bufferDeallocator, MapTilePool tilePool, OverlayManager overlayManager, TextureUploader textureUploader, WorldDataHandler worldDataHandler, BranchTextureRenderer branchTextureRenderer, MultiTextureRenderTypeRendererProvider mtrtrs, CustomVertexConsumers cvc, BiomeColorCalculator biomeColorCalculator, BlockStateShortShapeCache blockStateShortShapeCache, BiomeGetter biomeGetter, BrokenBlockTintCache brokenBlockTintCache, HighlighterRegistry highlighterRegistry, MapRegionHighlightsPreparer mapRegionHighlightsPreparer, MessageBox messageBox, MessageBoxRenderer messageBoxRenderer, CaveStartCalculator caveStartCalculator, ClientSyncedTrackedPlayerManager clientSyncedTrackedPlayerManager) throws NoSuchFieldException {
      this.branchTextureRenderer = branchTextureRenderer;
      this.mapSaveLoad = mapSaveLoad;
      this.mapWriter = mapWriter;
      this.mapLimiter = mapLimiter;
      this.bufferDeallocator = bufferDeallocator;
      this.tilePool = tilePool;
      this.overlayManager = overlayManager;
      this.textureUploader = textureUploader;
      this.worldDataHandler = worldDataHandler;
      this.scheduledTasksField = ReflectionUtils.getFieldReflection(class_310.class, "progressTasks", "field_17404", "Ljava/util/Queue;", "f_91023_");
      this.renderStartTimeUpdaterRunnable = new Runnable() {
         public void run() {
            MapProcessor.this.updateRenderStartTime();
         }
      };
      this.mainStuffSync = new Object();
      this.toProcessLevels = new ArrayList[4];

      for(int i = 0; i < this.toProcessLevels.length; ++i) {
         this.toProcessLevels[i] = new ArrayList();
      }

      this.multiTextureRenderTypeRenderers = mtrtrs;
      this.cvc = cvc;
      this.biomeColorCalculator = biomeColorCalculator;
      this.blockStateShortShapeCache = blockStateShortShapeCache;
      this.hardcodedNetherlike = Sets.newHashSet(new class_2960[]{class_7134.field_37671, class_2960.method_60655("undergarden", "undergarden")});
      this.biomeGetter = biomeGetter;
      this.brokenBlockTintCache = brokenBlockTintCache;
      this.highlighterRegistry = highlighterRegistry;
      this.mapRegionHighlightsPreparer = mapRegionHighlightsPreparer;
      this.messageBox = messageBox;
      this.messageBoxRenderer = messageBoxRenderer;
      this.caveStartCalculator = caveStartCalculator;
      this.clientSyncedTrackedPlayerManager = clientSyncedTrackedPlayerManager;
      this.minimapRenderListener = new MinimapRenderListener();
      this.updateMapItem();
   }

   public void onInit(class_634 connection) {
      String mainId = this.getMainId(5, connection);
      this.fixRootFolder(mainId, connection);
      this.mapWorld = new MapWorld(mainId, this.getMainId(0, connection), this);
      this.mapWorld.load();
   }

   public void run(MapRunner runner) {
      if (this.state < 2) {
         while(true) {
            try {
               if (this.state < 2 && WorldMap.crashHandler.getCrashedBy() == null) {
                  synchronized(this.processorThreadPauseSync) {
                     if (!this.isProcessingPaused()) {
                        this.updateWorld();
                        if (this.world != null) {
                           this.updateFootprints(class_310.method_1551().field_1755 instanceof GuiMap ? 1 : 10);
                        }

                        if (this.mapWorldUsable) {
                           this.mapLimiter.applyLimit(this.mapWorld, this);
                           long currentTime = System.currentTimeMillis();

                           for(int l = 0; l < this.toProcessLevels.length; ++l) {
                              ArrayList<LeveledRegion<?>> regionsToProcess = this.toProcessLevels[l];

                              for(int i = 0; i < regionsToProcess.size(); ++i) {
                                 LeveledRegion leveledRegion;
                                 synchronized(regionsToProcess) {
                                    if (i >= regionsToProcess.size()) {
                                       break;
                                    }

                                    leveledRegion = (LeveledRegion)regionsToProcess.get(i);
                                 }

                                 this.mapSaveLoad.updateSave(leveledRegion, currentTime, this.currentCaveLayer);
                              }
                           }
                        }

                        this.mapSaveLoad.run(this.worldBlockLookup, this.worldBlockRegistry, this.worldFluidRegistry, this.biomeGetter, this.worldBiomeRegistry);
                        this.handleRefresh();
                        runner.doTasks(this);
                        this.releaseLocksIfNeeded();
                     }
                  }

                  try {
                     Thread.sleep(this.world != null && !shouldSkipWorldRender() && this.state <= 0 ? 100L : 40L);
                  } catch (InterruptedException var12) {
                  }
                  continue;
               }
            } catch (Throwable var15) {
               WorldMap.crashHandler.setCrashedBy(var15);
            }

            if (this.state < 2) {
               this.forceClean();
            }
            break;
         }
      }

      if (this.state == 2) {
         this.state = 3;
      }

   }

   public static boolean shouldSkipWorldRender() {
      if (!(class_310.method_1551().field_1755 instanceof IScreenBase)) {
         return false;
      } else {
         IScreenBase screenBase = (IScreenBase)class_310.method_1551().field_1755;
         return screenBase.shouldSkipWorldRender();
      }
   }

   public void onRenderProcess(class_310 mc) throws RuntimeException {
      try {
         this.mapWriter.onRender(this.biomeColorCalculator, this.overlayManager);
         long renderProcessTime = System.nanoTime();
         if (this.testingFreeFrame == 1) {
            this.testingFreeFrame = 2;
         } else {
            synchronized(this.renderThreadPauseSync) {
               if (this.lastRenderProcessTime == -1L) {
                  this.lastRenderProcessTime = renderProcessTime;
               }

               long sinceLastProcessTime = renderProcessTime - this.lastRenderProcessTime;
               if (this.testingFreeFrame == 2) {
                  this.freeFramePeriod = sinceLastProcessTime;
                  this.testingFreeFrame = 0;
               }

               if (this.pauseUploading == 0 && this.mapWorldUsable && this.currentWorldId != null) {
                  mc.method_22940().method_23000().method_22993();

                  while(true) {
                     if (GL11.glGetError() == 0) {
                        GlStateManager._clearColor(0.0F, 0.0F, 0.0F, 0.0F);
                        GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                        RenderSystem.pixelStore(3317, 4);
                        RenderSystem.pixelStore(3316, 0);
                        RenderSystem.pixelStore(3315, 0);
                        RenderSystem.pixelStore(3314, 0);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                        OpenGLException.checkGLError();
                        this.updateCaveStart();
                        MapDimension currentDim = this.mapWorld.getCurrentDimension();
                        if (currentDim.getFullReloader() != null) {
                           currentDim.getFullReloader().onRenderProcess();
                        }

                        DimensionHighlighterHandler highlighterHandler = currentDim.getHighlightHandler();
                        ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
                        SingleConfigManager<Config> primaryConfigManager = configManager.getPrimaryConfigManager();
                        int globalRegionCacheHashCode = WorldMap.settings.getRegionCacheHashCode();
                        boolean debugConfig = (Boolean)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.DEBUG);
                        boolean detailedDebug = WorldMap.detailed_debug;
                        MapUpdateFastConfig updateConfig = new MapUpdateFastConfig();
                        long uploadStart = System.nanoTime();
                        long totalTime = Math.min(sinceLastProcessTime, this.freeFramePeriod);
                        long passed = uploadStart - this.renderStartTime;
                        long timeAvailable = Math.max(3000000L, totalTime - passed);
                        long uploadUntil = uploadStart + timeAvailable / 4L;
                        long gpuLimit = Math.max(1000000L, class_310.method_1551().field_1755 instanceof GuiMap ? totalTime * 5L / 12L : Math.min(totalTime / 5L, timeAvailable));
                        boolean noLimits = false;
                        if (class_310.method_1551().field_1755 instanceof GuiMap) {
                           GuiMap guiMap = (GuiMap)class_310.method_1551().field_1755;
                           noLimits = guiMap.noUploadingLimits;
                           guiMap.noUploadingLimits = false;
                        }

                        int firstLevel = 0;
                        boolean branchesCatchup = (int)(Math.random() * 5.0D) == 0;
                        if (branchesCatchup) {
                           firstLevel = 1 + this.firstBranchLevel;
                        }

                        this.firstBranchLevel = (this.firstBranchLevel + 1) % (this.toProcessLevels.length - 1);

                        for(int j = 0; j < this.toProcessLevels.length; ++j) {
                           int level = (firstLevel + j) % this.toProcessLevels.length;
                           ArrayList<LeveledRegion<?>> toProcess = this.toProcessLevels[level];

                           for(int i = 0; i < toProcess.size(); ++i) {
                              LeveledRegion region;
                              synchronized(toProcess) {
                                 if (i >= toProcess.size()) {
                                    break;
                                 }

                                 region = (LeveledRegion)toProcess.get(i);
                              }

                              if (region != null) {
                                 synchronized(region) {
                                    if (region.shouldBeProcessed()) {
                                       boolean cleanAndCacheRequestsBlocked = region.cleanAndCacheRequestsBlocked();
                                       boolean allCleaned = true;
                                       boolean allCached = true;
                                       boolean allUploaded = true;
                                       boolean hasLoadedTextures = false;

                                       for(int x = 0; x < 8; ++x) {
                                          for(int z = 0; z < 8; ++z) {
                                             RegionTexture texture = region.getTexture(x, z);
                                             if (texture != null) {
                                                if (texture.canUpload()) {
                                                   hasLoadedTextures = true;
                                                   if (noLimits || gpuLimit > 0L && System.nanoTime() < uploadUntil) {
                                                      texture.preUpload(this, this.worldBlockTintProvider, this.overlayManager, region, detailedDebug, this.blockStateShortShapeCache, updateConfig);
                                                      if (texture.shouldUpload()) {
                                                         if (texture.getTimer() == 0) {
                                                            gpuLimit -= texture.uploadBuffer(highlighterHandler, this.textureUploader, region, this.branchTextureRenderer, x, z);
                                                         } else {
                                                            texture.decTimer();
                                                         }
                                                      }
                                                   }

                                                   texture.postUpload(this, region, cleanAndCacheRequestsBlocked);
                                                }

                                                if (texture.hasSourceData()) {
                                                   allCleaned = false;
                                                }

                                                if (texture.shouldIncludeInCache() && !texture.isCachePrepared()) {
                                                   allCached = false;
                                                }

                                                if (!texture.isUploaded()) {
                                                   allUploaded = false;
                                                }
                                             }
                                          }
                                       }

                                       if (hasLoadedTextures) {
                                          region.processWhenLoadedChunksExist(globalRegionCacheHashCode);
                                       }

                                       allUploaded = allUploaded && region.isLoaded() && !cleanAndCacheRequestsBlocked;
                                       allCached = allCached && allUploaded;
                                       if ((!region.shouldCache() || !region.recacheHasBeenRequested()) && region.shouldEndProcessingAfterUpload() && allCleaned && allUploaded) {
                                          region.onProcessingEnd();
                                          region.deleteGLBuffers();
                                          synchronized(toProcess) {
                                             if (i < toProcess.size()) {
                                                toProcess.remove(i);
                                                --i;
                                             }
                                          }

                                          if (debugConfig) {
                                             Logger var10000 = WorldMap.LOGGER;
                                             String var10001 = String.valueOf(region);
                                             var10000.info("Region freed: " + var10001 + " " + this.mapWriter.getUpdateCounter() + " " + this.currentWorldId + " " + this.currentDimId);
                                          }
                                       }

                                       if (allCached && !region.isAllCachePrepared()) {
                                          region.setAllCachePrepared(true);
                                       }

                                       if (region.shouldCache() && region.recacheHasBeenRequested() && region.isAllCachePrepared() && !cleanAndCacheRequestsBlocked) {
                                          this.getMapSaveLoad().requestCache(region);
                                       }
                                    }
                                 }
                              }
                           }
                        }

                        ++this.workingFramesCount;
                        if (this.workingFramesCount >= 30) {
                           this.testingFreeFrame = 1;
                           this.workingFramesCount = 0;
                        }

                        this.textureUploader.uploadTextures();
                        break;
                     }
                  }
               }
            }
         }

         this.mapLimiter.updateAvailableVRAM();
         this.lastRenderProcessTime = renderProcessTime;
      } catch (Throwable var51) {
         WorldMap.crashHandler.setCrashedBy(var51);
      }

      WorldMap.crashHandler.checkForCrashes();
      MapRenderHelper.restoreDefaultShaderBlendState();
   }

   public void updateCaveStart() {
      class_310 mc = class_310.method_1551();
      MapDimension dimension = this.mapWorld.getCurrentDimension();
      boolean caveModeAllowed = WorldMapClientConfigUtils.getEffectiveCaveModeAllowed();
      int newCaveStart;
      if (caveModeAllowed && dimension.getCaveModeType() != 0) {
         ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
         SingleConfigManager<Config> primaryConfigManager = configManager.getPrimaryConfigManager();
         int caveModeStartConfig = (Integer)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.CAVE_MODE_START);
         if (caveModeStartConfig == Integer.MAX_VALUE) {
            newCaveStart = Integer.MIN_VALUE;
         } else {
            newCaveStart = caveModeStartConfig;
         }

         boolean customDim = dimension.getDimId() != mc.field_1687.method_27983();
         boolean isMapScreen = mc.field_1755 instanceof GuiMap || shouldSkipWorldRender();
         int autoCaveModeConfig = (Integer)configManager.getEffective(WorldMapProfiledConfigOptions.AUTO_CAVE_MODE);
         double caveModeToggleTimerConfig = (Double)configManager.getEffective(WorldMapProfiledConfigOptions.CAVE_MODE_TOGGLE_TIMER);
         if (SupportMods.minimap() && (!customDim && autoCaveModeConfig < 0 && newCaveStart == Integer.MIN_VALUE || !isMapScreen)) {
            newCaveStart = SupportMods.xaeroMinimap.getCaveStart(newCaveStart, isMapScreen);
         }

         if (newCaveStart == Integer.MIN_VALUE) {
            long currentTime = System.currentTimeMillis();
            int nextLocalCaveMode = customDim ? Integer.MAX_VALUE : this.caveStartCalculator.getCaving(mc.field_1724.method_23317(), mc.field_1724.method_23318(), mc.field_1724.method_23321(), mc.field_1687);
            boolean toggling = this.localCaveMode == Integer.MAX_VALUE != (nextLocalCaveMode == Integer.MAX_VALUE);
            if (!toggling || currentTime - this.lastLocalCaveModeToggle > (long)(caveModeToggleTimerConfig * 1000.0D)) {
               if (toggling) {
                  this.lastLocalCaveModeToggle = currentTime;
               }

               this.localCaveMode = nextLocalCaveMode;
            }

            newCaveStart = this.localCaveMode;
         }

         if (newCaveStart != Integer.MAX_VALUE) {
            if (dimension.getCaveModeType() == 2) {
               newCaveStart = Integer.MIN_VALUE;
            } else {
               newCaveStart = class_3532.method_15340(newCaveStart, this.world.method_31607(), this.world.method_31600() - 1);
            }
         }
      } else {
         newCaveStart = Integer.MAX_VALUE;
      }

      int newCaveLayer = this.getCaveLayer(newCaveStart);
      dimension.getLayeredMapRegions().getLayer(newCaveLayer).setCaveStart(newCaveStart);
      this.currentCaveLayer = newCaveLayer;
   }

   public boolean ignoreWorld(class_1937 world) {
      for(int i = 0; i < this.dimensionsToIgnore.length; ++i) {
         if (this.dimensionsToIgnore[i].equals(world.method_27983().method_29177().method_12832())) {
            return true;
         }
      }

      return false;
   }

   public String getDimensionName(class_5321<class_1937> id) {
      if (id == class_1937.field_25179) {
         return "null";
      } else if (id == class_1937.field_25180) {
         return "DIM-1";
      } else if (id == class_1937.field_25181) {
         return "DIM1";
      } else {
         class_2960 identifier = id.method_29177();
         String path = identifier.method_12832().replace('/', '%');
         path = IOUtils.replaceTrailingDots(path, ',');
         String var10000 = identifier.method_12836();
         return var10000 + "$" + path;
      }
   }

   public class_5321<class_1937> getDimensionIdForFolder(String folderName) {
      if (folderName.equals("null")) {
         return class_1937.field_25179;
      } else if (folderName.equals("DIM-1")) {
         return class_1937.field_25180;
      } else if (folderName.equals("DIM1")) {
         return class_1937.field_25181;
      } else {
         int separatorIndex = folderName.indexOf(36);
         if (separatorIndex == -1) {
            return null;
         } else {
            String namespace = folderName.substring(0, separatorIndex);
            String path = folderName.substring(separatorIndex + 1).replace('%', '/');
            path = path.replace(',', '.');

            try {
               class_2960 dimensionId = class_2960.method_60655(namespace, path);
               return class_5321.method_29179(class_7924.field_41223, dimensionId);
            } catch (class_151 var6) {
               return null;
            }
         }
      }
   }

   public void waitForLoadingToFinish(Runnable onFinish) {
      while(true) {
         synchronized(this.loadingSync) {
            if (!this.isLoading) {
               onFinish.run();
               return;
            }

            this.blockStateShortShapeCache.supplyForIOThread();
            this.worldDataHandler.handleRenderExecutor();
         }
      }
   }

   public synchronized void changeWorld(class_638 world, class_7225<class_2248> blockLookup, class_2378<class_2248> blockRegistry, class_2378<class_3611> fluidRegistry, class_2378<class_1959> biomeRegistry, class_2378<class_2874> dimensionTypeRegistry) {
      this.pushWriterPause();
      if (world != this.newWorld) {
         this.waitForLoadingToFinish(() -> {
            this.waitingForWorldUpdate = true;
         });
      }

      this.newWorld = world;
      this.newWorldBlockLookup = blockLookup;
      this.newWorldBlockRegistry = blockRegistry;
      this.newWorldFluidRegistry = fluidRegistry;
      this.newWorldBiomeRegistry = biomeRegistry;
      this.newWorldDimensionTypeRegistry = dimensionTypeRegistry;
      if (world == null) {
         this.mapWorldUsableRequest = false;
      } else {
         this.mapWorldUsableRequest = true;
         class_5321<class_1937> dimId = this.mapWorld.getPotentialDimId();
         this.mapWorld.setFutureDimensionId(dimId);
         this.updateDimension(world, dimId);
         this.mapWorld.getFutureDimension().resetCustomMultiworldUnsynced();
      }

      this.popWriterPause();
   }

   public void updateVisitedDimension(class_638 world) {
      this.updateDimension(world, world.method_27983());
   }

   public synchronized void updateDimension(class_638 world, class_5321<class_1937> dimId) {
      if (world != null) {
         Object autoIdBase = this.getAutoIdBase(world);
         MapDimension mapDimension = this.mapWorld.getDimension(dimId);
         if (mapDimension == null) {
            mapDimension = this.mapWorld.createDimensionUnsynced(dimId);
         }

         mapDimension.updateFutureAutomaticUnsynced(class_310.method_1551(), autoIdBase);
      }
   }

   /** @deprecated */
   @Deprecated
   private String getMainId(boolean rootFolderFormat, boolean preIP6Fix, class_634 connection) {
      return !rootFolderFormat ? this.getMainId(0, connection) : this.getMainId(preIP6Fix ? 1 : 2, connection);
   }

   private String getMainId(int version, class_634 connection) {
      class_310 mc = class_310.method_1551();
      String result = null;
      if (mc.method_1576() != null) {
         result = MapWorld.convertWorldFolderToRootId(version, mc.method_1576().method_27050(class_5218.field_24188).getParent().getFileName().toString());
      } else {
         class_642 serverData = connection.method_45734();
         if (serverData != null && serverData.method_52811() && WorldMap.events.getLatestRealm() != null) {
            String var10000 = String.valueOf(WorldMap.events.getLatestRealm().field_22605);
            result = "Realms_" + var10000 + "." + WorldMap.events.getLatestRealm().field_22599;
         } else if (serverData != null) {
            ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
            SingleConfigManager<Config> primaryConfigManager = configManager.getPrimaryConfigManager();
            boolean differentiateByServerAddress = (Boolean)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.DIFFERENTIATE_BY_SERVER_ADDRESS);
            String serverIP = differentiateByServerAddress ? serverData.field_3761 : "Any Address";
            int portDivider;
            if (version >= 2 && serverIP.indexOf(":") != serverIP.lastIndexOf(":")) {
               portDivider = serverIP.lastIndexOf("]:") + 1;
            } else {
               portDivider = serverIP.indexOf(":");
            }

            if (portDivider > 0) {
               serverIP = serverIP.substring(0, portDivider);
            }

            while(version >= 1 && serverIP.endsWith(".")) {
               serverIP = serverIP.substring(0, serverIP.length() - 1);
            }

            if (version >= 3) {
               serverIP = serverIP.replace("[", "").replace("]", "");
            }

            serverIP = serverIP.replaceAll(":", version < 4 ? "§" : ".");
            if (version >= 5) {
               serverIP = serverIP.trim();
               serverIP = IOUtils.replaceTrailingDots(serverIP, ',');
            }

            if (serverIP.isEmpty()) {
               serverIP = "Empty Address";
            }

            result = "Multiplayer_" + serverIP;
         } else {
            result = "Multiplayer_Unknown";
         }
      }

      return result;
   }

   public synchronized void toggleMultiworldType(MapDimension dim) {
      if (this.mapWorldUsable && !this.waitingForWorldUpdate && this.mapWorld.isMultiplayer() && this.mapWorld.getCurrentDimension() == dim) {
         this.mapWorld.toggleMultiworldTypeUnsynced();
      }

   }

   public synchronized void quickConfirmMultiworld() {
      if (this.canQuickConfirmUnsynced() && this.mapWorld.getCurrentDimension().hasConfirmedMultiworld()) {
         this.confirmMultiworld(this.mapWorld.getCurrentDimension());
      }

   }

   public synchronized boolean confirmMultiworld(MapDimension dim) {
      if (this.mapWorldUsable && this.mainWorld != null && this.mapWorld.getPotentialDimId() == this.mapWorld.getCurrentDimensionId() && this.mapWorld.getCurrentDimension() == dim) {
         this.mapWorld.confirmMultiworldTypeUnsynced();
         this.mapWorld.getCurrentDimension().confirmMultiworldUnsynced();
         return true;
      } else {
         return false;
      }
   }

   public synchronized void setMultiworld(MapDimension dimToCompare, String customMW) {
      if (this.mapWorldUsable && dimToCompare.getMapWorld() == this.mapWorld) {
         dimToCompare.setMultiworldUnsynced(customMW);
      }

   }

   public boolean canQuickConfirmUnsynced() {
      return this.mapWorldUsable && !this.mapWorld.getCurrentDimension().futureMultiworldWritable && this.mapWorld.getPotentialDimId() == this.mapWorld.getCurrentDimensionId();
   }

   public String getCrosshairMessage() {
      synchronized(this.uiPauseSync) {
         if (this.isUIPaused) {
            return null;
         } else if (this.canQuickConfirmUnsynced()) {
            String selectedMWName = this.mapWorld.getCurrentDimension().getMultiworldName(this.mapWorld.getCurrentDimension().getFutureMultiworldUnsynced());
            String var10000 = ControlsRegister.keyOpenMap.method_16007().getString().toUpperCase();
            String message = "§2(" + var10000 + ")§r " + class_1074.method_4662("gui.xaero_map_unconfirmed", new Object[0]);
            if (this.mapWorld.getCurrentDimension().hasConfirmedMultiworld()) {
               message = message + " §2" + ControlsRegister.keyQuickConfirm.method_16007().getString().toUpperCase() + "§r for map \"" + class_1074.method_4662(selectedMWName, new Object[0]) + "\"";
            }

            return message;
         } else {
            return null;
         }
      }
   }

   public synchronized void checkForWorldUpdate() {
      if (this.mainWorld != null) {
         Object autoIdBase = this.getAutoIdBase(this.mainWorld);
         if (autoIdBase != null) {
            boolean baseChanged = !autoIdBase.equals(this.getUsedAutoIdBase(this.mainWorld));
            class_5321<class_1937> potentialDimId = this.mapWorld.getPotentialDimId();
            if (baseChanged && this.mapWorldUsableRequest) {
               MapDimension mapDimension = this.mapWorld.getDimension(potentialDimId);
               if (mapDimension != null) {
                  boolean serverBasedBefore = mapDimension.isFutureMultiworldServerBased();
                  mapDimension.updateFutureAutomaticUnsynced(class_310.method_1551(), autoIdBase);
                  if (serverBasedBefore != mapDimension.isFutureMultiworldServerBased()) {
                     mapDimension.resetCustomMultiworldUnsynced();
                  }
               }
            }

            if (this.mainWorld != this.world || potentialDimId != this.mapWorld.getFutureDimensionId()) {
               this.changeWorld(this.mainWorld, this.mainWorldBlockLookup, this.mainWorldBlockRegistry, this.mainWorldFluidRegistry, this.mainWorldBiomeRegistry, this.mainWorldDimensionTypeRegistry);
            }

            Object updatedAutoIdBase = this.getAutoIdBase(this.mainWorld);
            if (updatedAutoIdBase != null) {
               this.setUsedAutoIdBase(this.mainWorld, updatedAutoIdBase);
            } else {
               this.removeUsedAutoIdBase(this.mainWorld);
            }

            if (potentialDimId != this.mainWorld.method_27983()) {
               this.updateVisitedDimension(this.mainWorld);
            }
         }
      }

   }

   private void updateWorld() throws IOException, CommandSyntaxException {
      this.pushUIPause();
      this.updateWorldSynced();
      this.popUIPause();
      if (this.mapWorldUsable && !this.mapSaveLoad.isRegionDetectionComplete()) {
         this.mapSaveLoad.detectRegions(10);
         this.mapSaveLoad.setRegionDetectionComplete(true);
      }

   }

   private synchronized void updateWorldSynced() throws IOException, CommandSyntaxException {
      synchronized(this.uiSync) {
         boolean changedDimension = this.mapWorldUsable != this.mapWorldUsableRequest || !this.mapWorldUsableRequest || this.mapWorld.getFutureDimension() != this.mapWorld.getCurrentDimension();
         if (this.mapWorldUsable == this.mapWorldUsableRequest && (!this.mapWorldUsableRequest || !changedDimension && this.mapWorld.getFutureDimension().getFutureMultiworldUnsynced().equals(this.mapWorld.getFutureDimension().getCurrentMultiworld()))) {
            if (this.newWorld != this.world) {
               this.pushRenderPause(false, true);
               this.pushWriterPause();
               this.checkFootstepsReset(this.world, this.newWorld);
               this.world = this.newWorld;
               this.worldBlockLookup = this.newWorldBlockLookup;
               this.worldBlockRegistry = this.newWorldBlockRegistry;
               this.worldFluidRegistry = this.newWorldFluidRegistry;
               this.worldBiomeRegistry = this.newWorldBiomeRegistry;
               this.worldDimensionTypeRegistry = this.newWorldDimensionTypeRegistry;
               this.worldBlockTintProvider = this.world == null ? null : new BlockTintProvider(this.worldBiomeRegistry, this.biomeColorCalculator, this, this.brokenBlockTintCache, this.mapWriter);
               if (SupportMods.framedBlocks()) {
                  SupportMods.supportFramedBlocks.onWorldChange();
               }

               if (SupportMods.pac()) {
                  SupportMods.xaeroPac.resetDetection();
               }

               this.mapWorld.onWorldChangeUnsynced(this.world);
               this.popRenderPause(false, true);
               this.popWriterPause();
            }
         } else {
            ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
            SingleConfigManager<Config> primaryConfigManager = configManager.getPrimaryConfigManager();
            boolean debugConfig = (Boolean)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.DEBUG);
            String newMWId = !this.mapWorldUsableRequest ? null : this.mapWorld.getFutureMultiworldUnsynced();
            this.pushRenderPause(true, true);
            this.pushWriterPause();
            String newWorldId = !this.mapWorldUsableRequest ? null : this.mapWorld.getMainId();
            boolean shouldClearAllDimensions = this.state == 1;
            boolean shouldClearNewDimension = this.mapWorldUsableRequest && !this.mapWorld.getFutureMultiworldUnsynced().equals(this.mapWorld.getFutureDimension().getCurrentMultiworld());
            this.mapSaveLoad.getToSave().clear();
            if (this.currentMapLock != null) {
               this.mapLockToRelease = this.currentMapLock;
               this.mapLockChannelToClose = this.currentMapLockChannel;
               this.currentMapLock = null;
               this.currentMapLockChannel = null;
            }

            this.releaseLocksIfNeeded();
            MapDimension reqDim;
            boolean totalLockAttempts;
            if (this.mapWorld.getCurrentDimensionId() != null) {
               MapDimension currentDim = this.mapWorld.getCurrentDimension();
               reqDim = !this.mapWorldUsableRequest ? null : this.mapWorld.getFutureDimension();
               totalLockAttempts = this.mapWorldUsable && !this.currentMapNeedsDeletion;
               boolean currentDimChecked = false;
               if (totalLockAttempts) {
                  this.mapSaveLoad.saveAll = true;
               }

               Iterator var14;
               LeveledRegion region;
               if (totalLockAttempts || shouldClearNewDimension && reqDim == currentDim) {
                  var14 = currentDim.getLayeredMapRegions().getUnsyncedSet().iterator();

                  label304:
                  while(true) {
                     do {
                        if (!var14.hasNext()) {
                           currentDimChecked = true;
                           break label304;
                        }

                        region = (LeveledRegion)var14.next();
                        if (totalLockAttempts) {
                           if (region.getLevel() == 0) {
                              MapRegion leafRegion = (MapRegion)region;
                              if (!leafRegion.isNormalMapData() && !leafRegion.hasLookedForCache() && leafRegion.isOutdatedWithOtherLayers()) {
                                 File potentialCacheFile = this.mapSaveLoad.getCacheFile(leafRegion, leafRegion.getCaveLayer(), false, false);
                                 if (potentialCacheFile.exists()) {
                                    leafRegion.setCacheFile(potentialCacheFile);
                                    leafRegion.setLookedForCache(true);
                                 }
                              }

                              if (leafRegion.shouldConvertCacheToOutdatedOnFinishDim() && leafRegion.getCacheFile() != null) {
                                 leafRegion.convertCacheToOutdated(this.mapSaveLoad, "might be outdated");
                                 if (debugConfig) {
                                    WorldMap.LOGGER.info(String.format("Converting cache for region %s because it might be outdated.", leafRegion));
                                 }
                              }
                           }

                           region.setReloadHasBeenRequested(false, "world/dim change");
                           region.onCurrentDimFinish(this.mapSaveLoad, this);
                        }
                     } while(!shouldClearAllDimensions && (!shouldClearNewDimension || reqDim != currentDim));

                     region.onDimensionClear(this);
                  }
               }

               if (reqDim != currentDim && shouldClearNewDimension) {
                  var14 = reqDim.getLayeredMapRegions().getUnsyncedSet().iterator();

                  while(var14.hasNext()) {
                     region = (LeveledRegion)var14.next();
                     region.onDimensionClear(this);
                  }
               }

               if (shouldClearAllDimensions) {
                  var14 = this.mapWorld.getDimensionsList().iterator();

                  label280:
                  while(true) {
                     MapDimension dim;
                     do {
                        if (!var14.hasNext()) {
                           break label280;
                        }

                        dim = (MapDimension)var14.next();
                     } while(currentDimChecked && dim == currentDim);

                     Iterator var32 = dim.getLayeredMapRegions().getUnsyncedSet().iterator();

                     while(var32.hasNext()) {
                        LeveledRegion<?> region = (LeveledRegion)var32.next();
                        region.onDimensionClear(this);
                     }
                  }
               }

               if (this.currentMapNeedsDeletion) {
                  this.mapWorld.getCurrentDimension().deleteMultiworldMapDataUnsynced(this.mapWorld.getCurrentDimension().getCurrentMultiworld());
               }
            }

            this.currentMapNeedsDeletion = false;
            if (!shouldClearAllDimensions) {
               if (shouldClearNewDimension) {
                  this.mapWorld.getFutureDimension().clear();
                  if (debugConfig) {
                     WorldMap.LOGGER.info("Dimension map data cleared!");
                  }
               }
            } else {
               if (this.mapWorld.getCurrentDimensionId() != null) {
                  Iterator var22 = this.mapWorld.getDimensionsList().iterator();

                  while(var22.hasNext()) {
                     reqDim = (MapDimension)var22.next();
                     reqDim.clear();
                  }
               }

               if (debugConfig) {
                  WorldMap.LOGGER.info("All map data cleared!");
               }

               if (this.state == 1) {
                  WorldMap.LOGGER.info("World map cleaned normally!");
                  this.state = 2;
               }
            }

            if (debugConfig) {
               WorldMap.LOGGER.info("World changed!");
            }

            this.mapWorldUsable = this.mapWorldUsableRequest;
            if (this.mapWorldUsableRequest) {
               this.mapWorld.switchToFutureUnsynced();
            }

            this.currentWorldId = newWorldId;
            this.currentDimId = !this.mapWorldUsableRequest ? null : this.getDimensionName(this.mapWorld.getFutureDimensionId());
            this.currentMWId = newMWId;
            Path mapPath = this.mapSaveLoad.getMWSubFolder(this.currentWorldId, this.currentDimId, this.currentMWId);
            if (this.mapWorldUsable) {
               Files.createDirectories(mapPath);
               Path mapLockPath = mapPath.resolve(".lock");
               totalLockAttempts = true;
               int lockAttempts = 10;

               while(lockAttempts-- > 0) {
                  if (lockAttempts < 9) {
                     WorldMap.LOGGER.info("Failed attempt to lock the current world map! Retrying in 50 ms... " + lockAttempts);

                     try {
                        Thread.sleep(50L);
                     } catch (InterruptedException var19) {
                     }
                  }

                  try {
                     FileChannel lockChannel = FileChannel.open(mapLockPath, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                     this.currentMapLock = lockChannel.tryLock();
                     if (this.currentMapLock != null) {
                        this.currentMapLockChannel = lockChannel;
                        break;
                     }
                  } catch (Exception var20) {
                     WorldMap.LOGGER.error("suppressed exception", var20);
                  }
               }
            }

            this.checkFootstepsReset(this.world, this.newWorld);
            this.mapSaveLoad.clearToLoad();
            this.mapSaveLoad.setNextToLoadByViewing((LeveledRegion)null);
            this.clearToRefresh();

            for(int i = 0; i < this.toProcessLevels.length; ++i) {
               this.toProcessLevels[i].clear();
            }

            if (this.mapWorldUsable && !this.isCurrentMapLocked()) {
               Iterator var26 = this.mapWorld.getCurrentDimension().getLayeredMapRegions().getUnsyncedSet().iterator();

               while(var26.hasNext()) {
                  LeveledRegion<?> region = (LeveledRegion)var26.next();
                  if (region.shouldBeProcessed()) {
                     this.addToProcess(region);
                  }
               }
            }

            this.mapWriter.resetPosition();
            this.world = this.newWorld;
            this.worldBlockLookup = this.newWorldBlockLookup;
            this.worldBlockRegistry = this.newWorldBlockRegistry;
            this.worldFluidRegistry = this.newWorldFluidRegistry;
            this.worldBiomeRegistry = this.newWorldBiomeRegistry;
            this.worldDimensionTypeRegistry = this.newWorldDimensionTypeRegistry;
            this.worldBlockTintProvider = this.world == null ? null : new BlockTintProvider(this.worldBiomeRegistry, this.biomeColorCalculator, this, this.brokenBlockTintCache, this.mapWriter);
            if (SupportMods.framedBlocks()) {
               SupportMods.supportFramedBlocks.onWorldChange();
            }

            if (SupportMods.pac()) {
               SupportMods.xaeroPac.onMapChange(changedDimension);
               SupportMods.xaeroPac.resetDetection();
            }

            this.mapWorld.onWorldChangeUnsynced(this.world);
            if (debugConfig) {
               WorldMap.LOGGER.info("World/dimension changed to: " + this.currentWorldId + " " + this.currentDimId + " " + this.currentMWId);
            }

            this.worldDataHandler.prepareSingleplayer(this.world, this);
            if (this.worldDataHandler.getWorldDir() == null && this.currentWorldId != null && this.mapWorld.getCurrentDimension().isUsingWorldSave()) {
               this.currentWorldId = this.currentDimId = null;
            }

            boolean shouldDetect = this.mapWorldUsable && !this.mapWorld.getCurrentDimension().hasDoneRegionDetection();
            this.mapSaveLoad.setRegionDetectionComplete(!shouldDetect);
            this.popRenderPause(true, true);
            this.popWriterPause();
         }

         if (this.mapWorldUsable) {
            this.mapWorld.getCurrentDimension().switchToFutureMultiworldWritableValueUnsynced();
            this.mapWorld.switchToFutureMultiworldTypeUnsynced();
         }

         this.waitingForWorldUpdate = false;
      }
   }

   public void updateFootprints(int step) {
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      if ((Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.FOOTSTEPS)) {
         if (this.footprintsTimer > 0) {
            this.footprintsTimer -= step;
         } else {
            Double[] coords = new Double[]{this.mainPlayerX, this.mainPlayerZ};
            synchronized(this.footprints) {
               this.footprints.add(coords);
               if (this.footprints.size() > 32) {
                  this.footprints.remove(0);
               }
            }

            this.footprintsTimer = 20;
         }
      }

   }

   public void addToRefresh(MapRegion region, boolean prepareHighlights) {
      synchronized(this.toRefresh) {
         if (!this.toRefresh.contains(region)) {
            this.toRefresh.add(0, region);
         }
      }

      if (prepareHighlights) {
         this.mapRegionHighlightsPreparer.prepare(region, false);
      }

   }

   public void removeToRefresh(MapRegion region) {
      synchronized(this.toRefresh) {
         this.toRefresh.remove(region);
      }
   }

   private void clearToRefresh() {
      synchronized(this.toRefresh) {
         this.toRefresh.clear();
      }
   }

   private void handleRefresh() throws RuntimeException {
      this.pushIsLoading();
      if (!this.waitingForWorldUpdate && !this.toRefresh.isEmpty()) {
         MapRegion region = (MapRegion)this.toRefresh.get(0);
         if (!region.isRefreshing()) {
            throw new RuntimeException(String.format("Trying to refresh region %s, which is not marked as being refreshed!", region));
         }

         ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
         SingleConfigManager<Config> primaryConfigManager = configManager.getPrimaryConfigManager();
         boolean debugConfig = (Boolean)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.DEBUG);
         int globalVersion = (Integer)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.GLOBAL_VERSION);
         int globalReloadVersion = (Integer)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.RELOAD_VIEWED_VERSION);
         int globalCacheHashCode = WorldMap.settings.getRegionCacheHashCode();
         boolean regionLoaded;
         synchronized(region) {
            regionLoaded = region.getLoadState() == 2;
            if (regionLoaded) {
               region.setRecacheHasBeenRequested(true, "refresh handle");
               region.setShouldCache(true, "refresh handle");
               region.setVersion(globalVersion);
               region.setCacheHashCode(globalCacheHashCode);
               region.setReloadVersion(globalReloadVersion);
               region.setHighlightsHash(region.getTargetHighlightsHash());
            }
         }

         boolean isEmpty = true;
         if (regionLoaded) {
            synchronized(region) {
               region.setAllCachePrepared(false);
            }

            boolean skipRegularRefresh = false;
            int upToDateCaveStart = region.getUpToDateCaveStart();
            int caveModeDepth = (Integer)configManager.getEffective(WorldMapProfiledConfigOptions.CAVE_MODE_DEPTH);
            Logger var10000;
            String var10001;
            if (region.isBeingWritten() && region.caveStartOutdated(upToDateCaveStart, caveModeDepth)) {
               try {
                  this.getWorldDataHandler().buildRegion(region, this.worldBlockLookup, this.worldBlockRegistry, this.worldFluidRegistry, false, (int[])null);
                  skipRegularRefresh = true;
               } catch (Throwable var23) {
                  var10000 = WorldMap.LOGGER;
                  var10001 = String.valueOf(region);
                  var10000.info("Region failed to refresh from world save: " + var10001 + " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId());
               }
            }

            int i = 0;

            while(true) {
               if (i >= 8) {
                  if (debugConfig) {
                     var10000 = WorldMap.LOGGER;
                     var10001 = String.valueOf(region);
                     var10000.info("Region refreshed: " + var10001 + " " + String.valueOf(region) + " " + this.mapWriter.getUpdateCounter());
                  }
                  break;
               }

               for(int j = 0; j < 8; ++j) {
                  MapTileChunk chunk = region.getChunk(i, j);
                  if (chunk != null) {
                     if (chunk.hasHadTerrain()) {
                        if (!skipRegularRefresh && chunk.getLoadState() == 2) {
                           for(int tileX = 0; tileX < 4; ++tileX) {
                              for(int tileZ = 0; tileZ < 4; ++tileZ) {
                                 region.pushWriterPause();
                                 MapTile tile = chunk.getTile(tileX, tileZ);
                                 if (tile != null && tile.isLoaded()) {
                                    for(int o = 0; o < 16; ++o) {
                                       MapBlock[] column = tile.getBlockColumn(o);

                                       for(int p = 0; p < 16; ++p) {
                                          column[p].setSlopeUnknown(true);
                                       }
                                    }
                                 }

                                 chunk.setTile(tileX, tileZ, tile, this.blockStateShortShapeCache);
                                 region.popWriterPause();
                              }
                           }

                           chunk.setToUpdateBuffers(true);
                        }
                     } else {
                        region.pushWriterPause();
                        if (!chunk.hasHadTerrain() && !chunk.wasChanged() && !chunk.getToUpdateBuffers()) {
                           region.uncountTextureBiomes(chunk.getLeafTexture());
                           chunk.getLeafTexture().resetBiomes();
                           if (chunk.hasHighlightsIfUndiscovered()) {
                              chunk.getLeafTexture().requestHighlightOnlyUpload();
                           } else {
                              region.setChunk(i, j, (MapTileChunk)null);
                              chunk.getLeafTexture().deleteTexturesAndBuffers();
                           }
                        }

                        region.popWriterPause();
                     }

                     isEmpty = false;
                  }
               }

               ++i;
            }
         }

         synchronized(region) {
            region.setRefreshing(false);
            if (isEmpty) {
               region.setShouldCache(false, "refresh handle");
               region.setRecacheHasBeenRequested(false, "refresh handle");
            }
         }

         if (region.isResaving()) {
            region.setLastSaveTime(-60000L);
         }

         this.removeToRefresh(region);
      }

      this.popIsLoading();
   }

   public boolean regionExists(int caveLayer, int x, int z) {
      return this.regionDetectionExists(caveLayer, x, z) || this.mapWorld.getCurrentDimension().getHighlightHandler().shouldApplyRegionHighlights(x, z, false);
   }

   public boolean regionDetectionExists(int caveLayer, int x, int z) {
      return !this.mapSaveLoad.isRegionDetectionComplete() ? false : this.mapWorld.getCurrentDimension().getLayeredMapRegions().getLayer(caveLayer).regionDetectionExists(x, z);
   }

   public void removeMapRegion(LeveledRegion<?> region) {
      MapDimension regionDim = region.getDim();
      LayeredRegionManager regions = regionDim.getLayeredMapRegions();
      if (region.getLevel() == 0) {
         regions.remove(region.getCaveLayer(), region.getRegionX(), region.getRegionZ(), region.getLevel());
         regions.removeListRegion(region);
      }

      regions.removeLoadedRegion(region);
      this.removeToProcess(region);
   }

   public LeveledRegion<?> getLeveledRegion(int caveLayer, int leveledRegX, int leveledRegZ, int level) {
      MapDimension mapDimension = this.mapWorld.getCurrentDimension();
      LayeredRegionManager regions = mapDimension.getLayeredMapRegions();
      return regions.get(caveLayer, leveledRegX, leveledRegZ, level);
   }

   public void initMinimapRender(int flooredMapCameraX, int flooredMapCameraZ) {
      this.minimapRenderListener.init(this, flooredMapCameraX, flooredMapCameraZ);
   }

   public void beforeMinimapRegionRender(MapRegion region) {
      this.minimapRenderListener.beforeMinimapRender(region);
   }

   public void finalizeMinimapRender() {
      this.minimapRenderListener.finalize(this);
   }

   public MapRegion getLeafMapRegion(int caveLayer, int regX, int regZ, boolean create) {
      if (!this.mapSaveLoad.isRegionDetectionComplete()) {
         return null;
      } else {
         MapDimension mapDimension = this.mapWorld.getCurrentDimension();
         LayeredRegionManager regions = mapDimension.getLayeredMapRegions();
         MapRegion region = regions.getLeaf(caveLayer, regX, regZ);
         if (region == null) {
            if (!create) {
               return null;
            }

            if (!class_310.method_1551().method_18854()) {
               throw new IllegalAccessError();
            }

            ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
            SingleConfigManager<Config> primaryConfigManager = configManager.getPrimaryConfigManager();
            int globalVersion = (Integer)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.GLOBAL_VERSION);
            region = new MapRegion(this.currentWorldId, this.currentDimId, this.currentMWId, mapDimension, regX, regZ, caveLayer, globalVersion, !mapDimension.isUsingWorldSave(), this.worldBiomeRegistry);
            MapLayer mapLayer = regions.getLayer(caveLayer);
            region.updateCaveMode();
            RegionDetection regionDetection = mapLayer.getRegionDetection(regX, regZ);
            if (regionDetection != null) {
               regionDetection.transferInfoTo(region);
               mapLayer.removeRegionDetection(regX, regZ);
            } else if (mapLayer.getCompleteRegionDetection(regX, regZ) == null) {
               RegionDetection perpetualRegionDetection = new RegionDetection(region.getWorldId(), region.getDimId(), region.getMwId(), region.getRegionX(), region.getRegionZ(), region.getRegionFile(), globalVersion, true);
               mapLayer.tryAddingToCompleteRegionDetection(perpetualRegionDetection);
               if (!region.isNormalMapData()) {
                  mapLayer.removeRegionDetection(regX, regZ);
               }
            }

            if (!region.hasHadTerrain()) {
               regions.getLayer(caveLayer).getRegionHighlightExistenceTracker().stopTracking(regX, regZ);
               region.setVersion(globalVersion);
               region.setCacheHashCode(WorldMap.settings.getRegionCacheHashCode());
               region.setReloadVersion((Integer)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.RELOAD_VIEWED_VERSION));
            }

            regions.putLeaf(regX, regZ, region);
            regions.addListRegion(region);
            if (regionDetection != null) {
               regionDetection.transferInfoPostAddTo(region, this);
            }
         }

         return region;
      }
   }

   public MapRegion getMinimapMapRegion(int regX, int regZ) {
      int renderedCaveLayer = this.minimapRenderListener.getRenderedCaveLayer();
      return this.getLeafMapRegion(renderedCaveLayer, regX, regZ, this.regionExists(renderedCaveLayer, regX, regZ));
   }

   public MapTileChunk getMapChunk(int caveLayer, int chunkX, int chunkZ) {
      int regionX = chunkX >> 3;
      int regionZ = chunkZ >> 3;
      MapRegion region = this.getLeafMapRegion(caveLayer, regionX, regionZ, false);
      if (region == null) {
         return null;
      } else {
         int localChunkX = chunkX & 7;
         int localChunkZ = chunkZ & 7;
         return region.getChunk(localChunkX, localChunkZ);
      }
   }

   public MapTile getMapTile(int caveLayer, int x, int z) {
      MapTileChunk tileChunk = this.getMapChunk(caveLayer, x >> 2, z >> 2);
      if (tileChunk == null) {
         return null;
      } else {
         int tileX = x & 3;
         int tileZ = z & 3;
         return tileChunk.getTile(tileX, tileZ);
      }
   }

   public void updateWorldSpawn(class_2338 newSpawn, class_638 world) {
      boolean debugConfig = WorldMapClientConfigUtils.getDebug();
      class_5321<class_1937> dimId = world.method_27983();
      WorldMapClientWorldData worldData = WorldMapClientWorldDataHelper.getWorldData(world);
      worldData.latestSpawn = newSpawn;
      if (debugConfig) {
         Logger var10000 = WorldMap.LOGGER;
         String var10001 = String.valueOf(dimId);
         var10000.info("Updated spawn for dimension " + var10001 + " " + String.valueOf(newSpawn));
      }

      this.spawnToRestore = newSpawn;
      if (world == this.mainWorld) {
         this.mainWorldChangedTime = -1L;
         if (debugConfig) {
            WorldMap.LOGGER.info("Done waiting for main spawn.");
         }
      }

      this.checkForWorldUpdate();
   }

   public void onServerLevelId(int serverLevelId) {
      WorldMapClientWorldData worldData = WorldMapClientWorldDataHelper.getCurrentWorldData();
      worldData.serverLevelId = serverLevelId;
      if (WorldMapClientConfigUtils.getDebug()) {
         WorldMap.LOGGER.info("Updated server level id " + serverLevelId);
      }

      this.checkForWorldUpdate();
   }

   public void onWorldUnload() {
      if (!this.mainWorldUnloaded) {
         if (WorldMapClientConfigUtils.getDebug()) {
            WorldMap.LOGGER.info("Changing worlds, pausing the world map...");
         }

         this.mainWorldUnloaded = true;
         this.mapWorld.clearAllCachedHighlightHashes();
         this.mainWorldChangedTime = -1L;
         this.changeWorld((class_638)null, (class_7225)null, (class_2378)null, (class_2378)null, (class_2378)null, (class_2378)null);
      }
   }

   public void onClientTickStart() throws RuntimeException {
      if (this.mainWorld != null && this.spawnToRestore != null && this.mainWorldChangedTime != -1L && System.currentTimeMillis() - this.mainWorldChangedTime >= 3000L) {
         if (WorldMapClientConfigUtils.getDebug()) {
            WorldMap.LOGGER.info("SPAWN SET TIME OUT");
         }

         this.updateWorldSpawn(this.spawnToRestore, this.mainWorld);
      }

   }

   private void updateRenderStartTime() {
      if (this.renderStartTime == -1L) {
         this.renderStartTime = System.nanoTime();
      }

   }

   public void pushWriterPause() {
      synchronized(this.renderThreadPauseSync) {
         ++this.pauseWriting;
      }
   }

   public void popWriterPause() {
      synchronized(this.renderThreadPauseSync) {
         --this.pauseWriting;
      }
   }

   public void pushRenderPause(boolean rendering, boolean uploading) {
      synchronized(this.renderThreadPauseSync) {
         if (rendering) {
            ++this.pauseRendering;
         }

         if (uploading) {
            ++this.pauseUploading;
         }

      }
   }

   public void popRenderPause(boolean rendering, boolean uploading) {
      synchronized(this.renderThreadPauseSync) {
         if (rendering) {
            --this.pauseRendering;
         }

         if (uploading) {
            --this.pauseUploading;
         }

      }
   }

   public void pushIsLoading() {
      synchronized(this.loadingSync) {
         this.isLoading = true;
      }
   }

   public void popIsLoading() {
      synchronized(this.loadingSync) {
         this.isLoading = false;
      }
   }

   public void pushUIPause() {
      synchronized(this.uiPauseSync) {
         this.isUIPaused = true;
      }
   }

   public void popUIPause() {
      synchronized(this.uiPauseSync) {
         this.isUIPaused = false;
      }
   }

   public boolean isUIPaused() {
      return this.isUIPaused;
   }

   public boolean isWritingPaused() {
      return this.pauseWriting > 0;
   }

   public boolean isRenderingPaused() {
      return this.pauseRendering > 0;
   }

   public boolean isUploadingPaused() {
      return this.pauseUploading > 0;
   }

   public boolean isProcessingPaused() {
      return this.pauseProcessing > 0;
   }

   public boolean isProcessed(LeveledRegion<?> region) {
      ArrayList<LeveledRegion<?>> toProcess = this.toProcessLevels[region.getLevel()];
      synchronized(toProcess) {
         return toProcess.contains(region);
      }
   }

   public void addToProcess(LeveledRegion<?> region) {
      ArrayList<LeveledRegion<?>> toProcess = this.toProcessLevels[region.getLevel()];
      synchronized(toProcess) {
         toProcess.add(region);
      }
   }

   public void removeToProcess(LeveledRegion<?> region) {
      ArrayList<LeveledRegion<?>> toProcess = this.toProcessLevels[region.getLevel()];
      synchronized(toProcess) {
         toProcess.remove(region);
      }
   }

   public int getProcessedCount() {
      int total = 0;

      for(int i = 0; i < this.toProcessLevels.length; ++i) {
         total += this.toProcessLevels[i].size();
      }

      return total;
   }

   public int getAffectingLoadingFrequencyCount() {
      int total = 0;

      for(int i = 0; i < this.toProcessLevels.length; ++i) {
         ArrayList<LeveledRegion<?>> processed = this.toProcessLevels[i];

         for(int j = 0; j < processed.size(); ++j) {
            synchronized(processed) {
               if (j >= processed.size()) {
                  break;
               }

               if (((LeveledRegion)processed.get(j)).shouldAffectLoadingRequestFrequency()) {
                  ++total;
               }
            }
         }
      }

      return total;
   }

   public MapSaveLoad getMapSaveLoad() {
      return this.mapSaveLoad;
   }

   public class_638 getWorld() {
      return this.world;
   }

   public class_638 getNewWorld() {
      return this.newWorld;
   }

   public String getCurrentWorldId() {
      return this.currentWorldId;
   }

   public String getCurrentDimId() {
      return this.currentDimId;
   }

   public String getCurrentMWId() {
      return this.currentMWId;
   }

   public MapWriter getMapWriter() {
      return this.mapWriter;
   }

   public MapLimiter getMapLimiter() {
      return this.mapLimiter;
   }

   public ArrayList<Double[]> getFootprints() {
      return this.footprints;
   }

   public ByteBufferDeallocator getBufferDeallocator() {
      return this.bufferDeallocator;
   }

   public MapTilePool getTilePool() {
      return this.tilePool;
   }

   public OverlayManager getOverlayManager() {
      return this.overlayManager;
   }

   public int getGlobalVersion() {
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      SingleConfigManager<Config> primaryConfigManager = configManager.getPrimaryConfigManager();
      return (Integer)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.GLOBAL_VERSION);
   }

   public long getRenderStartTime() {
      return this.renderStartTime;
   }

   public void resetRenderStartTime() {
      this.renderStartTime = -1L;
   }

   public Queue<Runnable> getMinecraftScheduledTasks() {
      this.scheduledTasksField.setAccessible(true);

      Queue result;
      try {
         result = (Queue)this.scheduledTasksField.get(class_310.method_1551());
      } catch (IllegalArgumentException var3) {
         result = null;
      } catch (IllegalAccessException var4) {
         result = null;
      }

      this.scheduledTasksField.setAccessible(false);
      return result;
   }

   public Runnable getRenderStartTimeUpdater() {
      return this.renderStartTimeUpdaterRunnable;
   }

   public boolean isWaitingForWorldUpdate() {
      return this.waitingForWorldUpdate;
   }

   public WorldDataHandler getWorldDataHandler() {
      return this.worldDataHandler;
   }

   public void setMainValues() {
      synchronized(this.mainStuffSync) {
         class_1297 player = class_310.method_1551().method_1560();
         if (player != null) {
            class_638 worldToChangeTo = !this.ignoreWorld(player.method_37908()) && player.method_37908() instanceof class_638 ? (class_638)player.method_37908() : this.mainWorld;
            boolean worldChanging = worldToChangeTo != this.mainWorld;
            if (worldChanging) {
               this.mainWorldChangedTime = -1L;
               if (this.spawnToRestore != null) {
                  WorldMapClientWorldData worldData = WorldMapClientWorldDataHelper.getWorldData(worldToChangeTo);
                  if (worldData.latestSpawn == null) {
                     this.mainWorldChangedTime = System.currentTimeMillis();
                  }
               }

               this.mainWorldUnloaded = false;
               this.mainWorldBlockLookup = worldToChangeTo == null ? null : worldToChangeTo.method_45448(class_7924.field_41254);
               this.mainWorldBlockRegistry = worldToChangeTo == null ? null : worldToChangeTo.method_30349().method_30530(class_7924.field_41254);
               this.mainWorldFluidRegistry = worldToChangeTo == null ? null : worldToChangeTo.method_30349().method_30530(class_7924.field_41270);
               this.mainWorldBiomeRegistry = worldToChangeTo == null ? null : worldToChangeTo.method_30349().method_30530(class_7924.field_41236);
               this.mainWorldDimensionTypeRegistry = worldToChangeTo == null ? null : worldToChangeTo.method_30349().method_30530(class_7924.field_41241);
            }

            this.mainWorld = worldToChangeTo;
            this.mainPlayerX = player.method_23317();
            this.mainPlayerY = player.method_23318();
            this.mainPlayerZ = player.method_23321();
            if (worldChanging) {
               this.checkForWorldUpdate();
            }
         } else {
            if (this.mainWorld != null && !this.mainWorldUnloaded) {
               this.onWorldUnload();
            }

            this.mainWorld = null;
         }

      }
   }

   public float getBrightness() {
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      return this.getBrightness((Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.LIGHTING));
   }

   public float getBrightness(boolean lighting) {
      return this.getBrightness(this.currentCaveLayer, this.world, lighting);
   }

   public float getBrightness(int layer, class_638 world, boolean lighting) {
      if (world != null && world == this.world) {
         MapDimension dim = this.mapWorld.getCurrentDimension();
         class_2874 dimType = dim.getDimensionType(this.worldDimensionTypeRegistry);
         float sunBrightness;
         if (layer == Integer.MAX_VALUE || dimType != null && !dimType.comp_642()) {
            if (!lighting) {
               return 1.0F;
            }

            if (dimType != null && dim.getDimensionEffects(this.worldDimensionTypeRegistry).method_28114()) {
               return 1.0F;
            }

            sunBrightness = (dim.getSkyDarken(1.0F, world, this.worldDimensionTypeRegistry) - 0.2F) / 0.8F;
         } else {
            if (!lighting) {
               return 1.0F;
            }

            sunBrightness = 0.0F;
         }

         float ambient = this.getAmbientBrightness(dimType);
         return ambient + (1.0F - ambient) * class_3532.method_15363(sunBrightness, 0.0F, 1.0F);
      } else {
         return 1.0F;
      }
   }

   public float getAmbientBrightness(class_2874 dimType) {
      float result = 0.375F + (dimType == null ? 0.0F : dimType.comp_656());
      if (result > 1.0F) {
         result = 1.0F;
      }

      return result;
   }

   public static boolean isWorldRealms(String world) {
      return world.startsWith("Realms_");
   }

   public static boolean isWorldMultiplayer(boolean realms, String world) {
      return realms || world.startsWith("Multiplayer_");
   }

   public MapWorld getMapWorld() {
      return this.mapWorld;
   }

   public boolean isCurrentMultiworldWritable() {
      return this.mapWorldUsable && this.mapWorld.getCurrentDimension().currentMultiworldWritable;
   }

   public String getCurrentDimension() {
      return "placeholder";
   }

   public void requestCurrentMapDeletion() {
      if (this.currentMapNeedsDeletion) {
         throw new RuntimeException("Requesting map deletion at a weird time!");
      } else {
         this.currentMapNeedsDeletion = true;
      }
   }

   public boolean isFinalizing() {
      return this.finalizing;
   }

   public void stop() {
      this.finalizing = true;
      WorldMap.mapRunner.addTask(new MapRunnerTask() {
         public void run(MapProcessor doNotUse) {
            if (MapProcessor.this.state == 0) {
               MapProcessor.this.state = 1;
               if (!MapProcessor.this.mapWorldUsable) {
                  MapProcessor.this.forceClean();
               } else {
                  MapProcessor.this.changeWorld((class_638)null, (class_7225)null, (class_2378)null, (class_2378)null, (class_2378)null, (class_2378)null);
               }
            }

         }
      });
   }

   private synchronized void forceClean() {
      this.pushRenderPause(true, true);
      this.pushWriterPause();
      if (this.mapWorld != null) {
         Iterator var1 = this.mapWorld.getDimensionsList().iterator();

         while(var1.hasNext()) {
            MapDimension dim = (MapDimension)var1.next();
            Iterator var3 = dim.getLayeredMapRegions().getUnsyncedSet().iterator();

            while(var3.hasNext()) {
               LeveledRegion<?> region = (LeveledRegion)var3.next();
               region.onDimensionClear(this);
            }
         }
      }

      this.popRenderPause(true, true);
      this.popWriterPause();
      if (this.currentMapLock != null) {
         if (this.mapLockToRelease != null) {
            this.releaseLocksIfNeeded();
         }

         this.mapLockToRelease = this.currentMapLock;
         this.mapLockChannelToClose = this.currentMapLockChannel;
         this.releaseLocksIfNeeded();
      }

      this.state = 2;
      WorldMap.LOGGER.info("World map force-cleaned!");
   }

   public boolean isMapWorldUsable() {
      return this.mapWorldUsable;
   }

   private Object getAutoIdBase(class_638 world) {
      return this.hasServerLevelId() ? WorldMapClientWorldDataHelper.getCurrentWorldData().serverLevelId : WorldMapClientWorldDataHelper.getWorldData(world).latestSpawn;
   }

   private Object getUsedAutoIdBase(class_638 world) {
      WorldMapClientWorldData worldData = WorldMapClientWorldDataHelper.getWorldData(world);
      return this.hasServerLevelId() ? WorldMapClientWorldDataHelper.getCurrentWorldData().usedServerLevelId : worldData.usedSpawn;
   }

   private void setUsedAutoIdBase(class_638 world, Object autoIdBase) {
      WorldMapClientWorldData worldData = WorldMapClientWorldDataHelper.getWorldData(world);
      if (this.hasServerLevelId()) {
         WorldMapClientWorldDataHelper.getCurrentWorldData().usedServerLevelId = (Integer)autoIdBase;
      } else {
         worldData.usedSpawn = (class_2338)autoIdBase;
      }

   }

   private void removeUsedAutoIdBase(class_638 world) {
      WorldMapClientWorldData worldData = WorldMapClientWorldDataHelper.getWorldData(world);
      if (this.hasServerLevelId()) {
         WorldMapClientWorldDataHelper.getCurrentWorldData().usedServerLevelId = null;
      } else {
         worldData.usedSpawn = null;
      }

   }

   private boolean hasServerLevelId() {
      WorldMapClientWorldData worldData = WorldMapClientWorldDataHelper.getCurrentWorldData();
      if (worldData == null) {
         return false;
      } else {
         return worldData.serverLevelId != null && !this.mapWorld.isIgnoreServerLevelId();
      }
   }

   public boolean isEqual(String worldId, String dimId, String mwId) {
      return worldId.equals(this.currentWorldId) && dimId.equals(this.currentDimId) && (mwId == this.currentMWId || mwId != null && mwId.equals(this.currentMWId));
   }

   public boolean isFinished() {
      return this.state == 3;
   }

   public MultiTextureRenderTypeRendererProvider getMultiTextureRenderTypeRenderers() {
      return this.multiTextureRenderTypeRenderers;
   }

   public CustomVertexConsumers getCvc() {
      return this.cvc;
   }

   public boolean isCurrentMapLocked() {
      return this.currentMapLock == null;
   }

   private void releaseLocksIfNeeded() {
      if (this.mapLockToRelease != null) {
         int lockAttempts = 10;

         while(lockAttempts-- > 0) {
            try {
               if (this.mapLockToRelease.isValid()) {
                  this.mapLockToRelease.release();
               }

               this.mapLockChannelToClose.close();
               break;
            } catch (Exception var4) {
               WorldMap.LOGGER.error("Failed attempt to release the lock for the world map! Retrying in 50 ms... " + lockAttempts, var4);

               try {
                  Thread.sleep(50L);
               } catch (InterruptedException var3) {
               }
            }
         }

         this.mapLockToRelease = null;
         this.mapLockChannelToClose = null;
      }

   }

   private int getCaveLayer(int caveStart) {
      return caveStart != Integer.MAX_VALUE && caveStart != Integer.MIN_VALUE ? caveStart >> 4 : caveStart;
   }

   public int getCurrentCaveLayer() {
      return this.currentCaveLayer;
   }

   public BlockStateShortShapeCache getBlockStateShortShapeCache() {
      return this.blockStateShortShapeCache;
   }

   public BlockTintProvider getWorldBlockTintProvider() {
      return this.worldBlockTintProvider;
   }

   public HighlighterRegistry getHighlighterRegistry() {
      return this.highlighterRegistry;
   }

   public MapRegionHighlightsPreparer getMapRegionHighlightsPreparer() {
      return this.mapRegionHighlightsPreparer;
   }

   public MessageBox getMessageBox() {
      return this.messageBox;
   }

   public MessageBoxRenderer getMessageBoxRenderer() {
      return this.messageBoxRenderer;
   }

   public class_2378<class_2248> getWorldBlockRegistry() {
      return this.worldBlockRegistry;
   }

   public class_7225<class_2248> getWorldBlockLookup() {
      return this.worldBlockLookup;
   }

   public boolean isConsideringNetherFairPlay() {
      return this.consideringNetherFairPlayMessage;
   }

   public void setConsideringNetherFairPlayMessage(boolean consideringNetherFairPlay) {
      this.consideringNetherFairPlayMessage = consideringNetherFairPlay;
   }

   public BiomeColorCalculator getBiomeColorCalculator() {
      return this.biomeColorCalculator;
   }

   public ClientSyncedTrackedPlayerManager getClientSyncedTrackedPlayerManager() {
      return this.clientSyncedTrackedPlayerManager;
   }

   public boolean serverHasMod() {
      WorldMapClientWorldData worldData = WorldMapClientWorldDataHelper.getCurrentWorldData();
      return worldData != null && worldData.serverLevelId != null;
   }

   public void setServerModNetworkVersion(int networkVersion) {
      WorldMapClientWorldData worldData = WorldMapClientWorldDataHelper.getCurrentWorldData();
      if (worldData != null) {
         worldData.setServerModNetworkVersion(networkVersion);
      }
   }

   public int getServerModNetworkVersion() {
      WorldMapClientWorldData worldData = WorldMapClientWorldDataHelper.getCurrentWorldData();
      return worldData == null ? 0 : worldData.getServerModNetworkVersion();
   }

   public class_2378<class_2874> getWorldDimensionTypeRegistry() {
      return this.worldDimensionTypeRegistry;
   }

   private void checkFootstepsReset(class_1937 oldWorld, class_1937 newWorld) {
      class_5321<class_1937> oldDimId = oldWorld == null ? null : oldWorld.method_27983();
      class_5321<class_1937> newDimId = newWorld == null ? null : newWorld.method_27983();
      if (oldDimId != newDimId) {
         this.footprints.clear();
      }

   }

   private void fixRootFolder(String mainId, class_634 connection) {
      for(int format = 4; format >= 1; --format) {
         this.fixRootFolder(mainId, this.getMainId(format, connection));
      }

   }

   private void fixRootFolder(String mainId, String oldMainId) {
      if (!mainId.equals(oldMainId)) {
         Path oldFolder;
         try {
            oldFolder = WorldMap.saveFolder.toPath().resolve(oldMainId);
         } catch (InvalidPathException var7) {
            return;
         }

         if (Files.exists(oldFolder, new LinkOption[0])) {
            Path fixedFolder = WorldMap.saveFolder.toPath().resolve(mainId);
            if (!Files.exists(fixedFolder, new LinkOption[0])) {
               try {
                  Files.move(oldFolder, fixedFolder);
               } catch (IOException var6) {
                  throw new RuntimeException("failed to auto-restore old world map folder", var6);
               }
            }
         }
      }

   }

   public boolean fairplayMessageWasReceived() {
      return this.fairplayMessageReceived;
   }

   public void setFairplayMessageReceived(boolean fairplayMessageReceived) {
      this.fairplayMessageReceived = fairplayMessageReceived;
   }

   public void updateMapItem() {
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      String mapItemString = ((String)configManager.getEffective(WorldMapProfiledConfigOptions.MAP_ITEM)).trim();
      if (!mapItemString.isEmpty() && !mapItemString.equals("-")) {
         class_2960 mapItemRL;
         try {
            mapItemRL = class_2960.method_60654(mapItemString);
         } catch (class_151 var5) {
            WorldMap.LOGGER.error("Tried setting the full screen map required item to a misformatted ID: {}, Error: {}", mapItemString, var5.getMessage());
            this.mapItem = null;
            return;
         }

         this.mapItem = (class_1792)class_7923.field_41178.method_10223(mapItemRL);
         if (this.mapItem == class_1802.field_8162) {
            this.mapItem = null;
            WorldMap.LOGGER.error("Tried setting the full screen map required item to an invalid ID: {}", mapItemString);
         } else {
            WorldMap.LOGGER.info("Fullscreen map required item set to: {}", mapItemString);
         }
      } else {
         this.mapItem = null;
         WorldMap.LOGGER.info("Fullscreen map required item set to nothing.");
      }
   }

   public class_1792 getMapItem() {
      return this.mapItem;
   }
}
