package xaero.map.mixin;

import net.minecraft.class_2535;
import net.minecraft.class_3222;
import net.minecraft.class_3324;
import net.minecraft.class_8792;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.WorldMap;

@Mixin({class_3324.class})
public class MixinFabricPlayerList {
   @Inject(
      at = {@At("TAIL")},
      method = {"placeNewPlayer"}
   )
   public void onPlaceNewPlayer(class_2535 connection, class_3222 serverPlayer, class_8792 commonListenerCookie, CallbackInfo info) {
      if (WorldMap.loaded) {
         WorldMap.commonEvents.onPlayerLogIn(serverPlayer);
      }
   }
}
