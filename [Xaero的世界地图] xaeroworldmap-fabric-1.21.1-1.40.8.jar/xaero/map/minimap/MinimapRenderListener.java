package xaero.map.minimap;

import java.util.ArrayList;
import net.minecraft.class_1657;
import net.minecraft.class_310;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.common.config.Config;
import xaero.lib.common.config.single.SingleConfigManager;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.config.primary.option.WorldMapPrimaryClientConfigOptions;
import xaero.map.misc.Misc;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapRegion;

public class MinimapRenderListener {
   private ArrayList<MapRegion> regionBuffer = new ArrayList();
   private boolean shouldRequestLoading;
   private boolean playerMoving;
   private int renderedCaveLayer;
   private boolean isCacheOnlyMode;
   private int globalRegionCacheHashCode;
   private boolean reloadEverything;
   private int globalVersion;
   private int globalReloadVersion;
   private int globalCaveStart;
   private int globalCaveDepth;
   private MapRegion prevRegion;

   public void init(MapProcessor mapProcessor, int flooredMapCameraX, int flooredMapCameraZ) {
      mapProcessor.updateCaveStart();
      class_1657 player = class_310.method_1551().field_1724;
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      SingleConfigManager<Config> primaryConfigManager = configManager.getPrimaryConfigManager();
      this.playerMoving = player.field_6014 != player.method_23317() || player.field_6036 != player.method_23318() || player.field_5969 != player.method_23321();
      this.renderedCaveLayer = mapProcessor.getCurrentCaveLayer();
      this.isCacheOnlyMode = mapProcessor.getMapWorld().getCurrentDimension().isCacheOnlyMode(mapProcessor.getWorldDimensionTypeRegistry());
      this.globalRegionCacheHashCode = WorldMap.settings.getRegionCacheHashCode();
      this.reloadEverything = (Boolean)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.RELOAD_VIEWED);
      this.globalVersion = (Integer)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.GLOBAL_VERSION);
      this.globalReloadVersion = (Integer)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.RELOAD_VIEWED_VERSION);
      this.globalCaveStart = mapProcessor.getMapWorld().getCurrentDimension().getLayeredMapRegions().getLayer(this.renderedCaveLayer).getCaveStart();
      this.globalCaveDepth = (Integer)configManager.getEffective(WorldMapProfiledConfigOptions.CAVE_MODE_DEPTH);
      this.prevRegion = null;
      this.shouldRequestLoading = false;
      LeveledRegion<?> nextToLoad = mapProcessor.getMapSaveLoad().getNextToLoadByViewing();
      this.shouldRequestLoading = nextToLoad == null || nextToLoad.shouldAllowAnotherRegionToLoad();
      int comparisonChunkX = (flooredMapCameraX >> 4) - 16;
      int comparisonChunkZ = (flooredMapCameraZ >> 4) - 16;
      LeveledRegion.setComparison(comparisonChunkX, comparisonChunkZ, 0, comparisonChunkX, comparisonChunkZ);
   }

   public void beforeMinimapRender(MapRegion region) {
      if (this.shouldRequestLoading) {
         if (region != null && region != this.prevRegion) {
            synchronized(region) {
               int regionHashCode = region.getCacheHashCode();
               if (region.canRequestReload_unsynced() && (region.getLoadState() == 0 || (region.getLoadState() == 4 || region.getLoadState() == 2 && region.isBeingWritten()) && (!this.isCacheOnlyMode && (this.reloadEverything && region.getReloadVersion() != this.globalReloadVersion || regionHashCode != this.globalRegionCacheHashCode || !this.playerMoving && region.caveStartOutdated(this.globalCaveStart, this.globalCaveDepth) || region.getVersion() != this.globalVersion || region.getLoadState() != 2 && region.shouldCache()) || (region.isMetaLoaded() || region.getLoadState() != 0 || !region.hasHadTerrain()) && region.getHighlightsHash() != region.getDim().getHighlightHandler().getRegionHash(region.getRegionX(), region.getRegionZ()))) && !this.regionBuffer.contains(region)) {
                  region.calculateSortingChunkDistance();
                  Misc.addToListOfSmallest(10, this.regionBuffer, region);
               }
            }
         }

         this.prevRegion = region;
      }
   }

   public void finalize(MapProcessor mapProcessor) {
      int toRequest = 1;
      int counter = 0;
      LeveledRegion<?> nextToLoad = mapProcessor.getMapSaveLoad().getNextToLoadByViewing();

      for(int i = 0; i < this.regionBuffer.size() && counter < toRequest; ++i) {
         MapRegion region = (MapRegion)this.regionBuffer.get(i);
         if (region != nextToLoad || this.regionBuffer.size() <= 1) {
            synchronized(region) {
               if (region.canRequestReload_unsynced()) {
                  if (region.getLoadState() == 2) {
                     region.requestRefresh(mapProcessor);
                  } else {
                     mapProcessor.getMapSaveLoad().requestLoad(region, "Minimap listener", false);
                  }

                  if (counter == 0) {
                     mapProcessor.getMapSaveLoad().setNextToLoadByViewing(region);
                  }

                  ++counter;
                  if (region.getLoadState() == 4) {
                     break;
                  }
               }
            }
         }
      }

      this.regionBuffer.clear();
   }

   public int getRenderedCaveLayer() {
      return this.renderedCaveLayer;
   }

   public boolean shouldRequestLoading() {
      return this.shouldRequestLoading;
   }

   public boolean isPlayerMoving() {
      return this.playerMoving;
   }

   public boolean isCacheOnlyMode() {
      return this.isCacheOnlyMode;
   }

   public int getGlobalRegionCacheHashCode() {
      return this.globalRegionCacheHashCode;
   }

   public boolean isReloadEverything() {
      return this.reloadEverything;
   }

   public int getGlobalVersion() {
      return this.globalVersion;
   }

   public int getGlobalReloadVersion() {
      return this.globalReloadVersion;
   }

   public int getGlobalCaveStart() {
      return this.globalCaveStart;
   }

   public int getGlobalCaveDepth() {
      return this.globalCaveDepth;
   }
}
