package xaero.map.mixin;

import net.minecraft.class_1937;
import net.minecraft.class_3218;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.WorldMap;

@Mixin({class_1937.class})
public class MixinFabricWorld {
   @Inject(
      at = {@At("HEAD")},
      method = {"close"}
   )
   public void onClose(CallbackInfo info) {
      if (this instanceof class_3218) {
         if (!WorldMap.loaded) {
            return;
         }

         WorldMap.events.handleWorldUnload((class_3218)this);
      }

   }
}
