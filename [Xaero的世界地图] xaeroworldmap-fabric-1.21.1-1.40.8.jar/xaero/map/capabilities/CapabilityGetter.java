package xaero.map.capabilities;

import net.minecraft.class_3218;
import xaero.map.core.IWorldMapServerLevel;

public class CapabilityGetter {
   public static ServerWorldCapabilities getServerWorldCapabilities(class_3218 level) {
      IWorldMapServerLevel serverLevel = (IWorldMapServerLevel)level;
      ServerWorldCapabilities result = serverLevel.getXaero_wm_capabilities();
      if (result == null) {
         serverLevel.setXaero_wm_capabilities(result = new ServerWorldCapabilities());
      }

      return result;
   }
}
