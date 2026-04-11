package xaero.map.mods.minimap.tracker.system;

import java.util.UUID;
import net.minecraft.class_1937;
import net.minecraft.class_5321;
import xaero.common.server.radar.tracker.SyncedTrackedPlayer;
import xaero.map.mods.SupportXaeroMinimap;
import xaero.map.radar.tracker.system.ITrackedPlayerReader;

public class MinimapSyncedTrackedPlayerReader implements ITrackedPlayerReader<SyncedTrackedPlayer> {
   private SupportXaeroMinimap minimapSupport;

   public MinimapSyncedTrackedPlayerReader(SupportXaeroMinimap minimapSupport) {
      this.minimapSupport = minimapSupport;
   }

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
      return player.getDimensionKey();
   }
}
