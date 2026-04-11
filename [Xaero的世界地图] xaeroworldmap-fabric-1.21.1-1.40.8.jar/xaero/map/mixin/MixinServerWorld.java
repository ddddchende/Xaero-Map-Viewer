package xaero.map.mixin;

import net.minecraft.class_3218;
import org.spongepowered.asm.mixin.Mixin;
import xaero.map.capabilities.ServerWorldCapabilities;
import xaero.map.core.IWorldMapServerLevel;

@Mixin({class_3218.class})
public class MixinServerWorld implements IWorldMapServerLevel {
   public ServerWorldCapabilities xaero_wm_capabilities;

   public ServerWorldCapabilities getXaero_wm_capabilities() {
      return this.xaero_wm_capabilities;
   }

   public void setXaero_wm_capabilities(ServerWorldCapabilities capabilities) {
      this.xaero_wm_capabilities = capabilities;
   }
}
