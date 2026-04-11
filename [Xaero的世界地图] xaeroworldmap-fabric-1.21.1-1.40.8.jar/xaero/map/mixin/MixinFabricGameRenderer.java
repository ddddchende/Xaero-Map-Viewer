package xaero.map.mixin;

import net.minecraft.class_310;
import net.minecraft.class_757;
import net.minecraft.class_9779;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.WorldMap;

@Mixin(
   value = {class_757.class},
   priority = 1000001
)
public class MixinFabricGameRenderer {
   @Shadow
   private class_310 field_4015;

   @Inject(
      at = {@At("RETURN")},
      method = {"render"}
   )
   public void onRenderEnd(class_9779 deltaTracker, boolean boolean_1, CallbackInfo info) {
      if (WorldMap.loaded) {
         WorldMap.events.handleRenderTick(false);
         if (!this.field_4015.field_1743 && this.field_4015.method_18506() == null && this.field_4015.field_1755 != null) {
            WorldMap.events.handleDrawScreen(this.field_4015.field_1755);
         }

      }
   }
}
