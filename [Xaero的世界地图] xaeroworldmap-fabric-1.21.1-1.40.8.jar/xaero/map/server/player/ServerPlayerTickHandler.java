package xaero.map.server.player;

import net.minecraft.class_3222;
import net.minecraft.server.MinecraftServer;
import xaero.map.server.MinecraftServerData;

public class ServerPlayerTickHandler {
   public void tick(class_3222 player) {
      MinecraftServer server = player.method_5682();
      MinecraftServerData serverData = MinecraftServerData.get(server);
      ServerPlayerData playerData = ServerPlayerData.get(player);
      serverData.getSyncedPlayerTracker().onTick(server, player, serverData, playerData);
   }
}
