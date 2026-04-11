package xaero.map.radar.tracker.system.impl;

import java.util.Iterator;
import net.minecraft.class_310;
import xaero.map.WorldMapSession;
import xaero.map.mcworld.WorldMapClientWorldData;
import xaero.map.mcworld.WorldMapClientWorldDataHelper;
import xaero.map.radar.tracker.synced.ClientSyncedTrackedPlayerManager;
import xaero.map.radar.tracker.system.IPlayerTrackerSystem;
import xaero.map.radar.tracker.system.ITrackedPlayerReader;
import xaero.map.server.radar.tracker.SyncedTrackedPlayer;

public class SyncedPlayerTrackerSystem implements IPlayerTrackerSystem<SyncedTrackedPlayer> {
   private final SyncedTrackedPlayerReader reader = new SyncedTrackedPlayerReader();

   public ITrackedPlayerReader<SyncedTrackedPlayer> getReader() {
      return this.reader;
   }

   public Iterator<SyncedTrackedPlayer> getTrackedPlayerIterator() {
      WorldMapSession session = WorldMapSession.getCurrentSession();
      if (session == null) {
         return null;
      } else {
         if (class_310.method_1551().method_1576() == null) {
            WorldMapClientWorldData worldData = WorldMapClientWorldDataHelper.getCurrentWorldData();
            if (worldData.serverLevelId == null) {
               return null;
            }
         }

         ClientSyncedTrackedPlayerManager manager = session.getMapProcessor().getClientSyncedTrackedPlayerManager();
         return manager.getPlayers().iterator();
      }
   }
}
