package xaero.map.file;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import net.minecraft.class_156;
import net.minecraft.class_1959;
import net.minecraft.class_1972;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2378;
import net.minecraft.class_2487;
import net.minecraft.class_2507;
import net.minecraft.class_2512;
import net.minecraft.class_2680;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3611;
import net.minecraft.class_5321;
import net.minecraft.class_7225;
import net.minecraft.class_7924;
import org.apache.logging.log4j.Logger;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.common.config.Config;
import xaero.lib.common.config.single.SingleConfigManager;
import xaero.lib.common.util.IOUtils;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.biome.BiomeGetter;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.config.primary.option.WorldMapPrimaryClientConfigOptions;
import xaero.map.config.util.WorldMapClientConfigUtils;
import xaero.map.file.export.PNGExportResult;
import xaero.map.file.export.PNGExporter;
import xaero.map.file.worldsave.WorldDataHandler;
import xaero.map.gui.ExportScreen;
import xaero.map.gui.MapTileSelection;
import xaero.map.region.BranchLeveledRegion;
import xaero.map.region.LayeredRegionManager;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapBlock;
import xaero.map.region.MapLayer;
import xaero.map.region.MapRegion;
import xaero.map.region.MapTile;
import xaero.map.region.MapTileChunk;
import xaero.map.region.Overlay;
import xaero.map.region.OverlayBuilder;
import xaero.map.region.OverlayManager;
import xaero.map.region.state.UnknownBlockState;
import xaero.map.task.MapRunnerTask;
import xaero.map.world.MapDimension;

public class MapSaveLoad {
   private static final int currentSaveMajorVersion = 6;
   private static final int currentSaveMinorVersion = 8;
   public static final int SAVE_TIME = 60000;
   public static final int currentCacheSaveMajorVersion = 1;
   public static final int currentCacheSaveMinorVersion = 24;
   private ArrayList<MapRegion> toSave = new ArrayList();
   private ArrayList<MapRegion> toLoad = new ArrayList();
   private ArrayList<BranchLeveledRegion> toLoadBranchCache = new ArrayList();
   private ArrayList<File> cacheToConvertFromTemp = new ArrayList();
   private LeveledRegion<?> nextToLoadByViewing;
   private boolean regionDetectionComplete;
   private Path lastRealmOwnerPath;
   public boolean loadingFiles;
   private OverlayBuilder overlayBuilder;
   private PNGExporter pngExporter;
   private OldFormatSupport oldFormatSupport;
   private HashMap<class_2680, Integer> regionSavePalette;
   private ArrayList<class_2680> regionLoadPalette;
   private HashMap<class_5321<class_1959>, Integer> regionSaveBiomePalette;
   private ArrayList<class_5321<class_1959>> regionLoadBiomePalette;
   private List<MapDimension> workingDimList;
   public boolean saveAll;
   private MapProcessor mapProcessor;
   public int mainTextureLevel;
   private BlockStateShortShapeCache blockStateShortShapeCache;
   private boolean exporting;

   public MapSaveLoad(OverlayManager overlayManager, PNGExporter pngExporter, OldFormatSupport oldFormatSupport, BlockStateShortShapeCache blockStateShortShapeCache) {
      this.overlayBuilder = new OverlayBuilder(overlayManager);
      this.pngExporter = pngExporter;
      this.oldFormatSupport = oldFormatSupport;
      this.regionSavePalette = new HashMap();
      this.regionLoadPalette = new ArrayList();
      this.regionSaveBiomePalette = new HashMap();
      this.regionLoadBiomePalette = new ArrayList();
      this.workingDimList = new ArrayList();
      this.blockStateShortShapeCache = blockStateShortShapeCache;
   }

   public void setMapProcessor(MapProcessor mapProcessor) {
      this.mapProcessor = mapProcessor;
   }

   public boolean exportPNG(ExportScreen destScreen, MapTileSelection selection) {
      if (this.exporting) {
         return false;
      } else {
         this.exporting = true;
         WorldMap.mapRunner.addTask(new MapRunnerTask() {
            public void run(MapProcessor mapProcessor) {
               class_310.method_1551().method_20493(() -> {
                  try {
                     PNGExportResult result = MapSaveLoad.this.pngExporter.export(mapProcessor, mapProcessor.worldBiomeRegistry, mapProcessor.getWorldDimensionTypeRegistry(), selection, MapSaveLoad.this.oldFormatSupport);
                     WorldMap.LOGGER.info(result.getMessage().getString());
                     if (destScreen != null) {
                        destScreen.onExportDone(result);
                     }

                     if (result.getFolderToOpen() != null && Files.exists(result.getFolderToOpen(), new LinkOption[0])) {
                        class_156.method_668().method_672(result.getFolderToOpen().toFile());
                     }
                  } catch (Throwable var6) {
                     WorldMap.LOGGER.error("Failed to export PNG with exception!", var6);
                     WorldMap.crashHandler.setCrashedBy(var6);
                  }

                  MapSaveLoad.this.exporting = false;
                  class_310.method_1551().method_1507(destScreen);
               });

               while(MapSaveLoad.this.exporting) {
                  try {
                     Thread.sleep(100L);
                  } catch (InterruptedException var3) {
                  }
               }

            }
         });
         return true;
      }
   }

   private File getSecondaryFile(String extension, File realFile) {
      if (realFile == null) {
         return null;
      } else {
         String p = realFile.getPath();
         if (p.endsWith(".outdated")) {
            p = p.substring(0, p.length() - ".outdated".length());
         }

         String var10002 = p.substring(0, p.lastIndexOf("."));
         return new File(var10002 + extension);
      }
   }

   public File getTempFile(File realFile) {
      return this.getSecondaryFile(".zip.temp", realFile);
   }

   private Path getCacheFolder(Path subFolder) {
      return subFolder != null ? subFolder.resolve("cache_" + this.mapProcessor.getGlobalVersion()) : null;
   }

   public File getCacheFile(MapRegionInfo region, int caveLayer, boolean checkOutdated, boolean requestCache) throws IOException {
      Path subFolder = this.getMWSubFolder(region.getWorldId(), region.getDimId(), region.getMwId());
      Path layerFolder = this.getCaveLayerFolder(caveLayer, subFolder);
      Path latestCacheFolder = this.getCacheFolder(layerFolder);
      if (latestCacheFolder == null) {
         return null;
      } else {
         if (!Files.exists(latestCacheFolder, new LinkOption[0])) {
            Files.createDirectories(latestCacheFolder);
         }

         int var10001 = region.getRegionX();
         Path cacheFile = latestCacheFolder.resolve(var10001 + "_" + region.getRegionZ() + ".xwmc");
         if (checkOutdated && !Files.exists(cacheFile, new LinkOption[0])) {
            if (requestCache) {
               region.setShouldCache(true, "cache file");
            }

            Path outdatedCacheFile = cacheFile.resolveSibling(cacheFile.getFileName().toString() + ".outdated");
            return Files.exists(outdatedCacheFile, new LinkOption[0]) ? outdatedCacheFile.toFile() : cacheFile.toFile();
         } else {
            return cacheFile.toFile();
         }
      }
   }

   public File getFile(MapRegion region) {
      if (region.getWorldId() == null) {
         return null;
      } else {
         File detectedFile = region.getRegionFile();
         boolean normalMapData = region.isNormalMapData();
         if (!normalMapData) {
            if (detectedFile != null) {
               return detectedFile;
            } else {
               Path var10000 = this.mapProcessor.getWorldDataHandler().getWorldDir().resolve("region");
               int var10001 = region.getRegionX();
               return var10000.resolve("r." + var10001 + "." + region.getRegionZ() + ".mca").toFile();
            }
         } else {
            return this.getNormalFile(region);
         }
      }
   }

   public File getNormalFile(MapRegion region) {
      if (region.getWorldId() == null) {
         return null;
      } else {
         File detectedFile = region.isNormalMapData() ? region.getRegionFile() : null;
         MapProcessor var10000 = this.mapProcessor;
         boolean realms = MapProcessor.isWorldRealms(region.getWorldId());
         String mwId = region.isNormalMapData() ? region.getMwId() : "cm$converted";
         Path mainFolder = this.getMainFolder(region.getWorldId(), region.getDimId());
         Path subFolder = this.getMWSubFolder(region.getWorldId(), mainFolder, mwId);
         Path layerFolder = subFolder;
         if (region.getCaveLayer() != Integer.MAX_VALUE) {
            layerFolder = subFolder.resolve("caves").resolve(region.getCaveLayer().makeConcatWithConstants<invokedynamic>(region.getCaveLayer()));
         }

         File zipFile;
         try {
            zipFile = layerFolder.toFile();
            if (!zipFile.exists()) {
               Files.createDirectories(zipFile.toPath());
               if (realms && WorldMap.events.getLatestRealm() != null) {
                  Path ownerPath = mainFolder.resolve(WorldMap.events.getLatestRealm().field_22604 + ".owner");
                  if (!ownerPath.equals(this.lastRealmOwnerPath)) {
                     if (!Files.exists(ownerPath, new LinkOption[0])) {
                        Files.createFile(ownerPath);
                     }

                     this.lastRealmOwnerPath = ownerPath;
                  }
               }
            }
         } catch (IOException var10) {
            WorldMap.LOGGER.error("suppressed exception", var10);
         }

         int var10001;
         if (detectedFile != null && detectedFile.getName().endsWith(".xaero")) {
            var10001 = region.getRegionX();
            zipFile = layerFolder.resolve(var10001 + "_" + region.getRegionZ() + ".zip").toFile();
            if (detectedFile.exists() && !zipFile.exists()) {
               this.xaeroToZip(detectedFile);
            }

            region.setRegionFile(zipFile);
            return zipFile;
         } else {
            File var11;
            if (detectedFile == null) {
               var10001 = region.getRegionX();
               var11 = layerFolder.resolve(var10001 + "_" + region.getRegionZ() + ".zip").toFile();
            } else {
               var11 = detectedFile;
            }

            return var11;
         }
      }
   }

   public static Path getRootFolder(String world) {
      return world == null ? null : WorldMap.saveFolder.toPath().resolve(world);
   }

   public Path getMainFolder(String world, String dim) {
      return world == null ? null : WorldMap.saveFolder.toPath().resolve(world).resolve(dim);
   }

   Path getMWSubFolder(String world, Path mainFolder, String mw) {
      if (world == null) {
         return null;
      } else {
         return mw == null ? mainFolder : mainFolder.resolve(mw);
      }
   }

   public Path getCaveLayerFolder(int caveLayer, Path subFolder) {
      Path layerFolder = subFolder;
      if (caveLayer != Integer.MAX_VALUE) {
         layerFolder = subFolder.resolve("caves").resolve(caveLayer.makeConcatWithConstants<invokedynamic>(caveLayer));
      }

      return layerFolder;
   }

   public Path getMWSubFolder(String world, String dim, String mw) {
      return world == null ? null : this.getMWSubFolder(world, this.getMainFolder(world, dim), mw);
   }

   public Path getOldFolder(String oldUnfixedMainId, String dim) {
      return oldUnfixedMainId == null ? null : WorldMap.saveFolder.toPath().resolve(oldUnfixedMainId + "_" + dim);
   }

   private void xaeroToZip(File xaero) {
      Path var10000 = xaero.toPath().getParent();
      String var10001 = xaero.getName();
      String var10003 = xaero.getName();
      File zipFile = var10000.resolve(var10001.substring(0, var10003.lastIndexOf(46)) + ".zip").toFile();

      try {
         BufferedInputStream in = new BufferedInputStream(new FileInputStream(xaero), 1024);
         ZipOutputStream zipOutput = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
         ZipEntry e = new ZipEntry("region.xaero");
         zipOutput.putNextEntry(e);
         byte[] bytes = new byte[1024];

         int got;
         while((got = in.read(bytes)) > 0) {
            zipOutput.write(bytes, 0, got);
         }

         zipOutput.closeEntry();
         zipOutput.flush();
         zipOutput.close();
         in.close();
         Files.deleteIfExists(xaero.toPath());
      } catch (IOException var8) {
         WorldMap.LOGGER.error("suppressed exception", var8);
      }
   }

   public void detectRegions(int attempts) {
      MapDimension mapDimension = this.mapProcessor.getMapWorld().getCurrentDimension();
      mapDimension.preDetection();
      String worldId = this.mapProcessor.getCurrentWorldId();
      if (worldId != null && !this.mapProcessor.isCurrentMapLocked()) {
         String dimId = this.mapProcessor.getCurrentDimId();
         String mwId = this.mapProcessor.getCurrentMWId();
         boolean usingNormalMapData = !mapDimension.isUsingWorldSave();
         Path mapFolder = this.getMWSubFolder(worldId, dimId, mwId);
         boolean mapFolderExists = mapFolder.toFile().exists();
         String multiplayerMapRegex = "^(-?\\d+)_(-?\\d+)\\.(zip|xaero)$";
         MapLayer mainLayer = mapDimension.getLayeredMapRegions().getLayer(Integer.MAX_VALUE);
         Path cavesFolder;
         if (usingNormalMapData) {
            if (mapFolderExists) {
               Objects.requireNonNull(mainLayer);
               this.detectRegionsFromFiles(mapDimension, worldId, dimId, mwId, mapFolder, multiplayerMapRegex, 1, 2, 0, 20, mainLayer::addRegionDetection);
            }
         } else {
            cavesFolder = this.mapProcessor.getWorldDataHandler().getWorldDir();
            if (cavesFolder == null) {
               return;
            }

            Path worldFolder = cavesFolder.resolve("region");
            if (!worldFolder.toFile().exists()) {
               return;
            }

            Objects.requireNonNull(mapDimension);
            this.detectRegionsFromFiles(mapDimension, worldId, dimId, mwId, worldFolder, "^r\\.(-{0,1}[0-9]+)\\.(-{0,1}[0-9]+)\\.mc[ar]$", 1, 2, 8192, 20, mapDimension::addWorldSaveRegionDetection);
         }

         if (mapFolderExists) {
            cavesFolder = mapFolder.resolve("caves");

            try {
               if (!Files.exists(cavesFolder, new LinkOption[0])) {
                  Files.createDirectories(cavesFolder);
               }

               Stream cavesFolderStream = Files.list(cavesFolder);

               try {
                  cavesFolderStream.forEach((layerFolder) -> {
                     if (Files.isDirectory(layerFolder, new LinkOption[0])) {
                        String folderName = layerFolder.getFileName().toString();

                        try {
                           int layerInt = Integer.parseInt(folderName);
                           MapLayer layer = mapDimension.getLayeredMapRegions().getLayer(layerInt);
                           if (usingNormalMapData) {
                              Objects.requireNonNull(layer);
                              this.detectRegionsFromFiles(mapDimension, worldId, dimId, mwId, layerFolder, multiplayerMapRegex, 1, 2, 0, 20, layer::addRegionDetection);
                           }
                        } catch (NumberFormatException var11) {
                        }

                     }
                  });
               } catch (Throwable var17) {
                  if (cavesFolderStream != null) {
                     try {
                        cavesFolderStream.close();
                     } catch (Throwable var16) {
                        var17.addSuppressed(var16);
                     }
                  }

                  throw var17;
               }

               if (cavesFolderStream != null) {
                  cavesFolderStream.close();
               }
            } catch (IOException var18) {
               WorldMap.LOGGER.error("IOException trying to detect map layers!");
               if (attempts > 1) {
                  --attempts;
                  WorldMap.LOGGER.error("Retrying... " + attempts);

                  try {
                     Thread.sleep(30L);
                  } catch (InterruptedException var15) {
                  }

                  this.detectRegions(attempts);
                  return;
               }

               throw new RuntimeException("Couldn't detect map layers after multiple attempts.", var18);
            }
         }

      }
   }

   public void detectRegionsFromFiles(MapDimension mapDimension, String worldId, String dimId, String mwId, Path folder, String regex, int xIndex, int zIndex, int emptySize, int attempts, Consumer<RegionDetection> detectionConsumer) {
      int total = 0;
      Pattern fileRegexPattern = Pattern.compile(regex);
      long before = System.currentTimeMillis();

      try {
         Stream<Path> files = Files.list(folder);
         Iterator<Path> iter = files.iterator();
         int globalVersion = this.mapProcessor.getGlobalVersion();

         while(true) {
            if (this.mapProcessor.isFinalizing() || !iter.hasNext()) {
               files.close();
               break;
            }

            Path file = (Path)iter.next();
            String regionName = file.getFileName().toString();
            Matcher matcher = fileRegexPattern.matcher(regionName);
            if (matcher.matches()) {
               int x = Integer.parseInt(matcher.group(xIndex));
               int z = Integer.parseInt(matcher.group(zIndex));
               RegionDetection regionDetection = new RegionDetection(worldId, dimId, mwId, x, z, file.toFile(), globalVersion, true);
               detectionConsumer.accept(regionDetection);
               ++total;
            }
         }
      } catch (IOException var26) {
         WorldMap.LOGGER.error("IOException trying to detect map files!");
         if (attempts > 1) {
            --attempts;
            WorldMap.LOGGER.error("Retrying... " + attempts);

            try {
               Thread.sleep(30L);
            } catch (InterruptedException var25) {
            }

            this.detectRegionsFromFiles(mapDimension, worldId, dimId, mwId, folder, regex, xIndex, zIndex, emptySize, attempts, detectionConsumer);
            return;
         }

         throw new RuntimeException("Couldn't detect map files after multiple attempts.", var26);
      }

      if (WorldMapClientConfigUtils.getDebug()) {
         WorldMap.LOGGER.info(String.format("%d regions detected in %d ms!", total, System.currentTimeMillis() - before));
      }

   }

   private boolean saveRegion(MapRegion region, boolean debugConfig, int extraAttempts) {
      try {
         Logger var10000;
         String var10001;
         if (!region.hasHadTerrain()) {
            if (debugConfig) {
               var10000 = WorldMap.LOGGER;
               var10001 = String.valueOf(region);
               var10000.info("Save not required for highlight-only region: " + var10001 + " " + region.getWorldId() + " " + region.getDimId());
            }

            return region.countChunks() > 0;
         } else if (!region.isResaving() && !region.isNormalMapData()) {
            if (debugConfig) {
               var10000 = WorldMap.LOGGER;
               var10001 = String.valueOf(region);
               var10000.info("Save not required for world save map: " + var10001 + " " + region.getWorldId() + " " + region.getDimId());
            }

            return region.countChunks() > 0;
         } else {
            File permFile = this.getNormalFile(region);
            if (!permFile.toPath().startsWith(WorldMap.saveFolder.toPath())) {
               throw new IllegalArgumentException();
            } else {
               File file = this.getTempFile(permFile);
               if (file == null) {
                  return true;
               } else {
                  if (!file.exists()) {
                     file.createNewFile();
                  }

                  boolean hasAnything = false;
                  boolean regionWasSavedEmpty = true;
                  DataOutputStream out = null;

                  try {
                     ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
                     out = new DataOutputStream(zipOut);
                     ZipEntry e = new ZipEntry("region.xaero");
                     zipOut.putNextEntry(e);
                     int fullVersion = 393224;
                     out.write(255);
                     out.writeInt(fullVersion);
                     this.regionSavePalette.clear();
                     this.regionSaveBiomePalette.clear();
                     class_2378<class_1959> biomeRegistry = region.getBiomeRegistry();
                     int o = 0;

                     while(true) {
                        if (o >= 8) {
                           zipOut.closeEntry();
                           break;
                        }

                        for(int p = 0; p < 8; ++p) {
                           MapTileChunk chunk = region.getChunk(o, p);
                           if (chunk != null) {
                              hasAnything = true;
                              if (!chunk.includeInSave()) {
                                 if (!chunk.hasHighlightsIfUndiscovered()) {
                                    region.uncountTextureBiomes(chunk.getLeafTexture());
                                    region.setChunk(o, p, (MapTileChunk)null);
                                    synchronized(chunk) {
                                       chunk.getLeafTexture().deleteTexturesAndBuffers();
                                    }
                                 }

                                 BranchLeveledRegion parentRegion = region.getParent();
                                 if (parentRegion != null) {
                                    parentRegion.setShouldCheckForUpdatesRecursive(true);
                                 }
                              } else {
                                 out.write(o << 4 | p);
                                 boolean chunkIsEmpty = true;

                                 for(int i = 0; i < 4; ++i) {
                                    for(int j = 0; j < 4; ++j) {
                                       MapTile tile = chunk.getTile(i, j);
                                       if (tile != null && tile.isLoaded()) {
                                          chunkIsEmpty = false;

                                          for(int x = 0; x < 16; ++x) {
                                             MapBlock[] c = tile.getBlockColumn(x);

                                             for(int z = 0; z < 16; ++z) {
                                                this.savePixel(c[z], out, biomeRegistry);
                                             }
                                          }

                                          out.write(tile.getWorldInterpretationVersion());
                                          out.writeInt(tile.getWrittenCaveStart());
                                          out.write(tile.getWrittenCaveDepth());
                                       } else {
                                          out.writeInt(-1);
                                       }
                                    }
                                 }

                                 if (!chunkIsEmpty) {
                                    regionWasSavedEmpty = false;
                                 }
                              }
                           }
                        }

                        ++o;
                     }
                  } finally {
                     if (out != null) {
                        out.close();
                     }

                  }

                  if (regionWasSavedEmpty) {
                     this.safeDelete(permFile.toPath(), ".zip");
                     this.safeDelete(file.toPath(), ".temp");
                     if (debugConfig) {
                        var10000 = WorldMap.LOGGER;
                        var10001 = String.valueOf(region);
                        var10000.info("Save cancelled because the region would be saved empty: " + var10001 + " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId());
                     }

                     return hasAnything;
                  } else {
                     this.safeMoveAndReplace(file.toPath(), permFile.toPath(), ".temp", ".zip");
                     if (debugConfig) {
                        var10000 = WorldMap.LOGGER;
                        var10001 = String.valueOf(region);
                        var10000.info("Region saved: " + var10001 + " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId() + ", " + this.mapProcessor.getMapWriter().getUpdateCounter());
                     }

                     return true;
                  }
               }
            }
         }
      } catch (IOException var31) {
         WorldMap.LOGGER.error("IO exception while trying to save " + String.valueOf(region), var31);
         if (extraAttempts > 0) {
            WorldMap.LOGGER.info("Retrying...");

            try {
               Thread.sleep(20L);
            } catch (InterruptedException var28) {
            }

            return this.saveRegion(region, debugConfig, extraAttempts - 1);
         } else {
            return true;
         }
      }
   }

   private Path getBackupFolder(Path filePath, int saveVersion, int backupVersion) {
      return filePath.getParent().resolve(saveVersion + "_backup_" + backupVersion);
   }

   public void backupFile(File file, int saveVersion) throws IOException {
      if (!file.getName().endsWith(".mca") && !file.getName().endsWith(".mcr")) {
         Path filePath = file.toPath();
         int backupVersion = 0;
         Path backupFolder = this.getBackupFolder(filePath, saveVersion, backupVersion);
         String backupName = filePath.getFileName().toString();

         Path backup;
         for(backup = backupFolder.resolve(backupName); Files.exists(backup, new LinkOption[0]); backup = backupFolder.resolve(backupName)) {
            ++backupVersion;
            backupFolder = this.getBackupFolder(filePath, saveVersion, backupVersion);
         }

         if (!Files.exists(backupFolder, new LinkOption[0])) {
            Files.createDirectories(backupFolder);
         }

         Files.move(file.toPath(), backup);
         Logger var10000 = WorldMap.LOGGER;
         String var10001 = file.getPath();
         var10000.info("File " + var10001 + " backed up to " + backupFolder.toFile().getPath());
      } else {
         throw new RuntimeException("World save protected: " + String.valueOf(file));
      }
   }

   public boolean loadRegion(MapRegion region, class_7225<class_2248> blockLookup, class_2378<class_2248> blockRegistry, class_2378<class_3611> fluidRegistry, BiomeGetter biomeGetter, boolean debugConfig, int extraAttempts) {
      boolean multiplayer = region.isNormalMapData();
      int emptySize = multiplayer ? 0 : 8192;
      int minorSaveVersion = -1;
      int majorSaveVersion = 0;
      boolean versionReached = false;

      try {
         File file = this.getFile(region);
         Logger var10000;
         String var10001;
         if (region.hasHadTerrain() && file != null && file.exists() && Files.size(file.toPath()) > (long)emptySize) {
            synchronized(region) {
               region.setLoadState((byte)1);
            }

            region.setSaveExists(true);
            region.restoreBufferUpdateObjects();
            int totalChunks = 0;
            if (multiplayer) {
               this.regionLoadPalette.clear();
               this.regionLoadBiomePalette.clear();
               DataInputStream in = null;

               try {
                  ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(new FileInputStream(file), 2048));
                  in = new DataInputStream(zipIn);
                  zipIn.getNextEntry();
                  int firstByte = in.read();
                  boolean is115not114 = false;
                  if (firstByte == 255) {
                     int fullVersion = in.readInt();
                     minorSaveVersion = fullVersion & '\uffff';
                     majorSaveVersion = fullVersion >> 16 & '\uffff';
                     if (majorSaveVersion == 2 && minorSaveVersion >= 5) {
                        is115not114 = in.read() == 1;
                     }

                     if (8 < minorSaveVersion || 6 < majorSaveVersion) {
                        zipIn.closeEntry();
                        in.close();
                        WorldMap.LOGGER.info("Trying to load a newer region " + String.valueOf(region) + " save using an older version of Xaero's World Map!");
                        this.backupFile(file, fullVersion);
                        region.setSaveExists((Boolean)null);
                        boolean var56 = false;
                        return var56;
                     }

                     firstByte = -1;
                  }

                  versionReached = true;
                  int o;
                  int p;
                  MapTileChunk chunk;
                  synchronized(region.getLevel() == 3 ? region : region.getParent()) {
                     synchronized(region) {
                        o = 0;

                        while(true) {
                           if (o >= 8) {
                              break;
                           }

                           for(p = 0; p < 8; ++p) {
                              chunk = region.getChunk(o, p);
                              if (chunk != null) {
                                 chunk.setLoadState((byte)1);
                              }
                           }

                           ++o;
                        }
                     }
                  }

                  class_2378 biomeRegistry = region.getBiomeRegistry();

                  while(true) {
                     int chunkCoords = firstByte == -1 ? in.read() : firstByte;
                     if (chunkCoords == -1) {
                        zipIn.closeEntry();
                        break;
                     }

                     firstByte = -1;
                     o = chunkCoords >> 4;
                     p = chunkCoords & 15;
                     chunk = region.getChunk(o, p);
                     if (chunk == null) {
                        region.setChunk(o, p, chunk = new MapTileChunk(region, region.getRegionX() * 8 + o, region.getRegionZ() * 8 + p));
                     } else if (chunk.getLoadState() >= 2) {
                        throw new Exception("Map data for region " + String.valueOf(region) + " is probably corrupt! Has the same map tile chunk saved twice.");
                     }

                     if (region.isMetaLoaded()) {
                        chunk.getLeafTexture().setBufferedTextureVersion(region.getAndResetCachedTextureVersion(o, p));
                     }

                     chunk.resetHeights();

                     for(int i = 0; i < 4; ++i) {
                        for(int j = 0; j < 4; ++j) {
                           Integer nextTile = in.readInt();
                           if (nextTile != -1) {
                              MapTile tile = this.mapProcessor.getTilePool().get(this.mapProcessor.getCurrentDimension(), chunk.getX() * 4 + i, chunk.getZ() * 4 + j);

                              for(int x = 0; x < 16; ++x) {
                                 MapBlock[] c = tile.getBlockColumn(x);

                                 for(int z = 0; z < 16; ++z) {
                                    if (c[z] == null) {
                                       c[z] = new MapBlock();
                                    } else {
                                       c[z].prepareForWriting(0);
                                    }

                                    this.loadPixel(nextTile, c[z], in, minorSaveVersion, majorSaveVersion, is115not114, blockLookup, biomeGetter, biomeRegistry);
                                    nextTile = null;
                                 }
                              }

                              if (minorSaveVersion >= 4) {
                                 tile.setWorldInterpretationVersion(in.read());
                              }

                              if (minorSaveVersion >= 6) {
                                 tile.setWrittenCave(in.readInt(), minorSaveVersion >= 7 ? in.read() : 32);
                              }

                              chunk.setTile(i, j, tile, this.blockStateShortShapeCache);
                              tile.setLoaded(true);
                           }
                        }
                     }

                     if (!chunk.includeInSave()) {
                        if (!chunk.hasHighlightsIfUndiscovered()) {
                           region.uncountTextureBiomes(chunk.getLeafTexture());
                           region.setChunk(o, p, (MapTileChunk)null);
                           chunk.getLeafTexture().deleteTexturesAndBuffers();
                           chunk = null;
                        }
                     } else {
                        region.pushWriterPause();
                        ++totalChunks;
                        chunk.setToUpdateBuffers(true);
                        chunk.setLoadState((byte)2);
                        region.popWriterPause();
                     }
                  }
               } finally {
                  if (in != null) {
                     in.close();
                  }

               }

               if (totalChunks > 0) {
                  if (debugConfig) {
                     var10000 = WorldMap.LOGGER;
                     var10001 = String.valueOf(region);
                     var10000.info("Region loaded: " + var10001 + " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId() + ", " + majorSaveVersion + " " + minorSaveVersion);
                  }

                  return true;
               } else {
                  region.setSaveExists((Boolean)null);
                  this.safeDelete(file.toPath(), ".zip");
                  if (debugConfig) {
                     var10000 = WorldMap.LOGGER;
                     var10001 = String.valueOf(region);
                     var10000.info("Cancelled loading an empty region: " + var10001 + " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId() + ", " + majorSaveVersion + " " + minorSaveVersion);
                  }

                  return false;
               }
            } else {
               int[] chunkCount = new int[1];
               WorldDataHandler.Result buildResult = this.mapProcessor.getWorldDataHandler().buildRegion(region, blockLookup, blockRegistry, fluidRegistry, true, chunkCount);
               if (buildResult == WorldDataHandler.Result.CANCEL) {
                  if (region.hasHadTerrain()) {
                     RegionDetection restoredDetection = new RegionDetection(region.getWorldId(), region.getDimId(), region.getMwId(), region.getRegionX(), region.getRegionZ(), region.getRegionFile(), this.mapProcessor.getGlobalVersion(), true);
                     restoredDetection.transferInfoFrom(region);
                     region.getDim().getLayeredMapRegions().getLayer(region.getCaveLayer()).addRegionDetection(restoredDetection);
                  }

                  this.mapProcessor.removeMapRegion(region);
                  var10000 = WorldMap.LOGGER;
                  var10001 = String.valueOf(region);
                  var10000.info("Region cancelled from world save: " + var10001 + " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId());
                  return false;
               } else {
                  region.setRegionFile(file);
                  boolean result = buildResult == WorldDataHandler.Result.SUCCESS && chunkCount[0] > 0;
                  if (!result) {
                     region.setSaveExists((Boolean)null);
                     if (debugConfig) {
                        var10000 = WorldMap.LOGGER;
                        var10001 = String.valueOf(region);
                        var10000.info("Region failed to load from world save: " + var10001 + " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId());
                     }
                  } else if (debugConfig) {
                     var10000 = WorldMap.LOGGER;
                     var10001 = String.valueOf(region);
                     var10000.info("Region loaded from world save: " + var10001 + " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId());
                  }

                  return result;
               }
            }
         } else {
            if (region.getLoadState() == 4 || region.hasHadTerrain()) {
               region.setSaveExists((Boolean)null);
            }

            if (region.hasHadTerrain()) {
               return false;
            } else {
               synchronized(region) {
                  region.setLoadState((byte)1);
               }

               region.restoreBufferUpdateObjects();
               if (debugConfig) {
                  var10000 = WorldMap.LOGGER;
                  var10001 = String.valueOf(region);
                  var10000.info("Highlight region fake-loaded: " + var10001 + " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId());
               }

               return true;
            }
         }
      } catch (IOException var49) {
         WorldMap.LOGGER.error("IO exception while trying to load " + String.valueOf(region), var49);
         if (extraAttempts > 0) {
            synchronized(region) {
               region.setLoadState((byte)4);
            }

            WorldMap.LOGGER.info("Retrying...");

            try {
               Thread.sleep(20L);
            } catch (InterruptedException var42) {
            }

            return this.loadRegion(region, blockLookup, blockRegistry, fluidRegistry, biomeGetter, debugConfig, extraAttempts - 1);
         } else {
            region.setSaveExists((Boolean)null);
            return false;
         }
      } catch (Throwable var50) {
         region.setSaveExists((Boolean)null);
         WorldMap.LOGGER.error("Region failed to load: " + String.valueOf(region) + (versionReached ? " " + majorSaveVersion + " " + minorSaveVersion : ""), var50);
         return false;
      }
   }

   public boolean beingSaved(MapDimension dim, int regX, int regZ) {
      for(int i = 0; i < this.toSave.size(); ++i) {
         MapRegion r = (MapRegion)this.toSave.get(i);
         if (r != null && r.getDim() == dim && r.getRegionX() == regX && r.getRegionZ() == regZ) {
            return true;
         }
      }

      return false;
   }

   public void requestLoad(MapRegion region, String reason) {
      this.requestLoad(region, reason, true);
   }

   public void requestLoad(MapRegion region, String reason, boolean prioritize) {
      this.addToLoad(region, reason, prioritize);
   }

   public void requestBranchCache(BranchLeveledRegion region, String reason) {
      this.requestBranchCache(region, reason, true);
      if (reason != null) {
         if (WorldMapClientConfigUtils.getDebug()) {
            Logger var10000 = WorldMap.LOGGER;
            String var10001 = String.valueOf(region);
            var10000.info("Requesting branch load for: " + var10001 + ", " + reason);
         }

      }
   }

   public void requestBranchCache(BranchLeveledRegion region, String reason, boolean prioritize) {
      synchronized(this.toLoadBranchCache) {
         if (prioritize) {
            this.toLoadBranchCache.remove(region);
            this.toLoadBranchCache.add(0, region);
         } else if (!this.toLoadBranchCache.contains(region)) {
            this.toLoadBranchCache.add(region);
         }

      }
   }

   public void addToLoad(MapRegion region, String reason, boolean prioritize) {
      synchronized(this.toLoad) {
         Logger var10000;
         String var10001;
         if (prioritize) {
            region.setReloadHasBeenRequested(true, reason);
            this.toLoad.remove(region);
            this.toLoad.add(0, region);
            if (WorldMapClientConfigUtils.getDebug() && reason != null) {
               var10000 = WorldMap.LOGGER;
               var10001 = String.valueOf(region);
               var10000.info("Requesting load for: " + var10001 + " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId() + ", " + reason);
            }
         } else if (!this.loadingFiles && !this.toLoad.contains(region)) {
            region.setReloadHasBeenRequested(true, reason);
            this.toLoad.add(region);
            if (WorldMapClientConfigUtils.getDebug() && reason != null) {
               var10000 = WorldMap.LOGGER;
               var10001 = String.valueOf(region);
               var10000.info("Requesting load for: " + var10001 + " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId() + ", " + reason);
            }
         }
      }

      this.mapProcessor.getMapRegionHighlightsPreparer().prepare(region, false);
   }

   public void removeToLoad(MapRegion region) {
      synchronized(this.toLoad) {
         this.toLoad.remove(region);
      }
   }

   public void clearToLoad() {
      synchronized(this.toLoad) {
         this.toLoad.clear();
      }

      synchronized(this.toLoadBranchCache) {
         this.toLoadBranchCache.clear();
      }
   }

   public int getSizeOfToLoad() {
      return this.toLoad.size();
   }

   public boolean saveExists(MapRegion region) {
      if (region.getSaveExists() != null) {
         return region.getSaveExists();
      } else {
         boolean result = true;
         File file = this.getFile(region);
         if (file == null || !file.exists()) {
            result = false;
         }

         region.setSaveExists(result);
         return result;
      }
   }

   public void updateSave(LeveledRegion<?> leveledRegion, long currentTime, int currentLayer) {
      if (leveledRegion.getLevel() == 0) {
         MapRegion region = (MapRegion)leveledRegion;
         int saveTime = 60000;
         if (region.getCaveLayer() != currentLayer) {
            saveTime /= 100;
         }

         if (region.getLoadState() == 2 && region.isBeingWritten() && currentTime - region.getLastSaveTime() >= (long)saveTime && !this.beingSaved(region.getDim(), region.getRegionX(), region.getRegionZ())) {
            this.toSave.add(region);
            region.setSaveExists(true);
            region.setLastSaveTime(currentTime);
         }
      } else {
         BranchLeveledRegion region = (BranchLeveledRegion)leveledRegion;
         if (region.eligibleForSaving(currentTime)) {
            region.startDownloadingTexturesForCache(this.mapProcessor);
         }
      }

   }

   public void run(class_7225<class_2248> blockLookup, class_2378<class_2248> blockRegistry, class_2378<class_3611> fluidRegistry, BiomeGetter biomeGetter, class_2378<class_1959> biomeRegistry) throws Exception {
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      SingleConfigManager<Config> primaryConfigManager = configManager.getPrimaryConfigManager();
      boolean debugConfig = (Boolean)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.DEBUG);
      int globalVersion = this.mapProcessor.getGlobalVersion();
      boolean skipCaching;
      if (!this.toLoad.isEmpty()) {
         boolean loaded = false;
         this.mapProcessor.pushIsLoading();
         this.loadingFiles = true;
         boolean reloadEverything = (Boolean)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.RELOAD_VIEWED);
         int limit = this.toLoad.size();

         while(limit > 0 && !this.mapProcessor.isWaitingForWorldUpdate() && !loaded && !this.toLoad.isEmpty()) {
            --limit;
            MapRegion region;
            synchronized(this.toLoad) {
               if (this.toLoad.isEmpty()) {
                  break;
               }

               region = (MapRegion)this.toLoad.get(0);
            }

            if (region.hasHadTerrain() && region.getCacheFile() == null && !region.hasLookedForCache()) {
               File potentialCacheFile = this.getCacheFile(region, region.getCaveLayer(), true, true);
               if (potentialCacheFile.exists()) {
                  region.setCacheFile(potentialCacheFile);
               }

               region.setLookedForCache(true);
            }

            int globalRegionCacheHashCode = WorldMap.settings.getRegionCacheHashCode();
            int globalReloadVersion = (Integer)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.RELOAD_VIEWED_VERSION);
            int globalCaveDepth = (Integer)configManager.getEffective(WorldMapProfiledConfigOptions.CAVE_MODE_DEPTH);
            synchronized(region) {
               skipCaching = region.getLoadState() == 0 || region.getLoadState() == 4;
               if (skipCaching) {
                  if (region.hasVersion() && region.getVersion() != globalVersion || !region.hasVersion() && region.getInitialVersion() != globalVersion || region.getLoadState() == 4 && reloadEverything && region.getReloadVersion() != globalReloadVersion || (region.getLoadState() == 4 || region.isMetaLoaded() && this.mainTextureLevel != region.getLevel()) && (globalRegionCacheHashCode != region.getCacheHashCode() || region.caveStartOutdated(region.getUpToDateCaveStart(), globalCaveDepth)) || region.getDim().getFullReloader() != null && region.getDim().getFullReloader().isPartOfReload(region)) {
                     region.setShouldCache(true, "loading");
                  }

                  region.setVersion(globalVersion);
               }
            }

            if (skipCaching) {
               synchronized(region) {
                  region.setAllCachePrepared(false);
               }

               boolean cacheOnlyMode = region.getDim().getMapWorld().isCacheOnlyMode();
               boolean fromNothing = region.getLoadState() == 0;
               boolean hasSomething = false;
               boolean justMetaData = false;
               boolean[] leafShouldAffectBranchesDest = new boolean[1];
               int targetHighlightsHash = region.getTargetHighlightsHash();
               boolean[] metaLoadedDest = new boolean[1];
               boolean[][] textureLoaded = null;
               if (cacheOnlyMode || region.getLoadState() == 0 && (!region.shouldCache() || !region.isMetaLoaded() || this.mainTextureLevel == region.getLevel()) || !region.shouldCache() && region.getLoadState() == 4) {
                  textureLoaded = new boolean[8][8];
                  justMetaData = region.loadCacheTextures(this.mapProcessor, biomeRegistry, !region.isMetaLoaded() && this.mainTextureLevel != region.getLevel(), textureLoaded, targetHighlightsHash, leafShouldAffectBranchesDest, metaLoadedDest, 10, this.oldFormatSupport);
               }

               if (justMetaData) {
                  hasSomething = this.cleanupLoadedCache(region, textureLoaded, justMetaData, hasSomething, targetHighlightsHash, metaLoadedDest[0]);
                  if (debugConfig) {
                     WorldMap.LOGGER.info("Loaded meta data for " + String.valueOf(region));
                  }
               } else {
                  region.setHighlightsHash(targetHighlightsHash);
                  boolean shouldAddToLoaded = region.getLoadState() == 0;
                  boolean shouldLoadProperly;
                  synchronized(region) {
                     boolean goingToPrepareCache = region.shouldCache() && (region.isMetaLoaded() && this.mainTextureLevel != region.getLevel() || region.getLoadState() == 4 || region.getCacheFile() == null || !region.getCacheFile().exists());
                     if (!goingToPrepareCache) {
                        goingToPrepareCache = region.getDim().getFullReloader() != null && region.getDim().getFullReloader().isPartOfReload(region);
                     }

                     shouldLoadProperly = region.getLoadState() == 4 && region.isBeingWritten() || goingToPrepareCache;
                     if (cacheOnlyMode) {
                        shouldLoadProperly = false;
                     }

                     if (!shouldLoadProperly) {
                        if (leafShouldAffectBranchesDest[0]) {
                           region.setRecacheHasBeenRequested(true, "cache affects branches");
                           region.setShouldCache(true, "cache affects branches");
                        }

                        region.setLoadState((byte)3);
                     } else if (region.shouldCache()) {
                        region.setRecacheHasBeenRequested(true, "loading");
                     }
                  }

                  if (!shouldLoadProperly && textureLoaded != null) {
                     hasSomething = this.cleanupLoadedCache(region, textureLoaded, justMetaData, hasSomething, targetHighlightsHash, metaLoadedDest[0]);
                  }

                  this.mapProcessor.addToProcess(region);
                  if (shouldAddToLoaded) {
                     this.mapProcessor.getMapWorld().getCurrentDimension().getLayeredMapRegions().addLoadedRegion(region);
                  }

                  if (shouldLoadProperly) {
                     region.setCacheHashCode(globalRegionCacheHashCode);
                     region.setReloadVersion(globalReloadVersion);
                     loaded = this.loadRegion(region, blockLookup, blockRegistry, fluidRegistry, biomeGetter, debugConfig, 10);
                     hasSomething = false;
                     if (!loaded) {
                        region.setShouldCache(false, "couldn't load");
                        region.setRecacheHasBeenRequested(false, "couldn't load");
                        if (region.getSaveExists() == null) {
                           synchronized(region) {
                              region.setLoadState((byte)4);
                           }

                           region.deleteTexturesAndBuffers();
                           this.mapProcessor.removeMapRegion(region);
                        }
                     } else {
                        int i = 0;

                        while(true) {
                           if (i >= 8) {
                              if (hasSomething) {
                                 break;
                              }

                              synchronized(region) {
                                 if (!region.isBeingWritten() && region.getLoadState() <= 1) {
                                    region.setLoadState((byte)3);
                                 }
                              }

                              loaded = false;
                              break;
                           }

                           for(int j = 0; j < 8; ++j) {
                              MapTileChunk mapTileChunk = region.getChunk(i, j);
                              if (mapTileChunk != null) {
                                 if (!mapTileChunk.includeInSave()) {
                                    region.uncountTextureBiomes(mapTileChunk.getLeafTexture());
                                    mapTileChunk.getLeafTexture().resetBiomes();
                                    if (!mapTileChunk.hasHighlightsIfUndiscovered()) {
                                       region.setChunk(i, j, (MapTileChunk)null);
                                       mapTileChunk.getLeafTexture().deleteTexturesAndBuffers();
                                    } else {
                                       mapTileChunk.setLoadState((byte)2);
                                       mapTileChunk.unsetHasHadTerrain();
                                       mapTileChunk.getLeafTexture().requestHighlightOnlyUpload();
                                       hasSomething = true;
                                       synchronized(region) {
                                          region.updateLeafTextureVersion(i, j, targetHighlightsHash);
                                       }
                                    }
                                 } else {
                                    hasSomething = true;
                                 }
                              } else if (region.leafTextureVersionSum[i][j] != 0) {
                                 synchronized(region) {
                                    region.updateLeafTextureVersion(i, j, 0);
                                 }
                              }
                           }

                           ++i;
                        }
                     }

                     synchronized(region) {
                        if (region.getLoadState() <= 1) {
                           region.setLoadState((byte)2);
                        }

                        region.setLastSaveTime(region.isResaving() ? -60000L : System.currentTimeMillis());
                     }

                     BranchLeveledRegion parentRegion = region.getParent();
                     if (parentRegion != null) {
                        parentRegion.setShouldCheckForUpdatesRecursive(true);
                     }
                  } else if (debugConfig) {
                     WorldMap.LOGGER.info("Loaded from cache only for " + String.valueOf(region));
                  }

                  region.loadingNeededForBranchLevel = 0;
               }

               if (fromNothing && !hasSomething) {
                  BranchLeveledRegion parentRegion = region.getParent();
                  if (parentRegion != null) {
                     parentRegion.setShouldCheckForUpdatesRecursive(true);
                  }
               }
            }

            region.setReloadHasBeenRequested(false, "loading");
            this.removeToLoad(region);
         }

         this.loadingFiles = false;
         this.mapProcessor.popIsLoading();
      }

      Logger var10000;
      String var10001;
      MapRegion region;
      for(int regionsToSave = 3; !this.toSave.isEmpty() && (this.saveAll || regionsToSave > 0); this.toSave.remove(region)) {
         region = (MapRegion)this.toSave.get(0);
         boolean regionLoaded;
         synchronized(region) {
            regionLoaded = region.getLoadState() == 2;
         }

         if (regionLoaded) {
            if (!region.isBeingWritten()) {
               throw new Exception("Saving a weird region: " + String.valueOf(region));
            }

            region.pushWriterPause();
            boolean notEmpty = this.saveRegion(region, debugConfig, 20);
            region.setResaving(false);
            if (notEmpty) {
               if (!region.isAllCachePrepared()) {
                  synchronized(region) {
                     if (!region.isAllCachePrepared()) {
                        region.requestRefresh(this.mapProcessor, false);
                     }
                  }
               }

               region.setRecacheHasBeenRequested(true, "saving");
               region.setShouldCache(true, "saving");
               region.setBeingWritten(false);
               --regionsToSave;
            } else {
               this.mapProcessor.removeMapRegion(region);
            }

            region.popWriterPause();
            if (region.getWorldId() == null || !this.mapProcessor.isEqual(region.getWorldId(), region.getDimId(), region.getMwId())) {
               if (region.getCacheFile() != null) {
                  region.convertCacheToOutdated(this, "is outdated");
                  if (debugConfig) {
                     WorldMap.LOGGER.info(String.format("Converting cache for region %s because it IS outdated.", region));
                  }
               }

               region.clearRegion(this.mapProcessor);
            }
         } else if (debugConfig) {
            var10000 = WorldMap.LOGGER;
            var10001 = String.valueOf(region);
            var10000.info("Tried to save a weird region: " + var10001 + " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId() + " " + region.getLoadState());
         }
      }

      this.saveAll = false;
      int i;
      if (!this.toLoadBranchCache.isEmpty()) {
         i = this.toLoadBranchCache.size();
         this.mapProcessor.pushIsLoading();
         if (!this.mapProcessor.isWaitingForWorldUpdate()) {
            while(i > 0) {
               --i;
               BranchLeveledRegion region;
               synchronized(this.toLoadBranchCache) {
                  if (this.toLoadBranchCache.isEmpty()) {
                     break;
                  }

                  region = (BranchLeveledRegion)this.toLoadBranchCache.get(0);
               }

               region.preCacheLoad();
               LayeredRegionManager regionManager = this.mapProcessor.getMapWorld().getCurrentDimension().getLayeredMapRegions();
               regionManager.addLoadedRegion(region);
               region.setCacheFile(region.findCacheFile(this));
               boolean[] metaLoadedDest = new boolean[1];
               boolean[][] textureLoaded = new boolean[8][8];
               region.loadCacheTextures(this.mapProcessor, biomeRegistry, false, textureLoaded, 0, (boolean[])null, metaLoadedDest, 10, this.oldFormatSupport);
               if (metaLoadedDest[0]) {
                  region.confirmMetaLoaded();
               }

               this.mapProcessor.addToProcess(region);
               if (region.getCacheFile() == null) {
                  region.setShouldCheckForUpdatesRecursive(true);
               } else {
                  region.setShouldCheckForUpdatesSingle(true);
               }

               region.setShouldCache(false, "branch loading");
               region.setLoaded(true);
               if (debugConfig) {
                  WorldMap.LOGGER.info("Loaded cache for branch region " + String.valueOf(region));
               }

               region.setReloadHasBeenRequested(false, "loading");
               synchronized(this.toLoadBranchCache) {
                  this.toLoadBranchCache.remove(region);
               }
            }
         }

         this.mapProcessor.popIsLoading();
      }

      if (this.mapProcessor.getMapWorld().getCurrentDimensionId() != null) {
         this.workingDimList.clear();
         this.mapProcessor.getMapWorld().getDimensions(this.workingDimList);

         label400:
         for(i = 0; i < this.workingDimList.size(); ++i) {
            MapDimension dim = (MapDimension)this.workingDimList.get(i);

            while(true) {
               while(true) {
                  if (dim.regionsToCache.isEmpty()) {
                     continue label400;
                  }

                  LeveledRegion<?> region = this.removeToCache(dim, 0);
                  region.preCache();
                  skipCaching = region.skipCaching(globalVersion);
                  if (region.shouldCache() && region.recacheHasBeenRequested() && !skipCaching) {
                     if (!region.isAllCachePrepared()) {
                        String var10002 = String.valueOf(region);
                        throw new RuntimeException("Trying to save cache for a region with cache not prepared: " + var10002 + " " + region.getExtraInfo());
                     }

                     if (region.getCacheFile() != null) {
                        this.removeTempCacheRequest(region.getCacheFile());
                     }

                     File permFile = region.findCacheFile(this);
                     File tempFile = this.getSecondaryFile(".xwmc.temp", permFile);
                     boolean successfullySaved = region.saveCacheTextures(tempFile, debugConfig, 10);
                     if (successfullySaved) {
                        this.cacheToConvertFromTemp.add(permFile);
                        region.setCacheFile(permFile);
                     }

                     region.setShouldCache(false, "toCache normal");
                     region.setRecacheHasBeenRequested(false, "toCache normal");
                     region.postCache(permFile, this, successfullySaved);
                  } else {
                     if (WorldMap.detailed_debug) {
                        var10000 = WorldMap.LOGGER;
                        var10001 = String.valueOf(region);
                        var10000.info("toCache cancel: " + var10001 + " " + !region.shouldCache() + " " + !region.recacheHasBeenRequested() + " " + !region.isAllCachePrepared() + " " + skipCaching + " " + globalVersion);
                     }

                     if (region.shouldCache()) {
                        region.deleteBuffers();
                     }

                     region.setShouldCache(false, "toCache cancel");
                     region.setRecacheHasBeenRequested(false, "toCache cancel");
                     region.postCache((File)null, this, false);
                  }
               }
            }
         }
      }

      for(i = 0; i < this.cacheToConvertFromTemp.size(); ++i) {
         File permFile = (File)this.cacheToConvertFromTemp.get(i);
         File tempFile = this.getSecondaryFile(".xwmc.temp", permFile);

         try {
            if (Files.exists(tempFile.toPath(), new LinkOption[0])) {
               IOUtils.safeMoveAndReplace(tempFile.toPath(), permFile.toPath(), true);
            }

            this.cacheToConvertFromTemp.remove(i);
            --i;
         } catch (IOException var40) {
         }
      }

   }

   public boolean removeTempCacheRequest(File file) {
      boolean result;
      for(result = false; this.cacheToConvertFromTemp.remove(file); result = true) {
      }

      return result;
   }

   public void addTempCacheRequest(File file) {
      this.cacheToConvertFromTemp.add(file);
   }

   private boolean cleanupLoadedCache(MapRegion region, boolean[][] textureLoaded, boolean justMetaData, boolean hasSomething, int targetHighlightsHash, boolean metaLoaded) {
      for(int i = 0; i < 8; ++i) {
         for(int j = 0; j < 8; ++j) {
            boolean loaded = textureLoaded[i][j];
            if (!justMetaData && loaded) {
               hasSomething = true;
            } else {
               MapTileChunk mapTileChunk = region.getChunk(i, j);
               if (mapTileChunk == null) {
                  if (!loaded && region.leafTextureVersionSum[i][j] != 0) {
                     region.updateLeafTextureVersion(i, j, 0);
                  }
               } else {
                  if (!justMetaData && mapTileChunk.hasHighlightsIfUndiscovered()) {
                     mapTileChunk.getLeafTexture().requestHighlightOnlyUpload();
                     hasSomething = true;
                  } else {
                     region.setChunk(i, j, (MapTileChunk)null);
                     if (!justMetaData) {
                        mapTileChunk.getLeafTexture().deleteTexturesAndBuffers();
                     }
                  }

                  if (!loaded && mapTileChunk.hasHighlightsIfUndiscovered()) {
                     region.updateLeafTextureVersion(i, j, targetHighlightsHash);
                  }
               }
            }
         }
      }

      if (metaLoaded) {
         region.confirmMetaLoaded();
      }

      return hasSomething;
   }

   private void savePixel(MapBlock pixel, DataOutputStream out, class_2378<class_1959> biomeRegistry) throws IOException {
      boolean isGrass = pixel.isGrass();
      boolean inPalette = false;
      boolean biomeInPalette = false;
      class_2680 state = null;
      int parametres = pixel.getParametres();
      if (!isGrass) {
         state = pixel.getState();
         inPalette = this.regionSavePalette.containsKey(state);
         if (!inPalette) {
            parametres |= 2097152;
         }
      }

      class_5321<class_1959> pixelBiome = pixel.getBiome();
      String pixelBiomeString = null;
      if (pixelBiome != null) {
         biomeInPalette = this.regionSaveBiomePalette.containsKey(pixelBiome);
         if (!biomeInPalette) {
            parametres |= 4194304;
            class_2960 biomeIdentifier = pixelBiome.method_29177();
            pixelBiomeString = biomeIdentifier == null ? null : biomeIdentifier.toString();
            if (pixelBiomeString == null) {
               pixelBiomeString = class_1972.field_9451.method_29177().toString();
            }
         }
      }

      out.writeInt(parametres);
      if (!isGrass) {
         if (inPalette) {
            out.writeInt((Integer)this.regionSavePalette.get(state));
         } else {
            if (state instanceof UnknownBlockState) {
               ((UnknownBlockState)state).write(out);
            } else {
               class_2507.method_10628(class_2512.method_10686(state), out);
            }

            this.regionSavePalette.put(state, this.regionSavePalette.size());
         }
      }

      if ((parametres & 16777216) != 0) {
         out.write(pixel.getTopHeight());
      }

      if (pixel.getNumberOfOverlays() != 0) {
         out.write(pixel.getOverlays().size());

         for(int i = 0; i < pixel.getOverlays().size(); ++i) {
            this.saveOverlay((Overlay)pixel.getOverlays().get(i), out);
         }
      }

      if (pixelBiome != null) {
         if (biomeInPalette) {
            out.writeInt((Integer)this.regionSaveBiomePalette.get(pixelBiome));
         } else {
            out.writeUTF(pixelBiomeString);
            this.regionSaveBiomePalette.put(pixelBiome, this.regionSaveBiomePalette.size());
         }
      }

   }

   private void loadPixel(Integer next, MapBlock pixel, DataInputStream in, int minorSaveVersion, int majorSaveVersion, boolean is115not114, class_7225<class_2248> blockLookup, BiomeGetter biomeGetter, class_2378<class_1959> biomeRegistry) throws IOException {
      int parametres;
      if (next != null) {
         parametres = next;
      } else {
         parametres = in.readInt();
      }

      int heightSecondPartOffset;
      boolean topHeightIsDifferent;
      int savedColourType;
      if ((parametres & 1) != 0) {
         if (majorSaveVersion == 0) {
            heightSecondPartOffset = in.readInt();
            pixel.setState(this.oldFormatSupport.getStateForId(heightSecondPartOffset));
         } else {
            topHeightIsDifferent = (parametres & 2097152) != 0;
            class_2680 state;
            if (topHeightIsDifferent) {
               class_2487 nbt = class_2507.method_10627(in);
               if (majorSaveVersion < 6) {
                  this.oldFormatSupport.fixBlock(nbt, majorSaveVersion);
               }

               state = WorldMap.unknownBlockStateCache.getBlockStateFromNBT(blockLookup, nbt);
               this.regionLoadPalette.add(state);
            } else {
               savedColourType = in.readInt();
               state = (class_2680)this.regionLoadPalette.get(savedColourType);
            }

            pixel.setState(state);
         }
      } else {
         pixel.setState(class_2246.field_10219.method_9564());
      }

      if ((parametres & 64) != 0) {
         pixel.setHeight(in.read());
      } else {
         heightSecondPartOffset = minorSaveVersion >= 4 ? 25 : 24;
         int heightBitsCombined = parametres >> 12 & 255 | (parametres >> heightSecondPartOffset & 15) << 8;
         savedColourType = heightBitsCombined << 20 >> 20;
         pixel.setHeight(savedColourType);
      }

      topHeightIsDifferent = minorSaveVersion < 4 ? false : (parametres & 16777216) != 0;
      if (topHeightIsDifferent) {
         pixel.setTopHeight(in.read());
      } else {
         pixel.setTopHeight(pixel.getHeight());
      }

      boolean stillUsesColorTypes = minorSaveVersion < 5 || majorSaveVersion <= 2 && !is115not114;
      this.overlayBuilder.startBuilding();
      if ((parametres & 2) != 0) {
         savedColourType = in.read();

         for(int i = 0; i < savedColourType; ++i) {
            this.loadOverlay(pixel, in, minorSaveVersion, majorSaveVersion, stillUsesColorTypes, blockLookup, biomeGetter);
         }
      }

      this.overlayBuilder.finishBuilding(pixel);
      savedColourType = stillUsesColorTypes ? parametres >> 2 & 3 : 0;
      if (savedColourType == 3) {
         in.readInt();
      }

      class_5321<class_1959> biomeKey = null;
      boolean hasSlope;
      if (savedColourType != 0 && savedColourType != 3 || (parametres & 1048576) != 0) {
         int biomeByte;
         String biomeIdentifier;
         if (majorSaveVersion < 4) {
            biomeByte = in.read();
            int oldBiomeByte;
            if (minorSaveVersion >= 3 && biomeByte >= 255) {
               oldBiomeByte = in.readInt();
            } else {
               oldBiomeByte = biomeByte;
            }

            biomeIdentifier = this.oldFormatSupport.fixBiome(oldBiomeByte, majorSaveVersion);
            biomeKey = class_5321.method_29179(class_7924.field_41236, class_2960.method_60654(biomeIdentifier));
         } else {
            hasSlope = (parametres & 4194304) != 0;
            if (hasSlope) {
               boolean biomeAsInt = (parametres & 8388608) != 0;
               if (biomeAsInt) {
                  int biomeId = in.readInt();
                  biomeIdentifier = this.oldFormatSupport.fixBiome(biomeId, majorSaveVersion);
               } else {
                  biomeIdentifier = this.oldFormatSupport.fixBiome(in.readUTF(), majorSaveVersion);
               }

               biomeKey = class_5321.method_29179(class_7924.field_41236, class_2960.method_60654(biomeIdentifier));
               this.regionLoadBiomePalette.add(biomeKey);
            } else {
               biomeByte = in.readInt();
               biomeKey = (class_5321)this.regionLoadBiomePalette.get(biomeByte);
            }
         }
      }

      pixel.setBiome(biomeKey);
      if (minorSaveVersion == 2) {
         hasSlope = (parametres & 16) != 0;
         if (hasSlope) {
            pixel.setVerticalSlope((byte)in.read());
            pixel.setSlopeUnknown(false);
         }
      }

      pixel.setLight((byte)(parametres >> 8 & 15));
      pixel.setGlowing(this.mapProcessor.getMapWriter().isGlowing(pixel.getState()));
   }

   private void saveOverlay(Overlay o, DataOutputStream out) throws IOException {
      boolean isWater = o.isWater();
      boolean inPalette = false;
      class_2680 state = null;
      int parametres = o.getParametres();
      if (!isWater) {
         state = o.getState();
         inPalette = this.regionSavePalette.containsKey(state);
         if (!inPalette) {
            parametres |= 1024;
         }
      }

      out.writeInt(parametres);
      if (!isWater) {
         if (inPalette) {
            out.writeInt((Integer)this.regionSavePalette.get(state));
         } else {
            if (state instanceof UnknownBlockState) {
               ((UnknownBlockState)state).write(out);
            } else {
               class_2507.method_10628(class_2512.method_10686(state), out);
            }

            this.regionSavePalette.put(state, this.regionSavePalette.size());
         }
      }

   }

   private void loadOverlay(MapBlock pixel, DataInputStream in, int minorSaveVersion, int majorSaveVersion, boolean stillUsesColorTypes, class_7225<class_2248> blockLookup, BiomeGetter biomeGetter) throws IOException {
      int parametres = in.readInt();
      class_2680 state;
      if ((parametres & 1) != 0) {
         if (majorSaveVersion == 0) {
            state = this.oldFormatSupport.getStateForId(in.readInt());
         } else {
            boolean paletteNew = (parametres & 1024) != 0;
            if (paletteNew) {
               class_2487 nbt = class_2507.method_10627(in);
               state = WorldMap.unknownBlockStateCache.getBlockStateFromNBT(blockLookup, nbt);
               this.regionLoadPalette.add(state);
            } else {
               int paletteIndex = in.readInt();
               state = (class_2680)this.regionLoadPalette.get(paletteIndex);
            }
         }
      } else {
         state = class_2246.field_10382.method_9564();
      }

      int opacity = 1;
      if (minorSaveVersion < 1 && (parametres & 2) != 0) {
         in.readInt();
      }

      byte savedColourType = stillUsesColorTypes ? (byte)(parametres >> 8 & 3) : 0;
      if (savedColourType == 2 || (parametres & 4) != 0) {
         in.readInt();
      }

      if (minorSaveVersion < 8) {
         if ((parametres & 8) != 0) {
            opacity = in.readInt();
         }
      } else {
         opacity = parametres >> 11 & 15;
      }

      byte light = (byte)(parametres >> 4 & 15);
      this.overlayBuilder.build(state, opacity, light, this.mapProcessor, (class_5321)null);
   }

   public boolean isRegionDetectionComplete() {
      return this.regionDetectionComplete;
   }

   public void setRegionDetectionComplete(boolean regionDetectionComplete) {
      this.regionDetectionComplete = regionDetectionComplete;
   }

   public void requestCache(LeveledRegion<?> region) {
      if (!this.toCacheContains(region)) {
         synchronized(region.getDim().regionsToCache) {
            region.getDim().regionsToCache.add(region);
         }

         if (WorldMapClientConfigUtils.getDebug()) {
            WorldMap.LOGGER.info("Requesting cache! " + String.valueOf(region));
         }
      }

   }

   public LeveledRegion<?> removeToCache(MapDimension mapDim, int index) {
      synchronized(mapDim.regionsToCache) {
         return (LeveledRegion)mapDim.regionsToCache.remove(index);
      }
   }

   public void removeToCache(LeveledRegion<?> region) {
      synchronized(region.getDim().regionsToCache) {
         region.getDim().regionsToCache.remove(region);
      }
   }

   public boolean toCacheContains(LeveledRegion<?> region) {
      synchronized(region.getDim().regionsToCache) {
         return region.getDim().regionsToCache.contains(region);
      }
   }

   public ArrayList<MapRegion> getToSave() {
      return this.toSave;
   }

   public LeveledRegion<?> getNextToLoadByViewing() {
      return this.nextToLoadByViewing;
   }

   public void setNextToLoadByViewing(LeveledRegion<?> nextToLoadByViewing) {
      this.nextToLoadByViewing = nextToLoadByViewing;
   }

   public OldFormatSupport getOldFormatSupport() {
      return this.oldFormatSupport;
   }

   public void safeDelete(Path filePath, String extension) throws IOException {
      if (!filePath.getFileName().toString().endsWith(extension)) {
         throw new RuntimeException("Incorrect file extension: " + String.valueOf(filePath));
      } else {
         Files.deleteIfExists(filePath);
      }
   }

   public void safeMoveAndReplace(Path fromPath, Path toPath, String fromExtension, String toExtension) throws IOException {
      if (toPath.getFileName().toString().endsWith(toExtension) && fromPath.getFileName().toString().endsWith(fromExtension)) {
         IOUtils.safeMoveAndReplace(fromPath, toPath, true);
      } else {
         String var10002 = String.valueOf(fromPath);
         throw new RuntimeException("Incorrect file extension: " + var10002 + " " + String.valueOf(toPath));
      }
   }

   public int getSizeOfToLoadBranchCache() {
      return this.toLoadBranchCache.size();
   }
}
