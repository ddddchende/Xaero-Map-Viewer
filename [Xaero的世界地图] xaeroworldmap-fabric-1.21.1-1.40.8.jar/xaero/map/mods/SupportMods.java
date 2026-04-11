package xaero.map.mods;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import xaero.map.WorldMap;
import xaero.map.mods.pac.SupportOpenPartiesAndClaims;

public class SupportMods {
   public static SupportXaeroMinimap xaeroMinimap = null;
   public static SupportOpenPartiesAndClaims xaeroPac = null;
   public static boolean optifine;
   public static boolean vivecraft;
   public static boolean iris;
   public static SupportIris supportIris;
   public static SupportFramedBlocks supportFramedBlocks = null;

   public static boolean minimap() {
      return xaeroMinimap != null && xaeroMinimap.modMain != null;
   }

   public static boolean framedBlocks() {
      return supportFramedBlocks != null;
   }

   public static boolean pac() {
      return xaeroPac != null;
   }

   public void load() {
      Class mmClassTest;
      try {
         mmClassTest = Class.forName("xaero.common.IXaeroMinimap");
         xaeroMinimap = new SupportXaeroMinimap();
         xaeroMinimap.register();
      } catch (ClassNotFoundException var14) {
      }

      try {
         mmClassTest = Class.forName("xaero.pac.OpenPartiesAndClaims");
         xaeroPac = new SupportOpenPartiesAndClaims();
         xaeroPac.register();
         WorldMap.LOGGER.info("Xaero's WorldMap Mod: Open Parties And Claims found!");
      } catch (ClassNotFoundException var13) {
      }

      try {
         mmClassTest = Class.forName("optifine.Patcher");
         optifine = true;
         WorldMap.LOGGER.info("Optifine!");
      } catch (ClassNotFoundException var12) {
         optifine = false;
         WorldMap.LOGGER.info("No Optifine!");
      }

      try {
         mmClassTest = Class.forName("org.vivecraft.api.VRData");
         vivecraft = true;

         try {
            Class<?> vrStateClass = Class.forName("org.vivecraft.VRState");
            Method checkVRMethod = vrStateClass.getDeclaredMethod("checkVR");
            vivecraft = (Boolean)checkVRMethod.invoke((Object)null);
         } catch (ClassNotFoundException var6) {
         } catch (NoSuchMethodException var7) {
         } catch (IllegalAccessException var8) {
         } catch (IllegalArgumentException var9) {
         } catch (InvocationTargetException var10) {
         }
      } catch (ClassNotFoundException var11) {
      }

      if (vivecraft) {
         WorldMap.LOGGER.info("Xaero's World Map: Vivecraft!");
      } else {
         WorldMap.LOGGER.info("Xaero's World Map: No Vivecraft!");
      }

      try {
         mmClassTest = Class.forName("xfacthd.framedblocks.FramedBlocks");
         supportFramedBlocks = new SupportFramedBlocks();
         WorldMap.LOGGER.info("Xaero's World Map: Framed Blocks found!");
      } catch (ClassNotFoundException var5) {
      }

      try {
         Class.forName("net.irisshaders.iris.api.v0.IrisApi");
         supportIris = new SupportIris();
         iris = true;
         WorldMap.LOGGER.info("Xaero's World Map: Iris found!");
      } catch (Exception var4) {
      }

   }
}
