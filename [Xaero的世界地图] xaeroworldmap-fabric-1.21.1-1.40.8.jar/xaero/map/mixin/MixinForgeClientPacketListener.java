package xaero.map.mixin;

import net.minecraft.class_2678;
import net.minecraft.class_634;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.core.XaeroWorldMapCore;

@Mixin({class_634.class})
public class MixinForgeClientPacketListener {
   @Inject(
      at = {@At(
   value = "INVOKE",
   shift = Shift.AFTER,
   target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V"
)},
      method = {"handleLogin"}
   )
   public void onOnGameJoin(class_2678 packet, CallbackInfo info) {
      XaeroWorldMapCore.onPlayNetHandler((class_634)this, packet);
   }
}
