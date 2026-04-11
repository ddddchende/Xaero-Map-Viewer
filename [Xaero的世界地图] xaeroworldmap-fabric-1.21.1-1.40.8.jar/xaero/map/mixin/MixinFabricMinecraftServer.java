package xaero.map.mixin;

import java.util.function.BooleanSupplier;
import net.minecraft.class_3176;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.WorldMap;
import xaero.map.WorldMapFabric;

@Mixin({MinecraftServer.class})
public class MixinFabricMinecraftServer {
   @Inject(
      at = {@At("HEAD")},
      method = {"tickServer"}
   )
   public void onTick(BooleanSupplier shouldKeepTicking, CallbackInfo info) {
      if (this instanceof class_3176) {
         if (WorldMap.INSTANCE != null) {
            ((WorldMapFabric)WorldMap.INSTANCE).tryLoadLaterServer();
         }

      }
   }
}
