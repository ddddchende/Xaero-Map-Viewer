package xaero.map.server.player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.class_3222;
import net.minecraft.class_5321;
import xaero.map.server.radar.tracker.SyncedTrackedPlayer;

public class ServerPlayerData {
   private final UUID playerId;
   private SyncedTrackedPlayer lastSyncedData;
   private Set<UUID> currentlySyncedPlayers;
   private long lastTrackedPlayerSync;
   private int clientModNetworkVersion;
   private Object opacData;

   public ServerPlayerData(UUID playerId) {
      this.playerId = playerId;
   }

   public SyncedTrackedPlayer getLastSyncedData() {
      return this.lastSyncedData;
   }

   public SyncedTrackedPlayer ensureLastSyncedData() {
      if (this.lastSyncedData == null) {
         this.lastSyncedData = new SyncedTrackedPlayer(this.playerId, 0.0D, 0.0D, 0.0D, (class_5321)null);
      }

      return this.lastSyncedData;
   }

   public Set<UUID> getCurrentlySyncedPlayers() {
      return this.currentlySyncedPlayers;
   }

   public Set<UUID> ensureCurrentlySyncedPlayers() {
      if (this.currentlySyncedPlayers == null) {
         this.currentlySyncedPlayers = new HashSet();
      }

      return this.currentlySyncedPlayers;
   }

   public long getLastTrackedPlayerSync() {
      return this.lastTrackedPlayerSync;
   }

   public void setLastTrackedPlayerSync(long lastTrackedPlayerSync) {
      this.lastTrackedPlayerSync = lastTrackedPlayerSync;
   }

   public static ServerPlayerData get(class_3222 player) {
      ServerPlayerData result = ((IServerPlayer)player).getXaeroWorldMapPlayerData();
      if (result == null) {
         ((IServerPlayer)player).setXaeroWorldMapPlayerData(result = new ServerPlayerData(player.method_5667()));
      }

      return result;
   }

   public boolean hasMod() {
      return this.clientModNetworkVersion != 0;
   }

   public void setClientModNetworkVersion(int clientModNetworkVersion) {
      this.clientModNetworkVersion = clientModNetworkVersion;
   }

   public int getClientModNetworkVersion() {
      return this.clientModNetworkVersion;
   }

   public void setOpacData(Object opacData) {
      this.opacData = opacData;
   }

   public Object getOpacData() {
      return this.opacData;
   }
}
