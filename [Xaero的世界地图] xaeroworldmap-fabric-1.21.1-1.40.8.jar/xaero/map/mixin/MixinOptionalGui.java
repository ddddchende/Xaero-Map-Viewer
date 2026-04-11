package xaero.map.mixin;

import net.minecraft.class_329;
import net.minecraft.class_332;
import net.minecraft.class_9779;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.core.XaeroWorldMapCore;

@Mixin({class_329.class})
public class MixinOptionalGui {
   @Inject(
      at = {@At("HEAD")},
      method = {"renderCrosshair"},
      cancellable = true
   )
   public void onRenderCrosshair(class_332 guiGraphics, class_9779 deltaTracker, CallbackInfo info) {
      if (XaeroWorldMapCore.onRenderCrosshair(guiGraphics)) {
         info.cancel();
      }

   }
}
