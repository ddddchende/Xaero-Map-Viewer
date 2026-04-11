package xaero.map.mcworld;

import net.minecraft.class_2338;
import net.minecraft.class_638;
import xaero.map.message.basic.ClientboundRulesPacket;

public class WorldMapClientWorldData {
   private int serverModNetworkVersion;
   public Integer serverLevelId;
   public Integer usedServerLevelId;
   public class_2338 latestSpawn;
   public class_2338 usedSpawn;
   private ClientboundRulesPacket syncedRules;

   public WorldMapClientWorldData(class_638 world) {
   }

   public void setServerModNetworkVersion(int serverModNetworkVersion) {
      this.serverModNetworkVersion = serverModNetworkVersion;
   }

   public int getServerModNetworkVersion() {
      return this.serverModNetworkVersion;
   }

   public void setSyncedRules(ClientboundRulesPacket syncedRules) {
      this.syncedRules = syncedRules;
   }

   public ClientboundRulesPacket getSyncedRules() {
      if (this.syncedRules == null) {
         this.syncedRules = new ClientboundRulesPacket(true, true);
      }

      return this.syncedRules;
   }
}
