package xaero.map.region;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import xaero.map.highlight.RegionHighlightExistenceTracker;
import xaero.map.world.MapDimension;

public class LayeredRegionManager {
   private final MapDimension mapDimension;
   private final Int2ObjectMap<MapLayer> mapLayers;
   private Set<LeveledRegion<?>> regionsListAll;
   private List<LeveledRegion<?>> regionsListLoaded;

   public LayeredRegionManager(MapDimension mapDimension) {
      this.mapDimension = mapDimension;
      this.mapLayers = new Int2ObjectOpenHashMap();
      this.regionsListAll = new HashSet();
      this.regionsListLoaded = new ArrayList();
   }

   public void putLeaf(int X, int Z, MapRegion leaf) {
      this.getLayer(leaf.caveLayer).getMapRegions().putLeaf(X, Z, leaf);
   }

   public MapRegion getLeaf(int caveLayer, int X, int Z) {
      return this.getLayer(caveLayer).getMapRegions().getLeaf(X, Z);
   }

   public LeveledRegion<?> get(int caveLayer, int leveledX, int leveledZ, int level) {
      return this.getLayer(caveLayer).getMapRegions().get(leveledX, leveledZ, level);
   }

   public boolean remove(int caveLayer, int leveledX, int leveledZ, int level) {
      return this.getLayer(caveLayer).getMapRegions().remove(leveledX, leveledZ, level);
   }

   public MapLayer getLayer(int caveLayer) {
      synchronized(this.mapLayers) {
         MapLayer mapLayer = (MapLayer)this.mapLayers.get(caveLayer);
         if (mapLayer == null) {
            this.mapLayers.put(caveLayer, mapLayer = new MapLayer(this.mapDimension, new RegionHighlightExistenceTracker(this.mapDimension, caveLayer)));
         }

         return mapLayer;
      }
   }

   public void clear() {
      synchronized(this.mapLayers) {
         this.mapLayers.clear();
      }

      synchronized(this.regionsListAll) {
         this.regionsListAll.clear();
      }

      synchronized(this.regionsListLoaded) {
         this.regionsListLoaded.clear();
      }
   }

   public int loadedCount() {
      return this.regionsListLoaded.size();
   }

   public void removeListRegion(LeveledRegion<?> reg) {
      synchronized(this.regionsListAll) {
         this.regionsListAll.remove(reg);
      }
   }

   public void addListRegion(LeveledRegion<?> reg) {
      synchronized(this.regionsListAll) {
         this.regionsListAll.add(reg);
      }
   }

   public void bumpLoadedRegion(MapRegion reg) {
      this.bumpLoadedRegion((LeveledRegion)reg);
   }

   public void bumpLoadedRegion(LeveledRegion<?> reg) {
      synchronized(this.regionsListLoaded) {
         if (this.regionsListLoaded.remove(reg)) {
            this.regionsListLoaded.add(reg);
         }

      }
   }

   public List<LeveledRegion<?>> getLoadedListUnsynced() {
      return this.regionsListLoaded;
   }

   public LeveledRegion<?> getLoadedRegion(int index) {
      synchronized(this.regionsListLoaded) {
         return (LeveledRegion)this.regionsListLoaded.get(index);
      }
   }

   public void addLoadedRegion(LeveledRegion<?> reg) {
      synchronized(this.regionsListLoaded) {
         this.regionsListLoaded.add(reg);
      }
   }

   public void removeLoadedRegion(LeveledRegion<?> reg) {
      synchronized(this.regionsListLoaded) {
         this.regionsListLoaded.remove(reg);
      }
   }

   public int size() {
      return this.regionsListAll.size();
   }

   public Set<LeveledRegion<?>> getUnsyncedSet() {
      return this.regionsListAll;
   }

   public void onClearCachedHighlightHash(int regionX, int regionZ) {
      synchronized(this.mapLayers) {
         this.mapLayers.forEach((i, layer) -> {
            layer.getRegionHighlightExistenceTracker().onClearCachedHash(regionX, regionZ);
         });
      }
   }

   public void onClearCachedHighlightHashes() {
      synchronized(this.mapLayers) {
         this.mapLayers.forEach((i, layer) -> {
            layer.getRegionHighlightExistenceTracker().onClearCachedHashes();
         });
      }
   }

   public void applyToEachLoadedLayer(BiConsumer<Integer, MapLayer> consumer) {
      synchronized(this.mapLayers) {
         Int2ObjectMap var10000 = this.mapLayers;
         Objects.requireNonNull(consumer);
         var10000.forEach(consumer::accept);
      }
   }

   public void preDetection() {
      synchronized(this.mapLayers) {
         this.mapLayers.forEach((i, layer) -> {
            layer.preDetection();
         });
      }
   }
}
