package xaero.map.effects;

import net.minecraft.class_1291;
import net.minecraft.class_4081;
import net.minecraft.class_6880;

public class Effects {
   public static WorldMapStatusEffect NO_WORLD_MAP_UNHELD = null;
   public static WorldMapStatusEffect NO_WORLD_MAP_HARMFUL_UNHELD = null;
   public static WorldMapStatusEffect NO_CAVE_MAPS_UNHELD = null;
   public static WorldMapStatusEffect NO_CAVE_MAPS_HARMFUL_UNHELD = null;
   public static class_6880<class_1291> NO_WORLD_MAP = null;
   public static class_6880<class_1291> NO_WORLD_MAP_HARMFUL = null;
   public static class_6880<class_1291> NO_CAVE_MAPS = null;
   public static class_6880<class_1291> NO_CAVE_MAPS_HARMFUL = null;

   public static void init() {
      if (NO_WORLD_MAP == null) {
         NO_WORLD_MAP_UNHELD = new NoWorldMapEffect(class_4081.field_18273);
         NO_WORLD_MAP_HARMFUL_UNHELD = new NoWorldMapEffect(class_4081.field_18272);
         NO_CAVE_MAPS_UNHELD = new NoCaveMapsEffect(class_4081.field_18273);
         NO_CAVE_MAPS_HARMFUL_UNHELD = new NoCaveMapsEffect(class_4081.field_18272);
      }
   }
}
