package xaero.map.server.mods.argonauts;

import xaero.map.server.radar.tracker.ISyncedPlayerTrackerSystem;

public class SupportArgonautsServer {
   private final ISyncedPlayerTrackerSystem syncedPlayerTrackerSystem = new ArgonautsSyncedPlayerTrackerSystem();

   public ISyncedPlayerTrackerSystem getSyncedPlayerTrackerSystem() {
      return this.syncedPlayerTrackerSystem;
   }
}
