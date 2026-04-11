package xaero.map.region;

import java.util.Hashtable;
import xaero.map.file.RegionDetection;
import xaero.map.highlight.RegionHighlightExistenceTracker;
import xaero.map.util.linked.LinkedChain;
import xaero.map.world.MapDimension;

public class MapLayer {
   private final MapDimension mapDimension;
   private final LeveledRegionManager mapRegions;
   private final RegionHighlightExistenceTracker regionHighlightExistenceTracker;
   private final Hashtable<Integer, Hashtable<Integer, RegionDetection>> detectedRegions;
   private final Hashtable<Integer, Hashtable<Integer, RegionDetection>> completeDetectedRegions;
   private final LinkedChain<RegionDetection> completeDetectedRegionsLinked;
   private int caveStart;

   public MapLayer(MapDimension mapDimension, RegionHighlightExistenceTracker regionHighlightExistenceTracker) {
      this.mapDimension = mapDimension;
      this.mapRegions = new LeveledRegionManager();
      this.regionHighlightExistenceTracker = regionHighlightExistenceTracker;
      this.detectedRegions = new Hashtable();
      this.completeDetectedRegions = new Hashtable();
      this.completeDetectedRegionsLinked = new LinkedChain();
   }

   public boolean regionDetectionExists(int x, int z) {
      return this.getRegionDetection(x, z) != null;
   }

   public void addRegionDetection(RegionDetection regionDetection) {
      synchronized(this.detectedRegions) {
         Hashtable<Integer, RegionDetection> column = (Hashtable)this.detectedRegions.get(regionDetection.getRegionX());
         if (column == null) {
            this.detectedRegions.put(regionDetection.getRegionX(), column = new Hashtable());
         }

         column.put(regionDetection.getRegionZ(), regionDetection);
         this.tryAddingToCompleteRegionDetection(regionDetection);
      }
   }

   public RegionDetection getCompleteRegionDetection(int x, int z) {
      if (this.mapDimension.isUsingWorldSave()) {
         return this.mapDimension.getWorldSaveRegionDetection(x, z);
      } else {
         Hashtable<Integer, RegionDetection> column = (Hashtable)this.completeDetectedRegions.get(x);
         return column != null ? (RegionDetection)column.get(z) : null;
      }
   }

   private boolean completeRegionDetectionContains(RegionDetection regionDetection) {
      return this.getCompleteRegionDetection(regionDetection.getRegionX(), regionDetection.getRegionZ()) != null;
   }

   public void tryAddingToCompleteRegionDetection(RegionDetection regionDetection) {
      if (!this.completeRegionDetectionContains(regionDetection)) {
         if (this.mapDimension.isUsingWorldSave()) {
            this.mapDimension.addWorldSaveRegionDetection(regionDetection);
         } else {
            synchronized(this.completeDetectedRegions) {
               Hashtable<Integer, RegionDetection> column = (Hashtable)this.completeDetectedRegions.get(regionDetection.getRegionX());
               if (column == null) {
                  this.completeDetectedRegions.put(regionDetection.getRegionX(), column = new Hashtable());
               }

               column.put(regionDetection.getRegionZ(), regionDetection);
               this.completeDetectedRegionsLinked.add(regionDetection);
            }
         }
      }
   }

   public RegionDetection getRegionDetection(int x, int z) {
      Hashtable<Integer, RegionDetection> column = (Hashtable)this.detectedRegions.get(x);
      RegionDetection result = null;
      if (column != null) {
         result = (RegionDetection)column.get(z);
      }

      if (result == null) {
         RegionDetection worldSaveDetection = this.mapDimension.getWorldSaveRegionDetection(x, z);
         if (worldSaveDetection != null) {
            result = new RegionDetection(worldSaveDetection.getWorldId(), worldSaveDetection.getDimId(), worldSaveDetection.getMwId(), worldSaveDetection.getRegionX(), worldSaveDetection.getRegionZ(), worldSaveDetection.getRegionFile(), worldSaveDetection.getInitialVersion(), worldSaveDetection.isHasHadTerrain());
            this.addRegionDetection(result);
            return result;
         }
      } else if (result.isRemoved()) {
         return null;
      }

      return result;
   }

   public void removeRegionDetection(int x, int z) {
      if (this.mapDimension.getWorldSaveRegionDetection(x, z) != null) {
         RegionDetection regionDetection = this.getRegionDetection(x, z);
         if (regionDetection != null) {
            regionDetection.setRemoved(true);
         }

      } else {
         synchronized(this.detectedRegions) {
            Hashtable<Integer, RegionDetection> column = (Hashtable)this.detectedRegions.get(x);
            if (column != null) {
               column.remove(z);
               if (column.isEmpty()) {
                  this.detectedRegions.remove(x);
               }
            }

         }
      }
   }

   public RegionHighlightExistenceTracker getRegionHighlightExistenceTracker() {
      return this.regionHighlightExistenceTracker;
   }

   public LeveledRegionManager getMapRegions() {
      return this.mapRegions;
   }

   public Hashtable<Integer, Hashtable<Integer, RegionDetection>> getDetectedRegions() {
      return this.detectedRegions;
   }

   public Iterable<RegionDetection> getLinkedCompleteWorldSaveDetectedRegions() {
      return (Iterable)(this.mapDimension.isUsingWorldSave() ? this.mapDimension.getLinkedWorldSaveDetectedRegions() : this.completeDetectedRegionsLinked);
   }

   public void preDetection() {
      this.detectedRegions.clear();
      this.completeDetectedRegions.clear();
      this.completeDetectedRegionsLinked.reset();
   }

   public int getCaveStart() {
      return this.caveStart;
   }

   public void setCaveStart(int caveStart) {
      this.caveStart = caveStart;
   }
}
