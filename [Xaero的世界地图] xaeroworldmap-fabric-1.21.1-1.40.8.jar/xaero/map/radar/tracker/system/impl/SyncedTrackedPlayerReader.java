package xaero.map.radar.tracker.system.impl;

import java.util.UUID;
import net.minecraft.class_1937;
import net.minecraft.class_5321;
import xaero.map.radar.tracker.system.ITrackedPlayerReader;
import xaero.map.server.radar.tracker.SyncedTrackedPlayer;

public class SyncedTrackedPlayerReader implements ITrackedPlayerReader<SyncedTrackedPlayer> {
   public UUID getId(SyncedTrackedPlayer player) {
      return player.getId();
   }

   public double getX(SyncedTrackedPlayer player) {
      return player.getX();
   }

   public double getY(SyncedTrackedPlayer player) {
      return player.getY();
   }

   public double getZ(SyncedTrackedPlayer player) {
      return player.getZ();
   }

   public class_5321<class_1937> getDimension(SyncedTrackedPlayer player) {
      return player.getDimension();
   }
}
