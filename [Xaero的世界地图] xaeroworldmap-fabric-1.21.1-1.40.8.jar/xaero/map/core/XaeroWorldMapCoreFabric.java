package xaero.map.core;

import net.minecraft.class_2678;
import net.minecraft.class_634;
import xaero.map.WorldMap;
import xaero.map.WorldMapFabric;

public class XaeroWorldMapCoreFabric {
   public static void onPlayNetHandler(class_634 netHandler, class_2678 packet) {
      if (WorldMap.INSTANCE != null) {
         ((WorldMapFabric)WorldMap.INSTANCE).tryLoadLater();
      }

      if (WorldMap.loaded) {
         if (WorldMap.crashHandler.getCrashedBy() == null) {
            XaeroWorldMapCore.onPlayNetHandler(netHandler, packet);
         }
      }
   }

   public static void onMinecraftRunTick() {
      if (WorldMap.INSTANCE != null) {
         ((WorldMapFabric)WorldMap.INSTANCE).tryLoadLater();
      }

      XaeroWorldMapCore.onMinecraftRunTick();
   }
}
