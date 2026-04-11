package xaero.map.server.mods;

import net.minecraft.class_3222;
import xaero.common.server.XaeroMinimapServer;
import xaero.common.server.player.ServerPlayerData;

public class SupportMinimapServer {
   private final int compatibilityVersion;

   public SupportMinimapServer() {
      int compatibilityVersion = 0;

      try {
         compatibilityVersion = XaeroMinimapServer.SERVER_COMPATIBILITY;
      } catch (Throwable var3) {
      }

      this.compatibilityVersion = compatibilityVersion;
   }

   public boolean supportsTrackedPlayers() {
      return this.compatibilityVersion >= 1;
   }

   public boolean playerSupportsTrackedPlayers(class_3222 player) {
      ServerPlayerData playerData = ServerPlayerData.get(player);
      return playerData.hasMod();
   }
}
