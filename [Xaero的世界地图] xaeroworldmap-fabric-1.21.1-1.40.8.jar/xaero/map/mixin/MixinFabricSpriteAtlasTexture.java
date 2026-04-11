package xaero.map.mixin;

import net.minecraft.class_1059;
import net.minecraft.class_7766.class_7767;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.WorldMap;

@Mixin({class_1059.class})
public class MixinFabricSpriteAtlasTexture {
   @Inject(
      at = {@At("RETURN")},
      method = {"upload"}
   )
   public void onUpload(class_7767 spriteAtlasTexture$Data_1, CallbackInfo info) {
      if (WorldMap.loaded) {
         WorldMap.modEvents.handleTextureStitchEventPost((class_1059)this);
      }
   }
}
