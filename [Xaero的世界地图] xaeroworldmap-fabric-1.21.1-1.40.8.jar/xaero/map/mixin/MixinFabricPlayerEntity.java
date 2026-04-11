package xaero.map.mixin;

import net.minecraft.class_1657;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.WorldMap;

@Mixin({class_1657.class})
public class MixinFabricPlayerEntity {
   @Inject(
      at = {@At("HEAD")},
      method = {"tick"}
   )
   public void onTickStart(CallbackInfo info) {
      if (WorldMap.loaded) {
         WorldMap.commonEvents.handlePlayerTickStart((class_1657)this);
      }
   }
}
