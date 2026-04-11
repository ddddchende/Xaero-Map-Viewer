package xaero.map.mcworld;

import net.minecraft.class_310;
import net.minecraft.class_638;

public class WorldMapClientWorldDataHelper {
   public static WorldMapClientWorldData getCurrentWorldData() {
      return getWorldData(class_310.method_1551().field_1687);
   }

   public static synchronized WorldMapClientWorldData getWorldData(class_638 clientWorld) {
      if (clientWorld == null) {
         return null;
      } else {
         IWorldMapClientWorld inter = (IWorldMapClientWorld)clientWorld;
         WorldMapClientWorldData worldmapWorldData = inter.getXaero_worldmapData();
         if (worldmapWorldData == null) {
            inter.setXaero_worldmapData(worldmapWorldData = new WorldMapClientWorldData(clientWorld));
         }

         return worldmapWorldData;
      }
   }
}
