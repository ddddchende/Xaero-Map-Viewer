package xaero.map.radar.tracker.synced;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.class_1937;
import net.minecraft.class_5321;
import xaero.map.server.radar.tracker.SyncedTrackedPlayer;

public class ClientSyncedTrackedPlayerManager {
   private final Map<UUID, SyncedTrackedPlayer> trackedPlayers = new HashMap();

   public void remove(UUID id) {
      this.trackedPlayers.remove(id);
   }

   public void update(UUID id, double x, double y, double z, class_5321<class_1937> dim) {
      SyncedTrackedPlayer current = (SyncedTrackedPlayer)this.trackedPlayers.get(id);
      if (current != null) {
         current.setPos(x, y, z).setDimension(dim);
      } else {
         this.trackedPlayers.put(id, new SyncedTrackedPlayer(id, x, y, z, dim));
      }
   }

   public Iterable<SyncedTrackedPlayer> getPlayers() {
      return this.trackedPlayers.values();
   }

   public void reset() {
      this.trackedPlayers.clear();
   }
}
