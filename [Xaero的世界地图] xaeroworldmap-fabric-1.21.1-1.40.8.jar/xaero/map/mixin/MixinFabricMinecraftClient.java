package xaero.map.mixin;

import net.minecraft.class_310;
import net.minecraft.class_437;
import net.minecraft.class_638;
import net.minecraft.class_434.class_9678;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.WorldMap;
import xaero.map.WorldMapFabric;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.core.XaeroWorldMapCoreFabric;

@Mixin({class_310.class})
public class MixinFabricMinecraftClient {
   @Shadow
   public class_437 field_1755;
   @Shadow
   public class_638 field_1687;

   @Shadow
   public void method_1507(class_437 screen) {
   }

   @Inject(
      at = {@At("HEAD")},
      method = {"tick"}
   )
   public void onTickStart(CallbackInfo info) {
      if (WorldMap.loaded) {
         WorldMap.events.handleClientTickStart();
      }

   }

   @Inject(
      at = {@At("HEAD")},
      method = {"runTick"}
   )
   public void onRunTickStart(CallbackInfo info) {
      XaeroWorldMapCoreFabric.onMinecraftRunTick();
   }

   @Inject(
      at = {@At("HEAD")},
      method = {"setScreen"},
      cancellable = true
   )
   public void onOpenScreen(class_437 screen_1, CallbackInfo info) {
      if (WorldMap.loaded) {
         class_437 resultScreen = WorldMap.events.handleGuiOpen(screen_1);
         if (screen_1 != resultScreen) {
            this.method_1507(resultScreen);
            info.cancel();
         }
      }

   }

   @Inject(
      at = {@At("HEAD")},
      method = {"disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V"}
   )
   public void onDisconnect(class_437 screen_1, boolean b, CallbackInfo info) {
      if (this.field_1687 != null) {
         if (!WorldMap.loaded) {
            return;
         }

         WorldMap.events.handleWorldUnload(this.field_1687);
      }

   }

   @Inject(
      at = {@At("HEAD")},
      method = {"setLevel"}
   )
   public void onJoinWorld(class_638 newWorld, class_9678 reason, CallbackInfo info) {
      if (this.field_1687 != null) {
         if (!WorldMap.loaded) {
            return;
         }

         WorldMap.events.handleWorldUnload(this.field_1687);
      }

   }

   @ModifyArg(
      method = {"runTick"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V"
),
      index = 1
   )
   public boolean onRenderCall(boolean renderingInGame) {
      if (WorldMap.INSTANCE != null) {
         ((WorldMapFabric)WorldMap.INSTANCE).tryLoadLater();
      }

      return XaeroWorldMapCore.onRenderCall(renderingInGame);
   }
}
