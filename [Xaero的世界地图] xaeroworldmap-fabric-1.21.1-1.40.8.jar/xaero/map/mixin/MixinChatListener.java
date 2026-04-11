package xaero.map.mixin;

import net.minecraft.class_2561;
import net.minecraft.class_7594;
import net.minecraft.class_2556.class_7602;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.core.XaeroWorldMapCore;

@Mixin({class_7594.class})
public class MixinChatListener {
   @Inject(
      method = {"handleDisguisedChatMessage"},
      cancellable = true,
      at = {@At("HEAD")}
   )
   public void onHandleDisguisedChatMessag(class_2561 component, class_7602 bound, CallbackInfo info) {
      if (!XaeroWorldMapCore.onHandleDisguisedChatMessage(bound, component)) {
         info.cancel();
      }

   }

   @Inject(
      method = {"handleSystemMessage"},
      cancellable = true,
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessage(Lnet/minecraft/network/chat/Component;)V"
)}
   )
   public void onHandleSystemChat(class_2561 component, boolean bl, CallbackInfo info) {
      if (XaeroWorldMapCore.onSystemChat(component)) {
         info.cancel();
      }

   }
}
