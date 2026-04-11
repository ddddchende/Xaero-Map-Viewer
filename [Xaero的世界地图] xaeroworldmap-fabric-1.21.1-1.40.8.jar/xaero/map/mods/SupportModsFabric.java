package xaero.map.mods;

import xaero.map.WorldMap;

public class SupportModsFabric extends SupportMods {
   public static SupportAmecs amecs = null;

   public void load() {
      super.load();

      try {
         Class mmClassTest = Class.forName("de.siphalor.amecs.api.KeyModifiers");
         amecs = new SupportAmecs(WorldMap.LOGGER);
      } catch (ClassNotFoundException var2) {
      }

   }

   public static boolean amecs() {
      return amecs != null;
   }
}
