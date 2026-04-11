package xaero.map.mixin;

import net.minecraft.class_2637;
import net.minecraft.class_4076;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import xaero.map.core.IWorldMapSMultiBlockChangePacket;

@Mixin({class_2637.class})
public class MixinChunkDeltaUpdateS2CPacketAccessor implements IWorldMapSMultiBlockChangePacket {
   @Shadow
   private class_4076 field_26345;

   public class_4076 xaero_wm_getSectionPos() {
      return this.field_26345;
   }
}
