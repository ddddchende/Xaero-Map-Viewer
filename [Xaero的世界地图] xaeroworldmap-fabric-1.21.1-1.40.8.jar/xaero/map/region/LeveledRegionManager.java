package xaero.map.region;

import java.util.HashMap;

public class LeveledRegionManager {
   public static final int MAX_LEVEL = 3;
   private HashMap<Integer, HashMap<Integer, LeveledRegion<?>>> regionTextureMap = new HashMap();

   public void putLeaf(int X, int Z, MapRegion leaf) {
      int maxLevelX = X >> 3;
      int maxLevelZ = Z >> 3;
      HashMap column;
      synchronized(this.regionTextureMap) {
         column = (HashMap)this.regionTextureMap.get(maxLevelX);
         if (column == null) {
            column = new HashMap();
            this.regionTextureMap.put(maxLevelX, column);
         }
      }

      Object rootBranch;
      synchronized(column) {
         rootBranch = (LeveledRegion)column.get(maxLevelZ);
         if (rootBranch == null) {
            rootBranch = new BranchLeveledRegion(leaf.getWorldId(), leaf.getDimId(), leaf.getMwId(), leaf.getDim(), 3, maxLevelX, maxLevelZ, leaf.caveLayer, (BranchLeveledRegion)null);
            column.put(maxLevelZ, rootBranch);
            leaf.getDim().getLayeredMapRegions().addListRegion((LeveledRegion)rootBranch);
         }
      }

      if (!(rootBranch instanceof MapRegion)) {
         ((LeveledRegion)rootBranch).putLeaf(X, Z, leaf);
      }

   }

   public MapRegion getLeaf(int X, int Z) {
      return (MapRegion)this.get(X, Z, 0);
   }

   public LeveledRegion<?> get(int leveledX, int leveledZ, int level) {
      if (level > 3) {
         throw new RuntimeException(new IllegalArgumentException());
      } else {
         int maxLevelX = leveledX >> 3 - level;
         int maxLevelZ = leveledZ >> 3 - level;
         HashMap column;
         synchronized(this.regionTextureMap) {
            column = (HashMap)this.regionTextureMap.get(maxLevelX);
         }

         if (column == null) {
            return null;
         } else {
            LeveledRegion rootBranch;
            synchronized(column) {
               rootBranch = (LeveledRegion)column.get(maxLevelZ);
            }

            if (rootBranch == null) {
               return null;
            } else {
               return level == 3 ? rootBranch : rootBranch.get(leveledX, leveledZ, level);
            }
         }
      }
   }

   public boolean remove(int leveledX, int leveledZ, int level) {
      if (level > 3) {
         throw new RuntimeException(new IllegalArgumentException());
      } else {
         int maxLevelX = leveledX >> 3 - level;
         int maxLevelZ = leveledZ >> 3 - level;
         HashMap column;
         synchronized(this.regionTextureMap) {
            column = (HashMap)this.regionTextureMap.get(maxLevelX);
         }

         if (column == null) {
            return false;
         } else {
            LeveledRegion rootBranch;
            synchronized(column) {
               rootBranch = (LeveledRegion)column.get(maxLevelZ);
            }

            if (rootBranch == null) {
               return false;
            } else if (!(rootBranch instanceof MapRegion)) {
               return rootBranch.remove(leveledX, leveledZ, level);
            } else {
               synchronized(column) {
                  column.remove(maxLevelZ);
                  return true;
               }
            }
         }
      }
   }
}
