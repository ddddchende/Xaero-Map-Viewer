package xaero.map.region.texture;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import net.minecraft.class_1959;
import net.minecraft.class_5321;
import org.lwjgl.opengl.GL11;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.biome.BlockTintProvider;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.config.util.WorldMapClientConfigUtils;
import xaero.map.exception.OpenGLException;
import xaero.map.file.IOHelper;
import xaero.map.graphics.PixelBuffers;
import xaero.map.graphics.TextureUploader;
import xaero.map.highlight.DimensionHighlighterHandler;
import xaero.map.misc.ConsistentBitArray;
import xaero.map.palette.FastIntPalette;
import xaero.map.palette.Paletted2DFastBitArrayIntStorage;
import xaero.map.pool.buffer.PoolTextureDirectBufferUnit;
import xaero.map.region.BranchLeveledRegion;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapRegion;
import xaero.map.region.MapUpdateFastConfig;
import xaero.map.region.OverlayManager;

public abstract class RegionTexture<T extends RegionTexture<T>> {
   public static final int PBO_UNPACK_LENGTH = 16384;
   public static final int PBO_PACK_LENGTH = 16384;
   private static final long[] ONE_BIOME_PALETTE_DATA;
   private static final ThreadLocal<ConsistentBitArray> OLD_HEIGHT_VALUES_SUPPORT;
   protected int textureVersion;
   protected int glColorTexture = -1;
   protected boolean textureHasLight;
   protected PoolTextureDirectBufferUnit colorBuffer;
   protected boolean bufferHasLight;
   protected int colorBufferFormat = -1;
   protected boolean colorBufferCompressed;
   protected int bufferedTextureVersion;
   protected int packPbo;
   protected int[] unpackPbo = new int[2];
   protected boolean shouldDownloadFromPBO;
   protected int timer;
   private boolean cachePrepared;
   protected boolean toUpload;
   protected LeveledRegion<T> region;
   protected ConsistentBitArray heightValues;
   protected ConsistentBitArray topHeightValues;
   protected RegionTextureBiomes biomes;

   public RegionTexture(LeveledRegion<T> region) {
      this.region = region;
      this.bufferedTextureVersion = this.textureVersion = -1;
      this.heightValues = new ConsistentBitArray(13, 4096);
      this.topHeightValues = new ConsistentBitArray(13, 4096);
   }

   private void setupTextureParameters() {
      GL11.glTexParameteri(3553, 33084, 0);
      GL11.glTexParameteri(3553, 33085, 0);
      GL11.glTexParameterf(3553, 33082, 0.0F);
      GL11.glTexParameterf(3553, 33083, 1.0F);
      GL11.glTexParameterf(3553, 34049, 0.0F);
      GL11.glTexParameteri(3553, 10240, 9728);
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexParameteri(3553, 10242, 33071);
      GL11.glTexParameteri(3553, 10243, 33071);
   }

   public void prepareBuffer() {
      if (this.colorBuffer != null) {
         this.colorBuffer.reset();
      } else {
         this.colorBuffer = WorldMap.textureDirectBufferPool.get(true);
      }

   }

   public int bindColorTexture(boolean create) {
      boolean result = false;
      int texture = this.glColorTexture;
      if (texture == -1) {
         if (!create) {
            return -1;
         }

         texture = this.glColorTexture = GlStateManager._genTexture();
         result = true;
      }

      GlStateManager._bindTexture(texture);
      if (result) {
         this.setupTextureParameters();
      }

      RenderSystem.setShaderTexture(0, texture);
      return texture;
   }

   public long uploadBuffer(DimensionHighlighterHandler highlighterHandler, TextureUploader textureUploader, LeveledRegion<T> inRegion, BranchTextureRenderer branchTextureRenderer, int x, int y) throws OpenGLException, IllegalArgumentException, IllegalAccessException {
      long result = this.uploadBufferHelper(highlighterHandler, textureUploader, inRegion, branchTextureRenderer);
      if (!this.shouldDownloadFromPBO()) {
         this.setToUpload(false);
         if (this.getColorBufferFormat() == -1) {
            this.deleteColorBuffer();
         } else {
            this.setCachePrepared(true);
         }
      }

      return result;
   }

   public void postBufferWrite(PoolTextureDirectBufferUnit buffer) {
   }

   private long uploadBufferHelper(DimensionHighlighterHandler highlighterHandler, TextureUploader textureUploader, LeveledRegion<T> inRegion, BranchTextureRenderer branchTextureRenderer) throws OpenGLException, IllegalArgumentException, IllegalAccessException {
      return this.uploadBufferHelper(highlighterHandler, textureUploader, inRegion, branchTextureRenderer, false);
   }

   private long uploadBufferHelper(DimensionHighlighterHandler highlighterHandler, TextureUploader textureUploader, LeveledRegion<T> inRegion, BranchTextureRenderer branchTextureRenderer, boolean retrying) throws OpenGLException, IllegalArgumentException, IllegalAccessException {
      int internalFormat;
      if (this.colorBufferFormat != -1) {
         boolean isCompressed = this.colorBufferCompressed;
         PoolTextureDirectBufferUnit colorBufferToUpload = this.colorBuffer;
         if (!isCompressed) {
            colorBufferToUpload = this.applyHighlights(highlighterHandler, colorBufferToUpload, true);
         }

         this.updateTextureVersion(this.bufferedTextureVersion);
         if (colorBufferToUpload == null) {
            return 0L;
         } else {
            int length = colorBufferToUpload.getDirectBuffer().remaining();
            this.writeToUnpackPBO(0, colorBufferToUpload);
            internalFormat = this.colorBufferFormat;
            this.textureHasLight = this.bufferHasLight;
            this.colorBufferCompressed = false;
            this.colorBufferFormat = -1;
            this.bufferedTextureVersion = -1;
            boolean subsequent = this.glColorTexture != -1;
            this.bindColorTexture(true);
            OpenGLException.checkGLError();
            long totalEstimatedTime = 0L;
            if (this.unpackPbo[0] == 0) {
               return 0L;
            } else {
               if (isCompressed) {
                  totalEstimatedTime = textureUploader.requestCompressed(this.glColorTexture, this.unpackPbo[0], 3553, 0, internalFormat, 64, 64, 0, 0L, length);
               } else if (subsequent) {
                  totalEstimatedTime = textureUploader.requestSubsequentNormal(this.glColorTexture, this.unpackPbo[0], 3553, 0, 64, 64, 0, 0L, 32993, 32821, 0, 0);
               } else {
                  totalEstimatedTime = textureUploader.requestNormal(this.glColorTexture, this.unpackPbo[0], 3553, 0, internalFormat, 64, 64, 0, 0L, 32993, 32821);
               }

               this.onCacheUploadRequested();
               return totalEstimatedTime;
            }
         }
      } else if (!this.shouldDownloadFromPBO) {
         return this.uploadNonCache(highlighterHandler, textureUploader, branchTextureRenderer);
      } else {
         int glTexture = this.glColorTexture;
         GlStateManager._bindTexture(glTexture);
         int isCompressed = 0;
         this.bindPackPBO();
         if (this.packPbo == 0) {
            this.onDownloadedBuffer((ByteBuffer)null, 0);
            this.endPBODownload(32856, false, false);
            return 0L;
         } else {
            ByteBuffer mappedPBO = PixelBuffers.glMapBuffer(35051, 35000);
            if (mappedPBO != null) {
               OpenGLException.checkGLError();
               this.onDownloadedBuffer(mappedPBO, isCompressed);
               PixelBuffers.glUnmapBuffer(35051);
               OpenGLException.checkGLError();
               this.unbindPackPBO();
               OpenGLException.checkGLError();
               internalFormat = GL11.glGetTexLevelParameteri(3553, 0, 4099);
               OpenGLException.checkGLError();
               this.endPBODownload(internalFormat, isCompressed == 1, true);
               return 0L;
            } else {
               this.unbindPackPBO();
               WorldMap.LOGGER.warn("Failed to map PBO {} {} (uploadBufferHelper).", this.packPbo, retrying);
               PixelBuffers.glDeleteBuffers(this.packPbo);

               while((internalFormat = GL11.glGetError()) != 0) {
                  WorldMap.LOGGER.warn("OpenGL error (uploadBufferHelper): " + internalFormat);
               }

               this.packPbo = 0;
               if (retrying) {
                  this.onDownloadedBuffer((ByteBuffer)null, 0);
                  this.endPBODownload(32856, false, false);
                  return 0L;
               } else {
                  return this.uploadBufferHelper(highlighterHandler, textureUploader, inRegion, branchTextureRenderer, true);
               }
            }
         }
      }
   }

   protected PoolTextureDirectBufferUnit applyHighlights(DimensionHighlighterHandler highlighterHandler, PoolTextureDirectBufferUnit colorBuffer, boolean separateBuffer) {
      return colorBuffer;
   }

   protected abstract void onDownloadedBuffer(ByteBuffer var1, int var2);

   protected void endPBODownload(int format, boolean compressed, boolean success) {
      this.bufferHasLight = this.textureHasLight;
      this.colorBufferFormat = format;
      this.colorBufferCompressed = compressed;
      this.shouldDownloadFromPBO = false;
      this.bufferedTextureVersion = this.textureVersion;
      if (format == -1) {
         throw new RuntimeException("Invalid texture internal format returned by the driver.");
      }
   }

   protected void bindPackPBO() {
      boolean created = false;
      if (this.packPbo == 0) {
         this.packPbo = PixelBuffers.glGenBuffers();
         created = this.packPbo != 0;
      }

      PixelBuffers.glBindBuffer(35051, this.packPbo);
      if (created) {
         PixelBuffers.glBufferData(35051, 16384L, 35041);
         OpenGLException.checkGLError();
      }

   }

   private void bindUnpackPBO(int index) {
      boolean created = false;
      if (this.unpackPbo[index] == 0) {
         this.unpackPbo[index] = PixelBuffers.glGenBuffers();
         created = this.unpackPbo[index] != 0;
      }

      PixelBuffers.glBindBuffer(35052, this.unpackPbo[index]);
      if (created) {
         PixelBuffers.glBufferData(35052, 16384L, 35040);
         OpenGLException.checkGLError();
      }

   }

   protected void unbindPackPBO() {
      PixelBuffers.glBindBuffer(35051, 0);
   }

   private void unbindUnpackPBO() {
      PixelBuffers.glBindBuffer(35052, 0);
   }

   protected void writeToUnpackPBO(int pboIndex, PoolTextureDirectBufferUnit buffer) throws OpenGLException {
      this.writeToUnpackPBO(pboIndex, buffer, false);
   }

   private void writeToUnpackPBO(int pboIndex, PoolTextureDirectBufferUnit buffer, boolean retrying) throws OpenGLException {
      this.bindUnpackPBO(pboIndex);
      if (this.unpackPbo[pboIndex] == 0) {
         this.postBufferWrite(buffer);
      } else {
         ByteBuffer mappedPBO = PixelBuffers.glMapBuffer(35052, 35001);
         if (mappedPBO != null) {
            OpenGLException.checkGLError();
            mappedPBO.put(buffer.getDirectBuffer());
            PixelBuffers.glUnmapBuffer(35052);
            this.unbindUnpackPBO();
            this.postBufferWrite(buffer);
         } else {
            this.unbindUnpackPBO();
            WorldMap.LOGGER.warn("Failed to map PBO {} {} (writeToUnpackPBO).", this.unpackPbo[pboIndex], retrying);
            PixelBuffers.glDeleteBuffers(this.unpackPbo[pboIndex]);
            this.unpackPbo[pboIndex] = 0;

            int error;
            while((error = GL11.glGetError()) != 0) {
               WorldMap.LOGGER.warn("OpenGL error (writeToUnpackPBO): " + error);
            }

            if (!retrying) {
               this.writeToUnpackPBO(pboIndex, buffer, true);
            }

         }
      }
   }

   public void deleteColorBuffer() {
      if (this.colorBuffer != null) {
         if (!WorldMap.textureDirectBufferPool.addToPool(this.colorBuffer)) {
            WorldMap.bufferDeallocator.deallocate(this.colorBuffer.getDirectBuffer(), WorldMapClientConfigUtils.getDebug());
         }

         this.colorBuffer = null;
      }

      this.colorBufferFormat = -1;
      this.bufferedTextureVersion = -1;
   }

   public void deletePBOs() {
      if (this.packPbo > 0) {
         WorldMap.glObjectDeleter.requestBufferToDelete(this.packPbo);
      }

      this.packPbo = 0;

      for(int i = 0; i < this.unpackPbo.length; ++i) {
         if (this.unpackPbo[i] > 0) {
            WorldMap.glObjectDeleter.requestBufferToDelete(this.unpackPbo[i]);
            this.unpackPbo[i] = 0;
         }
      }

   }

   public void writeCacheMapData(DataOutputStream output, byte[] usableBuffer, byte[] integerByteBuffer, LeveledRegion<T> inRegion) throws IOException {
      output.write(this.colorBufferCompressed ? 1 : 0);
      output.writeInt(this.colorBufferFormat);
      ByteBuffer directBuffer = this.colorBuffer.getDirectBuffer();
      int length = directBuffer.remaining();
      output.writeInt(length);
      directBuffer.get(usableBuffer, 0, length);
      directBuffer.position(0);
      output.write(usableBuffer, 0, length);
      output.writeBoolean(this.bufferHasLight);
      long[] heightData = this.heightValues.getData();

      for(int i = 0; i < heightData.length; ++i) {
         output.writeLong(heightData[i]);
      }

      long[] topHeightData = this.topHeightValues.getData();

      for(int i = 0; i < topHeightData.length; ++i) {
         output.writeLong(topHeightData[i]);
      }

      this.saveBiomeIndexStorage(output);
   }

   public void readCacheData(int minorSaveVersion, int majorSaveVersion, DataInputStream input, byte[] usableBuffer, byte[] integerByteBuffer, LeveledRegion<T> inRegion, MapProcessor mapProcessor, int x, int y, boolean leafShouldAffectBranches) throws IOException {
      if (minorSaveVersion >= 7 && (minorSaveVersion < 9 || minorSaveVersion > 11)) {
         this.bufferedTextureVersion = inRegion.getAndResetCachedTextureVersion(x, y);
      } else {
         this.bufferedTextureVersion = 1;
      }

      if (minorSaveVersion == 6) {
         input.readInt();
      }

      int lightLevelsInCache = minorSaveVersion < 3 ? 4 : 1;

      int lightLength;
      int i;
      for(lightLength = 0; lightLength < lightLevelsInCache; ++lightLength) {
         if (lightLength == 0) {
            this.colorBufferCompressed = true;
            if (minorSaveVersion > 1) {
               this.colorBufferCompressed = input.read() == 1;
            }

            this.colorBufferFormat = input.readInt();
         } else {
            if (minorSaveVersion > 1) {
               input.read();
            }

            input.readInt();
         }

         i = input.readInt();
         IOHelper.readToBuffer(usableBuffer, i, input);
         if (lightLength == 0) {
            if (inRegion.getLevel() == 0 && i == 16384 && this.colorBufferCompressed) {
               if (this.colorBuffer == null) {
                  this.colorBuffer = WorldMap.textureDirectBufferPool.get(true);
               }

               this.colorBufferCompressed = false;
               this.colorBufferFormat = 32856;
               inRegion.setShouldCache(true, "broken texture compression fix");
               this.colorBuffer.getDirectBuffer().limit(16384);
            } else {
               if (this.colorBuffer == null) {
                  this.colorBuffer = WorldMap.textureDirectBufferPool.get(false);
               }

               ByteBuffer directBuffer = this.colorBuffer.getDirectBuffer();
               directBuffer.put(usableBuffer, 0, i);
               directBuffer.flip();
            }
         }
      }

      if (minorSaveVersion >= 14) {
         this.bufferHasLight = input.readBoolean();
      } else if (minorSaveVersion > 2) {
         lightLength = input.readInt();
         if (lightLength > 0) {
            IOHelper.readToBuffer(usableBuffer, lightLength, input);
         }

         this.bufferHasLight = false;
      }

      if (minorSaveVersion >= 13) {
         long[] heightData = new long[majorSaveVersion == 0 ? 586 : 1024];

         for(i = 0; i < heightData.length; ++i) {
            heightData[i] = input.readLong();
         }

         int i;
         int i;
         if (majorSaveVersion == 0) {
            ConsistentBitArray oldHeightArray = (ConsistentBitArray)OLD_HEIGHT_VALUES_SUPPORT.get();
            oldHeightArray.setData(heightData);

            for(i = 0; i < 4096; ++i) {
               i = oldHeightArray.get(i);
               if (i >> 8 != 0) {
                  this.putHeight(i, i & 255);
               }
            }
         } else {
            this.heightValues.setData(heightData);
         }

         long[] topHeightData;
         if (minorSaveVersion < 17) {
            topHeightData = this.heightValues.getData();
            long[] topHeightData = new long[this.topHeightValues.getData().length];
            System.arraycopy(topHeightData, 0, topHeightData, 0, topHeightData.length);
            this.topHeightValues.setData(topHeightData);
         } else {
            topHeightData = new long[majorSaveVersion == 0 ? 586 : 1024];

            for(i = 0; i < topHeightData.length; ++i) {
               topHeightData[i] = input.readLong();
            }

            if (majorSaveVersion == 0) {
               ConsistentBitArray oldHeightArray = (ConsistentBitArray)OLD_HEIGHT_VALUES_SUPPORT.get();
               oldHeightArray.setData(topHeightData);

               for(i = 0; i < 4096; ++i) {
                  int oldValue = oldHeightArray.get(i);
                  if (oldValue >> 8 != 0) {
                     this.putTopHeight(i, oldValue & 255);
                  }
               }
            } else {
               this.topHeightValues.setData(topHeightData);
            }
         }

         this.loadBiomeIndexStorage(input, minorSaveVersion, majorSaveVersion);
         if (minorSaveVersion == 16) {
            for(i = 0; i < 64; ++i) {
               input.readLong();
            }
         }
      }

      this.toUpload = true;
   }

   private void saveBiomeIndexStorage(DataOutputStream output) throws IOException {
      Paletted2DFastBitArrayIntStorage biomeIndexStorage = this.biomes == null ? null : this.biomes.getBiomeIndexStorage();
      int paletteSize = biomeIndexStorage == null ? 0 : biomeIndexStorage.getPaletteSize();
      if (paletteSize > 0) {
         if (this.region.getBiomePalette() == null) {
            throw new RuntimeException("saving biomes for a texture in a biomeless region");
         }

         int i;
         int paletteElement;
         if (biomeIndexStorage.getPaletteNonNullCount() <= 1 && biomeIndexStorage.getDefaultValueCount() == 0) {
            i = biomeIndexStorage.getPaletteElement(paletteSize - 1);
            paletteElement = biomeIndexStorage.getPaletteElementCount(paletteSize - 1);
            output.writeInt(1);
            output.writeInt(i);
            output.writeShort(paletteElement);
            output.write(0);
         } else {
            output.writeInt(paletteSize);

            for(i = 0; i < paletteSize; ++i) {
               paletteElement = biomeIndexStorage.getPaletteElement(i);
               output.writeInt(paletteElement);
               if (paletteElement != -1) {
                  output.writeShort(biomeIndexStorage.getPaletteElementCount(i));
               }
            }

            output.write(1);
            biomeIndexStorage.writeData(output);
         }
      } else {
         output.writeInt(0);
      }

   }

   private void loadBiomeIndexStorage(DataInputStream input, int minorSaveVersion, int majorSaveVersion) throws IOException {
      if (minorSaveVersion >= 19) {
         int paletteSize = input.readInt();
         if (paletteSize > 0) {
            int defaultValueCount = 4096;
            FastIntPalette fastIntPalette = FastIntPalette.Builder.begin().setMaxCountPerElement(4096).build();

            int i;
            for(int i = 0; i < paletteSize; ++i) {
               i = input.readInt();
               if (i == -1) {
                  fastIntPalette.addNull();
               } else {
                  int count = input.readShort() & '\uffff';
                  fastIntPalette.append(i, count);
                  defaultValueCount -= count;
               }
            }

            long[] data = new long[1024];
            if (minorSaveVersion != 19 && input.read() != 1) {
               System.arraycopy(ONE_BIOME_PALETTE_DATA, 0, data, 0, data.length);
            } else {
               for(i = 0; i < data.length; ++i) {
                  data[i] = input.readLong();
               }
            }

            ConsistentBitArray dataStorage = new ConsistentBitArray(13, 4096, data);
            Paletted2DFastBitArrayIntStorage biomeIndexStorage = Paletted2DFastBitArrayIntStorage.Builder.begin().setPalette(fastIntPalette).setData(dataStorage).setWidth(64).setHeight(64).setDefaultValueCount(defaultValueCount).setMaxPaletteElements(4096).build();
            if (this.region.getBiomePalette() != null) {
               for(int i = 0; i < fastIntPalette.getSize(); ++i) {
                  int paletteElement = fastIntPalette.get(i, -1);
                  if (paletteElement != -1) {
                     this.region.getBiomePalette().count(paletteElement, true);
                  }
               }

               this.biomes = new RegionTextureBiomes(biomeIndexStorage, this.region.getBiomePalette());
            }
         }
      }

   }

   public void deleteTexturesAndBuffers() {
      int textureToDelete = this.getGlColorTexture();
      this.glColorTexture = -1;
      if (textureToDelete != -1) {
         WorldMap.glObjectDeleter.requestTextureDeletion(textureToDelete);
      }

      this.onTextureDeletion();
      if (this.getColorBuffer() != null) {
         this.deleteColorBuffer();
      }

      this.deletePBOs();
   }

   public PoolTextureDirectBufferUnit getColorBuffer() {
      return this.colorBuffer;
   }

   public ByteBuffer getDirectColorBuffer() {
      return this.colorBuffer == null ? null : this.colorBuffer.getDirectBuffer();
   }

   public void setShouldDownloadFromPBO(boolean shouldDownloadFromPBO) {
      this.shouldDownloadFromPBO = shouldDownloadFromPBO;
   }

   public int getColorBufferFormat() {
      return this.colorBufferFormat;
   }

   public boolean isColorBufferCompressed() {
      return this.colorBufferCompressed;
   }

   public boolean shouldDownloadFromPBO() {
      return this.shouldDownloadFromPBO;
   }

   public int getTimer() {
      return this.timer;
   }

   public void decTimer() {
      --this.timer;
   }

   public void resetTimer() {
      this.timer = 0;
   }

   public final int getGlColorTexture() {
      return this.glColorTexture;
   }

   public void onTextureDeletion() {
      this.updateTextureVersion(0);
   }

   public boolean shouldUpload() {
      return this.toUpload;
   }

   public void setToUpload(boolean value) {
      this.toUpload = value;
   }

   public boolean isCachePrepared() {
      return this.cachePrepared;
   }

   public void setCachePrepared(boolean cachePrepared) {
      this.cachePrepared = cachePrepared;
   }

   public boolean canUpload() {
      return true;
   }

   public boolean isUploaded() {
      return !this.shouldUpload();
   }

   public int getTextureVersion() {
      return this.textureVersion;
   }

   public int getBufferedTextureVersion() {
      return this.bufferedTextureVersion;
   }

   public void setBufferedTextureVersion(int bufferedTextureVersion) {
      this.bufferedTextureVersion = bufferedTextureVersion;
   }

   public LeveledRegion<T> getRegion() {
      return this.region;
   }

   protected void updateTextureVersion(int newVersion) {
      this.textureVersion = newVersion;
   }

   public int getHeight(int x, int z) {
      int index = (z << 6) + x;
      int value = this.heightValues.get(index);
      return value >> 12 == 0 ? 32767 : (value & 4095) << 20 >> 20;
   }

   public void putHeight(int x, int z, int height) {
      int index = (z << 6) + x;
      this.putHeight(index, height);
   }

   public void putHeight(int index, int height) {
      int value = 4096 | height & 4095;
      this.heightValues.set(index, value);
   }

   public void removeHeight(int x, int z) {
      int index = (z << 6) + x;
      this.heightValues.set(index, 0);
   }

   public int getTopHeight(int x, int z) {
      int index = (z << 6) + x;
      int value = this.topHeightValues.get(index);
      return value >> 12 == 0 ? 32767 : (value & 4095) << 20 >> 20;
   }

   public void putTopHeight(int x, int z, int height) {
      int index = (z << 6) + x;
      this.putTopHeight(index, height);
   }

   public void putTopHeight(int index, int height) {
      int value = 4096 | height & 4095;
      this.topHeightValues.set(index, value);
   }

   public void removeTopHeight(int x, int z) {
      int index = (z << 6) + x;
      this.topHeightValues.set(index, 0);
   }

   public void ensureBiomeIndexStorage() {
      if (this.biomes == null) {
         Paletted2DFastBitArrayIntStorage biomeIndexStorage = Paletted2DFastBitArrayIntStorage.Builder.begin().setMaxPaletteElements(4096).setDefaultValue(-1).setWidth(64).setHeight(64).build();
         this.region.ensureBiomePalette();
         this.biomes = new RegionTextureBiomes(biomeIndexStorage, this.region.getBiomePalette());
      }

   }

   public class_5321<class_1959> getBiome(int x, int z) {
      RegionTextureBiomes biomes = this.biomes;
      if (biomes == null) {
         return null;
      } else {
         int biomePaletteIndex = biomes.getBiomeIndexStorage().get(x, z);
         return biomePaletteIndex == -1 ? null : (class_5321)biomes.getRegionBiomePalette().get(biomePaletteIndex);
      }
   }

   public void setBiome(int x, int z, class_5321<class_1959> biome) {
      this.ensureBiomeIndexStorage();
      Paletted2DFastBitArrayIntStorage biomeIndexStorage = this.biomes.getBiomeIndexStorage();
      int currentBiomePaletteIndex = biomeIndexStorage.get(x, z);
      int biomePaletteIndex = biome == null ? -1 : this.region.getBiomePaletteIndex(biome);
      if (biome != null && (biomePaletteIndex == -1 || !biomeIndexStorage.contains(biomePaletteIndex))) {
         biomePaletteIndex = this.region.onBiomeAddedToTexture(biome);
      } else if (biomePaletteIndex == currentBiomePaletteIndex) {
         return;
      }

      try {
         biomeIndexStorage.set(x, z, biomePaletteIndex);
      } catch (Throwable var13) {
         WorldMap.LOGGER.error("weird biomes " + String.valueOf(this.region) + " pixel x:" + x + " z:" + z + " " + currentBiomePaletteIndex + " " + biomePaletteIndex, var13);

         int p;
         for(int i = 0; i < 8; ++i) {
            for(p = 0; p < 8; ++p) {
               if (this.region.getTexture(i, p) == this) {
                  WorldMap.LOGGER.info("texture " + i + " " + p);
               }
            }
         }

         WorldMap.LOGGER.error(biomeIndexStorage.getBiomePaletteDebug());
         int[] realCounts = new int[biomeIndexStorage.getPaletteSize()];

         for(p = 0; p < 64; ++p) {
            String line = "";

            for(int o = 0; o < 64; ++o) {
               int rawIndex = biomeIndexStorage.getRaw(o, p) - 1;
               line = line + " " + rawIndex;
               if (rawIndex >= 0 && rawIndex < realCounts.length) {
                  int var10002 = realCounts[rawIndex]++;
               }
            }

            WorldMap.LOGGER.error(line);
         }

         WorldMap.LOGGER.error("real counts: " + Arrays.toString(realCounts));
         WorldMap.LOGGER.error("suppressed exception", var13);
         this.region.setShouldCache(true, "broken cache biome data");
         if (this.region.getLevel() > 0) {
            this.textureVersion = (new Random()).nextInt();
            ((BranchLeveledRegion)this.region).setShouldCheckForUpdatesRecursive(true);
         } else {
            ((MapRegion)this.region).setCacheHashCode(0);
         }

         this.biomes = null;
      }

      if (currentBiomePaletteIndex != -1 && !biomeIndexStorage.contains(currentBiomePaletteIndex)) {
         this.region.onBiomeRemovedFromTexture(currentBiomePaletteIndex);
      }

   }

   public boolean getTextureHasLight() {
      return this.textureHasLight;
   }

   public void addDebugLines(List<String> debugLines) {
      boolean var10001 = this.shouldUpload();
      debugLines.add("shouldUpload: " + var10001 + " timer: " + this.getTimer());
      debugLines.add(String.format("buffer exists: %s", this.getColorBuffer() != null));
      int var2 = this.getGlColorTexture();
      debugLines.add("glColorTexture: " + var2 + " textureHasLight: " + this.textureHasLight);
      debugLines.add("cachePrepared: " + this.isCachePrepared());
      debugLines.add("textureVersion: " + this.textureVersion);
      debugLines.add("colorBufferFormat: " + this.colorBufferFormat);
      if (this.biomes != null) {
         debugLines.add(this.biomes.getBiomeIndexStorage().getBiomePaletteDebug());
      }

   }

   protected void onCacheUploadRequested() {
   }

   public boolean shouldBeUsedForBranchUpdate(int usedVersion) {
      return (this.shouldHaveContentForBranchUpdate() ? this.textureVersion : 0) != usedVersion;
   }

   public boolean shouldHaveContentForBranchUpdate() {
      return true;
   }

   public boolean shouldIncludeInCache() {
      return true;
   }

   public RegionTextureBiomes getBiomes() {
      return this.biomes;
   }

   public void resetBiomes() {
      this.biomes = null;
   }

   public abstract boolean hasSourceData();

   public abstract void preUpload(MapProcessor var1, BlockTintProvider var2, OverlayManager var3, LeveledRegion<T> var4, boolean var5, BlockStateShortShapeCache var6, MapUpdateFastConfig var7);

   public abstract void postUpload(MapProcessor var1, LeveledRegion<T> var2, boolean var3);

   protected abstract long uploadNonCache(DimensionHighlighterHandler var1, TextureUploader var2, BranchTextureRenderer var3);

   public boolean getBufferHasLight() {
      return this.bufferHasLight;
   }

   static {
      ConsistentBitArray dataStorage = new ConsistentBitArray(13, 4096);

      for(int i = 0; i < 4096; ++i) {
         dataStorage.set(i, 1);
      }

      ONE_BIOME_PALETTE_DATA = dataStorage.getData();
      OLD_HEIGHT_VALUES_SUPPORT = ThreadLocal.withInitial(() -> {
         return new ConsistentBitArray(9, 4096);
      });
   }
}
