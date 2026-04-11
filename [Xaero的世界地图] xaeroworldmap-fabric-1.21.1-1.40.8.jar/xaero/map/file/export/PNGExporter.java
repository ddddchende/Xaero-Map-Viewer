package xaero.map.file.export;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import javax.imageio.ImageIO;
import net.minecraft.class_1959;
import net.minecraft.class_2378;
import net.minecraft.class_2874;
import net.minecraft.class_308;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_8251;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.client.graphics.shader.LibShaders;
import xaero.lib.common.config.Config;
import xaero.lib.common.config.single.SingleConfigManager;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.biome.BlockTintProvider;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.config.primary.option.WorldMapPrimaryClientConfigOptions;
import xaero.map.exception.OpenGLException;
import xaero.map.file.MapRegionInfo;
import xaero.map.file.MapSaveLoad;
import xaero.map.file.OldFormatSupport;
import xaero.map.file.RegionDetection;
import xaero.map.graphics.CustomRenderTypes;
import xaero.map.graphics.ImprovedFramebuffer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.map.gui.GuiMap;
import xaero.map.gui.MapTileSelection;
import xaero.map.misc.Misc;
import xaero.map.mods.SupportMods;
import xaero.map.region.ExportMapRegion;
import xaero.map.region.ExportMapTileChunk;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapLayer;
import xaero.map.region.MapRegion;
import xaero.map.region.MapTileChunk;
import xaero.map.region.MapUpdateFastConfig;
import xaero.map.region.OverlayManager;
import xaero.map.region.texture.ExportLeafRegionTexture;
import xaero.map.world.MapDimension;

public class PNGExporter {
   private final Calendar calendar = Calendar.getInstance();
   private Path destinationPath;
   private class_4587 matrixStack;

   public PNGExporter(Path destinationPath) {
      this.destinationPath = destinationPath;
      this.matrixStack = new class_4587();
   }

   public PNGExportResult export(MapProcessor mapProcessor, class_2378<class_1959> biomeRegistry, class_2378<class_2874> dimensionTypes, MapTileSelection selection, OldFormatSupport oldFormatSupport) throws IllegalArgumentException, IllegalAccessException, OpenGLException {
      if (!mapProcessor.getMapSaveLoad().isRegionDetectionComplete()) {
         return new PNGExportResult(PNGExportResultType.NOT_PREPARED, (Path)null);
      } else {
         int exportedLayer = mapProcessor.getCurrentCaveLayer();
         MapDimension dim = mapProcessor.getMapWorld().getCurrentDimension();
         Set<LeveledRegion<?>> list = dim.getLayeredMapRegions().getUnsyncedSet();
         if (list.isEmpty()) {
            return new PNGExportResult(PNGExportResultType.EMPTY, (Path)null);
         } else {
            ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
            SingleConfigManager<Config> primaryConfigManager = configManager.getPrimaryConfigManager();
            boolean multipleImagesSetting = (Boolean)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.EXPORT_MULTIPLE_IMAGES);
            boolean nightExportSetting = (Boolean)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.NIGHT_EXPORT);
            int exportScaleDownSquareSetting = (Integer)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.EXPORT_SCALE_DOWN_SQUARE);
            boolean includingHighlights = (Boolean)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.EXPORT_HIGHLIGHTS);
            boolean full = selection == null;
            Integer minX = null;
            Integer maxX = null;
            Integer minZ = null;
            Integer maxZ = null;
            MapLayer mapLayer = dim.getLayeredMapRegions().getLayer(exportedLayer);
            if (full) {
               Iterator var21 = list.iterator();

               label451:
               while(true) {
                  LeveledRegion region;
                  do {
                     do {
                        do {
                           do {
                              if (!var21.hasNext()) {
                                 Iterable<Hashtable<Integer, RegionDetection>> regionDetectionIterable = !dim.isUsingWorldSave() ? mapLayer.getDetectedRegions().values() : dim.getWorldSaveDetectedRegions();
                                 Iterator var99 = ((Iterable)regionDetectionIterable).iterator();

                                 label413:
                                 while(var99.hasNext()) {
                                    Hashtable<Integer, RegionDetection> column = (Hashtable)var99.next();
                                    Iterator var24 = column.values().iterator();

                                    while(true) {
                                       RegionDetection regionDetection;
                                       do {
                                          do {
                                             if (!var24.hasNext()) {
                                                continue label413;
                                             }

                                             regionDetection = (RegionDetection)var24.next();
                                          } while(!regionDetection.isHasHadTerrain());

                                          if (minX == null || regionDetection.getRegionX() < minX) {
                                             minX = regionDetection.getRegionX();
                                          }

                                          if (maxX == null || regionDetection.getRegionX() > maxX) {
                                             maxX = regionDetection.getRegionX();
                                          }

                                          if (minZ == null || regionDetection.getRegionZ() < minZ) {
                                             minZ = regionDetection.getRegionZ();
                                          }
                                       } while(maxZ != null && regionDetection.getRegionZ() <= maxZ);

                                       maxZ = regionDetection.getRegionZ();
                                    }
                                 }
                                 break label451;
                              }

                              region = (LeveledRegion)var21.next();
                           } while(region.getLevel() != 0);
                        } while(!((MapRegion)region).hasHadTerrain());
                     } while(region.getCaveLayer() != exportedLayer);

                     if (minX == null || region.getRegionX() < minX) {
                        minX = region.getRegionX();
                     }

                     if (maxX == null || region.getRegionX() > maxX) {
                        maxX = region.getRegionX();
                     }

                     if (minZ == null || region.getRegionZ() < minZ) {
                        minZ = region.getRegionZ();
                     }
                  } while(maxZ != null && region.getRegionZ() <= maxZ);

                  maxZ = region.getRegionZ();
               }
            } else {
               minX = selection.getLeft() >> 5;
               minZ = selection.getTop() >> 5;
               maxX = selection.getRight() >> 5;
               maxZ = selection.getBottom() >> 5;
            }

            int minBlockX = minX * 512;
            int minBlockZ = minZ * 512;
            int maxBlockX = (maxX + 1) * 512 - 1;
            int maxBlockZ = (maxZ + 1) * 512 - 1;
            if (!full) {
               minBlockX = Math.max(minBlockX, selection.getLeft() << 4);
               minBlockZ = Math.max(minBlockZ, selection.getTop() << 4);
               maxBlockX = Math.min(maxBlockX, (selection.getRight() << 4) + 15);
               maxBlockZ = Math.min(maxBlockZ, (selection.getBottom() << 4) + 15);
            }

            int exportAreaWidthInRegions = maxX - minX + 1;
            int exportAreaHeightInRegions = maxZ - minZ + 1;
            long exportAreaSizeInRegions = (long)exportAreaWidthInRegions * (long)exportAreaHeightInRegions;
            int exportAreaWidth = exportAreaWidthInRegions * 512;
            int exportAreaHeight = exportAreaHeightInRegions * 512;
            if (!full) {
               exportAreaWidth = maxBlockX - minBlockX + 1;
               exportAreaHeight = maxBlockZ - minBlockZ + 1;
            }

            int scaleDownSquareSquared = exportScaleDownSquareSetting * exportScaleDownSquareSetting;
            float scale = exportAreaSizeInRegions >= (long)scaleDownSquareSquared && !multipleImagesSetting && scaleDownSquareSquared > 0 ? (float)((double)exportScaleDownSquareSetting / Math.sqrt((double)exportAreaSizeInRegions)) : 1.0F;
            int exportImageWidth = (int)((float)exportAreaWidth * scale);
            int exportImageHeight = (int)((float)exportAreaHeight * scale);
            if (!multipleImagesSetting && scaleDownSquareSquared > 0) {
               long maxExportAreaSizeInRegions = (long)scaleDownSquareSquared * 262144L;
               if ((long)exportAreaWidth * (long)exportAreaHeight / 512L / 512L > maxExportAreaSizeInRegions) {
                  return new PNGExportResult(PNGExportResultType.TOO_BIG, (Path)null);
               }
            }

            int maxTextureSize = GL11.glGetInteger(3379);
            OpenGLException.checkGLError();
            int frameWidth = Math.min(1024, Math.min(maxTextureSize, exportImageWidth));
            int frameHeight = Math.min(1024, Math.min(maxTextureSize, exportImageHeight));
            int horizontalFrames = (int)Math.ceil((double)exportImageWidth / (double)frameWidth);
            int verticalFrames = (int)Math.ceil((double)exportImageHeight / (double)frameHeight);
            boolean multipleImages = multipleImagesSetting && horizontalFrames * verticalFrames > 1;
            if (multipleImages) {
               exportImageWidth = frameWidth;
               exportImageHeight = frameHeight;
            }

            int pixelCount = exportImageWidth * exportImageHeight;
            if (pixelCount != Integer.MAX_VALUE && pixelCount / exportImageHeight == exportImageWidth) {
               boolean debugConfig = (Boolean)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.DEBUG);
               if (debugConfig) {
                  WorldMap.LOGGER.info(String.format("Exporting PNG of size %dx%d using a framebuffer of size %dx%d.", exportImageWidth, exportImageHeight, frameWidth, frameHeight));
               }

               BufferedImage image;
               ImprovedFramebuffer exportFrameBuffer;
               ByteBuffer frameDataBuffer;
               int[] bufferArray;
               try {
                  image = new BufferedImage(exportImageWidth, exportImageHeight, 1);
                  exportFrameBuffer = new ImprovedFramebuffer(frameWidth, frameHeight, false);
                  frameDataBuffer = BufferUtils.createByteBuffer(frameWidth * frameHeight * 4);
                  bufferArray = new int[frameWidth * frameHeight];
               } catch (OutOfMemoryError var96) {
                  return new PNGExportResult(PNGExportResultType.OUT_OF_MEMORY, (Path)null);
               }

               if (exportFrameBuffer.field_1476 == -1) {
                  return new PNGExportResult(PNGExportResultType.BAD_FBO, (Path)null);
               } else {
                  MapUpdateFastConfig updateConfig = new MapUpdateFastConfig();
                  boolean lighting = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.LIGHTING);
                  class_308.method_24210();
                  Matrix4f ortho = (new Matrix4f()).setOrtho(0.0F, (float)frameWidth, 0.0F, (float)frameHeight, 0.0F, 1000.0F);
                  RenderSystem.setProjectionMatrix(ortho, class_8251.field_43361);
                  class_4587 matrixStack = this.matrixStack;
                  BlockStateShortShapeCache shortShapeCache = mapProcessor.getBlockStateShortShapeCache();
                  BlockTintProvider blockTintProvider = mapProcessor.getWorldBlockTintProvider();
                  OverlayManager overlayManager = mapProcessor.getOverlayManager();
                  MapSaveLoad mapSaveLoad = mapProcessor.getMapSaveLoad();
                  MultiTextureRenderTypeRendererProvider rendererProvider = mapProcessor.getMultiTextureRenderTypeRenderers();
                  Matrix4fStack shaderMatrixStack = RenderSystem.getModelViewStack();
                  shaderMatrixStack.pushMatrix();
                  shaderMatrixStack.identity();
                  RenderSystem.applyModelViewMatrix();
                  matrixStack.method_22903();
                  exportFrameBuffer.bindAsMainTarget(true);
                  matrixStack.method_22905(scale, scale, 1.0F);
                  boolean[] justMetaDest = new boolean[1];
                  Path imageDestination = this.destinationPath;
                  if (multipleImages) {
                     imageDestination = this.destinationPath.resolve(this.getExportBaseName());
                  }

                  boolean empty = true;
                  PNGExportResultType resultType = PNGExportResultType.SUCCESS;

                  for(int i = 0; i < horizontalFrames; ++i) {
                     for(int j = 0; j < verticalFrames; ++j) {
                        boolean renderedSomething = false;
                        RenderSystem.bindTexture(0);
                        RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 1.0F);
                        RenderSystem.clear(16640, class_310.field_1703);
                        matrixStack.method_22903();
                        float frameLeft = (float)minBlockX + (float)(i * frameWidth) / scale;
                        float frameRight = (float)minBlockX + (float)((i + 1) * frameWidth) / scale - 1.0F;
                        float frameTop = (float)minBlockZ + (float)(j * frameHeight) / scale;
                        float frameBottom = (float)minBlockZ + (float)((j + 1) * frameHeight) / scale - 1.0F;
                        if (!full) {
                           if ((float)maxBlockX < frameRight) {
                              frameRight = (float)maxBlockX;
                           }

                           if ((float)maxBlockZ < frameBottom) {
                              frameBottom = (float)maxBlockZ;
                           }
                        }

                        int minTileChunkX = (int)Math.floor((double)frameLeft) >> 6;
                        int maxTileChunkX = (int)Math.floor((double)frameRight) >> 6;
                        int minTileChunkZ = (int)Math.floor((double)frameTop) >> 6;
                        int maxTileChunkZ = (int)Math.floor((double)frameBottom) >> 6;
                        int minRegionX = minTileChunkX >> 3;
                        int minRegionZ = minTileChunkZ >> 3;
                        int maxRegionX = maxTileChunkX >> 3;
                        int maxRegionZ = maxTileChunkZ >> 3;
                        matrixStack.method_22904(0.1D, 0.0D, 0.0D);
                        Matrix4f matrix = matrixStack.method_23760().method_23761();

                        int regionX;
                        int regionZ;
                        for(regionX = minRegionX; regionX <= maxRegionX; ++regionX) {
                           for(regionZ = minRegionZ; regionZ <= maxRegionZ; ++regionZ) {
                              MapRegion originalRegion = mapProcessor.getLeafMapRegion(exportedLayer, regionX, regionZ, false);
                              MapRegionInfo regionInfo = originalRegion;
                              if (originalRegion == null && mapLayer.regionDetectionExists(regionX, regionZ)) {
                                 regionInfo = mapLayer.getRegionDetection(regionX, regionZ);
                              }

                              boolean regionHasHighlightsIfUndiscovered = includingHighlights && dim.getHighlightHandler().shouldApplyRegionHighlights(regionX, regionZ, false);
                              if (regionInfo != null || regionHasHighlightsIfUndiscovered) {
                                 File cacheFile = null;
                                 boolean loadingFromCache = regionInfo != null && (originalRegion == null || !originalRegion.isBeingWritten() || originalRegion.getLoadState() != 2);
                                 if (loadingFromCache) {
                                    cacheFile = ((MapRegionInfo)regionInfo).getCacheFile();
                                    if (cacheFile == null && !((MapRegionInfo)regionInfo).hasLookedForCache()) {
                                       try {
                                          cacheFile = mapSaveLoad.getCacheFile((MapRegionInfo)regionInfo, exportedLayer, true, false);
                                       } catch (IOException var95) {
                                       }
                                    }

                                    if (cacheFile == null) {
                                       if (!regionHasHighlightsIfUndiscovered) {
                                          continue;
                                       }

                                       loadingFromCache = false;
                                    }
                                 }

                                 ExportMapRegion region = new ExportMapRegion(dim, regionX, regionZ, exportedLayer, biomeRegistry);
                                 int tx;
                                 if (loadingFromCache) {
                                    region.setShouldCache(true, "png");
                                    region.setHasHadTerrain();
                                    region.setCacheFile(cacheFile);
                                    region.loadCacheTextures(mapProcessor, biomeRegistry, false, (boolean[][])null, 0, (boolean[])null, justMetaDest, 1, oldFormatSupport);
                                 } else if (originalRegion != null) {
                                    for(int o = 0; o < 8; ++o) {
                                       for(int p = 0; p < 8; ++p) {
                                          MapTileChunk originalTileChunk = originalRegion.getChunk(o, p);
                                          if (originalTileChunk != null && originalTileChunk.hasHadTerrain()) {
                                             MapTileChunk tileChunk = region.createTexture(o, p).getTileChunk();

                                             for(tx = 0; tx < 4; ++tx) {
                                                for(int tz = 0; tz < 4; ++tz) {
                                                   tileChunk.setTile(tx, tz, originalTileChunk.getTile(tx, tz), shortShapeCache);
                                                }
                                             }

                                             tileChunk.setLoadState((byte)2);
                                             tileChunk.updateBuffers(mapProcessor, blockTintProvider, overlayManager, WorldMap.detailed_debug, shortShapeCache, updateConfig);
                                          }
                                       }
                                    }
                                 }

                                 if (includingHighlights) {
                                    mapProcessor.getMapRegionHighlightsPreparer().prepare(region, true);
                                 }

                                 MultiTextureRenderTypeRenderer rendererLight = rendererProvider.getRenderer((t) -> {
                                    RenderSystem.setShaderTexture(0, t);
                                 }, MultiTextureRenderTypeRendererProvider::defaultTextureBind, CustomRenderTypes.MAP);
                                 MultiTextureRenderTypeRenderer rendererNoLight = rendererProvider.getRenderer((t) -> {
                                    RenderSystem.setShaderTexture(0, t);
                                 }, MultiTextureRenderTypeRendererProvider::defaultTextureBind, CustomRenderTypes.MAP);
                                 IntList texturesToDelete = new IntArrayList();

                                 for(int localChunkX = 0; localChunkX < 8; ++localChunkX) {
                                    for(tx = 0; tx < 8; ++tx) {
                                       ExportMapTileChunk tileChunk = region.getChunk(localChunkX, tx);
                                       if (tileChunk != null) {
                                          ExportLeafRegionTexture tileChunkTexture = tileChunk.getLeafTexture();
                                          if (tileChunkTexture != null) {
                                             if (tileChunk.getX() >= minTileChunkX && tileChunk.getX() <= maxTileChunkX && tileChunk.getZ() >= minTileChunkZ && tileChunk.getZ() <= maxTileChunkZ) {
                                                int textureId = tileChunkTexture.bindColorTexture(true);
                                                if (tileChunkTexture.getColorBuffer() == null) {
                                                   tileChunkTexture.prepareBuffer();
                                                }

                                                ByteBuffer colorBuffer = tileChunkTexture.getDirectColorBuffer();
                                                if (includingHighlights) {
                                                   tileChunkTexture.applyHighlights(dim.getHighlightHandler(), tileChunkTexture.getColorBuffer());
                                                }

                                                if (tileChunkTexture.isColorBufferCompressed()) {
                                                   GL13.glCompressedTexImage2D(3553, 0, tileChunkTexture.getColorBufferFormat(), 64, 64, 0, colorBuffer);
                                                } else {
                                                   int internalFormat = tileChunkTexture.getColorBufferFormat() == -1 ? '聘' : tileChunkTexture.getColorBufferFormat();
                                                   GL11.glTexImage2D(3553, 0, internalFormat, 64, 64, 0, 32993, 32821, colorBuffer);
                                                }

                                                tileChunkTexture.deleteColorBuffer();
                                                if (textureId != -1) {
                                                   GL11.glTexParameteri(3553, 33085, 9);
                                                   RenderSystem.texParameter(3553, 33083, 9);
                                                   exportFrameBuffer.generateMipmaps();
                                                   RenderSystem.texParameter(3553, 10241, 9987);
                                                   GuiMap.renderTexturedModalRectWithLighting3(matrix, (float)(tileChunk.getX() * 64) - frameLeft, (float)(tileChunk.getZ() * 64) - frameTop, 64.0F, 64.0F, textureId, tileChunkTexture.getBufferHasLight(), tileChunkTexture.getBufferHasLight() ? rendererLight : rendererNoLight);
                                                   renderedSomething = true;
                                                   texturesToDelete.add(textureId);
                                                }
                                             } else {
                                                tileChunkTexture.deleteColorBuffer();
                                             }
                                          }
                                       }
                                    }
                                 }

                                 float brightness = nightExportSetting ? mapProcessor.getAmbientBrightness(dim.getDimensionType(dimensionTypes)) : mapProcessor.getBrightness(exportedLayer, mapProcessor.getWorld(), lighting && exportedLayer != Integer.MAX_VALUE);
                                 LibShaders.WORLD_MAP.setBrightness(brightness);
                                 LibShaders.WORLD_MAP.setWithLight(true);
                                 rendererProvider.draw(rendererLight);
                                 LibShaders.WORLD_MAP.setWithLight(false);
                                 rendererProvider.draw(rendererNoLight);
                                 GL11.glDeleteTextures(texturesToDelete.toIntArray());
                                 RenderSystem.bindTexture(0);
                              }
                           }
                        }

                        matrixStack.method_22909();
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                        if (renderedSomething) {
                           empty = false;
                           exportFrameBuffer.method_35610();
                           frameDataBuffer.clear();
                           GL11.glGetTexImage(3553, 0, 32993, 33639, frameDataBuffer);
                           frameDataBuffer.asIntBuffer().get(bufferArray);
                           regionX = i * frameWidth;
                           regionZ = j * frameHeight;
                           if (multipleImages) {
                              regionX = 0;
                              regionZ = 0;
                           }

                           int actualFrameWidth = Math.min(frameWidth, exportImageWidth - regionX);
                           int actualFrameHeight = Math.min(frameHeight, exportImageHeight - regionZ);
                           image.setRGB(regionX, regionZ, actualFrameWidth, actualFrameHeight, bufferArray, 0, frameWidth);
                           if (multipleImages) {
                              PNGExportResultType saveResult = this.saveImage(image, imageDestination, i + "_" + j, "_x" + (int)frameLeft + "_z" + (int)frameTop);
                              if (saveResult != PNGExportResultType.SUCCESS) {
                                 resultType = saveResult;
                              }
                           }
                        }
                     }
                  }

                  exportFrameBuffer.method_1240();
                  class_310 mc = class_310.method_1551();
                  exportFrameBuffer.bindDefaultFramebuffer(mc);
                  RenderSystem.enableCull();
                  matrixStack.method_22909();
                  shaderMatrixStack.popMatrix();
                  RenderSystem.applyModelViewMatrix();
                  Misc.minecraftOrtho(mc, SupportMods.vivecraft);
                  RenderSystem.bindTexture(0);
                  exportFrameBuffer.method_1238();
                  mapProcessor.getBufferDeallocator().deallocate(frameDataBuffer, debugConfig);
                  if (empty) {
                     return new PNGExportResult(PNGExportResultType.EMPTY, (Path)null);
                  } else if (multipleImages) {
                     image.flush();
                     return new PNGExportResult(resultType, imageDestination);
                  } else {
                     resultType = this.saveImage(image, imageDestination, (String)null, "_x" + minBlockX + "_z" + minBlockZ);
                     image.flush();
                     return new PNGExportResult(resultType, imageDestination);
                  }
               }
            } else {
               return new PNGExportResult(PNGExportResultType.IMAGE_TOO_BIG, (Path)null);
            }
         }
      }
   }

   private PNGExportResultType saveImage(BufferedImage image, Path destinationPath, String baseName, String suffix) {
      if (baseName == null) {
         baseName = this.getExportBaseName();
      }

      baseName = baseName + suffix;
      int additionalIndex = 1;

      try {
         if (!Files.exists(destinationPath, new LinkOption[0])) {
            Files.createDirectories(destinationPath);
         }

         Path imagePath;
         for(imagePath = destinationPath.resolve(baseName + ".png"); Files.exists(imagePath, new LinkOption[0]); imagePath = destinationPath.resolve(baseName + "_" + additionalIndex + ".png")) {
            ++additionalIndex;
         }

         ImageIO.write(image, "png", imagePath.toFile());
         return PNGExportResultType.SUCCESS;
      } catch (IOException var7) {
         WorldMap.LOGGER.error("IO exception while exporting PNG: ", var7);
         return PNGExportResultType.IO_EXCEPTION;
      }
   }

   private String getExportBaseName() {
      this.calendar.setTimeInMillis(System.currentTimeMillis());
      int year = this.calendar.get(1);
      int month = 1 + this.calendar.get(2);
      int day = this.calendar.get(5);
      int hours = this.calendar.get(11);
      int minutes = this.calendar.get(12);
      int seconds = this.calendar.get(13);
      return String.format("%d-%02d-%02d_%02d.%02d.%02d", year, month, day, hours, minutes, seconds);
   }
}
