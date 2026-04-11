package xaero.map.mixin;

import net.minecraft.class_3222;
import org.spongepowered.asm.mixin.Mixin;
import xaero.map.server.player.IServerPlayer;
import xaero.map.server.player.ServerPlayerData;

@Mixin({class_3222.class})
public class MixinServerPlayer implements IServerPlayer {
   private ServerPlayerData xaeroWorldMapPlayerData;

   public ServerPlayerData getXaeroWorldMapPlayerData() {
      return this.xaeroWorldMapPlayerData;
   }

   public void setXaeroWorldMapPlayerData(ServerPlayerData data) {
      this.xaeroWorldMapPlayerData = data;
   }
}
