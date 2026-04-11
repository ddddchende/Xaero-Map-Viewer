package xaero.map;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import xaero.map.config.util.WorldMapClientConfigUtils;
import xaero.map.region.LayeredRegionManager;
import xaero.map.region.LeveledRegion;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;

public class MapLimiter {
   private static final int MIN_LIMIT = 53;
   private static final int DEFAULT_LIMIT = 203;
   private static final int MAX_LIMIT = 403;
   private int availableVRAM = -1;
   private int mostRegionsAtATime;
   private IntBuffer vramBuffer = BufferUtils.createByteBuffer(64).asIntBuffer();
   private int driverType = -1;
   private ArrayList<MapDimension> workingDimList = new ArrayList();

   public int getAvailableVRAM() {
      return this.availableVRAM;
   }

   private void determineDriverType() {
      if (GL.getCapabilities().GL_NVX_gpu_memory_info) {
         this.driverType = 0;
      } else if (GL.getCapabilities().GL_ATI_meminfo) {
         this.driverType = 1;
      } else {
         this.driverType = 2;
      }

   }

   public void updateAvailableVRAM() {
      if (this.driverType == -1) {
         this.determineDriverType();
      }

      switch(this.driverType) {
      case 0:
         this.vramBuffer.clear();
         GL11.glGetIntegerv(36937, this.vramBuffer);
         this.availableVRAM = this.vramBuffer.get(0);
         break;
      case 1:
         this.vramBuffer.clear();
         GL11.glGetIntegerv(34812, this.vramBuffer);
         this.availableVRAM = this.vramBuffer.get(0);
      }

   }

   public int getMostRegionsAtATime() {
      return this.mostRegionsAtATime;
   }

   public void setMostRegionsAtATime(int mostRegionsAtATime) {
      this.mostRegionsAtATime = mostRegionsAtATime;
   }

   public void applyLimit(MapWorld mapWorld, MapProcessor mapProcessor) {
      int limit = Math.max(this.mostRegionsAtATime, 53);
      int vramDetermined = false;
      int loadedCount = 0;
      this.workingDimList.clear();
      mapWorld.getDimensions(this.workingDimList);

      MapDimension dim;
      for(Iterator var6 = this.workingDimList.iterator(); var6.hasNext(); loadedCount += dim.getLayeredMapRegions().loadedCount()) {
         dim = (MapDimension)var6.next();
      }

      int vramDetermined;
      if (this.availableVRAM != -1) {
         if (this.availableVRAM < 204800) {
            vramDetermined = Math.min(403, loadedCount) - 6;
         } else {
            if (loadedCount <= 403) {
               return;
            }

            vramDetermined = 397;
         }
      } else {
         vramDetermined = loadedCount > 203 ? 197 : loadedCount;
      }

      if (vramDetermined > limit) {
         limit = vramDetermined;
      }

      int count = 0;
      mapProcessor.pushRenderPause(false, true);
      LeveledRegion<?> nextToLoad = mapProcessor.getMapSaveLoad().getNextToLoadByViewing();
      int currentDimIndex = this.workingDimList.indexOf(mapWorld.getCurrentDimension());
      int dimCount = 0;
      int dimTotal = this.workingDimList.size();

      for(int d = (currentDimIndex + 1) % dimTotal; dimCount < dimTotal && loadedCount > limit; d = (d + 1) % dimTotal) {
         MapDimension dimension = (MapDimension)this.workingDimList.get(d);
         LayeredRegionManager regions = dimension.getLayeredMapRegions();

         for(int i = 0; i < regions.loadedCount() && loadedCount > limit; ++i) {
            LeveledRegion<?> region = regions.getLoadedRegion(i);
            if (region.isLoaded() && !region.shouldBeProcessed() && region.activeBranchUpdateReferences == 0) {
               region.onLimiterRemoval(mapProcessor);
               region.deleteTexturesAndBuffers();
               mapProcessor.getMapSaveLoad().removeToCache(region);
               region.afterLimiterRemoval(mapProcessor);
               if (region == nextToLoad) {
                  mapProcessor.getMapSaveLoad().setNextToLoadByViewing((LeveledRegion)null);
               }

               ++count;
               --i;
               --loadedCount;
            }
         }

         ++dimCount;
      }

      if (count > 0 && WorldMapClientConfigUtils.getDebug()) {
         WorldMap.LOGGER.info("Unloaded " + count + " world map regions!");
      }

      mapProcessor.popRenderPause(false, true);
   }

   public void onSessionFinalized() {
      this.workingDimList.clear();
   }
}
