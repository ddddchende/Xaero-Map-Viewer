package xaero.map.mixin;

import com.mojang.authlib.GameProfile;
import java.time.Instant;
import net.minecraft.class_2561;
import net.minecraft.class_7471;
import net.minecraft.class_7594;
import net.minecraft.class_2556.class_7602;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.WorldMap;

@Mixin({class_7594.class})
public class MixinFabricChatListener {
   @Inject(
      method = {"showMessageToPlayer"},
      cancellable = true,
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V"
)}
   )
   public void onShowMessageToPlayer(class_7602 bound, class_7471 playerChatMessage, class_2561 component, GameProfile gameProfile, boolean bl, Instant instant, CallbackInfoReturnable<Boolean> info) {
      if (WorldMap.loaded) {
         if (WorldMap.events.handleClientPlayerChatReceivedEvent(bound, component, gameProfile)) {
            info.setReturnValue(false);
         }

      }
   }
}
