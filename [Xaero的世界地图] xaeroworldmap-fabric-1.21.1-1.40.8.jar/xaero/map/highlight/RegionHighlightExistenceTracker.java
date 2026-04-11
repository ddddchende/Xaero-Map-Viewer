package xaero.map.highlight;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import xaero.map.region.BranchLeveledRegion;
import xaero.map.region.LeveledRegion;
import xaero.map.world.MapDimension;

public class RegionHighlightExistenceTracker {
   private final MapDimension mapDimension;
   private final int caveLayer;
   private final LongSet regionsToTrackExistenceOf;

   public RegionHighlightExistenceTracker(MapDimension mapDimension, int caveLayer) {
      this.mapDimension = mapDimension;
      this.caveLayer = caveLayer;
      this.regionsToTrackExistenceOf = new LongOpenHashSet();
   }

   private void requestBranchUpdates(int regionX, int regionZ) {
      for(int i = 1; i <= 3; ++i) {
         int leveledRegionX = regionX >> i;
         int leveledRegionZ = regionZ >> i;
         LeveledRegion<?> leveledParent = this.mapDimension.getLayeredMapRegions().get(this.caveLayer, leveledRegionX, leveledRegionZ, i);
         if (leveledParent != null) {
            ((BranchLeveledRegion)leveledParent).setShouldCheckForUpdatesRecursive(true);
            break;
         }
      }

   }

   public void onClearCachedHash(int regionX, int regionZ) {
      long key = DimensionHighlighterHandler.getKey(regionX, regionZ);
      if (this.regionsToTrackExistenceOf.remove(key)) {
         this.requestBranchUpdates(regionX, regionZ);
      }

   }

   public void onClearCachedHashes() {
      this.regionsToTrackExistenceOf.forEach((key) -> {
         this.requestBranchUpdates(DimensionHighlighterHandler.getXFromKey(key), DimensionHighlighterHandler.getZFromKey(key));
      });
      this.regionsToTrackExistenceOf.clear();
   }

   public void track(int regionX, int regionZ) {
      this.regionsToTrackExistenceOf.add(DimensionHighlighterHandler.getKey(regionX, regionZ));
   }

   public void stopTracking(int regionX, int regionZ) {
      this.regionsToTrackExistenceOf.remove(DimensionHighlighterHandler.getKey(regionX, regionZ));
   }
}
